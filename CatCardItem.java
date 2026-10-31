package com.example.battlecats;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** A cat in card form. Put cards in your hotbar, right-click during a battle to deploy. Cards are never used up. */
public class CatCardItem extends Item {
    public final UnitType type;

    public CatCardItem(UnitType type, Settings settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand != Hand.MAIN_HAND) return TypedActionResult.pass(stack);

        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            Battle b = BattleManager.get(sp);
            if (b == null) {
                sp.sendMessage(Text.literal("Right-click the Doge to start a battle!").formatted(Formatting.YELLOW), true);
                return TypedActionResult.fail(stack);
            }
            if (b.tryDeploy(type, sp)) {
                sp.getItemCooldownManager().set(this, type.deployCooldown);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
