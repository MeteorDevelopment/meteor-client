/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IMatrix4f;
import meteordevelopment.meteorclient.utils.misc.Vec4;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public class Matrix4fMixin implements IMatrix4f {
    @Shadow(remap = false)float m00;
    @Shadow(remap = false)float m10;
    @Shadow(remap = false)float m20;
    @Shadow(remap = false)float m30;

    @Shadow(remap = false)float m01;
    @Shadow(remap = false)float m11;
    @Shadow(remap = false)float m21;
    @Shadow(remap = false)float m31;

    @Shadow(remap = false)float m02;
    @Shadow(remap = false)float m12;
    @Shadow(remap = false)float m22;
    @Shadow(remap = false)float m32;

    @Shadow(remap = false)float m03;
    @Shadow(remap = false)float m13;
    @Shadow(remap = false) float m23;
    @Shadow(remap = false)float m33;

    @Override
    public void multiplyMatrix(Vec4 v, Vec4 out) {
        out.set(
                m00 * v.x + m01 * v.y + m02 * v.z + m03 * v.w,
                m10 * v.x + m11 * v.y + m12 * v.z + m13 * v.w,
                m20 * v.x + m21 * v.y + m22 * v.z + m23 * v.w,
                m30 * v.x + m31 * v.y + m32 * v.z + m33 * v.w
        );
    }

    @Override
    public Vec3d mul(Vec3d vec) {
        return new Vec3d(
            vec.x * m00 + vec.y * m01 + vec.z * m02 + m03,
            vec.x * m10 + vec.y * m11 + vec.z * m12 + m13,
            vec.x * m20 + vec.y * m21 + vec.z * m22 + m23
        );
    }
}
