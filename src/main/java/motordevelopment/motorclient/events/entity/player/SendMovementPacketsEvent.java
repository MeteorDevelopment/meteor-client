/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity.player;

public class SendMovementPacketsEvent {
    public static class Pre {
        private static final Pre INSTANCE = new Pre();

        public static SendMovementPacketsEvent.Pre get() {
            return INSTANCE;
        }
    }

    public static class Post {
        private static final Post INSTANCE = new Post();

        public static SendMovementPacketsEvent.Post get() {
            return INSTANCE;
        }
    }
}
