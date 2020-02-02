package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.setting.GUI;

import java.util.List;

public class ModuleGroup extends Widget {
    public final Category category;

    public ModuleGroup(Category category) {
        super(0, 0, 0);
        this.category = category;

        Background root = new Background(1);
        VerticalContainer list = new VerticalContainer(0, 0);

        // Title
        Container titleContainer = new Container(7, true, false);
        titleContainer.addWidget(new Label(0, category.toString(), true));
        list.addWidget(titleContainer);
        list.addWidget(new Separator(0, GUI.outline));

        // Modules
        List<Module> modules = ModuleManager.getGroup(category);
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) list.addWidget(new Separator(0, GUI.separator, 5));

            list.addWidget(new ModuleWidget(modules.get(i)));
        }

        root.addWidget(list);
        addWidget(root);
    }

    @Override
    public void layout() {
        super.layout();
        double yy = widgets.get(0).widgets.get(0).y + widgets.get(0).widgets.get(0).margin;
        for (int i = 0; i < widgets.get(0).widgets.get(0).widgets.size(); i++) {
            if (i == 2) yy += 5;
            widgets.get(0).widgets.get(0).widgets.get(i).y = yy;
            widgets.get(0).widgets.get(0).widgets.get(i).layout();
            yy += widgets.get(0).widgets.get(0).widgets.get(i).heightMargin();
        }
        widgets.get(0).widgets.get(0).calculateSize();
        widgets.get(0).calculateSize();
        widgets.get(0).height += 5;
        calculateSize();
    }
}
