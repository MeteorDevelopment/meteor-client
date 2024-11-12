/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import it.unimi.dsi.fastutil.objects.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.mixin.*;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.settings.StatusEffectAmplifierMapSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockEntityIterator;
import meteordevelopment.meteorclient.utils.world.ChunkIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.resource.ResourceReloadLogger;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Range;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.*;

public class Utils {
    public static final Pattern FILE_NAME_INVALID_CHARS_PATTERN = Pattern.compile("[\\s\\\\/:*?\"<>|]");
    public static final Color WHITE = new Color(255, 255, 255);

    private static final Random random = new Random();
    public static boolean firstTimeTitleScreen = true;
    public static boolean isReleasingTrident;
    public static boolean rendering3D = true;
    public static double frameTime;
    public static Screen screenToOpen;
    public static VertexSorter vertexSorter;

    private Utils() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(Utils.class);
    }

    @EventHandler
    private static void onTick(TickEvent.Post event) {
        if (screenToOpen != null && mc.currentScreen == null) {
            mc.setScreen(screenToOpen);
            screenToOpen = null;
        }
    }

    public static Vec3d getPlayerSpeed() {
        if (mc.player == null) return Vec3d.ZERO;

        double tX = mc.player.getX() - mc.player.prevX;
        double tY = mc.player.getY() - mc.player.prevY;
        double tZ = mc.player.getZ() - mc.player.prevZ;

        Timer timer = Modules.get().get(Timer.class);
        if (timer.isActive()) {
            tX *= timer.getMultiplier();
            tY *= timer.getMultiplier();
            tZ *= timer.getMultiplier();
        }

        tX *= 20;
        tY *= 20;
        tZ *= 20;

        return new Vec3d(tX, tY, tZ);
    }

    public static String getWorldTime() {
        if (mc.world == null) return "00:00";

        int ticks = (int) (mc.world.getTimeOfDay() % 24000);
        ticks += 6000;
        if (ticks > 24000) ticks -= 24000;

        return String.format("%02d:%02d", ticks / 1000, (int) (ticks % 1000 / 1000.0 * 60));
    }

    public static Iterable<Chunk> chunks(boolean onlyWithLoadedNeighbours) {
        return () -> new ChunkIterator(onlyWithLoadedNeighbours);
    }

    public static Iterable<Chunk> chunks() {
        return chunks(false);
    }

    public static Iterable<BlockEntity> blockEntities() {
        return BlockEntityIterator::new;
    }

    public static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
        enchantments.clear();

        if (!itemStack.isEmpty()) {
            Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
                ? itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantmentEntries()
                : itemStack.getEnchantments().getEnchantmentEntries();

            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantments) {
                enchantments.put(entry.getKey(), entry.getIntValue());
            }
        }
    }

    public static int getEnchantmentLevel(ItemStack itemStack, RegistryKey<Enchantment> enchantment) {
        if (itemStack.isEmpty()) return 0;
        Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);
        return getEnchantmentLevel(itemEnchantments, enchantment);
    }

    public static int getEnchantmentLevel(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantment) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : Object2IntMaps.fastIterable(itemEnchantments)) {
            if (entry.getKey().matchesKey(enchantment)) return entry.getIntValue();
        }
        return 0;
    }

    @SafeVarargs
    public static boolean hasEnchantments(ItemStack itemStack, RegistryKey<Enchantment>... enchantments) {
        if (itemStack.isEmpty()) return false;
        Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);

        for (RegistryKey<Enchantment> enchantment : enchantments) {
            if (!hasEnchantment(itemEnchantments, enchantment)) return false;
        }
        return true;
    }

    public static boolean hasEnchantment(ItemStack itemStack, RegistryKey<Enchantment> enchantmentKey) {
        if (itemStack.isEmpty()) return false;
        Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);
        return hasEnchantment(itemEnchantments, enchantmentKey);
    }

    private static boolean hasEnchantment(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantmentKey) {
        for (RegistryEntry<Enchantment> enchantment : itemEnchantments.keySet()) {
            if (enchantment.matchesKey(enchantmentKey)) return true;
        }
        return false;
    }

    public static int getRenderDistance() {
        return Math.max(mc.options.getViewDistance().getValue(), ((ClientPlayNetworkHandlerAccessor) mc.getNetworkHandler()).getChunkLoadDistance());
    }

    public static int getWindowWidth() {
        return mc.getWindow().getFramebufferWidth();
    }

    public static int getWindowHeight() {
        return mc.getWindow().getFramebufferHeight();
    }

    public static void unscaledProjection() {
        vertexSorter = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), 0, 1000, 21000), VertexSorter.BY_Z);
        rendering3D = false;
    }

    public static void scaledProjection() {
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, (float) (mc.getWindow().getFramebufferWidth() / mc.getWindow().getScaleFactor()), (float) (mc.getWindow().getFramebufferHeight() / mc.getWindow().getScaleFactor()), 0, 1000, 21000), vertexSorter);
        rendering3D = true;
    }

    public static Vec3d vec3d(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean openContainer(ItemStack itemStack, ItemStack[] contents, boolean pause) {
        if (hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
            Utils.getItemsInContainerItem(itemStack, contents);
            if (pause) screenToOpen = new PeekScreen(itemStack, contents);
            else mc.setScreen(new PeekScreen(itemStack, contents));
            return true;
        }

        return false;
    }

    public static void getItemsInContainerItem(ItemStack itemStack, ItemStack[] items) {
        if (itemStack.getItem() == Items.ENDER_CHEST) {
            for (int i = 0; i < EChestMemory.ITEMS.size(); i++) {
                items[i] = EChestMemory.ITEMS.get(i);
            }

            return;
        }

        Arrays.fill(items, ItemStack.EMPTY);
        ComponentMap components = itemStack.getComponents();

        if (components.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponentAccessor container = ((ContainerComponentAccessor) (Object) components.get(DataComponentTypes.CONTAINER));
            DefaultedList<ItemStack> stacks = container.getStacks();

            for (int i = 0; i < stacks.size(); i++) {
                if (i >= 0 && i < items.length) items[i] = stacks.get(i);
            }
        }
        else if (components.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            NbtComponent nbt2 = components.get(DataComponentTypes.BLOCK_ENTITY_DATA);

            if (nbt2.contains("Items")) {
                NbtList nbt3 = (NbtList) nbt2.getNbt().get("Items");

                for (int i = 0; i < nbt3.size(); i++) {
                    int slot = nbt3.getCompound(i).getByte("Slot"); // Apparently shulker boxes can store more than 27 items, good job Mojang
                    // now NPEs when mc.world == null
                    if (slot >= 0 && slot < items.length) items[slot] = ItemStack.fromNbtOrEmpty(mc.player.getRegistryManager(), nbt3.getCompound(i));
                }
            }
        }
    }

    public static Color getShulkerColor(ItemStack shulkerItem) {
        if (shulkerItem.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block == Blocks.ENDER_CHEST) return BetterTooltips.ECHEST_COLOR;
            if (block instanceof ShulkerBoxBlock shulkerBlock) {
                DyeColor dye = shulkerBlock.getColor();
                if (dye == null) return WHITE;
                final int color = dye.getEntityColor();
                return new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 1f);
            }
        }
        return WHITE;
    }

    public static boolean hasItems(ItemStack itemStack) {
        ContainerComponentAccessor container = ((ContainerComponentAccessor) (Object) itemStack.get(DataComponentTypes.CONTAINER));
        if (container != null && !container.getStacks().isEmpty()) return true;

        NbtCompound compoundTag = itemStack.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT).getNbt();
        return compoundTag != null && compoundTag.contains("Items", 9);
    }

    public static Reference2IntMap<StatusEffect> createStatusEffectMap() {
        return new Reference2IntArrayMap<>(StatusEffectAmplifierMapSetting.EMPTY_STATUS_EFFECT_MAP);
    }

    public static String getEnchantSimpleName(RegistryEntry<Enchantment> enchantment, int length) {
        String name = Names.get(enchantment);
        return name.length() > length ? name.substring(0, length) : name;
    }

    public static boolean searchTextDefault(String text, String filter, boolean caseSensitive) {
        return searchInWords(text, filter) > 0 || searchLevenshteinDefault(text, filter, caseSensitive) < text.length() / 2;
    }

    public static int searchLevenshteinDefault(String text, String filter, boolean caseSensitive) {
        return levenshteinDistance(caseSensitive ? filter : filter.toLowerCase(Locale.ROOT), caseSensitive ? text : text.toLowerCase(Locale.ROOT), 1, 8, 8);
    }

    public static int searchInWords(String text, String filter) {
        if (filter.isEmpty()) return 1;

        int wordsFound = 0;
        text = text.toLowerCase(Locale.ROOT);
        String[] words = filter.toLowerCase(Locale.ROOT).split(" ");

        for (String word : words) {
            if (!text.contains(word)) return 0;
            wordsFound += StringUtils.countMatches(text, word);
        }

        return wordsFound;
    }

    public static int levenshteinDistance(String from, String to, int insCost, int subCost, int delCost) {
        int textLength = from.length();
        int filterLength = to.length();

        if (textLength == 0) return filterLength * insCost;
        if (filterLength == 0) return textLength * delCost;

        // Populate matrix
        int[][] d = new int[textLength + 1][filterLength + 1];

        for (int i = 0; i <= textLength; i++) {
            d[i][0] = i * delCost;
        }

        for (int j = 0; j <= filterLength; j++) {
            d[0][j] = j * insCost;
        }

        // Find best route
        for (int i = 1; i <= textLength; i++) {
            for (int j = 1; j <= filterLength; j++) {
                int sCost = d[i - 1][j - 1] + (from.charAt(i - 1) == to.charAt(j - 1) ? 0 : subCost);
                int dCost = d[i - 1][j] + delCost;
                int iCost = d[i][j - 1] + insCost;
                d[i][j] = Math.min(Math.min(dCost, iCost), sCost);
            }
        }

        return d[textLength][filterLength];
    }

    public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;
        return dX * dX + dY * dY + dZ * dZ;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static String getFileWorldName() {
        return FILE_NAME_INVALID_CHARS_PATTERN.matcher(getWorldName()).replaceAll("_");
    }

    public static String getWorldName() {
        // Singleplayer
        if (mc.isInSingleplayer()) {
            if (mc.world == null) return "";

            File folder = ((MinecraftServerAccessor) mc.getServer()).getSession().getWorldDirectory(mc.world.getRegistryKey()).toFile();
            if (folder.toPath().relativize(mc.runDirectory.toPath()).getNameCount() != 2) {
                folder = folder.getParentFile();
            }
            return folder.getName();
        }

        // Multiplayer
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().isRealm() ? "realms" : mc.getCurrentServerEntry().address;
        }

        return "";
    }

    public static String nameToTitle(String name) {
        return Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    public static String titleToName(String title) {
        return title.replace(" ", "-").toLowerCase(Locale.ROOT);
    }

    public static String getKeyName(int key) {
        return switch (key) {
            case GLFW_KEY_UNKNOWN -> "Unknown";
            case GLFW_KEY_ESCAPE -> "Esc";
            case GLFW_KEY_GRAVE_ACCENT -> "Grave Accent";
            case GLFW_KEY_WORLD_1 -> "World 1";
            case GLFW_KEY_WORLD_2 -> "World 2";
            case GLFW_KEY_PRINT_SCREEN -> "Print Screen";
            case GLFW_KEY_PAUSE -> "Pause";
            case GLFW_KEY_INSERT -> "Insert";
            case GLFW_KEY_DELETE -> "Delete";
            case GLFW_KEY_HOME -> "Home";
            case GLFW_KEY_PAGE_UP -> "Page Up";
            case GLFW_KEY_PAGE_DOWN -> "Page Down";
            case GLFW_KEY_END -> "End";
            case GLFW_KEY_TAB -> "Tab";
            case GLFW_KEY_LEFT_CONTROL -> "Left Control";
            case GLFW_KEY_RIGHT_CONTROL -> "Right Control";
            case GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW_KEY_UP -> "Arrow Up";
            case GLFW_KEY_DOWN -> "Arrow Down";
            case GLFW_KEY_LEFT -> "Arrow Left";
            case GLFW_KEY_RIGHT -> "Arrow Right";
            case GLFW_KEY_APOSTROPHE -> "Apostrophe";
            case GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW_KEY_MENU -> "Menu";
            case GLFW_KEY_LEFT_SUPER -> "Left Super";
            case GLFW_KEY_RIGHT_SUPER -> "Right Super";
            case GLFW_KEY_ENTER -> "Enter";
            case GLFW_KEY_KP_ENTER -> "Numpad Enter";
            case GLFW_KEY_NUM_LOCK -> "Num Lock";
            case GLFW_KEY_SPACE -> "Space";
            case GLFW_KEY_F1 -> "F1";
            case GLFW_KEY_F2 -> "F2";
            case GLFW_KEY_F3 -> "F3";
            case GLFW_KEY_F4 -> "F4";
            case GLFW_KEY_F5 -> "F5";
            case GLFW_KEY_F6 -> "F6";
            case GLFW_KEY_F7 -> "F7";
            case GLFW_KEY_F8 -> "F8";
            case GLFW_KEY_F9 -> "F9";
            case GLFW_KEY_F10 -> "F10";
            case GLFW_KEY_F11 -> "F11";
            case GLFW_KEY_F12 -> "F12";
            case GLFW_KEY_F13 -> "F13";
            case GLFW_KEY_F14 -> "F14";
            case GLFW_KEY_F15 -> "F15";
            case GLFW_KEY_F16 -> "F16";
            case GLFW_KEY_F17 -> "F17";
            case GLFW_KEY_F18 -> "F18";
            case GLFW_KEY_F19 -> "F19";
            case GLFW_KEY_F20 -> "F20";
            case GLFW_KEY_F21 -> "F21";
            case GLFW_KEY_F22 -> "F22";
            case GLFW_KEY_F23 -> "F23";
            case GLFW_KEY_F24 -> "F24";
            case GLFW_KEY_F25 -> "F25";
            default -> {
                String keyName = glfwGetKeyName(key, 0);
                yield keyName == null ? "Unknown" : StringUtils.capitalize(keyName);
            }
        };
    }

    public static String getButtonName(int button) {
        return switch (button) {
            case -1 -> "Unknown";
            case 0 -> "Mouse Left";
            case 1 -> "Mouse Right";
            case 2 -> "Mouse Middle";
            default -> "Mouse " + button;
        };
    }

    public static byte[] readBytes(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            MeteorClient.LOG.error("Error reading from stream.", e);
            return new byte[0];
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static boolean canUpdate() {
        return mc != null && mc.world != null && mc.player != null;
    }

    public static boolean canOpenGui() {
        if (canUpdate()) return mc.currentScreen == null;

        return mc.currentScreen instanceof TitleScreen || mc.currentScreen instanceof MultiplayerScreen || mc.currentScreen instanceof SelectWorldScreen;
    }

    public static boolean canCloseGui() {
        return mc.currentScreen instanceof TabScreen;
    }

    public static int random(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public static double random(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    public static void leftClick() {
        mc.options.attackKey.setPressed(true);
        ((MinecraftClientAccessor) mc).leftClick();
        mc.options.attackKey.setPressed(false);
    }

    public static void rightClick() {
        ((IMinecraftClient) mc).meteor_client$rightClick();
    }

    public static boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX || item == Items.WHITE_SHULKER_BOX || item == Items.ORANGE_SHULKER_BOX || item == Items.MAGENTA_SHULKER_BOX || item == Items.LIGHT_BLUE_SHULKER_BOX || item == Items.YELLOW_SHULKER_BOX || item == Items.LIME_SHULKER_BOX || item == Items.PINK_SHULKER_BOX || item == Items.GRAY_SHULKER_BOX || item == Items.LIGHT_GRAY_SHULKER_BOX || item == Items.CYAN_SHULKER_BOX || item == Items.PURPLE_SHULKER_BOX || item == Items.BLUE_SHULKER_BOX || item == Items.BROWN_SHULKER_BOX || item == Items.GREEN_SHULKER_BOX || item == Items.RED_SHULKER_BOX || item == Items.BLACK_SHULKER_BOX;
    }

    public static boolean isThrowable(Item item) {
        return item instanceof ExperienceBottleItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof FishingRodItem || item instanceof TridentItem;
    }

    public static void addEnchantment(ItemStack itemStack, RegistryEntry<Enchantment> enchantment, int level) {
        ItemEnchantmentsComponent.Builder b = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(itemStack));
        b.add(enchantment, level);

        EnchantmentHelper.set(itemStack, b.build());
    }

    public static void clearEnchantments(ItemStack itemStack) {
        EnchantmentHelper.apply(itemStack, components -> components.remove(a -> true));
    }

    public static void removeEnchantment(ItemStack itemStack, Enchantment enchantment) {
        EnchantmentHelper.apply(itemStack, components -> components.remove(enchantment1 -> enchantment1.value().equals(enchantment)));
    }

    public static Color lerp(Color first, Color second, @Range(from = 0, to = 1) float v) {
        return new Color(
            (int) (first.r * (1 - v) + second.r * v),
            (int) (first.g * (1 - v) + second.g * v),
            (int) (first.b * (1 - v) + second.b * v)
        );
    }

    public static boolean isLoading() {
        ResourceReloadLogger.ReloadState state = ((ResourceReloadLoggerAccessor) ((MinecraftClientAccessor) mc).getResourceReloadLogger()).getReloadState();
        return state == null || !((ReloadStateAccessor) state).isFinished();
    }

    public static int parsePort(String full) {
        if (full == null || full.isBlank() || !full.contains(":")) return -1;

        int port;

        try {
            port = Integer.parseInt(full.substring(full.lastIndexOf(':') + 1, full.length() - 1));
        } catch (NumberFormatException ignored) {
            port = -1;
        }

        return port;
    }

    public static String parseAddress(String full) {
        if (full == null || full.isBlank() || !full.contains(":")) return full;
        return full.substring(0, full.lastIndexOf(':'));
    }

    public static boolean resolveAddress(String address) {
        if (address == null || address.isBlank()) return false;

        int port = parsePort(address);
        if (port == -1) port = 25565;
        else address = parseAddress(address);

        return resolveAddress(address, port);
    }

    public static boolean resolveAddress(String address, int port) {
        if (port <= 0 || port > 65535 || address == null || address.isBlank()) return false;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        return !socketAddress.isUnresolved();
    }

    public static Vector3d set(Vector3d vec, Vec3d v) {
        vec.x = v.x;
        vec.y = v.y;
        vec.z = v.z;

        return vec;
    }

    public static Vector3d set(Vector3d vec, Entity entity, double tickDelta) {
        vec.x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        vec.y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        vec.z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

        return vec;
    }

    // Filters

    public static boolean nameFilter(String text, char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '_' || character == '-' || character == '.' || character == ' ';
    }

    public static boolean ipFilter(String text, char character) {
        if (text.contains(":") && character == ':') return false;
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '.' || character == '-';
    }
}
