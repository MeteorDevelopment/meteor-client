package meteordevelopment.meteorclient.utils.render;

/**
 * Animation helpers driven by real time. Game tick deltas are tiny per frame and stop
 * entirely while singleplayer is paused behind a screen, which made UI animations crawl.
 */
public final class EmberAnim {
    private EmberAnim() {
    }

    /**
     * Eases toward the target, independent of frame rate.
     *
     * @param tau time constant in seconds; the value is ~95% of the way after 3 * tau
     */
    public static float approach(float current, float target, double dtSeconds, double tau) {
        if (tau <= 0) return target;

        float next = current + (target - current) * (float) (1 - Math.exp(-dtSeconds / tau));
        return Math.abs(next - target) < 0.002f ? target : next;
    }

    /** Seconds between frames, capped so a hitch doesn't jump an animation to its end. */
    public static final class Clock {
        private long last = -1;

        public float tick() {
            long now = System.nanoTime();
            float dt = last < 0 ? 0f : (now - last) / 1_000_000_000f;
            last = now;
            return Math.min(dt, 0.1f);
        }
    }
}
