/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.commands.Commands;
import minegame159.meteorclient.commands.commands.SwarmCommand;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.containers.WVerticalList;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.world.InfinityMiner;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.BlockState;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static minegame159.meteorclient.utils.Utils.getWindowWidth;

/**
 * @author Inclemental
 * Special thanks to Eli for lending me the test account. Love you bud.
 */

public class Swarm extends Module {
    public enum Mode {
        Queen,
        Slave,
        Idle
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> ipAddress = sgGeneral.add(new StringSetting.Builder()
            .name("iP-address")
            .description("The IP address of the Queen.")
            .defaultValue("localhost")
            .build());

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
            .name("port")
            .description("The port used for connections.")
            .defaultValue(7777)
            .sliderMin(1)
            .sliderMax(65535)
            .build());

    public Swarm() {
        super(Categories.Misc, "Swarm", "I Am... The Swarm.");
    }

    public SwarmServer server;
    public SwarmClient client;
    public BlockState targetBlock;
    public Mode currentMode = Mode.Idle;
    private WLabel label;

    @Override
    public void onActivate() {
        currentMode = Mode.Idle;
        closeAllServerConnections();
    }

    @Override
    public void onDeactivate() {
        currentMode = Mode.Idle;
        closeAllServerConnections();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        // Status
        label = list.add(theme.label("")).widget();
        setLabel();

        // Buttons
        WHorizontalList b = list.add(theme.horizontalList()).expandX().widget();

        //   Run Server
        WButton runServer = b.add(theme.button("Run Server (Q)")).expandX().widget();
        runServer.action = this::runServer;

        //   Connect
        WButton connect = b.add(theme.button("Connect (S)")).expandX().widget();
        connect.action = this::runClient;

        //   Reset
        WButton reset = b.add(theme.button("Rest")).expandX().widget();
        reset.action = () -> {
            ChatUtils.moduleInfo(this, "Closing all connections.");
            closeAllServerConnections();
            currentMode = Mode.Idle;
            setLabel();
        };

        // Guide
        WButton guide = list.add(theme.button("Guide")).expandX().widget();
        guide.action = () -> mc.openScreen(new SwarmHelpScreen(theme));

        return list;
    }

    public void runServer() {
        if (server == null) {
            currentMode = Mode.Queen;
            setLabel();
            closeAllServerConnections();
            server = new SwarmServer();
        }
    }

    public void runClient() {
        if (client == null) {
            currentMode = Mode.Slave;
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
        if (currentMode != null) label.set("Current Mode: " + currentMode);
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (targetBlock != null)
            mine();
    }

    public void idle() {
        currentMode = Mode.Idle;
        if (Modules.get().isActive(InfinityMiner.class))
            Modules.get().get(InfinityMiner.class).toggle();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public void mine() {
        ChatUtils.moduleInfo(this, "Starting mining job.");
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
                        ChatUtils.moduleWarning(Modules.get().get(Swarm.class), "Server not found. Attempting to reconnect in 5 seconds.");
                    }
                    if (socket == null) {
                        Thread.sleep(5000);
                    }
                }
                if (socket != null) {
                    inputStream = socket.getInputStream();
                    dataInputStream = new DataInputStream(inputStream);
                    ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "New Socket");
                    while (!isInterrupted()) {
                        if (socket != null) {
                            String read;
                            read = dataInputStream.readUTF();
                            if (!read.equals("")) {
                                ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "New Command: " + read);
                                execute(read);
                            }
                        }
                    }
                    dataInputStream.close();
                    inputStream.close();
                }
            } catch (Exception e) {
                ChatUtils.moduleError(Modules.get().get(Swarm.class), "There is an error in your connection to the server.");
                disconnect();
                client = null;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        ChatUtils.moduleError(Modules.get().get(Swarm.class), "There is an error in your connection to the server.");
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
                ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "New Server Opened On Port " + serverPort.get());
                start();
            } catch (Exception ignored) {

            }
        }

        @Override
        public void run() {
            try {
                ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "Listening for incoming connections.");
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
                    ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "New slave connected.");
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
                ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "Server closed.");
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
                ChatUtils.moduleError(Modules.get().get(Swarm.class), "Error in subsystem.");
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
            Commands.get().dispatch(s);
        } catch (CommandSyntaxException ignored) {
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void gameLeftEventListener(GameLeftEvent event) {
        closeAllServerConnections();
        this.toggle();
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void gameJoinedEventListener(GameJoinedEvent event) {
        closeAllServerConnections();
        this.toggle();
    }

    private class SwarmHelpScreen extends WindowScreen {
        private final WVerticalList list;

        public SwarmHelpScreen(GuiTheme theme) {
            super(theme, "Swarm Help");

            WHorizontalList b = add(theme.horizontalList()).expandX().widget();

            WButton intro = b.add(theme.button("(1) Introduction")).expandX().widget();
            intro.action = () -> fillTextList(getSwarmGuideIntro());

            WButton config = b.add(theme.button("(2) Configuration")).expandX().widget();
            config.action = () -> fillTextList(getSwarmGuideConfig());

            WButton queen = b.add(theme.button("(3) Queen")).expandX().widget();
            queen.action = () -> fillTextList(getSwarmGuideQueen());

            WButton slave = b.add(theme.button("(4) Slave")).expandX().widget();
            slave.action = () -> fillTextList(getSwarmGuideSlave());

            list = add(theme.verticalList()).expandX().widget();
            fillTextList(getSwarmGuideIntro());
        }

        private void fillTextList(List<String> text) {
            list.clear();

            for (String s : text) {
                if (s.isEmpty()) list.add(theme.label(""));
                else list.add(theme.label(s, getWindowWidth() / 2.0));
            }
        }
    }

    //I know its ugly, I don't care.
    private List<String> getSwarmGuideIntro() {
        return Arrays.asList(
                "Welcome to Swarm!",
                "",
                "Swarm at its heart is a command tunnel which allows a controlling account, referred to as the queen account, to control other accounts by means of a background server.",
                "",
                "By default, Swarm is configured to work with multiple instances of Minecraft running on the, same computer however with some additional configuration it will work across your local network or the broader internet.",
                "",
                String.format("All swarm commands should be proceeded by \"%s\"", Commands.get().get(SwarmCommand.class).toString())
        );
    }

    private List<String> getSwarmGuideConfig() {
        return Arrays.asList(
                "Localhost Connections:",
                "If the Queen and Slave accounts are all being run on the same computer, there is no need to change anything here if the configured port is not being used for anything else.",
                "",
                "Local Connections:",
                "If the Queen and Slave accounts are not on the same computer, but on the same WiFi/Ethernet network, you will need to change the ip-address on each Slave client to the IPv4/6 address of the computer the Queen instance is running on. To find your IPv4 address on Windows, open CMD and enter the command ipconfig.",
                "",
                "Broad-Internet Connections:",
                "If you are attempting to make a connection over the broader internet a port forward will be required on the queen account. I will not cover how to perform a port forward, look it up. You will need administrator access to your router. Route all traffic through your configured port to the IPv4 address of the computer which is hosting the queen account. After you have successfully port-forwarded on the queen instance, change the ip address of the slave accounts to the public-ip address of the queen account. To find your public-ip address just google 'what is my ip'. NEVER SHARE YOUR PUBLIC IP WITH ANYONE YOU DO NOT TRUST. Assuming you setup everything correctly, you may now proceed as usual."
        );
    }

    private List<String> getSwarmGuideQueen() {
        return Arrays.asList(
                "Setting up the Queen:",
                "Pick an instance of Minecraft to be your queen account. Ensure the swarm module is enabled. Then click the, button labeled 'Run Server(Q)' under the Swarm config menu.",
                "",
                String.format("You may also enter the command \"%s\".", Commands.get().get(SwarmCommand.class).toString("queen"))
        );
    }

    private List<String> getSwarmGuideSlave() {
        return Arrays.asList(
                "Connecting your slaves:",
                "For each slave account, assuming you correctly configured the ip and port in Step 1 simply press the button labeled 'Connect (S)'.",
                "",
                String.format("You may also enter the command \"%s\".", Commands.get().get(SwarmCommand.class).toString("slave"))
        );
    }
}