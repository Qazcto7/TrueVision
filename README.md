# Anti-Invisibility (True Vision) - Minecraft 26.3

Minecraft 26.3 (Fabric Loader, Mojang Mappings, Java 25+) için geliştirilmiş, **Sodium**, **Iris**, **ImmediatelyFast**, **ModernFix**, **NoRender**, **ViaFabricPlus** ve **HyperBoost** gibi agresif render & optimizasyon modlarıyla tam uyumlu çalışan istemci taraflı (client-side) görünmezlik engelleyici / True Vision modu.

---

## 🌟 Öne Çıkan Özellikler

1. **Minecraft 26.3 Render State Mimarisi ile Tam Uyum**:
   - Minecraft 26.3 `LivingEntityRenderState` ve `AvatarRenderState` veri çıkarma (extraction) katmanında çalışır.
   - Doğrudan çizim komutlarına (`submit/render`) dokunulmaz; mevcut vanilla parlama efektleri (Spectral Arrow vb.) korunur.
   - Zırhlar (`HumanoidArmorLayer`), elde tutulan eşyalar (`PlayerItemInHandLayer`), pelerinler ve elytra kanatları normal bir varlık gibi render kuyruğuna sokulur.
   - Çok oyunculu sunuculardaki hologram `ArmorStand` ve dekoratif `ItemFrame` varlıkları otomatik olarak hariç tutulur.

2. **Gelişmiş Görünmezlik Tespiti ve İsim Fallback'i**:
   - Hem metadata bayrağı (`entity.isInvisible()`) hem de sunucu kaynaklı iksir etkileri (`entity.hasEffect(MobEffects.INVISIBILITY)`) taranır.
   - Eğilen (crouching/sneaking) veya etiketleri gizlenmiş görünmez oyuncuların isimleri `entity.getDisplayName()` / `entity.getName()` fallback'i ile çekilerek renk sızıntısı olmadan `[GÖRÜNMEZ]` etiketi eklenir; F1 (HUD gizli) ve F5/Freecam kamera varlığı otomatik korunur.

3. **Görünüm Modları ve Duvardan Parlama Filtresi (`seeThroughWalls`)**:
   - `OPAQUE_GLOWING`, `TRANSLUCENT`, `OPAQUE_TAGGED` ve `OPAQUE_PURE` modları arasında anında geçiş yapılabilir.
   - `OPAQUE_GLOWING` modunda `seeThroughWalls = false` seçildiğinde `ClipContext.Block.VISUAL` görüş hattı kontrolü ile cam gibi şeffaf blokların arkasındaki oyuncular parlamaya devam ederken katı duvar arkasındakiler parlamaz.

4. **Kısayol Tuşları, Komutlar ve Bildirimler**:
   - **`V` Tuşu**: Modu anında AÇAR / KAPATIR.
   - **`B` Tuşu**: Görsel render modları arasında döngüsel geçiş yapar.
   - **Komutlar (`/ai` veya `/antiinvis`)**: `toggle`, `mode`, `wallhack`, `players`, `badge`, `status`, `help`.
   - Tuş ve komut işlemlerinde hafif bir UI tıklama sesi çalar, alt kısımda **ActionBar** mesajı ve sağ üstte **Toast** bildirimi gösterilir.

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
