package minegame159.meteorclient.events.entity.player;

public class ClipAtLedgeEvent {
    private static final ClipAtLedgeEvent INSTANCE = new ClipAtLedgeEvent();

    private boolean set, clip;

    public void reset() {
        set = false;
    }

    public void setClip(boolean clip) {
        set = true;
        this.clip = clip;
    }

    public boolean isSet() {
        return set;
    }
    public boolean isClip() {
        return clip;
    }

    public static ClipAtLedgeEvent get() {
        INSTANCE.reset();
        return INSTANCE;
    }
}
