package org.lyy.mektmc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

import java.util.UUID;

public record CategoryRenamePacket(int menuId, UUID categoryId, String name) implements CustomPacketPayload {
    public static final Type<CategoryRenamePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryRenamePacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryRenamePacket::write, CategoryRenamePacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        buf.writeUUID(categoryId);
        buf.writeUtf(name);
    }

    private static CategoryRenamePacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryRenamePacket(buf.readVarInt(), buf.readUUID(), buf.readUtf());
    }
}
