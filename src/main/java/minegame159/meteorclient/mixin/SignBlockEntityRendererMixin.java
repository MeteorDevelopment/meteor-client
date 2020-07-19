package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.text.StringRenderable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.UnaryOperator;

@Mixin(SignBlockEntityRenderer.class)
public class SignBlockEntityRendererMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;getTextBeingEditedOnRow(ILjava/util/function/UnaryOperator;)Lnet/minecraft/text/StringRenderable;"))
    private StringRenderable onRenderSignBlockEntityGetTextBeingEditedOnRowProxy(SignBlockEntity sign, int row, UnaryOperator<StringRenderable> unaryOperator) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noSignText()) return null;
        return sign.getTextBeingEditedOnRow(row, unaryOperator);
    }
}
