package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class CompassHud extends HudModule {

    public enum Mode {
        Axis,
        Pole
    }

    public static MinecraftClient mc = MinecraftClient.getInstance();

    public CompassHud(HUD hud) {
        super(hud, "compass", "Displays your rotation as a 3d compass.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(100, 100);
    }

    private static final Color RED = new Color(225, 45, 45);
    private static final Color WHITE = new Color(225, 225, 225);

    @Override
    public void render(HudRenderer renderer) {
        int x = box.getX();
        int y = box.getY();

        for (Direction dir : Direction.values()) {
            renderer.text(hud.compassMode() == Mode.Axis ? dir.getAlternate() : dir.name(), (x + (box.width / 2.0)) + getX(getPosOnCompass(dir)), (y + (box.height / 2.0)) + getY(getPosOnCompass(dir)), (dir == Direction.N) ? RED : WHITE);
        }
    }

    private double getX(double rad) {
        return Math.sin(rad) * (hud.compassScale() * 40);
    }

    private double getY(double rad) {
        if (mc.player == null) return 0;
        return Math.cos(rad) * Math.sin(Math.toRadians(MathHelper.clamp(mc.player.pitch + 30.0f, -90.0f, 90.0f))) * (hud.compassScale() * 40);
    }

    private static double getPosOnCompass(Direction dir) {
        if (mc.player == null) return 0;
        return Math.toRadians(MathHelper.wrapDegrees(mc.player.yaw)) + dir.ordinal() * 1.5707963267948966;
    }

    private enum Direction {
        N("Z-"),
        W("X-"),
        S("Z+"),
        E("X+");

        String alternate;

        Direction(String alternate) {
            this.alternate = alternate;
        }

        public String getAlternate() {
            return alternate;
        }
    }
}