package minegame159.meteorclient.utils;

import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.Random;

public class Utils {
    public static MinecraftClient mc;
    public static int offhandSlotId = 45;

    private static Random random = new Random();

    public static boolean canUpdate() {
        return mc.world != null && mc.player != null;
    }

    public static int random(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public static int getTextWidth(String text) {
        return mc.textRenderer.getStringWidth(text);
    }
    public static int getTextHeight() {
        return mc.textRenderer.fontHeight;
    }

    public static void drawText(String text, float x, float y, int color) {
        mc.textRenderer.draw(text, x, y, color);
    }
    public static void drawTextWithShadow(String text, float x, float y, int color) {
        mc.textRenderer.drawWithShadow(text, x, y, color);
    }

    public static void sendMessage(String msg, Object... args) {
        msg = String.format(msg, args);
        msg = msg.replaceAll("#yellow", Formatting.YELLOW.toString());
        msg = msg.replaceAll("#white", Formatting.WHITE.toString());
        msg = msg.replaceAll("#red", Formatting.RED.toString());
        msg = msg.replaceAll("#blue", Formatting.BLUE.toString());
        msg = msg.replaceAll("#pink", Formatting.LIGHT_PURPLE.toString());
        msg = msg.replaceAll("#gray", Formatting.GRAY.toString());

        mc.player.sendMessage(new LiteralText(msg));
    }

    public static void leftClick() {
        ((IMinecraftClient) mc).leftClick();
    }
    public static void rightClick() {
        ((IMinecraftClient) mc).rightClick();
    }

    public static Module tryToGetModule(String[] args) {
        if (args.length < 1) {
            Utils.sendMessage("#redYou must specify module name.");
            return null;
        }
        Module module = ModuleManager.INSTANCE.get(args[0]);
        if (module == null) {
            Utils.sendMessage("#redModule with name #blue'%s' #reddoesn't exist.", args[0]);
            return null;
        }
        return module;
    }

    public static boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX && item == Items.WHITE_SHULKER_BOX && item == Items.ORANGE_SHULKER_BOX && item == Items.MAGENTA_SHULKER_BOX && item == Items.LIGHT_BLUE_SHULKER_BOX && item == Items.YELLOW_SHULKER_BOX && item == Items.LIME_SHULKER_BOX && item == Items.PINK_SHULKER_BOX && item == Items.GRAY_SHULKER_BOX && item == Items.LIGHT_GRAY_SHULKER_BOX && item == Items.CYAN_SHULKER_BOX && item == Items.PURPLE_SHULKER_BOX && item == Items.BLUE_SHULKER_BOX && item == Items.BROWN_SHULKER_BOX && item == Items.GREEN_SHULKER_BOX && item == Items.RED_SHULKER_BOX && item == Items.BLACK_SHULKER_BOX;
    }

    public static String floatToString(float number) {
        if (number % 1 == 0) return Integer.toString((int) number);
        return Float.toString(number);
    }

    public static String doubleToString(double number) {
        if (number % 1 == 0) return Integer.toString((int) number);
        return Double.toString(number);
    }

    public static int invIndexToSlotId(int invIndex) {
        if (invIndex < 9) return 44 - (8 - invIndex);
        return invIndex;
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
}
