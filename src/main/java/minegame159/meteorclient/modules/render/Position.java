package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.world.dimension.DimensionType;

public class Position extends Module {
    public Position() {
        super(Category.Render, "position", "Displays your position.");
    }

    private void drawPosition(int screenWidth, String text, int yy, double x, double y, double z) {
        String msg1 = String.format("%.1f %.1f %.1f", x, y, z);
        int x1 = screenWidth - Utils.getTextWidth(msg1) - 2;
        int x2 = screenWidth - Utils.getTextWidth(msg1) - Utils.getTextWidth(text) - 2;
        Utils.drawTextWithShadow(msg1, x1, yy, Color.fromRGBA(185, 185, 185, 255));
        Utils.drawTextWithShadow(text, x2, yy, Color.fromRGBA(255, 255, 255, 255));
    }

    @EventHandler
    private Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        int y = event.screenHeight - Utils.getTextHeight() - 2;

        if (mc.player.dimension == DimensionType.OVERWORLD) {
            drawPosition(event.screenWidth, "Nether Pos: ", y, mc.player.x / 8.0, mc.player.y / 8.0, mc.player.z / 8.0);
            y -= Utils.getTextHeight() + 2;
            drawPosition(event.screenWidth, "Pos: ", y, mc.player.x, mc.player.y, mc.player.z);
        } else if (mc.player.dimension == DimensionType.THE_NETHER) {
            drawPosition(event.screenWidth, "Overworld Pos: ", y, mc.player.x * 8.0, mc.player.y * 8.0, mc.player.z * 8.0);
            y -= Utils.getTextHeight() + 2;
            drawPosition(event.screenWidth, "Pos: ", y, mc.player.x, mc.player.y, mc.player.z);
        } else if (mc.player.dimension == DimensionType.THE_END) {
            drawPosition(event.screenWidth, "Pos: ", y, mc.player.x, mc.player.y, mc.player.z);
        }
    });
}
