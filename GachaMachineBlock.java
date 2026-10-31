package com.example.battlecats;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Gacha Machine. Also the Gacha Master villager's job-site block.
 * Right-click with a Gacha Capsule (or 6 emeralds) to pull a random cat card.
 */
public class GachaMachineBlock extends Block {
    public GachaMachineBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ItemStack held = player.getMainHandStack();
        boolean capsule = held.isOf(BattleCats.GACHA_CAPSULE);
        boolean emeralds = held.isOf(Items.EMERALD) && held.getCount() >= 6;

        if (!capsule && !emeralds) {
            player.sendMessage(Text.literal("Hold a Gacha Capsule (or 6 emeralds) and right-click! "
                    + "Capsules are sold by the Gacha Master villager.").formatted(Formatting.YELLOW), true);
            return ActionResult.SUCCESS;
        }

        held.decrement(capsule ? 1 : 6);

        UnitType pulled = Gacha.roll(world.random);
        ItemStack card = new ItemStack(BattleCats.CARDS.get(pulled));
        player.getInventory().offerOrDrop(card);

        player.sendMessage(Text.literal("You pulled: ").formatted(Formatting.GRAY)
                .append(Text.literal(pulled.displayName + " [" + Gacha.rarity(pulled) + "]")
                        .formatted(Gacha.color(pulled), Formatting.BOLD)), false);

        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    pulled == UnitType.TITAN_CAT ? 60 : 15, 0.3, 0.3, 0.3, 0.2);
        }
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.8f, 1.2f);
        return ActionResult.SUCCESS;
    }
}
