package org.lyy.mektmc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

public record CategoryRequestSnapshotPacket(int menuId) implements CustomPacketPayload {
    public static final Type<CategoryRequestSnapshotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_request_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryRequestSnapshotPacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryRequestSnapshotPacket::write, CategoryRequestSnapshotPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
    }

    private static CategoryRequestSnapshotPacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryRequestSnapshotPacket(buf.readVarInt());
    }
}
