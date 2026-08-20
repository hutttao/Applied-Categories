package org.lyy.mektmc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

import java.util.UUID;

public record CategorySetActivePacket(int menuId, UUID categoryId) implements CustomPacketPayload {
    public static final Type<CategorySetActivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_set_active"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategorySetActivePacket> STREAM_CODEC =
            StreamCodec.ofMember(CategorySetActivePacket::write, CategorySetActivePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        buf.writeUUID(categoryId);
    }

    private static CategorySetActivePacket read(RegistryFriendlyByteBuf buf) {
        return new CategorySetActivePacket(buf.readVarInt(), buf.readUUID());
    }
}
