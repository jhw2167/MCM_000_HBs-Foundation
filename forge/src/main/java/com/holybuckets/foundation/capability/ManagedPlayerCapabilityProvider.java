package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.player.ManagedPlayer;
import com.holybuckets.foundation.player.ManagedPlayerConsumer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManagedPlayerCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag>, ManagedPlayerConsumer {

    public static final String CLASS_ID = "006";
    public static Capability<ManagedPlayer> MANAGED_PLAYER = CapabilityManager.get(new CapabilityToken<>() { });

    private ManagedPlayer managedPlayer;
    private Player player;
    private final LazyOptional<ManagedPlayer> optional = LazyOptional.of(this::getManagedPlayer);

    public ManagedPlayerCapabilityProvider(Player player) {
        super();
        this.player = player;
        this.managedPlayer = null;
    }

    @Nullable
    public ManagedPlayer getManagedPlayer() {
        if(managedPlayer != null) return managedPlayer;
        if(player.getGameProfile()==null) return null;
        this.managedPlayer = ManagedPlayer.getManagedPlayer(player);
        return managedPlayer;
    }

    public void accept(ManagedPlayer managedPlayer) {
        this.managedPlayer = managedPlayer;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
       if(getManagedPlayer()==null) return tag;
        return ManagedPlayer.serialize(getManagedPlayer());
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(getManagedPlayer()==null) {
            ManagedPlayer.addPending(player, tag);
            return;
        }
        ManagedPlayer.deserialize(getManagedPlayer(), tag);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        if(capability == MANAGED_PLAYER) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if(cap == MANAGED_PLAYER) {
            return optional.cast();
        }
        return ICapabilityProvider.super.getCapability(cap);
    }
}
