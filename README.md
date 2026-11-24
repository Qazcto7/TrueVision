# TrueVision — Minecraft 26.3

A lightweight, **100% client-side** Fabric mod for **Minecraft 26.3 (Java 25+, Mojang Mappings)** that reveals invisible players and mobs using Minecraft's native Render State pipeline.

Works out of the box on **all multiplayer servers** (Vanilla, Paper, Spigot, Purpur, Folia, BungeeCord, Velocity, Realms, Fabric/NeoForge, and ViaFabricPlus multi-version servers) as well as **all singleplayer and LAN worlds** — sends **zero network packets** and requires **no server-side installation**.

---

## Features

1. **Native Minecraft 26.3 Render State Architecture**:
   - Hooks `LivingEntityRenderState` / `AvatarRenderState` extraction cleanly without touching vertex buffers or draw calls (`submit`/`render`).
   - Fully compatible with **Sodium**, **Iris**, **ImmediatelyFast**, **ModernFix**, **NoRender**, **Freecam**, **ViaFabricPlus**, and large client modpacks (`priority = 1100`, `require = 0`, zero `@Overwrite`).
   - Preserves vanilla glowing effects (such as Spectral Arrows) and excludes multiplayer server holograms (`ArmorStand`) and decorative invisible entities (`ItemFrame`, `Display`, `Marker`).

2. **Four Customizable Vision Modes**:
   - **`glowing` (Opaque + Outline)**: Solid model, armor, held items, cape, and a glowing outline.
   - **`translucent` (Translucent / Ghost)**: Spectator-style semi-transparent ghost rendering.
   - **`tagged` (Opaque + Badge)**: Solid rendering with an `[INVIS]` badge on the nametag.
   - **`pure` (Opaque / Pure)**: Normal solid rendering with no extra visual indicators.

3. **Smart Nametag & Line-of-Sight Handling**:
   - Detects both synced entity metadata flags (`entity.isInvisible()`) and local potion effects (`MobEffects.INVISIBILITY`).
   - Recovers hidden, sneaking, or blank-display-name tags with a color-safe `[INVIS]` prefix while respecting `F1` (Hide HUD) and `F5`/Freecam/Spectator camera entities.
   - Toggleable **See Through Walls** setting: when disabled, uses `ClipContext.Block.VISUAL` so transparent blocks (glass, iron bars, fences) never block vision while solid walls suppress outlines.

4. **Controls, Commands & Feedback**:
   - **`V` Key**: Toggle TrueVision ON / OFF (rebindable under *Controls → TrueVision*).
   - **`B` Key**: Cycle between vision modes.
   - **Client-Only Commands (`/tv` or `/truevision`)**:
     - `/tv` or `/tv toggle` — Toggle ON / OFF
     - `/tv mode [glowing|translucent|tagged|pure]` — Cycle or directly select a vision mode
     - `/tv walls` (or `/tv throughwalls`) — Toggle outlines and sneaking nametags through walls
     - `/tv players` — Toggle Players Only vs All Living Entities (Players & Mobs)
     - `/tv badge` — Toggle the `[INVIS]` nametag badge
     - `/tv status` — View current settings and active keybinds
     - `/tv help` — Show command and vision mode guide
   - Plays a subtle UI click sound with ActionBar and Toast notifications.

---

## Configuration

Settings are automatically saved atomically to:
```text
.minecraft/config/truevision.json
```

---

## Building (`derle.sh`)

```bash
./derle.sh
```

Compiles all Java sources with `--release 25 -encoding UTF-8`, packages `build/libs/TrueVision-26.3-1.0.0.jar`, and installs it into `$SKLAUNCHER_DIR/instances/fabric-latest/mods/` when present.
