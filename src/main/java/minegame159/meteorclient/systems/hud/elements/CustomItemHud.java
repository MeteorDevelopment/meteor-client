package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.systems.hud.*;
import minegame159.meteorclient.utils.misc.Names;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@ElementRegister(name = "item", category = Category.Custom)
public class CustomItemHud extends ScaleableHudElement {

    private Item item;

    public CustomItemHud(Item item) {
        super(Names.get(item), String.format("A counter of all %s items.", Names.get(item)));

        this.item = item;
    }


    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * scale.get(), 16 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (isInEditor()) {
            RenderUtils.drawItem(item.getDefaultStack(), (int) x, (int) y, scale.get(), true);
        } else if (InvUtils.findItemWithCount(item).count > 0) {
            RenderUtils.drawItem(new ItemStack(item, InvUtils.findItemWithCount(item).count), (int) x, (int) y, scale.get(), true);
        }
    }

    public CustomItemHud() {
        super("nothing", "nothing");
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.put("item", StringTag.of(Registry.ITEM.getId(item).toString()));
        return tag;
    }

    @Override
    public HudElement fromTag(CompoundTag tag) {
        item = Registry.ITEM.get(new Identifier(tag.getString("item")));
        name = Names.get(item);
        description = String.format("A counter of all %s items.", name);
        return super.fromTag(tag);
    }
}
