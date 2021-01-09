package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import net.minecraft.client.options.Perspective;

public class FreeRotate extends Module {

    public enum Mode {
        Player,
        Camera
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which entity to rotate.")
            .defaultValue(Mode.Player)
            .build()
    );

    public final Setting<Boolean> togglePerpective = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-perspective")
            .description("Changes your perspective on toggle.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> sensativity = sgGeneral.add(new DoubleSetting.Builder()
            .name("camera-sensativity")
            .description("How fast the camera moves.")
            .defaultValue(8)
            .min(0)
            .sliderMax(10)
            .build()
    );

    public FreeRotate() {
        super(Category.Render, "free-rotate", "Allows more rotation options in third person.");
    }

    public float cameraYaw;
    public float cameraPitch;

    private Perspective prePers;

    @Override
    public void onActivate() {
        cameraYaw = mc.player.yaw;
        cameraPitch = mc.player.pitch;
        prePers = mc.options.getPerspective();

        if (prePers != Perspective.THIRD_PERSON_BACK &&  togglePerpective.get()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    public void onDeactivate() {
        if (mc.options.getPerspective() != prePers && togglePerpective.get()) mc.options.setPerspective(prePers);
    }

    public boolean playerMode() {
        return isActive() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK && mode.get() == Mode.Player;
    }

    public boolean cameraMode() {
        return isActive() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK && mode.get() == Mode.Camera;
    }
}