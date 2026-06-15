package meteordevelopment.meteorclient.utils.skyblock.terminal;

import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.*;
import net.minecraft.world.item.DyeColor;

import java.util.function.Function;
import java.util.regex.Pattern;

public enum TerminalTypes {
    PANES("Correct all the panes!", Pattern.compile("^Correct all the panes!$"), 45),
    RUBIX("Change all to same color!", Pattern.compile("^Change all to same color!$"), 45),
    NUMBERS("Click in order!", Pattern.compile("^Click in order!$"), 36),
    STARTS_WITH("What starts with: \"*\"?", Pattern.compile("^What starts with: '(\\w)'\\?$"), 45),
    SELECT("Select all the \"*\" items!", Pattern.compile("^Select all the ([\\w ]+) items!$"), 54),
    MELODY("Click the button on time!", Pattern.compile("^Click the button on time!$"), 54);

    public final String termName;
    public final Pattern regex;
    public final int windowSize;

    TerminalTypes(String termName, Pattern regex, int windowSize) {
        this.termName = termName;
        this.regex = regex;
        this.windowSize = windowSize;
    }

    public TerminalHandler createHandler(String guiName) {
        return switch (this) {
            case PANES -> new PanesHandler();
            case RUBIX -> new RubixHandler();
            case NUMBERS -> new NumbersHandler();
            case STARTS_WITH -> {
                var matcher = regex.matcher(guiName);
                if (matcher.find()) yield new StartsWithHandler(matcher.group(1));
                else yield null;
            }
            case SELECT -> {
                var matcher = regex.matcher(guiName);
                if (matcher.find()) {
                    String colorName = matcher.group(1).replace("SILVER", "LIGHT GRAY");
                    DyeColor color = null;
                    for (DyeColor dc : DyeColor.values()) {
                        if (dc.getName().replace("_", " ").equalsIgnoreCase(colorName)) {
                            color = dc;
                            break;
                        }
                    }
                    if (color != null) yield new SelectAllHandler(color);
                    else yield null;
                } else yield null;
            }
            case MELODY -> new MelodyHandler();
        };
    }
}
