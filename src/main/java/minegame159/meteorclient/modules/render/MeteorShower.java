package minegame159.meteorclient.modules.render;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.Matrices;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.util.Identifier;

public final class MeteorShower extends ToggleModule {
    private final Identifier[] meteors =
            {new Identifier("meteor-client", "meteor1.png"),
                    new Identifier("meteor-client", "meteor2.png"),
                    new Identifier("meteor-client", "meteor3.png"),
                    new Identifier("meteor-client", "meteor4.png")};

    private int ticks = 0;

    public MeteorShower() {
        super(Category.Render, "meteor-shower", ";)");
    }

    public void onActivate() {
        drawShower();
    }

    private void drawShower() {    {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(meteors[ticks / 8]);
        int WIDTH = mc.getWindow().getScaledWidth();
        int HEIGHT = mc.getWindow().getScaledHeight();
        DrawableHelper.drawTexture(Matrices.getMatrixStack(), 0, 0, 0, 0, 0, WIDTH, HEIGHT, HEIGHT, WIDTH);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
}