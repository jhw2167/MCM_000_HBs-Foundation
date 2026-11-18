package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.structure.StructureInfo;
import net.minecraft.world.level.Level;

public class StructureLoadedEvent {
    private final StructureInfo structureInfo;
    private final boolean isFirstTimeLoaded;
    private final Level level;

    public StructureLoadedEvent(Level level, StructureInfo structureInfo, boolean isFirstTimeLoaded) {
        this.level = level;
        this.structureInfo = structureInfo;
        this.isFirstTimeLoaded = isFirstTimeLoaded;
    }

    public Level getLevel() { return level; }

    public StructureInfo getStructureInfo() {
        return structureInfo;
    }

    public boolean isFirstTimeLoaded() {
        return isFirstTimeLoaded;
    }
}
