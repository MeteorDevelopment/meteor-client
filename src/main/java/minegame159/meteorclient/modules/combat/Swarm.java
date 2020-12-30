package minegame159.meteorclient.modules.combat;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.player.InfinityMiner;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.MeteorExecutor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * @author Inclemental
 * Special thanks to Eli for lending me the test account. Love you bud.
 */

public class Swarm extends ToggleModule {
    public Swarm() {
        super(Category.Combat, "Swarm", "I Am... The Swarm.");
    }

    public enum Mode {
        QUEEN,
        SLAVE,
        IDLE
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> ipAddress = sgGeneral.add(new StringSetting.Builder()
            .name("ip-address")
            .description("Ip address of the Queen.")
            .defaultValue("localhost")
            .build());

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
            .name("Port")
            .description("The port used for connections.")
            .defaultValue(7777)
            .sliderMin(1)
            .sliderMax(65535)
            .build());

    public SwarmServer server;
    public SwarmClient client;
    public BlockState targetBlock;
    public Mode currentMode = Mode.IDLE;
    private WLabel label;

    @Override
    public void onActivate() {
        currentMode = Mode.IDLE;
        closeAllServerConnections();
    }

    @Override
    public void onDeactivate() {
        currentMode = Mode.IDLE;
        closeAllServerConnections();
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();
        label = new WLabel("");
        table.add(label);
        setLabel();
        table.row();
        WTable table2 = new WTable();
        WButton runServer = new WButton("Run Server(Q)");
        runServer.action = this::runServer;
        table2.add(runServer);
        WButton connect = new WButton("Connect(S)");
        connect.action = this::runClient;
        table2.add(connect);
        WButton reset = new WButton("Reset");
        reset.action = () -> {
            Chat.info("Swarm: Closing all connections.");
            closeAllServerConnections();
            currentMode = Mode.IDLE;
            setLabel();
        };
        table2.add(reset);
        table.add(table2);
        table.row();
        WButton guide = new WButton("Guide");
        guide.action = () -> {
            MinecraftClient.getInstance().openScreen(new SwarmHelpScreen());
        };
        table.add(guide);
        table.row();
        return table;
    }

    public void runServer() {
        if (server == null) {
            currentMode = Mode.QUEEN;
            setLabel();
            closeAllServerConnections();
            server = new SwarmServer();
        }
    }

    public void runClient() {
        if (client == null) {
            currentMode = Mode.SLAVE;
            setLabel();
            closeAllServerConnections();
            client = new SwarmClient();
        }
    }

    public void closeAllServerConnections() {
        try {
            if (server != null) {
                server.interrupt();
                server.close();
                server.serverSocket.close();
                server = null;
            }
            if (client != null) {
                client.interrupt();
                client.disconnect();
                client.socket.close();
                client = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void setLabel() {
        if (currentMode != null)
            label.setText("Current Mode: " + currentMode);
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (targetBlock != null)
            mine();
    });

    public void idle() {
        currentMode = Mode.IDLE;
        if (ModuleManager.INSTANCE.get(InfinityMiner.class).isActive())
            ModuleManager.INSTANCE.get(InfinityMiner.class).toggle();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public void mine() {
        Chat.info("Swarm: Starting Mining Job.");
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(targetBlock.getBlock());
        targetBlock = null;

    }

    public class SwarmClient extends Thread {

        public Socket socket;
        public String ipAddress;

        SwarmClient() {
            ipAddress = Swarm.this.ipAddress.get();
            start();
        }

        @Override
        public void run() {
            InputStream inputStream;
            DataInputStream dataInputStream;
            try {
                while (socket == null && !isInterrupted()) {
                    try {
                        socket = new Socket(ipAddress, serverPort.get());
                    } catch (Exception ignored) {
                        Chat.info("Server not found. Retrying in 5 seconds.");
                    }
                    if (socket == null) {
                        Thread.sleep(5000);
                    }
                }
                if (socket != null) {
                    inputStream = socket.getInputStream();
                    dataInputStream = new DataInputStream(inputStream);
                    Chat.info("New Socket");
                    while (!isInterrupted()) {
                        if (socket != null) {
                            String read;
                            read = dataInputStream.readUTF();
                            if (!read.equals("")) {
                                Chat.info("New Command: " + read);
                                execute(read);
                            }
                        }
                    }
                    dataInputStream.close();
                    inputStream.close();
                }
            } catch (Exception e) {
                Chat.error("There is in error in your connection to the server.");
                disconnect();
                client = null;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        Chat.error("There is in error in your connection to the server.");
                    }
                }
            }
        }

        public void disconnect() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public class SwarmServer extends Thread {
        private ServerSocket serverSocket;
        public final static int MAX_CLIENTS = 50;
        final private SubServer[] clientConnections = new SubServer[MAX_CLIENTS];

        public SwarmServer() {
            try {
                int port = serverPort.get();
                this.serverSocket = new ServerSocket(port);
                Chat.info("Swarm Server: New Server Opened On Port " + serverPort.get());
                start();
            } catch (Exception ignored) {

            }
        }

        @Override
        public void run() {
            try {
                Chat.info("Swarm Server: Listening for incoming connections.");
                while (!this.isInterrupted()) {
                    Socket connection = this.serverSocket.accept();
                    assignConnectionToSubServer(connection);
                }
            } catch (Exception ignored) {
            }
        }

        public void assignConnectionToSubServer(Socket connection) {
            for (int i = 0; i < clientConnections.length; i++) {
                if (this.clientConnections[i] == null) {
                    this.clientConnections[i] = new SubServer(connection);
                    Chat.info("Swarm Server: New Slave Connected.");
                    break;
                }
            }
        }

        public void close() {
            try {
                interrupt();
                for (SubServer clientConnection : clientConnections) {
                    if (clientConnection != null) {
                        clientConnection.close();
                    }
                }
                serverSocket.close();
            } catch (Exception e) {
                Chat.info("Server closed.");
            }
        }

        public void closeAllClients() {
            try {
                for (SubServer s : clientConnections) {
                    if (s.connection != null)
                        s.close();
                }
            } catch (Exception e) {
                closeAllServerConnections();
            }
        }

        public synchronized void sendMessage(@Nonnull String s) {
            MeteorExecutor.execute(() -> {
                try {
                    for (SubServer clientConnection : clientConnections) {
                        if (clientConnection != null) {
                            clientConnection.messageToSend = s;
                        }
                    }
                } catch (Exception ignored) {
                }
            });

        }

    }

    public static class SubServer extends Thread {
        final private Socket connection;
        private volatile String messageToSend;

        public SubServer(@Nonnull Socket connection) {
            this.connection = connection;
            start();
        }

        @Override
        public void run() {
            OutputStream outputStream;
            DataOutputStream dataOutputStream;
            try {
                outputStream = connection.getOutputStream();
                dataOutputStream = new DataOutputStream(outputStream);
                while (!this.isInterrupted()) {
                    if (messageToSend != null) {
                        dataOutputStream.writeUTF(messageToSend);
                        dataOutputStream.flush();
                        messageToSend = null;
                    }
                }
                outputStream.close();
                dataOutputStream.close();
            } catch (Exception e) {
                Chat.info("Error in subsystem.");
            }
        }

        public void close() {
            try {
                interrupt();
                this.connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void execute(@Nonnull String s) {
        try {
            CommandManager.dispatch(s);
        } catch (CommandSyntaxException ignored) {
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<GameLeftEvent> gameLeftEventListener = new Listener<>(event -> {
        closeAllServerConnections();
        this.toggle();
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<GameJoinedEvent> gameJoinedEventListener = new Listener<>(event -> {
        closeAllServerConnections();
        this.toggle();
    });


    enum currentScreen {
        INTRO,
        CONFIG,
        QUEEN,
        SLAVE
    }

    private class SwarmHelpScreen extends WindowScreen {

        private currentScreen currentScreen = Swarm.currentScreen.CONFIG;
        private final WLabel textBox;
        private final WButton introButton;
        private final WButton ipConfigButton;
        private final WButton queenButton;
        private final WButton slaveButton;

        public SwarmHelpScreen() {
            super("Swarm Help", true);
            textBox = new WLabel(swarmGuideIntro);
            textBox.shadow = true;
            textBox.maxWidth = textBox.maxWidth * .75;
            introButton = new WButton("(1)Introduction");
            introButton.action = () -> {
                currentScreen = Swarm.currentScreen.INTRO;
                initWidgets();
            };
            ipConfigButton = new WButton("(2)Configuration");
            ipConfigButton.action = () -> {
                currentScreen = Swarm.currentScreen.CONFIG;
                initWidgets();
            };
            queenButton = new WButton("(3)Queen");
            queenButton.action = () -> {
                currentScreen = Swarm.currentScreen.QUEEN;
                initWidgets();
            };
            slaveButton = new WButton("(4)Slave");
            slaveButton.action = () -> {
                currentScreen = Swarm.currentScreen.SLAVE;
                initWidgets();
            };

            initWidgets();
        }

        private void initWidgets() {
            clear();
            WTable table = new WTable();
            table.add(introButton);
            table.add(ipConfigButton);
            table.add(queenButton);
            table.add(slaveButton);
            add(table);
            row();
            add(textBox).getWidget();
            row();
        }


    }

    private final static String swarmGuideIntro = "Swarm at its heart is a command tunnel which allows a controlling account, referred to" +
            " as the queen account, to control other accounts by means of a background server. By default, it" +
            " is configured to work with multiple instances of Minecraft running on the same computer," +
            " however with some additional configuration it will work across your local network. For" +
            " functionality over the broader internet you will need to perform a port forward for your queen" +
            " instance.  All swarm commands are proceded by \\“.s\\”. ";
    private final static String swarmGuideConfig = "Bacon";
    private final static String swarmGuideQueen = "";
    private final static String swarmGuideSlave = "";

}