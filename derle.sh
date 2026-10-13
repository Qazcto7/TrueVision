#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "  Anti-Invisibility (True Vision) - Minecraft 26.3"
echo "  Bagimsiz Derleyici (Java 25+ / Mojang Mappings)"
echo "=========================================================="

mkdir -p build/libs
TMP_BUILD=$(mktemp -d)
TMP_API_DIR=$(mktemp -d)

# Hata olussa dahi gecici dizinleri guvenle temizle
trap 'rm -rf "$TMP_BUILD" "$TMP_API_DIR"' EXIT

# 1. Java 27 / 25 Derleyici Tespiti
JAVA_HOME_27="/usr/lib/jvm/java-27-openjdk"
if [ -d "$JAVA_HOME_27" ]; then
    JAVAC_BIN="$JAVA_HOME_27/bin/javac"
    JAR_BIN="$JAVA_HOME_27/bin/jar"
else
    JAVAC_BIN="javac"
    JAR_BIN="jar"
fi

if [ ! -f "$JAVAC_BIN" ]; then
    JAVAC_BIN="javac"
    JAR_BIN="jar"
fi

# 2. SKLauncher Kütüphanelerini Topla
MC_JAR="/home/kazim/.sklauncher/versions/26.3/26.3.jar"
if [ ! -f "$MC_JAR" ]; then
    echo "[HATA] Minecraft 26.3.jar bulunamadi: $MC_JAR"
    exit 1
fi

LOADER_JAR=$(find /home/kazim/.sklauncher/libraries/net/fabricmc/fabric-loader/ -name "fabric-loader-*.jar" 2>/dev/null | sort -V | tail -n 1)
MIXIN_JAR=$(find /home/kazim/.sklauncher/libraries/net/fabricmc/sponge-mixin/ -name "sponge-mixin-*.jar" 2>/dev/null | sort -V | tail -n 1)
GSON_JAR=$(find /home/kazim/.sklauncher/libraries/com/google/code/gson/gson/ -name "gson-*.jar" 2>/dev/null | sort -V | tail -n 1)
SLF4J_JAR=$(find /home/kazim/.sklauncher/libraries/org/slf4j/slf4j-api/ -name "slf4j-api-*.jar" 2>/dev/null | sort -V | tail -n 1)
FASTUTIL_JAR=$(find /home/kazim/.sklauncher/libraries/it/unimi/dsi/fastutil/ -name "fastutil-*.jar" 2>/dev/null | sort -V | tail -n 1)
BRIGADIER_JAR=$(find /home/kazim/.sklauncher/libraries/com/mojang/brigadier/ -name "brigadier-*.jar" 2>/dev/null | sort -V | tail -n 1)
DFU_JAR=$(find /home/kazim/.sklauncher/libraries/com/mojang/datafixerupper/ -name "datafixerupper-*.jar" 2>/dev/null | sort -V | tail -n 1)
NETTY_JARS=$(find /home/kazim/.sklauncher/libraries/io/netty/ -name "*.jar" 2>/dev/null | tr '\n' ':')
LWJGL_JARS=$(find /home/kazim/.sklauncher/libraries/org/lwjgl/ -name "*.jar" 2>/dev/null | grep -v "natives" | tr '\n' ':')

# Fabric API modül JAR'larını çıkar ve classpath'e ekle
FABRIC_API_MOD=$(find /home/kazim/.sklauncher/instances/fabric-latest/mods/ -name "fabric-api-*.jar" 2>/dev/null | sort -V | tail -n 1)
if [ -f "$FABRIC_API_MOD" ]; then
    unzip -q -j "$FABRIC_API_MOD" "META-INF/jars/*.jar" -d "$TMP_API_DIR" 2>/dev/null || true
fi
API_JARS=$(find "$TMP_API_DIR" -name "*.jar" 2>/dev/null | tr '\n' ':')

CP="$MC_JAR:$LOADER_JAR:$MIXIN_JAR:$GSON_JAR:$SLF4J_JAR:$FASTUTIL_JAR:$BRIGADIER_JAR:$DFU_JAR:$NETTY_JARS:$LWJGL_JARS:$API_JARS"

echo "[1/3] Java 25 kaynak kodlari derleniyor..."
$JAVAC_BIN --release 25 -cp "$CP" -d "$TMP_BUILD" $(find src/main/java -name "*.java")

echo "[2/3] Kaynaklar ve mixin yapilandirmasi kopyalaniyor..."
cp -r src/main/resources/* "$TMP_BUILD/"

OUTPUT_JAR="build/libs/AntiInvis-26.3-1.0.0.jar"
echo "[3/3] JAR paketi olusturuluyor: $OUTPUT_JAR"
$JAR_BIN --create --file "$OUTPUT_JAR" -C "$TMP_BUILD" .

# 3. SKLauncher fabric-latest mods klasörüne otomatik kopyala
DEST_DIR="/home/kazim/.sklauncher/instances/fabric-latest/mods"
if [ -d "$DEST_DIR" ]; then
    rm -f "$DEST_DIR"/AntiInvis-*.jar
    cp "$OUTPUT_JAR" "$DEST_DIR/AntiInvis-26.3-1.0.0.jar"
    echo "  -> SKLauncher 'fabric-latest' mod klasorune kuruldu!"
fi

echo "=========================================================="
echo "  [BASARILI] AntiInvis-26.3-1.0.0.jar olusturuldu!"
echo "  Konum: $OUTPUT_JAR"
echo "=========================================================="
