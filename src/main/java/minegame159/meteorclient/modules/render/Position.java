package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.world.dimension.DimensionType;

import java.text.DecimalFormat;

public class Position extends Module {
    private DecimalFormat decimalFormat = new DecimalFormat(".#");

    public Position() {
        super(Category.Render, "position", "Displays your position.");
    }

    private void drawPosition(int screenWidth, String text, int yy, double x, double y, double z) {
        String msg1 = decimalFormat.format(x) + " " + decimalFormat.format(y) + " " + decimalFormat.format(z);
        int x1 = screenWidth - Utils.getTextWidth(msg1) - 2;
        int x2 = screenWidth - Utils.getTextWidth(msg1) - Utils.getTextWidth(text) - 2;
        Utils.drawText(msg1, x1, yy, Color.fromRGBA(185, 185, 185, 255));
        Utils.drawText(text, x2, yy, Color.fromRGBA(255, 255, 255, 255));
    }

    @SubscribeEvent
    private void onRender2D(Render2DEvent e) {
        int y = e.screenHeight - Utils.getTextHeight() - 2;

        if (mc.player.dimension == DimensionType.OVERWORLD) {
            drawPosition(e.screenWidth, "Nether Pos: ", y, mc.player.getX() / 8.0, mc.player.getY() / 8.0, mc.player.getZ() / 8.0);
            y -= Utils.getTextHeight() + 2;
            drawPosition(e.screenWidth, "Pos: ", y, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        } else if (mc.player.dimension == DimensionType.THE_NETHER) {
            drawPosition(e.screenWidth, "Overworld Pos: ", y, mc.player.getX() * 8.0, mc.player.getY() * 8.0, mc.player.getZ() * 8.0);
            y -= Utils.getTextHeight() + 2;
            drawPosition(e.screenWidth, "Pos: ", y, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        } else if (mc.player.dimension == DimensionType.THE_END) {
            drawPosition(e.screenWidth, "Pos: ", y, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
    }
}
