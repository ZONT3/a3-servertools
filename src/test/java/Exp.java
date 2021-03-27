import ru.zont.musicpacker.MPMain;

import java.io.File;
import java.io.IOException;

public class Exp {
    public static void main(String[] args) throws IOException {
        MPMain.convert(new File("test.wav"), new File("testout"));
    }
}
