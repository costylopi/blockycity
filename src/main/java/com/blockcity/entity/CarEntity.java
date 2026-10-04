package com.blockcity.entity;

import com.blockcity.BlockCityConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Masina conducibila. Fizica ruleaza pe clientul soferului (ca la barca vanilla); serverul
 * primeste pozitia prin pachetul vanilla VehicleMove, deci multiplayer functioneaza fara pachete custom.
 */
public class CarEntity extends Entity {
    private double speed;            // blocuri/tick, cu semn, pe directia inainte (doar simulare locala)
    private float health;
    private Vec3d lastPos = Vec3d.ZERO;
    private double measuredSpeed;    // viteza estimata din deplasare (merge si pe server)

    public CarEntity(EntityType<?> type, World world) {
        super(type, world);
        this.health = BlockCityConfig.get().carHealth;
    }

    // ---------- interactiune ----------
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) return ActionResult.PASS;
        if (!getWorld().isClient) {
            return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }

    @Override public boolean canHit() { return !isRemoved(); }
    @Override public boolean isCollidable() { return true; }
    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengerList().size() < 2; }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        // coboara soferul in partea stanga a masinii, daca e loc
        Vec3d left = Vec3d.fromPolar(0, getYaw() - 90f).multiply(1.7);
        Vec3d target = getPos().add(left);
        Box box = passenger.getBoundingBox().offset(target.subtract(passenger.getPos()));
        if (getWorld().isSpaceEmpty(passenger, box)) return target;
        return super.updatePassengerForDismount(passenger);
    }

    // ---------- damage ----------
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || getWorld().isClient || isRemoved()) return false;
        if (source.getAttacker() instanceof PlayerEntity p && p.isCreative() && p.isSneaking()) {
            discard();
            return true;
        }
        health -= amount;
        if (health <= 0) {
            getWorld().createExplosion(this, getX(), getY() + 0.5, getZ(), 1.8f, World.ExplosionSourceType.NONE);
            removeAllPassengers();
            discard();
        }
        return true;
    }

    // ---------- tick / fizica ----------
    @Override
    public void tick() {
        super.tick();
        BlockCityConfig cfg = BlockCityConfig.get();

        if (isLogicalSideForUpdatingMovement()) {
            LivingEntity driver = getControllingPassenger();
            if (driver != null) drive(driver, cfg); else coast();
            Vec3d heading = Vec3d.fromPolar(0, getYaw());
            double vy = getVelocity().y;
            if (isTouchingWater()) { speed *= 0.85; vy = Math.max(vy * 0.8, -0.05) + 0.02; }
            else { vy = (vy - 0.08) * 0.98; }
            setVelocity(heading.x * speed, vy, heading.z * speed);
            move(MovementType.SELF, getVelocity());

            if (horizontalCollision && Math.abs(speed) > 0.03) {
                Vec3d dir = heading.multiply(speed > 0 ? 0.9 : -0.9);
                boolean canStep = isOnGround() && getWorld().isSpaceEmpty(this, getBoundingBox().offset(dir.x, 1.05, dir.z));
                if (canStep) setVelocity(getVelocity().x, 0.45, getVelocity().z);   // urca un bloc
                else speed *= 0.25;                                                 // lovitura in zid
            }
        } else {
            setVelocity(Vec3d.ZERO);
        }

        measuredSpeed = getPos().distanceTo(lastPos);
        lastPos = getPos();

        if (!getWorld().isClient && cfg.carRunOverDamage && measuredSpeed > 0.35) runOverNearby(cfg);
    }

    private void drive(LivingEntity driver, BlockCityConfig cfg) {
        float fwd = driver.forwardSpeed;
        float side = driver.sidewaysSpeed;
        double max = cfg.carMaxSpeed;
        if (fwd > 0) {
            speed += cfg.carAcceleration * fwd;
        } else if (fwd < 0) {
            speed -= (speed > 0.02) ? cfg.carBrake : cfg.carAcceleration * 0.6;
        } else {
            speed *= isOnGround() ? 0.97 : 0.995;
            if (Math.abs(speed) < 0.004) speed = 0;
        }
        speed *= 0.995; // rezistenta aerului -> viteza maxima naturala
        speed = MathHelper.clamp(speed, -max * cfg.carReverseFraction, max);
        double grip = MathHelper.clamp(Math.abs(speed) / 0.25, 0.0, 1.0) * Math.signum(speed);
        setYaw(getYaw() - (float) (side * cfg.carTurnRate * grip));
    }

    private void coast() {
        speed *= 0.9;
        if (Math.abs(speed) < 0.004) speed = 0;
    }

    private void runOverNearby(BlockCityConfig cfg) {
        List<LivingEntity> hit = getWorld().getEntitiesByClass(LivingEntity.class,
                getBoundingBox().expand(0.15), e -> e != this && !hasPassenger(e) && e.isAlive() && e.getVehicle() != this);
        LivingEntity driver = getControllingPassenger();
        for (LivingEntity e : hit) {
            float dmg = (float) (measuredSpeed * 18.0);
            DamageSource src = (driver instanceof ServerPlayerEntity p)
                    ? ((ServerWorld) getWorld()).getDamageSources().playerAttack(p)
                    : getWorld().getDamageSources().generic();
            e.damage(src, dmg);
            Vec3d push = Vec3d.fromPolar(0, getYaw()).multiply(0.8);
            e.addVelocity(push.x, 0.3, push.z);
            e.velocityModified = true;
        }
    }

    // ---------- date pentru HUD ----------
    public double getSpeedBlocksPerTick() {
        return isLogicalSideForUpdatingMovement() && getControllingPassenger() != null ? speed : measuredSpeed;
    }
    public float getCarHealth() { return health; }

    // ---------- persistenta ----------
    @Override protected void initDataTracker(DataTracker.Builder builder) {}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { if (nbt.contains("CarHealth")) health = nbt.getFloat("CarHealth"); }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { nbt.putFloat("CarHealth", health); }
}
