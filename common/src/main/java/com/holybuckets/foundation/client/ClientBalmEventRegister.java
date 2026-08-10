package com.holybuckets.foundation.client;

import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.custom.TickType;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.client.ClientStartedEvent;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.client.BlockHighlightDrawEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenDrawEvent;
import net.blay09.mods.balm.api.event.client.screen.ContainerScreenDrawEvent;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ClientBalmEventRegister {
    private static final Set<Integer> registeredEvents = new ConcurrentSet<>();
    private static ClientEventRegistrar events;
    private static boolean notRegistered(Consumer<?> c) { return c!=null && !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.getOrDefault(func.hashCode(), EventPriority.Normal); }

    public static void registerEvents() {
        BalmEvents registry = Balm.getEvents();
        events = ClientEventRegistrar.getInstance();

        events.ON_CLIENT_STARTED_EVENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ClientStartedEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONNECTED_TO_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ConnectedToServerEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_DISCONNECTED_FROM_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(DisconnectedFromServerEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_BLOCK_HIGHLIGHT_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(BlockHighlightDrawEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ScreenDrawEvent.Pre.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ScreenDrawEvent.Post.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONTAINER_SCREEN_DRAW_BACKGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ContainerScreenDrawEvent.Background.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONTAINER_SCREEN_DRAW_FOREGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(ContainerScreenDrawEvent.Foreground.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(GuiDrawEvent.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(GuiDrawEvent.Pre.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(GuiDrawEvent.Post.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_ELEMENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            registry.onEvent(GuiDrawEvent.Element.class, c, p(c));
            registeredEvents.add(c.hashCode());
        });

        registerEntityEvents();
    }

    /**
     * Client mirror of the entity/player Balm callbacks that {@link com.holybuckets.foundation.event.BalmEventRegister}
     * handles server-side. Each callback is guarded to the client side (the server registrar rejects
     * client-side firings) and dispatched into {@link ClientEventRegistrar#fireEntityEvent(Object)} so
     * client-only code registered via {@code registerOnEntityEvent(...)} can react. These mirrors run
     * for side effects only — they always return the pass-through value and never cancel/alter the
     * server-authoritative outcome. Registered once.
     */
    static void registerEntityEvents() {
        if (!registeredEvents.add(Objects.hash("entityEvents"))) return;

        PlayerCallback.Attack.Before.EVENT.register((player, target) -> {
            if (player.level().isClientSide()) events.fireEntityEvent(new PlayerAttackEvent(player, target));
            return true;
        });

        BlockCallback.Break.Before.EVENT.register((level, pos, state, blockEntity, player) -> {
            if (level.isClientSide()) events.fireEntityEvent(new BreakBlockEvent(level, player, pos, state, blockEntity));
            return true;
        });

        LivingEntityCallback.Death.Before.EVENT.register((entity, damageSource) -> {
            if (entity.level().isClientSide()) events.fireEntityEvent(new LivingDeathEvent(entity, damageSource));
            return true;
        });

        LivingEntityCallback.Damage.Before.EVENT.register((entity, damageSource, damageAmount) -> {
            if (entity.level().isClientSide()) events.fireEntityEvent(new LivingDamageEvent(entity, damageSource, damageAmount));
            return damageAmount;
        });

        LivingEntityCallback.Fall.Before.EVENT.register((entity, fallDamage) -> {
            if (entity.level().isClientSide()) events.fireEntityEvent(new LivingFallEvent(entity, fallDamage));
            return fallDamage;
        });

        LivingEntityCallback.Heal.Before.EVENT.register((entity, healAmount) -> {
            if (entity.level().isClientSide()) events.fireEntityEvent(new LivingHealEvent(entity, healAmount));
            return healAmount;
        });

        BlockCallback.Use.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) events.fireEntityEvent(new UseBlockEvent(player, level, hand, hitResult));
            return InteractionEventResult.DEFAULT;
        });

        BlockCallback.DigSpeed.EVENT.register((blockGetter, pos, state, player, digSpeed) -> {
            if (player.level().isClientSide()) events.fireEntityEvent(new DigSpeedEvent(player, state, digSpeed));
            return digSpeed;
        });

        ItemCallback.Toss.Before.EVENT.register((player, itemStack) -> {
            if (player.level().isClientSide()) events.fireEntityEvent(new TossItemEvent(player, itemStack));
            return true;
        });
    }

    static void registerClientTickEvents() {
        BalmEvents registry = Balm.getEvents();

        if (registeredEvents.add(Objects.hash("onClientTick"))) {
            registry.onTickEvent(
                net.blay09.mods.balm.api.event.TickType.Client,
             TickPhase.End, events::onClientTick);
        }

        if (registeredEvents.add(Objects.hash("onClientLevelTick"))) {
            registry.onTickEvent(
                net.blay09.mods.balm.api.event.TickType.ClientLevel,
                 TickPhase.End, events::onClientLevelTick);
        }
    }
}
