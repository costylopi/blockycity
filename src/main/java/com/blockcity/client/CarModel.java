package com.blockcity.client;

import com.blockcity.BlockCityMod;
import com.blockcity.entity.CarEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;

/** Model procedural al masinii (cuboizi). Fata masinii = -Z in spatiul modelului; textura 256x128. */
public class CarModel extends EntityModel<CarEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(BlockCityMod.id("car"), "main");
    private final ModelPart root;

    public CarModel(ModelPart root) {
        this.root = root;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();
        r.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-14f, 12f, -18f, 28f, 6f, 36f), ModelTransform.NONE);
        r.addChild("cabin", ModelPartBuilder.create().uv(0, 52).cuboid(-11f, 6f, -2f, 22f, 6f, 20f), ModelTransform.NONE);
        ModelPartBuilder wheel = ModelPartBuilder.create().uv(150, 0).cuboid(-2f, 0f, -4f, 4f, 8f, 8f);
        r.addChild("wheel_fl", wheel, ModelTransform.pivot(-14f, 16f, -11f));
        r.addChild("wheel_fr", wheel, ModelTransform.pivot(14f, 16f, -11f));
        r.addChild("wheel_bl", wheel, ModelTransform.pivot(-14f, 16f, 11f));
        r.addChild("wheel_br", wheel, ModelTransform.pivot(14f, 16f, 11f));
        return TexturedModelData.of(data, 256, 128);
    }

    @Override
    public void setAngles(CarEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        root.render(matrices, vertices, light, overlay, color);
    }
}
