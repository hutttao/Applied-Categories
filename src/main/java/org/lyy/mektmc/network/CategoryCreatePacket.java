package org.lyy.mektmc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

public record CategoryCreatePacket(int menuId, String name, int color) implements CustomPacketPayload {
    public static final Type<CategoryCreatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_create"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryCreatePacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryCreatePacket::write, CategoryCreatePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        buf.writeUtf(name);
        buf.writeVarInt(color);
    }

    private static CategoryCreatePacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryCreatePacket(buf.readVarInt(), buf.readUtf(), buf.readVarInt());
    }
}
