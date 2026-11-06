package com.holybuckets.foundation.api;

import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.core.WaystoneTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;


/**
 * Only class that references Waystones types, so they are never loaded unless Waystones is present.
 */
public class WaystonesCompat {
    public static class WaystoneEntry {
        public final Level level;
        public final ResourceLocation type;
        public final BlockPos pos;

        public WaystoneEntry(Level level, ResourceLocation type, BlockPos pos) {
            this.level = level;
            this.type = type;
            this.pos = pos;
        }
    }

    public static List<WaystoneEntry> collectWaystones(MinecraftServer server) {

        return WaystonesAPI.getAllWaystones(server)
            .filter(w -> w.getWaystoneUid()!=null)
            .filter(w -> w.getDimension() != null)
            .filter(w -> w.getPos() != null)
            .map(w -> {
                ServerLevel l = server.getLevel(w.getDimension());
                return new WaystoneEntry(l, w.getWaystoneType(), w.getPos());
            })
            .toList();
    }

    public static boolean isWaystone(ResourceLocation location) {
        return location.equals(WaystoneTypes.WAYSTONE);
    }

}
