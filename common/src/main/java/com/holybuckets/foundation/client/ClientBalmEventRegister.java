package com.holybuckets.foundation.client;

import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.balm.EventPriority;
import com.holybuckets.foundation.event.balm.BreakBlockEvent;
import com.holybuckets.foundation.event.balm.DigSpeedEvent;
import com.holybuckets.foundation.event.balm.LivingDamageEvent;
import com.holybuckets.foundation.event.balm.LivingDeathEvent;
import com.holybuckets.foundation.event.balm.LivingFallEvent;
import com.holybuckets.foundation.event.balm.LivingHealEvent;
import com.holybuckets.foundation.event.balm.PlayerAttackEvent;
import com.holybuckets.foundation.event.balm.TossItemEvent;
import com.holybuckets.foundation.event.balm.UseBlockEvent;
import com.holybuckets.foundation.event.balm.client.ClientStartedEvent;
import com.holybuckets.foundation.event.balm.client.ConnectedToServerEvent;
import com.holybuckets.foundation.event.balm.client.DisconnectedFromServerEvent;
import com.holybuckets.foundation.event.balm.client.BlockHighlightDrawEvent;
import com.holybuckets.foundation.event.balm.client.GuiDrawEvent;
import com.holybuckets.foundation.event.balm.client.screen.ScreenDrawEvent;
import com.holybuckets.foundation.event.balm.client.screen.ContainerScreenDrawEvent;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.blay09.mods.balm.client.platform.event.callback.RenderCallback;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.blay09.mods.balm.platform.event.callback.BlockCallback;
import net.blay09.mods.balm.platform.event.callback.InteractionEventResult;
import net.blay09.mods.balm.platform.event.callback.ItemCallback;
import net.blay09.mods.balm.platform.event.callback.LivingEntityCallback;
import net.blay09.mods.balm.platform.event.callback.PlayerCallback;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ClientBalmEventRegister {
    private static final Set<Integer> registeredEvents = new ConcurrentSet<>();
    private static ClientEventRegistrar events;
    private static boolean notRegistered(Consumer<?> c) { return c!=null && !registeredEvents.contains(c.hashCode()); }
    public static EventPriority p(Consumer<?> func) { return events.PRIORITIES.getOrDefault(func.hashCode(), EventPriority.Normal); }

    public static void registerEvents() {
        events = ClientEventRegistrar.getInstance();

        events.ON_CLIENT_STARTED_EVENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ClientLifecycleCallback.Started.EVENT.register(p(c).toPhase(), client ->
                c.accept(new ClientStartedEvent(client)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONNECTED_TO_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ClientLifecycleCallback.ConnectedToServer.EVENT.register(p(c).toPhase(), client ->
                c.accept(new ConnectedToServerEvent(client)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_DISCONNECTED_FROM_SERVER.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ClientLifecycleCallback.DisconnectedFromServer.EVENT.register(p(c).toPhase(), client ->
                c.accept(new DisconnectedFromServerEvent(client)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_BLOCK_HIGHLIGHT_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.BlockHighlight.EVENT.register(p(c).toPhase(), (hitResult, poseStack, multiBufferSource, camera, color, lineWidth) -> {
                BlockHighlightDrawEvent event = new BlockHighlightDrawEvent(hitResult, poseStack, multiBufferSource, camera);
                c.accept(event);
                return !event.isCanceled();
            });
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.BEFORE.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ScreenDrawEvent.Pre(screen, guiGraphics, mouseX, mouseY, delta)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_SCREEN_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.AFTER.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ScreenDrawEvent.Post(screen, guiGraphics, mouseX, mouseY, delta)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_CONTAINER_SCREEN_DRAW_BACKGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.AFTER_BACKGROUND.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ContainerScreenDrawEvent.Background(screen, guiGraphics, mouseX, mouseY)));
            registeredEvents.add(c.hashCode());
        });

        // Balm 26.1 has no dedicated foreground (renderLabels) hook; Render.AFTER is the closest equivalent
        events.ON_CONTAINER_SCREEN_DRAW_FOREGROUND.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            ScreenCallback.Render.AFTER.register(p(c).toPhase(), (screen, guiGraphics, mouseX, mouseY, delta) ->
                c.accept(new ContainerScreenDrawEvent.Foreground(screen, guiGraphics, mouseX, mouseY)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.AFTER.register(p(c).toPhase(), (guiGraphics, window) ->
                c.accept(new GuiDrawEvent.Post(window, guiGraphics, GuiDrawEvent.Element.ALL)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_PRE.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.BEFORE.register(p(c).toPhase(), (guiGraphics, window) -> {
                GuiDrawEvent.Pre event = new GuiDrawEvent.Pre(window, guiGraphics, GuiDrawEvent.Element.ALL);
                c.accept(event);
                return !event.isCanceled();
            });
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_POST.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.AFTER.register(p(c).toPhase(), (guiGraphics, window) ->
                c.accept(new GuiDrawEvent.Post(window, guiGraphics, GuiDrawEvent.Element.ALL)));
            registeredEvents.add(c.hashCode());
        });

        events.ON_GUI_DRAW_ELEMENT.stream().filter(ClientBalmEventRegister::notRegistered).forEach(c -> {
            RenderCallback.Gui.Health.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.HEALTH));
            RenderCallback.Gui.Chat.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.CHAT));
            RenderCallback.Gui.Debug.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.DEBUG));
            RenderCallback.Gui.BossInfo.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.BOSS_INFO));
            RenderCallback.Gui.PlayerList.AFTER.register((guiGraphics, window) -> c.accept(GuiDrawEvent.Element.PLAYER_LIST));
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
        if (registeredEvents.add(Objects.hash("onClientTick"))) {
            ClientTickCallback.AFTER.register(events::onClientTick);
        }

        if (registeredEvents.add(Objects.hash("onClientLevelTick"))) {
            ClientTickCallback.ClientLevelTick.AFTER.register(events::onClientLevelTick);
        }
    }
}
