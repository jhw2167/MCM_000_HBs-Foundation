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
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ManagedPlayerAttachment {
    
    static void init() {}

    static final Map<String, CompoundTag> PENDING_PLAYERS = new HashMap<>();

    static final AttachmentType<ManagedPlayer> MANAGED_PLAYER_ATTACHMENT = AttachmentRegistry.createPersistent(
        new ResourceLocation(Constants.MOD_ID, "managed_player"),
        new Codec<>() {
            @Override
            public <T> DataResult<T> encode(ManagedPlayer input, DynamicOps<T> ops, T prefix) {
                CompoundTag tag = ManagedPlayer.serialize(input);
                T converted = NbtOps.INSTANCE.convertTo(ops, tag);
                return DataResult.success(converted);
            }

            private static final Supplier<String> ERROR_NO_ID = () -> "Player id not found in tag. Cannot deserialize Managed Player.";
            private static final Function<String,Supplier<String>> ERROR_NO_PLAYER = (id) -> { return () -> "Player with id " + id + " not found on reload, will wait for player join"; };
            @Override
            public <T> DataResult<Pair<ManagedPlayer, T>> decode(DynamicOps<T> ops, T input)
            {
                if (input instanceof CompoundTag tag) {
                    ManagedPlayer mp = ManagedPlayer.getManagedPlayer(tag);
                    if (mp == null) {
                        String id = ManagedPlayer.getIdFromTag(tag);
                        if (id == null) return DataResult.error(ERROR_NO_ID);
                        PENDING_PLAYERS.put(id, tag);
                        return DataResult.success(Pair.of(null, ops.empty()));
                    } else {
                        ManagedPlayer.deserialize(mp, tag);
                        return DataResult.success(Pair.of(mp, ops.empty()));
                    }
                }
                return DataResult.error(() -> "Not an NBT tag");
            }
        });

    static void onPlayerLoginRegisterAttachment(PlayerLoginEvent event) {
        Player p = event.getPlayer();
        ManagedPlayer.onPlayerLogin(event);
        ManagedPlayer mp = ManagedPlayer.getManagedPlayer(p);
        if(!p.hasAttached(MANAGED_PLAYER_ATTACHMENT))
            p.setAttached(MANAGED_PLAYER_ATTACHMENT, mp);

        ManagedPlayer.deserialize(mp, PENDING_PLAYERS.remove(mp.getId()));
    }
}
