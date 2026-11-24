#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "  TrueVision (Anti-Invisibility) - Minecraft 26.3"
echo "  Standalone Compiler (Java 25+ / Mojang Mappings)"
echo "=========================================================="

mkdir -p build/libs
TMP_BUILD=$(mktemp -d)
TMP_API_DIR=$(mktemp -d)

# Clean up temporary directories even on error
trap 'rm -rf "$TMP_BUILD" "$TMP_API_DIR"' EXIT

# 1. Detect Java 27 / 25 compiler
JAVA_HOME_27="/usr/lib/jvm/java-27-openjdk"
if [ -x "$JAVA_HOME_27/bin/javac" ] && [ -x "$JAVA_HOME_27/bin/jar" ]; then
    JAVAC_BIN="$JAVA_HOME_27/bin/javac"
    JAR_BIN="$JAVA_HOME_27/bin/jar"
else
    JAVAC_BIN="javac"
    JAR_BIN="jar"
fi

# 2. Collect SKLauncher libraries
SKLAUNCHER_DIR="${SKLAUNCHER_DIR:-$HOME/.sklauncher}"
MC_JAR="$SKLAUNCHER_DIR/versions/26.3/26.3.jar"
if [ ! -f "$MC_JAR" ]; then
    echo "[ERROR] Minecraft 26.3.jar not found: $MC_JAR"
    exit 1
fi

find_latest_jar() {
    local dir="$1"
    local pattern="$2"
    if [ -d "$dir" ]; then
        find "$dir" -name "$pattern" 2>/dev/null | sort -V | tail -n 1 || true
    fi
}

LOADER_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/net/fabricmc/fabric-loader" "fabric-loader-*.jar")
MIXIN_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/net/fabricmc/sponge-mixin" "sponge-mixin-*.jar")
GSON_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/com/google/code/gson/gson" "gson-*.jar")
SLF4J_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/org/slf4j/slf4j-api" "slf4j-api-*.jar")
FASTUTIL_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/it/unimi/dsi/fastutil" "fastutil-*.jar")
BRIGADIER_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/com/mojang/brigadier" "brigadier-*.jar")
DFU_JAR=$(find_latest_jar "$SKLAUNCHER_DIR/libraries/com/mojang/datafixerupper" "datafixerupper-*.jar")
FABRIC_API_MOD=$(find_latest_jar "$SKLAUNCHER_DIR/instances/fabric-latest/mods" "fabric-api-*.jar")

for req_var in LOADER_JAR MIXIN_JAR GSON_JAR BRIGADIER_JAR FABRIC_API_MOD; do
    if [ -z "${!req_var}" ] || [ ! -f "${!req_var}" ]; then
        echo "[ERROR] Required library not found: $req_var"
        exit 1
    fi
done

# Extract nested Fabric API module JARs
unzip -q -j "$FABRIC_API_MOD" "META-INF/jars/*.jar" -d "$TMP_API_DIR" 2>/dev/null || true

# Build clean classpath array without empty (::) entries
CP_ENTRIES=()
for jar_entry in "$MC_JAR" "$LOADER_JAR" "$MIXIN_JAR" "$GSON_JAR" "$SLF4J_JAR" "$FASTUTIL_JAR" "$BRIGADIER_JAR" "$DFU_JAR"; do
    [ -n "$jar_entry" ] && [ -f "$jar_entry" ] && CP_ENTRIES+=("$jar_entry")
done

if [ -d "$SKLAUNCHER_DIR/libraries/io/netty" ]; then
    while IFS= read -r j; do [ -n "$j" ] && CP_ENTRIES+=("$j"); done < <(find "$SKLAUNCHER_DIR/libraries/io/netty" -name "*.jar" 2>/dev/null)
fi
if [ -d "$SKLAUNCHER_DIR/libraries/org/lwjgl" ]; then
    while IFS= read -r j; do [ -n "$j" ] && CP_ENTRIES+=("$j"); done < <(find "$SKLAUNCHER_DIR/libraries/org/lwjgl" -name "*.jar" ! -name "*natives*" 2>/dev/null)
fi
while IFS= read -r j; do [ -n "$j" ] && CP_ENTRIES+=("$j"); done < <(find "$TMP_API_DIR" -name "*.jar" 2>/dev/null)

CP=$(IFS=:; echo "${CP_ENTRIES[*]}")

mapfile -t JAVA_FILES < <(find src/main/java -name "*.java" | sort)
if [ "${#JAVA_FILES[@]}" -eq 0 ]; then
    echo "[ERROR] No Java source files found to compile."
    exit 1
fi

echo "[1/3] Compiling Java 25 source files (${#JAVA_FILES[@]} files)..."
"$JAVAC_BIN" --release 25 -encoding UTF-8 -Xlint:all,-processing -cp "$CP" -d "$TMP_BUILD" "${JAVA_FILES[@]}"

echo "[2/3] Copying resources and mixin configuration..."
cp -r src/main/resources/* "$TMP_BUILD/"
if [ -f "LICENSE" ]; then
    cp "LICENSE" "$TMP_BUILD/LICENSE"
fi

OUTPUT_JAR="build/libs/AntiInvis-26.3-1.0.0.jar"
echo "[3/3] Packaging JAR: $OUTPUT_JAR"
"$JAR_BIN" --create --file "$OUTPUT_JAR" -C "$TMP_BUILD" .

# 3. Install automatically into SKLauncher fabric-latest mods directory
DEST_DIR="$SKLAUNCHER_DIR/instances/fabric-latest/mods"
if [ -d "$DEST_DIR" ]; then
    rm -f "$DEST_DIR"/AntiInvis-*.jar
    cp "$OUTPUT_JAR" "$DEST_DIR/AntiInvis-26.3-1.0.0.jar"
    echo "  -> Installed into SKLauncher 'fabric-latest' mods folder!"
fi

echo "=========================================================="
echo "  [SUCCESS] Built AntiInvis-26.3-1.0.0.jar!"
echo "  Location: $OUTPUT_JAR"
echo "=========================================================="
