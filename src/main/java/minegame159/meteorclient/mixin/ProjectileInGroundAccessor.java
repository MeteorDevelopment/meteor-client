package minegame159.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.projectile.PersistentProjectileEntity;

@Mixin(PersistentProjectileEntity.class)
public interface ProjectileInGroundAccessor {
    @Accessor("inGround")
    public boolean getInGround();
}
