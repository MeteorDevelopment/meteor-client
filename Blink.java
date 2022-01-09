package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.*;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.*;

public class Blink extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public Blink() {
        super(Categories.Movement, "blink", "Allows you to essentially teleport while suspending motion updates.");
    }

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render trail")
        .description("Renders the path you went while using blink.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> player = sgRender.add(new BoolSetting.Builder()
        .name("fake player")
        .description("Renders a fake player at the position visible for everybody else.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("trail color")
        .description("The color of the trail.")
        .defaultValue(new SettingColor(255, 0, 0))
        .visible(render::get)
        .build()
    );

    private final Setting<Integer> maxSections = sgRender.add(new IntSetting.Builder()
        .name("max-sections")
        .description("The maximum number of sections.")
        .defaultValue(1000)
        .min(50)
        .max(5000)
        .sliderMin(500)
        .sliderMax(10000)
        .visible(render::get)
        .build()
    );

    private final Setting<Double> sectionLength = sgRender.add(new DoubleSetting.Builder()
        .name("section-length")
        .description("The section length in blocks.")
        .defaultValue(0.5)
        .min(0.01)
        .sliderMax(1)
        .visible(render::get)
        .build()
    );

    private ArrayList<Module> modules;

    private static final List<FakePlayerEntity> fakePlayers = new ArrayList<>();
    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private Random random;
    private int timer;
    private int delay;

    private final Pool<Section> sectionPool = new Pool<>(Section::new);
    private final Queue<Section> sections = new ArrayDeque<>();

    private Section section;

    @Override
    public void onActivate() {
        modules = new ArrayList<>();
        section = sectionPool.get();
        section.set1();

        random = new Random();

        timer = 0;
        delay = 0;

        clear();

        if (player.get()) {
            FakePlayerEntity fakePlayer = new FakePlayerEntity(mc.player, mc.getSession().getProfile().getName(), mc.player.getHealth(), true);
            fakePlayers.add(fakePlayer);
            fakePlayer.spawn();
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.world == null || mc.player == null) return;

        clear();

        synchronized (packets) {
            packets.forEach(packet -> mc.getNetworkHandler().sendPacket(packet));
            packets.clear();
        }

        for (Section section : sections) sectionPool.free(section);
        sections.clear();

        for (Module module : modules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        timer++;
        delay--;

        if (render.get() && isFarEnough(section.x1, section.y1, section.z1)) {
            section.set2();

            if (sections.size() >= maxSections.get()) {
                Section section = sections.poll();
                if (section != null) sectionPool.free(section);
            }

            sections.add(section);
            section = sectionPool.get();
            section.set1();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        int iLast = -1;

        for (Section section : sections) {
            if (iLast == -1) {
                iLast = event.renderer.lines.vec3(section.x1, section.y1, section.z1).color(color.get()).next();
            }

            int i = event.renderer.lines.vec3(section.x2, section.y2, section.z2).color(color.get()).next();
            event.renderer.lines.line(iLast, i);
            iLast = i;
        }
    }

    @Override
    public String getInfoString() {
        return String.format("%.1f", timer / 20f);
    }

    private void clear() {
        if (!fakePlayers.isEmpty()) {
            fakePlayers.forEach(FakePlayerEntity::despawn);
            fakePlayers.clear();
        }
    }

    private void clearPosLook() {
        {
            synchronized (packets) {
                packets.forEach(packet -> mc.getNetworkHandler().sendPacket(packet));
                packets.clear();
            }

            if (player.get()) {
                clear();

                FakePlayerEntity fakePlayer = new FakePlayerEntity(mc.player, mc.getSession().getProfile().getName(), mc.player.getHealth(), true);
                fakePlayers.add(fakePlayer);
                fakePlayer.spawn();
            }
        }
    }

    private boolean isFarEnough(double x, double y, double z) {
        return Math.abs(mc.player.getX() - x) >= sectionLength.get() || Math.abs(mc.player.getY() - y) >= sectionLength.get() || Math.abs(mc.player.getZ() - z) >= sectionLength.get();
    }

    private class Section {
        public float x1, y1, z1;
        public float x2, y2, z2;

        public void set1() {
            x1 = (float) mc.player.getX();
            y1 = (float) mc.player.getY();
            z1 = (float) mc.player.getZ();
        }

        public void set2() {
            x2 = (float) mc.player.getX();
            y2 = (float) mc.player.getY();
            z2 = (float) mc.player.getZ();
        }

        public void render(Render3DEvent event) {
            event.renderer.line(x1, y1, z1, x2, y2, z2, color.get());
        }
    }
}
