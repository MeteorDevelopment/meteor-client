/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.*;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
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
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Range;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.*;

public class Utils {
    private static final Random random = new Random();
    public static boolean firstTimeTitleScreen = true;
    public static boolean isReleasingTrident;
    public static final Color WHITE = new Color(255, 255, 255);
    public static boolean rendering3D = true;
    public static double frameTime;
    public static Screen screenToOpen;

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

    public static double getPlayerSpeed() {
        if (mc.player == null) return 0;

        double tX = Math.abs(mc.player.getX() - mc.player.prevX);
        double tZ = Math.abs(mc.player.getZ() - mc.player.prevZ);
        double length = Math.sqrt(tX * tX + tZ * tZ);

        Timer timer = Modules.get().get(Timer.class);
        if (timer.isActive()) length *= Modules.get().get(Timer.class).getMultiplier();

        return length * 20;
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

    public static void getEnchantments(ItemStack itemStack, Object2IntMap<Enchantment> enchantments) {
        enchantments.clear();

        if (!itemStack.isEmpty()) {
            NbtList listTag = itemStack.getItem() == Items.ENCHANTED_BOOK ? EnchantedBookItem.getEnchantmentNbt(itemStack) : itemStack.getEnchantments();

            for (int i = 0; i < listTag.size(); ++i) {
                NbtCompound tag = listTag.getCompound(i);

                Registry.ENCHANTMENT.getOrEmpty(Identifier.tryParse(tag.getString("id"))).ifPresent((enchantment) -> enchantments.put(enchantment, tag.getInt("lvl")));
            }
        }
    }

    public static boolean hasEnchantments(ItemStack itemStack, Enchantment... enchantments) {
        if (itemStack.isEmpty()) return false;

        Object2IntMap<Enchantment> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);
        for (Enchantment enchantment : enchantments) if (!itemEnchantments.containsKey(enchantment)) return false;

        return true;
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
        RenderSystem.setProjectionMatrix(Matrix4f.projectionMatrix(0, mc.getWindow().getFramebufferWidth(), 0, mc.getWindow().getFramebufferHeight(), 1000, 3000));
        rendering3D = false;
    }

    public static void scaledProjection() {
        RenderSystem.setProjectionMatrix(Matrix4f.projectionMatrix(0, (float) (mc.getWindow().getFramebufferWidth() / mc.getWindow().getScaleFactor()), 0, (float) (mc.getWindow().getFramebufferHeight() / mc.getWindow().getScaleFactor()), 1000, 3000));
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
        NbtCompound nbt = itemStack.getNbt();

        if (nbt != null && nbt.contains("BlockEntityTag")) {
            NbtCompound nbt2 = nbt.getCompound("BlockEntityTag");

            if (nbt2.contains("Items")) {
                NbtList nbt3 = (NbtList) nbt2.get("Items");

                for (int i = 0; i < nbt3.size(); i++) {
                    int slot = nbt3.getCompound(i).getByte("Slot"); // Apparently shulker boxes can store more than 27 items, good job Mojang
                    if (slot >= 0 && slot < items.length) items[slot] = ItemStack.fromNbt(nbt3.getCompound(i));
                }
            }
        }
    }

    public static Color getShulkerColor(ItemStack shulkerItem) {
        if (!(shulkerItem.getItem() instanceof BlockItem)) return WHITE;
        Block block = ((BlockItem) shulkerItem.getItem()).getBlock();
        if (block == Blocks.ENDER_CHEST) return BetterTooltips.ECHEST_COLOR;
        if (!(block instanceof ShulkerBoxBlock)) return WHITE;
        ShulkerBoxBlock shulkerBlock = (ShulkerBoxBlock) ShulkerBoxBlock.getBlockFromItem(shulkerItem.getItem());
        DyeColor dye = shulkerBlock.getColor();
        if (dye == null) return WHITE;
        final float[] colors = dye.getColorComponents();
        return new Color(colors[0], colors[1], colors[2], 1f);
    }

    public static boolean hasItems(ItemStack itemStack) {
        NbtCompound compoundTag = itemStack.getSubNbt("BlockEntityTag");
        return compoundTag != null && compoundTag.contains("Items", 9);
    }

    public static Object2IntMap<StatusEffect> createStatusEffectMap() {
        Object2IntMap<StatusEffect> map = new Object2IntArrayMap<>(Registry.STATUS_EFFECT.getIds().size());

        Registry.STATUS_EFFECT.forEach(potion -> map.put(potion, 0));

        return map;
    }

    public static String getEnchantSimpleName(Enchantment enchantment, int length) {
        return enchantment.getName(0).getString().substring(0, length);
    }

    public static int search(String text, String filter) {
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
            String name = mc.isConnectedToRealms() ? "realms" : mc.getCurrentServerEntry().address;
            if (SystemUtils.IS_OS_WINDOWS) {
                name = name.replace(":", "_");
            }
            return name;
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
        switch (key) {
            case GLFW_KEY_UNKNOWN: return "Unknown";
            case GLFW_KEY_ESCAPE: return "Esc";
            case GLFW_KEY_GRAVE_ACCENT: return "Grave Accent";
            case GLFW_KEY_WORLD_1: return "World 1";
            case GLFW_KEY_WORLD_2: return "World 2";
            case GLFW_KEY_PRINT_SCREEN: return "Print Screen";
            case GLFW_KEY_PAUSE: return "Pause";
            case GLFW_KEY_INSERT: return "Insert";
            case GLFW_KEY_DELETE: return "Delete";
            case GLFW_KEY_HOME: return "Home";
            case GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW_KEY_END: return "End";
            case GLFW_KEY_TAB: return "Tab";
            case GLFW_KEY_LEFT_CONTROL: return "Left Control";
            case GLFW_KEY_RIGHT_CONTROL: return "Right Control";
            case GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW_KEY_UP: return "Arrow Up";
            case GLFW_KEY_DOWN: return "Arrow Down";
            case GLFW_KEY_LEFT: return "Arrow Left";
            case GLFW_KEY_RIGHT: return "Arrow Right";
            case GLFW_KEY_APOSTROPHE: return "Apostrophe";
            case GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW_KEY_MENU: return "Menu";
            case GLFW_KEY_LEFT_SUPER: return "Left Super";
            case GLFW_KEY_RIGHT_SUPER: return "Right Super";
            case GLFW_KEY_ENTER: return "Enter";
            case GLFW_KEY_KP_ENTER: return "Numpad Enter";
            case GLFW_KEY_NUM_LOCK: return "Num Lock";
            case GLFW_KEY_SPACE: return "Space";
            case GLFW_KEY_F1: return "F1";
            case GLFW_KEY_F2: return "F2";
            case GLFW_KEY_F3: return "F3";
            case GLFW_KEY_F4: return "F4";
            case GLFW_KEY_F5: return "F5";
            case GLFW_KEY_F6: return "F6";
            case GLFW_KEY_F7: return "F7";
            case GLFW_KEY_F8: return "F8";
            case GLFW_KEY_F9: return "F9";
            case GLFW_KEY_F10: return "F10";
            case GLFW_KEY_F11: return "F11";
            case GLFW_KEY_F12: return "F12";
            case GLFW_KEY_F13: return "F13";
            case GLFW_KEY_F14: return "F14";
            case GLFW_KEY_F15: return "F15";
            case GLFW_KEY_F16: return "F16";
            case GLFW_KEY_F17: return "F17";
            case GLFW_KEY_F18: return "F18";
            case GLFW_KEY_F19: return "F19";
            case GLFW_KEY_F20: return "F20";
            case GLFW_KEY_F21: return "F21";
            case GLFW_KEY_F22: return "F22";
            case GLFW_KEY_F23: return "F23";
            case GLFW_KEY_F24: return "F24";
            case GLFW_KEY_F25: return "F25";
            default:
                String keyName = glfwGetKeyName(key, 0);
                if (keyName == null) return "Unknown";
                return StringUtils.capitalize(keyName);
        }
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
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buffer = new byte[256];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);

            in.close();
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static boolean canUpdate() {
        return mc != null && mc.world != null && mc.player != null;
    }

    public static boolean canOpenGui() {
        if (canUpdate()) return mc.currentScreen == null;

        return mc.currentScreen instanceof TitleScreen || mc.currentScreen instanceof MultiplayerScreen || mc.currentScreen instanceof SelectWorldScreen;
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
        ((IMinecraftClient) mc).rightClick();
    }

    public static boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX || item == Items.WHITE_SHULKER_BOX || item == Items.ORANGE_SHULKER_BOX || item == Items.MAGENTA_SHULKER_BOX || item == Items.LIGHT_BLUE_SHULKER_BOX || item == Items.YELLOW_SHULKER_BOX || item == Items.LIME_SHULKER_BOX || item == Items.PINK_SHULKER_BOX || item == Items.GRAY_SHULKER_BOX || item == Items.LIGHT_GRAY_SHULKER_BOX || item == Items.CYAN_SHULKER_BOX || item == Items.PURPLE_SHULKER_BOX || item == Items.BLUE_SHULKER_BOX || item == Items.BROWN_SHULKER_BOX || item == Items.GREEN_SHULKER_BOX || item == Items.RED_SHULKER_BOX || item == Items.BLACK_SHULKER_BOX;
    }

    public static boolean isThrowable(Item item) {
        return item instanceof ExperienceBottleItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof FishingRodItem || item instanceof TridentItem;
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static void addEnchantment(ItemStack itemStack, Enchantment enchantment, int level) {
        NbtCompound tag = itemStack.getOrCreateNbt();
        NbtList listTag;

        // Get list tag
        if (!tag.contains("Enchantments", 9)) {
            listTag = new NbtList();
            tag.put("Enchantments", listTag);
        } else {
            listTag = tag.getList("Enchantments", 10);
        }

        // Check if item already has the enchantment and modify the level
        String enchId = Registry.ENCHANTMENT.getId(enchantment).toString();

        for (NbtElement _t : listTag) {
            NbtCompound t = (NbtCompound) _t;

            if (t.getString("id").equals(enchId)) {
                t.putShort("lvl", (short) level);
                return;
            }
        }

        // Add the enchantment if it doesn't already have it
        NbtCompound enchTag = new NbtCompound();
        enchTag.putString("id", enchId);
        enchTag.putShort("lvl", (short) level);

        listTag.add(enchTag);
    }

    public static void clearEnchantments(ItemStack itemStack) {
        NbtCompound nbt = itemStack.getNbt();
        if (nbt != null) nbt.remove("Enchantments");
    }

    public static void removeEnchantment(ItemStack itemStack, Enchantment enchantment) {
        NbtCompound nbt = itemStack.getNbt();
        if (nbt == null) return;

        if (!nbt.contains("Enchantments", 9)) return;
        NbtList list = nbt.getList("Enchantments", 10);

        String enchId = Registry.ENCHANTMENT.getId(enchantment).toString();

        for (Iterator<NbtElement> it = list.iterator(); it.hasNext();) {
            NbtCompound ench = (NbtCompound) it.next();

            if (ench.getString("id").equals(enchId)) {
                it.remove();
                break;
            }
        }
    }

    @SafeVarargs
    public static <T> Object2BooleanOpenHashMap<T> asO2BMap(T... checked) {
        Map<T, Boolean> map = new HashMap<>();
        for (T item : checked)
            map.put(item, true);
        return new Object2BooleanOpenHashMap<>(map);
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
        }
        catch (NumberFormatException ignored) {
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

    // Filters

    public static boolean nameFilter(String text, char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '_' || character == '-' || character == '.' || character == ' ';
    }

    public static boolean ipFilter(String text, char character) {
        if (text.contains(":") && character == ':') return false;
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '.';
    }
}
