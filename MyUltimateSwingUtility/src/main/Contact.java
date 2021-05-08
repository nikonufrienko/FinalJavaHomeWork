package main;

import javax.swing.*;
import java.util.List;

public class Contact {
    public String name;
    public List<Message> messages;
    public ImageIcon icon;
    public ImageIcon maxIcon;
    public String description = "";
    public Contact(String name) {
        this.name = name;
    }
}
