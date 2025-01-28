package com.holybuckets.foundation;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.blay09.mods.balm.api.BalmRegistries;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
* Class: HolyBucketsUtility
* Description: This class will contain utility methods amd objects that I find myself using frequently
*
 */
public class HBUtil {

    public static final String CLASS_ID = "004";


    public static class BlockUtil {

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
                return Blocks.AIR;
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

        public static String positionToString(BlockPos pos)
        {
            if( pos == null)
                return null;
            return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
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

    }
    //END BLOCK UTIL

    public static class LevelUtil {

        public enum LevelNameSpace {
            CLIENT,
            SERVER
        }
        @Nullable
        public static LevelAccessor toLevel(LevelNameSpace nameSpace, String dimensionId)
        {
            String levelId = dimensionId.replace("CLIENT:", "").replace("SERVER:", "");
            if( nameSpace == LevelNameSpace.CLIENT ) {
                levelId = "CLIENT:" + levelId;
            } else if( nameSpace == LevelNameSpace.SERVER ) {
                levelId = "SERVER:" + levelId;
            }
            return toLevel(levelId);
        }
        public static LevelAccessor toLevel(String id) {
            return GeneralConfig.getInstance().getLevel(id);
        }

        public static String toLevelId(LevelAccessor level)
        {
            if (level == null)
                return null;
            String levelName = level.dimensionType().effectsLocation().toString();
            if(level.isClientSide()) {
                return "CLIENT:" + levelName;
            } else {
                return "SERVER:" + levelName;
            }
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

        public static ChunkPos getPos(String id) {
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
            return chunkDist(getPos(id1), getPos(id2));
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

        public static Long getChunkRandom(String id) {
            return getChunkRandom(getPos(id));
        }

        public static Long getChunkRandom( ChunkPos pos ) {
            //map the chunks x and z position to a random number
            final Long WIDTH = 10_000_000l;
            return (pos.x * WIDTH) + pos.z;
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

            if( forceLoad )
            {
                return level.getChunkSource().getChunk( x, z, true);
            }

            // Try non-blocking getChunkNow first
            LevelChunk c = level.getChunkSource().getChunkNow( x, z );
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
        }



    }

    public static class ShapeUtil {

        /**
         * Returns a 3D array of all vertices within a circle of radius r.
         * The y value is always 0. If the provided radius is even, it will be incremented by 1.
         * @param radius
         * @return null if radius is less than 0. returns 3x3 square if radius is 2 or 3. Otherwise
         * returns all integer solutions where x^2 + z^2 = r^2 or less
         */
        public static Fast3DArray getCircle(int radius)
        {
            if( radius <= 0 )
                return null;

            Fast3DArray circle = new Fast3DArray(4 * radius * radius);

            circle.add(0, 0, 0);
            if( radius <= 1 ) {
                return circle;
            }

            if( radius % 2 == 0) {
                radius++;
            }

            //if radius is 3, we just return 3x3 square
            if( radius == 3)
            {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        circle.add(x, 0, z);
                    }
                }
                return circle;
            }

            int radiusSquared = radius * radius;
            for (int x = -radius; x <= radius; x++)
            {
                // Calculate y^2 for the current x
                int zSquared = radiusSquared - x * x;

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

        public static <T> void sendToAllPlayers(T message) {
            new Thread(() -> threadedSendToAllPlayers(message)).start();
        }
            private static <T> void threadedSendToAllPlayers( T message) {
                BalmNetworking networking = Balm.getNetworking();
                networking.sendToAll(GeneralConfig.getInstance().getServer(), message);
            }

        //create a new method that sends a message to a specific player
        public static <T> void sendToPlayer(Player player, T message) {
            BalmNetworking networking = Balm.getNetworking();
            networking.sendTo(player, message);
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
                warnNoUserFile.append("Could not find the provided ore cluster config file at path: ");
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
                    warnNoDefaultFile.append("Could not find the default ore cluster JSON config file at path: ");
                    warnNoDefaultFile.append(configFile.getAbsolutePath());
                    warnNoDefaultFile.append(". A default file will be created for future reference.");
                    LoggerBase.logError( null, "000002", warnNoDefaultFile.toString());

                    try {
                        configFile.createNewFile();
                    }
                    catch (Exception e)
                    {
                        final StringBuilder error = new StringBuilder();
                        error.append("Could not create the default ore cluster JSON config file at path: ");
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
                error.append("Could not read the ore cluster JSON config file at path: ");
                error.append(configFile.getAbsolutePath());
                error.append(" due to an unknown exception. The game will still run using default values from memory.");
                LoggerBase.logError( null, "000004", error.toString());

                return DEFAULT_DATA;
            }

            return json;

        }
        //END loadJsonOreConfigs

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

    }


    public static class WorldPos {
        public static BlockPos blockPos;
        public static TripleInt sectionIndicies;
        public static int sectionIndex;
        public static final int SECTION_SZ = 16;
        public boolean DNE = false;

        public WorldPos(BlockPos pos, LevelChunk chunk) {
            blockPos = pos;
            setWorldPos(pos, chunk);
        }

        public WorldPos(TripleInt indices, int section, LevelChunk chunk) {
            sectionIndicies = indices;
            sectionIndex = section;
            setWorldPos(indices, section, chunk);
        }

        /** Setters **/

        public void setWorldPos(BlockPos pos, LevelChunk chunk)
        {
            try {
                chunk.getBlockState(pos);
            } catch (Exception e) {
                LoggerBase.logError( null, "004003", "Error setting world position, block state not found at position: " + pos.toString());
                DNE = true;
                return;
            }


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


        public void setWorldPos(TripleInt indices, int section, LevelChunk chunk)
        {
            final BlockPos chunkPos = chunk.getPos().getWorldPosition();
            final Integer Y_MIN = chunk.getMinBuildHeight();
            final Integer Y_MAX = chunk.getMaxBuildHeight();

             int x = chunkPos.getX() + indices.x;
             int y = Y_MIN + (SECTION_SZ * section);
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
