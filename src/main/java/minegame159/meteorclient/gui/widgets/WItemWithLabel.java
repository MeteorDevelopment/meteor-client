package minegame159.meteorclient.gui.widgets;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;

import java.util.List;

public class WItemWithLabel extends WHorizontalList {
    private ItemStack itemStack;
    private WItem item;
    private WLabel label;

    public WItemWithLabel(ItemStack itemStack) {
        super(4);

        this.itemStack = itemStack;
        item = add(new WItem(itemStack));
        label = add(new WLabel(itemStack.getName().asString() + getStringToAppend()));
    }

    private String getStringToAppend() {
        String str = "";
        if (itemStack.getItem() == Items.POTION) {
            List<StatusEffectInstance> effects = PotionUtil.getPotion(itemStack).getEffects();
            if (effects.size() > 0) {
                str += " ";
                StatusEffectInstance effect = effects.get(0);
                if (effect.getAmplifier() > 0) str += effect.getAmplifier() + 1 + " ";
                str += "(" + StatusEffectUtil.durationToString(effect, 1) + ")";
            }
        }
        return str;
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
        item.itemStack = itemStack;
        label.text = itemStack.getName().asString() + getStringToAppend();
    }

    public String getLabelText() {
        return label.text;
    }
}
