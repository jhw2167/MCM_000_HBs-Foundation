package com.holybuckets.foundation.event;

import com.holybuckets.foundation.Constants;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.custom.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeModEvents {

    private static boolean isIntegrated = false;
    @SubscribeEvent
    public static void onPlayerInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent forgeEvent) {
        PlayerInteractEvent custom = null;

        if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) {
            custom = new PlayerInteractEvent.EntityInteract(
                e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                e.getPos(), e.getFace(), e.getTarget(), e.getLocalPos()
            );
        } else if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
           //use entityInteractSpecific only, fires first
        } else if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) {
            custom = new PlayerInteractEvent.LeftClickInteraction(
                e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                e.getPos(), e.getFace()
            );
        } else if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
            custom = new PlayerInteractEvent.RightClickInteraction(
                e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                e.getPos(), e.getFace()
            );
        } else if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem e) {
            custom = new PlayerInteractEvent.RightClickInteraction(
                e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                e.getPos(), e.getFace()
            );
        } else if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty e) {
            custom = new PlayerInteractEvent.LeftClickInteraction(
                e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                e.getPos(), e.getFace()
            );
        } else if (forgeEvent instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty e) {
            custom = new PlayerInteractEvent.RightClickInteraction(
                e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                e.getPos(), e.getFace()
            );
        }

        if (custom == null) return;
        boolean canceled = EventRegistrar.getInstance().onPlayerInteract(custom);
        if (canceled && forgeEvent.isCancelable()) forgeEvent.setCanceled(true);
    }
}
