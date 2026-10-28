package com.holybuckets.foundation.event.custom;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class RegisterCustomBlockEntitiesEvent {

    public record Entry(Supplier<? extends ItemLike> item,
                        Supplier<? extends BlockEntityType<?>> type,
                        Supplier<BlockState> state) {}

    private final List<Entry> entries = new ArrayList<>();

    public void register(Supplier<? extends ItemLike> item,
                         Supplier<? extends BlockEntityType<?>> type,
                         Supplier<BlockState> state) {
        entries.add(new Entry(item, type, state));
    }

    public void register(Supplier<? extends Block> block, Supplier<? extends BlockEntityType<?>> type) {
        register(block, type, () -> block.get().defaultBlockState());
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
