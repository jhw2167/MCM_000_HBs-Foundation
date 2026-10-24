package com.holybuckets.foundation.event.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Fired per bottle slot when a brewing stand checks whether it can brew (CHECK, every tick)
 * and again when the brew completes (BREW). Set a result to override that slot's output.
 *
 * Instances double as registration keys: a registered key matches a live event through
 * matchesIngredient and matchesBottle. Null key items act as wildcards.
 */
public class BrewingUpdateEvent {

    public enum Phase { CHECK, BREW }

    public static final int BOTTLE_SLOT_COUNT = 3;
    public static final int INGREDIENT_SLOT = 3;
    public static final int FUEL_SLOT = 4;

    //Registration key
    private final Item keyIngredient;
    private final Item keyBottle;

    //Live event data
    private final Phase phase;
    private final Level level;
    private final BlockPos pos;
    private final BrewingStandBlockEntity blockEntity;
    private final NonNullList<ItemStack> slots;
    private final int bottleSlot;

    private ItemStack resultItem = null;
    private int ingredientCost = 1;

    /** Wildcard key, matches every brewing event */
    public BrewingUpdateEvent() {
        this(null, null);
    }

    /**
     * Key constructor. Items are registry backed, so construct with null and use a subclass,
     * or construct on ServerStartedEvent if the items are not yet available at init.
     */
    public BrewingUpdateEvent(@Nullable Item ingredient, @Nullable Item bottle) {
        this.keyIngredient = ingredient;
        this.keyBottle = bottle;
        this.phase = null;
        this.level = null;
        this.pos = null;
        this.blockEntity = null;
        this.slots = null;
        this.bottleSlot = -1;
    }

    public BrewingUpdateEvent(Phase phase, @Nullable Level level, @Nullable BlockPos pos,
                              @Nullable BrewingStandBlockEntity blockEntity,
                              NonNullList<ItemStack> slots, int bottleSlot) {
        this.keyIngredient = null;
        this.keyBottle = null;
        this.phase = phase;
        this.level = level;
        this.pos = pos;
        this.blockEntity = blockEntity;
        this.slots = slots;
        this.bottleSlot = bottleSlot;
    }

    //** LIVE EVENT GETTERS

    public Phase getPhase() { return phase; }

    public boolean isCheck() { return phase == Phase.CHECK; }

    public boolean isBrew() { return phase == Phase.BREW; }

    /** Null during CHECK if the stand was not ticked through serverTick */
    public @Nullable Level getLevel() { return level; }

    public @Nullable BlockPos getPos() { return pos; }

    public @Nullable BrewingStandBlockEntity getBlockEntity() { return blockEntity; }

    /** Live stand inventory: 0-2 bottles, 3 ingredient, 4 fuel. Do not mutate. */
    public NonNullList<ItemStack> getSlots() { return slots; }

    /** Index (0-2) of the bottle slot this event is evaluating */
    public int getBottleSlot() { return bottleSlot; }

    public ItemStack getBottle() { return getSlot(bottleSlot); }

    public ItemStack getIngredient() { return getSlot(INGREDIENT_SLOT); }

    public ItemStack getFuel() { return getSlot(FUEL_SLOT); }

    public ItemStack getSlot(int index) {
        if (slots == null || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        return slots.get(index);
    }

    //** RESULT

    public @Nullable ItemStack getResultItem() { return resultItem; }

    public boolean hasResult() { return resultItem != null && !resultItem.isEmpty(); }

    /** Output that replaces this bottle slot when the brew completes */
    public void setResultItem(@Nullable ItemStack resultItem) { this.resultItem = resultItem; }

    public int getIngredientCost() { return ingredientCost; }

    /** Total ingredients consumed by the brew, vanilla consumes 1. Highest cost across slots wins. */
    public void setIngredientCost(int ingredientCost) { this.ingredientCost = Math.max(1, ingredientCost); }

    //** KEY MATCHING

    public boolean matchesIngredient(ItemStack ingredient) {
        if (keyIngredient == null) return true;
        return ingredient != null && ingredient.is(keyIngredient);
    }

    public boolean matchesBottle(ItemStack bottle) {
        if (keyBottle == null) return true;
        return bottle != null && bottle.is(keyBottle);
    }

    /** True when this key names a specific ingredient, used to allow it into the ingredient slot */
    public boolean acceptsIngredientForPlacement(ItemStack ingredient) {
        return keyIngredient != null && matchesIngredient(ingredient);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BrewingUpdateEvent that)) return false;
        return matchesIngredient(that.getIngredient()) && matchesBottle(that.getBottle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyIngredient, keyBottle);
    }

    /**
     * Key matched by predicates on the ingredient and bottle stacks. A null predicate matches anything.
     */
    public static class PredicateDriven extends BrewingUpdateEvent {

        private final Predicate<ItemStack> ingredientFilter;
        private final Predicate<ItemStack> bottleFilter;

        public PredicateDriven(@Nullable Predicate<ItemStack> ingredientFilter, @Nullable Predicate<ItemStack> bottleFilter) {
            super(null, null);
            this.ingredientFilter = ingredientFilter;
            this.bottleFilter = bottleFilter;
        }

        @Override
        public boolean matchesIngredient(ItemStack ingredient) {
            if (ingredient == null || ingredient.isEmpty()) return false;
            return ingredientFilter == null || ingredientFilter.test(ingredient);
        }

        @Override
        public boolean matchesBottle(ItemStack bottle) {
            if (bottle == null || bottle.isEmpty()) return false;
            return bottleFilter == null || bottleFilter.test(bottle);
        }

        @Override
        public boolean acceptsIngredientForPlacement(ItemStack ingredient) {
            return ingredientFilter != null && matchesIngredient(ingredient);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ingredientFilter, bottleFilter);
        }
    }

    /**
     * Key matched by a specific ingredient and the potion contained in the bottle.
     */
    public static class PotionDriven extends BrewingUpdateEvent {

        private final Set<Potion> bottlePotions;

        public PotionDriven(@Nullable Item ingredient, Set<Potion> bottlePotions) {
            super(ingredient, null);
            this.bottlePotions = bottlePotions == null ? Set.of() : bottlePotions;
        }

        @Override
        public boolean matchesBottle(ItemStack bottle) {
            if (bottle == null || bottle.isEmpty()) return false;
            return bottlePotions.contains(PotionUtils.getPotion(bottle));
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), bottlePotions);
        }
    }

}
