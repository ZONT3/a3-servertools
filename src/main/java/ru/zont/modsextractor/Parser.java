package ru.zont.modsextractor;

import net.gcardone.junidecode.Junidecode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class Parser {
    public static ArrayList<Mod> parse(File file) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8");
        Elements trs = doc.body().getElementsByAttributeValue("data-type", "ModContainer");

        ArrayList<Mod> mods = new ArrayList<>();
        for (Element tr: trs) {
            String name;
            long id;
            try {
                name = tr.getElementsByAttributeValue("data-type", "DisplayName").first().text();
                id = Long.parseLong(new URI(tr.getElementsByAttributeValue("data-type", "Link").first().text()).getQuery().split("=")[1]);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            Mod mod = new Mod(name, id);
            genLinkName(mod);
            mods.add(mod);
        }
        return mods;
    }

    private static void genLinkName(Mod mod) {
        String name = Junidecode.unidecode(mod.getName())
                .replaceAll("([^\\w]|[_])", "-")
                .replaceAll("--+", "-")
                .toLowerCase();
        if (name.startsWith("-"))
            name = name.replaceFirst("-+", "");
        while (name.endsWith("-"))
            name = name.substring(0, name.length() -1);

        mod.setLink("@" + name);
    }
}
