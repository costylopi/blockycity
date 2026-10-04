package com.blockcity.item;

import com.blockcity.BlockCityConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.projectile.ProjectileUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/** Pistol hitscan, fara munitie (configurabil: damage, raza, cooldown). */
public class PistolItem extends Item {
    public PistolItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        BlockCityConfig cfg = BlockCityConfig.get();
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        user.getItemCooldownManager().set(this, cfg.pistolCooldownTicks);
        if (world instanceof ServerWorld sw) shoot(sw, user, cfg);
        return TypedActionResult.success(stack, world.isClient());
    }

    private static void shoot(ServerWorld w, PlayerEntity user, BlockCityConfig cfg) {
        double range = cfg.pistolRange;
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0f);
        Vec3d end = eye.add(look.multiply(range));

        BlockHitResult blockHit = w.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
        Vec3d stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();

        Box box = user.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        Entity vehicle = user.getVehicle();
        EntityHitResult hit = ProjectileUtil.raycast(user, eye, stop, box,
                e -> !e.isSpectator() && e.canHit() && e != vehicle, range * range);
        if (hit != null) {
            stop = hit.getPos();
            hit.getEntity().damage(w.getDamageSources().playerAttack(user), cfg.pistolDamage);
        }

        w.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
                SoundCategory.PLAYERS, 1.2f, 1.6f);
        double dist = eye.distanceTo(stop);
        for (double d = 1.5; d < dist; d += 2.0) {
            Vec3d p = eye.add(look.multiply(d));
            w.spawnParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (hit != null || blockHit.getType() != HitResult.Type.MISS) {
            w.spawnParticles(ParticleTypes.CRIT, stop.x, stop.y, stop.z, 4, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
