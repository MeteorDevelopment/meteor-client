package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IMatrix4f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public class Matrix4fMixin implements IMatrix4f {
    @Shadow protected float a00;
    @Shadow protected float a10;
    @Shadow protected float a20;
    @Shadow protected float a30;

    @Shadow protected float a01;
    @Shadow protected float a11;
    @Shadow protected float a21;
    @Shadow protected float a31;

    @Shadow protected float a02;
    @Shadow protected float a12;
    @Shadow protected float a22;
    @Shadow protected float a32;

    @Shadow protected float a03;
    @Shadow protected float a13;
    @Shadow protected float a23;
    @Shadow protected float a33;

    @Override
    public Quaternion multiplyMatrix(Quaternion q) {
        return new Quaternion(
                a00 * q.getX() + a01 * q.getY() + a02 * q.getZ() + a03 * q.getW(),
                a10 * q.getX() + a11 * q.getY() + a12 * q.getZ() + a13 * q.getW(),
                a20 * q.getX() + a21 * q.getY() + a22 * q.getZ() + a23 * q.getW(),
                a30 * q.getX() + a31 * q.getY() + a32 * q.getZ() + a33 * q.getW()
        );
    }
}
