package com.example.battlecats;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * One lane unit. Used for both teams: the entity TYPE decides the team
 * (CAT_UNIT marches +X, ENEMY_UNIT marches -X).
 * There is no pathfinding: units just walk down the lane and fight whatever is in range.
 */
public class BattleUnitEntity extends PathAwareEntity {
    private static final TrackedData<Integer> UNIT =
            DataTracker.registerData(BattleUnitEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public Battle battle;
    private double damageAmount = 1.0;
    private int attackCooldown = 0;

    public BattleUnitEntity(EntityType<? extends BattleUnitEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.06)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(UNIT, 0);
    }

    public boolean isCat() {
        return this.getType() == BattleCats.CAT_UNIT;
    }

    public UnitType getUnitType() {
        int i = this.dataTracker.get(UNIT);
        UnitType[] all = UnitType.values();
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }

    public void setUnitType(UnitType t, double statMultiplier) {
        this.dataTracker.set(UNIT, t.ordinal());
        this.damageAmount = t.damage * statMultiplier;

        EntityAttributeInstance hp = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (hp != null) hp.setBaseValue(t.hp * statMultiplier);
        EntityAttributeInstance spd = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(t.speed);
        EntityAttributeInstance scale = this.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (scale != null) scale.setBaseValue(t.scale);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOf(DamageTypes.GENERIC_KILL)) return super.damage(source, amount);
        if (source.getAttacker() instanceof BattleUnitEntity a && a.isCat() != this.isCat()) {
            return super.damage(source, amount);
        }
        if (source.isOf(DamageTypes.MAGIC)) return super.damage(source, amount); // Cat Cannon
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;
        if (battle == null || battle.isOver()) {
            this.discard();
            return;
        }

        ServerWorld w = (ServerWorld) this.getWorld();
        UnitType t = this.getUnitType();
        boolean cat = this.isCat();

        float yaw = cat ? -90.0f : 90.0f; // east / west
        this.setYaw(yaw);
        this.setBodyYaw(yaw);
        this.setHeadYaw(yaw);

        if (attackCooldown > 0) attackCooldown--;

        // 1) fight the nearest enemy unit in range
        BattleUnitEntity target = findTarget(t.range);
        if (target != null) {
            this.getMoveControl().moveTo(this.getX(), this.getY(), this.getZ(), 0.0);
            if (attackCooldown <= 0) {
                attackCooldown = t.attackCooldown;
                this.swingHand(Hand.MAIN_HAND);
                if (t.ranged) beam(w, target.getX(), target.getY() + 0.6, target.getZ());
                target.timeUntilRegen = 0; // every hit lands, like the real game
                target.damage(w.getDamageSources().mobAttack(this), (float) damageAmount);
            }
            return;
        }

        // 2) hit the enemy base if we reached it
        if (battle.inBaseRange(this, t.range)) {
            this.getMoveControl().moveTo(this.getX(), this.getY(), this.getZ(), 0.0);
            if (attackCooldown <= 0) {
                attackCooldown = t.attackCooldown;
                this.swingHand(Hand.MAIN_HAND);
                battle.hitBase(cat, damageAmount);
            }
            return;
        }

        // 3) otherwise march down the lane
        this.getMoveControl().moveTo(this.getX() + (cat ? 3.0 : -3.0), this.getY(), battle.laneZ(), 1.0);
    }

    private BattleUnitEntity findTarget(double range) {
        boolean cat = this.isCat();
        Box box = this.getBoundingBox().expand(range + 1.0, 2.0, 2.0);
        BattleUnitEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BattleUnitEntity o : this.getWorld().getEntitiesByClass(BattleUnitEntity.class, box,
                u -> u != this && u.isAlive() && u.isCat() != cat)) {
            double ahead = cat ? o.getX() - this.getX() : this.getX() - o.getX();
            if (ahead < -0.3 || ahead > range) continue;
            if (ahead < bestDist) {
                bestDist = ahead;
                best = o;
            }
        }
        return best;
    }

    private void beam(ServerWorld w, double tx, double ty, double tz) {
        double x1 = this.getX(), y1 = this.getY() + 0.6, z1 = this.getZ();
        double dx = tx - x1, dy = ty - y1, dz = tz - z1;
        int steps = (int) Math.max(2, Math.abs(dx) * 2);
        for (int i = 1; i <= steps; i++) {
            double f = i / (double) steps;
            w.spawnParticles(ParticleTypes.CRIT, x1 + dx * f, y1 + dy * f, z1 + dz * f, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isCat() ? SoundEvents.ENTITY_CAT_HURT : SoundEvents.ENTITY_ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isCat() ? SoundEvents.ENTITY_CAT_DEATH : SoundEvents.ENTITY_ZOMBIE_DEATH;
    }
}
