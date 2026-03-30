package io.github.tissyboxc.attributeunified;

import com.mojang.logging.LogUtils;
import io.github.tissyboxc.attributeunified.config.ModifierRedirector;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.targets.CommonLaunchHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.io.Charsets;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mod(attributeunified.MODID)
public class attributeunified {
    public static final String MODID = "attributeunified";
    public static final Logger LOGGER = LogUtils.getLogger();

    public attributeunified(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        modEventBus.addListener(attributeunified::onSetup);
    }

    private static void onSetup(FMLCommonSetupEvent event) {
        StringBuilder sb = new StringBuilder();
        AttributeSupplier attributeSupplier = DefaultAttributes.getSupplier(EntityType.PLAYER);
        Stream<Holder.Reference<Attribute>> holders = BuiltInRegistries.ATTRIBUTE.holders().filter(attributeSupplier::hasAttribute);

        holders.sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.key().location().toString(), o2.key().location().toString())).forEach(attributeReference -> {
            var v = attributeReference.value();
            if (v instanceof RangedAttribute rangedAttribute) {
                IOriginValue instance = (IOriginValue) attributeSupplier.createInstance((a) -> {
                }, attributeReference);
                double defaultValue = instance != null ? instance.au$getOriginValue() : rangedAttribute.getDefaultValue();
                double minValue = rangedAttribute.getMinValue();
                double maxValue = rangedAttribute.getMaxValue();

                sb.append("id ").append(attributeReference.key().location())
                        .append(" 默认值 ").append(formatNumber(defaultValue))
                        .append(" 范围 [").append(formatNumber(minValue)).append(",").append(formatNumber(maxValue)).append("]");
            } else {
                sb.append("id ").append(attributeReference.key().location())
                        .append(" 默认值 ").append(formatNumber(v.getDefaultValue()));
            }
            sb.append(" 本地化 ").append(Component.translatable(v.getDescriptionId()).getString());
            sb.append(" 类名 ");
            sb.append(v.getClass().getName());
            sb.append("\n");
        });

        Path p = FMLPaths.GAMEDIR.get().resolve("属性列表.txt");
        try {
            FileWriter fw = new FileWriter(p.toFile(), StandardCharsets.UTF_8, false);
            fw.append(sb.toString());
            fw.flush();
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatNumber(double value) {
        if (value == 0) {
            return "0";
        }

        double absValue = Math.abs(value);
        int digits = (int) Math.log10(absValue) + 1;

        if (digits <= 4 && absValue >= 0.0001 && absValue < 10000) {
            if (value == (long) value) {
                return String.valueOf((long) value);
            } else {
                return String.valueOf(value);
            }
        } else {
            return String.format("%.2e", value);
        }
    }
}
