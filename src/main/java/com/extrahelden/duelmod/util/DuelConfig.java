package com.extrahelden.duelmod.util;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class DuelConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        COMMON = new Common(b);
        COMMON_SPEC = b.build();
    }

    public static class Common {
        // Bereits vorhanden (Beispiel)
        public final ForgeConfigSpec.ConfigValue<String> defaultTexture;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> playerTextures;

        // 🔥 NEU: Overrides für Besitzer-Namen des LinkedHeart-Kopfes
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> ownerOverrides;

        public Common(ForgeConfigSpec.Builder b) {
            b.push("linked_hearts");

            defaultTexture = b
                    .comment("Fallback Linked-Heart Textur, falls kein spezieller Eintrag passt.")
                    .define("defaultTexture", "forge_mod:textures/gui/linked_heart.png");

            playerTextures = b
                    .comment("Spieler-spezifische Texturen (falls du PNGs statt Skins verwenden willst).",
                            "Format: name:<mcname>->modid:textures/gui/<file>.png ODER uuid:<uuid>->...")
                    .defineList("playerTextures",
                            List.of(),
                            o -> o instanceof String);

            // 🔥 NEU: Owner-Overrides. Links: name:<mcname> ODER uuid:<uuid> ; rechts: Ziel-Spielername für den Kopf
            ownerOverrides = b
                    .comment("LinkedHeart-Kopf-Overrides: welcher Spieler-Kopf soll angezeigt werden?",
                            "Format OHNE Leerzeichen: <key>-><ownerName>",
                            "Beispiele:",
                            "  name:UfoSMP->netheriteDOThuhn",
                            "  uuid:123e4567-e89b-12d3-a456-426614174000->SomeOtherName")
                    .defineList("ownerOverrides",
                            List.of("name:UfoSMP->netheriteDOThuhn"),
                            o -> o instanceof String);

            b.pop();
        }
    }
}
