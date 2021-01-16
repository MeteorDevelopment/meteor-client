package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemCountHud extends HudModule {

    private Item itemCount;
    public ItemCountHud(HUD hud, Item item) {
        super(hud, "active-modules", "Displays your active modules.");
        this.itemCount = item;
    }


    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * hud.armorScale(), 16 * hud.armorScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if(mc.player == null || mc.currentScreen instanceof HudEditorScreen) {

            RenderUtils.drawItem(itemCount.getDefaultStack(), (int) (x / hud.armorScale()), (int) (y / hud.armorScale()), hud.armorScale(), true);

        } else if(InvUtils.findItemWithCount(itemCount).count > 0) {

            RenderUtils.drawItem(new ItemStack(itemCount, InvUtils.findItemWithCount(itemCount).count), (int) (x / hud.armorScale()), (int) (y / hud.armorScale()), hud.armorScale(), true);

        }
    }
}
