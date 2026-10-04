package com.blockcity.client;

import com.blockcity.BlockCityMod;
import com.blockcity.entity.CarEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class CarRenderer extends EntityRenderer<CarEntity> {
    private static final Identifier TEXTURE = BlockCityMod.id("textures/entity/car.png");
    private final CarModel model;

    public CarRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new CarModel(ctx.getPart(CarModel.LAYER));
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(CarEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0.0f, -1.501f, 0.0f);
        VertexConsumer vc = vertexConsumers.getBuffer(model.getLayer(TEXTURE));
        model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, -1);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(CarEntity entity) {
        return TEXTURE;
    }
}
