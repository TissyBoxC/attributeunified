package io.github.tissyboxc.attributeunified.config;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public interface IModifierRedirector {
    AttributeModifier remap(AttributeModifier in);

    double getDefaultValue(double input);

    IModifierRedirector NOOP = new IModifierRedirector() {
        @Override
        public AttributeModifier remap(AttributeModifier in) {
            return in;
        }

        @Override
        public double getDefaultValue(double input) {
            return input;
        }

    };

    static IModifierRedirector simple(double defaultValue, IModifierRedirector remapper) {
        return new Simple(defaultValue, remapper);
    }

    static IModifierRedirector simple(double defaultValue) {
        return new Simple(defaultValue, IModifierRedirector.NOOP);
    }

    static IModifierRedirector simple(double defaultValue, double scale) {
        return new IModifierRedirector() {
            @Override
            public AttributeModifier remap(AttributeModifier in) {
                return new AttributeModifier(in.id(), in.amount() * scale, in.operation());
            }

            @Override
            public double getDefaultValue(double input) {
                return defaultValue;
            }
        };
    }

    record Simple(double defaultValue, IModifierRedirector remapper) implements IModifierRedirector {
        @Override
        public AttributeModifier remap(AttributeModifier in) {
            return remapper.remap(in);
        }

        @Override
        public double getDefaultValue(double input) {
            return defaultValue;
        }
    }
}
