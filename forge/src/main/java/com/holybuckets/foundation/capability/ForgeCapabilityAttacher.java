package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.capability.ManagedChunkCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;




@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = Constants.MOD_ID)
public class ForgeCapabilityAttacher {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent event)
    {

        if (event.getObject() instanceof LevelChunk)
        {
            LevelChunk chunk = (LevelChunk) event.getObject();
            if(chunk.getLevel().isClientSide()) return;

            //LoggerBase.logDebug("002001", "Attaching Capabilities to MOD EVENT:  ");
            if (!chunk.getCapability(ManagedChunkCapabilityProvider.MANAGED_CHUNK).isPresent()) {
                event.addCapability(new ResourceLocation(Constants.MOD_ID, "managed_chunk"),
                    new ManagedChunkCapabilityProvider(chunk));
            }
        }

        if (event.getObject() instanceof Player)
        {
            Player player = (Player) event.getObject();
            if (!player.getCapability(ManagedPlayerCapabilityProvider.MANAGED_PLAYER).isPresent()) {
                event.addCapability(new ResourceLocation(Constants.MOD_ID, "managed_player"),
                    new ManagedPlayerCapabilityProvider(player));
            }
        }

    }

}
