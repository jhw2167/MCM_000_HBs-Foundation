package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.player.ManagedPlayer;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import com.holybuckets.foundation.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.NbtOps;

public class ManagedPlayerAttachment {
    
    static void init() {}

    static final AttachmentType<ManagedPlayer> MANAGED_PLAYER_ATTACHMENT = AttachmentRegistry.createPersistent(
        new ResourceLocation(Constants.MOD_ID, "managed_player"),
        new Codec<>() {
            @Override
            public <T> DataResult<T> encode(ManagedPlayer input, DynamicOps<T> ops, T prefix) {
                CompoundTag tag = input.serializeNBT();
                T converted = NbtOps.INSTANCE.convertTo(ops, tag);
                return DataResult.success(converted);
            }

            @Override
            public <T> DataResult<Pair<ManagedPlayer, T>> decode(DynamicOps<T> ops, T input) {
                if(ops instanceof NbtOps) {
                    CompoundTag tag = (CompoundTag) input;
                    return DataResult.success(Pair.of(new ManagedPlayer(tag), ops.empty()));
                }
                return DataResult.error(() -> "Not an NBT tag");
            }
        });

    static void onPlayerLoginRegisterAttachment(PlayerLoginEvent event) {
        if(event.getPlayer().hasAttached(MANAGED_PLAYER_ATTACHMENT)) return;
        event.getPlayer().setAttached(MANAGED_PLAYER_ATTACHMENT, new ManagedPlayer(event.getPlayer()));
    }
}
