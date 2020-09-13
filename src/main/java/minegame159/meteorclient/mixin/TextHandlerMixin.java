package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.ITextHandler;
import net.minecraft.client.font.TextHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextHandler.class)
public class TextHandlerMixin implements ITextHandler {
    @Shadow @Final private TextHandler.WidthRetriever widthRetriever;

    @Override
    public TextHandler.WidthRetriever getWidthRetriever() {
        return widthRetriever;
    }
}
