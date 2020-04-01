package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;

public class GUI extends Module {
    public static Color background = new Color();
    public static Color backgroundModuleActive = new Color();
    public static Color backgroundHighlighted = new Color();
    public static Color backgroundTextBox = new Color();
    public static Color accent = new Color();

    public static Color separator = new Color();
    public static int separatorC;

    public static Color outline = new Color();
    public static Color outlineHighlighted = new Color();

    public static Color checkbox = new Color();
    public static Color plus = new Color();
    public static Color minus = new Color();

    public static Color text = new Color();
    public static int textC;
    public static Color windowHeaderText = new Color();
    public static int windowHeaderTextC;
    public static Color textLoggedIn = new Color();
    public static int textLoggedInC;

    public static double hoverAnimationSpeedMultiplier;

    public static double scrollMultiplier;



    private static Setting<Color> backgroundS;
    private static Setting<Color> backgroundModuleActiveS;
    private static Setting<Color> backgroundHighlightedS;
    private static Setting<Color> backgroundTextBoxS;
    private static Setting<Color> accentS;

    private static Setting<Color> separatorS;

    private static Setting<Color> outlineS;
    private static Setting<Color> outlineHighlightedS;

    private static Setting<Color> checkboxS;
    private static Setting<Color> plusS;
    private static Setting<Color> minusS;

    private static Setting<Color> textS;
    private static Setting<Color> windowHeaderTextS;
    private static Setting<Color> textLoggedInS;

    private static Setting<Double> hoverAnimationSpeedMultiplierS;

    private static Setting<Double> scrollMultiplierS;

    public GUI() {
        super(Category.Setting, "gui", "GUI settings.");

        backgroundS = addSetting(new ColorSetting.Builder()
                .name("background")
                .description("Background color.")
                .defaultValue(new Color(50, 50, 50, 225))
                .onChanged(color1 -> background.set(color1))
                .build()
        );
        background.set(backgroundS.get());
        backgroundModuleActiveS = addSetting(new ColorSetting.Builder()
                .name("background-moule-active")
                .description("Active module background color.")
                .defaultValue(new Color(50, 50, 50, 180))
                .onChanged(color1 -> backgroundModuleActive.set(color1))
                .build()
        );
        backgroundModuleActive.set(backgroundModuleActiveS.get());
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
        accentS = addSetting(new ColorSetting.Builder()
                .name("accent")
                .description("Accent color.")
                .defaultValue(new Color(0, 255, 180))
                .onChanged(color1 -> accent.set(color1))
                .build()
        );
        accent.set(accentS.get());

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
        plusS = addSetting(new ColorSetting.Builder()
                .name("plus")
                .description("Plus color.")
                .defaultValue(new Color(45, 225, 45))
                .onChanged(color1 -> plus.set(color1))
                .build()
        );
        plus.set(plusS.get());
        minusS = addSetting(new ColorSetting.Builder()
                .name("minus")
                .description("Minus color.")
                .defaultValue(new Color(225, 45, 45))
                .onChanged(color1 -> minus.set(color1))
                .build()
        );
        minus.set(minusS.get());

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

        windowHeaderTextS = addSetting(new ColorSetting.Builder()
                .name("window-header-text")
                .description("Window header text color.")
                .defaultValue(new Color(255, 255, 255))
                .onChanged(color1 -> windowHeaderText.set(color1))
                .build()
        );
        windowHeaderText.set(windowHeaderTextS.get());
        windowHeaderTextC = windowHeaderText.getPacked();

        textLoggedInS = addSetting(new ColorSetting.Builder()
                .name("text-logged-in")
                .description("Text logged in color.")
                .defaultValue(new Color(45, 225, 45))
                .onChanged(color1 -> {
                    textLoggedIn.set(color1);
                    textLoggedInC = color1.getPacked();
                })
                .build()
        );
        textLoggedIn.set(textLoggedInS.get());
        textLoggedInC = textLoggedIn.getPacked();

        hoverAnimationSpeedMultiplierS = addSetting(new DoubleSetting.Builder()
                .name("hover-animation-speed-multiplier")
                .description("Module hover animation speed multiplier.")
                .defaultValue(1.0)
                .min(0.0)
                .onChanged(aDouble -> hoverAnimationSpeedMultiplier = aDouble)
                .build()
        );
        hoverAnimationSpeedMultiplier = hoverAnimationSpeedMultiplierS.get();

        scrollMultiplierS = addSetting(new DoubleSetting.Builder()
                .name("scroll-multiplier")
                .description("Scroll multiplier.")
                .defaultValue(1)
                .min(0)
                .onChanged(aDouble -> scrollMultiplier = aDouble)
                .build()
        );
        scrollMultiplier = scrollMultiplierS.get();
    }
}
