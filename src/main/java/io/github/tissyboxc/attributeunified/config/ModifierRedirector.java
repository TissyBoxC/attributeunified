package io.github.tissyboxc.attributeunified.config;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.*;
import java.util.function.Consumer;

public class ModifierRedirector {
    public static void finishLoad() {
        // 初始化完成后，优化所有重定向配置
        optimizeRedirects();
    }

    public static RedirectorEntry get(Holder<Attribute> attribute) {
        var rd = RD_CONFIG.getOrDefault(attribute, new ArrayList<>());
        if (!rd.isEmpty()) return rd.getFirst();
        return RedirectorEntry.noop(attribute);
    }

    public record RedirectorEntry(Holder<Attribute> target, IModifierRedirector func) {
        private static final Map<Holder<Attribute>, RedirectorEntry> noop = new HashMap<>();

        public static RedirectorEntry noop(Holder<Attribute> from) {
            return noop.computeIfAbsent(from, (a) -> new RedirectorEntry(a, IModifierRedirector.NOOP));
        }

        public AttributeModifier wrap(AttributeModifier modifier) {
            return func.remap(modifier);
        }
    }

    private static final Map<Holder<Attribute>, List<RedirectorEntry>> RD_CONFIG = new HashMap<>();


    /**
     * 优化所有重定向配置：
     * 1. 检测并移除循环重定向（如 A→B 且 B→A，只保留其中一个）
     * 2. 合并链式重定向（如 C→B→A 优化为 C→A 和 B→A）
     */
    private static void optimizeRedirects() {
        // 第一步：构建图并检测循环
        Set<String> processedPairs = new HashSet<>();
        List<RedirectToRemove> toRemove = new ArrayList<>();

        for (Map.Entry<Holder<Attribute>, List<RedirectorEntry>> entry : RD_CONFIG.entrySet()) {
            Holder<Attribute> from = entry.getKey();
            String fromId = from.getKey().location().toString();

            for (RedirectorEntry redirectEntry : entry.getValue()) {
                Holder<Attribute> to = redirectEntry.target;
                String toId = to.getKey().location().toString();

                // 检查是否存在反向重定向（循环）
                String reversePair = getPairKey(toId, fromId);
                if (processedPairs.contains(reversePair)) {
                    // 发现循环，标记当前这个待删除
                    toRemove.add(new RedirectToRemove(from, redirectEntry));
                } else {
                    processedPairs.add(getPairKey(fromId, toId));
                }
            }
        }

        // 移除循环的重定向
        for (RedirectToRemove r : toRemove) {
            RD_CONFIG.get(r.from).remove(r.entry);
        }

        // 第二步：优化链式重定向
        optimizeChains();
    }

    public static boolean has(Holder<Attribute> from) {
        return RD_CONFIG.containsKey(from) && !RD_CONFIG.get(from).isEmpty();
    }

    private static String getPairKey(String from, String to) {
        return from + "->" + to;
    }

    /**
     * 优化链式重定向：将 C→B→A 转换为 C→A（直接指向最终目标）
     * 但保留复合的 remap 函数（链式调用所有中间转换）
     */
    private static void optimizeChains() {
        // 构建映射表便于查找
        Map<Holder<Attribute>, Holder<Attribute>> directTarget = new HashMap<>();
        Map<Holder<Attribute>, List<IModifierRedirector>> redirectorChains = new HashMap<>();

        for (Map.Entry<Holder<Attribute>, List<RedirectorEntry>> entry : RD_CONFIG.entrySet()) {
            Holder<Attribute> from = entry.getKey();
            if (entry.getValue().isEmpty()) continue;

            // 只处理单个重定向的情况
            RedirectorEntry firstEntry = entry.getValue().getFirst();
            Holder<Attribute> to = firstEntry.target;
            directTarget.put(from, to);
            redirectorChains.computeIfAbsent(from, k -> new ArrayList<>()).add(firstEntry.func);
        }

        // 对每个起点，追踪到最终目标
        for (Holder<Attribute> start : directTarget.keySet().stream().toList()) {
            List<Holder<Attribute>> path = new ArrayList<>();
            List<IModifierRedirector> chain = new ArrayList<>();
            Set<Holder<Attribute>> visited = new HashSet<>();

            Holder<Attribute> current = start;
            while (directTarget.containsKey(current) && !visited.contains(current)) {
                visited.add(current);
                path.add(current);
                Holder<Attribute> next = directTarget.get(current);

                // 获取当前段的转换函数
                if (RD_CONFIG.containsKey(current) && !RD_CONFIG.get(current).isEmpty()) {
                    chain.add(RD_CONFIG.get(current).getFirst().func);
                }

                current = next;
            }

            // 如果路径长度 > 1，说明存在链式重定向
            if (path.size() > 1) {

                // 为路径上的每个节点（除了最后一个）添加直接指向最终目标的边
                for (int i = 0; i < path.size() - 1; i++) {
                    Holder<Attribute> node = path.get(i);

                    // 创建复合转换函数（按顺序应用所有后续转换）
                    List<IModifierRedirector> subChain = new ArrayList<>();
                    for (int j = i; j < chain.size(); j++) {
                        subChain.add(chain.get(j));
                    }

                    if (!subChain.isEmpty()) {
                        IModifierRedirector composedFunc = composeRedirectors(subChain, start);

                        // 更新或添加直接指向最终目标的重定向
                        RD_CONFIG.computeIfAbsent(node, k -> new ArrayList<>());
                        // 替换第一个条目为直接指向最终目标的条目
                        List<RedirectorEntry> entries = RD_CONFIG.get(node);
                        if (!entries.isEmpty()) {
                            entries.set(0, new RedirectorEntry(current, composedFunc));
                        } else {
                            entries.add(new RedirectorEntry(current, composedFunc));
                        }
                    }
                }
            }
        }

    }

    /**
     * 复合多个重定向函数，并在最后验证 defaultValue 是否合法
     * 如果目标 attribute 是 RangedAttribute，则判定是否在范围内，不在则取最近的值
     */
    private static IModifierRedirector composeRedirectors(List<IModifierRedirector> redirectors, Holder<Attribute> targetAttribute) {
        if (redirectors.isEmpty()) {
            return IModifierRedirector.NOOP;
        }

        // 获取第一个函数的默认值
        double defaultValue = redirectors.getFirst().getDefaultValue(0);

        // 检查目标 attribute 是否有范围限制
        Attribute attr = targetAttribute.value();
        final double finalDefaultValue = getFinalDefaultValue(attr, defaultValue);

        return new IModifierRedirector() {
            @Override
            public AttributeModifier remap(AttributeModifier in) {
                AttributeModifier result = in;
                // 按顺序应用所有转换
                for (IModifierRedirector func : redirectors) {
                    result = func.remap(in);
                }
                return result;
            }

            @Override
            public double getDefaultValue(double input) {
                return finalDefaultValue;
            }
        };
    }

    private static double getFinalDefaultValue(Attribute attr, double defaultValue) {
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;

        // 通过类型检查获取 RangedAttribute 的范围
        if (attr instanceof RangedAttribute rangedAttr) {
            min = rangedAttr.getMinValue();
            max = rangedAttr.getMaxValue();
        }

        // 如果在范围内，认为正确；否则取最接近的值
        double finalDefaultValue;
        if (defaultValue < min) {
            finalDefaultValue = min;
        } else finalDefaultValue = Math.min(defaultValue, max);

        return finalDefaultValue;
    }

    /**
     * 复合多个重定向函数（不验证默认值，用于非链式场景）
     */
    private static IModifierRedirector composeRedirectors(List<IModifierRedirector> redirectors) {
        return composeRedirectors(redirectors, null);
    }

    private record RedirectToRemove(Holder<Attribute> from, RedirectorEntry entry) {
    }

    public static void add(Holder<Attribute> from, Holder<Attribute> to, IModifierRedirector func) {
        RD_CONFIG.computeIfAbsent(from, k -> new ArrayList<>()).add(new RedirectorEntry(to, func));
    }

    public static void add(String from, Holder<Attribute> to, IModifierRedirector func) {
        load(from).ifPresent((Consumer<Holder<Attribute>>) attributeHolder -> add(attributeHolder, to, func));
    }

    public static void add(String from, String to, IModifierRedirector func) {
        load(from)
                .ifPresent(f -> {
                    load(to).ifPresent(t -> {
                        add(f, t, func);
                    });
                });
    }

    public static void add(Holder<Attribute> from, String to, IModifierRedirector func) {
        load(to).ifPresent((Consumer<Holder<Attribute>>) attributeHolder -> add(from, attributeHolder, func));
    }

    private static Optional<? extends Holder<Attribute>> load(String id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id));
    }


}
