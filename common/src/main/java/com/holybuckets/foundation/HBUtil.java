package com.holybuckets.foundation;


import com.google.gson.*;
import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.exception.NoDefaultConfig;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import com.holybuckets.foundation.player.ManagedPlayer;
import com.mojang.authlib.GameProfile;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.blay09.mods.balm.api.BalmRegistries;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
* Class: HolyBucketsUtility
* Description: This class will contain utility methods amd objects that I find myself using frequently
*
 */
public class HBUtil {

    public static final String CLASS_ID = "004";


    public static class PlayerUtil {


        public static List<ServerPlayer> getAllPlayers() {
            MinecraftServer server = GeneralConfig.getInstance().getServer();
            if (server == null)
                return Collections.emptyList();
            return server.getPlayerList().getPlayers();
        }


        public static List<ServerPlayer> getAllPlayersInBlockRange(BlockPos center, int range) {
            if (range < 1)
                return Collections.emptyList();

            List<ServerPlayer> players = getAllPlayers();
            List<ServerPlayer> playersInRange = new ArrayList<>();
            for (ServerPlayer player : players) {
                if (player.blockPosition().distSqr(center) <= range * range) {
                    playersInRange.add(player);
                }
            }
            return playersInRange;
        }

        public static List<ServerPlayer> getAllPlayersInChunkRange(ChunkAccess center, int range) {
            List<ServerPlayer> players = getAllPlayers();
            List<ServerPlayer> playersInRange = new ArrayList<>();
            BlockPos centerPos = center.getPos().getWorldPosition();
            for (ServerPlayer player : players) {
                if (getAllPlayersInBlockRange(centerPos, range * 16).contains(player)) {
                    playersInRange.add(player);
                }
            }
            return playersInRange;
        }


        public static String getId(Player p) {
            GameProfile gp = p.getGameProfile();
            if( gp == null ) {
                LoggerBase.logError( null, "004000", "Error getting player id, game profile is null");
                return null;
            }
            String prefix = "CLIENT:";
            if( p instanceof ServerPlayer ) {
                prefix = "SERVER:";
            }
            return prefix + gp.getName().toString();
        }

    //create a PlayerNameSpace for CLIENT and SERVER
        public enum PlayerNameSpace {
            SERVER,
            CLIENT
        }
        public static Player getPlayer(String id, PlayerNameSpace nameSpace) {

             if( id == null || id.isEmpty() || nameSpace == null ) {
                 return null;
             }
            String playerName = id.replace("CLIENT:", "").replace("SERVER:", "");
            if( nameSpace == PlayerNameSpace.CLIENT ) {
                return ManagedPlayer.PLAYERS.get("CLIENT:" + playerName).getPlayer();
            } else if( nameSpace == PlayerNameSpace.SERVER ) {
                return ManagedPlayer.PLAYERS.get("SERVER:" + playerName).getServerPlayer();
            }
            return null;
        }

    }

    public static class ItemUtil {

        public static String itemToString(Item item) {
            if( item == null ) {
                LoggerBase.logError( null, "004000", "Error parsing item to string, item is null");
                return null;
            }
            return item.toString().replace("Item{", "").replace("}", "");
        }

        public static Item itemNameToItem(String qualifiedItemName) {
            String[] parts = qualifiedItemName.split(":");
            if( parts.length < 2 ) {
             return itemNameToItem("minecraft", qualifiedItemName);
            }
            return itemNameToItem(parts[0], parts[1]);
        }

        public static Item itemNameToItem(String nameSpace, String itemName)
        {
            if( itemName == null || itemName.isEmpty() ) {
                LoggerBase.logError( null, "004001", "Error parsing item name as string into a Minecraft Item type, " +
                    "type provided was null or provided as empty string");
                return null;
            }

            ResourceLocation itemKey = new ResourceLocation(nameSpace.trim(), itemName.trim());
            Item item = BuiltInRegistries.ITEM.get(itemKey);

            if( item == null ) {
                LoggerBase.logError( null, "004002", "Error parsing item name as string into a Minecraft Item type, " +
                    "item name provided was not found in Minecraft Item registry: " + itemName);
            }

            return item;
        }
    }

    public static class BlockUtil {

        public static Vec3 toVec3(BlockPos pos ) {
            if( pos == null ) return null;
            return new Vec3(pos.getX(), pos.getY(), pos.getZ());
        }

        public static BlockPos toBlockPos(Vec3 pos) {
            if( pos == null ) return null;
            return new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
        }

        /**
         * Convert a block to its string name for formatting
         * @param blockType - Block object
         * @return String formatted like Block{minecraft:iron_ore}
         */
        public static String blockToString(Block blockType)
        {
            if( blockType == null)
            {
                LoggerBase.logError( null, "004000", "Error parsing blockType to string, blockType is null");
                return null;
            }

            return blockType.toString().replace("Block{", "").replace("}", "");
        }

        /**
         * Convert a block name as a string to a Minecraft Block Object: eg. "minecraft:iron_ore" to Blocks.IRON_ORE
         * @param blockName - String name of the block
         * @return Block object
         */
        public static Block blockNameToBlock(String blockName)
        {
            if( blockName == null || blockName.isEmpty() )
            {
                LoggerBase.logError( null, "004001", "Error parsing block name as string into a Minecraft Block type, " +
                    "type provided was null or provided as empty string");
                return ModBlocks.empty;
            }

            if( blockName.contains(":") )
            {
                String[] parts = blockName.split(":");
                return blockNameToBlock(parts[0], parts[1]);
            }

            return blockNameToBlock("minecraft", blockName);
        }

        /**
         * Convert a block name as a string to a Minecraft Block Object: eg. "minecraft:iron_ore" to Blocks.IRON_ORE
         * @param blockName - String name of the block
         * @param namespace - Namespace of the block
         * @return Block object
         */
        public static Block blockNameToBlock(String namespace, String blockName)
        {
            if( blockName == null || blockName.isEmpty() )
            {
                LoggerBase.logError( null, "004001", "Error parsing block name as string into a Minecraft Block type, " +
                    "type provided was null or provided as empty string");
                return null;
            }

            if( namespace == null || namespace.isEmpty() )
               namespace = "minecraft";

            BalmRegistries registries = Balm.getRegistries();
            ResourceLocation blockKey = new ResourceLocation(namespace.trim(), blockName.trim());
            Block b = registries.getBlock(blockKey);

            if( b == null )
            {
                LoggerBase.logError( null, "004002", "Error parsing block name as string into a Minecraft Block type, " +
                    "block name provided was not found in Minecraft Block registry: " + blockName);
            }

            return b;
        }

        public static String positionToString(Vec3i pos)
        {
            if( pos == null)
                return null;
            return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
        }

        //write stringToBlockPos method to reverse the above, return Vec3i
        public static Vec3i stringToBlockPos(String posString)
        {
            if( posString == null || posString.isEmpty() )
                return null;

            posString = posString.replace("[", "").replace("]", "");
            String[] parts = posString.split(",");
            if( parts.length != 3 ) {
                return null;
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());

            return new BlockPos(x, y, z);
        }

        /**
         * If one or more coordinates are specified as null, any will match
         * @param all
         * @param x
         * @param y
         * @param z
         * @return
         */
        public static List<Pair<BlockState, BlockPos>> findBlockPos(List<Pair<BlockState, BlockPos>> all, Integer x, Integer y, Integer z) {
            List<Pair<BlockState, BlockPos>> found = new ArrayList<>();
            for(Pair<BlockState, BlockPos> p : all) {
                if( (x == null || p.getRight().getX() == x) &&
                    (y == null || p.getRight().getY() == y) &&
                    (z == null || p.getRight().getZ() == z) )
                {
                    found.add(p);
                }
            }
            return found;
        }

        /**
         * Returns true if the given pair is within the interger block range
         * @param p
         * @param center
         * @param radius
         * @return
         */
        public static boolean inRange(Pair<BlockState, BlockPos> p, BlockPos center,  int radius) {
            return inRange(p.getRight(), center, radius);
        }

        public static boolean inRange(BlockPos p, BlockPos center,  int radius) {
            if( p == null ) return false;
            if( center == null ) center = new BlockPos(0, 0, 0);
            return p.distSqr(center) <= radius * radius;
        }

        public static int distanceSqr(BlockPos p1, BlockPos p2) {
            return (int) p1.distSqr(p2);
        }

        public static Map<BlockState, List<BlockPos>> condenseBlockStates(List<Pair<BlockState,BlockPos>> blockStates)
        {
            Map<BlockState, List<BlockPos>> blocks = new HashMap<>();
            for(Pair<BlockState, BlockPos> pair : blockStates)
            {
                blocks.putIfAbsent(pair.getLeft(), new ArrayList<>());
                blocks.get(pair.getLeft()).add(pair.getRight());
            }
            return blocks;
        }

        public static List<Pair<BlockState, BlockPos>> expandBlockStates(Map<BlockState, List<BlockPos>> blocks)
        {
            List<Pair<BlockState, BlockPos>> blockStates = new ArrayList<>();
            for(BlockState block : blocks.keySet())
            {
                for(BlockPos pos : blocks.get(block))
                {
                    blockStates.add(Pair.of(block, pos));
                }
            }
            return blockStates;
        }

        public static String serializeBlockPos(List<BlockPos> list) {
            //hijack serialize BlockPairs and ModBlocks.Empty
            if( list == null || list.isEmpty() )
                return "[]";

            Map<Block, List<BlockPos>> blocks = new HashMap<>();
            blocks.putIfAbsent(ModBlocks.empty, list);
            return serializeBlockPairs(blocks);
        }

        public static List<BlockPos> deserializeBlockPos(String data) {
            //hijack deserialize BlockPairs and ModBlocks.Empty
            if( data == null || data.isEmpty() || data.equals("[]") )
                return Collections.emptyList();

            Map<Block, List<BlockPos>> blocks = deserializeBlockPairs(data);
            if( blocks.isEmpty() )
                return Collections.emptyList();

            return blocks.getOrDefault(ModBlocks.empty, Collections.emptyList());
        }

        /**
         * Serialize a map of blocks and their positions to a string
         * @param blocks - Map of blocks and their positions
         * @return serialized string of block, position collections
         */
        public static String serializeBlockPairs(Map<Block, List<BlockPos>> blocks)
        {
            StringBuilder blockUpdates = new StringBuilder();
            for(Block block : blocks.keySet())
            {
                blockUpdates.append("{");
                String blockName = BlockUtil.blockToString(block);
                blockUpdates.append(blockName);
                blockUpdates.append("=");

                List<BlockPos> positions = blocks.get(block);
                if( positions.isEmpty() )
                {
                    blockUpdates.append("[]}, ");
                    continue;
                }

                for(BlockPos pos : positions)
                {
                    TripleInt vec = new TripleInt(pos);
                    blockUpdates.append( ("[" + vec.x + "," + vec.y + "," + vec.z + "]") );
                    blockUpdates.append("&");
                }
                blockUpdates.deleteCharAt(blockUpdates.length() - 1);
                blockUpdates.append("}, ");
            }
            //remove trailing comma and space
            blockUpdates.deleteCharAt(blockUpdates.length() - 1);
            blockUpdates.deleteCharAt(blockUpdates.length() - 1);

            return blockUpdates.toString();
        }

        /**
         * Deserialize a string to a map of blocks and all their positions
         * @param data - serialized string of block, position collections
         * @return Map of blocks and their positions
         */
        public static Map<Block, List<BlockPos>> deserializeBlockPairs(String data)
        {
            Map<Block, List<BlockPos>> blockPairs = new HashMap<>();
            String[] blocks = data.split(", ");

            for (String pairs : blocks)
            {
                //remove curly braces
                pairs = pairs.substring(1, pairs.length() - 1);

                String[] parts = pairs.split("=");
                String[] blockParts = parts[0].split(":");
                Block blockType = BlockUtil.blockNameToBlock(blockParts[0], blockParts[1]);

                blockPairs.put(blockType, new ArrayList<>());
                String[] positions = parts[1].split("&");

                if( positions[0].contains("[]") )
                    continue;

                for( String pos : positions )
                {
                    String[] vec = pos.replace("[", "").replace("]","").split(",");
                    int x = Integer.parseInt(vec[0]);
                    int y = Integer.parseInt(vec[1]);
                    int z = Integer.parseInt(vec[2]);
                    BlockPos blockPos = new BlockPos(x, y, z);
                    blockPairs.get(blockType).add(blockPos);
                }

            }

            return blockPairs;
        }

        public static String serializeBlockStatePairs(Map<BlockState, List<BlockPos>> blocks) {
            //Create Map<Block, List<BlockPos>> from Map<BlockState, List<BlockPos>>
            Map<Block, List<BlockPos>> blockMap = new HashMap<>();
            for(BlockState state : blocks.keySet()) {
                blockMap.put(state.getBlock(), blocks.get(state));
            }
            return serializeBlockPairs(blockMap);
        }

        public static Map<BlockState, List<BlockPos>> deserializeBlockStatePairs(String data) {
            Map<Block, List<BlockPos>> blockMap = deserializeBlockPairs(data);
            Map<BlockState, List<BlockPos>> stateMap = new HashMap<>();
            for(Block block : blockMap.keySet()) {
                stateMap.put(block.defaultBlockState(), blockMap.get(block));
            }
            return stateMap;
        }


        public static int mapTo1DNumber(TripleInt p) {
            return mapTo1DNumber(new BlockPos(p.x,p.y,p.z));
        }

        /**
         * Takes a blockPos - a 3D entity and maps it to a unique 1D number
         * @param p
         * @return
         */
        public static int mapTo1DNumber(BlockPos p) {
            // Zigzag encoding: turns negative numbers into unique positive integers
            int x = (p.getX() << 1) ^ (p.getX() >> 31);
            int y = (p.getY() << 1) ^ (p.getY() >> 31);
            int z = (p.getZ() << 1) ^ (p.getZ() >> 31);

            // Szudzik pairing function for combining two ints into one (no overflow worries)
            long pairXY = (x >= y) ? (long)x * x + x + y : (long)y * y + x;
            long pairXYZ = ((int)pairXY >= z) ? (long)((int)pairXY) * ((int)pairXY) + (int)pairXY + z
                : (long)z * z + (int)pairXY;

            // Compress to 32-bit int with simple folding (optional)
            return (int)(pairXYZ ^ (pairXYZ >>> 32));
        }


    }
    //END BLOCK UTIL

    public static class LevelUtil {

        public enum LevelNameSpace {
            CLIENT,
            SERVER
        }
        @Nullable
        public static Level toLevel(LevelNameSpace nameSpace, String dimensionId)
        {
            String levelId = dimensionId.replace("CLIENT:", "").replace("SERVER:", "");
            if( nameSpace == LevelNameSpace.CLIENT ) {
                levelId = "CLIENT:" + levelId;
            } else if( nameSpace == LevelNameSpace.SERVER ) {
                levelId = "SERVER:" + levelId;
            }
            return (Level) toLevel(levelId);
        }

        public static Level toLevel(LevelNameSpace nameSpace, ResourceKey<Level> key) {
            return toLevel(nameSpace, key.location());
        }

        public static Level toLevel(LevelNameSpace nameSpace, ResourceLocation loc) {
            if (loc == null) {
                return null;
            }
            return toLevel(nameSpace, loc.toString());
        }


        public static ServerLevel toServerLevel(String id) {
            return (ServerLevel) toLevel(LevelNameSpace.SERVER, id);
        }

        public static ClientLevel toClientLevel(String id) {
            return (ClientLevel) toLevel(LevelNameSpace.CLIENT, id);
        }

        private static LevelAccessor toLevel(String id) {
            return GeneralConfig.getInstance().getLevel(id);
        }

        public static String toLevelId(LevelAccessor level)
        {
            if (level == null)
                return null;
            String levelName = ((Level) level).dimension().location().toString();
            if(level.isClientSide()) {
                return "CLIENT:" + levelName;
            } else {
                return "SERVER:" + levelName;
            }
        }

        public static boolean testLevel(Level level, String levelNameSpace, String levelId) {
            return testLevel(level, new ResourceLocation(levelNameSpace + ":" + levelId));
        }

        public static boolean testLevel(Level level, ResourceLocation location) {
            return level.dimension().location().equals(location);
        }
    }

    private static LevelChunk threadedChunkResult = null;

    public static class ChunkUtil {

        public static String getId(ChunkAccess chunk) {
            return chunk.getPos().x + "," + chunk.getPos().z;
        }

        public static String getId(int x, int z) {
            return x + "," + z;
        }

        public static String getId( ChunkPos pos ) {
            return pos.x + "," + pos.z;
        }

        public static String getId( BlockPos pos ) {
            return getId( Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16) );
        }

        public static BlockPos getWorldPos(String id) {
            return getChunkPos(id).getWorldPosition();
        }


        public static ChunkPos getChunkPos(String id) {
            String[] parts = id.split(",");
            return new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        public static ChunkPos getChunkPos(BlockPos pos) {
            String id = getId(pos);
            String[] parts = id.split(",");
            return new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        /** Check if chunk is within [-x, x] and [-z, z] **/
        public static boolean checkInBounds(ChunkAccess chunk, int x, int z) {
            return chunk.getPos().x >= -x && chunk.getPos().x <= x && chunk.getPos().z >= -z && chunk.getPos().z <= z;
        }

        public static int chunkDistSquared(ChunkPos p1, ChunkPos p2) {
            return (p1.x - p2.x) * (p1.x - p2.x) + (p1.z - p2.z) * (p1.z - p2.z);
        }

        public static float chunkDist(ChunkPos p1, ChunkPos p2) {
            return (float) Math.sqrt(chunkDistSquared(p1, p2));
        }

        //override with string Id args
        public static float chunkDist(String id1, String id2) {
            return chunkDist(getChunkPos(id1), getChunkPos(id2));
        }

        public static ChunkPos posAdd(ChunkPos p, int x, int z) {
            return new ChunkPos(p.x + x, p.z + z);
        }

        public static ChunkPos posAdd(ChunkPos p1, ChunkPos p2) {
            return new ChunkPos(p1.x + p2.x, p1.z + p2.z);
        }

        public static ChunkPos posAdd(ChunkPos p1,  int[] dir ) {
            return new ChunkPos(p1.x + dir[0], p1.z + dir[1]);
        }

        public static Long getChunkPos1DMap(String id) {
            return getChunkPos1DMap(getChunkPos(id));
        }

        public static Long getChunkPos1DMap(ChunkPos pos ) {
            //map the chunks x and z position to a random number
            final Long WIDTH = 10_000_000l;
            return (pos.x * WIDTH) + pos.z;
        }

        /**
         * Gets all chunk ids for all chunks around the provided chunk
         * @param radius
         * @param center
         * @return
         */
        public static List<String> getLocalChunkIds(ChunkPos center, int radius) {
            List<String> ids = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    ids.add(getId(posAdd(center, x, z)));
                }
            }
            return ids;
        }


        /*
        public static String readNBT(ChunkAccess chunk, String property) {

            return "";
        }



        //public static String writeNBT(ChunkAccess chunk, String property, String value) {
            return "";
        }
        */


        /**
         * Loads the targeted chunk, forceloading if necessary. Force loading may
         * cause a circular dependency during worldLoad, first call is getChunkNow call
         * to protect against this. If not force loading, uses a threaded approach with
         * 10ms timeout to prevent deadlocks during world load.
         * @param level
         * @param x
         * @param z
         * @return
         */
        public static LevelChunk getLevelChunk(LevelAccessor level, int x, int z, boolean forceLoad)
        {
            if( Math.abs( x ) > 25 ||  Math.abs( z ) > 25 ) {
                 int i = 0;
            }
            if( level == null ) return null;
            if( level.getChunkSource() == null ) return null;
            if( level.getChunkSource().getLoadedChunksCount() == 0 ) return null;

            if( forceLoad )
            {
                return level.getChunkSource().getChunk( x, z, true);
            }

            // Try non-blocking getChunkNow first
            LevelChunk c = level.getChunkSource().getChunkNow( x, z );
            return c;

            /*
            if( c != null )
                return c;

            // If that fails, try getChunk in a separate thread with timeout
            threadedChunkResult = null;
            Thread chunkLoader = new Thread(() -> {
                threadedChunkResult = level.getChunkSource().getChunk( x, z, false);
            });
            chunkLoader.start();

            try {
                chunkLoader.join(10); // Wait up to 10ms
                if(chunkLoader.isAlive()) {
                    chunkLoader.interrupt();
                    return null;
                }
                return threadedChunkResult;
            } catch (InterruptedException e) {
                return null;
            }

             */
        }



    }

    public static class EntityUtil {

        //All entities: SuggestionProviders.SUMMONABLE_ENTITIES;

        /**
         * Desciption: - You can do entity create in place if you want, here the argument are described
         * public T create(
         *     ServerLevel world,                 // The server-level/world where the entity should be spawned
         *     @Nullable CompoundTag customNbt,   // Optional NBT data (e.g., from a spawn egg or saved entity)
         *     @Nullable Consumer<T> postProcess, // Optional function to customize the entity after creation
         *     BlockPos position,                 // The target block position for spawning the entity
         *     MobSpawnType spawnType,            // The reason/type of spawn (e.g., NATURAL, SPAWNER, COMMAND)
         *     boolean applyOffset,               // Whether to position the entity above the block center (usually true)
         *     boolean alignToGround              // Whether to adjust the entity’s Y offset based on the terrain
         * )
         *
         */
        public static <T extends Entity> T createEntityAtPosition(EntityType<T> entityType, ServerLevel world, BlockPos pos )
        {
            return entityType.create(
                world,
                null,               // Optional NBT data (e.g., from a spawn egg or saved entity)
                null,                   // Optional function to customize the entity after creation
                pos,                 // The target block position for spawning the entity
                MobSpawnType.COMMAND,            // The reason/type of spawn (e.g., NATURAL, SPAWNER, COMMAND)
            true,               // Whether to position the entity above the block center (usually true)
            true              // Whether to adjust the entity’s Y offset based on the terrain
            );
        }


    }


    public static class ShapeUtil {

        /**
         * Returns a 3D array of all vertices within a circle of radius r.
         * The y value is always 0. If the provided radius is odd, it will be incremented by 1.  Returns single block at
         * the center for radius of 0 or 1 Returns 3x3 square for radius of 2 or 3.
         * @param radius
         * @return null if radius is less than 0. returns 3x3 square if radius is 2 or 3. Otherwise
         * returns all integer solutions where x^2 + z^2 = r^2 or less
         */
        public static Fast3DArray getCircle(int radius)
        {
            if( radius <= 0 )
                return null;

            Fast3DArray circle = new Fast3DArray(4 * (radius+1) * (radius+1));
            circle.add(0, 0, 0);
            if( radius <= 1 ) {
                return circle;
            }

            //if radius is 2, we just return 3x3 square
            if( radius == 2 )
            {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        circle.add(x, 0, z);
                    }
                }
                return circle;
            }


            radius -= 1;
            double softness = 0.5;
            double radiusSquared = Math.ceil(radius + softness) * (radius+softness);
            for (int x = -radius; x <= radius; x++)
            {
                //Instead, we want all integer solutions where x^2 + y^2 <= r^2
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radiusSquared) {
                        circle.add(x, 0, z);
                    }
                }
            }

            return circle;
        }

        /**
         * Returns a 3D array of all vertices within a sphere of radius r.
         * If height < radius*2, the sphere is compressed vertically.
         * If height > radius*2, the sphere is stretched vertically.
         * @param radius Sphere radius
         * @param height Total height of the sphere
         * @return Fast3DArray containing all points, null if invalid params
         */
        public static Fast3DArray getSphere(int radius, int height) 
        {
            if (radius <= 0 || height <= 0) {
                return null;
            }

            // Special case: height < 2 returns a circle
            if (height < 2) {
                return getCircle(radius);
            }

            // Special case: tiny radius
            if (radius < 4) {
                Fast3DArray sphere = new Fast3DArray(height * 9); // Approximate size
                Fast3DArray circle = getCircle(1);
                
                for (int y = 0; y < height; y++) {
                    for (int i = 0; i < circle.size; i++) {
                        sphere.add(circle.X[i], y, circle.Z[i]);
                    }
                }
                return sphere;
            }

            Fast3DArray sphere = new Fast3DArray((int) (height * radius * radius * Math.PI*2));
            
            // Generate layers of circles with decreasing radii
            int halfHeight = (int) Math.floor(height / 2);
            double[] radii = new double[halfHeight + 1];
            double deltaRadiusPerHt =  ( (double) radius / height);
            for (int y = 0; y <= halfHeight; y++) {
                if( y == 0)
                    radii[y] = radius;
                else
                    radii[y] = (radii[y-1] - deltaRadiusPerHt) - 0.01;
            }
            int yAdj = 0;
            for (int y = halfHeight*-1; y <= halfHeight; y++)
            {
                if(height % 2 == 0 && y == 0) {
                    yAdj = -1;
                    continue;
                }


                Fast3DArray layer = getCircle( (int) Math.ceil(radii[Math.abs(y)]) );
                for (int i = 0; i < layer.size; i++) {
                    sphere.add(layer.X[i], y+ yAdj, layer.Z[i]);
                }
            }

            return sphere;
        }


        /**
         * Returns a 3D array of all vertices within a 2D square of provided length and width
         * The y parameter is always 0.
         * @param length
         * @param width
         * @return
         */
        public static Fast3DArray getSquare(int length, int width)
        {
            if( length <= 0 || width <= 0)
                return null;

            Fast3DArray square = new Fast3DArray(length * width + 1);

           int modL = ( length % 2 == 0) ? 1 : 0;
           int modW = ( width % 2 == 0) ? 1 : 0;

            int xStart = -length / 2 + modL;
            int zStart = -width / 2 + modW;

    //consider edge cases for l/w = 1 or 2
            if( length == 1 && width == 1)
            {
                square.add(0, 0, 0);
                return square;
            }

            if( length == 1)
            {
                for( int z = zStart; z <= width / 2; z++)
                {
                    square.add(0, 0, z);
                }
                return square;
            }

            if( width == 1)
            {
                for( int x = xStart; x <= length / 2; x++)
                {
                    square.add(x, 0, 0);
                }
                return square;
            }

            if( length == 2 && width == 2)
            {
                square.add(-1, 0, -1);
                square.add(-1, 0, 1);
                square.add(1, 0, -1);
                square.add(1, 0, 1);
                return square;
            }

            if( length == 2)
            {
                for( int z = zStart; z <= width / 2; z++)
                {
                    square.add(-1, 0, z);
                    square.add(1, 0, z);
                }
                return square;
            }

            if( width == 2)
            {
                for( int x = xStart; x <= length / 2; x++)
                {
                    square.add(x, 0, -1);
                    square.add(x, 0, 1);
                }
                return square;
            }

            for( int x = xStart; x <= length / 2; x++)
            {
                for( int z = zStart; z <= width / 2; z++)
                {
                    square.add(x, 0, z);
                }
            }
            //if l==3, xStart = -1, l/2 = 1, that works
            //if l==4, xStart = -2, l/2 = 2, thats 5 verticies, we need 4



            return square;
        }


        public static Fast3DArray getCube(int length, int width, int height)
        {
            if( height <= 0 || length <= 0 || width <= 0)
                return null;

            Fast3DArray cube = new Fast3DArray(height * length * width);

            for (int y = 0; y < height; y++)
            {
                Fast3DArray plane = getSquare(length, width);
                for( int i = 0; i < plane.size; i++)
                {
                    cube.add(plane.X[i], y, plane.Z[i]);
                }
            }

            return cube;
        }


    }
    //END SHAPEUTIL

    public static class NetworkUtil {

        private static int SENT = 0;
        private static boolean CLIENT_STARTED = false;
        private static boolean USING_DEDICATED_SERVER = false;
        private static BalmNetworking networking = Balm.getNetworking();
        private static MinecraftServer server;

        private static final Queue<Runnable> PENDING_TASKS = new LinkedBlockingQueue<>();
        public static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(1, 1, 60L, java.util.concurrent.TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        private static <T> void sendHandler(Runnable r)
        {
            if( CLIENT_STARTED ) {
                POOL.submit(r);
            }
            else {
                PENDING_TASKS.add(r);
            }
        }

        public static <T> void serverSendToAllPlayers(T message) {
            sendHandler(() -> networking.sendToAll(server, message));
        }

        public static <T> void serverSendToPlayer(Player player, T message) {
            sendHandler(() -> networking.sendTo(player, message));
        }

        public static void init(EventRegistrar reg) {
            reg.registerOnServerStarted(NetworkUtil::onServerStart);
            reg.registerOnPlayerLogin(NetworkUtil::onPlayerLoginEvent);
        }

        private static void onServerStart(ServerStartedEvent event) {
            if( event.getServer().isDedicatedServer() )
            {
                USING_DEDICATED_SERVER = true;
                CLIENT_STARTED  = true;
            }
            server = GeneralConfig.getInstance().getServer();
        }

        private static void onPlayerLoginEvent(PlayerLoginEvent event)
        {
            CLIENT_STARTED = true;

            Runnable task;
            while ((task = PENDING_TASKS.poll()) != null) {
                POOL.submit(task);
            }
        }
    }

    public static class FileIO {


        /**
         * - Attempts to load the HBOreClustersAndRegenConfigs.json file from the config directory
         * - Provided string may be a relative path or a full path from the root directory.
         * - First checks if a config file exists in the serverDirectory/fileName
         * - returns string that can be Parsed into a json object
         * @param userConfigFile
         * @param defaultConfigFile
         * @param defaultData
         * @return String
         */
        public static String loadJsonConfigs(File userConfigFile, File defaultConfigFile, IStringSerializable defaultData)
        {
            //Use gson to serialize the default values and write to the file
            File configFile = userConfigFile;
            final String DEFAULT_DATA = defaultData.serialize();

            if( !configFile.exists() )  //User set file
            {
                final StringBuilder warnNoUserFile = new StringBuilder();
                warnNoUserFile.append("Could not find the provided  config file at path: ");
                warnNoUserFile.append(configFile.getAbsolutePath());
                warnNoUserFile.append(". Provided file name from serverConfig/hbs_ore_clusters_and_regen-server.toml: ");
                warnNoUserFile.append(configFile.getPath());
                warnNoUserFile.append(". Attempting to load the default file at default location: ");
                warnNoUserFile.append(configFile.getAbsolutePath());
                LoggerBase.logWarning( null,"000001",  warnNoUserFile.toString() );

                configFile = defaultConfigFile;
                if( !configFile.exists() )  //default file
                {
                    final StringBuilder warnNoDefaultFile = new StringBuilder();
                    warnNoDefaultFile.append("Could not find the default  JSON config file at path: ");
                    warnNoDefaultFile.append(configFile.getAbsolutePath());
                    warnNoDefaultFile.append(". A default file will be created for future reference.");
                    LoggerBase.logError( null, "000002", warnNoDefaultFile.toString());

                    try {
                        configFile.createNewFile();
                    }
                    catch (Exception e)
                    {
                        final StringBuilder error = new StringBuilder();
                        error.append("Could not create the default  JSON config file at path: ");
                        error.append(configFile.getAbsolutePath());
                        error.append(" due to an unknown exception. The game will still run using default values from memory.");
                        error.append("  You can try running the game as an administrator or update the file permissions to fix this issue.");
                        LoggerBase.logError( null, "000003", error.toString());

                        return DEFAULT_DATA;
                    }

                    serializeJsonConfigs(configFile, DEFAULT_DATA);
                }

            }

            /**
             * At this point, configFile exists in some capacity, lets check
             * if its valid JSON or not by reading it in.
             */
            String json = "";
            try {
                //Read line by line into a single string
                json = Files.readString(Paths.get(configFile.getAbsolutePath()));
            } catch (IOException e) {
                final StringBuilder error = new StringBuilder();
                error.append("Could not read the  JSON config file at path: ");
                error.append(configFile.getAbsolutePath());
                error.append(" due to an unknown exception. The game will still run using default values from memory.");
                LoggerBase.logError( null, "000004", error.toString());

                return DEFAULT_DATA;
            }

            return json;

        }
        //END loadJsonOreConfigs

        /**
         * Attempts to load a json file or a default alternative, short circuits
         * if the default isnt found
         * @param userConfigFile
         * @param defaultConfigFile
         * @return
         * @throws IOException
         * @throws NoDefaultConfig
         */
        public static JsonObject loadJsonOrDefault(File userConfigFile, File defaultConfigFile) throws IOException, NoDefaultConfig
        {
            // Fail fast if default isnt found
            JsonObject defaultJson;
            try (FileReader reader = new FileReader(defaultConfigFile)) {
                defaultJson = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                throw new NoDefaultConfig("Failed to load or parse default config file: " + defaultConfigFile.getAbsolutePath(), e);
            }

            // Call your loadJsonConfigs with an anonymous IStringSerializable
            String jsonString = loadJsonConfigs(
                userConfigFile,
                defaultConfigFile,
                new IStringSerializable() {
                    @Override
                    public String serialize() { return "";}
                    @Override
                    public void deserialize(String jsonString) {}
                }
            );

            // Parse the returned JSON string into a JsonObject
            JsonObject configJson = JsonParser.parseString(jsonString).getAsJsonObject();

            // Iterate through default properties, add missing ones
            for (String key : defaultJson.keySet()) {
                if (!configJson.has(key)) {
                    JsonElement defaultVal = defaultJson.get(key);
                    configJson.add(key, defaultVal);
                }
            }

            // Return merged config
            return configJson;
        }


        public static boolean serializeJsonConfigs(File configFile, String jsonData)
        {
            try {
                Files.write(Paths.get(configFile.getAbsolutePath()), jsonData.getBytes());
            } catch (IOException e) {
                final StringBuilder error = new StringBuilder();
                error.append("Could not write JSON data to config file at path: ");
                error.append(configFile.getAbsolutePath());
                error.append(" due to an unknown exception." );
                error.append("  You can try running the game as an administrator or check the file permissions.");
                LoggerBase.logError( null, "000004", error.toString());
                return false;
            }

            return true;
        }

        public static JsonElement arrayToJson(String[] arr) {
            Gson gson = new GsonBuilder().serializeNulls().create();
            return gson.toJsonTree(arr);
        }
    }
    //END FILE IO
    public static class TripleInt {
        public int x;
        public int y;
        public int z;

        public TripleInt(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public TripleInt(Vec3i vec) {
            this.x = vec.getX();
            this.y = vec.getY();
            this.z = vec.getZ();
        }

        public static TripleInt of(int x, int y, int z) {
            return new TripleInt(x, y, z);
        }

        //override the toString method
        @Override
        public String toString() {
            return "[" + x + ", " + y + ", " + z + "]";
        }

    }


    public static class WorldPos {
        public BlockPos blockPos;
        public TripleInt sectionIndicies;
        public int sectionIndex;
        public static final int SECTION_SZ = 16;
        public boolean DNE = false;

        public WorldPos(BlockPos pos, ChunkAccess chunk) {
            blockPos = pos;
            setWorldPos(pos, chunk);
        }

        public WorldPos(TripleInt indices, int section, ChunkAccess chunk) {
            sectionIndicies = indices;
            sectionIndex = section;
            setWorldPos(indices, section, chunk);
        }

        /** Setters **/

        public void setWorldPos(BlockPos pos, ChunkAccess chunk)
        {
            blockPos = pos;

            //Convert worldPos to sectionIndicies
            final BlockPos chunkPos = chunk.getPos().getWorldPosition();
            final Integer Y_MIN = chunk.getMinBuildHeight();
            final Integer Y_MAX = chunk.getMaxBuildHeight();

            int x = pos.getX() - chunkPos.getX();
            int z = pos.getZ() - chunkPos.getZ();
            this.sectionIndex = (pos.getY() - Y_MIN) / SECTION_SZ;
            int y = (pos.getY() - Y_MIN) % SECTION_SZ;

            this.sectionIndicies = new TripleInt(x, y, z);

        }


        public void setWorldPos(TripleInt indices, int section, ChunkAccess chunk)
        {
            final BlockPos chunkPos = chunk.getPos().getWorldPosition();
            final Integer Y_MIN = chunk.getMinBuildHeight();
            final Integer Y_MAX = chunk.getMaxBuildHeight();

             int x = chunkPos.getX() + indices.x;
             int y = Y_MIN + (SECTION_SZ * section) + indices.y;
             int z = chunkPos.getZ() + indices.z;

             this.blockPos = new BlockPos(x, y, z);
        }

        /** Getters **/

        public BlockPos getWorldPos() {
            return blockPos;
        }

        public TripleInt getIndices() {
            return sectionIndicies;
        }

        public Integer getSectionIndex() {
            return sectionIndex;
        }

        public int getX() {
            return blockPos.getX();
        }

        public int getY() {
            return blockPos.getY();
        }

        public int getZ() {
            return blockPos.getZ();
        }

        //sectionToString
        public String sectionToString() {
            return "Indicies{" +
                "x,y,z=" + sectionIndicies +
                ", sectionIndex=" + sectionIndex +
                '}';
        }

        //worldPosToString
        public String worldPosToString() {
            return "WorldPos{" +
                "blockPos=" + blockPos +
                '}';
        }
    }
    //END WORLD POS


    public static class Validator
    {

        public static class ConfigNumber<T extends Number & Comparable<T>>
        {
            private final String name;
            private final T min;
            private final T max;
            private T current;

            public ConfigNumber(String name, T current, T min, T max) {
                if (min.compareTo(max) > 0) {
                    throw new IllegalArgumentException("Min value cannot be greater than max value.");
                }
                if (current.compareTo(min) < 0 || current.compareTo(max) > 0) {
                    throw new IllegalArgumentException("Current value must be within the range of min and max.");
                }
                this.name = name;
                this.min = min;
                this.max = max;
                this.current = current;
            }

            public T get() {
                return current;
            }

            public void set(T value) {
                if (test(value)) {
                    this.current = value;
                } else {
                    throw new IllegalArgumentException("Value is out of range: " + value);
                }
            }

            public boolean test(T value) {
                return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
            }

            @Override
            public String toString() {
                return "ConfigNumber{" +
                    "name='" + name + '\'' +
                    ", min=" + min +
                    ", max=" + max +
                    ", current=" + current +
                    '}';
            }
        }

        public static boolean validateNumber(Number value, ConfigNumber n, String element) {
            StringBuilder error = new StringBuilder();
            error.append("Error setting ");
            error.append(n.name);
            error.append(element);
            error.append(" using default value of ");
            error.append(n.current + " instead");

            if( value.doubleValue() >= n.min.doubleValue() && value.doubleValue() <= n.max.doubleValue() )
                return true;
            else {
                LoggerBase.logWarning( null, "001002",error.toString());
                return false;
            }
        }

        private static final HashSet<String> ACCEPTED_STRING_BOOLEAN_TRUE = new HashSet<>(Arrays.asList("TRUE", "true", "yes", "1"));
        public static Boolean parseBoolean(String value) {
            return ACCEPTED_STRING_BOOLEAN_TRUE.contains(value);
        }

    }
    //END VALIDATOR



    /**
    * Class: Fast3DArray
    * Description: A 3D array that is optimized for fast access by avoiding NEW calls
     */
    public static class Fast3DArray {

        private int[] X;
        private int[] Y;
        private int[] Z;

        public int size;
        public final int MAX_SIZE;

        public Fast3DArray(int size) {
            this.size = 0;
            this.MAX_SIZE = size;
            X = new int[size];
            Y = new int[size];
            Z = new int[size];
        }
        //Write an add method for blockState and Block
        public void add(BlockPos pos) {
            this.add(pos.getX(), pos.getY(), pos.getZ());
        }

        public void add(int x, int y, int z)
        {
            if( size >= MAX_SIZE)
                return;

            X[size] = x;
            Y[size] = y;
            Z[size] = z;
            size++;
        }

        public void addAll(Fast3DArray other)
        {
            for( int i = 0; i < other.size; i++)
            {
                add(other.X[i], other.Y[i], other.Z[i]);
            }
        }


        /**
         * Returns a new 3D array of all
         * @param index
         * @return null if index is out of bounds
         */
        public int[][] get(int index) {

            if( index >= MAX_SIZE || index < 0)
                return null;
            return new int[][] { { X[index], Y[index], Z[index] } };
        }

        public int getX(int index) {
            return X[index];
        }

        public int getY(int index) {
            return Y[index];
        }

        public int getZ(int index) {
            return Z[index];
        }

        //implement forreach that takes 3 ints
        public TripleInt[] toArray() {
            TripleInt[] arr = new TripleInt[size];
            for( int i = 0; i < size; i++) {
                arr[i] = new TripleInt(X[i], Y[i], Z[i]);
            }
            return arr;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Count: " + size + " ");
            String delimiter = "";
            for( int i = 0; i < size; i++)
            {
                sb.append(delimiter);
                sb.append("[" + X[i] + ", " + Y[i] + ", " + Z[i] + "]");
                delimiter = ", ";
            }
            return sb.toString();
        }
    }
}
