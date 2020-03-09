package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;

public class GUI extends Module {
    public enum HoverAnimation {
        FromLeft,
        FromRight,
        FromCenter
    }

    public static Color background = new Color();
    public static Color backgroundHighlighted = new Color();
    public static Color backgroundTextBox = new Color();

    public static Color separator = new Color();
    public static int separatorC;

    public static Color outline = new Color();
    public static Color outlineHighlighted = new Color();

    public static Color checkbox = new Color();

    public static Color text = new Color();
    public static int textC;

    public static HoverAnimation hoverAnimation;
    public static double hoverAnimationSpeedMultiplier;



    private static Setting<Color> backgroundS;
    private static Setting<Color> backgroundHighlightedS;
    private static Setting<Color> backgroundTextBoxS;

    private static Setting<Color> separatorS;

    private static Setting<Color> outlineS;
    private static Setting<Color> outlineHighlightedS;

    private static Setting<Color> checkboxS;

    private static Setting<Color> textS;

    private static Setting<HoverAnimation> hoverAnimationS;
    private static Setting<Double> hoverAnimationSpeedMultiplierS;

    public GUI() {
        super(Category.Setting, "gui", "GUI settings.", true);

        backgroundS = addSetting(new ColorSetting.Builder()
                .name("background")
                .description("Background color.")
                .defaultValue(new Color(50, 50, 50, 225))
                .onChanged(color1 -> background.set(color1))
                .build()
        );
        background.set(backgroundS.get());
        backgroundHighlightedS = addSetting(new ColorSetting.Builder()
                .name("background-highlighted")
                .description("Background highlighted color.")
                .defaultValue(new Color(90, 90, 90, 225))
                .onChanged(color1 -> backgroundHighlighted.set(color1))
                .build()
        );
        backgroundHighlighted.set(backgroundHighlightedS.get());
        backgroundTextBoxS = addSetting(new ColorSetting.Builder()
                .name("background-text-box")
                .description("Background text box color.")
                .defaultValue(new Color(60, 60, 60, 200))
                .onChanged(color1 -> backgroundTextBox.set(color1))
                .build()
        );
        backgroundTextBox.set(backgroundTextBoxS.get());

        separatorS = addSetting(new ColorSetting.Builder()
                .name("separator")
                .description("Separator color.")
                .defaultValue(new Color(75, 75, 75, 225))
                .onChanged(color1 -> {
                    separator.set(color1);
                    separatorC = color1.getPacked();
                })
                .build()
        );
        separator.set(separatorS.get());
        separatorC = separator.getPacked();

        outlineS = addSetting(new ColorSetting.Builder()
                .name("outline")
                .description("Outline color.")
                .defaultValue(new Color(0, 0, 0, 225))
                .onChanged(color1 -> outline.set(color1))
                .build()
        );
        outline.set(outlineS.get());
        outlineHighlightedS = addSetting(new ColorSetting.Builder()
                .name("outline-highlighted")
                .description("Outline highlighted color.")
                .defaultValue(new Color(50, 50, 50, 225))
                .onChanged(color1 -> outlineHighlighted.set(color1))
                .build()
        );
        outlineHighlighted.set(outlineHighlightedS.get());

        checkboxS = addSetting(new ColorSetting.Builder()
                .name("checkbox")
                .description("Checkbox color.")
                .defaultValue(new Color(45, 225, 45))
                .onChanged(color1 -> checkbox.set(color1))
                .build()
        );
        checkbox.set(checkboxS.get());

        textS = addSetting(new ColorSetting.Builder()
                .name("text")
                .description("Text color.")
                .defaultValue(new Color(255, 255, 255))
                .onChanged(color1 -> {
                    text.set(color1);
                    textC = color1.getPacked();
                })
                .build()
        );
        text.set(textS.get());
        textC = text.getPacked();

        hoverAnimationS = addSetting(new EnumSetting.Builder<HoverAnimation>()
                .name("hover-animation")
                .description("Module hover animation.")
                .defaultValue(HoverAnimation.FromLeft)
                .onChanged(hoverAnimation1 -> hoverAnimation = hoverAnimation1)
                .build()
        );
        hoverAnimation = hoverAnimationS.get();

        hoverAnimationSpeedMultiplierS = addSetting(new DoubleSetting.Builder()
                .name("hover-animation-speed-multiplier")
                .description("Module hover animation speed multiplier.")
                .defaultValue(1.0)
                .min(0.0)
                .onChanged(aDouble -> hoverAnimationSpeedMultiplier = aDouble)
                .build()
        );
        hoverAnimationSpeedMultiplier = hoverAnimationSpeedMultiplierS.get();
    }
}
