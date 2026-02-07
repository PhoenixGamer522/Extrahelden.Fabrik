package com.extrahelden.duelmod.perms;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class Permissions {
    private Permissions() {}

    // "Nodes" kannst du behalten, z.B. für spätere Integration (LuckPerms etc.)
    public static final String VANISH_SELF   = "duelmod.command.vanish";
    public static final String VANISH_OTHERS = "duelmod.command.vanish.others";
    public static final String MYLIVES_BASE     = "duelmod.command.mylives";
    public static final String MYLIVES_SET      = "duelmod.command.mylives.set";
    public static final String MYLIVES_SETNAME  = "duelmod.command.mylives.setname";

    // Welche OP-Level du brauchst (2 = Commands, 3 = Mehr Admin)
    private static final int OP_VANISH_SELF   = 2;
    private static final int OP_VANISH_OTHERS = 3;
    private static final int OP_MYLV_BASE    = 2;
    private static final int OP_MYLV_SET     = 2;
    private static final int OP_MYLV_SETNAME = 2;

    /** Konsolen/Command-Blocks erlauben; Spieler per OP-Level. */
    public static boolean check(CommandSourceStack src, String node) {
        if (!(src.getEntity() instanceof ServerPlayer)) return true; // Konsole & Cmd-Block
        int required = switch (node) {
            case VANISH_SELF   -> OP_VANISH_SELF;
            case VANISH_OTHERS -> OP_VANISH_OTHERS;
            case MYLIVES_BASE    -> OP_MYLV_BASE;
            case MYLIVES_SET     -> OP_MYLV_SET;
            case MYLIVES_SETNAME -> OP_MYLV_SETNAME;
            default -> 4; // sicherer Default
        };
        return src.hasPermission(required);
    }
}
