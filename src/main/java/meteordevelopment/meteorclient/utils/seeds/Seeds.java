package anticope.rejects.utils.seeds;

import java.util.HashMap;

import anticope.rejects.events.SeedChangedEvent;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.seedfinding.mccore.version.MCVersion;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Seeds extends System<Seeds> {
    private static final Seeds INSTANCE = new Seeds();

    public HashMap<String, Seed> seeds = new HashMap<>();

    public Seeds() {
        super("seeds");
        init();
        load(MeteorClient.FOLDER);
    }

    public static Seeds get() {
        return INSTANCE;
    }

    public Seed getSeed() {
        if (mc.isIntegratedServerRunning() && mc.getServer() != null) {
            MCVersion version = MCVersion.fromString(mc.getServer().getVersion());
            if (version == null)
                version = MCVersion.latest();
            return new Seed(mc.getServer().getOverworld().getSeed(), version);
        }

        return seeds.get(Utils.getWorldName());
    }

    public void setSeed(String seed, MCVersion version) {
        if (mc.isIntegratedServerRunning()) return;

        long numSeed = toSeed(seed);
        seeds.put(Utils.getWorldName(), new Seed(numSeed, version));
        MeteorClient.EVENT_BUS.post(SeedChangedEvent.get(numSeed));
    }

    public void setSeed(String seed) {
        if (mc.isIntegratedServerRunning()) return;

        ServerInfo server = mc.getCurrentServerEntry();
        MCVersion ver = null;
        if (server != null)
            ver = MCVersion.fromString(server.version.getString());
        if (ver == null) {
            String targetVer = "unknown";
            if (server != null) targetVer = server.version.getString();
            sendInvalidVersionWarning(seed, targetVer);
            ver = MCVersion.latest();
        }
        setSeed(seed, ver);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        seeds.forEach((key, seed) -> {
            if (seed == null) return;
            tag.put(key, seed.toTag());
        });
        return tag;
    }

    @Override
    public Seeds fromTag(NbtCompound tag) {
        tag.getKeys().forEach(key -> {
            seeds.put(key, Seed.fromTag(tag.getCompound(key)));
        });
        return this;
    }

    // https://minecraft.fandom.com/wiki/Seed_(level_generation)#Java_Edition
    private static long toSeed(String inSeed) {
        try {
            return Long.parseLong(inSeed);
        } catch (NumberFormatException e) {
            return inSeed.strip().hashCode();
        }
    }

    private static void sendInvalidVersionWarning(String seed, String targetVer) {
        MutableText msg = Text.literal(String.format("Couldn't resolve minecraft version \"%s\". Using %s instead. If you wish to change the version run: ", targetVer, MCVersion.latest().name));
        String cmd = String.format("%sseed %s ", Config.get().prefix, seed);
        MutableText cmdText = Text.literal(cmd+"<version>");
        cmdText.setStyle(cmdText.getStyle()
            .withUnderline(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("run command")))
        );
        msg.append(cmdText);
        msg.setStyle(msg.getStyle()
            .withColor(Formatting.YELLOW)
        );
        ChatUtils.sendMsg("Seed", msg);
    }
}
