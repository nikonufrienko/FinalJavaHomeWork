package main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

class FilePrototype {
    byte[] data;
    String name;

    FilePrototype(String name, byte[] data) {
        this.data = data;
        this.name = name;
    }

    ImageIcon toImageIcon() {
        return new CustomImage(this);
    }

    static FilePrototype fromImageIcon(ImageIcon icon) throws IOException {
        if(icon instanceof CustomImage)
            return ((CustomImage)icon).source;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        String name = String.valueOf(System.currentTimeMillis()) + ".jpg";;
        Image img = icon.getImage() ;
        BufferedImage bi = new BufferedImage(img.getWidth(null),img.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        ImageIO.write(bi, "jpg", data);
        return new FilePrototype(name, data.toByteArray());
    }
    public void saveAsFile(String dir){
        try (FileOutputStream stream = new FileOutputStream(dir + name)) {
            stream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
