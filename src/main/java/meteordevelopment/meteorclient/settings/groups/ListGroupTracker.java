package meteordevelopment.meteorclient.settings.groups;

public class ListGroupTracker {

    long a = 0;

    public long getVersion() { return a; }

    public void invalidate() { a++; }

}
