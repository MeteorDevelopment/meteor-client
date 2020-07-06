package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.AlignmentX;
import minegame159.meteorclient.utils.AlignmentY;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class InventoryViewer extends ToggleModule {
    private final SettingGroup sgBackground = settings.createGroup("Background", "draw-background", "Draw inventory background.", true);
    private final SettingGroup sgX = settings.createGroup("X");
    private final SettingGroup sgY = settings.createGroup("Y");
    
    private final Setting<Boolean> bgTransparent = sgBackground.add(new BoolSetting.Builder()
            .name("background-transparent")
            .description("Draws inventory background transparent.")
            .defaultValue(false)
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
            .build()
    );

    private static final Identifier TEXTURE = new Identifier("meteor-client", "container_3x9.png");
    private static final Identifier TEXTURE_TRANSPARENT = new Identifier("meteor-client", "container_3x9-transparent.png");

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
                drawItem(mc.player.inventory.getInvStack(9 + row * 9 + i), x + 8 + i * 18, y + 7 + row * 18);
            }
        }

        DiffuseLighting.disable();
    });

    private void drawItem(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItem(mc.player, itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
    }

    private void drawBackground(int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(bgTransparent.get() ? TEXTURE_TRANSPARENT : TEXTURE);
        DrawableHelper.blit(x, y, 0, 0, 0, WIDTH, HEIGHT, HEIGHT, WIDTH);
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
