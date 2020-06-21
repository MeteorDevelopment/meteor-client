package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBakedQuad;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuad.class)
public class BakedQuadMixin implements IBakedQuad {
    @Shadow @Final protected Sprite sprite;

    @Override
    public Sprite getSprite() {
        return sprite;
    }
}
