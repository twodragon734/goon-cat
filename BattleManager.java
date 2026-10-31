package com.example.battlecats;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BattleManager {
    private static final Map<UUID, Battle> ACTIVE = new HashMap<>();

    private BattleManager() {}

    public static Battle get(ServerPlayerEntity p) {
        return ACTIVE.get(p.getUuid());
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BattleManager::tick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Battle b = ACTIVE.remove(handler.player.getUuid());
            if (b != null) b.abort();
        });

        // If the server stopped mid-battle, send the player home when they come back.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (ACTIVE.containsKey(p.getUuid())) return;
            BattleData data = BattleData.get(server);
            BattleData.ReturnPoint rp = data.getReturn(p.getUuid());
            if (rp != null) {
                Battle.restorePlayer(server, p, rp);
                data.clearReturn(p.getUuid());
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID, Battle>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Battle b = it.next().getValue();
            b.tick();
            if (b.isOver()) it.remove();
        }
    }

    /** Hand-swap key (F). Returns true if we used it (so vanilla does not swap items). */
    public static boolean handleSwapKey(ServerPlayerEntity player) {
        Battle b = ACTIVE.get(player.getUuid());
        if (b == null) return false;
        if (player.isSneaking()) b.upgradeWallet();
        else b.fireCannon();
        return true;
    }

    public static void retreat(ServerPlayerEntity player) {
        Battle b = ACTIVE.remove(player.getUuid());
        if (b == null) {
            player.sendMessage(Text.literal("You are not in a battle.").formatted(Formatting.GRAY), false);
            return;
        }
        b.retreat();
    }

    private static boolean hasDeck(ServerPlayerEntity p) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.getItem() instanceof CatCardItem) return true;
        }
        return false;
    }

    private static int freeSlot() {
        Set<Integer> used = new HashSet<>();
        for (Battle b : ACTIVE.values()) used.add(b.slot);
        int s = 0;
        while (used.contains(s)) s++;
        return s;
    }

    public static void onDogeInteract(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (ACTIVE.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("You're already in a battle! (/battlecats retreat)").formatted(Formatting.RED), true);
            return;
        }

        BattleData data = BattleData.get(server);
        BattleData.Progress prog = data.progress(player.getUuid());

        if (!prog.starter) {
            prog.starter = true;
            data.markDirty();
            player.getInventory().offerOrDrop(new ItemStack(BattleCats.CARDS.get(UnitType.BASIC_CAT)));
            player.sendMessage(Text.literal("Doge: wow. such cat. Here is a free Basic Cat card. "
                    + "Put cat cards in your hotbar!").formatted(Formatting.GOLD), false);
        }

        if (player.isSneaking()) {
            prog.selected = prog.selected % prog.unlocked + 1;
            data.markDirty();
            player.sendMessage(Text.literal("Doge: Stage " + prog.selected + " selected (unlocked up to "
                    + prog.unlocked + "). Right-click me to start!").formatted(Formatting.GOLD), false);
            return;
        }

        if (!hasDeck(player)) {
            player.sendMessage(Text.literal("Doge: put at least one Cat Card in your hotbar first!").formatted(Formatting.YELLOW), false);
            return;
        }

        int stage = Math.max(1, Math.min(prog.selected, prog.unlocked));

        BattleData.ReturnPoint rp = new BattleData.ReturnPoint();
        rp.world = player.getServerWorld().getRegistryKey().getValue().toString();
        rp.x = player.getX();
        rp.y = player.getY();
        rp.z = player.getZ();
        rp.yaw = player.getYaw();
        rp.pitch = player.getPitch();
        rp.mode = player.interactionManager.getGameMode().getId();
        data.setReturn(player.getUuid(), rp);

        Battle b = new Battle(player, stage, freeSlot());
        ACTIVE.put(player.getUuid(), b);
        try {
            b.begin(player);
        } catch (Exception e) {
            BattleCats.LOGGER.error("Failed to start battle", e);
            ACTIVE.remove(player.getUuid());
            b.abort();
            Battle.restorePlayer(server, player, rp);
            data.clearReturn(player.getUuid());
            player.sendMessage(Text.literal("Battle failed to start (see server log).").formatted(Formatting.RED), false);
        }
    }
}
