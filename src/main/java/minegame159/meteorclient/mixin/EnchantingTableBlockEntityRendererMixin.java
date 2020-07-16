package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.render.block.entity.EnchantingTableBlockEntityRenderer;
import net.minecraft.client.render.entity.model.BookModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantingTableBlockEntityRenderer.class)
public class EnchantingTableBlockEntityRendererMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BookModel;render(FFFFFF)V"))
    private void onRenderBookModelRenderProxy(BookModel model, float ticks, float leftPageAngle, float rightPageAngle, float pageTurningSpeed, float f, float g) {
        if (!ModuleManager.INSTANCE.get(NoRender.class).noEnchTableBook()) model.render(ticks, leftPageAngle, rightPageAngle, pageTurningSpeed, f, g);
    }
}
