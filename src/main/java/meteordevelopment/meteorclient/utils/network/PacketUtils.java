/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import com.google.common.collect.Iterators;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import net.minecraft.network.Packet;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class PacketUtils {
    public static final Registry<Class<? extends Packet<?>>> REGISTRY = new PacketRegistry();

    private static final Map<Class<? extends Packet<?>>, String> S2C_PACKETS = new HashMap<>();
    private static final Map<Class<? extends Packet<?>>, String> C2S_PACKETS = new HashMap<>();

    private static final Map<String, Class<? extends Packet<?>>> S2C_PACKETS_R = new HashMap<>();
    private static final Map<String, Class<? extends Packet<?>>> C2S_PACKETS_R = new HashMap<>();

    static {
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.class, "ClientStatusC2SPacket");
        C2S_PACKETS_R.put("ClientStatusC2SPacket", net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket.class, "PlayerInteractItemC2SPacket");
        C2S_PACKETS_R.put("PlayerInteractItemC2SPacket", net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket.class, "SelectMerchantTradeC2SPacket");
        C2S_PACKETS_R.put("SelectMerchantTradeC2SPacket", net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class, "PlayerActionC2SPacket");
        C2S_PACKETS_R.put("PlayerActionC2SPacket", net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket.class, "CommandExecutionC2SPacket");
        C2S_PACKETS_R.put("CommandExecutionC2SPacket", net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.RenameItemC2SPacket.class, "RenameItemC2SPacket");
        C2S_PACKETS_R.put("RenameItemC2SPacket", net.minecraft.network.packet.c2s.play.RenameItemC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket.class, "LoginHelloC2SPacket");
        C2S_PACKETS_R.put("LoginHelloC2SPacket", net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket.class, "PlayerInteractBlockC2SPacket");
        C2S_PACKETS_R.put("PlayerInteractBlockC2SPacket", net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket.class, "QueryBlockNbtC2SPacket");
        C2S_PACKETS_R.put("QueryBlockNbtC2SPacket", net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.class, "PlayerInteractEntityC2SPacket");
        C2S_PACKETS_R.put("PlayerInteractEntityC2SPacket", net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket.class, "UpdatePlayerAbilitiesC2SPacket");
        C2S_PACKETS_R.put("UpdatePlayerAbilitiesC2SPacket", net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket.class, "RequestCommandCompletionsC2SPacket");
        C2S_PACKETS_R.put("RequestCommandCompletionsC2SPacket", net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket.class, "QueryRequestC2SPacket");
        C2S_PACKETS_R.put("QueryRequestC2SPacket", net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket.class, "UpdateCommandBlockC2SPacket");
        C2S_PACKETS_R.put("UpdateCommandBlockC2SPacket", net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket.class, "CustomPayloadC2SPacket");
        C2S_PACKETS_R.put("CustomPayloadC2SPacket", net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.HandSwingC2SPacket.class, "HandSwingC2SPacket");
        C2S_PACKETS_R.put("HandSwingC2SPacket", net.minecraft.network.packet.c2s.play.HandSwingC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket.class, "AdvancementTabC2SPacket");
        C2S_PACKETS_R.put("AdvancementTabC2SPacket", net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket.class, "ClickSlotC2SPacket");
        C2S_PACKETS_R.put("ClickSlotC2SPacket", net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket.class, "ClientSettingsC2SPacket");
        C2S_PACKETS_R.put("ClientSettingsC2SPacket", net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket.class, "SpectatorTeleportC2SPacket");
        C2S_PACKETS_R.put("SpectatorTeleportC2SPacket", net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket.class, "LoginKeyC2SPacket");
        C2S_PACKETS_R.put("LoginKeyC2SPacket", net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateDifficultyLockC2SPacket.class, "UpdateDifficultyLockC2SPacket");
        C2S_PACKETS_R.put("UpdateDifficultyLockC2SPacket", net.minecraft.network.packet.c2s.play.UpdateDifficultyLockC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket.class, "JigsawGeneratingC2SPacket");
        C2S_PACKETS_R.put("JigsawGeneratingC2SPacket", net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket.class, "QueryEntityNbtC2SPacket");
        C2S_PACKETS_R.put("QueryEntityNbtC2SPacket", net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket.class, "UpdateSelectedSlotC2SPacket");
        C2S_PACKETS_R.put("UpdateSelectedSlotC2SPacket", net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket.class, "RecipeCategoryOptionsC2SPacket");
        C2S_PACKETS_R.put("RecipeCategoryOptionsC2SPacket", net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class, "PlayerMoveC2SPacket");
        C2S_PACKETS_R.put("PlayerMoveC2SPacket", net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket.class, "PickFromInventoryC2SPacket");
        C2S_PACKETS_R.put("PickFromInventoryC2SPacket", net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket.class, "CloseHandledScreenC2SPacket");
        C2S_PACKETS_R.put("CloseHandledScreenC2SPacket", net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket.class, "BoatPaddleStateC2SPacket");
        C2S_PACKETS_R.put("BoatPaddleStateC2SPacket", net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket.class, "ChatMessageC2SPacket");
        C2S_PACKETS_R.put("ChatMessageC2SPacket", net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket.class, "ButtonClickC2SPacket");
        C2S_PACKETS_R.put("ButtonClickC2SPacket", net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket.class, "UpdateBeaconC2SPacket");
        C2S_PACKETS_R.put("UpdateBeaconC2SPacket", net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket.class, "UpdateSignC2SPacket");
        C2S_PACKETS_R.put("UpdateSignC2SPacket", net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket.class, "TeleportConfirmC2SPacket");
        C2S_PACKETS_R.put("TeleportConfirmC2SPacket", net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket.class, "UpdateStructureBlockC2SPacket");
        C2S_PACKETS_R.put("UpdateStructureBlockC2SPacket", net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateCommandBlockMinecartC2SPacket.class, "UpdateCommandBlockMinecartC2SPacket");
        C2S_PACKETS_R.put("UpdateCommandBlockMinecartC2SPacket", net.minecraft.network.packet.c2s.play.UpdateCommandBlockMinecartC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket.class, "KeepAliveC2SPacket");
        C2S_PACKETS_R.put("KeepAliveC2SPacket", net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket.class, "PlayerInputC2SPacket");
        C2S_PACKETS_R.put("PlayerInputC2SPacket", net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class, "ClientCommandC2SPacket");
        C2S_PACKETS_R.put("ClientCommandC2SPacket", net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateJigsawC2SPacket.class, "UpdateJigsawC2SPacket");
        C2S_PACKETS_R.put("UpdateJigsawC2SPacket", net.minecraft.network.packet.c2s.play.UpdateJigsawC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.query.QueryPingC2SPacket.class, "QueryPingC2SPacket");
        C2S_PACKETS_R.put("QueryPingC2SPacket", net.minecraft.network.packet.c2s.query.QueryPingC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.ResourcePackStatusC2SPacket.class, "ResourcePackStatusC2SPacket");
        C2S_PACKETS_R.put("ResourcePackStatusC2SPacket", net.minecraft.network.packet.c2s.play.ResourcePackStatusC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayPongC2SPacket.class, "PlayPongC2SPacket");
        C2S_PACKETS_R.put("PlayPongC2SPacket", net.minecraft.network.packet.c2s.play.PlayPongC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket.class, "CreativeInventoryActionC2SPacket");
        C2S_PACKETS_R.put("CreativeInventoryActionC2SPacket", net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket.class, "VehicleMoveC2SPacket");
        C2S_PACKETS_R.put("VehicleMoveC2SPacket", net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket.class, "BookUpdateC2SPacket");
        C2S_PACKETS_R.put("BookUpdateC2SPacket", net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.RequestChatPreviewC2SPacket.class, "RequestChatPreviewC2SPacket");
        C2S_PACKETS_R.put("RequestChatPreviewC2SPacket", net.minecraft.network.packet.c2s.play.RequestChatPreviewC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.RecipeBookDataC2SPacket.class, "RecipeBookDataC2SPacket");
        C2S_PACKETS_R.put("RecipeBookDataC2SPacket", net.minecraft.network.packet.c2s.play.RecipeBookDataC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket.class, "LoginQueryResponseC2SPacket");
        C2S_PACKETS_R.put("LoginQueryResponseC2SPacket", net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket.class, "HandshakeC2SPacket");
        C2S_PACKETS_R.put("HandshakeC2SPacket", net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.UpdateDifficultyC2SPacket.class, "UpdateDifficultyC2SPacket");
        C2S_PACKETS_R.put("UpdateDifficultyC2SPacket", net.minecraft.network.packet.c2s.play.UpdateDifficultyC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket.class, "CraftRequestC2SPacket");
        C2S_PACKETS_R.put("CraftRequestC2SPacket", net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround.class, "PlayerMoveC2SPacket.LookAndOnGround");
        C2S_PACKETS_R.put("PlayerMoveC2SPacket.LookAndOnGround", net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly.class, "PlayerMoveC2SPacket.OnGroundOnly");
        C2S_PACKETS_R.put("PlayerMoveC2SPacket.OnGroundOnly", net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround.class, "PlayerMoveC2SPacket.PositionAndOnGround");
        C2S_PACKETS_R.put("PlayerMoveC2SPacket.PositionAndOnGround", net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround.class);
        C2S_PACKETS.put(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full.class, "PlayerMoveC2SPacket.Full");
        C2S_PACKETS_R.put("PlayerMoveC2SPacket.Full", net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full.class);

        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldBorderSizeChangedS2CPacket.class, "WorldBorderSizeChangedS2CPacket");
        S2C_PACKETS_R.put("WorldBorderSizeChangedS2CPacket", net.minecraft.network.packet.s2c.play.WorldBorderSizeChangedS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket.class, "AdvancementUpdateS2CPacket");
        S2C_PACKETS_R.put("AdvancementUpdateS2CPacket", net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket.class, "CustomPayloadS2CPacket");
        S2C_PACKETS_R.put("CustomPayloadS2CPacket", net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldBorderInterpolateSizeS2CPacket.class, "WorldBorderInterpolateSizeS2CPacket");
        S2C_PACKETS_R.put("WorldBorderInterpolateSizeS2CPacket", net.minecraft.network.packet.s2c.play.WorldBorderInterpolateSizeS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket.class, "ChunkLoadDistanceS2CPacket");
        S2C_PACKETS_R.put("ChunkLoadDistanceS2CPacket", net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket.class, "ItemPickupAnimationS2CPacket");
        S2C_PACKETS_R.put("ItemPickupAnimationS2CPacket", net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket.class, "PlayerRespawnS2CPacket");
        S2C_PACKETS_R.put("PlayerRespawnS2CPacket", net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket.class, "PlayerListHeaderS2CPacket");
        S2C_PACKETS_R.put("PlayerListHeaderS2CPacket", net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket.class, "EntitySpawnS2CPacket");
        S2C_PACKETS_R.put("EntitySpawnS2CPacket", net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket.class, "SetCameraEntityS2CPacket");
        S2C_PACKETS_R.put("SetCameraEntityS2CPacket", net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.CraftFailedResponseS2CPacket.class, "CraftFailedResponseS2CPacket");
        S2C_PACKETS_R.put("CraftFailedResponseS2CPacket", net.minecraft.network.packet.s2c.play.CraftFailedResponseS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.StatisticsS2CPacket.class, "StatisticsS2CPacket");
        S2C_PACKETS_R.put("StatisticsS2CPacket", net.minecraft.network.packet.s2c.play.StatisticsS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket.class, "LoginQueryRequestS2CPacket");
        S2C_PACKETS_R.put("LoginQueryRequestS2CPacket", net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket.class, "VehicleMoveS2CPacket");
        S2C_PACKETS_R.put("VehicleMoveS2CPacket", net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket.class, "EntityAttributesS2CPacket");
        S2C_PACKETS_R.put("EntityAttributesS2CPacket", net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.StopSoundS2CPacket.class, "StopSoundS2CPacket");
        S2C_PACKETS_R.put("StopSoundS2CPacket", net.minecraft.network.packet.s2c.play.StopSoundS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket.class, "ScoreboardObjectiveUpdateS2CPacket");
        S2C_PACKETS_R.put("ScoreboardObjectiveUpdateS2CPacket", net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket.class, "EntitySetHeadYawS2CPacket");
        S2C_PACKETS_R.put("EntitySetHeadYawS2CPacket", net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket.class, "KeepAliveS2CPacket");
        S2C_PACKETS_R.put("KeepAliveS2CPacket", net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SelectAdvancementTabS2CPacket.class, "SelectAdvancementTabS2CPacket");
        S2C_PACKETS_R.put("SelectAdvancementTabS2CPacket", net.minecraft.network.packet.s2c.play.SelectAdvancementTabS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket.class, "SetTradeOffersS2CPacket");
        S2C_PACKETS_R.put("SetTradeOffersS2CPacket", net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket.class, "PlaySoundS2CPacket");
        S2C_PACKETS_R.put("PlaySoundS2CPacket", net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.BlockEventS2CPacket.class, "BlockEventS2CPacket");
        S2C_PACKETS_R.put("BlockEventS2CPacket", net.minecraft.network.packet.s2c.play.BlockEventS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket.class, "HealthUpdateS2CPacket");
        S2C_PACKETS_R.put("HealthUpdateS2CPacket", net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.class, "PlayerPositionLookS2CPacket");
        S2C_PACKETS_R.put("PlayerPositionLookS2CPacket", net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayPingS2CPacket.class, "PlayPingS2CPacket");
        S2C_PACKETS_R.put("PlayPingS2CPacket", net.minecraft.network.packet.s2c.play.PlayPingS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldEventS2CPacket.class, "WorldEventS2CPacket");
        S2C_PACKETS_R.put("WorldEventS2CPacket", net.minecraft.network.packet.s2c.play.WorldEventS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket.class, "ServerMetadataS2CPacket");
        S2C_PACKETS_R.put("ServerMetadataS2CPacket", net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket.class, "UpdateSelectedSlotS2CPacket");
        S2C_PACKETS_R.put("UpdateSelectedSlotS2CPacket", net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket.class, "ChunkDeltaUpdateS2CPacket");
        S2C_PACKETS_R.put("ChunkDeltaUpdateS2CPacket", net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket.class, "QueryResponseS2CPacket");
        S2C_PACKETS_R.put("QueryResponseS2CPacket", net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.TeamS2CPacket.class, "TeamS2CPacket");
        S2C_PACKETS_R.put("TeamS2CPacket", net.minecraft.network.packet.s2c.play.TeamS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket.class, "CooldownUpdateS2CPacket");
        S2C_PACKETS_R.put("CooldownUpdateS2CPacket", net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket.class, "OpenScreenS2CPacket");
        S2C_PACKETS_R.put("OpenScreenS2CPacket", net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket.class, "ExperienceOrbSpawnS2CPacket");
        S2C_PACKETS_R.put("ExperienceOrbSpawnS2CPacket", net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket.class, "EntityAnimationS2CPacket");
        S2C_PACKETS_R.put("EntityAnimationS2CPacket", net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket.class, "PlayerAbilitiesS2CPacket");
        S2C_PACKETS_R.put("PlayerAbilitiesS2CPacket", net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldBorderWarningBlocksChangedS2CPacket.class, "WorldBorderWarningBlocksChangedS2CPacket");
        S2C_PACKETS_R.put("WorldBorderWarningBlocksChangedS2CPacket", net.minecraft.network.packet.s2c.play.WorldBorderWarningBlocksChangedS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket.class, "EntitiesDestroyS2CPacket");
        S2C_PACKETS_R.put("EntitiesDestroyS2CPacket", net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.UnlockRecipesS2CPacket.class, "UnlockRecipesS2CPacket");
        S2C_PACKETS_R.put("UnlockRecipesS2CPacket", net.minecraft.network.packet.s2c.play.UnlockRecipesS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket.class, "LightUpdateS2CPacket");
        S2C_PACKETS_R.put("LightUpdateS2CPacket", net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket.class, "OverlayMessageS2CPacket");
        S2C_PACKETS_R.put("OverlayMessageS2CPacket", net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket.class, "WorldBorderInitializeS2CPacket");
        S2C_PACKETS_R.put("WorldBorderInitializeS2CPacket", net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldBorderCenterChangedS2CPacket.class, "WorldBorderCenterChangedS2CPacket");
        S2C_PACKETS_R.put("WorldBorderCenterChangedS2CPacket", net.minecraft.network.packet.s2c.play.WorldBorderCenterChangedS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket.class, "EntityVelocityUpdateS2CPacket");
        S2C_PACKETS_R.put("EntityVelocityUpdateS2CPacket", net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.DifficultyS2CPacket.class, "DifficultyS2CPacket");
        S2C_PACKETS_R.put("DifficultyS2CPacket", net.minecraft.network.packet.s2c.play.DifficultyS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.LookAtS2CPacket.class, "LookAtS2CPacket");
        S2C_PACKETS_R.put("LookAtS2CPacket", net.minecraft.network.packet.s2c.play.LookAtS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.TitleS2CPacket.class, "TitleS2CPacket");
        S2C_PACKETS_R.put("TitleS2CPacket", net.minecraft.network.packet.s2c.play.TitleS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.OpenHorseScreenS2CPacket.class, "OpenHorseScreenS2CPacket");
        S2C_PACKETS_R.put("OpenHorseScreenS2CPacket", net.minecraft.network.packet.s2c.play.OpenHorseScreenS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket.class, "ScreenHandlerPropertyUpdateS2CPacket");
        S2C_PACKETS_R.put("ScreenHandlerPropertyUpdateS2CPacket", net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket.class, "SimulationDistanceS2CPacket");
        S2C_PACKETS_R.put("SimulationDistanceS2CPacket", net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EnterCombatS2CPacket.class, "EnterCombatS2CPacket");
        S2C_PACKETS_R.put("EnterCombatS2CPacket", net.minecraft.network.packet.s2c.play.EnterCombatS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket.class, "DeathMessageS2CPacket");
        S2C_PACKETS_R.put("DeathMessageS2CPacket", net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket.class, "MapUpdateS2CPacket");
        S2C_PACKETS_R.put("MapUpdateS2CPacket", net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket.class, "ScreenHandlerSlotUpdateS2CPacket");
        S2C_PACKETS_R.put("ScreenHandlerSlotUpdateS2CPacket", net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.class, "BlockEntityUpdateS2CPacket");
        S2C_PACKETS_R.put("BlockEntityUpdateS2CPacket", net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SynchronizeTagsS2CPacket.class, "SynchronizeTagsS2CPacket");
        S2C_PACKETS_R.put("SynchronizeTagsS2CPacket", net.minecraft.network.packet.s2c.play.SynchronizeTagsS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket.class, "PlayerSpawnPositionS2CPacket");
        S2C_PACKETS_R.put("PlayerSpawnPositionS2CPacket", net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket.class, "EntityStatusEffectS2CPacket");
        S2C_PACKETS_R.put("EntityStatusEffectS2CPacket", net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket.class, "LoginCompressionS2CPacket");
        S2C_PACKETS_R.put("LoginCompressionS2CPacket", net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket.class, "CommandTreeS2CPacket");
        S2C_PACKETS_R.put("CommandTreeS2CPacket", net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket.class, "ClearTitleS2CPacket");
        S2C_PACKETS_R.put("ClearTitleS2CPacket", net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket.class, "TitleFadeS2CPacket");
        S2C_PACKETS_R.put("TitleFadeS2CPacket", net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChatPreviewS2CPacket.class, "ChatPreviewS2CPacket");
        S2C_PACKETS_R.put("ChatPreviewS2CPacket", net.minecraft.network.packet.s2c.play.ChatPreviewS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket.class, "PlayerSpawnS2CPacket");
        S2C_PACKETS_R.put("PlayerSpawnS2CPacket", net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.InventoryS2CPacket.class, "InventoryS2CPacket");
        S2C_PACKETS_R.put("InventoryS2CPacket", net.minecraft.network.packet.s2c.play.InventoryS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket.class, "PlayerActionResponseS2CPacket");
        S2C_PACKETS_R.put("PlayerActionResponseS2CPacket", net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket.class, "ResourcePackSendS2CPacket");
        S2C_PACKETS_R.put("ResourcePackSendS2CPacket", net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldBorderWarningTimeChangedS2CPacket.class, "WorldBorderWarningTimeChangedS2CPacket");
        S2C_PACKETS_R.put("WorldBorderWarningTimeChangedS2CPacket", net.minecraft.network.packet.s2c.play.WorldBorderWarningTimeChangedS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ScoreboardPlayerUpdateS2CPacket.class, "ScoreboardPlayerUpdateS2CPacket");
        S2C_PACKETS_R.put("ScoreboardPlayerUpdateS2CPacket", net.minecraft.network.packet.s2c.play.ScoreboardPlayerUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.query.QueryPongS2CPacket.class, "QueryPongS2CPacket");
        S2C_PACKETS_R.put("QueryPongS2CPacket", net.minecraft.network.packet.s2c.query.QueryPongS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChatPreviewStateChangeS2CPacket.class, "ChatPreviewStateChangeS2CPacket");
        S2C_PACKETS_R.put("ChatPreviewStateChangeS2CPacket", net.minecraft.network.packet.s2c.play.ChatPreviewStateChangeS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket.class, "ChatMessageS2CPacket");
        S2C_PACKETS_R.put("ChatMessageS2CPacket", net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket.class, "OpenWrittenBookS2CPacket");
        S2C_PACKETS_R.put("OpenWrittenBookS2CPacket", net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket.class, "PlaySoundFromEntityS2CPacket");
        S2C_PACKETS_R.put("PlaySoundFromEntityS2CPacket", net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket.class, "WorldTimeUpdateS2CPacket");
        S2C_PACKETS_R.put("WorldTimeUpdateS2CPacket", net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket.class, "SignEditorOpenS2CPacket");
        S2C_PACKETS_R.put("SignEditorOpenS2CPacket", net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ExplosionS2CPacket.class, "ExplosionS2CPacket");
        S2C_PACKETS_R.put("ExplosionS2CPacket", net.minecraft.network.packet.s2c.play.ExplosionS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket.class, "LoginDisconnectS2CPacket");
        S2C_PACKETS_R.put("LoginDisconnectS2CPacket", net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket.class, "RemoveEntityStatusEffectS2CPacket");
        S2C_PACKETS_R.put("RemoveEntityStatusEffectS2CPacket", net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EndCombatS2CPacket.class, "EndCombatS2CPacket");
        S2C_PACKETS_R.put("EndCombatS2CPacket", net.minecraft.network.packet.s2c.play.EndCombatS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.class, "PlayerListS2CPacket");
        S2C_PACKETS_R.put("PlayerListS2CPacket", net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.DisconnectS2CPacket.class, "DisconnectS2CPacket");
        S2C_PACKETS_R.put("DisconnectS2CPacket", net.minecraft.network.packet.s2c.play.DisconnectS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket.class, "ChunkRenderDistanceCenterS2CPacket");
        S2C_PACKETS_R.put("ChunkRenderDistanceCenterS2CPacket", net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket.class, "ExperienceBarUpdateS2CPacket");
        S2C_PACKETS_R.put("ExperienceBarUpdateS2CPacket", net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket.class, "BlockUpdateS2CPacket");
        S2C_PACKETS_R.put("BlockUpdateS2CPacket", net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket.class, "CommandSuggestionsS2CPacket");
        S2C_PACKETS_R.put("CommandSuggestionsS2CPacket", net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ParticleS2CPacket.class, "ParticleS2CPacket");
        S2C_PACKETS_R.put("ParticleS2CPacket", net.minecraft.network.packet.s2c.play.ParticleS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket.class, "CloseScreenS2CPacket");
        S2C_PACKETS_R.put("CloseScreenS2CPacket", net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket.class, "ScoreboardDisplayS2CPacket");
        S2C_PACKETS_R.put("ScoreboardDisplayS2CPacket", net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket.class, "PlaySoundIdS2CPacket");
        S2C_PACKETS_R.put("PlaySoundIdS2CPacket", net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket.class, "LoginSuccessS2CPacket");
        S2C_PACKETS_R.put("LoginSuccessS2CPacket", net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.GameMessageS2CPacket.class, "GameMessageS2CPacket");
        S2C_PACKETS_R.put("GameMessageS2CPacket", net.minecraft.network.packet.s2c.play.GameMessageS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket.class, "BlockBreakingProgressS2CPacket");
        S2C_PACKETS_R.put("BlockBreakingProgressS2CPacket", net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket.class, "EntityPassengersSetS2CPacket");
        S2C_PACKETS_R.put("EntityPassengersSetS2CPacket", net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket.class, "LoginHelloS2CPacket");
        S2C_PACKETS_R.put("LoginHelloS2CPacket", net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.GameJoinS2CPacket.class, "GameJoinS2CPacket");
        S2C_PACKETS_R.put("GameJoinS2CPacket", net.minecraft.network.packet.s2c.play.GameJoinS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket.class, "SynchronizeRecipesS2CPacket");
        S2C_PACKETS_R.put("SynchronizeRecipesS2CPacket", net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityS2CPacket.class, "EntityS2CPacket");
        S2C_PACKETS_R.put("EntityS2CPacket", net.minecraft.network.packet.s2c.play.EntityS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket.class, "EntityTrackerUpdateS2CPacket");
        S2C_PACKETS_R.put("EntityTrackerUpdateS2CPacket", net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket.class, "EntityStatusS2CPacket");
        S2C_PACKETS_R.put("EntityStatusS2CPacket", net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.SubtitleS2CPacket.class, "SubtitleS2CPacket");
        S2C_PACKETS_R.put("SubtitleS2CPacket", net.minecraft.network.packet.s2c.play.SubtitleS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.NbtQueryResponseS2CPacket.class, "NbtQueryResponseS2CPacket");
        S2C_PACKETS_R.put("NbtQueryResponseS2CPacket", net.minecraft.network.packet.s2c.play.NbtQueryResponseS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket.class, "EntityEquipmentUpdateS2CPacket");
        S2C_PACKETS_R.put("EntityEquipmentUpdateS2CPacket", net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket.class, "UnloadChunkS2CPacket");
        S2C_PACKETS_R.put("UnloadChunkS2CPacket", net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket.class, "EntityAttachS2CPacket");
        S2C_PACKETS_R.put("EntityAttachS2CPacket", net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.BossBarS2CPacket.class, "BossBarS2CPacket");
        S2C_PACKETS_R.put("BossBarS2CPacket", net.minecraft.network.packet.s2c.play.BossBarS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket.class, "EntityPositionS2CPacket");
        S2C_PACKETS_R.put("EntityPositionS2CPacket", net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket.class, "ChunkDataS2CPacket");
        S2C_PACKETS_R.put("ChunkDataS2CPacket", net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket.class, "GameStateChangeS2CPacket");
        S2C_PACKETS_R.put("GameStateChangeS2CPacket", net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityS2CPacket.RotateAndMoveRelative.class, "EntityS2CPacket.RotateAndMoveRelative");
        S2C_PACKETS_R.put("EntityS2CPacket.RotateAndMoveRelative", net.minecraft.network.packet.s2c.play.EntityS2CPacket.RotateAndMoveRelative.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityS2CPacket.Rotate.class, "EntityS2CPacket.Rotate");
        S2C_PACKETS_R.put("EntityS2CPacket.Rotate", net.minecraft.network.packet.s2c.play.EntityS2CPacket.Rotate.class);
        S2C_PACKETS.put(net.minecraft.network.packet.s2c.play.EntityS2CPacket.MoveRelative.class, "EntityS2CPacket.MoveRelative");
        S2C_PACKETS_R.put("EntityS2CPacket.MoveRelative", net.minecraft.network.packet.s2c.play.EntityS2CPacket.MoveRelative.class);
    }

    public static String getName(Class<? extends Packet<?>> packetClass) {
        String name = S2C_PACKETS.get(packetClass);
        if (name != null) return name;
        return C2S_PACKETS.get(packetClass);
    }

    public static Class<? extends Packet<?>> getPacket(String name) {
        Class<? extends Packet<?>> packet = S2C_PACKETS_R.get(name);
        if (packet != null) return packet;
        return C2S_PACKETS_R.get(name);
    }

    public static Set<Class<? extends Packet<?>>> getS2CPackets() {
        return S2C_PACKETS.keySet();
    }

    public static Set<Class<? extends Packet<?>>> getC2SPackets() {
        return C2S_PACKETS.keySet();
    }

    private static class PacketRegistry extends Registry<Class<? extends Packet<?>>> {
        public PacketRegistry() {
            super(RegistryKey.ofRegistry(new MeteorIdentifier("packets")), Lifecycle.stable());
        }

        @Override
        public int size() {
            return S2C_PACKETS.keySet().size() + C2S_PACKETS.keySet().size();
        }

        @Override
        public Identifier getId(Class<? extends Packet<?>> entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<Class<? extends Packet<?>>>> getKey(Class<? extends Packet<?>> entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(Class<? extends Packet<?>> entry) {
            return 0;
        }

        @Override
        public Class<? extends Packet<?>> get(RegistryKey<Class<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public Class<? extends Packet<?>> get(Identifier id) {
            return null;
        }

        @Override
        public Lifecycle getEntryLifecycle(Class<? extends Packet<?>> object) {
            return null;
        }

        @Override
        public Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public Set<Identifier> getIds() {
            return null;
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @Override
        public Class<? extends Packet<?>> get(int index) {
            return null;
        }

        @NotNull
        @Override
        public Iterator<Class<? extends Packet<?>>> iterator() {
            return Iterators.concat(S2C_PACKETS.keySet().iterator(), C2S_PACKETS.keySet().iterator());
        }

        @Override
        public boolean contains(RegistryKey<Class<? extends Packet<?>>> key) {
            return false;
        }

        @Override
        public Set<Map.Entry<RegistryKey<Class<? extends Packet<?>>>, Class<? extends Packet<?>>>> getEntrySet() {
            return null;
        }

        @Override
        public Optional<RegistryEntry<Class<? extends Packet<?>>>> getRandom(Random random) {
            return Optional.empty();
        }

        @Override
        public Registry<Class<? extends Packet<?>>> freeze() {
            return null;
        }

        @Override
        public RegistryEntry<Class<? extends Packet<?>>> getOrCreateEntry(RegistryKey<Class<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public RegistryEntry.Reference<Class<? extends Packet<?>>> createEntry(Class<? extends Packet<?>> value) {
            return null;
        }

        @Override
        public Optional<RegistryEntry<Class<? extends Packet<?>>>> getEntry(int rawId) {
            return Optional.empty();
        }

        @Override
        public Optional<RegistryEntry<Class<? extends Packet<?>>>> getEntry(RegistryKey<Class<? extends Packet<?>>> key) {
            return Optional.empty();
        }

        @Override
        public Stream<RegistryEntry.Reference<Class<? extends Packet<?>>>> streamEntries() {
            return null;
        }

        @Override
        public Optional<RegistryEntryList.Named<Class<? extends Packet<?>>>> getEntryList(TagKey<Class<? extends Packet<?>>> tag) {
            return Optional.empty();
        }

        @Override
        public RegistryEntryList.Named<Class<? extends Packet<?>>> getOrCreateEntryList(TagKey<Class<? extends Packet<?>>> tag) {
            return null;
        }

        @Override
        public Stream<Pair<TagKey<Class<? extends Packet<?>>>, RegistryEntryList.Named<Class<? extends Packet<?>>>>> streamTagsAndEntries() {
            return null;
        }

        @Override
        public Stream<TagKey<Class<? extends Packet<?>>>> streamTags() {
            return null;
        }

        @Override
        public boolean containsTag(TagKey<Class<? extends Packet<?>>> tag) {
            return false;
        }

        @Override
        public void clearTags() {}

        @Override
        public void populateTags(Map<TagKey<Class<? extends Packet<?>>>, List<RegistryEntry<Class<? extends Packet<?>>>>> tagEntries) {}

        @Override
        public DataResult<RegistryEntry<Class<? extends Packet<?>>>> getOrCreateEntryDataResult(RegistryKey<Class<? extends Packet<?>>> key) {
            return null;
        }

        @Override
        public Set<RegistryKey<Class<? extends Packet<?>>>> getKeys() {
            return null;
        }
    }
}
