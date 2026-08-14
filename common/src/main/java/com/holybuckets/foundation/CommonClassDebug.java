package com.holybuckets.foundation;

import com.holybuckets.foundation.block.ModBlocks;
import com.holybuckets.foundation.console.Messager;
import com.holybuckets.foundation.core.MovingWaypoint;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.AnvilUpdateEvent;
import com.holybuckets.foundation.event.custom.ClientInputEvent;
import com.holybuckets.foundation.event.custom.PlayerHasItemEvent;
import com.holybuckets.foundation.event.custom.PlayerInteractEvent;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkUtility;
import net.blay09.mods.balm.api.event.ChunkLoadingEvent;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.event.TossItemEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Thread.sleep;


public class CommonClassDebug {


    public static void init(EventRegistrar reg)
    {
        //testMessager(reg);
        //test(reg);
    }

    public static void testMessager(EventRegistrar reg) {
        //reg.registerOnClientInput(CommonClassDebug::onPlayerInput);
    }

    //has oak_log in inventory, send message to player using messager system
    private static void onPlayerHasOakLog(PlayerHasItemEvent event) {
        if (event.getPlayer() == null) return;
        Messager.getInstance().sendBottomActionHint(event.getPlayer(), "You have an oak log in your inventory!");
    }

    //if a player has a diamond sword in their inventory, send a message using the Messager system
    private static void onPlayerHasDiamondSword(PlayerHasItemEvent event) {
        if (event.getPlayer() == null) return;
        Messager.getInstance().sendBottomActionHint(event.getPlayer(), "You have a diamond sword in your inventory!");
    }

    private static void onPlayerInput(ClientInputEvent event) {
        if (event.getPlayer() == null) return;

        // Get the key codes that were pressed
        Set<Integer> keyCodes = event.getKeyCodes();
        if (keyCodes.isEmpty()) return;

        // Create a message showing which keys were pressed
        StringBuilder keyMessage = new StringBuilder("Keys pressed: ");
        for (int i = 0; i < keyCodes.size(); i++) {
            if (i > 0) keyMessage.append(", ");
            keyMessage.append(keyCodes.stream().toList().get(i));
        }

        // Send the message using the Messager system
        Messager.getInstance().sendBottomActionHint(event.getPlayer(), keyMessage.toString());

        //Set a MovingWaypoint to the players current position
        BlockPos pos = event.getPlayer().blockPosition();

        if(keyCodes.stream().toList().get(0)==1)
            MovingWaypoint.setWaypoint((ServerPlayer) event.getPlayer(), pos);

        // Also log it for debugging
        //LoggerBase.logInfo(null, "MESSAGER_TEST", "Player input: " + keyMessage.toString());
    }

    public static void test(EventRegistrar reg)
    {
        //reg.registerOnChunkLoad(CommonClassDebug::onChunkLoad);
        //reg.registerOnLevelLoad(CommonClassDebug::onLevelLoad);
        //reg.registerOnPlayerLogin(CommonClassDebug::onPlayerLogin);
        //reg.registerOnClientInput(CommonClassDebug::onClientInput);
        //reg.registerOnServerTick(TickType.ON_120_TICKS, CommonClassDebug::on120Ticks);
        //reg.registerOnDailyTick(null, CommonClassDebug::onDailyTick);
        //reg.registerOnServerTick(TickType.ON_1200_TICKS , CommonClassDebug::onServerTick);

        //reg.registerOnTossItem(CommonClassDebug::onTossItem);
        //reg.registerOnAnvilUpdate(swordUpdate, CommonClassDebug::onAnvilUpdateSword);
        //reg.registerOnAnvilUpdate(ironToolCobble, CommonClassDebug::onAnvilUpdateIronToolCobble);
        //reg.registerOnAnvilUpdate(empowerEnchant, CommonClassDebug::onAnvilUpdateRepair);

        // PlayerInteractEvent — uncomment any single line to verify one variant in isolation,
        // or the .class line to verify the catch-all subscription path.
        reg.registerOnPlayerInteract(PlayerInteractEvent.RightClickInteraction.class, CommonClassDebug::onRightClickInteraction);
        reg.registerOnPlayerInteract(PlayerInteractEvent.LeftClickInteraction.class, CommonClassDebug::onLeftClickInteraction);
        reg.registerOnPlayerInteract(PlayerInteractEvent.EntityInteract.class, CommonClassDebug::onEntityInteract);
        reg.registerOnPlayerInteract(PlayerInteractEvent.class, CommonClassDebug::onAnyPlayerInteract);

        testPlayerMatchesItem(reg);
    }


    /* PLAYER_MATCHES_ITEM */

    private static void testPlayerMatchesItem(EventRegistrar reg) {
        reg.registerOnPlayerMatchesItem(SHARPENED, CommonClassDebug::onPlayerHasSharpness);
        reg.registerOnBeforeServerStarted(CommonClassDebug::registerAppleCounterAtRuntime);
    }

    /**
     * The predicate only receives the Item, and enchantments live on the ItemStack, so the closest
     * a Predicate&lt;Item&gt; can get to "has Sharpness" is "could carry Sharpness".
     */
    private static final Predicate<ItemStack> SHARPENED =
        stack -> HBUtil.ItemUtil.itemHasEnchant(stack, Enchantments.SHARPNESS);

    // The consumer fires once per matching item key, so a player holding five enchantable items
    // triggers five calls for one event. Track the last event handled and report it once.
    private static PlayerHasItemEvent lastSharpnessEvent = null;

    private static void onPlayerHasSharpness(PlayerHasItemEvent event) {
        if (event == lastSharpnessEvent) return;
        lastSharpnessEvent = event;
        if (event.getPlayer() == null) return;

        LoggerBase.logInfo(null, "001310", "PLAYER_MATCHES_ITEM enchantable: SHARPNESS");
    }

    private static void registerAppleCounterAtRuntime(ServerStartingEvent event) {
        EventRegistrar.getInstance()
            .runtimeOnPlayerMatchesItem(stack -> stack.getItem()==Items.APPLE, CommonClassDebug::onPlayerHasApples);
        LoggerBase.logInfo(null, "001311", "PLAYER_MATCHES_ITEM apple counter registered at runtime");
    }

    private static final int APPLE_THRESHOLD = 2;

    private static void onPlayerHasApples(PlayerHasItemEvent event) {
        if (event.getPlayer() == null) return;

        Integer apples = event.getInventoryMap().get(Items.APPLE);
        if (apples == null || apples < APPLE_THRESHOLD) return;

        LoggerBase.logInfo(null, "001312",
            "PLAYER_MATCHES_ITEM apples: " + event.getPlayer().getName().getString()
            + " has " + apples + " apples");
    }

    private static void onRightClickInteraction(PlayerInteractEvent.RightClickInteraction event) {
        LoggerBase.logInfo(null, "001300",
            "RightClickInteraction: hand=" + event.getHand()
            + " item=" + event.getItemStack().getItem().getDescriptionId()
            + " pos=" + event.getPos()
            + " face=" + event.getFace());
    }

    private static void onLeftClickInteraction(PlayerInteractEvent.LeftClickInteraction event) {
        LoggerBase.logInfo(null, "001301",
            "LeftClickInteraction: hand=" + event.getHand()
            + " item=" + event.getItemStack().getItem().getDescriptionId()
            + " pos=" + event.getPos()
            + " face=" + event.getFace());
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        String targetName = event.getTarget() != null
            ? event.getTarget().getType().getDescriptionId() : "null";
        LoggerBase.logInfo(null, "001302",
            "EntityInteract: target=" + targetName
            + " localPos=" + event.getLocalPos()
            + " hand=" + event.getHand()
            + " item=" + event.getItemStack().getItem().getDescriptionId());
    }

    // Catch-all subscriber — exercises the PlayerInteractEvent.class registration path
    // that fires on every variant regardless of subclass.
    private static void onAnyPlayerInteract(PlayerInteractEvent event) {
        String playerName = event.getPlayer() != null
            ? event.getPlayer().getName().getString() : "null";
        LoggerBase.logInfo(null, "001303",
            "PlayerInteractEvent (any): type=" + event.getClass().getSimpleName()
            + " player=" + playerName
            + " canceled=" + event.isCanceled());
    }

    /*
    private static AnvilUpdateEvent swordUpdate = new AnvilUpdateEvent(Items.DIAMOND_SWORD, Items.COBBLESTONE);
    private static void onAnvilUpdateSword(AnvilUpdateEvent event) {
        ItemStack sword = event.getLeftItem();

        int currentSharpness = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, sword);
        int newSharpness = currentSharpness + 1;
        newSharpness = Math.min(newSharpness, 5);

        ItemStack enchantedSword = sword.copy();
        enchantedSword.enchant(Enchantments.SHARPNESS, newSharpness);
        event.setResultItem(enchantedSword);
        event.setCost(1);
        LoggerBase.logInfo(null, "ANVIL_UPDATE",
            String.format("Upgraded Sharpness from %d to %d", currentSharpness, newSharpness));
    }


    private static Set<Item> IRON_TOOLS = Set.of(Items.IRON_SWORD, Items.IRON_SHOVEL, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_HOE);
    private static AnvilUpdateEvent.MaterialDriven ironToolCobble = new AnvilUpdateEvent.MaterialDriven(IRON_TOOLS, Items.COBBLESTONE);
    private static void onAnvilUpdateIronToolCobble(AnvilUpdateEvent event) {
        ItemStack leftItem = event.getLeftItem();

        if(IRON_TOOLS.contains(leftItem.getItem())) {
            ItemStack repairedTool = leftItem.copy();
            repairedTool.setDamageValue(Math.max(0, leftItem.getDamageValue() - leftItem.getMaxDamage() / 4));
            event.setResultItem(repairedTool);
            event.setCost(1);
        }
    }

    private static Set<Enchantment> REPAIR_ENCHANTMENTS = Set.of(Enchantments.UNBREAKING, Enchantments.SHARPNESS);
    private static AnvilUpdateEvent.EnchantDriven empowerEnchant = new AnvilUpdateEvent.EnchantDriven(REPAIR_ENCHANTMENTS, Items.ROTTEN_FLESH);
    //onAnvilUpdate method to repair tool if it contains enchant
    private static void onAnvilUpdateRepair(AnvilUpdateEvent event) {
        ItemStack leftItem = event.getLeftItem();

        Enchantment repEnchant = null;
        for (Enchantment enchantment : REPAIR_ENCHANTMENTS) {
            if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, leftItem) > 0) {
                repEnchant = enchantment;
                break;
            }
        }

        if (repEnchant != null)
        {
            ItemStack empoweredTool = leftItem.copy();
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(empoweredTool);
            enchantments.put(repEnchant, enchantments.get(repEnchant) + 1);
            EnchantmentHelper.setEnchantments(enchantments, empoweredTool);

            event.setResultItem(empoweredTool);
            event.setCost(1);
        }
    }

    private static void onTossItem(TossItemEvent event) {
        LoggerBase.logInfo(null, "001200", "Player Tossed Item Event - Item: " + event.getItemStack().getItem().getDescriptionId() + " Qty: " + event.getItemStack().getCount());
    }

    private static void on120Ticks(ServerTickEvent event) {
        GeneralConfig config = GeneralConfig.getInstance();
        LoggerBase.logDebug(null, "001090", "Server ticks: " + config.getTotalTickCount());
        LoggerBase.logDebug(null, "001091", "Overworld ticks: " + config.getTotalTickCountWithSleep(GeneralConfig.OVERWORLD) );
        LoggerBase.logDebug(null, "001092", "Nether ticks: " + config.getTotalTickCountWithSleep(GeneralConfig.NETHER) );
    }

    private static void onDailyTick(ServerTickEvent.DailyTickEvent event) {
        LoggerBase.logDebug(null, "001100", "Daily tick: " + event.getLevel().dimension().identifier() );
        LoggerBase.logDebug(null, "001100", "Daily tick: " + event.getTickCountWithSleeps());
        LoggerBase.logDebug(null, "001100", "Daily tick: " + event.isTriggeredByWakeUp() );
    }

    // Subscribe to client input events

    private static void onClientInput(ClientInputEvent event) {
        LoggerBase.logInfo(null, "001001", "Client Input Event - Keys pressed: " + event.getKeyCodes());
    }

    private static void onPlayerLogin(PlayerLoginEvent event) {
        Constants.LOG.info("Player connected CONNECTED: " + event.getPlayer().getGameProfile().name());
        //Print out the location of each dimension by converting the resourceLocation to Level using HBUtil.LevelUtil
    }

    static void onServerTick(ServerTickEvent event) {
        Constants.LOG.info("Server ticked: " + event.getTickCount() );
    }

    private static void onPlayerLoad(PlayerLoginEvent event) {
        Constants.LOG.info("Player loaded: " + event.getPlayer().getGameProfile().name());
    }


    //Create a threadpool to add blocks to a chunk, max 16 threads, store in queue
    public static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 10L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());

    public static void onChunkLoad(ChunkLoadingEvent.Load event)
    {
        if( event.getLevel().isClientSide() ) return;

        String id = HBUtil.ChunkUtil.getId(event.getChunk().getPos().getWorldPosition());
        String chunkId = HBUtil.ChunkUtil.getId(event.getChunk());

        if(!chunkId.equals("0,0"))
            return;
        //Print ids to test if they match
        Constants.LOG.info("Chunk loaded: " + id + " " + chunkId + " MATCH: " + id.equals(chunkId) );

        POOL.submit(() -> threadAddChunkBlock(event));
    }

    public static void threadAddChunkBlock(ChunkLoadingEvent.Load event)
    {
        //final BlockState GOLD = Blocks.DIAMOND_BLOCK.defaultBlockState();
        final BlockState GOLDSTATE = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
        final Block GOLD = ModBlocks.empty.get();
        List<Pair<BlockState, BlockPos>> blocks = new ArrayList<>();
        ChunkAccess c = event.getChunk();
        BlockPos p = c.getPos().getWorldPosition();
        LevelAccessor level = event.getLevel();
        ManagedChunkUtility util = ManagedChunkUtility.getInstance(level);

        //Use MangedChunk.loadedChunks to determine when chunk is loaded
        while( !util.isChunkFullyLoaded(c.getPos()) ) {}

        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //Add blocks to chunk
        //HBUtil.TripleInt[] sphere = HBUtil.ShapeUtil.getSphere(8, 32).toArray();
        List<Pair<BlockState, BlockPos>> blockStateList = new ArrayList<>();



        LoggerBase.logInfo(null, "999000", "Added blocks to chunk: " + p);
        ManagedChunk.updateChunkBlockStates(level, blockStateList);


        //TestWorldPos
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(0, 15, 0);
        BlockPos p2a = new BlockPos(0, 16, 0);
        BlockPos p2b = new BlockPos(0, -15, 0);
        BlockPos p2c = new BlockPos(0, -16, 0);
        BlockPos p2d = new BlockPos(0, -17, 0);
        BlockPos p3 = new BlockPos(0, -64, 0);

        //Convert this blockPos to HBUtil.WorldPos and back, print the results
        HBUtil.WorldPos wp1 = new HBUtil.WorldPos(p1, c);
        HBUtil.WorldPos wp2 = new HBUtil.WorldPos(p2, c);
        HBUtil.WorldPos wp2a = new HBUtil.WorldPos(p2a, c);
        HBUtil.WorldPos wp2b = new HBUtil.WorldPos(p2b, c);
        HBUtil.WorldPos wp2c = new HBUtil.WorldPos(p2c, c);
        HBUtil.WorldPos wp2d = new HBUtil.WorldPos(p2d, c);
        HBUtil.WorldPos wp3 = new HBUtil.WorldPos(p3, c);

        Constants.LOG.info("WorldPos: " + wp1 + " " + wp1.worldPosToString() + " " + wp1.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2 + " " + wp2.worldPosToString() + " " + wp2.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2a + " " + wp2a.worldPosToString() + " " + wp2a.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2b + " " + wp2b.worldPosToString() + " " + wp2b.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2c + " " + wp2c.worldPosToString() + " " + wp2c.sectionToString());
        Constants.LOG.info("WorldPos: " + wp2d + " " + wp2d.worldPosToString() + " " + wp2d.sectionToString());
        Constants.LOG.info("WorldPos: " + wp3 + " " + wp3.worldPosToString() + " " + wp3.sectionToString());
    }

    public static void onLevelLoad(LevelLoadingEvent event)
    {
        if( event.getLevel().isClientSide() ) return;
        Constants.LOG.info("Level loaded: " + ( (ServerLevel) event.getLevel() ).dimensionTypeId() );
    }
    */
}

