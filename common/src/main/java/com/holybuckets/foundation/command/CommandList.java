package com.holybuckets.foundation.command;

//Project imports

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.biome.BiomeAPI;
import com.holybuckets.foundation.biome.BiomeInfo;
import com.holybuckets.foundation.config.PerformanceImpactConfig;
import com.holybuckets.foundation.core.ChunkAnalyzer;
import com.holybuckets.foundation.core.ChunkAnalyzerManager;
import com.holybuckets.foundation.core.ChunkExplorerManager;
import com.holybuckets.foundation.config.ModConfig;
import com.holybuckets.foundation.core.ChunkAnalyzer.ChunkBlockDistro;
import com.holybuckets.foundation.core.MovingWaypoint;
import com.holybuckets.foundation.core.MovingWaypoint.WaypointInfo;
import com.holybuckets.foundation.event.CommandRegistry;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.structure.StructureAPI;
import com.holybuckets.foundation.structure.StructureInfo;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
        CommandRegistry.register(StructureTeleport::withStructure);

        CommandRegistry.register(NearestBiomes::noLimit);
        CommandRegistry.register(NearestBiomes::withLimit);
        CommandRegistry.register(NearestBiomesOfType::withTypeAndLimit);
        CommandRegistry.register(NearestDistinctBiomes::withLimit);
        CommandRegistry.register(AllBiomes::list);
        CommandRegistry.register(GetBiomesNearPoint::withCoordinates);
        CommandRegistry.register(GetBiomesNearPoint::withCoordinatesAndRadius);

        CommandRegistry.register(ListWaypoints::noArgs);
        CommandRegistry.register(DeleteWaypoint::byId);

        CommandRegistry.register(SetChunkExploreRate::withRate);
        CommandRegistry.register(SetMaxAllocationRate::withRate);
        CommandRegistry.register(SetChunkAnalyzeRate::withRate);
        CommandRegistry.register(ChunkExplorerHealthCheck::allLevels);
        CommandRegistry.register(ChunkExplorerHealthCheck::withDimension);

        CommandRegistry.register(ChunkAnalyze::withRadius);
        CommandRegistry.register(ChunkAnalyze::withRadiusAndLimit);
    }

    //**** SUGGETTIONS ****//


    private static void onLevelLoad(LevelLoadingEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            dimensionStructureTypes.remove(serverLevel.dimension());
        }
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

    private static final Map<ResourceKey<Level>, Set<ResourceLocation>> dimensionStructureTypes = new ConcurrentHashMap<>();
    private static final SuggestionProvider<CommandSourceStack> DIMENSION_STRUCTURE_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggest(
            getDimensionStructureTypes(context.getSource().getLevel()).stream()
                .map(ResourceLocation::toString)
                .sorted(),
            builder
        );

    private static Set<ResourceLocation> getDimensionStructureTypes(ServerLevel level) {
        return dimensionStructureTypes.computeIfAbsent(level.dimension(), key -> {
            Set<Holder<Biome>> biomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
            Set<ResourceLocation> types = new HashSet<>();
            for (Holder<StructureSet> set : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
                for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                    Holder<Structure> structure = entry.structure();
                    if (structure.value().biomes().stream().noneMatch(biomes::contains)) continue;
                    structure.unwrapKey().ifPresent(k -> types.add(k.location()));
                }
            }
            return types;
        });
    }


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

    private static class StructureTeleport {

        private static final long LOCATE_TIMEOUT_MS = 5000L;
        private static final int SEARCH_RADIUS_CHUNKS = 100;
        private static final int TELEPORT_HEIGHT = 150;
        private static final ExecutorService locateExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "HBs-StructureTeleport");
            thread.setDaemon(true);
            return thread;
        });

        private static LiteralArgumentBuilder<CommandSourceStack> withStructure() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("structureTeleport")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("structure", StringArgumentType.greedyString())
                        .suggests(DIMENSION_STRUCTURE_SUGGESTIONS)
                        .executes(context -> execute(context.getSource(),
                            StringArgumentType.getString(context, "structure")))
                    )
                );
        }

        private static int execute(CommandSourceStack source, String structureName) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            ServerLevel level = player.serverLevel();
            String dimension = level.dimension().location().toString();
            ResourceLocation structureId = ResourceLocation.tryParse(structureName.trim());
            if (structureId == null || !getDimensionStructureTypes(level).contains(structureId)) {
                source.sendFailure(Component.literal("Structure " + structureName + " does not generate in " + dimension));
                return 0;
            }

            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Optional<Holder.Reference<Structure>> holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, structureId));
            if (holder.isEmpty()) {
                source.sendFailure(Component.literal("Structure " + structureId + " is not registered"));
                return 0;
            }

            HolderSet<Structure> target = HolderSet.direct(holder.get());
            BlockPos origin = player.blockPosition();
            source.sendSuccess(() -> Component.literal("Locating " + structureId + "..."), false);

            CompletableFuture.supplyAsync(
                () -> level.getChunkSource().getGenerator().findNearestMapStructure(level, target, origin, SEARCH_RADIUS_CHUNKS, false),
                locateExecutor
            )
                .orTimeout(LOCATE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete((result, error) -> {
                    MinecraftServer server = level.getServer();
                    if (server.isRunning()) server.execute(() -> onLocateComplete(source, player, level, structureId, result, error));
                });

            return 1;
        }

        private static void onLocateComplete(CommandSourceStack source, ServerPlayer player, ServerLevel level, ResourceLocation structureId,
                                             Pair<BlockPos, Holder<Structure>> result, Throwable error) {
            if (error != null) {
                Throwable cause = (error instanceof CompletionException && error.getCause() != null) ? error.getCause() : error;
                if (cause instanceof TimeoutException)
                    source.sendFailure(Component.literal("Locating " + structureId + " timed out after " + (LOCATE_TIMEOUT_MS / 1000) + "s"));
                else
                    source.sendFailure(Component.literal("Error locating " + structureId + ": " + cause.getMessage()));
                return;
            }

            if (result == null) {
                source.sendFailure(Component.literal("No " + structureId + " found within " + SEARCH_RADIUS_CHUNKS + " chunks"));
                return;
            }

            if (player.isRemoved() || player.serverLevel() != level) {
                source.sendFailure(Component.literal("Player left " + level.dimension().location() + " before " + structureId + " was located"));
                return;
            }

            BlockPos found = result.getFirst();
            BlockPos destination = new BlockPos(found.getX(), TELEPORT_HEIGHT, found.getZ());
            player.teleportTo(level, destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, player.getYRot(), player.getXRot());
            source.sendSuccess(() -> Component.literal("Teleported to " + structureId + " at " + posString(destination)), false);
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


    //**** CONFIG ****//

    private static class SetChunkExploreRate {

        private static final int RATE_MIN = -1;
        private static final int RATE_MAX = 100;

        private static LiteralArgumentBuilder<CommandSourceStack> withRate() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("setChunkExploreRate")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("rate", IntegerArgumentType.integer(RATE_MIN, RATE_MAX))
                        .executes(context -> {
                            int rate = IntegerArgumentType.getInteger(context, "rate");
                            return execute(context.getSource(), rate);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int rate) {
            PerformanceImpactConfig config = GeneralConfig.getInstance().getPerformanceImpactConfig();
            if (config == null) {
                source.sendFailure(Component.literal("Performance config is not initialized yet"));
                return 0;
            }

            int previous = config.getChunkExploreRate();

            if (rate < 1) {
                config.setEnableChunkExplorer(false);
                source.sendSuccess(() -> Component.literal(
                    "Chunk explorer disabled. Set a rate of 1 or higher to turn it back on."
                ), true);
                return 1;
            }

            config.setEnableChunkExplorer(true);
            config.setChunkExploreRate(rate);


            final int applied = rate;
            source.sendSuccess(() -> Component.literal(
                "Chunk explore rate set to " + applied + " (was " + previous + "). 1 is the fastest."
            ), true);
            return 1;
        }
    }


    private static class ChunkExplorerHealthCheck {

        private static LiteralArgumentBuilder<CommandSourceStack> allLevels() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("chunkExplorerHealthCheck")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> executeAll(context.getSource()))
                );
        }

        private static LiteralArgumentBuilder<CommandSourceStack> withDimension() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("chunkExplorerHealthCheck")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> executeLevel(context.getSource(),
                            DimensionArgument.getDimension(context, "dimension")))
                    )
                );
        }

        private static int executeAll(CommandSourceStack source) {
            JsonObject all = ChunkExplorerManager.healthCheckAll();
            if (all.size() == 0) {
                source.sendFailure(Component.literal("No chunk explorer managers are loaded"));
                return 0;
            }
            for (String levelId : all.keySet()) {
                report(source, levelId, all.getAsJsonObject(levelId));
            }
            return 1;
        }

        private static int executeLevel(CommandSourceStack source, ServerLevel level) {
            JsonObject health = ChunkExplorerManager.healthCheck(level);
            String levelId = HBUtil.LevelUtil.toLevelId(level);
            if (health == null) {
                source.sendFailure(Component.literal("No chunk explorer manager for " + levelId));
                return 0;
            }
            report(source, levelId, health);
            return 1;
        }

        private static void report(CommandSourceStack source, String levelId, JsonObject health) {
            ChunkExplorerManager.logHealthCheck(levelId, health);
            String summary = String.format("Chunk Explorer %s: rate %d, explore queue %d, priority queue %d, initialized chunks %d.",
                levelId,
                health.get("exploreRate").getAsInt(),
                health.get("exploreQueue").getAsInt(),
                health.get("priorityQueue").getAsInt(),
                health.get("initializedChunks").getAsLong());
            source.sendSuccess(() -> Component.literal(summary), false);
        }
    }

    private static class SetMaxAllocationRate {

        private static final int RATE_MIN = 1;
        private static final int RATE_MAX = 100000;

        private static LiteralArgumentBuilder<CommandSourceStack> withRate() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("setMaxAllocationRate")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("megabytesPerSecond", IntegerArgumentType.integer(RATE_MIN, RATE_MAX))
                        .executes(context -> {
                            int rate = IntegerArgumentType.getInteger(context, "megabytesPerSecond");
                            return execute(context.getSource(), rate);
                        })
                    )
                );
        }

        private static int execute(CommandSourceStack source, int megabytesPerSecond) {
            PerformanceImpactConfig config = GeneralConfig.getInstance().getPerformanceImpactConfig();
            if (config == null) {
                source.sendFailure(Component.literal("Performance config is not initialized yet"));
                return 0;
            }

            int previous = config.getChunkExploreMaxAllocationRate();
            config.setChunkExploreMaxAllocationRate(megabytesPerSecond);

            final int applied = megabytesPerSecond;
            final double current =  800; // config.getAverageAllocationRateMb(60);
            source.sendSuccess(() -> Component.literal(
                "Chunk explore allocation budget set to " + applied + "MB/s (was " + previous
                    + "MB/s). Current 60s average is " + String.format("%.0f", current) + "MB/s."
            ), true);
            return 1;
        }
    }


    //**** CHUNK ANALYSIS ****//

    private static class ChunkAnalyze {

        private static LiteralArgumentBuilder<CommandSourceStack> withRadius() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("chunkAnalyze")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                        .executes(context -> execute(context.getSource(),
                            IntegerArgumentType.getInteger(context, "radius"), -1))
                    )
                );
        }

        private static LiteralArgumentBuilder<CommandSourceStack> withRadiusAndLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("chunkAnalyze")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(-1))
                            .executes(context -> execute(context.getSource(),
                                IntegerArgumentType.getInteger(context, "radius"),
                                IntegerArgumentType.getInteger(context, "limit")))
                        )
                    )
                );
        }

        private static int execute(CommandSourceStack source, int radius, int limit) {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Level level = player.level();
            ChunkPos pos = player.chunkPosition();
            int chunkX = pos.x;
            int chunkZ = pos.z;

            ChunkBlockDistro distro;
            try {
                distro = ChunkAnalyzer.areaBlockDistro(level, pos, radius);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error analyzing chunk " + chunkX + ", " + chunkZ + ": " + e.getMessage()));
                return 0;
            }

            if (distro == null) {
                source.sendFailure(Component.literal("Chunk " + chunkX + ", " + chunkZ + " is not loaded" + (radius > 0 ? " (nor any chunk within radius " + radius + ")" : "")));
                return 0;
            }

            ChunkAnalyzer.log(distro, radius, limit);
            source.sendSuccess(() -> Component.literal("Block counts for chunk " + chunkX + ", " + chunkZ + " have been logged to the server."), false);
            return 1;
        }
    }


    private static class SetChunkAnalyzeRate {

        private static LiteralArgumentBuilder<CommandSourceStack> withRate() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("setChunkAnalyzeRate")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("rate", IntegerArgumentType.integer(0))
                        .executes(context -> execute(context.getSource(),
                            IntegerArgumentType.getInteger(context, "rate")))
                    )
                );
        }

        private static int execute(CommandSourceStack source, int rate) {
            int previous = ChunkAnalyzerManager.getAnalyzeRate();
            ModConfig.getInstance().setAnalyzeRate(rate);
            String msg = (rate == 0)
                ? "Chunk analyzer disabled (was 1 in " + previous + ")."
                : "Chunk analyzer will log 1 in every " + rate + " loaded chunks (was " + previous + ").";
            source.sendSuccess(() -> Component.literal(msg), true);
            return 1;
        }
    }


    //**** STATIC UTILITY ****//

    static String posString(BlockPos pos) {
        return HBUtil.BlockUtil.positionToString(pos);
    }

}
//END CLASS COMMANDLIST
