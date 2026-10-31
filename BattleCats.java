package com.example.battlecats;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class BattleCats implements ModInitializer {
    public static final String MOD_ID = "battlecats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    // ---------------- cat cards ----------------
    public static final Map<UnitType, CatCardItem> CARDS = new EnumMap<>(UnitType.class);

    static {
        for (UnitType t : UnitType.values()) {
            if (t.isCat) {
                CARDS.put(t, Registry.register(Registries.ITEM, id(t.id + "_card"),
                        new CatCardItem(t, new Item.Settings().maxCount(16))));
            }
        }
    }

    // ---------------- items / blocks ----------------
    public static final Item GACHA_CAPSULE = Registry.register(Registries.ITEM, id("gacha_capsule"),
            new Item(new Item.Settings()));

    public static final Block GACHA_MACHINE = Registry.register(Registries.BLOCK, id("gacha_machine"),
            new GachaMachineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));

    public static final Item GACHA_MACHINE_ITEM = Registry.register(Registries.ITEM, id("gacha_machine"),
            new BlockItem(GACHA_MACHINE, new Item.Settings()));

    // ---------------- entities ----------------
    public static final EntityType<BattleUnitEntity> CAT_UNIT = Registry.register(Registries.ENTITY_TYPE, id("cat_unit"),
            EntityType.Builder.<BattleUnitEntity>create(BattleUnitEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 0.7f).build("cat_unit"));

    public static final EntityType<BattleUnitEntity> ENEMY_UNIT = Registry.register(Registries.ENTITY_TYPE, id("enemy_unit"),
            EntityType.Builder.<BattleUnitEntity>create(BattleUnitEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.95f).build("enemy_unit"));

    public static final EntityType<DogeEntity> DOGE = Registry.register(Registries.ENTITY_TYPE, id("doge"),
            EntityType.Builder.<DogeEntity>create(DogeEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 0.85f).build("doge"));

    public static final Item DOGE_SPAWN_EGG = Registry.register(Registries.ITEM, id("doge_spawn_egg"),
            new SpawnEggItem(DOGE, 0xC8A165, 0xFFFFFF, new Item.Settings()));

    // ---------------- villager job ----------------
    public static final RegistryKey<PointOfInterestType> GACHA_POI_KEY =
            RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, id("gacha_poi"));

    // ---------------- creative tab ----------------
    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(CARDS.get(UnitType.BASIC_CAT)))
                    .displayName(Text.translatable("itemGroup.battlecats"))
                    .entries((ctx, entries) -> {
                        for (CatCardItem c : CARDS.values()) entries.add(c);
                        entries.add(GACHA_CAPSULE);
                        entries.add(GACHA_MACHINE_ITEM);
                        entries.add(DOGE_SPAWN_EGG);
                    })
                    .build());

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(CAT_UNIT, BattleUnitEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ENEMY_UNIT, BattleUnitEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DOGE, WolfEntity.createWolfAttributes());

        // Gacha Master: a villager profession whose workstation is the Gacha Machine
        PointOfInterestHelper.register(id("gacha_poi"), 1, 1, GACHA_MACHINE);
        VillagerProfession gachaMaster = Registry.register(Registries.VILLAGER_PROFESSION, id("gacha_master"),
                new VillagerProfession("gacha_master",
                        entry -> entry.matchesKey(GACHA_POI_KEY),
                        entry -> entry.matchesKey(GACHA_POI_KEY),
                        ImmutableSet.of(), ImmutableSet.of(),
                        SoundEvents.ENTITY_VILLAGER_WORK_LIBRARIAN));

        TradeOfferHelper.registerVillagerOffers(gachaMaster, 1, factories -> {
            factories.add((entity, random) -> new TradeOffer(
                    new TradedItem(Items.EMERALD, 6), new ItemStack(GACHA_CAPSULE, 1), 16, 2, 0.05f));
            factories.add((entity, random) -> new TradeOffer(
                    new TradedItem(Items.EMERALD, 16), new ItemStack(GACHA_CAPSULE, 3), 8, 4, 0.05f));
            factories.add((entity, random) -> new TradeOffer(
                    new TradedItem(Items.DIAMOND, 1), new ItemStack(GACHA_CAPSULE, 3), 8, 4, 0.05f));
        });

        BattleManager.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("battlecats")
                        .then(CommandManager.literal("retreat").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            BattleManager.retreat(p);
                            return 1;
                        }))
                        .then(CommandManager.literal("stage")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(1, 99))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                            int n = IntegerArgumentType.getInteger(ctx, "n");
                                            BattleData data = BattleData.get(ctx.getSource().getServer());
                                            BattleData.Progress prog = data.progress(p.getUuid());
                                            prog.unlocked = Math.max(prog.unlocked, n);
                                            prog.selected = n;
                                            data.markDirty();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Stage set to " + n), false);
                                            return 1;
                                        })))));
    }
}
