package com.example.battlecats;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/** Enemies use the biped model with zombie-family skins (placeholder until you draw your own). */
public class EnemyUnitRenderer extends MobEntityRenderer<BattleUnitEntity, BipedEntityModel<BattleUnitEntity>> {
    public EnemyUnitRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public Identifier getTexture(BattleUnitEntity entity) {
        return entity.getUnitType().texture;
    }
}
