package minegame159.meteorclient.mixininterface;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public interface IDisconnectedScreen {
    public Screen getParent();

    public Text getReason();

    public int getReasonHeight();
}
