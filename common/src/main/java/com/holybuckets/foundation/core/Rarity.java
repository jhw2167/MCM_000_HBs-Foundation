package com.holybuckets.foundation.core;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum Rarity implements StringRepresentable {

    COMMON("common", ChatFormatting.WHITE),
    RARE("rare", ChatFormatting.BLUE),
    EPIC("epic", ChatFormatting.LIGHT_PURPLE),
    LEGENDARY("legendary", ChatFormatting.GOLD);

    public static final Rarity DEFAULT = COMMON;

    private final String name;
    private final ChatFormatting color;

    Rarity(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public ChatFormatting getColor() {
        return this.color;
    }

    public int getTier() {
        return this.ordinal();
    }

    public boolean atLeast(Rarity other) {
        return other != null && this.ordinal() >= other.ordinal();
    }

    public String getTranslationKey(String modId) {
        return "rarity." + modId + "." + this.name;
    }

    public static Rarity byName(String name) {
        if (name == null) return DEFAULT;
        for (Rarity r : values()) {
            if (r.name.equalsIgnoreCase(name)) return r;
        }
        return DEFAULT;
    }

    public static Rarity byOrdinal(int ordinal) {
        Rarity[] all = values();
        if (ordinal < 0 || ordinal >= all.length) return DEFAULT;
        return all[ordinal];
    }

}
