package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.capability.ManagedChunkCapabilityProvider;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;




@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = Constants.MOD_ID)
public class ForgeCapabilityAttacher {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event)
    {
        if (event.getObject() instanceof LevelChunk) {
            LevelChunk chunk = event.getObject();
            if (chunk.getLevel().isClientSide())
                return;
            //LoggerBase.logDebug("002001", "Attaching Capabilities to MOD EVENT:  ");
            if (!event.getObject().getCapability(ManagedChunkCapabilityProvider.MANAGED_CHUNK).isPresent()) {
                event.addCapability(new ResourceLocation(Constants.MOD_ID, "managed_chunk"),
                    new ManagedChunkCapabilityProvider(chunk));
            }
        }
    }


}