package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TotemsHud extends HudModule {
    public TotemsHud(HUD hud) { super(hud, "totems", "Displays the amount of totems in your inventory."); }



    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * hud.armorScale(), 16 * hud.armorScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if(mc.player == null || mc.currentScreen instanceof HudEditorScreen) {

            RenderUtils.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), (int) (x / hud.armorScale()), (int) (y / hud.armorScale()), hud.armorScale(), true);

        } else if(InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING).count > 0) {

            RenderUtils.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING, InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING).count), (int) (x / hud.armorScale()), (int) (y / hud.armorScale()), hud.armorScale(), true);

        }
    }
}
