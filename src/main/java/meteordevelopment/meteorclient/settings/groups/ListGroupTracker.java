package meteordevelopment.meteorclient.settings.groups;

public class ListGroupTracker {

    long a = 0;

    public long get() { return a; }

    public void invalidate() { a++; }

}
