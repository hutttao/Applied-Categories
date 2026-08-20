package org.lyy.appcategories.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.appcategories.AppliedCategories;

import java.util.UUID;

public record CategoryDeletePacket(int menuId, UUID categoryId) implements CustomPacketPayload {
    public static final Type<CategoryDeletePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "category_delete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryDeletePacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryDeletePacket::write, CategoryDeletePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        buf.writeUUID(categoryId);
    }

    private static CategoryDeletePacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryDeletePacket(buf.readVarInt(), buf.readUUID());
    }
}
