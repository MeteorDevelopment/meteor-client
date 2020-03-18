package minegame159.meteorclient;

import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Vector2;

import java.util.HashMap;
import java.util.Map;

public class Config implements Listenable {
    public static Config INSTANCE;

    public String prefix = ".";

    public Map<Category, Vector2> guiPositions = new HashMap<>();
    private Map<Category, Color> categoryColors = new HashMap<>();

    public void setCategoryColor(Category category, Color color) {
        categoryColors.put(category, color);
    }

    public Color getCategoryColor(Category category) {
        return categoryColors.get(category);
    }
}
