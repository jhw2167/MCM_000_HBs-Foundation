package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.player.ManagedPlayer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ManagedPlayerAttachment {

    static void init() {}

    static final Map<String, CompoundTag> PENDING_PLAYERS = new HashMap<>();

    static final Supplier<AttachmentType<ManagedPlayer>> MANAGED_PLAYER_ATTACHMENT =
        FoundationAttachments.ATTACHMENT_TYPES.register("managed_player",
            () -> AttachmentType.builder(() -> (ManagedPlayer) null)
                .serialize(new Codec<>() {
                    @Override
                    public <T> DataResult<T> encode(ManagedPlayer input, DynamicOps<T> ops, T prefix) {
                        CompoundTag tag = ManagedPlayer.serialize(input);
                        T converted = NbtOps.INSTANCE.convertTo(ops, tag);
                        return DataResult.success(converted);
                    }

                    private static final Supplier<String> ERROR_NO_ID =
                        () -> "Player id not found in tag. Cannot deserialize Managed Player.";
                    private static final Function<String, Supplier<String>> ERROR_NO_PLAYER =
                        (id) -> () -> "Player with id " + id + " not found. Cannot deserialize Managed Player.";

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
                })
                .build()
        );

    static void onPlayerLoginRegisterAttachment(PlayerLoginEvent event) {
        Player p = event.getPlayer();
        ManagedPlayer.onPlayerLogin(event);
        ManagedPlayer mp = ManagedPlayer.getManagedPlayer(p);
        if (!p.hasData(MANAGED_PLAYER_ATTACHMENT))
            p.setData(MANAGED_PLAYER_ATTACHMENT, mp);

        ManagedPlayer.deserialize(mp, PENDING_PLAYERS.remove(mp.getId()));
    }
}
