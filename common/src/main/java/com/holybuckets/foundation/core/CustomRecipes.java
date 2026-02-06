package com.holybuckets.foundation.core;

import com.holybuckets.foundation.enchantment.EssenceEnchantment;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.AnvilUpdateEvent;
import com.holybuckets.foundation.item.ModItems;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;
import java.util.Set;

public class CustomRecipes {


    public static void init(EventRegistrar registrar) {
        registrar.registerOnAnvilUpdate(enchantEssenceEvent, CustomRecipes::onAnvilUpdateEnchantEssence);
        registrar.registerOnServerStarted(CustomRecipes::onServerStartedComplteAnvilRegistration);
        registrar.registerOnTossItem(CustomRecipes::onTossItem);
    }

    //** PLAYER TOSS RECIPES **//


    //** ANVIL RECIPES **//

    private static void onServerStartedComplteAnvilRegistration(ServerStartedEvent event) {
        enchantEssenceEvent.setLeftItem(ModItems.enchantedEssence.getDefaultInstance());
    }

    //private final static Set<Item> ENCHANTED_ESSENCE_SET = new HashSet<>();
    private final static AnvilUpdateEvent enchantEssenceEvent = new AnvilUpdateEvent();

    private static void onAnvilUpdateEnchantEssence(AnvilUpdateEvent event)
    {
        ItemStack essence = event.getLeftItem();
        if(essence.getItem() != ModItems.enchantedEssence) return;

        ItemStack enchantingItem = event.getRightItem();
        Enchantment essenceEnchantment = EssenceEnchantment.of(enchantingItem.getItem());
        if(essenceEnchantment == null) return;

        int total = Math.min(essence.getCount(), enchantingItem.getCount());

        ItemStack result = essence.copy();
        result.setCount(total);
        result.enchant(essenceEnchantment, 1);

        event.setResultItem(result);
        event.setRepairItemCost(total);
        event.setMainItemCost(total);

        event.setCost( (int) Math.ceil(total/10.0));
    }
}

