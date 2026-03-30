package io.github.tissyboxc.attributeunified.mixin;

import io.github.tissyboxc.attributeunified.IAttributeInstanceWrap;
import io.github.tissyboxc.attributeunified.IOriginValue;
import io.github.tissyboxc.attributeunified.config.ModifierRedirector;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = AttributeInstance.class, priority = 800)
public abstract class AUAttributeInstanceMixin implements IAttributeInstanceWrap, IOriginValue {
    @Shadow
    @Final
    private Holder<Attribute> attribute;
    @Shadow
    private boolean dirty;
    @Shadow
    private double cachedValue;

    @Shadow
    protected abstract double calculateValue();

    @Shadow
    public abstract double getBaseValue();

    @Shadow
    private double baseValue;
    @Unique
    @Nullable
    private ModifierRedirector.RedirectorEntry wrapped;

    @Unique
    @Nullable
    AttributeInstance target;

    @Override
    public void attributeUnification$setTarget(AttributeInstance target, ModifierRedirector.RedirectorEntry attr) {
        this.wrapped = attr;
        this.target = target;
    }


    @Override
    public double au$getOriginValue() {
        return this.baseValue;
    }

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void au$getValue(CallbackInfoReturnable<Double> cir) {
        if (wrapped != null) {
            cir.setReturnValue(wrapped.func().getDefaultValue(this.getBaseValue()));
        }
    }

    @Inject(method = "getModifier", at = @At("HEAD"), cancellable = true)
    private void au$getModifier(ResourceLocation id, CallbackInfoReturnable<AttributeModifier> cir) {
        if (wrapped != null && target != null) {
            cir.setReturnValue(target.getModifier(id));
        }
    }

    @Inject(method = "hasModifier", at = @At("HEAD"), cancellable = true)
    private void au$hasModifier(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if (wrapped != null && target != null) {
            cir.setReturnValue(target.hasModifier(id));
        }
    }

    @Inject(method = "addOrUpdateTransientModifier", at = @At("HEAD"), cancellable = true)
    private void au$addOrUpdateTransientModifier(AttributeModifier modifier, CallbackInfo ci) {
        if (wrapped != null && target != null) {
            target.addOrUpdateTransientModifier(wrapped.wrap(modifier));
            ci.cancel();
        }
    }

    @Inject(method = "addTransientModifier", at = @At("HEAD"), cancellable = true)
    private void au$addTransientModifier(AttributeModifier modifier, CallbackInfo ci) {
        if (wrapped != null && target != null) {
            target.addTransientModifier(wrapped.wrap(modifier));
            ci.cancel();
        }
    }

    @Inject(method = "addOrReplacePermanentModifier", at = @At("HEAD"), cancellable = true)
    private void au$addOrReplacePermanentModifier(AttributeModifier modifier, CallbackInfo ci) {
        if (wrapped != null && target != null) {
            target.addOrReplacePermanentModifier(wrapped.wrap(modifier));
            ci.cancel();
        }
    }

    @Inject(method = "addPermanentModifier", at = @At("HEAD"), cancellable = true)
    private void au$addPermanentModifier(AttributeModifier modifier, CallbackInfo ci) {
        if (wrapped != null && target != null) {
            target.addPermanentModifier(wrapped.wrap(modifier));
            ci.cancel();
        }
    }

    @Inject(method = "removeModifier(Lnet/minecraft/resources/ResourceLocation;)Z", at = @At("HEAD"), cancellable = true)
    private void au$removeModifier(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if (wrapped != null && target != null) {
            cir.setReturnValue(target.removeModifier(id));
        }
    }

    @Inject(method = "removeModifiers", at = @At("HEAD"), cancellable = true)
    private void au$removeModifiers(CallbackInfo ci) {
        if (wrapped != null && target != null) {
            target.removeModifiers();
            ci.cancel();
        }
    }

    @Inject(method = "replaceFrom", at = @At("HEAD"), cancellable = true)
    private void au$replaceFrom(AttributeInstance instance, CallbackInfo ci) {
        if (wrapped != null && target != null) {
            target.replaceFrom(instance);
            ci.cancel();
        }
    }
}
