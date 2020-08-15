package minegame159.meteorclient.gui;

public class GuiThings {
    public static int postKeyEvents = 0;

    public static void setPostKeyEvents(boolean post) {
        postKeyEvents += post ? 1 : -1;
    }
    public static boolean postKeyEvents() {
        return postKeyEvents <= 0;
    }
    public static void resetPostKeyEvents() {
        postKeyEvents = 0;
    }
}
