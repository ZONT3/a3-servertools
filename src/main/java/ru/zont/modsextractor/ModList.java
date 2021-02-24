package ru.zont.modsextractor;

import org.jsoup.nodes.Document;

import java.util.ArrayList;

public class ModList extends ArrayList<Mod> {
    private final Document document;

    public ModList(Document document) {
        this.document = document;
    }

    @Override
    public boolean add(Mod mod) {
        boolean sup = super.add(mod);

        setLink(mod, mod.getLink());
        mod.linkProperty().addListener((observable, oldValue, newValue) -> setLink(mod, newValue));

        return sup;
    }

    public void setLink(Mod mod, String link) {
        if (link != null && link.startsWith("@"))
            setLink(mod.getId(), mod.getLink());
    }

    public void setLink(long id, String link) {
        Parser.setSymLink(document, id, link);
    }

    public String getPresetHTML() {
        return document.outerHtml();
    }
}
