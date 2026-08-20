package org.lyy.mektmc.network;

import appeng.api.stacks.AEKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

import java.util.UUID;

public record CategoryAssignPacket(int menuId, AEKey key, UUID categoryId) implements CategoryKeyPacket {
    public static final Type<CategoryAssignPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_assign"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryAssignPacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryKeyPacket::writeBase, CategoryAssignPacket::read);

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private static CategoryAssignPacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryAssignPacket(buf.readVarInt(), AEKey.STREAM_CODEC.decode(buf), buf.readUUID());
    }
}
