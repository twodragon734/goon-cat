package com.example.battlecats;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.WolfEntityRenderer;

public class BattleCatsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BattleCats.CAT_UNIT, CatUnitRenderer::new);
        EntityRendererRegistry.register(BattleCats.ENEMY_UNIT, EnemyUnitRenderer::new);
        EntityRendererRegistry.register(BattleCats.DOGE, WolfEntityRenderer::new);
    }
}
