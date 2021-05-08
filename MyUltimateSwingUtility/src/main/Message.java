package main;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Message {
    String text;
    List<Addition> additions = new ArrayList<>();
    Contact author;
    public boolean isNewsEnded = false;
    public static class Addition{
        public static enum TypeOfAddition{
            IMAGE,
            SOUND_RECORDING,
            VIDEO,
        }
        ImageIcon valueImage;
        TypeOfAddition type;

        public Addition(ImageIcon image){
            this.type = TypeOfAddition.IMAGE;
            valueImage = image;
        }
    }
}
