package com.example.battlecats;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.util.Identifier;

/** Cats use the vanilla ocelot/cat model with different vanilla cat skins per unit type. */
public class CatUnitRenderer extends MobEntityRenderer<BattleUnitEntity, OcelotEntityModel<BattleUnitEntity>> {
    public CatUnitRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new OcelotEntityModel<>(ctx.getPart(EntityModelLayers.OCELOT)), 0.4f);
    }

    @Override
    public Identifier getTexture(BattleUnitEntity entity) {
        return entity.getUnitType().texture;
    }
}
