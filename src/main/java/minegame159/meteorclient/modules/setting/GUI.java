package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.ColorSettingBuilder;
import minegame159.meteorclient.settings.builders.DoubleSettingBuilder;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;
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

        backgroundS = addSetting(new ColorSettingBuilder()
                .name("background")
                .description("Background color.")
                .defaultValue(new Color(50, 50, 50, 225))
                .consumer((color1, color2) -> background.set(color2))
                .build()
        );
        background.set(backgroundS.value());
        backgroundHighlightedS = addSetting(new ColorSettingBuilder()
                .name("background-highlighted")
                .description("Background highlighted color.")
                .defaultValue(new Color(75, 75, 75, 225))
                .consumer((color1, color2) -> backgroundHighlighted.set(color2))
                .build()
        );
        backgroundHighlighted.set(backgroundHighlightedS.value());
        backgroundTextBoxS = addSetting(new ColorSettingBuilder()
                .name("background-text-box")
                .description("Background text box color.")
                .defaultValue(new Color(60, 60, 60, 200))
                .consumer((color1, color2) -> backgroundTextBox.set(color2))
                .build()
        );
        backgroundTextBox.set(backgroundTextBoxS.value());

        separatorS = addSetting(new ColorSettingBuilder()
                .name("separator")
                .description("Separator color.")
                .defaultValue(new Color(75, 75, 75, 225))
                .consumer((color1, color2) -> {
                    separator.set(color2);
                    separatorC = color2.getPacked();
                })
                .build()
        );
        separator.set(separatorS.value());
        separatorC = separator.getPacked();

        outlineS = addSetting(new ColorSettingBuilder()
                .name("outline")
                .description("Outline color.")
                .defaultValue(new Color(0, 0, 0, 225))
                .consumer((color1, color2) -> outline.set(color2))
                .build()
        );
        outline.set(outlineS.value());
        outlineHighlightedS = addSetting(new ColorSettingBuilder()
                .name("outline-highlighted")
                .description("Outline highlighted color.")
                .defaultValue(new Color(50, 50, 50, 225))
                .consumer((color1, color2) -> outlineHighlighted.set(color2))
                .build()
        );
        outlineHighlighted.set(outlineHighlightedS.value());

        checkboxS = addSetting(new ColorSettingBuilder()
                .name("checkbox")
                .description("Checkbox color.")
                .defaultValue(new Color(45, 225, 45))
                .consumer((color1, color2) -> checkbox.set(color2))
                .build()
        );
        checkbox.set(checkboxS.value());

        textS = addSetting(new ColorSettingBuilder()
                .name("text")
                .description("Text color.")
                .defaultValue(new Color(255, 255, 255))
                .consumer((color1, color2) -> {
                    text.set(color2);
                    textC = color2.getPacked();
                })
                .build()
        );
        text.set(textS.value());
        textC = text.getPacked();

        hoverAnimationS = addSetting(new EnumSettingBuilder<HoverAnimation>()
                .name("hover-animation")
                .description("Module hover animation.")
                .defaultValue(HoverAnimation.FromLeft)
                .consumer((hoverAnimation1, hoverAnimation2) -> hoverAnimation = hoverAnimation2)
                .build()
        );
        hoverAnimation = hoverAnimationS.value();

        hoverAnimationSpeedMultiplierS = addSetting(new DoubleSettingBuilder()
                .name("hover-animation-speed-multiplier")
                .description("Module hover animation speed multiplier.")
                .defaultValue(1.0)
                .min(0.0)
                .consumer((aDouble, aDouble2) -> hoverAnimationSpeedMultiplier = aDouble2)
                .build()
        );
        hoverAnimationSpeedMultiplier = hoverAnimationSpeedMultiplierS.value();
    }
}
