package minegame159.meteorclient.mixininterface;

public interface IChatHudLine<T> {
    void setText(T text);

    void setTimestamp(int timestamp);

    void setId(int id);
}
