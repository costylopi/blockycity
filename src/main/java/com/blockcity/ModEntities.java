package com.blockcity;

import com.blockcity.entity.CarEntity;
import com.blockcity.entity.PedestrianEntity;
import com.blockcity.entity.PoliceEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<CarEntity> CAR = EntityType.Builder
            .create(CarEntity::new, SpawnGroup.MISC)
            .dimensions(2.0f, 1.3f)
            .maxTrackingRange(10)
            .trackingTickInterval(1)
            .passengerAttachments(0.2f)
            .build("car");

    public static final EntityType<PedestrianEntity> PEDESTRIAN = EntityType.Builder
            .create(PedestrianEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6f, 1.8f)
            .maxTrackingRange(10)
            .build("pedestrian");

    public static final EntityType<PoliceEntity> POLICE = EntityType.Builder
            .create(PoliceEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6f, 1.8f)
            .maxTrackingRange(10)
            .build("police");

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, BlockCityMod.id("car"), CAR);
        Registry.register(Registries.ENTITY_TYPE, BlockCityMod.id("pedestrian"), PEDESTRIAN);
        Registry.register(Registries.ENTITY_TYPE, BlockCityMod.id("police"), POLICE);
        FabricDefaultAttributeRegistry.register(PEDESTRIAN, PedestrianEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(POLICE, PoliceEntity.createAttributes());
    }

    private ModEntities() {}
}
