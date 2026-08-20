package org.lyy.mektmc.network;

import appeng.api.stacks.AEKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

public record CategoryExtractPacket(int menuId, AEKey key, int mouseButton, boolean quickMove) implements CustomPacketPayload {
    public static final Type<CategoryExtractPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_extract"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryExtractPacket> STREAM_CODEC =
            StreamCodec.ofMember(CategoryExtractPacket::write, CategoryExtractPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        AEKey.STREAM_CODEC.encode(buf, key);
        buf.writeVarInt(mouseButton);
        buf.writeBoolean(quickMove);
    }

    private static CategoryExtractPacket read(RegistryFriendlyByteBuf buf) {
        return new CategoryExtractPacket(
                buf.readVarInt(),
                AEKey.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readBoolean());
    }
}
