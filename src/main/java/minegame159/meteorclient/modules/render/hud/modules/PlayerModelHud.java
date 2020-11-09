package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.rendering.ShapeBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

public class PlayerModelHud extends HudModule {
    public PlayerModelHud(HUD hud) {
        super(hud, "player-model", "Displays your player model.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(51 * hud.playerModelScale(), 75 * hud.playerModelScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int x = box.getX();
        int y = box.getY();

        if (hud.playerModelBackground()) {
            ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
            ShapeBuilder.quad(x, y, box.width, box.height, hud.playerModelColor());
            ShapeBuilder.end();
        }

        if (mc.player != null) {
            InventoryScreen.drawEntity(x + (int) (25 * hud.playerModelScale()), y + (int) (66 * hud.playerModelScale()), (int) (30 * hud.playerModelScale()), 0, 0, mc.player);
        }
    }
}
