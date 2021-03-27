package ru.zont.musicpacker;

import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.Collection;

public class MusicList extends ArrayList<MusicEntry> {
    private final SimpleStringProperty displayName;

    public MusicList(String displayName) {
        this.displayName = new SimpleStringProperty(displayName);
    }

    public MusicList(String displayName, Collection<? extends MusicEntry> c) {
        super(c);
        this.displayName = new SimpleStringProperty(displayName);
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public SimpleStringProperty displayNameProperty() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName.set(displayName);
    }
}
