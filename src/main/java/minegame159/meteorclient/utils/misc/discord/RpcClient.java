package minegame159.meteorclient.utils.misc.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import minegame159.meteorclient.MeteorClient;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RpcClient {
    private static final JsonParser JSON = new JsonParser();

    private Socket pipe;
    private Thread thread;

    private final Runnable onReady;

    public RpcClient(Runnable onReady) {
        this.onReady = onReady;
    }

    public void connect(long clientId) {
        pipe = DiscordPipe.create();

        thread = new Thread(() -> {
            try {
                run();
            } catch (InterruptedException | NullPointerException ignored) {}
        });
        thread.start();

        write(OpCode.Handshake, String.format("{\"v\":1,\"client_id\":\"%d\",\"nonce\":\"%s\"}", clientId, nonce()));
    }

    public void send(RichPresence richPresence) {
        String msg = String.format("{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":%d,\"activity\":%s},\"nonce\":\"%s\"", pid(), richPresence.getJson(), nonce());
        write(OpCode.Frame, msg);
    }

    @SuppressWarnings("all")
    private void run() throws InterruptedException {
        while (true) {
            try {
                // Read op code
                byte[] b = new byte[4];
                int read = 0;
                while (read < 4) {
                    int r = pipe.getInputStream().read(b, read, 4 - read);

                    while (r == -1) {
                        Thread.sleep(50);
                        r = pipe.getInputStream().read(b, read, 4 - read);
                    }

                    read += r;
                    if (read < 4) Thread.sleep(50);
                }

                OpCode opCode = OpCode.values()[Integer.reverseBytes((b[0] << 24) | (b[1] << 16) + (b[2] << 8) + b[3])];

                // Read message length
                read = 0;
                while (read < 4) {
                    int r = pipe.getInputStream().read(b, read, 4 - read);

                    while (r == -1) {
                        Thread.sleep(50);
                        r = pipe.getInputStream().read(b, read, 4 - read);
                    }

                    read += r;
                    if (read < 4) Thread.sleep(50);
                }

                int length = Integer.reverseBytes((b[0] << 24) | (b[1] << 16) + (b[2] << 8) + b[3]);

                // Read message
                b = new byte[length];
                read = 0;
                while (read < length) {
                    int r = pipe.getInputStream().read(b, read, length - read);

                    while (r == -1) {
                        Thread.sleep(50);
                        r = pipe.getInputStream().read(b, read, 4 - read);
                    }

                    read += r;
                    if (read < 4) Thread.sleep(50);
                }

                if (opCode == OpCode.Ping) {
                    write(OpCode.Pong, b);
                }
                else if (opCode == OpCode.Close) {
                    JsonObject json = JSON.parse(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();

                    int code = json.get("code").getAsInt();
                    String msg = json.get("message").getAsString();

                    MeteorClient.LOG.error("Discord IPC error code " + code + " with message '" + msg + "'");
                }
                else if (opCode == OpCode.Frame) {
                    JsonObject json = JSON.parse(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (json.has("evt") && json.get("evt").getAsString().equals("READY")) onReady.run();
                }
                else {
                    MeteorClient.LOG.warn("Discord IPC received message with rong op code.");
                }

                Thread.sleep(500);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        try {
            write(OpCode.Close, String.format("{\"nonce\":\"%s\"}", nonce()));

            pipe.close();
            pipe = null;

            thread.interrupt();
            thread.join();
            thread = null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void write(OpCode opCode, byte[] msgBytes) {
        ByteBuffer bytes = ByteBuffer.allocate(msgBytes.length + 2 * 4);
        bytes.putInt(Integer.reverseBytes(opCode.ordinal()));
        bytes.putInt(Integer.reverseBytes(msgBytes.length));
        bytes.put(msgBytes);

        try {
            pipe.getOutputStream().write(bytes.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(OpCode opCode, String msg) {
        write(opCode, msg.getBytes(StandardCharsets.UTF_8));
    }

    private static int pid() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }

    private static String nonce() {
        return UUID.randomUUID().toString();
    }

    private enum OpCode {
        Handshake,
        Frame,
        Close,
        Ping,
        Pong
    }
}
