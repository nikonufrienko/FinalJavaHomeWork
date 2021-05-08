package main;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CustomImage extends ImageIcon {
    FilePrototype source = null;

    public CustomImage(String filename) {
        super(filename);
        try {
            source = new FilePrototype(Paths.get(filename).getFileName().toString(), Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CustomImage(FilePrototype filePrototype) {
        super(filePrototype.data, filePrototype.name);
        source = filePrototype;
    }

    public ImageIcon toImageIcon(){
        return new ImageIcon(source.data, source.name);
    }
}
