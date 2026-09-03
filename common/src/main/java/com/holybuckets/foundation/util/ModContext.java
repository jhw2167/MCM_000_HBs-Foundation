package com.holybuckets.foundation.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ModContext {

    private static final ModContext INSTANCE = new ModContext();
    private final HashMap<String, ModData> mods = new HashMap<>();
    private final HashMap<String, List<Consumer<ModData>>> pending = new HashMap<>();

    private ModContext() {}

    public static ModContext getInstance() {
        return INSTANCE;
    }

    // Called by loader-specific code (Forge/Fabric) during mod init
    public void register(String modId, String modName, String version) {
        ModData data = new ModData(modId, modName, version);
        mods.put(modId, data);

        List<Consumer<ModData>> waiting = pending.remove(modId);
        if (waiting == null) return;
        for (Consumer<ModData> consumer : waiting) {
            accept(consumer, data);
        }
    }

    /**
     * Runs the consumer against the given mod, immediately if it is already registered,
     * otherwise once it registers. Never runs if the mod is never loaded.
     */
    public void whenLoaded(String modId, Consumer<ModData> consumer) {
        if (modId == null || consumer == null) return;

        ModData data = mods.get(modId);
        if (data != null) {
            accept(consumer, data);
            return;
        }
        pending.computeIfAbsent(modId, k -> new ArrayList<>()).add(consumer);
    }

    private void accept(Consumer<ModData> consumer, ModData data) {
        try {
            consumer.accept(data);
        } catch (Throwable t) {
            com.holybuckets.foundation.LoggerBase.logError(null, "040",
                "ModContext.whenLoaded handler failed for " + data.modId + ": " + t);
        }
    }

    public Optional<ModData> getMod(String modId) {
        return Optional.ofNullable(mods.get(modId));
    }

    public Optional<String> getVersion(String modId) {
        return getMod(modId).map(d -> d.version);
    }

    public boolean isLoaded(String modId) {
        return mods.containsKey(modId);
    }

    public Map<String, ModData> getAll() {
        return Collections.unmodifiableMap(mods);
    }

    // -------------------------------------------------------------------------

    public static class ModData {
        public final String modId;
        public final String modName;
        public final String version;

        public ModData(String modId, String modName, String version) {
            this.modId = modId;
            this.modName = modName;
            this.version = version;
        }

        @Override
        public String toString() {
            return modName + " (" + modId + ") v" + version;
        }
    }

}