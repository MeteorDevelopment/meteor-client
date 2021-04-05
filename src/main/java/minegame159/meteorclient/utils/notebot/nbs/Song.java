package minegame159.meteorclient.utils.notebot.nbs;

import java.io.File;
import java.util.HashMap;

public class Song {

    private HashMap<Integer, Layer> layerHashMap = new HashMap<Integer, Layer>();
    private short songHeight;
    private short length;
    private String title;
    private File path;
    private String author;
    private String description;
    private float speed;
    private float delay;

    public Song(Song other) {
        this.speed = other.getSpeed();
        delay = 20 / speed;
        this.layerHashMap = other.getLayerHashMap();
        this.songHeight = other.getSongHeight();
        this.length = other.getLength();
        this.title = other.getTitle();
        this.author = other.getAuthor();
        this.description = other.getDescription();
        this.path = other.getPath();
    }

    public Song(float speed, HashMap<Integer, Layer> layerHashMap,
                short songHeight, final short length, String title, String author,
                String description, File path) {
        this.speed = speed;
        delay = 20 / speed;
        this.layerHashMap = layerHashMap;
        this.songHeight = songHeight;
        this.length = length;
        this.title = title;
        this.author = author;
        this.description = description;
        this.path = path;
    }

    public HashMap<Integer, Layer> getLayerHashMap() {
        return layerHashMap;
    }

    public short getSongHeight() {
        return songHeight;
    }

    public short getLength() {
        return length;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public File getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public float getSpeed() {
        return speed;
    }

    public float getDelay() {
        return delay;
    }
}