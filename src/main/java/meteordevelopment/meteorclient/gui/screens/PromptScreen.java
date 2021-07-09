package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

import static meteordevelopment.meteorclient.utils.Utils.mc;

import net.minecraft.client.gui.screen.Screen;

public class PromptScreen extends WindowScreen {

    public PromptScreen(GuiTheme theme, String title, String message, Runnable onYes, Runnable onNo) {
        this(theme, title, message, onYes, onNo, mc.currentScreen);
    }

    public PromptScreen(GuiTheme theme, String title, String message, Runnable onYes, Runnable onNo, Screen parent) {
        super(theme, title);
        this.parent = parent;

        for (String line : message.split("\n")) {
            add(theme.label(line)).expandCellX();
        }
        
        
        WHorizontalList list = add(theme.horizontalList()).expandCellX().widget();

        WButton yesButton = list.add(theme.button("Yes")).widget();
        yesButton.action = () -> {
            onYes.run();
            this.onClose();
        };

        WButton noButton = list.add(theme.button("No")).widget();
        noButton.action = () -> {
            onNo.run();
            this.onClose();
        };
    }
    
}
