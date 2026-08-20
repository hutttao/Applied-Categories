package org.lyy.mektmc.network;

import appeng.api.stacks.AEKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public interface CategoryKeyPacket extends CustomPacketPayload {
    int menuId();

    AEKey key();

    UUID categoryId();

    static void writeBase(CategoryKeyPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.menuId());
        AEKey.STREAM_CODEC.encode(buf, packet.key());
        buf.writeUUID(packet.categoryId());
    }
}
