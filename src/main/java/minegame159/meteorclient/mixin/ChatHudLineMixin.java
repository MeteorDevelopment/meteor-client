package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatHudLine.class)
public class ChatHudLineMixin implements IChatHudLine {
    @Shadow private Text text;

    @Shadow private int creationTick;

    @Shadow private int id;

    @Override
    public void setText(Text text) {
        this.text = text;
    }

    @Override
    public void setTimestamp(int timestamp) {
        this.creationTick = timestamp;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
}
