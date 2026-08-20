package org.lyy.mektmc.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lyy.mektmc.Mektmc;
import org.lyy.mektmc.client.CategoryClientCache;
import org.lyy.mektmc.menu.CategorizedTerminalMenu;

public final class NetworkHandler {
    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Mektmc.MODID).versioned("1");
        registrar.playToClient(CategorySnapshotPacket.TYPE, CategorySnapshotPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> CategoryClientCache.apply(packet)));
        registrar.playToServer(CategoryRequestSnapshotPacket.TYPE, CategoryRequestSnapshotPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> sendSnapshot(context.player(), menu))));
        registrar.playToServer(CategorySetActivePacket.TYPE, CategorySetActivePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.setActiveCategory(packet.categoryId());
                    sendSnapshot(context.player(), menu);
                })));
        registrar.playToServer(CategoryCreatePacket.TYPE, CategoryCreatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.createCategory(packet.name(), packet.color());
                    sendSnapshot(context.player(), menu);
                })));
        registrar.playToServer(CategoryDeletePacket.TYPE, CategoryDeletePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.deleteCategory(packet.categoryId());
                    sendSnapshot(context.player(), menu);
                })));
        registrar.playToServer(CategoryRenamePacket.TYPE, CategoryRenamePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.renameCategory(packet.categoryId(), packet.name());
                    sendSnapshot(context.player(), menu);
                })));
        registrar.playToServer(CategoryAssignPacket.TYPE, CategoryAssignPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.assign(packet.key(), packet.categoryId());
                    sendSnapshot(context.player(), menu);
                })));
        registrar.playToServer(CategoryRemovePacket.TYPE, CategoryRemovePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.remove(packet.key(), packet.categoryId());
                    sendSnapshot(context.player(), menu);
                })));
        registrar.playToServer(CategoryExtractPacket.TYPE, CategoryExtractPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        withMenu(serverPlayer, packet.menuId(), menu -> {
                            menu.handleGridClick(serverPlayer, packet.key(), packet.mouseButton(), packet.quickMove());
                            sendSnapshot(serverPlayer, menu);
                        });
                    }
                }));
        registrar.playToServer(CategoryInsertCarriedPacket.TYPE, CategoryInsertCarriedPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> withMenu(context.player(), packet.menuId(), menu -> {
                    menu.insertCarried(packet.singleItem());
                    sendSnapshot(context.player(), menu);
                })));
    }

    private static void withMenu(net.minecraft.world.entity.player.Player player, int menuId, java.util.function.Consumer<CategorizedTerminalMenu> action) {
        if (player.containerMenu instanceof CategorizedTerminalMenu menu && menu.containerId == menuId) {
            action.accept(menu);
        }
    }

    private static void sendSnapshot(net.minecraft.world.entity.player.Player player, CategorizedTerminalMenu menu) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, menu.createSnapshot());
        }
    }
}
