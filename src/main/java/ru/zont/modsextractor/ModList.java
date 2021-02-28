package ru.zont.modsextractor;

import org.jsoup.nodes.Document;

import java.util.ArrayList;

public class ModList extends ArrayList<Mod> {
    private final Document document;

    ModList(Document document) {
        this.document = document;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean add(Mod mod, boolean modifyFile) {
        if (modifyFile) {
            setLink(mod, mod.getLink());
            mod.linkProperty().addListener((observable, oldValue, newValue) -> setLink(mod, newValue));
        }
        return add(mod);
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

    public ArrayList<Mod> getAdditionalMods(ModList comparingTo) {
        ArrayList<Mod> res = new ArrayList<>(this);
        res.removeIf(mod -> comparingTo.stream().filter(m -> m.getId() == mod.getId()).findAny().orElse(null) != null);
        return res;
    }
}
