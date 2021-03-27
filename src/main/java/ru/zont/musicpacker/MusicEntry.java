package ru.zont.musicpacker;

import javafx.beans.property.SimpleStringProperty;

public class MusicEntry {
    private final SimpleStringProperty name;
    private final SimpleStringProperty artist;
    private String filename;
    private final double dur;

    MusicEntry(String name, String artist, String filename, double dur) {
        this.name = new SimpleStringProperty(name);
        this.artist = new SimpleStringProperty(artist);
        this.filename = filename;
        this.dur = dur;
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getArtist() {
        return artist.get();
    }

    public SimpleStringProperty artistProperty() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist.set(artist);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public double getDur() {
        return dur;
    }
}
