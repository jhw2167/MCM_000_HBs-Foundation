package com.holybuckets.foundation.command;

//Project imports

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
        CommandRegistry.register(NearestStructures::withLimit);
        CommandRegistry.register(NearestStructuresOfType::withTypeAndLimit);
        CommandRegistry.register(NearestDistinctStructures::withLimit);
    }

    //**** SUGGETTIONS ****//


    private static void onLevelLoad(LevelLoadingEvent event) {
        try {
            StructureAPI api = new StructureAPI( (Level) event.getLevel());
            validStructureTypes.addAll(api.getAllStructures());
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


    //**** END SUGGESTIONS ****//



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
                        String message = structure.getId().toString() + " at " + structure.getOrigin().toString();
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

        private static LiteralArgumentBuilder<CommandSourceStack> withTypeAndLimit() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("nearestStructuresOfType")
                    .then(Commands.argument("type", StringArgumentType.string())
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
                        String message = structure.getId().toString() + " at " + structure.getOrigin().toString();
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
                        String message = structure.getId().toString() + " at " + structure.getOrigin().toString();
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


}
//END CLASS COMMANDLIST
