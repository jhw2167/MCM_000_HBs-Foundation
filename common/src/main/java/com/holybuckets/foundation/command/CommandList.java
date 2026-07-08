package com.holybuckets.foundation.command;

//Project imports

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.biome.BiomeAPI;
import com.holybuckets.foundation.biome.BiomeInfo;
import com.holybuckets.foundation.core.MovingWaypoint;
import com.holybuckets.foundation.core.MovingWaypoint.WaypointInfo;
import com.holybuckets.foundation.event.CommandRegistry;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.structure.StructureAPI;
import com.holybuckets.foundation.structure.StructureInfo;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandList {

    public static final String CLASS_ID = "033";
    private static final String PREFIX = "hb";

    public static void init(EventRegistrar reg) {
        reg.registerOnLevelLoad(CommandList::onLevelLoad, EventPriority.Low);
    }

    public static void register() {
//        CommandRegistry.register(LocateClusters::noArgs);
//        CommandRegistry.register(LocateClusters::limitCount);
//        CommandRegistry.register(LocateClusters::limitCountSpecifyBlockType);
        CommandRegistry.register(NearestStructures::noLimit);
        CommandRegistry.register(NearestStructures::withLimit);
        CommandRegistry.register(NearestStructuresOfType::withTypeAndLimit);
        CommandRegistry.register(NearestDistinctStructures::withLimit);
        CommandRegistry.register(AllStructures::list);

        CommandRegistry.register(NearestBiomes::noLimit);
        CommandRegistry.register(NearestBiomes::withLimit);
        CommandRegistry.register(NearestBiomesOfType::withTypeAndLimit);
        CommandRegistry.register(NearestDistinctBiomes::withLimit);
        CommandRegistry.register(AllBiomes::list);
        CommandRegistry.register(GetBiomesNearPoint::withCoordinates);
        CommandRegistry.register(GetBiomesNearPoint::withCoordinatesAndRadius);

        CommandRegistry.register(ListWaypoints::noArgs);
        CommandRegistry.register(DeleteWaypoint::byId);
    }

    //**** SUGGETTIONS ****//


    private static void onLevelLoad(LevelLoadingEvent event) {
        try {
            StructureAPI api = new StructureAPI( (Level) event.getLevel());
            validStructureTypes.addAll(api.getAllStructures());

            BiomeAPI biomeAPI = new BiomeAPI((Level) event.getLevel());
            validBiomeTypes.addAll(biomeAPI.getAllBiomes());
        } catch (Exception e) {
            // Log error if needed
        }
    }

    private static final Set<ResourceLocation> validStructureTypes = new HashSet<>();
    private static final SuggestionProvider<CommandSourceStack> STRUCTURE_TYPE_SUGGESTIONS =
        (context, builder) -> {
            // Provide some common structure types as suggestions
            return SharedSuggestionProvider.suggest(
                validStructureTypes.stream().map(ResourceLocation::toString)
                , builder);
        };


    //**** STRUCTURES ****//

    //1. Locate Clusters
    private static class LocateClusters
    {
        // Register the base command with no arguments
        private static LiteralArgumentBuilder<CommandSourceStack> noArgs() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                    .executes(context -> execute(context.getSource(), -1, null)) // Default case (no args)
                );

        }

        // Register command with count argument
        private static LiteralArgumentBuilder<CommandSourceStack> limitCount() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int count = IntegerArgumentType.getInteger(context, "count");
                            return execute(context.getSource(), count, null);
                        })
                    )
            );
        }

        // Register command with both count and blockType OR just blockType
        private static LiteralArgumentBuilder<CommandSourceStack> limitCountSpecifyBlockType() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .then(Commands.argument("blockType", StringArgumentType.string())
                            .executes(context -> {
                                int count = IntegerArgumentType.getInteger(context, "count");
                                String blockType = StringArgumentType.getString(context, "blockType");
                                return execute(context.getSource(), count, blockType);
                            })
                        )
                    )
                    .then(Commands.argument("blockType", StringArgumentType.string())
                        .executes(context -> {
                            String blockType = StringArgumentType.getString(context, "blockType");
                            return execute(context.getSource(), -1, blockType);
                        })
                    )
            );
        }


        private static int execute(CommandSourceStack source, int count, String blockType)
        {

            return 0;
        }


    }
    //END COMMAND

    //2. Nearest Structures
    private static class NearestStructures {

        private static LiteralArgumentBuilder<CommandSourceStack> noLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestStructures")
                    .executes(context -> execute(context.getSource(), 1)) // Default limit of 10
                );
        }

        private static LiteralArgumentBuilder<CommandSourceStack> withLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestStructures")
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int limit = IntegerArgumentType.getInteger(context, "limit");
                            return execute(context.getSource(), limit);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            try {
                StructureAPI api = new StructureAPI(level);
                List<StructureInfo> structures = api.nearestStructures(playerPos, limit);

                if (structures.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No structures found. This command can only locate structures that have been discovered."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + structures.size() + " structures:"), false);
                    for (StructureInfo structure : structures) {
                        String message = structure.getId().toString() + " at " + posString(structure.getOrigin());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing structure data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }

    //3. Nearest Structures Of Type
    private static class NearestStructuresOfType {

        private static LiteralArgumentBuilder<CommandSourceStack> withType() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestStructuresOfType")
                    .then(Commands.argument("type", StringArgumentType.greedyString())
                        .suggests(STRUCTURE_TYPE_SUGGESTIONS)
                            .executes(context -> {
                                String type = StringArgumentType.getString(context, "type");
                                return execute(context.getSource(), type, 1);
                            })
                        )
                );
        }


        private static LiteralArgumentBuilder<CommandSourceStack> withTypeAndLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestStructuresOfType")
                    .then(Commands.argument("type", StringArgumentType.greedyString())
                        .suggests(STRUCTURE_TYPE_SUGGESTIONS)
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                String type = StringArgumentType.getString(context, "type");
                                int limit = IntegerArgumentType.getInteger(context, "limit");
                                return execute(context.getSource(), type, limit);
                            })
                        )
                    )
                );
        }

        private static int execute(CommandSourceStack source, String type, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            try {
                ResourceLocation structureType = new ResourceLocation(type);
                StructureAPI api = new StructureAPI(level);
                List<StructureInfo> structures = api.nearestStructuresOfType(playerPos, structureType, limit);

                if (structures.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No structures of type " + type + " found. This command can only locate structures that have been discovered."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + structures.size() + " structures of type " + type + ":"), false);
                    for (StructureInfo structure : structures) {
                        String message = structure.getId().toString() + " at " + posString(structure.getOrigin());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing structure data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }

    //4. Nearest Distinct Structures
    private static class NearestDistinctStructures {
        private static LiteralArgumentBuilder<CommandSourceStack> withLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestDistinctStructures")
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int limit = IntegerArgumentType.getInteger(context, "limit");
                            return execute(context.getSource(), limit);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            try {
                StructureAPI api = new StructureAPI(level);
                List<StructureInfo> structures = api.nearestStructuresDistinct(playerPos, limit);

                if (structures.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No distinct structures found. This command can only locate structures that have been discovered."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + structures.size() + " distinct structures:"), false);
                    for (StructureInfo structure : structures) {
                        String message = structure.getId().toString() + " at " + posString(structure.getOrigin());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing structure data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }

    //5. All Structures
    private static class AllStructures {
        private static LiteralArgumentBuilder<CommandSourceStack> list() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("allStructures")
                    .executes(context -> execute(context.getSource()))
                );
        }

        private static int execute(CommandSourceStack source) {
            if (validStructureTypes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No structure types available. Structure data may not be loaded yet."), false);
                return 0;
            }

            List<String> sortedStructures = validStructureTypes.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.toList());

            source.sendSuccess(() -> Component.literal("Available structure types (" + sortedStructures.size() + "):"), false);
            for (String structure : sortedStructures) {
                source.sendSuccess(() -> Component.literal("  " + structure), false);
            }

            return 1;
        }
    }


    //**** BIOMES ****//


    private static final Set<ResourceLocation> validBiomeTypes = new HashSet<>();
    private static final SuggestionProvider<CommandSourceStack> BIOME_TYPE_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggest(
            validBiomeTypes.stream().map(ResourceLocation::toString),
            builder
        );

    private static final SuggestionProvider<CommandSourceStack> WAYPOINT_ID_SUGGESTIONS =
        (context, builder) -> {
            if (!(context.getSource().getEntity() instanceof ServerPlayer sp)) {
                return SharedSuggestionProvider.suggest(new String[0], builder);
            }
            return SharedSuggestionProvider.suggest(
                MovingWaypoint.getAllWaypoints(sp).stream()
                    .map(w -> Integer.toString(w.waypointId)),
                builder
            );
        };


    //1. Nearest Biomes
    private static class NearestBiomes {

        private static LiteralArgumentBuilder<CommandSourceStack> noLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestBiomes")
                    .executes(context -> execute(context.getSource(), 5))
                );
        }

        private static LiteralArgumentBuilder<CommandSourceStack> withLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestBiomes")
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int limit = IntegerArgumentType.getInteger(context, "limit");
                            return execute(context.getSource(), limit);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            try {
                BiomeAPI api = new BiomeAPI(level);
                List<BiomeInfo> biomes = api.nearestBiomes(playerPos, limit);

                if (biomes.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No biomes found. This command can only locate biomes in chunks that have been loaded."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + biomes.size() + " biomes:"), false);
                    for (BiomeInfo biome : biomes) {
                        String message = biome.getId().toString() + " at " + posString(biome.getSamplePos());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing biome data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }

    //2. Nearest Biomes Of Type
    private static class NearestBiomesOfType {

        private static LiteralArgumentBuilder<CommandSourceStack> withTypeAndLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestBiomesOfType")
                    .then(Commands.argument("biomeName", StringArgumentType.greedyString())
                        .suggests(BIOME_TYPE_SUGGESTIONS)
                        .then(Commands.argument("limit", IntegerArgumentType.integer(5))
                            .executes(context -> {
                                String type = StringArgumentType.getString(context, "biomeName");
                                int limit = IntegerArgumentType.getInteger(context, "limit");
                                return execute(context.getSource(), type, limit);
                            })
                        )
                    )
                );
        }

        private static int execute(CommandSourceStack source, String type, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            try {
                ResourceLocation biomeType = new ResourceLocation(type);
                BiomeAPI api = new BiomeAPI(level);
                List<BiomeInfo> biomes = api.nearestBiomesOfType(playerPos, biomeType, limit);

                if (biomes.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No biomes of type " + type + " found. This command can only locate biomes in chunks that have been loaded."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + biomes.size() + " biomes of type " + type + ":"), false);
                    for (BiomeInfo biome : biomes) {
                        String message = biome.getId().toString() + " at " + posString(biome.getSamplePos());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing biome data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }

    //3. Nearest Distinct Biomes
    private static class NearestDistinctBiomes {

        private static LiteralArgumentBuilder<CommandSourceStack> withLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestDistinctBiomes")
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int limit = IntegerArgumentType.getInteger(context, "limit");
                            return execute(context.getSource(), limit);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            try {
                BiomeAPI api = new BiomeAPI(level);
                List<BiomeInfo> biomes = api.nearestBiomesDistinct(playerPos, limit);

                if (biomes.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No distinct biomes found. This command can only locate biomes in chunks that have been loaded."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + biomes.size() + " distinct biomes:"), false);
                    for (BiomeInfo biome : biomes) {
                        String message = biome.getCommonName() + " (" + biome.getId() + ") at " + posString(biome.getSamplePos());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing biome data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }

    //4. All Biomes
    private static class AllBiomes {

        private static LiteralArgumentBuilder<CommandSourceStack> list() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("allBiomes")
                    .executes(context -> execute(context.getSource()))
                );
        }

        private static int execute(CommandSourceStack source) {
            if (validBiomeTypes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No biome types available. Biome data may not be loaded yet."), false);
                return 0;
            }

            List<String> sortedBiomes = validBiomeTypes.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.toList());

            source.sendSuccess(() -> Component.literal("Available biome types (" + sortedBiomes.size() + "):"), false);
            for (String biome : sortedBiomes) {
                source.sendSuccess(() -> Component.literal("  " + biome), false);
            }

            return 1;
        }
    }

    //5. Get Biomes Near Point
    private static class GetBiomesNearPoint {

        private static LiteralArgumentBuilder<CommandSourceStack> withCoordinates() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("getBiomesNearPoint")
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(context -> {
                                    int x = IntegerArgumentType.getInteger(context, "x");
                                    int y = IntegerArgumentType.getInteger(context, "y");
                                    int z = IntegerArgumentType.getInteger(context, "z");
                                    return execute(context.getSource(), x, y, z, 5); // Default chunk radius of 5
                                })
                            )
                        )
                    )
                );
        }

        private static LiteralArgumentBuilder<CommandSourceStack> withCoordinatesAndRadius() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("getBiomesNearPoint")
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkRadius", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        int x = IntegerArgumentType.getInteger(context, "x");
                                        int y = IntegerArgumentType.getInteger(context, "y");
                                        int z = IntegerArgumentType.getInteger(context, "z");
                                        int chunkRadius = IntegerArgumentType.getInteger(context, "chunkRadius");
                                        return execute(context.getSource(), x, y, z, chunkRadius);
                                    })
                                )
                            )
                        )
                    )
                );
        }

        private static int execute(CommandSourceStack source, int x, int y, int z, int chunkRadius) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            BlockPos targetPos = new BlockPos(x, y, z);

            try {
                BiomeAPI api = new BiomeAPI(level);
                List<BiomeInfo> biomes = api.getBiomesInChunkRange(targetPos, chunkRadius);

                if (biomes.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No biomes found near position " + posString(targetPos) + " within " + chunkRadius + " chunk radius. This command can only locate biomes in chunks that have been loaded."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Found " + biomes.size() + " biomes near position " + posString(targetPos) + " within " + chunkRadius + " chunk radius:"), false);
                    for (BiomeInfo biome : biomes) {
                        String message = biome.getId().toString() + " at " + posString(biome.getSamplePos());
                        source.sendSuccess(() -> Component.literal(message), false);
                    }
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error accessing biome data: " + e.getMessage()));
                return 0;
            }

            return 1;
        }
    }




    //**** WAYPOINTS ****//

    //1. List Waypoints
    private static class ListWaypoints {

        private static LiteralArgumentBuilder<CommandSourceStack> noArgs() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("listWaypoints")
                    .executes(context -> execute(context.getSource()))
                );
        }

        private static int execute(CommandSourceStack source) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Collection<WaypointInfo> waypoints = MovingWaypoint.getAllWaypoints(player);
            if (waypoints.isEmpty()) {
                source.sendSuccess(() -> Component.literal("You have no waypoints."), false);
                return 1;
            }

            source.sendSuccess(() -> Component.literal("You have " + waypoints.size() + " waypoint(s):"), false);
            for (WaypointInfo w : waypoints) {
                String name = (w.nameTag != null && !w.nameTag.isEmpty()) ? " \"" + w.nameTag + "\"" : "";
                String perm = w.isPermanent ? " [permanent]" : "";
                String message = "  id=" + w.waypointId
                    + " color=" + w.colorId
                    + " @ " + posString(w.targetPos)
                    + name + perm;
                source.sendSuccess(() -> Component.literal(message), false);
            }
            return 1;
        }
    }

    //2. Delete Waypoint By Id
    private static class DeleteWaypoint {

        private static LiteralArgumentBuilder<CommandSourceStack> byId() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("deleteWaypoint")
                    .then(Commands.argument("waypointId", IntegerArgumentType.integer(0))
                        .suggests(WAYPOINT_ID_SUGGESTIONS)
                        .executes(context -> {
                            int id = IntegerArgumentType.getInteger(context, "waypointId");
                            return execute(context.getSource(), id);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int waypointId) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            WaypointInfo match = null;
            for (WaypointInfo w : MovingWaypoint.getAllWaypoints(player)) {
                if (w.waypointId == waypointId) { match = w; break; }
            }

            MovingWaypoint.removeWaypointById(player, waypointId);
            final WaypointInfo removed = match;

            //write a case if match is null
            source.sendSuccess(() -> Component.literal(
                "Did not find Waypoint on server, but sent request to remove waypoint id=" + waypointId), false);

            source.sendSuccess(() -> Component.literal(
                "Removed waypoint id=" + removed.waypointId
                + " color=" + removed.colorId
                + " @ " + posString(removed.targetPos)
            ), false);
            return 1;
        }
    }


    //**** STATIC UTILITY ****//

    static String posString(BlockPos pos) {
        return HBUtil.BlockUtil.positionToString(pos);
    }

}
//END CLASS COMMANDLIST
