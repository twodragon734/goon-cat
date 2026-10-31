package com.example.battlecats;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** The Doge NPC. Right-click = start your current stage. Sneak + right-click = pick a stage. */
public class DogeEntity extends WolfEntity {

    public DogeEntity(EntityType<? extends WolfEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setPersistent();
        this.setCustomName(Text.literal("Doge").formatted(Formatting.GOLD));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void initGoals() {
        // No wolf combat AI: Doge just stands there and stares at you.
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity sp) {
            BattleManager.onDogeInteract(sp);
        }
        return ActionResult.success(this.getWorld().isClient);
    }
}
