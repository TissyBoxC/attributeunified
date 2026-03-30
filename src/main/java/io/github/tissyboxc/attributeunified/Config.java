package io.github.tissyboxc.attributeunified;

import io.github.tissyboxc.attributeunified.config.IModifierRedirector;
import io.github.tissyboxc.attributeunified.config.ModifierRedirector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@EventBusSubscriber(modid = attributeunified.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTES = BUILDER.comment("A list of attributes to modify.")
            .comment("From,To,value,scale")
            .comment("From and To is attribute")
            .comment("value is default value when From redirect to To,It's used to prevent From attribute applying effect.")
            .comment("scale in the scale when From value redirect to To value")
            .defineListAllowEmpty("attributes", List.of(
                    // 暴击几率统一
                    "l2damagetracker:crit_rate,apothic_attributes:crit_chance,0",
                    "lovely_sparkle_pieces:critical_hit_chance,apothic_attributes:crit_chance,0",
                    "critical_strike:chance,apothic_attributes:crit_chance,100,0.01",
                    // 暴击伤害统一
                    "l2damagetracker:crit_damage,apothic_attributes:crit_damage,0.5",
                    "critical_strike:damage,apothic_attributes:crit_damage,100,0.01",
                    // 火焰伤害统一
                    "l2damagetracker:fire_damage,apothic_attributes:fire_damage,0,1",
                    // 弓箭伤害统一
                    "l2damagetracker:bow_strength,apothic_attributes:arrow_damage,1,1"
            ), Config::validateAttributeName);

    private static boolean validateAttributeName(Object o) {
        return o instanceof String s && s.split(",").length >= 3;
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    private static void onConfigLoad(ModConfigEvent.Loading event) {
        for (String s : ATTRIBUTES.get()) {
            var list = s.split(",");
            if (list.length < 3) continue;
            var from = ResourceLocation.tryParse(list[0]);
            var target = ResourceLocation.tryParse(list[1]);
            double def;
            double scale = 1;
            try {
                def = Double.parseDouble(list[2]);
            } catch (Exception ignore) {
                continue;
            }
            try {
                if (list.length > 4)
                    scale = Double.parseDouble(list[3]);
            } catch (Exception ignore) {
                continue;
            }
            if (from == null || target == null) continue;
            if (BuiltInRegistries.ATTRIBUTE.containsKey(from) && BuiltInRegistries.ATTRIBUTE.containsKey(target)) {
                var fa = BuiltInRegistries.ATTRIBUTE.getHolder(from).orElseThrow();
                var fb = BuiltInRegistries.ATTRIBUTE.getHolder(target).orElseThrow();
                if (fa.value() instanceof RangedAttribute rangedAttribute) {
                    def = Mth.clamp(def, rangedAttribute.getMinValue(), rangedAttribute.getMaxValue());
                }
                ModifierRedirector.add(fa, fb, IModifierRedirector.simple(def, scale));
            }
        }
        ModifierRedirector.finishLoad();
    }
}
