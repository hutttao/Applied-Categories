package org.lyy.mektmc.network;

import appeng.api.stacks.AEKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lyy.mektmc.Mektmc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CategorySnapshotPacket(
        int menuId,
        UUID activeCategory,
        Status status,
        List<CategoryEntry> categories,
        List<StackEntry> stacks
) implements CustomPacketPayload {
    public static final Type<CategorySnapshotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "category_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CategorySnapshotPacket> STREAM_CODEC =
            StreamCodec.ofMember(CategorySnapshotPacket::write, CategorySnapshotPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(menuId);
        buf.writeUUID(activeCategory);
        buf.writeEnum(status);
        buf.writeVarInt(categories.size());
        for (var category : categories) {
            category.write(buf);
        }
        buf.writeVarInt(stacks.size());
        for (var stack : stacks) {
            stack.write(buf);
        }
    }

    private static CategorySnapshotPacket read(RegistryFriendlyByteBuf buf) {
        int menuId = buf.readVarInt();
        UUID activeCategory = buf.readUUID();
        Status status = buf.readEnum(Status.class);
        int categoryCount = buf.readVarInt();
        var categories = new ArrayList<CategoryEntry>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            categories.add(CategoryEntry.read(buf));
        }
        int stackCount = buf.readVarInt();
        var stacks = new ArrayList<StackEntry>(stackCount);
        for (int i = 0; i < stackCount; i++) {
            stacks.add(StackEntry.read(buf));
        }
        return new CategorySnapshotPacket(menuId, activeCategory, status, categories, stacks);
    }

    public enum Status {
        OK,
        OFFLINE,
        NO_INDEX,
        CONFLICT
    }

    public record CategoryEntry(UUID id, String name, int color, int sortOrder, boolean builtin) {
        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(name);
            buf.writeVarInt(color);
            buf.writeVarInt(sortOrder);
            buf.writeBoolean(builtin);
        }

        private static CategoryEntry read(RegistryFriendlyByteBuf buf) {
            return new CategoryEntry(buf.readUUID(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
        }
    }

    public record StackEntry(AEKey key, long amount, Set<UUID> categories) {
        public static StackEntry fromKey(AEKey key, long amount, Set<UUID> categories) {
            return new StackEntry(key, amount, Set.copyOf(categories));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            AEKey.STREAM_CODEC.encode(buf, key);
            buf.writeVarLong(amount);
            buf.writeVarInt(categories.size());
            for (var category : categories) {
                buf.writeUUID(category);
            }
        }

        private static StackEntry read(RegistryFriendlyByteBuf buf) {
            AEKey key = AEKey.STREAM_CODEC.decode(buf);
            long amount = buf.readVarLong();
            int count = buf.readVarInt();
            var categories = new LinkedHashSet<UUID>();
            for (int i = 0; i < count; i++) {
                categories.add(buf.readUUID());
            }
            return new StackEntry(key, amount, categories);
        }
    }
}
