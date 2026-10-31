package com.example.battlecats;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BattleCats implements ModInitializer {
    public static final String MOD_ID = "battlecats";

    public static final EntityType<BattleCatEntity> BATTLE_CAT = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "battle_cat"),
            EntityType.Builder.create(BattleCatEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 0.9f)
                    .build("battle_cat")
    );

    public static final Item CAT_FOOD = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "cat_food"),
            new Item(new Item.Settings().food(
                    new FoodComponent.Builder().nutrition(4).saturationModifier(0.4f).build()))
    );

    public static final Item BATTLE_CAT_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "battle_cat_spawn_egg"),
            new SpawnEggItem(BATTLE_CAT, 0xFFFFFF, 0xFFA500, new Item.Settings())
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(BATTLE_CAT, BattleCatEntity.createCatAttributes());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> entries.add(BATTLE_CAT_SPAWN_EGG));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
                .register(entries -> entries.add(CAT_FOOD));
    }
}
