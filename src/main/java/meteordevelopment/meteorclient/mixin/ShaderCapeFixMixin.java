package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "meteordevelopment.meteorclient.utils.render.CustomOutlineVertexConsumerProvider", remap = false)
public abstract class ShaderCapeFixMixin {

    /**
     * method_22991 is the intermediary name for getBuffer.
     * We target both to ensure compatibility with your environment.
     */
    @Inject(method = {"getBuffer", "method_22991"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetBuffer(RenderLayer layer, CallbackInfoReturnable<VertexConsumer> cir) {
        if (layer != null && layer.toString().toLowerCase().contains("cape")) {
            try {
                // We use Reflection to find the 'inner' provider field inside Meteor Client.
                // This prevents the "Shadow field not located" crash.
                for (Field field : this.getClass().getDeclaredFields()) {
                    if (VertexConsumerProvider.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        VertexConsumerProvider innerProvider = (VertexConsumerProvider) field.get(this);
                        
                        if (innerProvider != null) {
                            // Force the cape to render using the original world buffer 
                            // instead of the broken translucent ESP shader.
                            cir.setReturnValue(innerProvider.getBuffer(layer));
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
                // If reflection fails, the cape stays invisible or ghosted, 
                // but the game won't crash.
            }
        }
    }
}