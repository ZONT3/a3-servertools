package ru.zont.modsextractor;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

import java.net.URI;
import java.net.URL;

public class Mod {
    private final SimpleStringProperty name;
    private final SimpleLongProperty id;
    private final SimpleStringProperty link;

    public Mod(String name, long id) {
        this.name = new SimpleStringProperty(name);
        this.id = new SimpleLongProperty(id);
        link = new SimpleStringProperty("");
    }

    public String getName() {
        return name.get();
    }

    public long getId() {
        return id.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setId(long id) {
        this.id.set(id);
    }

    public String getLink() {
        return link.get();
    }

    public void setLink(String link) {
        this.link.set(link);
    }

    public SimpleLongProperty idProperty() {
        return id;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public SimpleStringProperty linkProperty() {
        return link;
    }
}
