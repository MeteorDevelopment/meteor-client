package minegame159.meteorclient.gui.listeners;

import minegame159.meteorclient.gui.widgets.WEnumButton;

public interface EnumButtonClickListener<T extends Enum<?>> {
    public void onEnumButtonClick(WEnumButton<T> enumButton);
}
