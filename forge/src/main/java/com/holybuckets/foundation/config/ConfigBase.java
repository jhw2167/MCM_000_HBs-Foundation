package com.holybuckets.foundation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public abstract class ConfigBase {

    public ForgeConfigSpec specification;

    protected int depth;
    protected List<CValue<?, ?>> allValues;
    protected List<ConfigBase> children;

    public void registerAll(final ForgeConfigSpec.Builder builder) {
        if(allValues == null)
            return;
        for (CValue<?, ?> cValue : allValues)
            cValue.register(builder);
    }

    public void onLoad() {
        if (children != null)
            children.forEach(ConfigBase::onLoad);
    }

    public void onReload() {
        if (children != null)
            children.forEach(ConfigBase::onReload);
    }

    public abstract String getName();

    @FunctionalInterface
    protected static interface IValueProvider<V, T extends ConfigValue<V>>
            extends Function<ForgeConfigSpec.Builder, T> {
    }

    protected ConfigBool b(boolean current, String name, String... comment) {
        return new ConfigBool(name, current, comment);
    }

    protected ConfigFloat f(float current, float min, float max, String name, String... comment) {
        return new ConfigFloat(name, current, min, max, comment);
    }

    protected ConfigFloat f(float current, float min, String name, String... comment) {
        return f(current, min, Float.MAX_VALUE, name, comment);
    }

    protected ConfigInt i(int current, int min, int max, String name, String... comment) {
        return new ConfigInt(name, current, min, max, comment);
    }

    protected ConfigInt i(int current, int min, String name, String... comment) {
        return i(current, min, Integer.MAX_VALUE, name, comment);
    }

    protected ConfigInt i(int current, String name, String... comment) {
        return i(current, Integer.MIN_VALUE, Integer.MAX_VALUE, name, comment);
    }

    protected ConfigString s(String current, String name, String... comment) {
        return new ConfigString(current, name, current, comment);
    }

    protected <T extends Enum<T>> ConfigEnum<T> e(T defaultValue, String name, String... comment) {
        return new ConfigEnum<>(name, defaultValue, comment);
    }

    protected ConfigList list(List<String> def, String name, String... comment) {
        return new ConfigList(name, def, comment);
    }

    protected ConfigGroup group(int depth, String name, String... comment) {
        return new ConfigGroup(name, depth, comment);
    }

    protected <T extends ConfigBase> T nested(int depth, Supplier<T> constructor, String... comment) {
        T config = constructor.get();
        new ConfigGroup(config.getName(), depth, comment);
        new CValue<Boolean, ForgeConfigSpec.BooleanValue>(config.getName(), builder -> {
            config.depth = depth;
            config.registerAll(builder);
            if (config.depth > depth)
                builder.pop(config.depth - depth);
            return null;
        });
        if (children == null)
            children = new ArrayList<>();
        children.add(config);
        return config;
    }

    public class CValue<V, T extends ConfigValue<V>> {
        protected ConfigValue<V> value;
        protected V defaultValue;
        protected float min;
        protected float max;
        protected String name;
        protected Boolean isDisabled;
        private IValueProvider<V, T> provider;

        private static HashSet<String> disabledValues = new HashSet<>(Arrays.asList("maxChunksBetweenOreClusters"));

        public CValue(String name, IValueProvider<V, T> provider, String... comment) {
            this.name = name;
            this.provider = builder -> {
                addComments(builder, comment);
                return provider.apply(builder);
            };
            if (allValues == null)
                allValues = new ArrayList<>();
            allValues.add(this);

            if(disabledValues.contains(name))
                isDisabled = true;
            else
                isDisabled = false;
        }

        public void addComments(Builder builder, String... comment) {
            if (comment.length > 0) {
                String[] comments = new String[comment.length + 1];
                comments[0] = ".";
                System.arraycopy(comment, 0, comments, 1, comment.length);
                builder.comment(comments);
            } else
                builder.comment(".");
        }

        public void register(ForgeConfigSpec.Builder builder) {
            if(!isDisabled)
                value = provider.apply(builder);
        }

        public V get() {
            if(isDisabled)
                return null;
            return value.get();
        }

        public boolean isDisabled() {
            return isDisabled;
        }

        public V getDefault() {
            return defaultValue;
        }

        public void set(V value) {
            this.value.set(value);
        }

        public String getName() {
            return name;
        }

    }

    /**
     * Marker for config subgroups
     */
    public class ConfigGroup extends CValue<Boolean, BooleanValue> {

        private int groupDepth;
        private String[] comment;

        public ConfigGroup(String name, int depth, String... comment) {
            super(name, builder -> null, comment);
            groupDepth = depth;
            this.comment = comment;
        }

        @Override
        public void register(Builder builder) {
            if (depth > groupDepth)
                builder.pop(depth - groupDepth);
            depth = groupDepth;
            addComments(builder, comment);
            builder.push(getName());
            depth++;
        }

    }

    public class ConfigBool extends CValue<Boolean, BooleanValue> {

        public ConfigBool(String name, boolean def, String... comment) {
            super(name, builder -> builder.define(name, def), comment);
        }
    }

    public class ConfigEnum<T extends Enum<T>> extends CValue<T, EnumValue<T>> {

        public ConfigEnum(String name, T defaultValue, String[] comment) {
            super(name, builder -> builder.defineEnum(name, defaultValue), comment);
        }

    }

    //Create a sourcer for ConfigList<String> don't use generic
    public class ConfigList extends CValue<List<String>, ForgeConfigSpec.ConfigValue<List<String>>>
    {
        public ConfigList(String name, List<String> def, String... comment)
        {
            super(name, builder -> builder.define(name, def), comment);
        }
    }


    public class ConfigFloat extends CValue<Double, DoubleValue> {

        public ConfigFloat(String name, float current, float min, float max, String... comment) {
            super(name, builder -> builder.defineInRange(name, current, min, max), comment);
            this.min = min;
            this.max = max;
            this.defaultValue = (double) current;
        }

        public float getF() {
            return get().floatValue();
        }

        public boolean test(float value) {
            return ( value <= max && value >= min);
        }
    }

    public class ConfigInt extends CValue<Integer, IntValue> {

        public ConfigInt(String name, int current, int min, int max, String... comment) {
            super(name, builder -> builder.defineInRange(name, current, min, max), comment);
            this.min = min;
            this.max = max;
            this.defaultValue = current;
        }

        public boolean test(int value) {
            return ( value <= max && value >= min);
        }
    }

    //Create a config for String using ValueSpec from forge library, any string is valid
    public class ConfigString extends CValue<String, ConfigValue<String>> {
        public ConfigString(String def, String name, String current, String... comment) {
            super(name, builder -> builder.define(name, current), comment);
            this.defaultValue = def;
        }
    }


}
