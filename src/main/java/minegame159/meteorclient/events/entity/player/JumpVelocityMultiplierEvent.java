package minegame159.meteorclient.events.entity.player;

public class JumpVelocityMultiplierEvent {
    private static final JumpVelocityMultiplierEvent INSTANCE = new JumpVelocityMultiplierEvent();

    public float multiplier = 1;

    public static JumpVelocityMultiplierEvent get() {
        INSTANCE.multiplier = 1;
        return INSTANCE;
    }
}
