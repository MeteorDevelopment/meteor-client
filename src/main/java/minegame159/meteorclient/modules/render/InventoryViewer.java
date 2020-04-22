package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.AlignmentX;
import minegame159.meteorclient.utils.AlignmentY;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class InventoryViewer extends ToggleModule {
    private Setting<Boolean> drawBackground = addSetting(new BoolSetting.Builder()
            .name("draw-background")
            .description("Draws inventory background.")
            .defaultValue(true)
            .build()
    );

    private Setting<AlignmentX> xAlignment = addSetting(new EnumSetting.Builder<AlignmentX>()
            .name("x-alignment")
            .description("X alignment.")
            .group("X")
            .defaultValue(AlignmentX.Left)
            .build()
    );

    private Setting<Integer> xOffset = addSetting(new IntSetting.Builder()
            .name("x-offset")
            .description("X offset.")
            .group("X")
            .defaultValue(3)
            .build()
    );

    private Setting<AlignmentY> yAlignment = addSetting(new EnumSetting.Builder<AlignmentY>()
            .name("y-alignment")
            .description("Y alignment.")
            .group("Y")
            .defaultValue(AlignmentY.Bottom)
            .build()
    );

    private Setting<Integer> yOffset = addSetting(new IntSetting.Builder()
            .name("y-offset")
            .description("Y offset.")
            .group("Y")
            .defaultValue(3)
            .build()
    );

    private static final Identifier TEXTURE = new Identifier("meteor-client", "container_3x9.png");

    private int width = 176;
    private int height = 67;

    public InventoryViewer() {
        super(Category.Render, "inventory-viewer", "Displays ur inventory.");
    }

    @EventHandler
    private Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        int x = getX(event.screenWidth);
        int y = getY(event.screenHeight);

        if (drawBackground.get()) drawBackground(x, y);
        DiffuseLighting.enableForItems();

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                drawItem(mc.player.inventory.getInvStack(9 + row * 9 + i), x + 8 + i * 18, y + 7 + row * 18);
            }
        }

        GlStateManager.disableLighting();
    });

    private void drawItem(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItem(mc.player, itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
    }

    private void drawBackground(int x, int y) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        DrawableHelper.blit(x, y, 0, 0, 0, width,height, height, width);
    }

    private int getX(int screenWidth) {
        switch (xAlignment.get()) {
            case Left:   return xOffset.get();
            case Center: return screenWidth / 2 - width / 2 + xOffset.get();
            case Right:  return screenWidth - width - xOffset.get();
            default:     return 0;
        }
    }

    private int getY(int screenHeight) {
        switch (yAlignment.get()) {
            case Top:    return yOffset.get();
            case Center: return screenHeight / 2 - height / 2 + yOffset.get();
            case Bottom: return screenHeight - height - yOffset.get();
            default:     return 0;
        }
    }
}
