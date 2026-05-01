package com.holybuckets.foundation.core;

import com.holybuckets.foundation.CommonClass;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.biome.BiomeManager;
import com.holybuckets.foundation.datacomponent.EssenceDataComponent;
import com.holybuckets.foundation.enchantment.EssenceEnchantment;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.AnvilUpdateEvent;
import com.holybuckets.foundation.event.custom.ItemEntityTickEvent;
import com.holybuckets.foundation.item.ModItems;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.List;

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

         //1. If the essence is in a cauldron in water
        if(entity.isInWater())
        {
            entity.setPickUpDelay(20);
            purifyEssence(entity);
        }
        else
        {
            BlockPos blockPos = entity.blockPosition();
            Level level = entity.level();
            boolean randomTick = level.random.nextFloat() < 0.07f; // 5% chance each tick to process the essence
            //if(!randomTick) return;

            if( level.getBlockState(blockPos).getBlock() instanceof LayeredCauldronBlock cauldronBlock)
            {
                if(cauldronBlock.isFull(level.getBlockState(blockPos)))
                    createPortalToBiome(entity);
            }
        }
    }

        private static void purifyEssence(ItemEntity entity)
        {
            ItemStack newStack = ModItems.enchantedEssence.getDefaultInstance();
            newStack.setCount(entity.getItem().getCount());
            ItemEntity newEntity = new ItemEntity(entity.level(),
                entity.getX(), entity.getY(), entity.getZ(), newStack);
            entity.level().addFreshEntity( newEntity );
            entity.discard();
        }

        private static void createPortalToBiome(ItemEntity entity)
        {
            BlockPos pos = entity.blockPosition();
            Level level = entity.level();
            if(EssenceCauldronManager.hasEssenceCauldron(level, pos)) return;
            BiomeManager manager = BiomeManager.get(level);
            if(manager == null || !entity.getItem().isEnchanted() ) { enchantedEssenceFailed(entity, null); return; }

            EssenceType type = EssenceDataComponent.getEssenceType(entity.getItem());
            if(type == null) { enchantedEssenceFailed(entity, null); return; }

            EssenceCauldronManager.addEssenceCauldron(level, pos, type);

            entity.discard();
        }

        private static void enchantedEssenceFailed(ItemEntity entity, String biomeTypes)
        {
            List<ServerPlayer> players = HBUtil.PlayerUtil
                .getAllPlayersInBlockRange(entity.blockPosition(), 3);
            for(ServerPlayer player : players) {
                if(biomeTypes!=null)
                    CommonClass.MESSAGER.bottomScreenErrorHint(player,
                     "No biomes of type " + biomeTypes + " found nearby");
                else
                    CommonClass.MESSAGER.bottomScreenErrorHint(player, "No biomes found nearby");
            }
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
        if(!EssenceEnchantment.hasEssenceEnchantment(enchantingItem)) return;

        int total = Math.min(essence.getCount(), enchantingItem.getCount());

        ItemStack result = essence.copy();
        result.setCount(total);
        result.enchant(EssenceEnchantment.GET(), 1);
        EssenceDataComponent.createFromItem(result, enchantingItem.getItem());

        event.setResultItem(result);
        event.setRepairItemCost(total);
        event.setMainItemCost(total);

        event.setCost( (int) Math.ceil(total/10.0));
    }
}

