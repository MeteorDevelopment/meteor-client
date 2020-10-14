package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.AlignmentX;
import minegame159.meteorclient.utils.AlignmentY;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

public class InventoryViewer extends ToggleModule {
    private final SettingGroup sgBackground = settings.createGroup("Background", "draw-background", "Draw inventory background.", true);
    private final SettingGroup sgX = settings.createGroup("X");
    private final SettingGroup sgY = settings.createGroup("Y");

    public enum mode {
        LIGHT,
        LIGHT_TRANSPARENT,
        DARK,
        DARK_TRANSPARENT,
        FLAT
    }

    private final Setting<mode> bgMode = sgBackground.add(new EnumSetting.Builder<mode>()
            .name("background-mode")
            .description("Which background to use.")
            .defaultValue(mode.LIGHT)
            .build()
    );
    public final Setting<Color> flatBgColor = sgBackground.add(new ColorSetting.Builder()
            .name("flat-background-color")
            .description("Flat background color.")
            .defaultValue(new Color(0, 0, 0, 64))
            .build()
    );

    private final Setting<AlignmentX> xAlignment = sgX.add(new EnumSetting.Builder<AlignmentX>()
            .name("x-alignment")
            .description("X alignment.")
            .defaultValue(AlignmentX.Left)
            .build()
    );

    private final Setting<Integer> xOffset = sgX.add(new IntSetting.Builder()
            .name("x-offset")
            .description("X offset.")
            .defaultValue(3)
            .sliderMax(200)
            .build()
    );

    private final Setting<AlignmentY> yAlignment = sgY.add(new EnumSetting.Builder<AlignmentY>()
            .name("y-alignment")
            .description("Y alignment.")
            .defaultValue(AlignmentY.Bottom)
            .build()
    );

    private final Setting<Integer> yOffset = sgY.add(new IntSetting.Builder()
            .name("y-offset")
            .description("Y offset.")
            .defaultValue(3)
            .sliderMax(200)
            .build()
    );

    private static final Identifier TEXTURE_LIGHT = new Identifier("meteor-client", "container_3x9.png");
    private static final Identifier TEXTURE_LIGHT_TRANSPARENT = new Identifier("meteor-client", "container_3x9-transparent.png");
    private static final Identifier TEXTURE_DARK = new Identifier("meteor-client", "container_3x9-dark.png");
    private static final Identifier TEXTURE_DARK_TRANSPARENT = new Identifier("meteor-client", "container_3x9-dark-transparent.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 67;

    public InventoryViewer() {
        super(Category.Render, "inventory-viewer", "Displays your inventory.");
    }



    @EventHandler
    private Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        int x = getX(event.screenWidth);
        int y = getY(event.screenHeight);
        if (sgBackground.isEnabled()) drawBackground(x, y);
        DiffuseLighting.enable();

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                drawItem(mc.player.inventory.getStack(9 + row * 9 + i), x + 8 + i * 18, y + 7 + row * 18);
            }
        }
        DiffuseLighting.disable();
    });


    private void drawItem(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
    }
    private void drawBackground(int x, int y) {
        Identifier BACKGROUND;
        int posX = getX(mc.getWindow().getScaledWidth());
        int posY = getY(mc.getWindow().getScaledHeight());

        switch(bgMode.get()) {
            case LIGHT:
                BACKGROUND = TEXTURE_LIGHT;
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(BACKGROUND);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, WIDTH, HEIGHT, HEIGHT, WIDTH);
                break;
            case LIGHT_TRANSPARENT:
                BACKGROUND = TEXTURE_LIGHT_TRANSPARENT;
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(BACKGROUND);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, WIDTH, HEIGHT, HEIGHT, WIDTH);
                break;
            case DARK:
                BACKGROUND = TEXTURE_DARK;
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(BACKGROUND);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, WIDTH, HEIGHT, HEIGHT, WIDTH);
                break;
            case DARK_TRANSPARENT:
                BACKGROUND = TEXTURE_DARK_TRANSPARENT;
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(BACKGROUND);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, WIDTH, HEIGHT, HEIGHT, WIDTH);
                break;
            case FLAT:
                ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
                ShapeBuilder.quad(posX + 6, posY + 6, WIDTH - 12, HEIGHT - 12, flatBgColor.get());
                ShapeBuilder.end();
        }
    }

    private int getX(int screenWidth) {
        switch (xAlignment.get()) {
            case Left:   return xOffset.get();
            case Center: return screenWidth / 2 - WIDTH / 2 + xOffset.get();
            case Right:  return screenWidth - WIDTH - xOffset.get();
            default:     return 0;
        }
    }

    private int getY(int screenHeight) {
        switch (yAlignment.get()) {
            case Top:    return yOffset.get();
            case Center: return screenHeight / 2 - HEIGHT / 2 + yOffset.get();
            case Bottom: return screenHeight - HEIGHT - yOffset.get();
            default:     return 0;
        }
    }
}
