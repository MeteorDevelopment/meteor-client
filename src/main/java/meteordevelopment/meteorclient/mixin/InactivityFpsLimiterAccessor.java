package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.option.InactivityFpsLimiter;

@Mixin(InactivityFpsLimiter.class)
public interface InactivityFpsLimiterAccessor {
    @Accessor("lastInputTime")
    long lastInputTime();
}
