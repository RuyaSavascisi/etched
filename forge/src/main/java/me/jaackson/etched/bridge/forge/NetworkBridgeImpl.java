package me.jaackson.etched.bridge.forge;

import me.jaackson.etched.Etched;
import me.jaackson.etched.common.network.EtchedPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Jackson
 */
public class NetworkBridgeImpl {
    public static final SimpleChannel PLAY = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Etched.MOD_ID, "play"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions("1"::equals)
            .serverAcceptedVersions("1"::equals)
            .simpleChannel();
    private static int currentIndex = -1;

    public static <T extends EtchedPacket> void playToClient(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, Consumer<T> handle) {
        PLAY.registerMessage(currentIndex++, messageType, EtchedPacket::write, read, (packet, context) -> {
            NetworkEvent.Context ctx = context.get();
            if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                ctx.enqueueWork(() -> handle.accept(packet));
                ctx.setPacketHandled(true);
            }
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static <T extends EtchedPacket> void playToServer(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, BiConsumer<T, Player> handle) {
        PLAY.registerMessage(currentIndex++, messageType, EtchedPacket::write, read, (packet, context) -> {
            NetworkEvent.Context ctx = context.get();
            if (ctx.getDirection().getReceptionSide() == LogicalSide.SERVER) {
                ctx.enqueueWork(() ->
                {
                    if (ctx.getSender() == null)
                        return;

                    ServerPlayer player = ctx.getSender();
                    handle.accept(packet, player);
                });
                ctx.setPacketHandled(true);
            }
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(ResourceLocation channel, ServerPlayer player, EtchedPacket packet) {
        PLAY.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTracking(ResourceLocation channel, Entity tracking, EtchedPacket packet) {
        PLAY.send(PacketDistributor.TRACKING_ENTITY.with(() -> tracking), packet);
    }

    public static void sendToNear(ResourceLocation channel, ServerLevel level, double x, double y, double z, double distance, EtchedPacket packet) {
        PLAY.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, distance * distance, level.dimension())), packet);
    }

    public static void sendToServer(ResourceLocation channel, EtchedPacket packet) {
        PLAY.sendToServer(packet);
    }
}