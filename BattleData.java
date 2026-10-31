package com.example.battlecats;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saved with the world: each player's stage progress + where to send them back after a battle. */
public class BattleData extends PersistentState {
    public static class Progress {
        public int unlocked = 1;
        public int selected = 1;
        public boolean starter = false;
    }

    public static class ReturnPoint {
        public String world = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;
        public int mode;
    }

    private final Map<UUID, Progress> progress = new HashMap<>();
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();

    private static final PersistentState.Type<BattleData> TYPE =
            new PersistentState.Type<>(BattleData::new, BattleData::fromNbt, null);

    public static BattleData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "battlecats_data");
    }

    public Progress progress(UUID id) {
        return progress.computeIfAbsent(id, k -> new Progress());
    }

    public ReturnPoint getReturn(UUID id) {
        return returns.get(id);
    }

    public void setReturn(UUID id, ReturnPoint rp) {
        returns.put(id, rp);
        markDirty();
    }

    public void clearReturn(UUID id) {
        if (returns.remove(id) != null) markDirty();
    }

    public void onWin(UUID id, int stage) {
        Progress p = progress(id);
        if (stage >= p.unlocked && stage < 99) {
            p.unlocked = stage + 1;
            p.selected = p.unlocked;
        }
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList plist = new NbtList();
        for (Map.Entry<UUID, Progress> e : progress.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putUuid("id", e.getKey());
            c.putInt("unlocked", e.getValue().unlocked);
            c.putInt("selected", e.getValue().selected);
            c.putBoolean("starter", e.getValue().starter);
            plist.add(c);
        }
        nbt.put("progress", plist);

        NbtList rlist = new NbtList();
        for (Map.Entry<UUID, ReturnPoint> e : returns.entrySet()) {
            NbtCompound c = new NbtCompound();
            ReturnPoint r = e.getValue();
            c.putUuid("id", e.getKey());
            c.putString("world", r.world);
            c.putDouble("x", r.x);
            c.putDouble("y", r.y);
            c.putDouble("z", r.z);
            c.putFloat("yaw", r.yaw);
            c.putFloat("pitch", r.pitch);
            c.putInt("mode", r.mode);
            rlist.add(c);
        }
        nbt.put("returns", rlist);
        return nbt;
    }

    public static BattleData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        BattleData d = new BattleData();
        NbtList plist = nbt.getList("progress", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < plist.size(); i++) {
            NbtCompound c = plist.getCompound(i);
            Progress p = new Progress();
            p.unlocked = Math.max(1, c.getInt("unlocked"));
            p.selected = Math.max(1, c.getInt("selected"));
            p.starter = c.getBoolean("starter");
            d.progress.put(c.getUuid("id"), p);
        }
        NbtList rlist = nbt.getList("returns", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < rlist.size(); i++) {
            NbtCompound c = rlist.getCompound(i);
            ReturnPoint r = new ReturnPoint();
            r.world = c.getString("world");
            r.x = c.getDouble("x");
            r.y = c.getDouble("y");
            r.z = c.getDouble("z");
            r.yaw = c.getFloat("yaw");
            r.pitch = c.getFloat("pitch");
            r.mode = c.getInt("mode");
            d.returns.put(c.getUuid("id"), r);
        }
        return d;
    }
}
