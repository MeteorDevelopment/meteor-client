package meteordevelopment.meteorclient.utils.skyblock.terminal;

import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.TerminalHandler;

public class TerminalEvent {
    private static final Open OPEN = new Open();
    private static final Close CLOSE = new Close();
    private static Solve SOLVE;

    public static class Open {
        private static final Open INSTANCE = new Open();

        public TerminalHandler terminal;

        public static Open get(TerminalHandler terminal) {
            INSTANCE.terminal = terminal;
            return INSTANCE;
        }
    }

    public static class Close {
        private static final Close INSTANCE = new Close();

        public TerminalHandler terminal;

        public static Close get(TerminalHandler terminal) {
            INSTANCE.terminal = terminal;
            return INSTANCE;
        }
    }

    public static class Solve {
        public TerminalHandler terminal;

        public static Solve get(TerminalHandler terminal) {
            if (SOLVE == null) SOLVE = new Solve();
            SOLVE.terminal = terminal;
            return SOLVE;
        }
    }
}
