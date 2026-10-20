/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mc;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Bridges the API differences between the supported Minecraft versions.
 *
 * <p>This class is version specific, but every method has the same signature and the same
 * behaviour in all of them, so the shared code in {@code src/main/java} can use it without
 * knowing which Minecraft version it is running on. See COMPAT.md.
 */
public final class Vanilla {
    private Vanilla() {}

    // Player actions

    public static void swing(Player player, InteractionHand hand) {
        player.swing(hand, SwingAnimation.DEFAULT, false);
    }

    // Blocks

    public static BlockPos copy(BlockPos pos) {
        return pos.immutable();
    }

    public static boolean bedDestroysOnUse(BedRule rule) {
        return rule.destroyOnUse();
    }


    // Collision

    public static boolean blocksMotion(net.minecraft.world.level.block.state.BlockState state) {
        return state.isSolid();
    }

    public static java.util.Collection<net.minecraft.world.phys.EntityHitResult> getManyEntityHitResult(
        net.minecraft.world.level.Level level,
        net.minecraft.world.entity.Entity entity,
        net.minecraft.world.phys.Vec3 from,
        net.minecraft.world.phys.Vec3 to,
        net.minecraft.world.phys.AABB box,
        java.util.function.Predicate<net.minecraft.world.entity.Entity> predicate,
        float tolerance,
        net.minecraft.world.level.ClipContext.Block block,
        boolean onlyBlocks,
        boolean extra
    ) {
        return net.minecraft.world.entity.projectile.ProjectileUtil.getManyEntityHitResult(level, entity, from, to, box, predicate, tolerance, block, onlyBlocks, extra);
    }

    public static java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> quads(
        net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState layer
    ) {
        return ((net.minecraft.client.resources.model.geometry.ItemQuads) ((meteordevelopment.meteorclient.mixin.LayerRenderStateAccessor) layer).meteor$getQuads()).all();
    }

    // Registries

    public static HolderLookup.Provider worldLookup() {
        return VanillaRegistries.createWorldLookup();
    }

    /**
     * A swing the server is told about.
     *
     * <p>26.2 could send a swing packet; 26.3 removed the packet, so the closest equivalent
     * is the local swing.
     */
    public static void swingPacket(net.minecraft.client.player.LocalPlayer player, net.minecraft.world.InteractionHand hand) {
        player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
    }

    public static void dropMainHand(net.minecraft.client.player.LocalPlayer player) {
        player.drop(player.getMainHandItem(), true, net.minecraft.util.Prediction.PREDICTED);
    }

    public static void registerSwingPacket(com.google.common.collect.ImmutableMap.Builder<String, net.minecraft.network.protocol.PacketType<? extends net.minecraft.network.protocol.Packet<?>>> builder) {
        // 26.3 removed the swing packet: the client can no longer tell the server about a swing.
    }

    // Items

    public static boolean isFuel(Item item) {
        return item.components().get(net.minecraft.core.component.DataComponents.COOKING_FUEL) != null;
    }

    // Input

    public static final InputConstants.Type KEY_TYPE = InputConstants.Type.KEYBOARD;

    public static final int SUPER_KEY_LEFT = InputConstants.KEY_LGUI;

    public static final int SUPER_KEY_RIGHT = InputConstants.KEY_RGUI;

    public static int scancode(net.minecraft.client.input.KeyEvent event) {
        return event.keycode();
    }

    public static void selectCursor(com.mojang.blaze3d.platform.cursor.CursorType cursor) {
        cursor.select();
    }

    public static void releaseMouse(Window window, double x, double y) {
        InputConstants.releaseMouse(window, x, y);
    }


    // Transforms

    public static void rotate(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.math.Axis axis, float degrees) {
        poseStack.mulPose(axis.rotateDegrees(new org.joml.Matrix4f(), degrees));
    }

    // Entities

    public static boolean isEnderman(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Enderman;
    }

    public static boolean isCreepy(net.minecraft.world.entity.Entity entity) {
        return ((net.minecraft.world.entity.monster.Enderman) entity).isCreepy();
    }


    public static void tooltip(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                               net.minecraft.client.gui.Font font,
                               java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> components,
                               int x, int y, net.minecraft.world.item.ItemStack stack) {
        graphics.tooltip(font, components, x, y, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, stack.get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE), false);
    }


    public static boolean isSameFluid(net.minecraft.world.level.material.Fluid a, net.minecraft.world.level.material.Fluid b) {
        return a == b;
    }


    public static net.minecraft.client.multiplayer.ClientLevel createClientLevel(
        net.minecraft.client.multiplayer.ClientPacketListener listener,
        net.minecraft.client.multiplayer.ClientLevel.ClientLevelData data,
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
        net.minecraft.core.Holder<net.minecraft.world.level.dimension.DimensionType> dimensionType,
        int x, int z,
        net.minecraft.client.renderer.extract.LevelExtractor extractor,
        boolean isClientSide,
        int seaLevel
    ) {
        return new net.minecraft.client.multiplayer.ClientLevel(listener, data, dimension, dimensionType, x, z, extractor, isClientSide, seaLevel);
    }


    // Textures

    public static meteordevelopment.meteorclient.renderer.McTexture createTexture(java.util.function.Supplier<String> name, com.mojang.blaze3d.platform.NativeImage image) {
        return new meteordevelopment.meteorclient.renderer.McTexture(net.minecraft.client.renderer.texture.TextureResources.from2dImage(name, image));
    }

    public static void registerTexture(net.minecraft.resources.Identifier id, meteordevelopment.meteorclient.renderer.McTexture texture) {
        mc.getTextureManager().register(id, (net.minecraft.client.renderer.texture.TextureResources) texture.handle());
    }

    public static void renderTexture(meteordevelopment.meteorclient.renderer.Renderer2D renderer, meteordevelopment.meteorclient.renderer.McTexture texture) {
        net.minecraft.client.renderer.texture.TextureHandle t = (net.minecraft.client.renderer.texture.TextureHandle) texture.handle();
        renderer.render(t.textureView(), t.sampler());
    }


    /**
     * 26.4 removed {@code DyeColor#getTextureDiffuseColor}, so the dye colours are kept here.
     * The values are the ones vanilla used up to 26.3.
     */
    public static int dyeDiffuseColor(net.minecraft.world.item.DyeColor dye) {
        return switch (dye) {
            case WHITE -> 0xF9FFFE;
            case ORANGE -> 0xF9801D;
            case MAGENTA -> 0xC74EBD;
            case LIGHT_BLUE -> 0x3AB3DA;
            case YELLOW -> 0xFED83D;
            case LIME -> 0x80C71F;
            case PINK -> 0xF38BAA;
            case GRAY -> 0x474F52;
            case LIGHT_GRAY -> 0x9D9D97;
            case CYAN -> 0x169C9C;
            case PURPLE -> 0x8932B8;
            case BLUE -> 0x3C44AA;
            case BROWN -> 0x835432;
            case GREEN -> 0x5E7C16;
            case RED -> 0xB02E26;
            case BLACK -> 0x1D1D21;
        };
    }


    public static java.util.List<net.minecraft.world.item.ItemStack> containerItems(net.minecraft.core.component.DataComponentMap components) {
        return components.get(net.minecraft.core.component.DataComponents.CONTAINER).itemCopies().toList();
    }


    /**
     * 26.4: the map texture is a GpuTexture that cannot be read back, so the image is rebuilt
     * from the saved data exactly the way MapTextureManager does it.
     */
    public static com.mojang.blaze3d.platform.NativeImage mapImage(
        meteordevelopment.meteorclient.mixin.MapTextureManagerAccessor accessor,
        net.minecraft.world.level.saveddata.maps.MapId id,
        net.minecraft.world.level.saveddata.maps.MapItemSavedData state
    ) {
        com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(128, 128, false);
        byte[] colors = state.colors;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                image.setPixel(x, y, net.minecraft.world.level.material.MapColor.getColorFromPackedId(colors[x + y * 128]));
            }
        }
        return image;
    }

    public static void closeMapImage(com.mojang.blaze3d.platform.NativeImage image) {
        image.close();
    }


    // Keyboard

    public static final int MENU_KEY = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_MENU;

    public static final int WORLD_2_KEY = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_INTERNATIONAL2;

    public static boolean matchesKey(int input, int scancode, boolean key, com.mojang.blaze3d.platform.InputConstants.Key inputKey) {
        boolean isKey = inputKey.getType() != com.mojang.blaze3d.platform.InputConstants.Type.MOUSE;
        return isKey == key && input == inputKey.getValue();
    }

    public static boolean isKeyDown(int key) {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(key);
    }

    public static boolean isMouseButtonKey(int key) {
        return key >= 1 && key <= 8;
    }

    public static boolean isMouseButtonPressed(int button) {
        if (button < 1 || button > 8) return false;
        int state = org.lwjgl.sdl.SDLMouse.SDL_GetMouseState(org.lwjgl.BufferUtils.createFloatBuffer(1), org.lwjgl.BufferUtils.createFloatBuffer(1));
        return (state & (1 << (button - 1))) != 0;
    }


    public static net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo translucentMaterialInfo(
        net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo info
    ) {
        return new net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo(
            info.sprite(),
            net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT,
            info.itemRenderType(),
            info.itemGlintRenderType(),
            info.itemGlintSpecialRenderType(),
            info.tintIndex(),
            info.shadeDirectionOverride(),
            info.flags()
        );
    }


    public static <S> void submitBannerPatterns(
        net.minecraft.client.resources.model.sprite.SpriteGetter sprites,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.SubmitNodeCollector collector,
        int light, int overlay,
        net.minecraft.client.model.Model<S> model, S state,
        boolean renderOverlay,
        net.minecraft.world.item.DyeColor color,
        net.minecraft.world.level.block.entity.BannerPatternLayers patterns
    ) {
        net.minecraft.client.renderer.blockentity.BannerRenderer.submitPatterns(sprites, poseStack, collector, light, overlay, model, state, renderOverlay, color, patterns);
    }

    public static <S> void submitBannerModel(
        net.minecraft.client.renderer.SubmitNodeCollector collector,
        net.minecraft.client.model.Model<? super S> model, S state,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        int light, int overlay, int outlineColor,
        net.minecraft.client.resources.model.sprite.SpriteId sprite,
        net.minecraft.client.resources.model.sprite.SpriteGetter sprites,
        int color,
        Object crumblingOverlay
    ) {
        collector.submitModel(model, state, poseStack, light, overlay, outlineColor, sprite, sprites, color);
    }


    /**
     * 26.3 removed the swing packet, so a critical only holds back the attack.
     */
    public static boolean requiresSwingPacket() {
        return false;
    }

    public static boolean isSwingPacket(Object packet) {
        return false;
    }

    public static void sendSwingPacket(Object packet) {
    }


    // Authentication (authlib 10)

    public static Object createAuthService() {
        return com.mojang.authlib.services.MinecraftServicesDiscoveryService.create(mc.getProxy(), false);
    }

    public static void applyLoginEnvironment(Object authService) {
        com.mojang.authlib.services.MinecraftServicesDiscoveryService service =
            (com.mojang.authlib.services.MinecraftServicesDiscoveryService) authService;
        meteordevelopment.meteorclient.mixin.MinecraftAccessor mca = (meteordevelopment.meteorclient.mixin.MinecraftAccessor) mc;
        net.minecraft.util.SignatureValidator.from(service.getServicesKeySet(), com.mojang.authlib.services.ServicesKeyType.PROFILE_KEY);
        net.minecraft.client.resources.SkinManager.TextureCache skinCache =
            ((meteordevelopment.meteorclient.mixin.SkinManagerAccessor) mc.getSkinManager()).meteor$getSkinTextures();
        java.nio.file.Path skinCachePath =
            ((meteordevelopment.meteorclient.mixin.FileCacheAccessor) skinCache).meteor$getRoot();
        mca.meteor$setServices(net.minecraft.server.Services.create(service, mc.gameDirectory));
        mca.meteor$setSkinManager(new net.minecraft.client.resources.SkinManager(skinCachePath, mc.services(),
            new net.minecraft.client.renderer.texture.SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc), mc));
    }

    public static void setSession(net.minecraft.client.User session) {
        meteordevelopment.meteorclient.mixin.MinecraftAccessor mca = (meteordevelopment.meteorclient.mixin.MinecraftAccessor) mc;
        mca.meteor$setUser(session);

        com.mojang.authlib.services.MinecraftServicesDiscoveryService service =
            com.mojang.authlib.services.MinecraftServicesDiscoveryService.create(mc.getProxy(), false);

        com.mojang.authlib.minecraft.UserApiService apiService = service.createUserApiService(session.getAccessToken());
        com.mojang.authlib.services.FriendsService friendsService = service.createFriendsService(session.getAccessToken());
        net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler handler =
            new net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler(friendsService, mc);
        mca.meteor$setUserApiService(apiService);
        mca.meteor$setPlayerSocialManager(new net.minecraft.client.gui.screens.social.PlayerSocialManager(mc, apiService, friendsService, handler));
        mca.meteor$setProfileKeyPairManager(net.minecraft.client.multiplayer.ProfileKeyPairManager.create(apiService, session, mc.gameDirectory.toPath()));
        mca.meteor$setReportingContext(net.minecraft.client.multiplayer.chat.report.ReportingContext.create(net.minecraft.client.multiplayer.chat.report.ReportEnvironment.local(), apiService));
        mca.meteor$setProfileFuture(java.util.concurrent.CompletableFuture.supplyAsync(
            () -> mc.services().sessionService().fetchProfile(mc.getUser().getProfileId(), true), net.minecraft.util.Util.ioPool()));
    }

    public static Object createAlteningEnvironment() {
        return new com.mojang.authlib.Environment("https://authserver.thealtening.com", "The Altening");
    }

    public static Object createAlteningService(Object environment) {
        return com.mojang.authlib.services.MinecraftServicesDiscoveryService.create(mc.getProxy(), false, (com.mojang.authlib.Environment) environment);
    }

    public static String alteningAuthUrl(Object environment) {
        return ((com.mojang.authlib.Environment) environment).discoveryUrl() + "/authenticate";
    }

    // Packets

    public static ServerboundChatPacket chat(String message, Instant instant, long seen, MessageSignature signature, LastSeenMessages.Update update) {
        return new ServerboundChatPacket(message, instant, seen, Optional.ofNullable(signature), update);
    }

    public static ServerboundSignUpdatePacket signUpdate(BlockPos pos, String[] lines) {
        return new ServerboundSignUpdatePacket(pos, List.of(lines), SignTextSlot.FRONT);
    }

    public static String[] signLines(ServerboundSignUpdatePacket packet) {
        return packet.lines().toArray(String[]::new);
    }

    public static int chunkX(ClientboundLevelChunkWithLightPacket packet) {
        return packet.x();
    }

    public static int chunkZ(ClientboundLevelChunkWithLightPacket packet) {
        return packet.z();
    }

    public static IntList removeEntityIds(ClientboundRemoveEntitiesPacket packet) {
        return packet.entityIds();
    }

    public static Vec3 movingPosition(ServerboundMoveVehiclePacket packet) {
        return packet.movingTo().position();
    }

    public static BlockHitResult hitResult(ServerboundUseItemOnPacket packet) {
        return packet.hitResult();
    }

    // Misc

    public static void openUri(String url) {
        com.mojang.blaze3d.Blaze3D.openUri(java.net.URI.create(url));
    }
}
