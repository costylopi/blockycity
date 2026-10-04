package com.blockcity.entity;

import com.blockcity.BlockCityConfig;
import com.blockcity.wanted.WantedManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.EnumSet;

/** Politist: tinteste doar jucatori cu wanted > 0; ataca la contact si trage de la distanta. */
public class PoliceEntity extends PathAwareEntity {
    public PoliceEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new ShootGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                (LivingEntity t) -> WantedManager.getStars(t.getUuid()) > 0));
    }

    @Override
    public void tick() {
        super.tick();
        // dispare cand nu mai exista un jucator cautat in apropiere
        if (!getWorld().isClient && age % 40 == 0) {
            PlayerEntity p = getWorld().getClosestPlayer(this, 96);
            if (p == null || WantedManager.getStars(p.getUuid()) == 0) discard();
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return true;
    }

    /** Foc la distanta (hitscan simplificat, cu sanse de ratare). */
    static class ShootGoal extends Goal {
        private final PoliceEntity police;
        private int cooldown;

        ShootGoal(PoliceEntity police) {
            this.police = police;
            setControls(EnumSet.noneOf(Goal.Control.class));
        }

        @Override
        public boolean canStart() {
            LivingEntity t = police.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = police.squaredDistanceTo(t);
            return d > 4.0 && d < 26.0 * 26.0;
        }

        @Override public void start() { cooldown = 20; }

        @Override
        public void tick() {
            LivingEntity t = police.getTarget();
            if (t == null) return;
            police.getLookControl().lookAt(t, 30f, 30f);
            if (--cooldown > 0 || !police.canSee(t)) return;
            cooldown = 30 + police.getRandom().nextInt(15);
            ServerWorld w = (ServerWorld) police.getWorld();
            w.playSound(null, police.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.HOSTILE, 1.0f, 1.5f);
            w.spawnParticles(ParticleTypes.SMOKE, police.getX(), police.getEyeY(), police.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
            if (police.getRandom().nextFloat() < 0.55f) {
                t.damage(w.getDamageSources().mobAttack(police), BlockCityConfig.get().policeDamage);
            }
        }
    }
}
