package minegame159.meteorclient.mixininterface;

import net.minecraft.util.math.Vec3d;

public interface IVec3d {
    void set(double x, double y, double z);

    default void set(Vec3d vec) {
        set(vec.x, vec.y, vec.z);
    }

    void setY(double y);
}
