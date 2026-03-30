package io.github.tissyboxc.attributeunified.mixin;

import io.github.tissyboxc.attributeunified.IAttributeInstanceWrap;
import io.github.tissyboxc.attributeunified.config.ModifierRedirector;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = AttributeMap.class, priority = 800)
public abstract class AttributeMapMixin {

    @Shadow
    @Final
    private Map<Holder<Attribute>, AttributeInstance> attributes;
    @Shadow
    @Final
    private AttributeSupplier supplier;

    @Shadow
    protected abstract void onAttributeModified(AttributeInstance instance);


    @Redirect(method = "lambda$getInstance$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier;createInstance(Ljava/util/function/Consumer;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;"))
    private AttributeInstance attributeUnification$injectInstance(AttributeSupplier instance, Consumer<AttributeInstance> attributeInstanceConsumer, Holder<Attribute> attr) {
        var v = instance.createInstance(attributeInstanceConsumer, attr);
        if (v != null) {

            if (ModifierRedirector.has(attr)) {
                var rd = ModifierRedirector.get(attr);
                if (rd != null) {
                    var target = attributeUnification$getInstance(rd.target());
                    if(target!=null){
                        ((IAttributeInstanceWrap) v).attributeUnification$setTarget(target,rd);
                    }
                }
            }


        }
        return v;
    }

    @Nullable
    @Unique
    public AttributeInstance attributeUnification$getInstance(Holder<Attribute> attribute) {
        return this.attributes.computeIfAbsent(attribute, this::attributeUnification$innerCreate);
    }

    @Unique
    private AttributeInstance attributeUnification$innerCreate(Holder<Attribute> a) {
        return this.supplier.createInstance(this::onAttributeModified, a);
    }
}
