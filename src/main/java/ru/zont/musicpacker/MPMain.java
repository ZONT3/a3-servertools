package ru.zont.musicpacker;

import com.google.gson.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Pair;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.SinglePassFFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegError;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import ru.zont.apptools.Strings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class MPMain {
    private static FFmpeg fFmpeg;
    private static FFprobe fFprobe;
    private File workingDir;
    private MusicList list;
    private boolean destructed = false;

    private SimpleBooleanProperty working = new SimpleBooleanProperty(false);

    private static void checkInit() {
        if (fFmpeg == null) {
            try {
                fFmpeg = new FFmpeg("svtools_bin/ffmpeg.exe");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (fFprobe== null) {
            try {
                fFprobe = new FFprobe("svtools_bin/ffprobe.exe");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void convert(File in, File out) throws IOException {
        checkInit();

        FFmpegProbeResult probe = fFprobe.probe(in.getAbsolutePath());
        if (probe.hasError()) throw new ProbeError(probe.getError());

        if ("ogg".equals(probe.getFormat().format_name))
            Files.copy(in.toPath(), new FileOutputStream(out));
        else {
            String absolutePath = out.getAbsolutePath();
            if (!absolutePath.endsWith(".ogg")) absolutePath += ".ogg";
            FFmpegBuilder builder = fFmpeg.builder().addInput(in.getAbsolutePath())
                    .overrideOutputFiles(true)
                    .addOutput(absolutePath)
                    .setFormat("ogg")
                    .done();

            new SinglePassFFmpegJob(fFmpeg, builder)
                    .run();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ParseResult parseList(String listName, File provided, Consumer<Pair<Integer, Integer>> onProgress) throws IOException {
        IllegalArgumentException unknownTOF = new IllegalArgumentException("Unknown type of file");

        if (provided.isDirectory()) {
            File[] meta = provided.listFiles(file -> file.isFile() && file.getName().equals("meta.json"));
            File[] music = provided.listFiles(file -> file.isFile() && !file.getName().equals("meta.json"));

            if (meta == null || music == null) throw new IllegalArgumentException("Cannot read dir contents");

            MusicList inherit = null;
            for (File file: meta) {
                MusicList list = parseListMeta(file);
                if (list == null) continue;
                inherit = list;
                break;
            }

            workingDir = Files.createTempDirectory("musicPacker-working").toFile();
            workingDir.deleteOnExit();
            ArrayList<String> errors = new ArrayList<>();

            list = new MusicList(inherit != null && !inherit.getDisplayName().isEmpty()
                    ? inherit.getDisplayName() : listName );

            working.set(true);
            for (int i = 0; i < music.length; i++) {
                if (onProgress != null)
                    onProgress.accept(new Pair<>(i, music.length));
                File file = music[i];

                String filename = Strings.normalizeString(removeExt(file.getName())) + ".ogg";
                File out = new File(workingDir, filename);
                try {
                    convert(file, out);
                    if (destructed) {
                        destruct();
                        return null;
                    }
                    if (!out.isFile()) throw new IllegalStateException("Cannot find converted/copied file");
                } catch (Throwable t) {
                    errors.add(file.getName() + ": " + t.toString());
                    out.delete();
                    continue;
                }

                MusicEntry ie = inherit != null
                        ? inherit.stream().filter(musicEntry -> musicEntry.getFilename().equals(filename))
                        .findAny().orElse(null)
                        : null;

                String name = ie != null && ie.getName() != null ? ie.getName() : filename.replaceAll("-", " ").replaceAll("\\.ogg", "");
                String artist = ie != null && ie.getArtist() != null ? ie.getArtist() : "";

                list.add(new MusicEntry(name, artist, filename, getDuration(out)));
            }
            working.set(false);

            ParseResult result = new ParseResult();
            result.list = list;
            result.errors = errors;

            return result;
        } else if (provided.isFile()) {
            ZipFile zipFile;
            try {
                zipFile = new ZipFile(provided, ZipFile.OPEN_READ);
            } catch (ZipException e) {
                e.printStackTrace();
                throw unknownTOF;
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            File dir = null;
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) continue;

                if (dir == null) {
                    dir = Files.createTempDirectory("musicPacker-unzipped").toFile();
                    dir.deleteOnExit();
                }

                IOUtils.copy(
                        zipFile.getInputStream(zipEntry),
                        new FileOutputStream(new File(dir, zipEntry.getName())) );
            }

            if (dir != null) {
                ParseResult res = parseList(listName, dir, onProgress);
                FileUtils.deleteQuietly(dir);
                return res;
            } else return new ParseResult(new MusicList(listName));
        } else throw unknownTOF;
    }

    private String removeExt(String filename) {
        String[] split = filename.split("\\.");
        if (split.length > 1)
            return filename.substring(0, filename.length() - split[split.length - 1].length() - 1);
        else return filename;
    }

    private static double getDuration(File file) throws IOException {
        checkInit();

        FFmpegProbeResult probe = fFprobe.probe(file.getAbsolutePath());
        if (probe.hasError()) throw new IOException(probe.error.string);

        return probe.getFormat().duration;
    }

    public String buildCfgMusic(String pathPrefix) {
        assertions();

        String cfgMusicClass = getCfgMusicClass();
        String musicClassPrefix = getMusicClassPrefix(cfgMusicClass);
        pathPrefix = toArmaPath(pathPrefix) + "\\";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            MusicEntry entry = list.get(i);
            String s =
                    "    class %s {\n" +
                    "        name=\"%s\";\n" +
                    "        sound[]={\"%s\", 1, 1};\n" +
                    "        musicClass=\"%s\";\n" +
                    "        duration=%s;\n" +
                    "    };";
            sb.append(String.format(s,
                            musicClassPrefix + (i+1),
                            entry.getArtist() + " - " + entry.getName(),
                            pathPrefix + entry.getFilename(),
                            cfgMusicClass,
                            new DecimalFormat("#.0#").format(entry.getDur()).replaceAll(",", ".")
                    ))
                    .append("\n");
        }
        return sb.toString();
    }

    private static String toArmaPath(String pathPrefix) {
        String s = pathPrefix.replaceAll("/", "\\").replaceAll("\"", "\"\"");
        while (s.endsWith("\\"))
            s = s.substring(0, s.length() - 1);
        return s;
    }

    public String buildCfgMusicClasses() {
        assertions();

        return String.format("    class %s { displayName = \"%s\"; };", getCfgMusicClass(), list.getDisplayName().replaceAll("\"", "\"\""));
    }

    private String getCfgMusicClass() {
        assertions();

        String string = Strings.normalizeString(list.getDisplayName());
        if (!Character.isAlphabetic(string.charAt(0))) string = "A" + string;
        if (Character.isLowerCase(string.charAt(0))) string = Character.toUpperCase(string.charAt(0)) + string.substring(1);
        return string;
    }

    private static String getMusicClassPrefix(String cfgMusicClass) {
        StringBuilder sb = new StringBuilder();
        boolean prevUpper = true;
        for (char c: cfgMusicClass.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (prevUpper) sb.append(Character.toLowerCase(c));
                else {
                    sb.append("_").append(Character.toLowerCase(c));
                    prevUpper = true;
                }
            } else {
                if (prevUpper) prevUpper = false;
                sb.append(c);
            }
        }
        return sb.append("_").toString();
    }

    private void updateNames() throws IOException {
        assertions();

        if (!workingDir.isDirectory()) throw new IllegalStateException("List hasn't parsed");
        for (MusicEntry entry: list) {
            File f = new File(workingDir, entry.getFilename());
            if (!f.isFile()) throw new IllegalStateException("Cannot find " + entry.getFilename());
            String assertedFilename = f.getName();
            String[] split = assertedFilename.split("\\.");
            String ext = "." + split[split.length - 1];
            String newName = Strings.normalizeString(entry.getArtist() + " - " + entry.getName()) + ext;
            if (newName.equals(assertedFilename)) continue;
            Files.move(f.toPath(), new File(workingDir, newName).toPath());
            entry.setFilename(newName);
        }
    }

    private void createMeta() throws IOException {
        assertions();

        JsonObject root = new JsonObject();
        root.add("name", new JsonPrimitive(list.getDisplayName()));
        JsonArray arr = new JsonArray();
        for (MusicEntry entry: list) {
            JsonObject e = new JsonObject();
            e.add("name", new JsonPrimitive(entry.getName()));
            e.add("artist", new JsonPrimitive(entry.getArtist()));
            e.add("filename", new JsonPrimitive(entry.getFilename()));
            arr.add(e);
        }
        root.add("list", arr);

        FileOutputStream out = new FileOutputStream(new File(workingDir, "meta.json"));
        IOUtils.write(
                root.toString(),
                out,
                StandardCharsets.UTF_8
        );
        out.close();
    }

    public void export(File out) throws IOException {
        updateNames();
        createMeta();

        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(out), StandardCharsets.UTF_8);
        File[] files = workingDir.listFiles();
        if (files == null) throw new IllegalStateException("Cannot read working dir");
        for (File fileToZip: files) {
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) zipOut.write(bytes, 0, length);
            fis.close();
        }
        zipOut.close();
    }

    /**
     * Faster than {@link MPMain#export(File)}, moves result to the dir, not zip
     * <b>WARNING</b> destructs parser when returns
     * @param outDir output directory. If contents are present, then creates a new subdir
     * @throws IOException if process failed
     */
    public void exportDir(File outDir) throws IOException {
        updateNames();
        createMeta();

        if (outDir.isDirectory()) {
            String[] list = outDir.list();
            if (list == null) throw new RuntimeException("Bad dir");
            if (list.length > 0) {
                outDir = new File(outDir, this.list.getDisplayName());
                if (!outDir.mkdir()) throw new RuntimeException("Cannot create subdir. Try to create an empty one first");
            }
        } else if (!outDir.exists())
            if (!outDir.mkdirs()) throw new RuntimeException("Cannot create dir");

        File[] files = workingDir.listFiles();
        assert files != null;
        for (File file: files)
            Files.move(file.toPath(), new File(outDir, file.getName()).toPath());
        destruct();
    }

    private static MusicList parseListMeta(File file) {
        try {
            JsonObject list = new JsonParser().parse(new FileReader(file)).getAsJsonObject();
            JsonElement eListName = list.get("name");
            JsonArray array = list.getAsJsonArray("list");
            MusicList res = new MusicList(eListName != null && eListName.isJsonPrimitive() ? eListName.getAsString() : "");
            for (JsonElement e: array) {
                if (!e.isJsonObject()) continue;
                JsonObject obj = e.getAsJsonObject();
                JsonElement eName = obj.get("name");
                JsonElement eArtist = obj.get("artist");
                JsonElement eFilename = obj.get("filename");
                String name = eName         != null && eName.isJsonPrimitive()     ? eName.getAsString()     : null;
                String artist = eArtist     != null && eArtist.isJsonPrimitive()   ? eArtist.getAsString()   : null;
                String filename = eFilename != null && eFilename.isJsonPrimitive() ? eFilename.getAsString() : null;
                res.add(new MusicEntry(name, artist, filename, 0.0));
            }
            return res;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isWorking() {
        return working.get();
    }

    public SimpleBooleanProperty workingProperty() {
        return working;
    }

    public void destruct() {
        if (workingDir != null && workingDir.isDirectory())
            FileUtils.deleteQuietly(workingDir);
        destructed = true;
    }

    private void assertions() {
        if (destructed) throw new IllegalStateException("This parser was destructed");
        if (workingDir == null || !workingDir.isDirectory()) throw new IllegalStateException("Working dir");
        if (list == null) throw new IllegalStateException("list not parsed");
    }

    public static class ParseResult {
        public MusicList list;
        public List<String> errors;

        public ParseResult() { }

        public ParseResult(MusicList list) {
            this.list = list;
            errors = Collections.emptyList();
        }
    }

    public static class ProbeError extends IOException {

        private final FFmpegError error;

        public ProbeError(FFmpegError error) {
            this.error = error;
        }

        public FFmpegError getError() {
            return error;
        }
    }
}
