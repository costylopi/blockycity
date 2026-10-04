package com.blockcity.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/** Randare umanoida (model de jucator, skin-uri originale) pentru pietoni si politisti. */
public class HumanRenderer<T extends MobEntity> extends BipedEntityRenderer<T, PlayerEntityModel<T>> {
    private final Function<T, Identifier> textures;

    public HumanRenderer(EntityRendererFactory.Context ctx, Function<T, Identifier> textures) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.textures = textures;
    }

    @Override
    public void render(T mob, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        this.model.rightArmPose = mob.getMainHandStack().isEmpty()
                ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        super.render(mob, yaw, tickDelta, matrices, vcp, light);
    }

    @Override
    public Identifier getTexture(T entity) {
        return textures.apply(entity);
    }
}
