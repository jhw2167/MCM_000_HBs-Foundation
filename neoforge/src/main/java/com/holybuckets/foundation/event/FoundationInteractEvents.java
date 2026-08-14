package com.holybuckets.foundation.event;

import com.holybuckets.foundation.event.custom.PlayerInteractEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class FoundationInteractEvents {

    private FoundationInteractEvents() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) ->
                fire(e, new PlayerInteractEvent.EntityInteract(
                    e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                    e.getPos(), e.getFace(), e.getTarget(), e.getLocalPos())));

        NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) ->
                fire(e, new PlayerInteractEvent.LeftClickInteraction(
                    e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                    e.getPos(), e.getFace())));

        NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) ->
                fire(e, new PlayerInteractEvent.RightClickInteraction(
                    e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                    e.getPos(), e.getFace())));

        NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) ->
                fire(e, new PlayerInteractEvent.RightClickInteraction(
                    e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                    e.getPos(), e.getFace())));

        NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty e) ->
                fire(e, new PlayerInteractEvent.LeftClickInteraction(
                    e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                    e.getPos(), e.getFace())));

        NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty e) ->
                fire(e, new PlayerInteractEvent.RightClickInteraction(
                    e.getEntity(), e.getLevel(), e.getHand(), e.getItemStack(),
                    e.getPos(), e.getFace())));
    }

    /**
     * EntityInteract is intentionally not bridged; EntityInteractSpecific fires first and carries
     * the same data plus the local hit position.
     */
    private static void fire(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent source,
                             PlayerInteractEvent custom) {
        EventRegistrar registrar = EventRegistrar.getInstance();
        if (registrar == null) return;

        boolean canceled = registrar.onPlayerInteract(custom);
        if (canceled && source instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

}
