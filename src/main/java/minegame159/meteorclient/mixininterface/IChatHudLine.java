package minegame159.meteorclient.mixininterface;

import net.minecraft.text.Text;

public interface IChatHudLine {
    public void setText(Text text);

    public void setTimestamp(int timestamp);

    public void setId(int id);
}
