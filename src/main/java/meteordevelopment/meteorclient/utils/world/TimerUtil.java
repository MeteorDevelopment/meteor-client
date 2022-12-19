/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

public class TimerUtil {
    long time;

    public TimerUtil() {
        time = System.currentTimeMillis();
    }

    public void reset() {
        time = System.currentTimeMillis();
    }


    // Get times
    public long passedMillis() {
        return System.currentTimeMillis() - time;
    }

    public long passedTicks() {
        long remainder = passedMillis() % 50;
        return (passedMillis() / 50) + ((remainder > 25) ? 1 : 0);
    }

    public boolean hasPassedMillis(long time) {
        return passedMillis() > time;
    }

    public boolean hasPassedTicks(long time) {
        return passedTicks() > time;
    }
}
