package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.structure.StructureInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;

public class PlayerNearStructureEvent {
    private final Player player;
    private final StructureInfo structureInfo;
    private final Structure structure;
    private final LevelChunk chunk;

    public PlayerNearStructureEvent(Player player, StructureInfo structureInfo, Structure structure, LevelChunk chunk) {
        this.player = player;
        this.structureInfo = structureInfo;
        this.structure = structure;
        this.chunk = chunk;
    }

    public Player getPlayer() {
        return player;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public StructureInfo getStructureInfo() {
        return structureInfo;
    }

    public Structure getStructure() {
        return structure;
    }
}
