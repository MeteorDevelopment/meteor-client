package minegame159.meteorclient.mixin;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignEditScreen.class)
public interface SignEditScreenAccessor {
    @Accessor("sign")
    SignBlockEntity getSign();
}
