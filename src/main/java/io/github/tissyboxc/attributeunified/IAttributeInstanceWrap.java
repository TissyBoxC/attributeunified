package io.github.tissyboxc.attributeunified;

import io.github.tissyboxc.attributeunified.config.ModifierRedirector;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public interface IAttributeInstanceWrap {
    void attributeUnification$setTarget(AttributeInstance target, ModifierRedirector.RedirectorEntry entry);
}
