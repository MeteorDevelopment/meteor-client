package minegame159.meteorclient.mixininterface;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public interface IDisconnectedScreen {
    Screen getParent();

    Text getReason();

    int getReasonHeight();
}
