package com.holybuckets.foundation.block.entity;

import com.mojang.serialization.Codec;
import net.blay09.mods.balm.world.level.block.entity.BalmBlockEntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A block that indicates the bottom of a portal
 */
public class SimpleBlockEntity extends BlockEntity {

    private static final Codec<Map<String, String>> DATA_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private Map<String,String> data;
    public SimpleBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.simpleBlockEntity.get(), blockPos, blockState);
        data = new HashMap<>();
    }

    // Getters/setters for use in menus or logic
    public String getProperty(String key) {
        return data.get(key);
    }

    public void setProperty(String key, String value) {
        data.put(key, value);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return BalmBlockEntityUtils.createUpdatePacket(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        data.clear();
        input.read("data", DATA_CODEC).ifPresent(loaded -> data.putAll(loaded));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("data", DATA_CODEC, Map.copyOf(data));
    }
}
