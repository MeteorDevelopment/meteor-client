package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.gui.GuiConfig;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Scissor {
    public Scissor parent;

    private double x, y;
    private double width, height;

    private boolean active;
    private boolean textOnly;

    public List<Scissor> scissorStack = new ArrayList<>();

    public List<Operation> operations = new ArrayList<>();
    public List<Operation> postOperations = new ArrayList<>();

    public Scissor set(Scissor parent, double x, double y, double width, double height, boolean active, boolean textOnly) {
        // Check bounds
        if (parent != null) {
            // Horizontal
            if (x < parent.x) x = parent.x;
            else if (x + width > parent.x + parent.width) width -= (x + width) - (parent.x + parent.width);

            // Vertical
            if (y < parent.y) y = parent.y;
            else if (y + height > parent.y + parent.height) height -= (y + height) - (parent.y + parent.height);
        }

        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.active = active;
        this.textOnly = textOnly;

        return this;
    }

    public void render(GuiRenderer renderer) {
        // Begin
        if (active) {
            double scaleFactor = GuiConfig.INSTANCE.guiScale;

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (x * scaleFactor), (int) (y * scaleFactor), (int) (width * scaleFactor), (int) (height * scaleFactor));
        }

        // Render
        if (!textOnly) renderer.beginBuffers();
        for (Operation operation : operations) {
            operation.render(renderer);
            operation.free(renderer);
        }
        if (!textOnly) renderer.endBuffers();
        operations.clear();

        for (Operation operation : postOperations) {
            operation.render(renderer);
            operation.free(renderer);
        }
        postOperations.clear();

        // End
        if (active) GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Render childs
        for (Scissor scissor : scissorStack) {
            scissor.render(renderer);
            renderer.scissorPool.free(scissor);
        }
        scissorStack.clear();
    }
}
