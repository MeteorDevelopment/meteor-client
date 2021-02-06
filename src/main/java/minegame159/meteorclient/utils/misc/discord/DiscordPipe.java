package minegame159.meteorclient.utils.misc.discord;

import org.scalasbt.ipcsocket.UnixDomainSocket;
import org.scalasbt.ipcsocket.Win32NamedPipeSocket;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class DiscordPipe {
    private static final String[] UNIX_PATHS = { "XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP" };

    public static Socket create() {
        Socket pipe = null;

        if (System.getProperty("os.name").contains("Win")) {
            for (int i = 0; i < 10; i++) {
                try {
                    pipe = new Win32NamedPipeSocket("\\\\?\\pipe\\discord-ipc-" + i, true);
                    break;
                } catch (IOException ignored) {}
            }
        }
        else {
            for (int i = 0; i < 10; i++) {
                String tmpPath = null;

                for (String str : UNIX_PATHS) {
                    tmpPath = System.getenv(str);
                    if (tmpPath != null) break;
                }

                if (tmpPath == null) tmpPath = "/tmp";
                File file = new File(tmpPath + "/discord-ipc-" + i);

                try {
                    if (file.exists()) {
                        pipe = new UnixDomainSocket(tmpPath + "/discord-ipc-" + i, true);
                        break;
                    }
                } catch (IOException ignored) {}
            }
        }

        if (pipe == null) throw new RuntimeException("Failed to find Discord pipe.");
        return pipe;
    }
}
