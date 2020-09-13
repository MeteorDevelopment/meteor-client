package minegame159.meteorclient.mixininterface;

public interface IChatHudLine<T> {
    public void setText(T text);

    public void setTimestamp(int timestamp);

    public void setId(int id);
}
