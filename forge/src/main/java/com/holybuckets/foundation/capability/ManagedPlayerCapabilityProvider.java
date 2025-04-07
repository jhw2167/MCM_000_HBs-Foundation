package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.player.ManagedPlayer;
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

public class ManagedPlayerCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final String CLASS_ID = "006";
    public static Capability<ManagedPlayer> MANAGED_PLAYER = CapabilityManager.get(new CapabilityToken<>() { });

    private ManagedPlayer managedPlayer;
    private final LazyOptional<ManagedPlayer> optional = LazyOptional.of(this::getManagedPlayer);

    public ManagedPlayerCapabilityProvider(Player player) {
        super();
        this.managedPlayer = new ManagedPlayer(player);
    }

    public ManagedPlayer getManagedPlayer() {
        return this.managedPlayer;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if(this.managedPlayer == null) return tag;

        tag.put("managedPlayer", this.managedPlayer.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        CompoundTag tag = nbt.getCompound("managedPlayer");
        if(this.managedPlayer == null)
            this.managedPlayer = new ManagedPlayer(tag);
        else
            this.managedPlayer.deserializeNBT(tag);
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
