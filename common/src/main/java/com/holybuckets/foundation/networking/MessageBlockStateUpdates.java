package com.holybuckets.foundation.networking;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.HBUtil.LevelUtil;
import com.holybuckets.foundation.model.ManagedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static java.lang.Thread.sleep;

/**
 * Description: MessageUpdateBlockStates
 * Packet data for block state updates from server to client
 */
public class MessageBlockStateUpdates {

    public static final String LOCATION = "block_state_updates";
    private static final Integer BLOCKPOS_SIZE = 48;    //16 bytes per number x3 = 48 bytes
    LevelAccessor world;
    Map<BlockState, List<BlockPos>> blocks;

    MessageBlockStateUpdates(LevelAccessor level, Map<BlockState, List<BlockPos>> blocks) {
        this.world = level;
        this.blocks = blocks;
    }

    /**
     * Create and fire the packet. There is an upper limit of 32KB per pack sent enforced by Minecraft.
     * In order to avoid this, we will send multiple packets in increments of 512 blocks.
     * @param world
     * @param updates
     * @return
     */
    public static void createAndFire(LevelAccessor world, Map<BlockState, List<BlockPos>> updates)
    {
        //1. Create new instances of updates
        List< Map<BlockState, List<BlockPos>> > messages = new ArrayList<>();

        //2. Create map of blockState to ListIterator for each list
        Queue<Pair<BlockState, Iterator<BlockPos>>> iterators = new LinkedList<>();
        for (Map.Entry<BlockState, List<BlockPos>> entry : updates.entrySet()) {
            if(entry.getValue().size() == 0) continue;
            iterators.add(Pair.of(entry.getKey(), entry.getValue().listIterator()));
        }

        //3. for up to MAX_SIZE = 512 blocks, add to blockStates using iterators
        final int MAX_SIZE = 512;
        Pair<BlockState, Iterator<BlockPos>> current = iterators.poll();

        while(current != null)
        {
            Map<BlockState, List<BlockPos>> blockStateUpdates = new HashMap<>();
            for(int i = 0; i < MAX_SIZE; i++)
            {
                if( !current.getRight().hasNext() )
                    current = iterators.poll();

                if(current == null)
                    break;

                blockStateUpdates.putIfAbsent(current.getLeft(), new ArrayList<>());
                List<BlockPos> positions = blockStateUpdates.get(current.getLeft());
                positions.add(current.getRight().next());
            }

            messages.add(blockStateUpdates);
        }

        for(Map<BlockState, List<BlockPos>> message : messages) {
            MessageBlockStateUpdates packet = new MessageBlockStateUpdates(world, message);
            HBUtil.NetworkUtil.sendToAllPlayers(packet);
        }

    }


    public static void handle(Player player, MessageBlockStateUpdates message) {
        if(player.level() != message.world) return;
        new Thread(() -> threadUpdateChunkBlocks(message.world, message.blocks)).start();
    }

    public static void threadUpdateChunkBlocks( LevelAccessor world, Map<BlockState, List<BlockPos>> blocks) {
        boolean madeUpdates = false;
        try {
            while (!madeUpdates) {
                if (GeneralConfig.getInstance().getServer() == null) return;
                madeUpdates = ManagedChunk.updateChunkBlocks(world, blocks);
                sleep(10);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
