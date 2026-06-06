package com.holybuckets.foundation.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ModContext {

    private static final ModContext INSTANCE = new ModContext();
    private final HashMap<String, ModData> mods = new HashMap<>();

    private ModContext() {}

    public static ModContext getInstance() {
        return INSTANCE;
    }

    // Called by loader-specific code (Forge/Fabric) during mod init
    public void register(String modId, String modName, String version) {
        mods.put(modId, new ModData(modId, modName, version));
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