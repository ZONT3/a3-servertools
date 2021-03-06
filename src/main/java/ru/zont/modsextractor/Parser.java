package ru.zont.modsextractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.zont.apptools.Strings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Parser {
    public static ModList parse(File file) throws IOException {
        return parse(file, true);
    }

    public static ModList parse(File file, boolean modifyFile) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8");
        Elements trs = doc.body().getElementsByAttributeValue("data-type", "ModContainer");
        ModList mods = new ModList(doc);

        for (Element tr: trs) {
            String name, link = null;
            long id;
            try {
                name = tr.getElementsByAttributeValue("data-type", "DisplayName").first().text();
                id = getId(tr);

                Element symLink = tr.getElementsByAttributeValue("data-type", "SymLink").first();
                if (symLink != null)
                    link = symLink.text();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Mod mod = new Mod(name, id);

            if (link == null)
                genLinkName(mod);
            else mod.setLink(link);

            mods.add(mod, modifyFile);
        }
        return mods;
    }

    private static long getId(Element tr) throws URISyntaxException {
        return Long.parseLong(new URI(tr.getElementsByAttributeValue("data-type", "Link").first().text()).getQuery().split("=")[1]);
    }

    private static void genLinkName(Mod mod) {
        String name = Strings.normalizeString(mod.getName());

        mod.setLink("@" + name);
    }

    public static void setSymLink(Document doc, long id, String link) {
        Elements trs = doc.body().getElementsByAttributeValue("data-type", "ModContainer");
        Element mod = null;
        for (Element tr: trs) {
            try {
                if (getId(tr) == id) {
                    mod = tr;
                    break;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (mod == null) throw new NullPointerException("Mod with such id not found");

        setSymLink(mod, link);
    }

    private static void setSymLink(Element mod, String link) {
        Element e;
        while ((e = mod.getElementsByAttributeValue("data-type", "SymLink").first()) != null)
            e.remove();

        Element td = new Element("td");
        td.attributes().add("data-type", "SymLink");
        td.text(link);
        td.appendTo(mod);
    }
}
