package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "onChangedBlock",
            at = @At("TAIL")
    )
    protected void onChangedBlock(BlockPos pPos, CallbackInfo ci) {
        Entity entity = this.getLevel().getEntity(this.getId());

        if (!this.getLevel().isClientSide() && entity instanceof Unit unit &&
            SurvivalServerEvents.ENEMY_OWNER_NAMES.contains(unit.getOwnerName())) {
            FrostWalkerEnchantment.onEntityMoved((LivingEntity) entity, this.level, pPos, 1);
        }
    }
}
