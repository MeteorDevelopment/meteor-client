/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.baritoneflow;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.systems.baritoneflow.BaritoneFlows;
import meteordevelopment.meteorclient.systems.baritoneflow.Flow;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNode;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNodeType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.BaritoneFlow;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * n8n-style visual editor for a {@link Flow}. Nodes are draggable boxes; drag from a node's right
 * (output) port onto another node to wire them together. Right-click a node to edit it, right-click
 * empty space to add one. Rendered directly with {@link Renderer2D} like the HUD editor.
 */
public class FlowEditorScreen extends WidgetScreen {
    private static final int NODE_W = 172;
    private static final int NODE_H = 48;
    private static final int PORT = 8;

    private static final Color BG = new Color(38, 40, 52, 220);
    private static final Color BG_HOVER = new Color(54, 57, 72, 230);
    private static final Color BG_CURRENT = new Color(70, 62, 30, 235);
    private static final Color OL = new Color(120, 124, 145, 255);
    private static final Color OL_START = new Color(90, 190, 110, 255);
    private static final Color OL_CURRENT = new Color(235, 190, 70, 255);
    private static final Color PORT_COLOR = new Color(150, 155, 190, 255);
    private static final Color LINE = new Color(150, 155, 180, 220);
    private static final Color TITLE = new Color(235, 236, 245, 255);
    private static final Color SUBTITLE = new Color(170, 174, 190, 255);
    private static final Color HELP = new Color(200, 202, 214, 190);

    private final Flow flow;

    private int lastMouseX, lastMouseY;

    private FlowNode draggingNode;
    private int dragOffsetX, dragOffsetY;
    private FlowNode connectingFrom;

    public FlowEditorScreen(GuiTheme theme, Flow flow) {
        super(theme, "Flow: " + flow.name);
        this.flow = flow;
    }

    @Override
    public void initWidgets() {
        // Everything is custom-rendered; no widgets on the canvas itself.
    }

    // Input

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = scale(click.x());
        int my = scale(click.y());

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            FlowNode port = getOutputPortAt(mx, my);
            if (port != null) {
                connectingFrom = port;
                return true;
            }

            FlowNode node = getNodeAt(mx, my);
            if (node != null) {
                draggingNode = node;
                dragOffsetX = mx - node.x;
                dragOffsetY = my - node.y;
            }
        } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            FlowNode node = getNodeAt(mx, my);
            if (node != null) mc.setScreen(new FlowNodeScreen(theme, flow, node));
            else mc.setScreen(new AddFlowNodeScreen(theme, flow, mx, my));
            return true;
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        lastMouseX = scale(mouseX);
        lastMouseY = scale(mouseY);

        if (draggingNode != null) {
            draggingNode.x = lastMouseX - dragOffsetX;
            draggingNode.y = lastMouseY - dragOffsetY;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        int mx = scale(click.x());
        int my = scale(click.y());

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (connectingFrom != null) {
                FlowNode target = getNodeAt(mx, my);
                if (target != null && target != connectingFrom) {
                    // Toggle: wire them up, or remove an existing wire.
                    if (connectingFrom.children.contains(target.id)) flow.disconnect(connectingFrom, target);
                    else flow.connect(connectingFrom, target);
                    save();
                }
                connectingFrom = null;
            }

            if (draggingNode != null) {
                draggingNode = null;
                save();
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        switch (input.key()) {
            case GLFW.GLFW_KEY_DELETE -> {
                FlowNode node = getNodeAt(lastMouseX, lastMouseY);
                if (node != null) {
                    flow.removeNode(node);
                    save();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_R -> {
                BaritoneFlow module = Modules.get().get(BaritoneFlow.class);
                if (module != null) module.runFlow(flow);
                return true;
            }
            case GLFW.GLFW_KEY_X -> {
                BaritoneFlow module = Modules.get().get(BaritoneFlow.class);
                if (module != null) module.stopAll();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    // Rendering

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        FlowNode current = currentNode();
        FlowNode hovered = getNodeAt(mouseX, mouseY);

        Renderer2D r = Renderer2D.COLOR;
        r.begin();

        // Connections
        for (FlowNode node : flow.nodes) {
            double ox = node.x + NODE_W, oy = node.y + NODE_H / 2.0;
            for (int childId : node.children) {
                FlowNode child = flow.getById(childId);
                if (child == null) continue;
                r.line(ox, oy, child.x, child.y + NODE_H / 2.0, LINE);
            }
        }

        // Wire currently being dragged
        if (connectingFrom != null) {
            r.line(connectingFrom.x + NODE_W, connectingFrom.y + NODE_H / 2.0, lastMouseX, lastMouseY, LINE);
        }

        // Nodes
        for (FlowNode node : flow.nodes) {
            boolean isStart = node.type.get() == FlowNodeType.Start;
            Color bg = node == current ? BG_CURRENT : (node == hovered ? BG_HOVER : BG);
            Color ol = node == current ? OL_CURRENT : (isStart ? OL_START : OL);

            box(r, node.x, node.y, NODE_W, NODE_H, bg, ol);

            // Ports
            r.quad(node.x - PORT / 2.0, node.y + NODE_H / 2.0 - PORT / 2.0, PORT, PORT, PORT_COLOR);
            r.quad(node.x + NODE_W - PORT / 2.0, node.y + NODE_H / 2.0 - PORT / 2.0, PORT, PORT, PORT_COLOR);
        }

        r.render();

        // Text
        VanillaTextRenderer text = VanillaTextRenderer.INSTANCE;
        text.begin(1, false, false);

        text.render("Flow: " + flow.name, 8, 8, TITLE, true);
        text.render("Left-drag node / wire ports  -  Right-click: edit or add  -  Delete: remove  -  R: run  -  X: stop", 8, 26, HELP, true);

        for (FlowNode node : flow.nodes) {
            text.render(node.title(), node.x + 8, node.y + 7, TITLE, false);
            String summary = clip(node.summary(), 22);
            if (!summary.isEmpty()) text.render(summary, node.x + 8, node.y + 26, SUBTITLE, false);
        }

        text.end();
    }

    private void box(Renderer2D r, double x, double y, double w, double h, Color bg, Color ol) {
        r.quad(x + 1, y + 1, w - 2, h - 2, bg);
        r.quad(x, y, w, 1, ol);
        r.quad(x, y + h - 1, w, 1, ol);
        r.quad(x, y + 1, 1, h - 2, ol);
        r.quad(x + w - 1, y + 1, 1, h - 2, ol);
    }

    // Helpers

    private int scale(double coord) {
        return (int) (coord * mc.getWindow().getGuiScale());
    }

    private FlowNode getNodeAt(int mx, int my) {
        for (int i = flow.nodes.size() - 1; i >= 0; i--) {
            FlowNode n = flow.nodes.get(i);
            if (mx >= n.x && mx <= n.x + NODE_W && my >= n.y && my <= n.y + NODE_H) return n;
        }
        return null;
    }

    private FlowNode getOutputPortAt(int mx, int my) {
        for (int i = flow.nodes.size() - 1; i >= 0; i--) {
            FlowNode n = flow.nodes.get(i);
            double px = n.x + NODE_W, py = n.y + NODE_H / 2.0;
            if (Math.abs(mx - px) <= PORT && Math.abs(my - py) <= PORT) return n;
        }
        return null;
    }

    private FlowNode currentNode() {
        BaritoneFlow module = Modules.get().get(BaritoneFlow.class);
        if (module == null || module.getRunningFlow() != flow) return null;
        return module.getCurrentNode();
    }

    private void save() {
        BaritoneFlows.get().save();
    }

    private static String clip(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
