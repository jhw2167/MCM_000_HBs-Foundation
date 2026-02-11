package com.holybuckets.foundation.core;

import com.holybuckets.foundation.enchantment.EssenceEnchantment;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.AnvilUpdateEvent;
import com.holybuckets.foundation.event.custom.ItemEntityTickEvent;
import com.holybuckets.foundation.item.ModItems;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashSet;
import java.util.Set;

import static com.holybuckets.foundation.item.EnchantedEssence.ESSENCE_DATA_TAG;

public class CustomRecipes {


    public static void init(EventRegistrar registrar) {
        registrar.registerOnAnvilUpdate(enchantEssenceEvent, CustomRecipes::onAnvilUpdateEnchantEssence);
        registrar.registerOnServerStarted(CustomRecipes::onServerStartedComplteAnvilRegistration);
        registrar.registerOnItemEntityTick(() -> ModItems.enchantedEssence, CustomRecipes::onEnchantedEssenceTick);
    }

    //** ENTITY TICK RECIPES **//

    private static void onEnchantedEssenceTick(ItemEntityTickEvent event)
     {
        if(!event.is( ModItems.enchantedEssence) ) return;
         ItemEntity entity = event.getItemEntity();
        if(!entity.getItem().isEnchanted()) return;
        ItemStack newStack = ModItems.enchantedEssence.getDefaultInstance();
        newStack.setCount(entity.getItem().getCount());
        ItemEntity newEntity = new ItemEntity(entity.level(),
        entity.getX(), entity.getY(), entity.getZ(), newStack);
        newEntity.setPickUpDelay(20);
        event.getItemEntity().level().addFreshEntity( newEntity );
        event.getItemEntity().discard();
    }


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
        result.getOrCreateTag().putString(ESSENCE_DATA_TAG, essenceEnchantment.getDescriptionId());

        event.setResultItem(result);
        event.setRepairItemCost(total);
        event.setMainItemCost(total);

        event.setCost( (int) Math.ceil(total/10.0));
    }
}

