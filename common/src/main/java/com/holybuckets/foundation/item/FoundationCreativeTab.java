package com.holybuckets.foundation.item;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.LoggerBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;


public final class FoundationCreativeTab {

    public static final String CLASS_ID = "042";
    public static final int ROW_WIDTH = 9;
    public static final int FOUNDATION_ORDER = Integer.MIN_VALUE;

    private record Group(String groupId, int order, int index, Supplier<ItemLike[]> items) {}

    private static final List<Group> GROUPS = new CopyOnWriteArrayList<>();
    private static final Map<Item, String> ITEM_GROUPS = new ConcurrentHashMap<>();

    private FoundationCreativeTab() {}


    public static void register(String groupId, int order, Supplier<ItemLike[]> items) {
        if (groupId == null || items == null) return;
        GROUPS.add(new Group(groupId, order, GROUPS.size(), items));
    }

    public static boolean isFoundationTab(CreativeModeTab tab) {
        if (tab == null) return false;
        return ModItems.FOUNDATIONS_TAB.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab));
    }

    static ItemLike[] collectItems()
    {
        List<Group> sorted = new ArrayList<>(GROUPS);
        sorted.sort(Comparator.comparingInt(Group::order).thenComparingInt(Group::index));

        Set<Item> added = new LinkedHashSet<>();
        Map<String, Integer> groupOrder = new LinkedHashMap<>();
        for (Group group : sorted) groupOrder.putIfAbsent(group.groupId(), groupOrder.size());

        List<Group> merged = new ArrayList<>(sorted);
        merged.sort(Comparator.comparingInt(g -> groupOrder.get(g.groupId())));

        ITEM_GROUPS.clear();
        for (Group group : merged)
        {
            ItemLike[] items;
            try {
                items = group.items().get();
            } catch (Exception e) {
                LoggerBase.logError(null, CLASS_ID + "000", "Creative tab group " + group.groupId()
                    + " failed to supply items: " + e.getMessage());
                continue;
            }
            if (items == null) continue;

            for (ItemLike like : items) {
                if (like == null) continue;
                Item item = like.asItem();
                if (item == null || item == Items.AIR) continue;
                if (!added.add(item)) continue;
                ITEM_GROUPS.put(item, group.groupId());
            }
        }
        return added.toArray(new ItemLike[0]);
    }


    public static List<ItemStack> withRowGaps(Collection<ItemStack> items)
    {
        List<ItemStack> out = new ArrayList<>(items.size() + ROW_WIDTH * 4);
        String currentGroup = null;
        for (ItemStack stack : items)
        {
            String group = stack.isEmpty() ? currentGroup : ITEM_GROUPS.getOrDefault(stack.getItem(), Constants.MOD_ID);
            if (currentGroup != null && !currentGroup.equals(group)) {
                while (out.size() % ROW_WIDTH != 0) out.add(ItemStack.EMPTY);
            }
            currentGroup = group;
            out.add(stack);
        }
        return out;
    }

}
