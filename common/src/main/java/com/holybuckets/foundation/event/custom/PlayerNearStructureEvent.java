package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.structure.StructureInfo;
import net.minecraft.world.entity.player.Player;

public class PlayerNearStructureEvent {
    private final Player player;
    private final StructureInfo structureInfo;

    public PlayerNearStructureEvent(Player player, StructureInfo structureInfo) {
        this.player = player;
        this.structureInfo = structureInfo;
    }

    public Player getPlayer() {
        return player;
    }

    public StructureInfo getStructureInfo() {
        return structureInfo;
    }
}
