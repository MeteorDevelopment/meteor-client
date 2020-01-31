package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IAbstractButtonWidget;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractButtonWidget.class)
public class AbstractButtonWidgetMixin implements IAbstractButtonWidget {
    @Shadow private String message;

    @Override
    public void setText(String text) {
        message = text;
    }
}
