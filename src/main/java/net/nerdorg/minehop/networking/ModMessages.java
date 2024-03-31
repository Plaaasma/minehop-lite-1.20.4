package net.nerdorg.minehop.networking;

import net.minecraft.util.Identifier;
import net.nerdorg.minehop.Minehop;

public class ModMessages {
    public static final Identifier CONFIG_SYNC_ID = new Identifier(Minehop.MOD_ID, "config");
    public static final Identifier ZONE_SYNC_ID = new Identifier(Minehop.MOD_ID, "zone");
    public static final Identifier SELF_V_TOGGLE = new Identifier(Minehop.MOD_ID, "self_v_toggle");
    public static final Identifier OTHER_V_TOGGLE = new Identifier(Minehop.MOD_ID, "other_v_toggle");
    public static final Identifier ANTI_CHEAT_CHECK = new Identifier(Minehop.MOD_ID, "anti_cheat_check");
}
