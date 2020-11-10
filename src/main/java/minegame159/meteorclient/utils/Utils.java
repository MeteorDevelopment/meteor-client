package minegame159.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.mixininterface.IMinecraftServer;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.options.ServerList;
import net.minecraft.client.render.Camera;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShapes;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class Utils {
    public static MinecraftClient mc;

    public static boolean blockRenderingBlockEntitiesInXray;
    public static boolean firstTimeTitleScreen = true;

    private static final Random random = new Random();
    private static final Vec3d eyesPos = new Vec3d(0, 0, 0);
    private static final Vec3d vec1 = new Vec3d(0, 0, 0);
    private static final Vec3d vec2 = new Vec3d(0, 0, 0);
    private static final DecimalFormat df;

    static {
        df = new DecimalFormat("0");
        df.setMaximumFractionDigits(340);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);
    }

    public static void addMeteorPvpToServerList() {
        ServerList servers = new ServerList(mc);
        servers.loadFile();

        boolean contains = false;
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo server = servers.get(i);

            if (server.address.contains("pvp.meteorclient.com")) {
                contains = true;
                break;
            }
        }

        if (!contains) {
            servers.add(new ServerInfo("Meteor Pvp", "pvp.meteorclient.com", false));
            servers.saveFile();
        }
    }

    public static int getWindowWidth() {
        return mc.getWindow().getFramebufferWidth();
    }

    public static int getWindowHeight() {
        return mc.getWindow().getFramebufferHeight();
    }

    public static void unscaledProjection() {
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
    }

    public static void scaledProjection() {
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / MinecraftClient.getInstance().getWindow().getScaleFactor(), MinecraftClient.getInstance().getWindow().getFramebufferHeight() / MinecraftClient.getInstance().getWindow().getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
    }

    public static Dimension getDimension() {
        switch (MinecraftClient.getInstance().world.getRegistryKey().getValue().getPath()) {
            case "the_nether": return Dimension.Nether;
            case "the_end":    return Dimension.End;
            default:           return Dimension.Overworld;
        }
    }

    public static Vec3d vec3d(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static void getItemsInContainerItem(ItemStack itemStack, ItemStack[] items) {
        Arrays.fill(items, ItemStack.EMPTY);
        CompoundTag nbt = itemStack.getTag();

        if (nbt != null && nbt.contains("BlockEntityTag")) {
            CompoundTag nbt2 = nbt.getCompound("BlockEntityTag");
            if (nbt2.contains("Items")) {
                ListTag nbt3 = (ListTag) nbt2.get("Items");
                for (int i = 0; i < nbt3.size(); i++) {
                    items[nbt3.getCompound(i).getByte("Slot")] = ItemStack.fromTag(nbt3.getCompound(i));
                }
            }
        }
    }

    public static double getScaledWindowWidthGui() {
        return mc.getWindow().getFramebufferWidth() / GuiConfig.INSTANCE.guiScale;
    }

    public static double getScaledWindowHeightGui() {
        return mc.getWindow().getFramebufferHeight() / GuiConfig.INSTANCE.guiScale;
    }

    public static Object2IntMap<StatusEffect> createStatusEffectMap() {
        Object2IntMap<StatusEffect> map = new Object2IntArrayMap<>(Registry.STATUS_EFFECT.getIds().size());

        Registry.STATUS_EFFECT.forEach(potion -> map.put(potion, 0));

        return map;
    }

    public static String getEnchantShortName(Enchantment enchantment) {
        if (enchantment == Enchantments.FIRE_PROTECTION) return "F Prot";
        if (enchantment == Enchantments.FEATHER_FALLING) return "Fea Fa";
        if (enchantment == Enchantments.BLAST_PROTECTION) return "B Prot";
        if (enchantment == Enchantments.PROJECTILE_PROTECTION) return "P Prot";
        if (enchantment == Enchantments.AQUA_AFFINITY) return "Aqua A";
        if (enchantment == Enchantments.THORNS) return "Thorns";
        if (enchantment == Enchantments.DEPTH_STRIDER) return "Depth S";
        if (enchantment == Enchantments.FROST_WALKER) return "Frost W";
        if (enchantment == Enchantments.BINDING_CURSE) return "Curse B";
        if (enchantment == Enchantments.SMITE) return "Smite";
        if (enchantment == Enchantments.BANE_OF_ARTHROPODS) return "Bane A";
        if (enchantment == Enchantments.FIRE_ASPECT) return "Fire A";
        if (enchantment == Enchantments.SILK_TOUCH) return "Silk T";
        if (enchantment == Enchantments.POWER) return "Power";
        if (enchantment == Enchantments.PUNCH) return "Punch";
        if (enchantment == Enchantments.FLAME) return "Flame";
        if (enchantment == Enchantments.LUCK_OF_THE_SEA) return "Luck S";
        if (enchantment == Enchantments.QUICK_CHARGE) return "Quick C";
        if (enchantment == Enchantments.VANISHING_CURSE) return "Curse V";

        return enchantment.getName(0).getString().substring(0, 4);
    }

    public static int search(String text, String filter) {
        int wordsFound = 0;
        String[] words = filter.split(" ");

        for (String word : words) {
            if (StringUtils.containsIgnoreCase(text, word)) wordsFound++;
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
        if (mc.isInSingleplayer()) {
            // Singleplayer
            File folder = ((IMinecraftServer) mc.getServer()).getSession().getWorldDirectory(mc.world.getRegistryKey());
            if (folder.toPath().relativize(mc.runDirectory.toPath()).getNameCount() != 2) {
                folder = folder.getParentFile();
            }
            return folder.getName();
        }

        // Multiplayer
        String name = mc.isConnectedToRealms() ? "realms" : mc.getCurrentServerEntry().address;
        if (SystemUtils.IS_OS_WINDOWS) {
            name = name.replace(":", "_");
        }
        return name;
    }

    public static String nameToTitle(String name) {
        return Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    public static String getKeyName(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_UNKNOWN: return "Unknown";
            case GLFW.GLFW_KEY_ESCAPE: return "Esc";
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "Print Screen";
            case GLFW.GLFW_KEY_PAUSE: return "Pause";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Control";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Control";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_UP: return "Arrow Up";
            case GLFW.GLFW_KEY_DOWN: return "Arrow Down";
            case GLFW.GLFW_KEY_LEFT: return "Arrow Left";
            case GLFW.GLFW_KEY_RIGHT: return "Arrow Right";
            case GLFW.GLFW_KEY_APOSTROPHE: return "Apostrophe";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_MENU: return "Menu";
            case GLFW.GLFW_KEY_LEFT_SUPER: return "Left Super";
            case GLFW.GLFW_KEY_RIGHT_SUPER: return "Right Super";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_NUM_LOCK: return "Num Lock";
            case GLFW.GLFW_KEY_SPACE: return "Space";
            default:
                String keyName = GLFW.glfwGetKeyName(key, 0);
                if (keyName == null) return "Unknown";
                return keyName;
        }
    }

    public static byte[] readBytes(File file) {
        try {
            InputStream in = new FileInputStream(file);
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

    public static boolean place(BlockState blockState, BlockPos blockPos, boolean swingHand, boolean checkFaceVisibility, boolean checkForEntities) {
        // Calculate eyes pos
        ((IVec3d) eyesPos).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            // Check if side is visible (facing away from player)
            if (checkFaceVisibility) {
                ((IVec3d) vec1).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                ((IVec3d) vec2).set(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5);
                if (eyesPos.squaredDistanceTo(vec1) >= eyesPos.squaredDistanceTo(vec2)) continue;
            }

            // Check if neighbor can be right clicked
            if (mc.world.getBlockState(neighbor).getOutlineShape(mc.world, blockPos) == VoxelShapes.empty()) continue;

            // Calculate hit pos
            ((IVec3d) vec1).set(neighbor.getX() + 0.5 + side2.getVector().getX() * 0.5, neighbor.getY() + 0.5 + side2.getVector().getY()     * 0.5, neighbor.getZ() + 0.5 + side2.getVector().getZ() * 0.5);

            // Check if hitVec is within range (4.25 blocks)
            if(eyesPos.squaredDistanceTo(vec1) > 18.0625) continue;

            // Check if intersects entities
            if (checkForEntities && !mc.world.canPlace(blockState, blockPos, ShapeContext.absent())) continue;

            // Place block
            PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(getNeededYaw(vec1), getNeededPitch(vec1), mc.player.isOnGround());
            mc.player.networkHandler.sendPacket(packet);
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(vec1, side2, neighbor, false));
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            if (swingHand) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            return true;
        }

        return false;
    }
    public static boolean place(BlockState blockState, BlockPos blockPos) {
        return place(blockState, blockPos, true, true, true);
    }

    public static float getNeededYaw(Vec3d vec) {
        return mc.player.yaw + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), vec.x - mc.player.getX())) - 90f - mc.player.yaw);
    }

    public static float getNeededPitch(Vec3d vec) {
        double diffX = vec.x - mc.player.getX();
        double diffY = vec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = vec.z - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.pitch + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.pitch);
    }

    public static double distanceToCamera(double x, double y, double z) {
        Camera camera = mc.gameRenderer.getCamera();
        return Math.sqrt(camera.getPos().squaredDistanceTo(x, y, z));
    }
    public static double distanceToCamera(Entity entity) {
        return distanceToCamera(entity.getX(), entity.getY(), entity.getZ());
    }

    public static boolean canUpdate() {
        return mc.world != null || mc.player != null;
    }

    public static int random(int min, int max) {
        return random.nextInt(max - min) + min;
    }
    public static double random(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    public static void sendMessage(String msg, Object... args) {
        if (mc.player == null) return;

        msg = String.format(msg, args);
        msg = msg.replaceAll("#yellow", Formatting.YELLOW.toString());
        msg = msg.replaceAll("#white", Formatting.WHITE.toString());
        msg = msg.replaceAll("#red", Formatting.RED.toString());
        msg = msg.replaceAll("#blue", Formatting.BLUE.toString());
        msg = msg.replaceAll("#pink", Formatting.LIGHT_PURPLE.toString());
        msg = msg.replaceAll("#gray", Formatting.GRAY.toString());

        mc.player.sendMessage(new LiteralText(msg), false);
    }

    public static void leftClick() {
        ((IMinecraftClient) mc).leftClick();
    }
    public static void rightClick() {
        ((IMinecraftClient) mc).rightClick();
    }

    public static Module tryToGetModule(String[] args) {
        if (args.length < 1) {
            Chat.error("You must specify module name.");
            return null;
        }
        Module oldModule = ModuleManager.INSTANCE.get(args[0]);
        if (oldModule == null) {
            Chat.error("Module with name (highlight)%s (default)doesn't exist.", args[0]);
            return null;
        }
        return oldModule;
    }

    public static boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX || item == Items.WHITE_SHULKER_BOX || item == Items.ORANGE_SHULKER_BOX || item == Items.MAGENTA_SHULKER_BOX || item == Items.LIGHT_BLUE_SHULKER_BOX || item == Items.YELLOW_SHULKER_BOX || item == Items.LIME_SHULKER_BOX || item == Items.PINK_SHULKER_BOX || item == Items.GRAY_SHULKER_BOX || item == Items.LIGHT_GRAY_SHULKER_BOX || item == Items.CYAN_SHULKER_BOX || item == Items.PURPLE_SHULKER_BOX || item == Items.BLUE_SHULKER_BOX || item == Items.BROWN_SHULKER_BOX || item == Items.GREEN_SHULKER_BOX || item == Items.RED_SHULKER_BOX || item == Items.BLACK_SHULKER_BOX;
    }

    public static boolean isThrowable(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof FishingRodItem || item instanceof TridentItem;
    }

    public static String floatToString(float number) {
        if (number % 1 == 0) return Integer.toString((int) number);
        return Float.toString(number);
    }

    public static String doubleToString(double number) {
        if (number % 1 == 0) return Integer.toString((int) number);
        return df.format(number);
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static void addEnchantment(ItemStack itemStack, Enchantment enchantment, int level) {
        itemStack.getOrCreateTag();
        if (!itemStack.getTag().contains("Enchantments", 9)) {
            itemStack.getTag().put("Enchantments", new ListTag());
        }

        ListTag listTag = itemStack.getTag().getList("Enchantments", 10);
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("id", String.valueOf(Registry.ENCHANTMENT.getId(enchantment)));
        compoundTag.putShort("lvl", (short) level);
        listTag.add(compoundTag);
    }
}
