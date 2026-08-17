package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.player.ManagedPlayer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.attachment.DataAttachmentLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;


public class ManagedPlayerAttachment {

    private static final String ATTACHMENT_NAME = "managed_player";

    private static DataAttachmentLookup<ManagedPlayer> ATTACHMENT_TYPE;

    // Retained for API parity / callers that referenced it; no longer performs registration.
    static void init() {}

    static final Codec<ManagedPlayer> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(ManagedPlayer input, DynamicOps<T> ops, T prefix) {
            CompoundTag tag = ManagedPlayer.serialize(input);
            return DataResult.success(NbtOps.INSTANCE.convertTo(ops, tag));
        }

        @Override
        public <T> DataResult<Pair<ManagedPlayer, T>> decode(DynamicOps<T> ops, T input) {

            CompoundTag tag = ops.convertTo(NbtOps.INSTANCE, input) instanceof CompoundTag c ? c : null;
            if (tag == null || tag.isEmpty()) {
                return DataResult.error(() -> "ManagedPlayer attachment: expected a CompoundTag");
            }

            ManagedPlayer mp = ManagedPlayer.getManagedPlayer(tag);
            if (mp == null) mp = new ManagedPlayer(tag);
            return DataResult.success(Pair.of(mp, ops.empty()));
        }
    };


    public static void register(BalmRegistrars registrars) {
        registrars.dataAttachmentTypes(r -> ATTACHMENT_TYPE = r.register(ATTACHMENT_NAME, CODEC
        ).asLookup());
        Balm.getEvents().onEvent(
            PlayerLoginEvent.class, ManagedPlayerAttachment::onPlayerLogin, EventPriority.Highest);
    }

    static void onPlayerLogin(PlayerLoginEvent event) {
        Player p = event.getPlayer();
        if (p == null || p.level().isClientSide()) return;
        ManagedPlayer.onPlayerLogin(event);
    }

}
