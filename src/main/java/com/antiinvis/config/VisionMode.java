package com.antiinvis.config;

/**
 * Anti-Invisibility görsel işleme ve render modları.
 */
public enum VisionMode {
    /**
     * Varlığın modeli, zırhı ve elindeki eşyalar tam opak render edilir;
     * ayrıca dış hatlarında parlayan bir silüet (outline/glowing) oluşturulur.
     * Bu sayede hem ne giyip tuttuğu net görülür hem de görünmez olduğu anında anlaşılır.
     */
    OPAQUE_GLOWING("§eTam Opak + Parlama", "Opak model, zırh, eşyalar ve parlayan dış çizgi"),

    /**
     * Spectator (İzleyici) modundaki gibi yarı saydam (translucent / alpha ~0.5f) hayalet görünümü.
     */
    TRANSLUCENT("§bYarı Saydam (Spectator)", "İzleyici modu benzeri yarı saydam hayalet görünümü"),

    /**
     * Varlık tam opak render edilir ve isim etiketinin başına "[GÖRÜNMEZ]" belirteci eklenir.
     */
    OPAQUE_TAGGED("§6Tam Opak + İsim Etiketi", "Tam opak render ve isim üzerinde görünmezlik etiketi"),

    /**
     * Tam opak render (görünmezlik etkisi tamamen yok sayılarak normal bir varlık gibi render edilir).
     */
    OPAQUE_PURE("§aTam Opak (Saf)", "Görünmezlik yok sayılır, tamamen normal render edilir");

    private final String displayName;
    private final String description;

    VisionMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sıradaki render moduna geçiş yapar (döngüsel).
     */
    public VisionMode next() {
        VisionMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
