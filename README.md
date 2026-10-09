# Anti-Invisibility (True Vision) - Minecraft 26.3

Minecraft 26.3 (Fabric Loader, Mojang Mappings, Java 25+) için geliştirilmiş, **Sodium**, **Iris**, **ImmediatelyFast**, **ModernFix**, **NoRender**, **ViaFabricPlus** ve **HyperBoost** gibi agresif render & optimizasyon modlarıyla tam uyumlu çalışan istemci taraflı (client-side) görünmezlik engelleyici / True Vision modu.

---

## 🌟 Öne Çıkan Özellikler

1. **Minecraft 26.3 Render State Mimarisi ile Tam Uyum**:
   - Minecraft 26.3 `LivingEntityRenderState` ve `PlayerRenderState` veri çıkarma (extraction) katmanında çalışır.
   - Doğrudan çizim komutlarına (`submit/render`) dokunulmaz.
   - Zırhlar (`HumanoidArmorLayer`), elde tutulan eşyalar (`ItemInHandLayer`), pelerinler ve elytra kanatları normal bir varlık gibi render kuyruğuna sokulur.

2. **Gelişmiş Görünmezlik Tespiti ve İsim Fallback'i**:
   - Hem metadata bayrağı (`entity.isInvisible()`) hem de sunucu kaynaklı iksir etkileri (`entity.hasEffect(MobEffects.INVISIBILITY)`) taranır.
   - Eğilen (crouching/sneaking) veya etiketleri gizlenmiş oyuncuların isimleri `entity.getDisplayName()` ve `entity.getName()` fallback'i ile çekilerek `§d[GÖRÜNMEZ] <İsim>` etiketi oluşturulur.

3. **Duvardan Parlama (Wallhack / ESP) Filtresi (`seeThroughWalls`)**:
   - `OPAQUE_GLOWING` modunda `seeThroughWalls = true/false` ayarı ile parlayan oyuncuların blokların arkasından görünüp görünmeyeceği seçilebilir. Ayar kapalıysa `player.hasLineOfSight(entity)` kontrolü ile duvar arkasındaki oyuncular parlamaz.

4. **Kısayol Tuşları, Sesli ve Görsel Bildirimler**:
   - **`V` Tuşu**: Modu anında AÇAR / KAPATIR.
   - **`B` Tuşu**: Görsel render modları arasında döngüsel geçiş yapar.
   - Tuş basımlarında hafif bir UI tıklama sesi çalar, alt kısımda **ActionBar** mesajı ve sağ üstte şık bir **Toast** bildirimi gösterilir.

---

## 🛠️ Bağımsız Derleme (`derle.sh`)

Sisteminizde Gradle kurulu olmasına gerek yoktur. Tıpkı HyperBoost projesinde olduğu gibi doğrudan Java 25/27 `javac` ve SKLauncher kütüphanelerini kullanarak derleme yapar:

```bash
./derle.sh
```

Betiğin yaptığı işlemler:
1. Java 27 / 25 (`javac`) tespit eder.
2. Minecraft 26.3 (`26.3.jar`), Fabric Loader, Sponge Mixin, Gson ve Fabric API kütüphanelerini classpath'e ekler.
3. Kaynak kodları `--release 25` ile derler ve `build/libs/AntiInvis-26.3-1.0.0.jar` dosyasını oluşturur.
4. Oluşan JAR'ı otomatik olarak `~/.sklauncher/instances/fabric-latest/mods/` dizinine kurar.
