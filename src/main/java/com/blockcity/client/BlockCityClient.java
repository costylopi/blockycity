package com.blockcity.client;

import com.blockcity.BlockCityConfig;
import com.blockcity.BlockCityMod;
import com.blockcity.ModEntities;
import com.blockcity.entity.CarEntity;
import com.blockcity.entity.PedestrianEntity;
import com.blockcity.entity.PoliceEntity;
import com.blockcity.wanted.WantedSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.Identifier;

public class BlockCityClient implements ClientModInitializer {
    private static final Identifier[] PED_TEXTURES = {
            BlockCityMod.id("textures/entity/pedestrian_0.png"), BlockCityMod.id("textures/entity/pedestrian_1.png"),
            BlockCityMod.id("textures/entity/pedestrian_2.png"), BlockCityMod.id("textures/entity/pedestrian_3.png")
    };
    private static final Identifier POLICE_TEXTURE = BlockCityMod.id("textures/entity/police.png");
    private static boolean wasInCar;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(CarModel.LAYER, CarModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.CAR, CarRenderer::new);
        EntityRendererRegistry.register(ModEntities.PEDESTRIAN,
                ctx -> new HumanRenderer<PedestrianEntity>(ctx, e -> PED_TEXTURES[Math.floorMod(e.getUuid().hashCode(), PED_TEXTURES.length)]));
        EntityRendererRegistry.register(ModEntities.POLICE,
                ctx -> new HumanRenderer<PoliceEntity>(ctx, e -> POLICE_TEXTURE));

        ClientPlayNetworking.registerGlobalReceiver(WantedSyncPayload.ID,
                (payload, context) -> ClientState.stars = payload.stars());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.stars = 0);

        // camera la persoana a treia la intrarea in lume si la urcarea in masina
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (BlockCityConfig.get().thirdPersonOnJoin) client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean inCar = client.player != null && client.player.getVehicle() instanceof CarEntity;
            if (inCar && !wasInCar) client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            wasInCar = inCar;
        });

        HudOverlay.init();
    }
}
