package com.example.battlecats;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.util.Identifier;

public class BattleCatRenderer extends MobEntityRenderer<BattleCatEntity, PigEntityModel<BattleCatEntity>> {
    // Placeholder: uses the pig model + texture. Swap for your own later.
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/pig/pig.png");

    public BattleCatRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PigEntityModel<>(ctx.getPart(EntityModelLayers.PIG)), 0.5f);
    }

    @Override
    public Identifier getTexture(BattleCatEntity entity) {
        return TEXTURE;
    }
}
