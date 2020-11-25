package minegame159.meteorclient.mixin;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Identifier.class)
public interface IdentifierAccessor {
    @Accessor
    void setPath(String path);
}
