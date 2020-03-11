package minegame159.meteorclient.altsfriends;

public class NameHistoryResponse {
    public NameHistoryEntry[] entries;

    public static class NameHistoryEntry {
        public String name;
    }

    public String getUsername() {
        return entries[entries.length - 1].name;
    }
}
