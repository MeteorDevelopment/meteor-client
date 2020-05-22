package minegame159.meteorclient.mixin;

import baritone.api.utils.Rotation;
import baritone.behavior.LookBehavior;
import minegame159.meteorclient.mixininterface.ILookBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LookBehavior.class)
public class LookBehaviorMixin implements ILookBehavior {
    @Shadow private Rotation target;

    @Override
    public Rotation getTarget() {
        return target;
    }
}
