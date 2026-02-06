package com.holybuckets.foundation.event.custom;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event fired when an anvil menu is updated with new items
 */
public class AnvilUpdateEvent {

    public static Map<AnvilMenu, AnvilUpdateEvent> ANVIL_EVENTS = new ConcurrentHashMap<>();

    private final AnvilMenu anvilMenu;
    ItemStack leftItem;
    ItemStack rightItem;
    private ItemStack resultItem = null;
    private Integer cost = 0;
    private Integer repairItemCountCost = 1;
    private Integer mainItemCost = 0;

    public AnvilUpdateEvent() {
        this.anvilMenu = null;
        this.leftItem = null;
        this.rightItem = null;
    }

    public AnvilUpdateEvent(AnvilMenu anvilMenu, ItemStack leftItem, ItemStack rightItem) {
        this.anvilMenu = anvilMenu;
        this.leftItem = leftItem;
        this.rightItem = rightItem;
    }

    /**
     * Be aware that items are defined in a registry so most items aren't defined when you are constructing this object
     * to register the event. Use the dummy contructor and set right and left items on the ServerStartedEvent
     * @param leftItem
     * @param rightItem
     */
    public AnvilUpdateEvent(Item leftItem, Item rightItem) {
        this.anvilMenu = null;
        this.leftItem = leftItem.getDefaultInstance();
        this.rightItem = rightItem.getDefaultInstance();
    }

    public AnvilMenu getAnvilMenu() {
        return anvilMenu;
    }

    public ItemStack getLeftItem() {
        return leftItem;
    }

    public ItemStack getRightItem() {
        return rightItem;
    }

    public void setLeftItem(ItemStack leftItem) {
        this.leftItem = leftItem;
    }

    public void setRightItem(ItemStack rightItem) {
        this.rightItem = rightItem;
    }

    public ItemStack getResultItem() {
        return resultItem;
    }

    public void setResultItem(ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    public Integer getResultCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public void setMainItemCost(Integer mainItemCost) {
        this.mainItemCost = mainItemCost;
    }

    public int getMainItemCost() {
        return mainItemCost;
    }

    public Integer getRepairItemCost() {
        return repairItemCountCost;
    }

    public void setRepairItemCost(Integer repairItemCountCost) {
        this.repairItemCountCost = repairItemCountCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AnvilUpdateEvent that = (AnvilUpdateEvent) obj;
        
        // Compare based on item types only
        //if the event item is null, passes
        if( this.leftItem == null ) {
            return Objects.equals(getItemType(rightItem), getItemType(that.rightItem));
        }

        if( this.rightItem == null ) {
            return Objects.equals(getItemType(leftItem), getItemType(that.leftItem));
        }

        return Objects.equals(getItemType(leftItem), getItemType(that.leftItem)) &&
               Objects.equals(getItemType(rightItem), getItemType(that.rightItem));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItemType(leftItem), getItemType(rightItem));
    }

    Item getItemType(ItemStack item) {
        if (item == null || item.isEmpty())
            return Items.AIR;
        return item.getItem();
    }

    /**
     * AnvilUpdateEvent driven by items with matching base materials
     */
    public static class MaterialDriven extends AnvilUpdateEvent
    {
        Set<Item> leftMaterials;
        Set<Item> rightMaterials;


        public MaterialDriven(@Nullable Item leftItem, @Nullable Item rightItem, @Nullable Set<Item> leftMats, @Nullable Set<Item> rightMats)
        {
            super(leftItem != null ? leftItem : Items.AIR, rightItem != null ? rightItem : Items.AIR);
            this.leftMaterials = leftMats==null ? Set.of() : leftMats;
            this.rightMaterials = rightMats==null ? Set.of() : rightMats;
        }

        public MaterialDriven(@NotNull Set<Item> leftMats, @NotNull Item rightItem) {
            this(null, rightItem, leftMats, null);
        }

        public MaterialDriven(@NotNull Item leftItem, @NotNull Set<Item> rightMats) {
            this(leftItem, null, null, rightMats);
        }


        public MaterialDriven(Set<Item> leftMats, Set<Item> rightMats) {
            this(null, null, leftMats, rightMats);
        }

        public void setLeftMaterials(Set<Item> leftMaterials) {
            this.leftMaterials = leftMaterials;
        }

        public void setRightMaterials(Set<Item> rightMaterials) {
            this.rightMaterials = rightMaterials;
        }

        //override .equals
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null ) return false;

            AnvilUpdateEvent that = (AnvilUpdateEvent) obj;

            boolean leftHasMaterial = leftMaterials.contains(getItemType(that.leftItem));
            boolean rightHasMaterial = rightMaterials.contains(getItemType(that.rightItem));

            boolean leftItemEquals = Objects.equals(getItemType(this.leftItem), getItemType(that.leftItem));
            boolean rightItemEquals = Objects.equals(getItemType(this.rightItem), getItemType(that.rightItem));

            //right item and left material or left item and right material
            if ((leftItemEquals && rightHasMaterial) || (rightItemEquals && leftHasMaterial)) {
                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getItemType(leftItem), getItemType(rightItem), leftMaterials, rightMaterials);
        }
    }

    /**
     * AnvilUpdateEvent driven by items with matching enchantments
     */
    public static class EnchantDriven extends AnvilUpdateEvent {

        Set<Enchantment> leftEnchantments;
        Set<Enchantment> rightEnchantments;

        public EnchantDriven(@Nullable Item leftItem, @Nullable Item rightItem,
                             @Nullable Set<Enchantment> leftEnchants, @Nullable Set<Enchantment> rightEnchants) {
            super(leftItem != null ? leftItem : Items.AIR, rightItem != null ? rightItem : Items.AIR);
            this.leftEnchantments = leftEnchants==null ? Set.of() : leftEnchants;
            this.rightEnchantments = rightEnchants==null ? Set.of() : rightEnchants;
        }

        public EnchantDriven(@NotNull Set<Enchantment> leftEnchants, @NotNull Item rightItem) {
            this(null, rightItem, leftEnchants, null);
        }

        public EnchantDriven(@NotNull Item leftItem, @NotNull Set<Enchantment> rightEnchants) {
            this(leftItem, null, null, rightEnchants);
        }

        public EnchantDriven(Set<Enchantment> leftEnchants, Set<Enchantment> rightEnchants) {
            this(null, null, leftEnchants, rightEnchants);
        }

        public void setLeftEnchantments(Set<Enchantment> leftEnchantments) {
            this.leftEnchantments = leftEnchantments;
        }

        public void setRightEnchantments(Set<Enchantment> rightEnchantments) {
            this.rightEnchantments = rightEnchantments;
        }

        //override .equals
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null ) return false;

            AnvilUpdateEvent that = (AnvilUpdateEvent) obj;

            //if that contains any items that have the enchantments, return true
            boolean leftHasEnchant = false;
            boolean rightHasEnchant = false;
            for (Enchantment enchantment : leftEnchantments) {
                if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, that.leftItem) > 0) {
                    leftHasEnchant = true;
                }
            }
            for (Enchantment enchantment : rightEnchantments) {
                if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, that.rightItem) > 0) {
                    rightHasEnchant = true;
                }
            }

            boolean leftItemEquals = Objects.equals(getItemType(this.leftItem), getItemType(that.leftItem));
            boolean rightItemEquals = Objects.equals(getItemType(this.rightItem), getItemType(that.rightItem));

            //right item and left enchant or left item and right enchant
            if ((leftItemEquals && rightHasEnchant) || (rightItemEquals && leftHasEnchant)) {
                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getItemType(leftItem), getItemType(rightItem), leftEnchantments, rightEnchantments);
        }
    }

}
