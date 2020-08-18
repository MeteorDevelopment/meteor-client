package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import net.minecraft.client.gl.JsonGlProgram;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(JsonGlProgram.class)
public class JsonGlProgramMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/util/Identifier"))
    private static Identifier onInit(String string) {
        if (MeteorClient.loadingOutlineShader && string.equals("shaders/program/my_entity_outline.json")) {
            return new Identifier("meteor-client", string);
        }

        return new Identifier(string);
    }

    @Redirect(method = "getShader", at = @At(value = "NEW", target = "net/minecraft/util/Identifier"))
    private static Identifier onGetShader(String string) {
        if (MeteorClient.loadingOutlineShader && string.equals("shaders/program/my_entity_sobel.fsh")) {
            return new Identifier("meteor-client", string);
        }

        return new Identifier(string);
    }
}
