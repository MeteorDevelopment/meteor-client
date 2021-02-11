package minegame159.meteorclient.mixin;

import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractButtonWidget.class)
public interface AbstractButtonWidgetAccessor {
    @Accessor("message")
    void setText(Text message);
}
