package minegame159.meteorclient.systems.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;

import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class ContainerViewerHud extends HudElement {

    private static final Identifier TEXTURE = new Identifier("meteor-client", "textures/container.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of inventory viewer.")
        .defaultValue(3)
        .min(0.1)
        .sliderMin(0.1)
        .max(10)
        .build()
    );

    private final Setting<Boolean> echestNoItem = sgGeneral.add(new BoolSetting.Builder()
        .name("echest-when-empty")
        .description("Display contents of ender chest if not holding any other container")
        .defaultValue(false)
        .build()
    );

    private final ItemStack[] inventory = new ItemStack[9 * 3];

    public ContainerViewerHud(HUD hud) {
        super(hud, "container-viewer", "Displays containers.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(176 * scale.get(), 67 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        ItemStack container = getContainer();

        if (container == null) return;

        drawBackground((int) x, (int) y, container);

        Utils.getItemsInContainerItem(container, inventory);

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory[row * 9 + i];
                if (stack == null) continue;

                RenderUtils.drawItem(stack, (int) (x + (8 + i * 18) * scale.get()), (int) (y + (7 + row * 18) * scale.get()), scale.get(), true);
            }
        }
    }

    private ItemStack getContainer() {
        if (isInEditor()) return Items.ENDER_CHEST.getDefaultStack();

        ItemStack item = mc.player.inventory.getMainHandStack();
        if (!(item.getItem() instanceof BlockItem)) item = mc.player.getOffHandStack();
        if (item.getItem() == Items.ENDER_CHEST) return item;
        if (!(item.getItem() instanceof BlockItem)) return echestNoItem.get()?Items.ENDER_CHEST.getDefaultStack():null;

        if (((BlockItem)item.getItem()).getBlock() instanceof ShulkerBoxBlock) return item;
        if (((BlockItem)item.getItem()).getBlock() instanceof AbstractChestBlock) return item;
        return echestNoItem.get()?Items.ENDER_CHEST.getDefaultStack():null;
    }

    private void drawBackground(int x, int y, ItemStack container) {
        int w = (int) box.width;
        int h = (int) box.height;

        Color color = Utils.getShulkerColor(container);

        RenderSystem.color4f(color.r / 255F, color.g / 255F, color.b / 255F, color.a / 255F);
        mc.getTextureManager().bindTexture(TEXTURE);
        DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, w, h, h, w);
    }


}