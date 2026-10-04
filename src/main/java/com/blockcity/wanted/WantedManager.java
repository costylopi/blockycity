package com.blockcity.wanted;

import com.blockcity.BlockCityConfig;
import com.blockcity.ModEntities;
import com.blockcity.ModItems;
import com.blockcity.entity.PedestrianEntity;
import com.blockcity.entity.PoliceEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Heat -> stele (0..5), spawn de politie, plus spawn de pietoni pe trotuare (beton gri deschis). */
public final class WantedManager {
    private static final int[] STAR_THRESHOLDS = {20, 60, 120, 200, 300}; // heat necesar pentru 1..5 stele
    private static final Map<UUID, Integer> HEAT = new HashMap<>();
    private static final Map<UUID, Integer> LAST_CRIME = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    public static int getStars(UUID id) {
        return starsFromHeat(HEAT.getOrDefault(id, 0));
    }

    public static int starsFromHeat(int heat) {
        int s = 0;
        for (int i = 0; i < STAR_THRESHOLDS.length; i++) if (heat >= STAR_THRESHOLDS[i]) s = i + 1;
        return s;
    }

    public static void addHeat(ServerPlayerEntity p, int amount) {
        if (!BlockCityConfig.get().wantedEnabled) return;
        HEAT.merge(p.getUuid(), amount, Integer::sum);
        LAST_CRIME.put(p.getUuid(), p.getServer().getTicks());
        sync(p);
    }

    public static void setStars(ServerPlayerEntity p, int stars) {
        stars = Math.max(0, Math.min(5, stars));
        HEAT.put(p.getUuid(), stars == 0 ? 0 : STAR_THRESHOLDS[stars - 1]);
        LAST_CRIME.put(p.getUuid(), p.getServer().getTicks());
        sync(p);
    }

    private static void sync(ServerPlayerEntity p) {
        int stars = getStars(p.getUuid());
        if (LAST_SENT.getOrDefault(p.getUuid(), -1) == stars) return;
        LAST_SENT.put(p.getUuid(), stars);
        ServerPlayNetworking.send(p, new WantedSyncPayload(stars));
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity p) {
                if (entity instanceof PedestrianEntity) addHeat(p, 10);
                else if (entity instanceof PoliceEntity) addHeat(p, 35);
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity p) {
                if (entity instanceof PedestrianEntity) addHeat(p, 25);
                else if (entity instanceof PoliceEntity) addHeat(p, 60);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            HEAT.remove(newP.getUuid());
            LAST_SENT.remove(newP.getUuid());
            sync(newP);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SENT.remove(handler.getPlayer().getUuid());
            sync(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            HEAT.remove(id); LAST_CRIME.remove(id); LAST_SENT.remove(id);
        });
        ServerTickEvents.END_SERVER_TICK.register(WantedManager::tick);
    }

    private static void tick(MinecraftServer server) {
        int t = server.getTicks();
        if (t % 20 != 0) return;
        BlockCityConfig cfg = BlockCityConfig.get();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            UUID id = p.getUuid();
            int heat = HEAT.getOrDefault(id, 0);
            if (heat > 0 && t - LAST_CRIME.getOrDefault(id, t) > cfg.heatDecayDelayTicks) {
                HEAT.put(id, Math.max(0, heat - 3));
            }
            sync(p);
            int stars = getStars(id);
            if (cfg.wantedEnabled && stars > 0 && t % Math.max(20, cfg.policeSpawnIntervalTicks) == 0) spawnPolice(p, stars, cfg);
            if (cfg.pedestriansEnabled && t % 100 == 0) spawnPedestrians(p, cfg);
        }
    }

    private static void spawnPolice(ServerPlayerEntity p, int stars, BlockCityConfig cfg) {
        ServerWorld w = p.getServerWorld();
        int have = w.getEntitiesByClass(PoliceEntity.class, p.getBoundingBox().expand(48), e -> true).size();
        if (have >= Math.min(cfg.maxPolice, stars * 2)) return;
        Random r = w.getRandom();
        for (int i = 0; i < 12; i++) {
            double ang = r.nextDouble() * Math.PI * 2, d = 20 + r.nextDouble() * 12;
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * d), z = (int) Math.floor(p.getZ() + Math.sin(ang) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!w.getBlockState(pos.down()).isSolidBlock(w, pos.down())) continue;
            PoliceEntity cop = ModEntities.POLICE.create(w);
            if (cop == null) return;
            cop.refreshPositionAndAngles(x + 0.5, y, z + 0.5, r.nextFloat() * 360f, 0f);
            cop.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PISTOL));
            cop.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0f);
            cop.setTarget(p);
            w.spawnEntity(cop);
            return;
        }
    }

    private static void spawnPedestrians(ServerPlayerEntity p, BlockCityConfig cfg) {
        ServerWorld w = p.getServerWorld();
        int have = w.getEntitiesByClass(PedestrianEntity.class, p.getBoundingBox().expand(48), e -> true).size();
        if (have >= cfg.maxPedestriansNearPlayer) return;
        Random r = w.getRandom();
        for (int i = 0; i < 10; i++) {
            double ang = r.nextDouble() * Math.PI * 2, d = 14 + r.nextDouble() * 26;
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * d), z = (int) Math.floor(p.getZ() + Math.sin(ang) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!w.getBlockState(pos.down()).isOf(Blocks.LIGHT_GRAY_CONCRETE)) continue; // doar trotuare
            PedestrianEntity ped = ModEntities.PEDESTRIAN.create(w);
            if (ped == null) return;
            ped.refreshPositionAndAngles(x + 0.5, y, z + 0.5, r.nextFloat() * 360f, 0f);
            w.spawnEntity(ped);
            return;
        }
    }

    private WantedManager() {}
}
