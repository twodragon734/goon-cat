package com.example.battlecats;

import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

public final class Gacha {
    private static final UnitType[] POOL = {
            UnitType.BASIC_CAT, UnitType.TANK_CAT, UnitType.AXE_CAT, UnitType.ARCHER_CAT, UnitType.TITAN_CAT
    };
    private static final int[] WEIGHT = {38, 30, 20, 9, 3};

    private Gacha() {}

    public static UnitType roll(Random random) {
        int total = 0;
        for (int w : WEIGHT) total += w;
        int n = random.nextInt(total);
        for (int i = 0; i < POOL.length; i++) {
            n -= WEIGHT[i];
            if (n < 0) return POOL[i];
        }
        return POOL[0];
    }

    public static String rarity(UnitType t) {
        return switch (t) {
            case BASIC_CAT, TANK_CAT -> "Common";
            case AXE_CAT -> "Uncommon";
            case ARCHER_CAT -> "Rare";
            default -> "LEGENDARY";
        };
    }

    public static Formatting color(UnitType t) {
        return switch (t) {
            case BASIC_CAT, TANK_CAT -> Formatting.WHITE;
            case AXE_CAT -> Formatting.GREEN;
            case ARCHER_CAT -> Formatting.AQUA;
            default -> Formatting.GOLD;
        };
    }
}
