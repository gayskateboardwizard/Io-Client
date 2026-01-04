package io.client.mixin;

import io.client.event.LivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    @Shadow
    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    public void swing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        ItemStack stack = ((LivingEntity) (Object) this).getItemInHand(hand);
        if (!stack.isEmpty()) {
        }
    }

    @Inject(method = "startUsingItem(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"))
    public void startUsingItem(InteractionHand hand, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack stack = entity.getItemInHand(hand);
        if (!stack.isEmpty() && !entity.isUsingItem()) {
            LivingEntityEvents.START_USE_ITEM.invoker().onStartUseItem(entity, stack);
        }
    }

    @Inject(method = "heal(F)V", at = @At("HEAD"), cancellable = true)
    public void heal(float amount, CallbackInfo ci) {
        if (!LivingEntityEvents.ENTITY_HEAL.invoker().onEntityHeal((LivingEntity) (Object) this, amount))
            ci.cancel();
    }

    @Inject(method = "applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("HEAD"), cancellable = true)
    public void applyItemBlocking(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfoReturnable<Float> cir) {
        if (!LivingEntityEvents.ENTITY_BLOCK.invoker().onEntityBlock((LivingEntity) (Object) this, damageSource, (double) f))
            cir.cancel();
    }

    @Inject(method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    public void dropExperience(ServerLevel serverLevel, Entity entity, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerMemoryTime > 0 && self.shouldDropExperience() && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
            if (!LivingEntityEvents.ENTITY_DROP_XP.invoker().onEntityDropXp(self, self.getLastHurtByPlayer(), (double) self.getExperienceReward(serverLevel, entity)))
                ci.cancel();
        }
    }

    @Inject(method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), cancellable = true)
    public void causeFallDamage(double d, float f, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!LivingEntityEvents.ENTITY_FALL.invoker().onEntityFall((LivingEntity) (Object) this, d, (double) f))
            cir.setReturnValue(false);
    }

    @Inject(method = "onItemPickup(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At("HEAD"))
    public void onItemPickup(ItemEntity itemEntity, CallbackInfo ci) {
        LivingEntityEvents.ENTITY_PICKUP_ITEM.invoker().onEntityPickupItem(itemEntity.getOwner(), itemEntity.getItem());
    }

    @Inject(method = "jumpFromGround()V", at = @At("TAIL"))
    public void jumpFromGround(CallbackInfo ci) {
        LivingEntityEvents.ENTITY_JUMP.invoker().onEntityJump((LivingEntity) (Object) this);
    }
}