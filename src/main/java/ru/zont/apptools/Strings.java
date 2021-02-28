package ru.zont.apptools;

import java.util.ResourceBundle;

public class Strings {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("strings", new UTF8Control());

    public static class STR {
        public static boolean containsKey(String key) {
            return bundle.containsKey(key);
        }

        public static String getString(String key) {
            if (containsKey(key))
                return bundle.getString(key);
            return String.format("{ESN:%s}", key);
        }

        public static String getString(String key, Object... args) {
            return String.format(getString(key), args);
        }
    }

    public static String getPlural(int count, String key) {
        String other = key + ".other";
        String few = key + ".few";
        String one = key + ".one";

        if (!STR.containsKey(other))
            other = key;
        if (!STR.containsKey(few))
            few = other;
        if (!STR.containsKey(one))
            one = other;

        return getPlural(count, STR.getString(one), STR.getString(few), STR.getString(other));
    }

    public static String getPlural(int count, String one, String few, String other) {
        int c = (count % 100);

        if (c == 1 || (c > 20 && c % 10 == 1))
            return String.format(one, count);
        if ((c < 10 || c > 20) && c % 10 >= 2 && c % 10 <= 4)
            return String.format(few, count);
        return String.format(other, count);
    }

    public static String trimSnippet(String original, int count) {
        int length = original.length();
        return original.substring(0, Math.min(count, length)) + "...";
    }
}
