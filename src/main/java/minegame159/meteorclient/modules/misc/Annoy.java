package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class Annoy extends ToggleModule {
    public Annoy() {
        super(Category.Misc, "annoy", "Makes your messages wEiRd.");
    }

    public String doAnnoy(String msg) {
        StringBuilder sb = new StringBuilder(msg.length());

        boolean upperCase = true;
        for (int cp : msg.codePoints().toArray()) {
            if (upperCase) sb.appendCodePoint(Character.toUpperCase(cp));
            else sb.appendCodePoint(Character.toLowerCase(cp));

            upperCase = !upperCase;
        }

        return sb.toString();
    }
}
