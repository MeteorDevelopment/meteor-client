/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.configuration.ConfigurationPacketTypes;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakePacketTypes;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginPacketTypes;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import net.minecraft.network.protocol.status.StatusPacketTypes;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class PacketUtils {
    private static final Map<Identifier, PacketType<? extends @NotNull Packet<?>>> CLIENTBOUND_PACKETS_MAP;
    private static final Map<Identifier, PacketType<? extends @NotNull Packet<?>>> SERVERBOUND_PACKETS_MAP;
    private static final Set<PacketType<? extends @NotNull Packet<?>>> CLIENTBOUND_PACKETS;
    private static final Set<PacketType<? extends @NotNull Packet<?>>> SERVERBOUND_PACKETS;

    public static Set<PacketType<? extends @NotNull Packet<?>>> getPackets() {
        return Sets.union(CLIENTBOUND_PACKETS, SERVERBOUND_PACKETS);
    }

    public static Set<PacketType<? extends @NotNull Packet<?>>> getClientboundPackets() {
        return CLIENTBOUND_PACKETS;
    }

    public static Set<PacketType<? extends @NotNull Packet<?>>> getServerboundPackets() {
        return SERVERBOUND_PACKETS;
    }

    public static @Nullable PacketType<? extends @NotNull Packet<?>> getClientboundPacket(Identifier id) {
        return CLIENTBOUND_PACKETS_MAP.get(id);
    }

    public static @Nullable PacketType<? extends @NotNull Packet<?>> getServerboundPacket(Identifier id) {
        return SERVERBOUND_PACKETS_MAP.get(id);
    }

    public static @Nullable PacketType<? extends @NotNull Packet<?>> getPacket(Identifier id) {
        @Nullable PacketType<? extends @NotNull Packet<?>> clientbound = getClientboundPacket(id);
        return clientbound != null ? clientbound : getServerboundPacket(id);
    }

    public static @Nullable PacketType<? extends @NotNull Packet<?>> getPacket(String name) {
        if (name.startsWith("clientbound/")) {
            @Nullable Identifier identifier = Identifier.tryParse(name.substring(12));
            return CLIENTBOUND_PACKETS_MAP.get(identifier);
        }

        if (name.startsWith("serverbound/")) {
            @Nullable Identifier identifier = Identifier.tryParse(name.substring(12));
            return SERVERBOUND_PACKETS_MAP.get(identifier);
        }

        @Nullable Identifier identifier = Identifier.tryParse(name);
        if (identifier != null) {
            @Nullable PacketType<? extends @NotNull Packet<?>> type = getPacket(identifier);
            if (type != null) return type;
        }

        return LEGACY_PACKET_MAPPINGS.get(name);
    }

    static {
        ImmutableMap.Builder<@NotNull Identifier, @NotNull PacketType<? extends @NotNull Packet<?>>> clientbound = ImmutableMap.builder();
        ImmutableMap.Builder<@NotNull Identifier, @NotNull PacketType<? extends @NotNull Packet<?>>> serverbound = ImmutableMap.builder();

        Stream.of(
                StatusProtocols.CLIENTBOUND_TEMPLATE,
                LoginProtocols.CLIENTBOUND_TEMPLATE,
                ConfigurationProtocols.CLIENTBOUND_TEMPLATE,
                GameProtocols.CLIENTBOUND_TEMPLATE
            ).map(ProtocolInfo.DetailsProvider::details)
            .forEach(details -> details.listPackets((type, _) -> clientbound.put(type.id(), type)));

        Stream.of(
                HandshakeProtocols.SERVERBOUND_TEMPLATE,
                StatusProtocols.SERVERBOUND_TEMPLATE,
                LoginProtocols.SERVERBOUND_TEMPLATE,
                ConfigurationProtocols.SERVERBOUND_TEMPLATE,
                GameProtocols.SERVERBOUND_TEMPLATE
            ).map(ProtocolInfo.DetailsProvider::details)
            .forEach(details -> details.listPackets((type, _) -> serverbound.put(type.id(), type)));

        CLIENTBOUND_PACKETS_MAP = clientbound.buildKeepingLast();
        SERVERBOUND_PACKETS_MAP = serverbound.buildKeepingLast();

        CLIENTBOUND_PACKETS = Set.copyOf(CLIENTBOUND_PACKETS_MAP.values());
        SERVERBOUND_PACKETS = Set.copyOf(SERVERBOUND_PACKETS_MAP.values());
    }

    /**
     * Maps our legacy packet names to modern packet types.
     * @implNote Do not update keys or add entries, only update values.
     */
    private static final Map<String, PacketType<? extends @NotNull Packet<?>>> LEGACY_PACKET_MAPPINGS;

    static {
        ImmutableMap.Builder<@NotNull String, @NotNull PacketType<? extends @NotNull Packet<?>>> builder = ImmutableMap.builder();
        builder.put("ClientIntentionPacket", HandshakePacketTypes.CLIENT_INTENTION);
        builder.put("ServerboundMovePlayerPacket.Pos", GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS);
        builder.put("ServerboundMovePlayerPacket.PosRot", GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT);
        builder.put("ServerboundMovePlayerPacket.Rot", GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT);
        builder.put("ServerboundAcceptCodeOfConductPacket", ConfigurationPacketTypes.SERVERBOUND_ACCEPT_CODE_OF_CONDUCT);
        builder.put("ServerboundAcceptTeleportationPacket", GamePacketTypes.SERVERBOUND_ACCEPT_TELEPORTATION);
        builder.put("ServerboundAttackPacket", GamePacketTypes.SERVERBOUND_ATTACK);
        builder.put("ServerboundBlockEntityTagQueryPacket", GamePacketTypes.SERVERBOUND_BLOCK_ENTITY_TAG_QUERY);
        builder.put("ServerboundChangeDifficultyPacket", GamePacketTypes.SERVERBOUND_CHANGE_DIFFICULTY);
        builder.put("ServerboundChangeGameModePacket", GamePacketTypes.SERVERBOUND_CHANGE_GAME_MODE);
        builder.put("ServerboundChatAckPacket", GamePacketTypes.SERVERBOUND_CHAT_ACK);
        builder.put("ServerboundChatCommandPacket", GamePacketTypes.SERVERBOUND_CHAT_COMMAND);
        builder.put("ServerboundChatCommandSignedPacket", GamePacketTypes.SERVERBOUND_CHAT_COMMAND_SIGNED);
        builder.put("ServerboundChatPacket", GamePacketTypes.SERVERBOUND_CHAT);
        builder.put("ServerboundChatSessionUpdatePacket", GamePacketTypes.SERVERBOUND_CHAT_SESSION_UPDATE);
        builder.put("ServerboundChunkBatchReceivedPacket", GamePacketTypes.SERVERBOUND_CHUNK_BATCH_RECEIVED);
        builder.put("ServerboundClientCommandPacket", GamePacketTypes.SERVERBOUND_CLIENT_COMMAND);
        builder.put("ServerboundClientInformationPacket", CommonPacketTypes.SERVERBOUND_CLIENT_INFORMATION);
        builder.put("ServerboundClientTickEndPacket", GamePacketTypes.SERVERBOUND_CLIENT_TICK_END);
        builder.put("ServerboundCommandSuggestionPacket", GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION);
        builder.put("ServerboundConfigurationAcknowledgedPacket", GamePacketTypes.SERVERBOUND_CONFIGURATION_ACKNOWLEDGED);
        builder.put("ServerboundContainerButtonClickPacket", GamePacketTypes.SERVERBOUND_CONTAINER_BUTTON_CLICK);
        builder.put("ServerboundContainerClickPacket", GamePacketTypes.SERVERBOUND_CONTAINER_CLICK);
        builder.put("ServerboundContainerClosePacket", GamePacketTypes.SERVERBOUND_CONTAINER_CLOSE);
        builder.put("ServerboundContainerSlotStateChangedPacket", GamePacketTypes.SERVERBOUND_CONTAINER_SLOT_STATE_CHANGED);
        builder.put("ServerboundCookieResponsePacket", CookiePacketTypes.SERVERBOUND_COOKIE_RESPONSE);
        builder.put("ServerboundCustomClickActionPacket", CommonPacketTypes.SERVERBOUND_CUSTOM_CLICK_ACTION);
        builder.put("ServerboundCustomPayloadPacket", CommonPacketTypes.SERVERBOUND_CUSTOM_PAYLOAD);
        builder.put("ServerboundCustomQueryAnswerPacket", LoginPacketTypes.SERVERBOUND_CUSTOM_QUERY_ANSWER);
        builder.put("ServerboundDebugSubscriptionRequestPacket", GamePacketTypes.SERVERBOUND_DEBUG_SUBSCRIPTION_REQUEST);
        builder.put("ServerboundEditBookPacket", GamePacketTypes.SERVERBOUND_EDIT_BOOK);
        builder.put("ServerboundEntityTagQueryPacket", GamePacketTypes.SERVERBOUND_ENTITY_TAG_QUERY);
        builder.put("ServerboundFinishConfigurationPacket", ConfigurationPacketTypes.SERVERBOUND_FINISH_CONFIGURATION);
        builder.put("ServerboundHelloPacket", LoginPacketTypes.SERVERBOUND_HELLO);
        builder.put("ServerboundInteractPacket", GamePacketTypes.SERVERBOUND_INTERACT);
        builder.put("ServerboundJigsawGeneratePacket", GamePacketTypes.SERVERBOUND_JIGSAW_GENERATE);
        builder.put("ServerboundKeepAlivePacket", CommonPacketTypes.SERVERBOUND_KEEP_ALIVE);
        builder.put("ServerboundKeyPacket", LoginPacketTypes.SERVERBOUND_KEY);
        builder.put("ServerboundLockDifficultyPacket", GamePacketTypes.SERVERBOUND_LOCK_DIFFICULTY);
        builder.put("ServerboundLoginAcknowledgedPacket", LoginPacketTypes.SERVERBOUND_LOGIN_ACKNOWLEDGED);
        builder.put("ServerboundMoveVehiclePacket", GamePacketTypes.SERVERBOUND_MOVE_VEHICLE);
        builder.put("ServerboundPaddleBoatPacket", GamePacketTypes.SERVERBOUND_PADDLE_BOAT);
        builder.put("ServerboundPickItemFromBlockPacket", GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_BLOCK);
        builder.put("ServerboundPickItemFromEntityPacket", GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_ENTITY);
        builder.put("ServerboundPingRequestPacket", PingPacketTypes.SERVERBOUND_PING_REQUEST);
        builder.put("ServerboundPlaceRecipePacket", GamePacketTypes.SERVERBOUND_PLACE_RECIPE);
        builder.put("ServerboundPlayerAbilitiesPacket", GamePacketTypes.SERVERBOUND_PLAYER_ABILITIES);
        builder.put("ServerboundPlayerActionPacket", GamePacketTypes.SERVERBOUND_PLAYER_ACTION);
        builder.put("ServerboundPlayerCommandPacket", GamePacketTypes.SERVERBOUND_PLAYER_COMMAND);
        builder.put("ServerboundPlayerInputPacket", GamePacketTypes.SERVERBOUND_PLAYER_INPUT);
        builder.put("ServerboundPlayerLoadedPacket", GamePacketTypes.SERVERBOUND_PLAYER_LOADED);
        builder.put("ServerboundPongPacket", CommonPacketTypes.SERVERBOUND_PONG);
        builder.put("ServerboundRecipeBookChangeSettingsPacket", GamePacketTypes.SERVERBOUND_RECIPE_BOOK_CHANGE_SETTINGS);
        builder.put("ServerboundRecipeBookSeenRecipePacket", GamePacketTypes.SERVERBOUND_RECIPE_BOOK_SEEN_RECIPE);
        builder.put("ServerboundRenameItemPacket", GamePacketTypes.SERVERBOUND_RENAME_ITEM);
        builder.put("ServerboundResourcePackPacket", CommonPacketTypes.SERVERBOUND_RESOURCE_PACK);
        builder.put("ServerboundSeenAdvancementsPacket", GamePacketTypes.SERVERBOUND_SEEN_ADVANCEMENTS);
        builder.put("ServerboundSelectBundleItemPacket", GamePacketTypes.SERVERBOUND_BUNDLE_ITEM_SELECTED);
        builder.put("ServerboundSelectKnownPacks", ConfigurationPacketTypes.SERVERBOUND_SELECT_KNOWN_PACKS);
        builder.put("ServerboundSelectTradePacket", GamePacketTypes.SERVERBOUND_SELECT_TRADE);
        builder.put("ServerboundSetBeaconPacket", GamePacketTypes.SERVERBOUND_SET_BEACON);
        builder.put("ServerboundSetCarriedItemPacket", GamePacketTypes.SERVERBOUND_SET_CARRIED_ITEM);
        builder.put("ServerboundSetCommandBlockPacket", GamePacketTypes.SERVERBOUND_SET_COMMAND_BLOCK);
        builder.put("ServerboundSetCommandMinecartPacket", GamePacketTypes.SERVERBOUND_SET_COMMAND_MINECART);
        builder.put("ServerboundSetCreativeModeSlotPacket", GamePacketTypes.SERVERBOUND_SET_CREATIVE_MODE_SLOT);
        builder.put("ServerboundSetGameRulePacket", GamePacketTypes.SERVERBOUND_SET_GAME_RULE);
        builder.put("ServerboundSetJigsawBlockPacket", GamePacketTypes.SERVERBOUND_SET_JIGSAW_BLOCK);
        builder.put("ServerboundSetStructureBlockPacket", GamePacketTypes.SERVERBOUND_SET_STRUCTURE_BLOCK);
        builder.put("ServerboundSetTestBlockPacket", GamePacketTypes.SERVERBOUND_SET_TEST_BLOCK);
        builder.put("ServerboundSignUpdatePacket", GamePacketTypes.SERVERBOUND_SIGN_UPDATE);
        builder.put("ServerboundSpectateEntityPacket", GamePacketTypes.SERVERBOUND_SPECTATE_ENTITY);
        builder.put("ServerboundStatusRequestPacket", StatusPacketTypes.SERVERBOUND_STATUS_REQUEST);
        builder.put("ServerboundSwingPacket", GamePacketTypes.SERVERBOUND_SWING);
        builder.put("ServerboundTeleportToEntityPacket", GamePacketTypes.SERVERBOUND_TELEPORT_TO_ENTITY);
        builder.put("ServerboundTestInstanceBlockActionPacket", GamePacketTypes.SERVERBOUND_TEST_INSTANCE_BLOCK_ACTION);
        builder.put("ServerboundUseItemOnPacket", GamePacketTypes.SERVERBOUND_USE_ITEM_ON);
        builder.put("ServerboundUseItemPacket", GamePacketTypes.SERVERBOUND_USE_ITEM);
        builder.put("ServerboundMovePlayerPacket.StatusOnly", GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY);
        builder.put("ClientboundAddEntityPacket", GamePacketTypes.CLIENTBOUND_ADD_ENTITY);
        builder.put("ClientboundAnimatePacket", GamePacketTypes.CLIENTBOUND_ANIMATE);
        builder.put("ClientboundAwardStatsPacket", GamePacketTypes.CLIENTBOUND_AWARD_STATS);
        builder.put("ClientboundBlockChangedAckPacket", GamePacketTypes.CLIENTBOUND_BLOCK_CHANGED_ACK);
        builder.put("ClientboundBlockDestructionPacket", GamePacketTypes.CLIENTBOUND_BLOCK_DESTRUCTION);
        builder.put("ClientboundBlockEntityDataPacket", GamePacketTypes.CLIENTBOUND_BLOCK_ENTITY_DATA);
        builder.put("ClientboundBlockEventPacket", GamePacketTypes.CLIENTBOUND_BLOCK_EVENT);
        builder.put("ClientboundBlockUpdatePacket", GamePacketTypes.CLIENTBOUND_BLOCK_UPDATE);
        builder.put("ClientboundBossEventPacket", GamePacketTypes.CLIENTBOUND_BOSS_EVENT);
        builder.put("ClientboundBundleDelimiterPacket", GamePacketTypes.CLIENTBOUND_BUNDLE_DELIMITER);
        builder.put("ClientboundBundlePacket", GamePacketTypes.CLIENTBOUND_BUNDLE);
        builder.put("ClientboundChangeDifficultyPacket", GamePacketTypes.CLIENTBOUND_CHANGE_DIFFICULTY);
        builder.put("ClientboundChunkBatchFinishedPacket", GamePacketTypes.CLIENTBOUND_CHUNK_BATCH_FINISHED);
        builder.put("ClientboundChunkBatchStartPacket", GamePacketTypes.CLIENTBOUND_CHUNK_BATCH_START);
        builder.put("ClientboundChunksBiomesPacket", GamePacketTypes.CLIENTBOUND_CHUNKS_BIOMES);
        builder.put("ClientboundClearDialogPacket", CommonPacketTypes.CLIENTBOUND_CLEAR_DIALOG);
        builder.put("ClientboundClearTitlesPacket", GamePacketTypes.CLIENTBOUND_CLEAR_TITLES);
        builder.put("ClientboundCodeOfConductPacket", ConfigurationPacketTypes.CLIENTBOUND_CODE_OF_CONDUCT);
        builder.put("ClientboundCommandSuggestionsPacket", GamePacketTypes.CLIENTBOUND_COMMAND_SUGGESTIONS);
        builder.put("ClientboundCommandsPacket", GamePacketTypes.CLIENTBOUND_COMMANDS);
        builder.put("ClientboundContainerClosePacket", GamePacketTypes.CLIENTBOUND_CONTAINER_CLOSE);
        builder.put("ClientboundContainerSetContentPacket", GamePacketTypes.CLIENTBOUND_CONTAINER_SET_CONTENT);
        builder.put("ClientboundContainerSetDataPacket", GamePacketTypes.CLIENTBOUND_CONTAINER_SET_DATA);
        builder.put("ClientboundContainerSetSlotPacket", GamePacketTypes.CLIENTBOUND_CONTAINER_SET_SLOT);
        builder.put("ClientboundCookieRequestPacket", CookiePacketTypes.CLIENTBOUND_COOKIE_REQUEST);
        builder.put("ClientboundCooldownPacket", GamePacketTypes.CLIENTBOUND_COOLDOWN);
        builder.put("ClientboundCustomChatCompletionsPacket", GamePacketTypes.CLIENTBOUND_CUSTOM_CHAT_COMPLETIONS);
        builder.put("ClientboundCustomPayloadPacket", CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD);
        builder.put("ClientboundCustomQueryPacket", LoginPacketTypes.CLIENTBOUND_CUSTOM_QUERY);
        builder.put("ClientboundCustomReportDetailsPacket", CommonPacketTypes.CLIENTBOUND_CUSTOM_REPORT_DETAILS);
        builder.put("ClientboundDamageEventPacket", GamePacketTypes.CLIENTBOUND_DAMAGE_EVENT);
        builder.put("ClientboundDebugBlockValuePacket", GamePacketTypes.CLIENTBOUND_DEBUG_BLOCK_VALUE);
        builder.put("ClientboundDebugChunkValuePacket", GamePacketTypes.CLIENTBOUND_DEBUG_CHUNK_VALUE);
        builder.put("ClientboundDebugEntityValuePacket", GamePacketTypes.CLIENTBOUND_DEBUG_ENTITY_VALUE);
        builder.put("ClientboundDebugEventPacket", GamePacketTypes.CLIENTBOUND_DEBUG_EVENT);
        builder.put("ClientboundDebugSamplePacket", GamePacketTypes.CLIENTBOUND_DEBUG_SAMPLE);
        builder.put("ClientboundDeleteChatPacket", GamePacketTypes.CLIENTBOUND_DELETE_CHAT);
        builder.put("ClientboundDisconnectPacket", CommonPacketTypes.CLIENTBOUND_DISCONNECT);
        builder.put("ClientboundDisguisedChatPacket", GamePacketTypes.CLIENTBOUND_DISGUISED_CHAT);
        builder.put("ClientboundEntityEventPacket", GamePacketTypes.CLIENTBOUND_ENTITY_EVENT);
        builder.put("ClientboundEntityPositionSyncPacket", GamePacketTypes.CLIENTBOUND_ENTITY_POSITION_SYNC);
        builder.put("ClientboundExplodePacket", GamePacketTypes.CLIENTBOUND_EXPLODE);
        builder.put("ClientboundFinishConfigurationPacket", ConfigurationPacketTypes.CLIENTBOUND_FINISH_CONFIGURATION);
        builder.put("ClientboundForgetLevelChunkPacket", GamePacketTypes.CLIENTBOUND_FORGET_LEVEL_CHUNK);
        builder.put("ClientboundGameEventPacket", GamePacketTypes.CLIENTBOUND_GAME_EVENT);
        builder.put("ClientboundGameRuleValuesPacket", GamePacketTypes.CLIENTBOUND_GAME_RULE_VALUES);
        builder.put("ClientboundGameTestHighlightPosPacket", GamePacketTypes.CLIENTBOUND_GAME_TEST_HIGHLIGHT_POS);
        builder.put("ClientboundHelloPacket", LoginPacketTypes.CLIENTBOUND_HELLO);
        builder.put("ClientboundHurtAnimationPacket", GamePacketTypes.CLIENTBOUND_HURT_ANIMATION);
        builder.put("ClientboundInitializeBorderPacket", GamePacketTypes.CLIENTBOUND_INITIALIZE_BORDER);
        builder.put("ClientboundKeepAlivePacket", CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE);
        builder.put("ClientboundLevelChunkWithLightPacket", GamePacketTypes.CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT);
        builder.put("ClientboundLevelEventPacket", GamePacketTypes.CLIENTBOUND_LEVEL_EVENT);
        builder.put("ClientboundLevelParticlesPacket", GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES);
        builder.put("ClientboundLightUpdatePacket", GamePacketTypes.CLIENTBOUND_LIGHT_UPDATE);
        builder.put("ClientboundLoginCompressionPacket", LoginPacketTypes.CLIENTBOUND_LOGIN_COMPRESSION);
        builder.put("ClientboundLoginDisconnectPacket", LoginPacketTypes.CLIENTBOUND_LOGIN_DISCONNECT);
        builder.put("ClientboundLoginFinishedPacket", LoginPacketTypes.CLIENTBOUND_LOGIN_FINISHED);
        builder.put("ClientboundLoginPacket", GamePacketTypes.CLIENTBOUND_LOGIN);
        builder.put("ClientboundLowDiskSpaceWarningPacket", GamePacketTypes.CLIENTBOUND_LOW_DISK_SPACE_WARNING);
        builder.put("ClientboundMapItemDataPacket", GamePacketTypes.CLIENTBOUND_MAP_ITEM_DATA);
        builder.put("ClientboundMerchantOffersPacket", GamePacketTypes.CLIENTBOUND_MERCHANT_OFFERS);
        builder.put("ClientboundMountScreenOpenPacket", GamePacketTypes.CLIENTBOUND_MOUNT_SCREEN_OPEN);
        builder.put("ClientboundMoveMinecartPacket", GamePacketTypes.CLIENTBOUND_MOVE_MINECART_ALONG_TRACK);
        builder.put("ClientboundMoveVehiclePacket", GamePacketTypes.CLIENTBOUND_MOVE_VEHICLE);
        builder.put("ClientboundOpenBookPacket", GamePacketTypes.CLIENTBOUND_OPEN_BOOK);
        builder.put("ClientboundOpenScreenPacket", GamePacketTypes.CLIENTBOUND_OPEN_SCREEN);
        builder.put("ClientboundOpenSignEditorPacket", GamePacketTypes.CLIENTBOUND_OPEN_SIGN_EDITOR);
        builder.put("ClientboundPingPacket", CommonPacketTypes.CLIENTBOUND_PING);
        builder.put("ClientboundPlaceGhostRecipePacket", GamePacketTypes.CLIENTBOUND_PLACE_GHOST_RECIPE);
        builder.put("ClientboundPlayerAbilitiesPacket", GamePacketTypes.CLIENTBOUND_PLAYER_ABILITIES);
        builder.put("ClientboundPlayerChatPacket", GamePacketTypes.CLIENTBOUND_PLAYER_CHAT);
        builder.put("ClientboundPlayerCombatEndPacket", GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_END);
        builder.put("ClientboundPlayerCombatEnterPacket", GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_ENTER);
        builder.put("ClientboundPlayerCombatKillPacket", GamePacketTypes.CLIENTBOUND_PLAYER_COMBAT_KILL);
        builder.put("ClientboundPlayerInfoRemovePacket", GamePacketTypes.CLIENTBOUND_PLAYER_INFO_REMOVE);
        builder.put("ClientboundPlayerInfoUpdatePacket", GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE);
        builder.put("ClientboundPlayerLookAtPacket", GamePacketTypes.CLIENTBOUND_PLAYER_LOOK_AT);
        builder.put("ClientboundPlayerPositionPacket", GamePacketTypes.CLIENTBOUND_PLAYER_POSITION);
        builder.put("ClientboundPlayerRotationPacket", GamePacketTypes.CLIENTBOUND_PLAYER_ROTATION);
        builder.put("ClientboundPongResponsePacket", PingPacketTypes.CLIENTBOUND_PONG_RESPONSE);
        builder.put("ClientboundProjectilePowerPacket", GamePacketTypes.CLIENTBOUND_PROJECTILE_POWER);
        builder.put("ClientboundRecipeBookAddPacket", GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_ADD);
        builder.put("ClientboundRecipeBookRemovePacket", GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_REMOVE);
        builder.put("ClientboundRecipeBookSettingsPacket", GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_SETTINGS);
        builder.put("ClientboundRegistryDataPacket", ConfigurationPacketTypes.CLIENTBOUND_REGISTRY_DATA);
        builder.put("ClientboundRemoveEntitiesPacket", GamePacketTypes.CLIENTBOUND_REMOVE_ENTITIES);
        builder.put("ClientboundRemoveMobEffectPacket", GamePacketTypes.CLIENTBOUND_REMOVE_MOB_EFFECT);
        builder.put("ClientboundResetChatPacket", ConfigurationPacketTypes.CLIENTBOUND_RESET_CHAT);
        builder.put("ClientboundResetScorePacket", GamePacketTypes.CLIENTBOUND_RESET_SCORE);
        builder.put("ClientboundResourcePackPopPacket", CommonPacketTypes.CLIENTBOUND_RESOURCE_PACK_POP);
        builder.put("ClientboundResourcePackPushPacket", CommonPacketTypes.CLIENTBOUND_RESOURCE_PACK_PUSH);
        builder.put("ClientboundRespawnPacket", GamePacketTypes.CLIENTBOUND_RESPAWN);
        builder.put("ClientboundRotateHeadPacket", GamePacketTypes.CLIENTBOUND_ROTATE_HEAD);
        builder.put("ClientboundSectionBlocksUpdatePacket", GamePacketTypes.CLIENTBOUND_SECTION_BLOCKS_UPDATE);
        builder.put("ClientboundSelectAdvancementsTabPacket", GamePacketTypes.CLIENTBOUND_SELECT_ADVANCEMENTS_TAB);
        builder.put("ClientboundSelectKnownPacks", ConfigurationPacketTypes.CLIENTBOUND_SELECT_KNOWN_PACKS);
        builder.put("ClientboundServerDataPacket", GamePacketTypes.CLIENTBOUND_SERVER_DATA);
        builder.put("ClientboundServerLinksPacket", CommonPacketTypes.CLIENTBOUND_SERVER_LINKS);
        builder.put("ClientboundSetActionBarTextPacket", GamePacketTypes.CLIENTBOUND_SET_ACTION_BAR_TEXT);
        builder.put("ClientboundSetBorderCenterPacket", GamePacketTypes.CLIENTBOUND_SET_BORDER_CENTER);
        builder.put("ClientboundSetBorderLerpSizePacket", GamePacketTypes.CLIENTBOUND_SET_BORDER_LERP_SIZE);
        builder.put("ClientboundSetBorderSizePacket", GamePacketTypes.CLIENTBOUND_SET_BORDER_SIZE);
        builder.put("ClientboundSetBorderWarningDelayPacket", GamePacketTypes.CLIENTBOUND_SET_BORDER_WARNING_DELAY);
        builder.put("ClientboundSetBorderWarningDistancePacket", GamePacketTypes.CLIENTBOUND_SET_BORDER_WARNING_DISTANCE);
        builder.put("ClientboundSetCameraPacket", GamePacketTypes.CLIENTBOUND_SET_CAMERA);
        builder.put("ClientboundSetChunkCacheCenterPacket", GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_CENTER);
        builder.put("ClientboundSetChunkCacheRadiusPacket", GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_RADIUS);
        builder.put("ClientboundSetCursorItemPacket", GamePacketTypes.CLIENTBOUND_SET_CURSOR_ITEM);
        builder.put("ClientboundSetDefaultSpawnPositionPacket", GamePacketTypes.CLIENTBOUND_SET_DEFAULT_SPAWN_POSITION);
        builder.put("ClientboundSetDisplayObjectivePacket", GamePacketTypes.CLIENTBOUND_SET_DISPLAY_OBJECTIVE);
        builder.put("ClientboundSetEntityDataPacket", GamePacketTypes.CLIENTBOUND_SET_ENTITY_DATA);
        builder.put("ClientboundSetEntityLinkPacket", GamePacketTypes.CLIENTBOUND_SET_ENTITY_LINK);
        builder.put("ClientboundSetEntityMotionPacket", GamePacketTypes.CLIENTBOUND_SET_ENTITY_MOTION);
        builder.put("ClientboundSetEquipmentPacket", GamePacketTypes.CLIENTBOUND_SET_EQUIPMENT);
        builder.put("ClientboundSetExperiencePacket", GamePacketTypes.CLIENTBOUND_SET_EXPERIENCE);
        builder.put("ClientboundSetHealthPacket", GamePacketTypes.CLIENTBOUND_SET_HEALTH);
        builder.put("ClientboundSetHeldSlotPacket", GamePacketTypes.CLIENTBOUND_SET_HELD_SLOT);
        builder.put("ClientboundSetObjectivePacket", GamePacketTypes.CLIENTBOUND_SET_OBJECTIVE);
        builder.put("ClientboundSetPassengersPacket", GamePacketTypes.CLIENTBOUND_SET_PASSENGERS);
        builder.put("ClientboundSetPlayerInventoryPacket", GamePacketTypes.CLIENTBOUND_SET_PLAYER_INVENTORY);
        builder.put("ClientboundSetPlayerTeamPacket", GamePacketTypes.CLIENTBOUND_SET_PLAYER_TEAM);
        builder.put("ClientboundSetScorePacket", GamePacketTypes.CLIENTBOUND_SET_SCORE);
        builder.put("ClientboundSetSimulationDistancePacket", GamePacketTypes.CLIENTBOUND_SET_SIMULATION_DISTANCE);
        builder.put("ClientboundSetSubtitleTextPacket", GamePacketTypes.CLIENTBOUND_SET_SUBTITLE_TEXT);
        builder.put("ClientboundSetTimePacket", GamePacketTypes.CLIENTBOUND_SET_TIME);
        builder.put("ClientboundSetTitleTextPacket", GamePacketTypes.CLIENTBOUND_SET_TITLE_TEXT);
        builder.put("ClientboundSetTitlesAnimationPacket", GamePacketTypes.CLIENTBOUND_SET_TITLES_ANIMATION);
        builder.put("ClientboundShowDialogPacket", CommonPacketTypes.CLIENTBOUND_SHOW_DIALOG);
        builder.put("ClientboundSoundEntityPacket", GamePacketTypes.CLIENTBOUND_SOUND_ENTITY);
        builder.put("ClientboundSoundPacket", GamePacketTypes.CLIENTBOUND_SOUND);
        builder.put("ClientboundStartConfigurationPacket", GamePacketTypes.CLIENTBOUND_START_CONFIGURATION);
        builder.put("ClientboundStatusResponsePacket", StatusPacketTypes.CLIENTBOUND_STATUS_RESPONSE);
        builder.put("ClientboundStopSoundPacket", GamePacketTypes.CLIENTBOUND_STOP_SOUND);
        builder.put("ClientboundStoreCookiePacket", CommonPacketTypes.CLIENTBOUND_STORE_COOKIE);
        builder.put("ClientboundSystemChatPacket", GamePacketTypes.CLIENTBOUND_SYSTEM_CHAT);
        builder.put("ClientboundTabListPacket", GamePacketTypes.CLIENTBOUND_TAB_LIST);
        builder.put("ClientboundTagQueryPacket", GamePacketTypes.CLIENTBOUND_TAG_QUERY);
        builder.put("ClientboundTakeItemEntityPacket", GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY);
        builder.put("ClientboundTeleportEntityPacket", GamePacketTypes.CLIENTBOUND_TELEPORT_ENTITY);
        builder.put("ClientboundTestInstanceBlockStatus", GamePacketTypes.CLIENTBOUND_TEST_INSTANCE_BLOCK_STATUS);
        builder.put("ClientboundTickingStatePacket", GamePacketTypes.CLIENTBOUND_TICKING_STATE);
        builder.put("ClientboundTickingStepPacket", GamePacketTypes.CLIENTBOUND_TICKING_STEP);
        builder.put("ClientboundTrackedWaypointPacket", GamePacketTypes.CLIENTBOUND_WAYPOINT);
        builder.put("ClientboundTransferPacket", CommonPacketTypes.CLIENTBOUND_TRANSFER);
        builder.put("ClientboundUpdateAdvancementsPacket", GamePacketTypes.CLIENTBOUND_UPDATE_ADVANCEMENTS);
        builder.put("ClientboundUpdateAttributesPacket", GamePacketTypes.CLIENTBOUND_UPDATE_ATTRIBUTES);
        builder.put("ClientboundUpdateEnabledFeaturesPacket", ConfigurationPacketTypes.CLIENTBOUND_UPDATE_ENABLED_FEATURES);
        builder.put("ClientboundUpdateMobEffectPacket", GamePacketTypes.CLIENTBOUND_UPDATE_MOB_EFFECT);
        builder.put("ClientboundUpdateRecipesPacket", GamePacketTypes.CLIENTBOUND_UPDATE_RECIPES);
        builder.put("ClientboundUpdateTagsPacket", CommonPacketTypes.CLIENTBOUND_UPDATE_TAGS);
        builder.put("ClientboundMoveEntityPacket.Pos", GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS);
        builder.put("ClientboundMoveEntityPacket.PosRot", GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS_ROT);
        builder.put("ClientboundMoveEntityPacket.Rot", GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT);
        LEGACY_PACKET_MAPPINGS = builder.buildOrThrow();
    }

    private PacketUtils() {}
}
