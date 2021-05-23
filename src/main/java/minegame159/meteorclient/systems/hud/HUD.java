package minegame159.meteorclient.systems.hud;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.gui.tabs.builtin.HudTab;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.systems.hud.elements.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HUD extends System<HUD> {

    public enum Guides {
        Always,
        Dragging,
        None
    }

    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEditor = settings.createGroup("Editor");

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the HUD.")
            .defaultValue(1)
            .min(0.75)
            .sliderMin(0.75).sliderMax(4)
            .build()
    );

    public final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("primary-color")
            .description("The primary color of all HUD text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public final Setting<SettingColor> secondaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("secondary-color")
            .description("The secondary color of all HUD text.")
            .defaultValue(new SettingColor(175, 175, 175))
            .build()
    );

    // Editor

    public final Setting<Integer> snappingRange = sgEditor.add(new IntSetting.Builder()
            .name("snapping-range")
            .description("The snapping range in the HUD editor.")
            .defaultValue(6)
            .build()
    );

    public final Setting<Integer> margin = sgEditor.add(new IntSetting.Builder()
            .name("margin")
            .description("The margin around the edge of the screen that elements cant go into.")
            .defaultValue(3)
            .min(0)
            .build()
    );

    public final Setting<Guides> guides = sgEditor.add(new EnumSetting.Builder<Guides>()
            .name("guides")
            .description("Shows guidelines to help you align elements.")
            .defaultValue(Guides.Dragging)
            .build()
    );

    protected final MinecraftClient mc;

    public final List<HudElement> activeElements = new ArrayList<>();
    public final Map<String, Class<? extends HudElement>> elementClasses = new HashMap<>();

    private static final HudRenderer renderer = new HudRenderer();

    public boolean active = true;

    public HUD() {
        super("hud");
        this.mc = MinecraftClient.getInstance();
    }

    public static HUD get() {
        return Systems.get(HUD.class);
    }

    @Override
    public void init() {
        register(ActiveModulesHud.class);
        register(ArmorHud.class);
        register(BiomeHud.class);
        register(BreakingBlockHud.class);
        register(CombatHud.class);
        register(CompassHud.class);
        register(DurabilityHud.class);
        register(FpsHud.class);
        register(HoleHud.class);
        register(InventoryViewerHud.class);
        register(LagNotifierHud.class);
        register(LookingAtHud.class);
        register(ModuleInfoHud.class);
        register(PingHud.class);
        register(PlayerModelHud.class);
        register(PositionHud.class);
        register(PotionTimersHud.class);
        register(RotationHud.class);
        register(ServerHud.class);
        register(SpeedHud.class);
        register(TextRadarHud.class);
        register(TimeHud.class);
        register(TpsHud.class);
        register(WatermarkHud.class);
        register(WelcomeHud.class);
        register(CustomItemHud.class);
    }

    public void add(HudElement element) {
        activeElements.add(element);
    }

    public void register(Class<? extends HudElement> klass) {
        if (klass.getAnnotation(ElementRegister.class) != null) {
            elementClasses.put(klass.getAnnotation(ElementRegister.class).name(), klass);
        }
    }

    @EventHandler
    public void onRender(Render2DEvent event) {
        if (mc.options.debugEnabled) return;

        renderer.begin(scale.get(), event.tickDelta, false);

        for (HudElement element : activeElements) {
            if ((active) || isInEditor()) {
                element.update(renderer);
                element.render(renderer);
            }
        }

        renderer.end();
    }

    public boolean isInEditor() {
        return mc.currentScreen instanceof HudTab.HudEditorScreen || mc.currentScreen instanceof HudTab.HudElementScreen || !Utils.canUpdate();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void reset() {
        activeElements.clear();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.put("settings", settings.toTag());
        tag.putBoolean("active", active);

        ListTag elementsTag = new ListTag();
        for (HudElement element : activeElements) elementsTag.add(element.toTag());
        tag.put("elements", elementsTag);

        return tag;
    }

    @Override
    public HUD fromTag(CompoundTag tag) {
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));

        active = !tag.contains("active") || tag.getBoolean("active");

        if (tag.contains("elements")) {
            ListTag elementsTag = tag.getList("elements", 10);

            for (Tag t : elementsTag) {
                CompoundTag elementTag = (CompoundTag) t;

                Class<? extends HudElement> klass = elementClasses.get(elementTag.getString("name"));
                if (klass == null) continue;

                HudElement element = null;
                try {
                    element = klass.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
                }

                if (element == null) continue;
                element.fromTag(elementTag);

                activeElements.add(element);
            }
        }

        return this;
    }
}
