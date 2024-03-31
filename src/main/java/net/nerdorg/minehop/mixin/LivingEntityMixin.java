// MADE BY hatninja ON GITHUB

package net.nerdorg.minehop.mixin;

import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.nerdorg.minehop.Minehop;
import net.nerdorg.minehop.block.ModBlocks;
import net.nerdorg.minehop.config.MinehopConfig;
import net.nerdorg.minehop.config.ConfigWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow private float movementSpeed;
    @Shadow public float sidewaysSpeed;
    @Shadow public float forwardSpeed;
    @Shadow private int jumpingCooldown;
    @Shadow protected boolean jumping;

    @Shadow protected abstract Vec3d applyClimbingSpeed(Vec3d velocity);
    @Shadow protected abstract float getJumpVelocity();
    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);
    @Shadow public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);
    @Shadow public abstract boolean isFallFlying();
    @Shadow public abstract boolean isClimbing();

    @Shadow public abstract float getYaw(float tickDelta);

    @Shadow public abstract void updateLimbs(boolean flutter);

    @Shadow private BlockPos lastBlockPos;
    @Shadow public float prevHeadYaw;

    @Shadow public abstract float getHeadYaw();

    @Shadow public abstract EntityGroup getGroup();

    private boolean wasOnGround;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void isPushable(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travel(Vec3d movementInput, CallbackInfo ci) {
        MinehopConfig config;
        if (Minehop.override_config) {
            config = new MinehopConfig();
            config.sv_friction = Minehop.o_sv_friction;
            config.sv_accelerate = Minehop.o_sv_accelerate;
            config.sv_airaccelerate = Minehop.o_sv_airaccelerate;
            config.sv_maxairspeed = Minehop.o_sv_maxairspeed;
            config.speed_mul = Minehop.o_speed_mul;
            config.sv_gravity = Minehop.o_sv_gravity;
        }
        else {
            config = ConfigWrapper.config;
        }

        boolean standingOnBooster = this.getWorld().getBlockState(this.getBlockPos()).isOf(ModBlocks.BOOSTER_BLOCK);

        //Enable for Players only
        if (this.getType() != EntityType.PLAYER) { return; }

        if (!this.canMoveVoluntarily() && !this.isLogicalSideForUpdatingMovement()) { return; }

        //Cancel override if not in plain walking state.
        if (this.isTouchingWater() || this.isInLava() || this.isFallFlying()) { return; }

        //I don't have a better clue how to do this atm.
        LivingEntity self = (LivingEntity) this.getWorld().getEntityById(this.getId());

        //Disable on creative flying.
        if (this.getType() == EntityType.PLAYER && isFlying((PlayerEntity) self)) { return; }

        //Reverse multiplication done by the function that calls this one.
        this.sidewaysSpeed /= 0.98F;
        this.forwardSpeed /= 0.98F;
        double sI = movementInput.x / 0.98F;
        double fI = movementInput.z / 0.98F;
        double uI = movementInput.y;

        //Have no jump cooldown, why not?
        this.jumpingCooldown = 0;


        //Get Slipperiness and Movement speed.
        BlockPos blockPos = this.getVelocityAffectingPos();
        float slipperiness = this.getWorld().getBlockState(blockPos).getBlock().getSlipperiness();
        float friction = 1-(slipperiness*slipperiness);

        //
        //Apply Friction
        //
        boolean fullGrounded = this.wasOnGround && this.isOnGround(); //Allows for no friction 1-frame upon landing.
        if (fullGrounded && !standingOnBooster) {
            Vec3d velFin = this.getVelocity();
            Vec3d horFin = new Vec3d(velFin.x,0.0F,velFin.z);
            float speed = (float) horFin.length();
            if (speed > 0.001F) {
                float drop = 0.0F;

                drop += (speed * config.sv_friction * friction);

                float newspeed = Math.max(speed - drop, 0.0F);
                newspeed /= speed;
                this.setVelocity(
                        horFin.x * newspeed,
                        velFin.y,
                        horFin.z * newspeed
                );
            }
        }
        this.wasOnGround = this.isOnGround();

        //
        // Accelerate
        //
        float yawDifference = MathHelper.wrapDegrees(this.getHeadYaw() - this.prevHeadYaw);
        if (yawDifference < 0) {
            yawDifference = yawDifference * -1;
        }
        if (!this.isOnGround()) {
            sI = sI * yawDifference;
        }
        if (sI != 0.0F || fI != 0.0F) {
            Vec3d moveDir = movementInputToVelocity(new Vec3d(sI, 0.0F, fI), 1.0F, this.getYaw());
            Vec3d accelVec = this.getVelocity();

            double projVel = new Vec3d(accelVec.x, 0.0F, accelVec.z).dotProduct(moveDir);
            double accelVel = (this.isOnGround() ? config.sv_accelerate : (config.sv_airaccelerate / (this.horizontalSpeed * 10000)));
            float maxVel = (float) (this.isOnGround() ? this.movementSpeed * config.speed_mul : config.sv_maxairspeed);

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3d accelDir = moveDir.multiply(Math.max(accelVel, 0.0F));

            Vec3d newVelocity = accelVec.add(accelDir);

            this.setVelocity(newVelocity);
        }

        this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        this.move(MovementType.SELF, this.getVelocity());

        //
        //Ladder Logic
        //
        Vec3d preVel = this.getVelocity();
        if ((this.horizontalCollision || this.jumping) && this.isClimbing()) {
            preVel = new Vec3d(preVel.x * 0.7D, 0.2D, preVel.z * 0.7D);
        }

        //
        //Apply Gravity (If not in Water)
        //
        double yVel = preVel.y;
        double gravity = config.sv_gravity;
        if (preVel.y <= 0.0D && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = 0.01D;
            this.fallDistance = 0.0F;
        }
        if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
            yVel += (0.05D * (this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - preVel.y) * 0.2D;
            this.fallDistance = 0.0F;
        } else if (this.getWorld().isClient && !this.getWorld().isChunkLoaded(blockPos)) {
            yVel = 0.0D;
        } else if (!this.hasNoGravity()) {
            yVel -= gravity;
        }
        this.setVelocity(preVel.x,yVel,preVel.z);

        //
        //Update limbs.
        //
        this.updateLimbs(self instanceof Flutterer);

        //Override original method.
        ci.cancel();
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    void jump(CallbackInfo ci) {
        boolean standingOnBooster = this.getWorld().getBlockState(this.getBlockPos()).isOf(ModBlocks.BOOSTER_BLOCK);

        if (!standingOnBooster) {
            Vec3d vecFin = this.getVelocity();
            double yVel = this.getJumpVelocity();
            if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                yVel += 0.1F * (this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
            }

            this.setVelocity(vecFin.x, yVel, vecFin.z);
            this.velocityDirty = true;
        }
        this.setOnGround(false);

        ci.cancel();
    }

    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        Vec3d vec3d = (d > 1.0D ? movementInput.normalize() : movementInput).multiply(speed);
        float f = MathHelper.sin(yaw * 0.017453292F);
        float g = MathHelper.cos(yaw * 0.017453292F);
        return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
    }

    private static boolean isFlying(PlayerEntity player) {
        return player != null && player.getAbilities().flying;
    }
}
