package com.example.battlecats;

import net.minecraft.util.Identifier;

/** All unit stats live here. Tweak numbers freely. */
public enum UnitType {
    //        id            name           cat    cost hp    dmg   range speed   atkCd depCd ranged scale texture
    BASIC_CAT("basic_cat",  "Basic Cat",   true,  50,  20,   3,    1.5,  0.06f,  20,   40,   false, 1.0f, "cat/tabby"),
    TANK_CAT("tank_cat",    "Tank Cat",    true,  100, 90,   1.5,  1.5,  0.05f,  30,   100,  false, 1.2f, "cat/british_shorthair"),
    AXE_CAT("axe_cat",      "Axe Cat",     true,  150, 30,   8,    1.6,  0.07f,  24,   140,  false, 1.0f, "cat/red"),
    ARCHER_CAT("archer_cat","Archer Cat",  true,  200, 25,   5,    8.0,  0.055f, 40,   180,  true,  1.0f, "cat/siamese"),
    TITAN_CAT("titan_cat",  "Titan Cat",   true,  400, 220,  14,   2.5,  0.05f,  36,   400,  false, 1.8f, "cat/all_black"),

    DOGE_GRUNT("doge_grunt","Doge Grunt",  false, 0,   20,   2,    1.4,  0.05f,  24,   0,    false, 1.0f, "zombie/zombie"),
    BRUTE_DOG("brute_dog",  "Brute Dog",   false, 0,   70,   5,    1.5,  0.045f, 30,   0,    false, 1.2f, "zombie/husk"),
    BOSS_DOG("boss_dog",    "Boss Dog",    false, 0,   300,  12,   2.5,  0.04f,  40,   0,    false, 1.7f, "zombie/drowned");

    public final String id;
    public final String displayName;
    public final boolean isCat;
    public final int cost;
    public final double hp;
    public final double damage;
    public final double range;
    public final float speed;
    public final int attackCooldown;
    public final int deployCooldown;
    public final boolean ranged;
    public final float scale;
    public final Identifier texture;

    UnitType(String id, String displayName, boolean isCat, int cost, double hp, double damage,
             double range, float speed, int attackCooldown, int deployCooldown,
             boolean ranged, float scale, String tex) {
        this.id = id;
        this.displayName = displayName;
        this.isCat = isCat;
        this.cost = cost;
        this.hp = hp;
        this.damage = damage;
        this.range = range;
        this.speed = speed;
        this.attackCooldown = attackCooldown;
        this.deployCooldown = deployCooldown;
        this.ranged = ranged;
        this.scale = scale;
        this.texture = Identifier.ofVanilla("textures/entity/" + tex + ".png");
    }
}
