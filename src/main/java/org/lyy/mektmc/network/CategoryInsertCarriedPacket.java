package org.lyy.mektmc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

public record CategoryInsertCarriedPacket(int menuId, boolean singleItem) implements CustomPacketPayload {
    public static final Type<CategoryInsertCarriedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_insert_carried"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryInsertCarriedPacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryInsertCarriedPacket::write, CategoryInsertCarriedPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        buf.writeBoolean(singleItem);
    }

    private static CategoryInsertCarriedPacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryInsertCarriedPacket(buf.readVarInt(), buf.readBoolean());
    }
}
