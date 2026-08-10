package com.holybuckets.foundation.capability;

import com.holybuckets.foundation.player.ManagedPlayer;
import com.holybuckets.foundation.event.balm.PlayerLoginEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ManagedPlayerAttachment {

    static void init() {}

    // NBT key the serialized ManagedPlayer data is stored under within the player's ValueInput/ValueOutput.
    private static final String NBT_KEY = "data";

    static final Map<String, CompoundTag> PENDING_PLAYERS = new HashMap<>();

    static final Supplier<AttachmentType<ManagedPlayer>> MANAGED_PLAYER_ATTACHMENT =
        FoundationAttachments.ATTACHMENT_TYPES.register("managed_player",
            () -> AttachmentType.builder(() -> (ManagedPlayer) null)
                .serialize(new IAttachmentSerializer<ManagedPlayer>() {

                    @Override
                    public ManagedPlayer read(IAttachmentHolder holder, ValueInput input) {
                        CompoundTag tag = input.read(NBT_KEY, CompoundTag.CODEC).orElse(null);

                        if (tag == null || tag.isEmpty()) {
                            return null;
                        }

                        ManagedPlayer mp = ManagedPlayer.getManagedPlayer(tag);
                        if (mp == null) {
                            String id = ManagedPlayer.getIdFromTag(tag);
                            if (id != null) {
                                PENDING_PLAYERS.put(id, tag);
                            }
                            return null;
                        }

                        ManagedPlayer.deserialize(mp, tag);
                        return mp;
                    }

                    @Override
                    public boolean write(ManagedPlayer attachment, ValueOutput output) {
                        // Don't serialize a null attachment (matches the null default supplier).
                        if (attachment == null) return false;
                        CompoundTag tag = ManagedPlayer.serialize(attachment);
                        if (tag == null || tag.isEmpty()) return false;
                        output.store(NBT_KEY, CompoundTag.CODEC, tag);
                        return true;
                    }
                })
                .copyOnDeath()
                .build()
        );

    static void onPlayerLoginRegisterAttachment(PlayerLoginEvent event) {
        Player p = event.getPlayer();
        ManagedPlayer.onPlayerLogin(event);
        ManagedPlayer mp = ManagedPlayer.getManagedPlayer(p);
        p.setData(MANAGED_PLAYER_ATTACHMENT, mp);

        ManagedPlayer.deserialize(mp, PENDING_PLAYERS.remove(mp.getId()));
    }
}
