package com.example.battlecats;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * One running lane battle for one player.
 *
 * Layout (looking from above, +X is to the right on your screen):
 *   [cat base] ---------------- lane ---------------- [enemy base]
 *                                  ^
 *                              you (side view)
 */
public class Battle {
    // ---- tweak these ----
    public static final int LANE_LEN = 32;          // blocks between the two bases
    public static final int VIEW_DIST = 18;         // how far back you stand from the lane
    public static final int MAX_WALLET = 8;
    public static final int CANNON_TICKS = 20 * 40; // cannon recharge time
    public static final int MAX_CATS = 25;
    public static final int MAX_ENEMIES = 30;
    public static final int CAT_BASE_HP = 300;

    private final MinecraftServer server;
    private final UUID playerId;
    private final ServerWorld world;
    private final int stage;
    public final int slot;
    private final BlockPos origin;

    private boolean over = false;
    private int tick = 0;

    private double money = 100;
    private int wallet = 1;
    private int cannon = 0;
    private int catBaseHp = CAT_BASE_HP;
    private final int enemyBaseMax;
    private int enemyBaseHp;
    private boolean surged = false;
    private double catBaseDamageBuffer = 0; // lets fractional damage add up
    private double enemyBaseDamageBuffer = 0;

    private final ServerBossBar moneyBar = new ServerBossBar(Text.literal("Money"), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
    private final ServerBossBar ownBar = new ServerBossBar(Text.literal("Cat Base"), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
    private final ServerBossBar enemyBar = new ServerBossBar(Text.literal("Enemy Base"), BossBar.Color.RED, BossBar.Style.PROGRESS);

    public Battle(ServerPlayerEntity player, int stage, int slot) {
        this.server = player.getServer();
        this.playerId = player.getUuid();
        this.world = server.getOverworld();
        this.stage = stage;
        this.slot = slot;
        this.origin = new BlockPos(1_000_000 + slot * 128, 200, 1_000_000);
        this.enemyBaseMax = 150 + 150 * stage;
        this.enemyBaseHp = enemyBaseMax;
    }

    // ------------------------------------------------------------ geometry
    public boolean isOver() { return over; }
    public double laneZ() { return origin.getZ() + 0.5; }
    private double catFrontX() { return origin.getX() + 1.0; }
    private double enemyFrontX() { return origin.getX() + LANE_LEN; }
    private int cellX() { return origin.getX() + LANE_LEN / 2; }
    private int cellY() { return origin.getY() + 3; }
    private int cellZ() { return origin.getZ() + VIEW_DIST; }

    private Box arenaBox() {
        return new Box(origin.getX() - 8, origin.getY() - 3, origin.getZ() - 6,
                origin.getX() + LANE_LEN + 8, origin.getY() + 12, origin.getZ() + VIEW_DIST + 6);
    }

    private ServerPlayerEntity player() {
        return server.getPlayerManager().getPlayer(playerId);
    }

    // ------------------------------------------------------------ start
    public void begin(ServerPlayerEntity p) {
        buildArena();
        p.changeGameMode(GameMode.ADVENTURE);
        p.teleport(world, cellX() + 0.5, cellY(), cellZ() + 0.5, 180.0f, 12.0f);

        moneyBar.addPlayer(p);
        ownBar.addPlayer(p);
        enemyBar.addPlayer(p);
        updateUi(p);

        title(p, Text.literal("Stage " + stage).formatted(Formatting.GOLD, Formatting.BOLD),
                Text.literal("Right-click cat cards  |  F: Cat Cannon  |  Shift+F: Upgrade wallet").formatted(Formatting.YELLOW));
        p.sendMessage(Text.literal("Battle start! Deploy cats from your hotbar. Destroy the red base. "
                + "(/battlecats retreat to leave)").formatted(Formatting.GREEN), false);
    }

    private void set(BlockPos.Mutable m, int x, int y, int z, BlockState s) {
        m.set(x, y, z);
        world.setBlockState(m, s, Block.NOTIFY_LISTENERS);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(m, x, y, z, s);
    }

    private void buildArena() {
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int L = LANE_LEN, V = VIEW_DIST;
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState barrier = Blocks.BARRIER.getDefaultState();

        // clear + ground (not grass, so no animals spawn)
        fill(ox - 8, oy - 3, oz - 6, ox + L + 8, oy + 12, oz + V + 6, air);
        fill(ox - 8, oy - 3, oz - 6, ox + L + 8, oy - 1, oz + V + 6, Blocks.POLISHED_ANDESITE.getDefaultState());
        // the lane itself
        fill(ox - 3, oy - 1, oz - 1, ox + L + 3, oy - 1, oz + 1, Blocks.STONE_BRICKS.getDefaultState());
        // sky-blue backdrop behind the lane
        fill(ox - 8, oy, oz - 6, ox + L + 8, oy + 11, oz - 6, Blocks.LIGHT_BLUE_CONCRETE.getDefaultState());

        // invisible walls so nobody leaves the lane
        fill(ox - 3, oy, oz - 2, ox + L + 3, oy + 4, oz - 2, barrier);
        fill(ox - 3, oy, oz + 2, ox + L + 3, oy + 4, oz + 2, barrier);
        fill(ox - 3, oy, oz - 2, ox - 3, oy + 4, oz + 2, barrier);
        fill(ox + L + 3, oy, oz - 2, ox + L + 3, oy + 4, oz + 2, barrier);
        fill(ox - 3, oy + 5, oz - 2, ox + L + 3, oy + 5, oz + 2, barrier);

        // bases
        fill(ox - 2, oy, oz - 1, ox, oy + 4, oz + 1, Blocks.ORANGE_CONCRETE.getDefaultState());
        fill(ox - 2, oy + 5, oz - 1, ox, oy + 5, oz + 1, Blocks.GOLD_BLOCK.getDefaultState());
        fill(ox + L, oy, oz - 1, ox + L + 2, oy + 4, oz + 1, Blocks.RED_CONCRETE.getDefaultState());
        fill(ox + L, oy + 5, oz - 1, ox + L + 2, oy + 5, oz + 1, Blocks.REDSTONE_BLOCK.getDefaultState());

        // your viewing cell: a bedrock pillar with a barrier cage so you can't wander off
        int cx = cellX(), cy = cellY(), cz = cellZ();
        fill(cx, oy, cz, cx, cy - 1, cz, Blocks.BEDROCK.getDefaultState());
        fill(cx - 1, cy, cz, cx - 1, cy + 1, cz, barrier);
        fill(cx + 1, cy, cz, cx + 1, cy + 1, cz, barrier);
        fill(cx, cy, cz + 1, cx, cy + 1, cz + 1, barrier);
        fill(cx, cy, cz - 1, cx, cy, cz - 1, barrier);          // front: feet-height only (no crosshair outline)
        fill(cx, cy + 2, cz, cx, cy + 2, cz, barrier);
        fill(cx, cy + 2, cz - 1, cx, cy + 2, cz - 1, barrier);

        // light blocks so hostile mobs never spawn in the arena
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = ox - 6; x <= ox + L + 6; x += 6)
            for (int z = oz - 4; z <= oz + V + 4; z += 6)
                set(m, x, oy + 7, z, Blocks.LIGHT.getDefaultState());
    }

    // ------------------------------------------------------------ economy
    private int maxMoney() { return 400 + 200 * (wallet - 1); }
    private double incomePerTick() { return (18.0 + 8.0 * (wallet - 1)) / 20.0; }
    private int upgradeCost() { return 80 * wallet; }

    public void upgradeWallet() {
        ServerPlayerEntity p = player();
        if (p == null || over) return;
        if (wallet >= MAX_WALLET) {
            p.sendMessage(Text.literal("Wallet is already maxed!").formatted(Formatting.GRAY), true);
            return;
        }
        int cost = upgradeCost();
        if (money < cost) {
            p.sendMessage(Text.literal("Need " + cost + " money to upgrade the wallet!").formatted(Formatting.RED), true);
            return;
        }
        money -= cost;
        wallet++;
        world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.4f);
        p.sendMessage(Text.literal("Wallet upgraded to Lv." + wallet + "!").formatted(Formatting.GOLD), true);
    }

    public boolean tryDeploy(UnitType t, ServerPlayerEntity p) {
        if (over) return false;
        if (money < t.cost) {
            p.sendMessage(Text.literal("Not enough money! " + t.displayName + " costs " + t.cost).formatted(Formatting.RED), true);
            return false;
        }
        if (countUnits(true) >= MAX_CATS) {
            p.sendMessage(Text.literal("Too many cats on the field!").formatted(Formatting.RED), true);
            return false;
        }
        money -= t.cost;

        BattleUnitEntity u = new BattleUnitEntity(BattleCats.CAT_UNIT, world);
        u.setUnitType(t, 1.0);
        u.battle = this;
        u.refreshPositionAndAngles(origin.getX() + 2.0, origin.getY(),
                laneZ() + (world.random.nextDouble() - 0.5) * 0.8, -90.0f, 0.0f);
        world.spawnEntity(u);
        world.spawnParticles(ParticleTypes.POOF, u.getX(), u.getY() + 0.5, u.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    // ------------------------------------------------------------ cannon
    public void fireCannon() {
        ServerPlayerEntity p = player();
        if (p == null || over) return;
        if (cannon < CANNON_TICKS) {
            p.sendMessage(Text.literal("Cat Cannon charging... " + (cannon * 100 / CANNON_TICKS) + "%").formatted(Formatting.GRAY), true);
            return;
        }
        cannon = 0;
        double ox = origin.getX();
        double reach = ox + LANE_LEN * 0.75;
        float dmg = 30.0f + stage * 5.0f;

        for (BattleUnitEntity u : world.getEntitiesByClass(BattleUnitEntity.class, arenaBox(),
                e -> !e.isCat() && e.isAlive() && e.getX() <= reach)) {
            u.timeUntilRegen = 0;
            u.damage(world.getDamageSources().magic(), dmg);
            u.takeKnockback(1.0, -1.0, 0.0);
        }
        for (double x = ox + 2; x <= reach; x += 1.5) {
            world.spawnParticles(ParticleTypes.EXPLOSION, x, origin.getY() + 0.8, laneZ(), 1, 0.3, 0.3, 0.3, 0);
        }
        world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2.0f, 0.7f);
    }

    // ------------------------------------------------------------ bases
    public boolean inBaseRange(BattleUnitEntity u, double range) {
        if (u.isCat()) return enemyFrontX() - u.getX() <= range + 0.5;
        return u.getX() - catFrontX() <= range + 0.5;
    }

    public void hitBase(boolean byCat, double amount) {
        if (over) return;
        if (byCat) {
            enemyBaseDamageBuffer += amount;
            int whole = (int) enemyBaseDamageBuffer;
            enemyBaseDamageBuffer -= whole;
            enemyBaseHp -= whole;
            world.spawnParticles(ParticleTypes.CRIT, enemyFrontX(), origin.getY() + 1.5, laneZ(), 6, 0.2, 0.6, 0.6, 0.1);
        } else {
            catBaseDamageBuffer += amount;
            int whole = (int) catBaseDamageBuffer;
            catBaseDamageBuffer -= whole;
            catBaseHp -= whole;
            world.spawnParticles(ParticleTypes.CRIT, catFrontX(), origin.getY() + 1.5, laneZ(), 6, 0.2, 0.6, 0.6, 0.1);
        }
    }

    // ------------------------------------------------------------ enemies
    private int countUnits(boolean cats) {
        return world.getEntitiesByClass(BattleUnitEntity.class, arenaBox(), u -> u.isCat() == cats && u.isAlive()).size();
    }

    private void spawnEnemy(UnitType t) {
        if (countUnits(false) >= MAX_ENEMIES) return;
        double mult = 1.0 + 0.2 * (stage - 1);
        BattleUnitEntity u = new BattleUnitEntity(BattleCats.ENEMY_UNIT, world);
        u.setUnitType(t, mult);
        u.battle = this;
        u.refreshPositionAndAngles(origin.getX() + LANE_LEN - 1.0, origin.getY(),
                laneZ() + (world.random.nextDouble() - 0.5) * 0.8, 90.0f, 0.0f);
        world.spawnEntity(u);
    }

    private void enemySchedule() {
        int s = stage;
        if (tick >= 100 && (tick - 100) % Math.max(50, 200 - s * 12) == 0) spawnEnemy(UnitType.DOGE_GRUNT);
        if (tick >= 1200 && (tick - 1200) % Math.max(160, 520 - s * 30) == 0) spawnEnemy(UnitType.BRUTE_DOG);
        if (s >= 3 && tick >= 2400 && (tick - 2400) % Math.max(500, 1800 - s * 80) == 0) spawnEnemy(UnitType.BOSS_DOG);
    }

    // ------------------------------------------------------------ tick
    public void tick() {
        if (over) return;
        ServerPlayerEntity p = player();
        if (p == null || !p.isAlive()) {
            abort();
            return;
        }
        tick++;

        if (tick % 20 == 0 && p.squaredDistanceTo(cellX() + 0.5, cellY(), cellZ() + 0.5) > 4.0) {
            p.teleport(world, cellX() + 0.5, cellY(), cellZ() + 0.5, p.getYaw(), p.getPitch());
        }

        money = Math.min(maxMoney(), money + incomePerTick());
        if (cannon < CANNON_TICKS) cannon++;

        enemySchedule();

        if (!surged && enemyBaseHp <= enemyBaseMax / 2) {
            surged = true;
            for (int i = 0; i < 4; i++) spawnEnemy(UnitType.DOGE_GRUNT);
            p.sendMessage(Text.literal("The enemy base is calling reinforcements!").formatted(Formatting.RED), false);
        }

        if (enemyBaseHp <= 0) { finish(true, false); return; }
        if (catBaseHp <= 0) { finish(false, false); return; }

        if (tick % 4 == 0) updateUi(p);
    }

    private static float pct(double cur, double max) {
        return (float) Math.max(0.0, Math.min(1.0, cur / max));
    }

    private void updateUi(ServerPlayerEntity p) {
        int max = maxMoney();
        moneyBar.setName(Text.literal("Money: " + (int) money + " / " + max + "    Wallet Lv." + wallet
                + (wallet < MAX_WALLET ? "    [Shift+F] Upgrade: " + upgradeCost() : "    (MAX)")).formatted(Formatting.GOLD));
        moneyBar.setPercent(pct(money, max));

        ownBar.setName(Text.literal("Your Base: " + Math.max(0, catBaseHp) + " / " + CAT_BASE_HP).formatted(Formatting.GREEN));
        ownBar.setPercent(pct(catBaseHp, CAT_BASE_HP));

        enemyBar.setName(Text.literal("Enemy Base: " + Math.max(0, enemyBaseHp) + " / " + enemyBaseMax).formatted(Formatting.RED));
        enemyBar.setPercent(pct(enemyBaseHp, enemyBaseMax));

        if (tick % 10 == 0) {
            if (cannon >= CANNON_TICKS) {
                p.sendMessage(Text.literal("Cat Cannon: READY  -  press F!").formatted(Formatting.GREEN, Formatting.BOLD), true);
            } else {
                p.sendMessage(Text.literal("Cat Cannon: " + (cannon * 100 / CANNON_TICKS) + "%").formatted(Formatting.AQUA), true);
            }
        }
    }

    // ------------------------------------------------------------ end
    public void retreat() {
        finish(false, true);
    }

    /** Player went offline: clean up only. They get sent home when they log back in. */
    public void abort() {
        if (over) return;
        over = true;
        cleanup();
    }

    private void cleanup() {
        moneyBar.clearPlayers();
        ownBar.clearPlayers();
        enemyBar.clearPlayers();
        for (BattleUnitEntity u : world.getEntitiesByClass(BattleUnitEntity.class, arenaBox(), e -> true)) {
            u.discard();
        }
    }

    private void finish(boolean win, boolean retreated) {
        if (over) return;
        over = true;
        cleanup();

        ServerPlayerEntity p = player();
        BattleData data = BattleData.get(server);
        if (p == null) return;

        BattleData.ReturnPoint rp = data.getReturn(playerId);
        if (rp != null) restorePlayer(server, p, rp);
        data.clearReturn(playerId);

        if (retreated) {
            p.sendMessage(Text.literal("You retreated.").formatted(Formatting.GRAY), false);
            return;
        }

        if (win) {
            data.onWin(playerId, stage);
            p.getInventory().offerOrDrop(new ItemStack(BattleCats.GACHA_CAPSULE, 1 + stage / 5));
            p.getInventory().offerOrDrop(new ItemStack(Items.EMERALD, 2 + stage));
            title(p, Text.literal("VICTORY!").formatted(Formatting.GOLD, Formatting.BOLD),
                    Text.literal("Stage " + stage + " cleared - reward: Gacha Capsule + emeralds").formatted(Formatting.YELLOW));
            world.playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            title(p, Text.literal("DEFEAT").formatted(Formatting.RED, Formatting.BOLD),
                    Text.literal("Your base was destroyed. Try again!").formatted(Formatting.GRAY));
        }
    }

    private static void title(ServerPlayerEntity p, Text title, Text subtitle) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 60, 15));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        p.networkHandler.sendPacket(new TitleS2CPacket(title));
    }

    /** Teleport a player back to where they were and restore their game mode. */
    public static void restorePlayer(MinecraftServer server, ServerPlayerEntity p, BattleData.ReturnPoint rp) {
        ServerWorld target = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(rp.world)));
        if (target == null) target = server.getOverworld();
        p.changeGameMode(GameMode.byId(rp.mode));
        p.teleport(target, rp.x, rp.y, rp.z, rp.yaw, rp.pitch);
    }
}
