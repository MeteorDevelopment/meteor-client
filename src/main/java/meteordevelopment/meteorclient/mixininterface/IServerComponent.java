package meteordevelopment.meteorclient.mixininterface;

/** Component contents that can be marked as having been sent by a remote server. */
public interface IServerComponent {
    void ember$markFromServer();

    boolean ember$isFromServer();
}
