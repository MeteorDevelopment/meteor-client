/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IMatrix4f;
import minegame159.meteorclient.utils.misc.Vec4;
import net.minecraft.util.math.Matrix4f;
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
    public void multiplyMatrix(Vec4 v, Vec4 out) {
        out.set(
                a00 * v.x + a01 * v.y + a02 * v.z + a03 * v.w,
                a10 * v.x + a11 * v.y + a12 * v.z + a13 * v.w,
                a20 * v.x + a21 * v.y + a22 * v.z + a23 * v.w,
                a30 * v.x + a31 * v.y + a32 * v.z + a33 * v.w
        );
    }
}
