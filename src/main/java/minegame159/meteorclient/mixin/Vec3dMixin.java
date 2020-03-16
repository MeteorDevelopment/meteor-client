package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IVec3d;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3d.class)
public class Vec3dMixin implements IVec3d {
    @Shadow public double x;

    @Shadow public double y;

    @Shadow public double z;

    @Override
    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
