package minegame159.meteorclient.mixininterface;

import net.minecraft.util.math.Quaternion;

public interface IMatrix4f {
    Quaternion multiplyMatrix(Quaternion q);
}
