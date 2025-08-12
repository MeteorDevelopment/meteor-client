package meteordevelopment.meteorclient.utils.network;

import com.mojang.serialization.Codec;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
/**
 * @author NDev007
 */

public class NetworkHandler {
    public static final Identifier ALLOW_MIDAIR_BLOCKS = Identifier.of(MeteorClient.MOD_ID, "allow_midair_blocks");

    public static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(AllowMidairBlocksPayload.ID, AllowMidairBlocksPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(AllowMidairBlocksPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MeteorClient.AllowMidAirGhostBlocks = true;
            });
        });
    }

    public static void sendAllowMidAirBlocks(ServerPlayerEntity player, boolean allow) {
        ServerPlayNetworking.send(player, new AllowMidairBlocksPayload(allow));
    }

    public static record AllowMidairBlocksPayload(Boolean allowPlace) implements CustomPayload {
        public static final CustomPayload.Id<AllowMidairBlocksPayload> ID =
            new CustomPayload.Id<>(ALLOW_MIDAIR_BLOCKS);

        public static final PacketCodec<PacketByteBuf, AllowMidairBlocksPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOLEAN, AllowMidairBlocksPayload::allowPlace, AllowMidairBlocksPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
