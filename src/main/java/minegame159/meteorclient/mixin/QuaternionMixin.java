package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IQuaternion;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Quaternion.class)
public class QuaternionMixin implements IQuaternion {
    @Shadow private float w;
    @Shadow private float x;
    @Shadow private float y;
    @Shadow private float z;

    @Override
    public void toScreen() {
        float newW = 1f / w * 0.5f;

        x = x * newW + 0.5f;
        y = y * newW + 0.5f;
        z = z * newW + 0.5f;
        w = newW;
    }
}
