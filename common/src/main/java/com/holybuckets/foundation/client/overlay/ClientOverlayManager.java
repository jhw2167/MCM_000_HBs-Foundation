package com.holybuckets.foundation.client.overlay;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class ClientOverlayManager {

    private static final List<OverlayPosition> FILL_ORDER = List.of(
        OverlayPosition.RIGHT_TOP,
        OverlayPosition.LEFT_TOP,
        OverlayPosition.RIGHT_MID,
        OverlayPosition.LEFT_MID,
        OverlayPosition.RIGHT_BOT,
        OverlayPosition.LEFT_BOT
    );

    private static final Comparator<Map.Entry<String, ClientOverlayInfo>> DRAW_ORDER =
        Comparator.<Map.Entry<String, ClientOverlayInfo>>comparingInt(e -> e.getValue().priority().ordinal())
            .thenComparing(Map.Entry::getKey);

    private static final Map<String, ClientOverlayInfo> OVERLAYS = new ConcurrentHashMap<>();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private ClientOverlayManager() {
    }

    public static void init(ClientEventRegistrar reg) {
        reg.registerOnGuiDrawPost(ClientOverlayManager::onGuiDrawPost);
    }

    public static void register(String modId, OverlayPriority priority, OverlayDrawer drawer) {
        register(modId, priority, drawer, () -> true);
    }

    public static void register(String modId, OverlayPriority priority, OverlayDrawer drawer, BooleanSupplier visible) {
        if (modId == null || priority == null || drawer == null || visible == null) return;
        OVERLAYS.put(modId, new ClientOverlayInfo(priority, drawer, visible));
        FAILED.remove(modId);
    }

    public static void unregister(String modId) {
        if (modId == null) return;
        OVERLAYS.remove(modId);
        FAILED.remove(modId);
    }

    public static Map<String, ClientOverlayInfo> getOverlays() {
        return Collections.unmodifiableMap(OVERLAYS);
    }

    private static void onGuiDrawPost(GuiDrawEvent.Post event) {
        if (event.getElement() != GuiDrawEvent.Element.ALL) return;
        if (OVERLAYS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.options.renderDebug) return;

        List<Map.Entry<String, ClientOverlayInfo>> ordered = new ArrayList<>(OVERLAYS.entrySet());
        ordered.sort(DRAW_ORDER);

        int slot = 0;
        for (Map.Entry<String, ClientOverlayInfo> entry : ordered) {
            if (slot >= FILL_ORDER.size()) return;
            ClientOverlayInfo info = entry.getValue();
            try {
                if (!info.visible().getAsBoolean()) continue;
                info.drawer().draw(event, FILL_ORDER.get(slot++));
            } catch (Exception e) {
                if (FAILED.add(entry.getKey())) {
                    LoggerBase.logError(null, "060001",
                        "Client overlay from " + entry.getKey() + " failed to draw: " + e);
                }
            }
        }
    }
}
