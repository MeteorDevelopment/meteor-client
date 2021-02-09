package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.Utils;

public class WKeybind extends WTable {
    public Runnable action;
    public Runnable actionOnSet;

    private final WLabel label;
    private final boolean addBindText;

    private int key;
    private boolean listening;

    public WKeybind(int key, boolean addBindText) {
        this.key = key;
        this.addBindText = addBindText;

        label = add(new WLabel("")).getWidget();
        WButton set = add(new WButton("Set")).getWidget();
        WButton reset = add(new WButton("Reset")).getWidget();

        set.action = () -> {
            listening = true;
            label.setText(appendBindText("Press any key"));

            if (actionOnSet != null) actionOnSet.run();
        };

        reset.action = () -> {
            set(-1);

            if (action != null) action.run();
        };

        setLabelToKey();
    }

    public WKeybind(int key) {
        this(key, true);
    }

    public void onKey(int key) {
        if (listening) {
            set(key);

            if (action != null) action.run();
        }
    }

    public void set(int key) {
        this.key = key;
        listening = false;

        setLabelToKey();
    }

    public int get() {
        return key;
    }

    private void setLabelToKey() {
        label.setText(appendBindText(key == -1 ? "None" :  Utils.getKeyName(key)));
    }

    private String appendBindText(String text) {
        return addBindText ? "Bind: " + text : text;
    }
}
