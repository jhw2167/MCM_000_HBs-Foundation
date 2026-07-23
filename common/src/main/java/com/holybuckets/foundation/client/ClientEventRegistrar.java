package com.holybuckets.foundation.client;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.event.custom.*;
import com.holybuckets.foundation.model.ManagedChunkEvents;
import com.holybuckets.foundation.networking.ClientInputMessage;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.holybuckets.foundation.util.MixinManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.holybuckets.foundation.event.balm.*;
import com.holybuckets.foundation.event.balm.client.BlockHighlightDrawEvent;
import com.holybuckets.foundation.event.balm.client.ClientStartedEvent;
import com.holybuckets.foundation.event.balm.client.ConnectedToServerEvent;
import com.holybuckets.foundation.event.balm.client.DisconnectedFromServerEvent;
import com.holybuckets.foundation.event.balm.client.screen.ScreenDrawEvent;
import com.holybuckets.foundation.event.balm.client.screen.ContainerScreenDrawEvent;
import com.holybuckets.foundation.event.balm.client.GuiDrawEvent;
import com.holybuckets.foundation.event.balm.server.ServerStartingEvent;
import com.holybuckets.foundation.event.balm.server.ServerStoppedEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class ClientEventRegistrar {
    public static final String CLASS_ID = "010";


    /**
     * World Data
     **/
    private static ClientEventRegistrar instance;
    final Map<Integer, EventPriority> PRIORITIES = new HashMap<>();
    
    final Set<Consumer<ClientStartedEvent>> ON_CLIENT_STARTED_EVENT = new ConcurrentSet<>();
    final Set<Consumer<ConnectedToServerEvent>> ON_CONNECTED_TO_SERVER = new ConcurrentSet<>();
    final Set<Consumer<DisconnectedFromServerEvent>> ON_DISCONNECTED_FROM_SERVER = new ConcurrentSet<>();
    final Set<Consumer<ServerStartingEvent>> ON_BEFORE_SERVER_START = new ConcurrentSet<>();
    final Set<Consumer<ServerStoppedEvent>> ON_SERVER_STOP = new ConcurrentSet<>();
    final Map<TickScheme, Consumer<?>> CLIENT_TICK_EVENTS = new ConcurrentHashMap<>();
    final Map<TickScheme, Consumer<?>> CLIENT_LEVEL_TICK_EVENTS = new ConcurrentHashMap<>();
    final Set<Consumer<ClientInputEvent>> ON_CLIENT_INPUT = new ConcurrentSet<>();
    final Multimap<String, Consumer<SimpleMessageEvent>> ON_SIMPLE_MESSAGE = HashMultimap.create();
    final Multimap<RenderLevelEvent.RenderStage, Consumer<RenderLevelEvent>> ON_RENDER_LEVEL = HashMultimap.create();
    final Set<Consumer<DetermineActiveWaypointEvent>> ON_DETERMINE_ACTIVE_WAYPOINT = new ConcurrentSet<>();

    // Static RenderLevelEvent instance for performance
    private static final RenderLevelEvent RENDER_LEVEL_EVENT = new RenderLevelEvent();
    
    // HashSet to track stages that have thrown exceptions
    private final Set<RenderLevelEvent.RenderStage> renderLevelErrorStages = new HashSet<>();

    private int ticks = 0;
    /**
     * Constructor
     **/
    private ClientEventRegistrar() {
        super();
        LoggerBase.logInit(null, "010000", this.getClass().getName());
        ticks = 0;
        instance = this;
    }

    public static ClientEventRegistrar getInstance() {
        if( instance == null ) init();
        return instance;
    }

    private static void init() {
        instance = new ClientEventRegistrar();
        instance.registerOnClientStarted(ClientEventRegistrar::registerOnClientStarted);
    }

    private static void registerOnClientStarted(ClientStartedEvent event) {
        ClientBalmEventRegister.registerClientTickEvents();
    }

    //Create public methods for pushing functions onto each function event
    private <T> void generalRegister(Consumer<T> function, Set<Consumer<T>> set, EventPriority priority) {
        set.add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }


    //** UI

    public void registerOnBlockHighlightDraw(Consumer<BlockHighlightDrawEvent> function) {
        registerOnBlockHighlightDraw(function, EventPriority.Normal);
    }
    final Set<Consumer<BlockHighlightDrawEvent>> ON_BLOCK_HIGHLIGHT_DRAW = new ConcurrentSet<>();
    public void registerOnBlockHighlightDraw(Consumer<BlockHighlightDrawEvent> function, EventPriority priority) {
        generalRegister(function, ON_BLOCK_HIGHLIGHT_DRAW, priority);
    }

    public void registerOnScreenDrawPre(Consumer<ScreenDrawEvent.Pre> function) {
        registerOnScreenDrawPre(function, EventPriority.Normal);
    }
    final Set<Consumer<ScreenDrawEvent.Pre>> ON_SCREEN_DRAW_PRE = new ConcurrentSet<>();
    public void registerOnScreenDrawPre(Consumer<ScreenDrawEvent.Pre> function, EventPriority priority) {
        generalRegister(function, ON_SCREEN_DRAW_PRE, priority);
    }

    public void registerOnScreenDrawPost(Consumer<ScreenDrawEvent.Post> function) {
        registerOnScreenDrawPost(function, EventPriority.Normal);
    }
    final Set<Consumer<ScreenDrawEvent.Post>> ON_SCREEN_DRAW_POST = new ConcurrentSet<>();
    public void registerOnScreenDrawPost(Consumer<ScreenDrawEvent.Post> function, EventPriority priority) {
        generalRegister(function, ON_SCREEN_DRAW_POST, priority);
    }

    public void registerOnContainerScreenDrawBackground(Consumer<ContainerScreenDrawEvent.Background> function) {
        registerOnContainerScreenDrawBackground(function, EventPriority.Normal);
    }
    final Set<Consumer<ContainerScreenDrawEvent.Background>> ON_CONTAINER_SCREEN_DRAW_BACKGROUND = new ConcurrentSet<>();
    public void registerOnContainerScreenDrawBackground(Consumer<ContainerScreenDrawEvent.Background> function, EventPriority priority) {
        generalRegister(function, ON_CONTAINER_SCREEN_DRAW_BACKGROUND, priority);
    }

    public void registerOnContainerScreenDrawForeground(Consumer<ContainerScreenDrawEvent.Foreground> function) {
        registerOnContainerScreenDrawForeground(function, EventPriority.Normal);
    }
    final Set<Consumer<ContainerScreenDrawEvent.Foreground>> ON_CONTAINER_SCREEN_DRAW_FOREGROUND = new ConcurrentSet<>();
    public void registerOnContainerScreenDrawForeground(Consumer<ContainerScreenDrawEvent.Foreground> function, EventPriority priority) {
        generalRegister(function, ON_CONTAINER_SCREEN_DRAW_FOREGROUND, priority);
    }

    public void registerOnGuiDraw(Consumer<GuiDrawEvent> function) {
        registerOnGuiDraw(function, EventPriority.Normal);
    }
    final Set<Consumer<GuiDrawEvent>> ON_GUI_DRAW = new ConcurrentSet<>();
    public void registerOnGuiDraw(Consumer<GuiDrawEvent> function, EventPriority priority) {
        generalRegister(function, ON_GUI_DRAW, priority);
    }

    public void registerOnGuiDrawPre(Consumer<GuiDrawEvent.Pre> function) {
        registerOnGuiDrawPre(function, EventPriority.Normal);
    }
    final Set<Consumer<GuiDrawEvent.Pre>> ON_GUI_DRAW_PRE = new ConcurrentSet<>();
    public void registerOnGuiDrawPre(Consumer<GuiDrawEvent.Pre> function, EventPriority priority) {
        generalRegister(function, ON_GUI_DRAW_PRE, priority);
    }

    public void registerOnGuiDrawPost(Consumer<GuiDrawEvent.Post> function) {
        registerOnGuiDrawPost(function, EventPriority.Normal);
    }
    final Set<Consumer<GuiDrawEvent.Post>> ON_GUI_DRAW_POST = new ConcurrentSet<>();
    public void registerOnGuiDrawPost(Consumer<GuiDrawEvent.Post> function, EventPriority priority) {
        generalRegister(function, ON_GUI_DRAW_POST, priority);
    }

    public void registerOnGuiDrawElement(Consumer<GuiDrawEvent.Element> function) {
        registerOnGuiDrawElement(function, EventPriority.Normal);
    }
    final Set<Consumer<GuiDrawEvent.Element>> ON_GUI_DRAW_ELEMENT = new ConcurrentSet<>();
    public void registerOnGuiDrawElement(Consumer<GuiDrawEvent.Element> function, EventPriority priority) {
        generalRegister(function, ON_GUI_DRAW_ELEMENT, priority);
    }


    //** STARTUP

    public void registerOnClientStarted(Consumer<ClientStartedEvent> function) {
        registerOnClientStarted(function, EventPriority.Normal);
    }

    public void registerOnClientStarted(Consumer<ClientStartedEvent> function, EventPriority priority) {
        generalRegister(function, ON_CLIENT_STARTED_EVENT, priority);
    }

    public void registerOnConnectedToServer(Consumer<ConnectedToServerEvent> function) {
        registerOnConnectedToServer(function, EventPriority.Normal);
    }

    public void registerOnConnectedToServer(Consumer<ConnectedToServerEvent> function, EventPriority priority) {
        generalRegister(function, ON_CONNECTED_TO_SERVER, priority);
    }

    public void registerOnDisconnectedFromServer(Consumer<DisconnectedFromServerEvent> function) {
        registerOnDisconnectedFromServer(function, EventPriority.Normal);
    }

    public void registerOnDisconnectedFromServer(Consumer<DisconnectedFromServerEvent> function, EventPriority priority) {
        generalRegister(function, ON_DISCONNECTED_FROM_SERVER, priority);
    }

    public void registerOnBeforeServerStart(Consumer<ServerStartingEvent> function) {
        registerOnBeforeServerStart(function, EventPriority.Normal);
    }

    public void registerOnBeforeServerStart(Consumer<ServerStartingEvent> function, EventPriority priority) {
        generalRegister(function, ON_BEFORE_SERVER_START, priority);
    }

    public void registerOnServerStop(Consumer<ServerStoppedEvent> function) {
        registerOnServerStop(function, EventPriority.Normal);
    }

    public void registerOnServerStop(Consumer<ServerStoppedEvent> function, EventPriority priority) {
        generalRegister(function, ON_SERVER_STOP, priority);
    }

    public void registerOnClientInput(Consumer<ClientInputEvent> function) {
        registerOnClientInput(function, EventPriority.Normal);
    }

    public void registerOnClientInput(Consumer<ClientInputEvent> function, EventPriority priority) {
        generalRegister(function, ON_CLIENT_INPUT, priority);
    }

    public void registerOnSimpleMessage(String messageId, Consumer<SimpleMessageEvent> function) {
        registerOnSimpleMessage(messageId, function, EventPriority.Normal);
    }

    public void registerOnSimpleMessage(String messageId, Consumer<SimpleMessageEvent> function, EventPriority priority) {
        ON_SIMPLE_MESSAGE.put(messageId, function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnRenderLevel(RenderLevelEvent.RenderStage stage, Consumer<RenderLevelEvent> function) {
        registerOnRenderLevel(stage, function, EventPriority.Normal);
    }

    private void registerOnRenderLevel(RenderLevelEvent.RenderStage stage, Consumer<RenderLevelEvent> function, EventPriority priority) {
        ON_RENDER_LEVEL.put(stage, function);
        PRIORITIES.put(function.hashCode(), priority);
    }

    public void registerOnDetermineActiveWaypoint(Consumer<DetermineActiveWaypointEvent> function) {
        registerOnDetermineActiveWaypoint(function, EventPriority.Normal);
    }

    public void registerOnDetermineActiveWaypoint(Consumer<DetermineActiveWaypointEvent> function, EventPriority priority) {
        ON_DETERMINE_ACTIVE_WAYPOINT.add(function);
        PRIORITIES.put(function.hashCode(), priority);
    }


    //** TICK EVENTS
    private void generalTickEventRegister(Consumer<?> function, Map<TickScheme, Consumer<?>> map, TickType type, EventPriority priority) {
        TickScheme scheme = new TickScheme(function, type);
        map.put(scheme, function);
        PRIORITIES.put(function.hashCode(), priority);
    }



    @SuppressWarnings("unchecked")
    public <T extends ClientTickEvent> void registerOnClientTick(TickType type, Consumer<T> function) {
        registerOnClientTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientTickEvent> void registerOnClientTick(TickType type, Consumer<T> function, EventPriority priority) {
        generalTickEventRegister(function, CLIENT_TICK_EVENTS, type, priority);
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientLevelTickEvent> void registerOnClientLevelTick(TickType type, Consumer<T> function) {
        registerOnClientLevelTick(type, function, EventPriority.Normal);
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientLevelTickEvent> void registerOnClientLevelTick(TickType type, Consumer<T> function, EventPriority priority) {
        generalTickEventRegister(function, CLIENT_LEVEL_TICK_EVENTS, type, priority);
    }

    /**
     * Custom Events
     **/
    public void onClientTick(Minecraft client) {
        if(client.player == null || client.level == null) return; //not in game
        ClientTickEvent event = new ClientTickEvent(ticks++);
        //LoggerBase.logDebug(null, "010001", "Client tick event: " + totalTicks);
        CLIENT_TICK_EVENTS.forEach((scheme, consumer) -> {
            if (ticks % scheme.getFrequency() == scheme.offset) {
                tryEvent((Consumer<ClientTickEvent>) consumer, event);
            }
        });
    }

    public void onClientLevelTick(Level level) {
        if(level == null) return; //not in game
        long totalTicks = level.getDayTime();
        ClientLevelTickEvent event = new ClientLevelTickEvent(level, totalTicks);

        ManagedChunkEvents.onWorldTickStart(level);
        //LoggerBase.logDebug(null, "010001", "Client level tick event: " + totalTicks);
        CLIENT_LEVEL_TICK_EVENTS.forEach((scheme, consumer) -> {
            if (totalTicks % scheme.getFrequency() == scheme.offset) {
                tryEvent((Consumer<ClientLevelTickEvent>) consumer, event);
            }
        });
    }

    public void onClientInput(ClientInputMessage message) {
        Player p = Minecraft.getInstance().player;
        ClientInputEvent event = new ClientInputEvent(p, message);
        ON_CLIENT_INPUT.forEach(consumer -> tryEvent(consumer, event));
    }

    public void onSimpleMessage(Player player, SimpleStringMessage message, String messageId) {
        SimpleMessageEvent event = new SimpleMessageEvent(player, message, messageId);
        Collection<Consumer<SimpleMessageEvent>> consumers = ON_SIMPLE_MESSAGE.get(messageId);

        // Sort consumers by priority
        List<Consumer<SimpleMessageEvent>> sortedConsumers = consumers.stream()
            .sorted((a, b) -> PRIORITIES.get(b.hashCode()).compareTo(PRIORITIES.get(a.hashCode())))
            .toList();

        // Execute in priority order
        for (Consumer<SimpleMessageEvent> consumer : sortedConsumers) {
            tryEvent(consumer, event);
        }
    }

    public void onDetermineActiveWaypoint(DetermineActiveWaypointEvent event) {
        if (ON_DETERMINE_ACTIVE_WAYPOINT.isEmpty()) return;
        for (Consumer<DetermineActiveWaypointEvent> consumer : ON_DETERMINE_ACTIVE_WAYPOINT) {
            tryEvent(consumer, event);
        }
    }

    public void onRenderLevel(RenderLevelEvent.RenderStage stage, DeltaTracker deltaTracker,
                              boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                              LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix)
    {
        // Skip this stage if it has previously thrown an exception
        if (renderLevelErrorStages.contains(stage)) return;
        Collection<Consumer<RenderLevelEvent>> consumers = ON_RENDER_LEVEL.get(stage);
        if(consumers.isEmpty()) return;

        // Update the static event instance with new values
        RENDER_LEVEL_EVENT.updateValues(stage, deltaTracker,
            renderBlockOutline, camera, gameRenderer,
            lightTexture, modelViewMatrix, projectionMatrix);

        for (Consumer<RenderLevelEvent> consumer : consumers) {
            try {
                consumer.accept(RENDER_LEVEL_EVENT);
            } catch (Exception e) {
                LoggerBase.logError(null, "RENDER_ERROR", "RenderLevelEvent stage " + stage.name() + " threw exception: " + e.getMessage());
                e.printStackTrace();
                renderLevelErrorStages.add(stage);
                break; // Stop processing this stage immediately
            }
        }
    }

    public void onClientStarted(ClientStartedEvent event) {
        ON_CLIENT_STARTED_EVENT.forEach(consumer -> tryEvent(consumer, event));
    }

    public void onGuiDraw(GuiDrawEvent event) {
        ON_GUI_DRAW.forEach(consumer -> tryEvent(consumer, event));
    }

    public void onGuiDrawPre(GuiDrawEvent.Pre event) {
        ON_GUI_DRAW_PRE.forEach(consumer -> tryEvent(consumer, event));
    }

    public void onGuiDrawPost(GuiDrawEvent.Post event) {
        ON_GUI_DRAW_POST.forEach(consumer -> tryEvent(consumer, event));
    }

    public void onGuiDrawElement(GuiDrawEvent.Element event) {
        ON_GUI_DRAW_ELEMENT.forEach(consumer -> tryEvent(consumer, event));
    }


        private <T> void tryEvent(Consumer<T> consumer, T event) {
            String id = consumer.toString() + "::" + event.getClass().getName();
            if( MixinManager.isEnabled(consumer.toString())) {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    MixinManager.recordError(id, e);
                }
            }
        }



    /**
     * ###############
     **/

    private class TickScheme {
        int offset;
        TickType frequency;

        <T> TickScheme(Consumer<T> func, TickType frequency) {
            this.frequency = frequency;
            this.offset = (func.hashCode() % getFrequency());
        }

        int getFrequency() {
            switch (frequency) {
                case ON_SINGLE_TICK:
                    return 1;
                case ON_20_TICKS:
                    return 20;
                case ON_120_TICKS:
                    return 120;
                case ON_1200_TICKS:
                    return 1200;
                case ON_24000_TICKS:
                    return 24000; // 1 day in ticks
                default:
                    return 1;
            }

        }

    }
}
//END CLASS
