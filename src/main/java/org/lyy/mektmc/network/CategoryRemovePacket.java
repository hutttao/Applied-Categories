package org.lyy.mektmc.network;

import appeng.api.stacks.AEKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

import java.util.UUID;

public record CategoryRemovePacket(int menuId, AEKey key, UUID categoryId) implements CategoryKeyPacket {
    public static final Type<CategoryRemovePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryRemovePacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryKeyPacket::writeBase, CategoryRemovePacket::read);

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private static CategoryRemovePacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryRemovePacket(buf.readVarInt(), AEKey.STREAM_CODEC.decode(buf), buf.readUUID());
    }
}
