package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.structure.StructureInfo;

public class StructureLoadedEvent {
    private final StructureInfo structureInfo;
    private final boolean isFirstTimeLoaded;

    public StructureLoadedEvent(StructureInfo structureInfo, boolean isFirstTimeLoaded) {
        this.structureInfo = structureInfo;
        this.isFirstTimeLoaded = isFirstTimeLoaded;
    }

    public StructureInfo getStructureInfo() {
        return structureInfo;
    }

    public boolean isFirstTimeLoaded() {
        return isFirstTimeLoaded;
    }
}
