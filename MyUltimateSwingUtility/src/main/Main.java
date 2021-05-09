package main;

import customFrame.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Main {
    static CustomFrame frameGlobalLink;
    static SocketOfClientNode core;
    static private boolean inConnecting = false;
    static String passwordGlobal;
    static String loginGlobal = "Вы не залогинились";
    static Map<String, Contact> contactsMapGlobal = new HashMap<>();
    static List<Contact> contactGlobalList;
    static JDialog info;

    static String voidReadServerDescription() {
        File file = new File("coreRes", "serverDescription.txt");
        if (file.exists()) {
            try {
                byte[] result = Files.readAllBytes(Paths.get(file.getPath()));
                return new String(result, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }

    static private void InitConnection(String login, String password, String confirmPassword, String ip, String port) {
        if (!inConnecting) {
            inConnecting = true;
            new Thread(() -> {
                try {
                    notifyUser("Подождите..");
                    core = new SocketOfClientNode(ip, Integer.parseInt(port));
                    if (password.equals(confirmPassword)) {
                        passwordGlobal = password;
                        loginGlobal = login;
                        if (!core.doLogin(login, password))
                            notifyUser("Ошибка авторизации!");
                    } else {
                        notifyUser("Пароли не совпадают!");
                    }
                    notifyUser("Получение списка контактов...");
                    contactListToMap(core.getContactList());
                    notifyUser("полученно: " + Main.contactsMapGlobal.keySet().size() + " контактов");
                    notifyUser("Успешное подключение!");
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    notifyUser("Превышено время ожидания!");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    notifyUser("Неверный порт!");
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    notifyUser("Неизвестная ошибка!");
                } catch (IOException e) {
                    e.printStackTrace();
                    notifyUser("Ошибка подключения!");
                } finally {
                    inConnecting = false;
                    useServerIcon();
                }
            }
            ).start();
        } else {
            notifyUser("Не нажимай так часто!");
        }
    }

    static public void saveFile(FilePrototype fp) {
        FileDialog fileDialog = new FileDialog(frameGlobalLink.frame);
        fileDialog.setFile(fp.name + ".");
        fileDialog.setMode(FileDialog.SAVE);
        fileDialog.setVisible(true);
        fp.saveAsFile(fileDialog.getDirectory());
    }

    static private void useServerIcon() {
        frameGlobalLink.frame.setIconImage(new ImageIcon(new File("coreRes", "icon.jpg").getPath()).getImage());
    }

    public static void showInfo(String text) {
        info = new JDialog(frameGlobalLink.frame);
        info.setAlwaysOnTop(true);
        info.setUndecorated(true);
        info.setBackground(new Color(90, 90, 104));
        info.setLocation(MouseInfo.getPointerInfo().getLocation());
        info.getContentPane().setLayout(new BorderLayout());
        AdvancedTextArea textArea = new AdvancedTextArea(text);
        Dimension dim = textArea.getPreferredSize();
        textArea.setHighlighter(null);
        textArea.setEnabled(false);
        info.setSize(dim.width * 2, dim.height * 2);
        textArea.setBackground(new Color(0, 0, 0, 0));
        textArea.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0)));
        info.getContentPane().add(textArea, BorderLayout.CENTER);
        info.setVisible(true);
    }

    public static void hideInfo() {
        if (info != null && info.isShowing()) {
            info.setVisible(false);
        }
    }

    static boolean sendNews(Message toSend) {
        if (core != null && loginGlobal != null && passwordGlobal != null) {
            notifyUser("Подождите...");
            try {
                boolean condition = core.sendMessage(toSend, loginGlobal, passwordGlobal);
                if (condition)
                    notifyUser("Успешно!");
                else
                    notifyUser("Узел ядра не принимает ваше сообщение!");
                return condition;
            } catch (IOException e) {
                notifyUser("Поизошла ошибка в процессе отправки сообщения!");
                e.printStackTrace();
                return false;
            }
        } else return false;
    }

    static boolean syncProfile(Contact contact) {
        if (core != null && loginGlobal != null && passwordGlobal != null) {
            notifyUser("Подождите...");
            try {
                boolean cond = core.sendChangeProfileRequest(loginGlobal, passwordGlobal, contact);
                notifyUser("Получение списка контактов...");
                contactListToMap(core.getContactList());
                return cond;
            } catch (IOException e) {
                notifyUser("Поизошла ошибка в процессе отправки!");
                e.printStackTrace();
                return false;
            }
        } else return false;
    }

    static void contactListToMap(List<Contact> contacts) {
        contactGlobalList = contacts;
        for (Contact contact : contacts) {
            contactsMapGlobal.put(contact.name, contact);
        }
    }

    static File getFile() {
        FileDialog chooser = new FileDialog(frameGlobalLink.frame);
        chooser.setVisible(true);
        File result = new File(chooser.getDirectory() + chooser.getFile());
        return result;
    }

    public static void main(String[] args) {
        CustomFrame f = new CustomFrame();
        frameGlobalLink = f;
        CustomFrame.UserInterface u1 = f.getInterface();
        CPanel mainPanel = new CPanel();
        mainPanel.setLayout(new AdvancedLayouter());
        mainPanel.boundsSetter = () -> new Rectangle(24, 24, f.frame.getWidth() - 48, f.frame.getHeight() - 48);
        u1.addLayoutable(mainPanel);
        CLabel logo = new CLabel("Sagittarius", () -> new Rectangle(0, 0, f.frame.getWidth() / 6, f.frame.getHeight() / 6)) {
            @Override
            public void advancedLayout() {
                this.setBounds(boundsSetter.get());
                label.setFont(new Font("", Font.BOLD, Math.min(this.getHeight() - 2, (int) ((float) this.getWidth() * 2) / label.getText().length())));
            }
        };
        mainPanel.addLayoutable(logo);
        // Здесь была ещё одна кнопка, но функционал сообщений я так и не успел реализовать.
        CButton news = new CButton("Лента",
                () -> new Rectangle(logo.getX(), logo.getY() + logo.getHeight(), logo.getWidth(), logo.getHeight() / 2));
        CButton myProfile = new CButton("Мой профиль",
                () -> new Rectangle(news.getX(), news.getY() + news.getHeight(), news.getWidth(), news.getHeight()));
        CButton options = new CButton("Параметры",
                () -> new Rectangle(myProfile.getX(), myProfile.getY() + myProfile.getHeight(), myProfile.getWidth(), myProfile.getHeight()));
        List<CButton> buttons = new ArrayList<>(Arrays.asList(news,/* messages*,*/ myProfile, options));
        buttons.forEach(it -> it.setBorder(BorderFactory.createLineBorder(new Color(59, 49, 49), 1)));
        Runner resetAllButt = () -> {
            Color color0 = new Color(75, 75, 75);
            buttons.forEach(it -> {
                if (it.color != color0) {
                    it.color = color0;
                    it.setBackground(it.color);
                    it.repaint();
                }
            });
        };
        mainPanel.addLayoutable(news);
        //mainPanel.addLayoutable(messages);
        mainPanel.addLayoutable(myProfile);
        mainPanel.addLayoutable(options);
        CPanel workPanel = new CPanel(() -> {
            int cx = news.getX();
            int cw = news.getWidth();
            if (!f.scrollBarIsHidden)
                return new Rectangle(cx + cw + 5, 0, f.frame.getWidth() - 80 - (cw + cx), mainPanel.getHeight());
            else return new Rectangle(cx + cw + 5, 0, f.frame.getWidth() - 55 - (cw + cx), mainPanel.getHeight());
        }) {
            @Override
            void changeContent(Component comp) {
                Component[] components = this.getComponents();
                for (Component component : components) {
                    if (component instanceof Container) {
                        ((Container) component).removeNotify();
                    }
                    if (component instanceof NewsPanel) {
                        System.out.println("removing news!");
                        ((NewsPanel) component).removeLinks();
                    }

                }
                System.gc();
                super.changeContent(comp);
                f.frame.pack();
            }
        }.alsoToInit(it -> it.setBorder(BorderFactory.createLineBorder(Color.BLACK)));
        workPanel.setLayout(new BorderLayout());
        mainPanel.addLayoutable(workPanel);
        mainPanel.doLayout();
        AdvancedTextArea intro = new AdvancedTextArea();
        intro.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0)));
        intro.setText(voidReadServerDescription());
        workPanel.changeContent(intro);
        f.frame.setVisible(true);
        options.setAction(() -> {
            f.hideScrollBar();
            resetAllButt.run();
            options.color = new Color(90, 90, 104);
            workPanel.changeContent(new OptionsPanel() {
                @Override
                void performAction() {
                    InitConnection(this.login, this.password, this.confirmPassword, this.ip, this.port);
                }
            });
            workPanel.repaint();
            workPanel.setVisible(false);
            workPanel.setVisible(true);
            f.frame.doLayout();
        });
        news.setAction(() ->
        {
            resetAllButt.run();
            CButton link = news;
            news.color = new Color(90, 90, 104);
            NewsPanel nwsPnl = new NewsPanel(f.scrollerOfCustomFrame) {
                @Override
                boolean send(Message message) {
                    SwingUtilities.invokeLater(link::performAsPressedAction);
                    return super.send(message);
                }
            };
            workPanel.changeContent(nwsPnl);
            f.scrollerOfCustomFrame.yp = 0;
            f.scrollerOfCustomFrame.repaint();
            nwsPnl.doLayout();
            nwsPnl.setVisible(false);
            SwingUtilities.invokeLater(() -> {
                nwsPnl.setVisible(true);
                nwsPnl.body.getVerticalScrollBar().setValue(0);
            });
        });
        myProfile.setAction(() ->
        {
            f.hideScrollBar();
            resetAllButt.run();
            myProfile.color = new Color(90, 90, 104);
            workPanel.changeContent(new MyProfile() {
                @Override
                boolean syncProfile(Contact newProfile) {
                    return Main.syncProfile(newProfile);
                }
            });
        });
        f.frame.pack();
        logo.setBackground(new Color(63, 63, 78));
    }

    static JDialog dialog;
    static volatile List<String> notificationStack = new ArrayList<>();

    static void notifyUser(String textValue) {
        System.out.println(textValue);
        if (dialog == null || !dialog.isShowing()) {
            dialog = new CustomFrame.LocalExpansion(frameGlobalLink.frame);
            dialog.setUndecorated(true);
            dialog.setSize(frameGlobalLink.frame.getWidth() / 4, frameGlobalLink.frame.getHeight() / 4);
            dialog.setLayout(new BorderLayout());
            dialog.setLocationRelativeTo(frameGlobalLink.frame);
            dialog.add(new CLabel(textValue).alsoToInit(it -> {
                it.setBackground(new Color(10, 10, 10, 122));
            }), BorderLayout.CENTER);
            dialog.setVisible(true);
            new Timer(50, new ActionListener() {
                Float targetOp = 1.f;
                int ttl = 15;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (ttl > 0) {
                        ttl--;
                    } else if (targetOp * 0.8f < 0.1) {
                        ((Timer) e.getSource()).stop();
                        dialog.setVisible(false);
                        if (!notificationStack.isEmpty())
                            notifyUser(notificationStack.remove(0));
                    } else {
                        targetOp *= 0.8f;
                        dialog.setOpacity(targetOp);
                    }
                }
            }).start();
        } else notificationStack.add(textValue);
    }

    public static boolean fileIsImage(File file) {
        try {
            return ImageIO.read(file) != null;
        } catch (Exception e) {
            return false;
        }
    }
}

/**
 * put this to CPanel
 **/
class ScrollablePane extends JScrollPane implements Layoutable {

    Getter<Rectangle> layouter;
    List<Runner> internal;
    CustomFrame.CustomScroller scroller;
    Getter<Integer> maxValueGetter = null;


    private Integer getMaxValue() {
        return this.getViewport().getView().getHeight();
    }

    Runner syncBackLogic = () -> {
        scroller.yp = (int) ((((double) getViewport().getViewPosition().y) / (getViewport().getViewSize().height - getHeight())) * 1000);
        scroller.repaint();
    };
    Runner scrollerEvent = () -> {
        scroller.setScrollMultiplier(Math.abs((double) scroller.getParent().getHeight()) / this.getViewport().getViewSize().height * 2);
        verticalScrollBar.setValue((int) ((float) scroller.yp * (((float) this.getViewport().getViewSize().height - this.getHeight()) / (float) 1000)));
    };

    public ScrollablePane(Component componentToScrolling, CustomFrame.CustomScroller scroller) {
        super(componentToScrolling);
        this.getVerticalScrollBar();
        this.scroller = scroller;
        this.getViewport().setBackground(new Color(67, 67, 75));
        this.setBorder(BorderFactory.createLineBorder(new Color(108, 106, 106), 0));
        JScrollBar verticalScrollBar = this.getVerticalScrollBar();
        verticalScrollBar.setPreferredSize(new Dimension(0, 0));
        this.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroller.setScrollerEvent(scrollerEvent);
        this.addMouseWheelListener(new MouseWheelListener() {
            final int defaultSpeed = 4;
            final int increment = 7;
            int currSpeed = defaultSpeed;

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int tp = (int) (e.getPreciseWheelRotation() * currSpeed + getViewport().getViewPosition().y);
                if (tp > 0 && tp < getMaxValue() - getHeight()) {
                    getViewport().setViewPosition(new Point(0, tp));
                    syncBack();
                } else if (tp > getMaxValue() - getHeight()) {
                    getViewport().setViewPosition(new Point(0, getMaxValue() - getHeight()));
                    syncBack();
                } else if (tp < 0) {
                    getViewport().setViewPosition(new Point(0, 0));
                    syncBack();
                }
                currSpeed += increment;
                new Timer(400, event -> {
                    ((Timer) event.getSource()).stop();
                    if (currSpeed > defaultSpeed)
                        currSpeed -= increment;
                    else
                        currSpeed = defaultSpeed;
                }).start();
            }
        });

        ScrollablePane link = this;
        componentToScrolling.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                syncBack();
            }

        });
        getViewport().addChangeListener(e -> syncBack());


    }

    public void syncBack() {
        syncBackLogic.run();

    }

    public ScrollablePane(Component componentToScrolling, CustomFrame.CustomScroller scroller, Getter<Rectangle> bounds) {
        this(componentToScrolling, scroller);
        layouter = bounds;
    }

    /* ToDelete*/
    static JPanel createTable(List<String> contents) {

        JPanel table = new JPanel();
        table.setBackground(new Color(67, 67, 75));
        table.setLayout(new GridLayout(contents.size(), 1, 5, 5));
        for (String content : contents) {
            table.add(new ContentElement(content));
        }
        return table;
    }

    @Override
    public void advancedLayout() {
        this.setBounds(layouter.get());
    }

}

class ContentElement extends JPanel {
    AdvancedTextArea contentValue;
    CPanel additionsPanel;

    public ContentElement(String content) {
        super();
        this.setBackground(new Color(67, 67, 75));
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createLineBorder(new Color(1, 1, 1), 1));
        contentValue = new AdvancedTextArea(content);
        contentValue.setEditable(false);
        contentValue.setFont(new Font("", Font.PLAIN, 14));
        this.add(contentValue, BorderLayout.CENTER);
    }

    public ContentElement(Message msg) {
        super();
        this.setBackground(new Color(67, 67, 75));
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createLineBorder(new Color(1, 1, 1), 1));
        contentValue = new AdvancedTextArea(msg.text);
        contentValue.setEditable(false);
        contentValue.setFont(new Font("", Font.PLAIN, 14));
        this.add(contentValue, BorderLayout.CENTER);
        if (!msg.additions.isEmpty()) {
            List<CImagePanel> stack = new ArrayList<>();
            additionsPanel = new CPanel() {
                @Override
                public Dimension getPreferredSize() {
                    CImagePanel last = stack.get(stack.size() - 1);
                    return new Dimension(super.getPreferredSize().width, last.getHeight() + last.getY());
                }
            };
            additionsPanel.setLayout(new AdvancedLayouter());
            for (Message.Addition addition : msg.additions) {
                if (addition.type == Message.Addition.TypeOfAddition.IMAGE) {
                    CImagePanel img = new CImagePanel(addition.valueImage) {
                        @Override
                        public Dimension getPreferredSize() {
                            int width = this.getParent().getWidth();
                            if (width * addition.valueImage.getIconHeight() / addition.valueImage.getIconWidth() <= width)
                                return new Dimension(width, width * addition.valueImage.getIconHeight() / addition.valueImage.getIconWidth());
                            else
                                return new Dimension(addition.valueImage.getIconWidth() * width / addition.valueImage.getIconHeight(), width);
                        }
                    };
                    if (!stack.isEmpty()) {
                        CImagePanel prev = stack.get(stack.size() - 1);
                        img.boundsSetter = () -> {
                            Dimension size = img.getPreferredSize();
                            int w = size.width;
                            int h = size.height;
                            int w0 = additionsPanel.getWidth();
                            return new Rectangle((w0 - w) / 2, 5 + prev.getY() + prev.getHeight(), w, h);
                        };
                    } else {
                        img.boundsSetter = () -> {
                            Dimension size = img.getPreferredSize();
                            int w = size.width;
                            int h = size.height;
                            int w0 = additionsPanel.getWidth();
                            return new Rectangle((w0 - w) / 2, 5, w, h);
                        };
                    }
                    additionsPanel.addLayoutable(img);
                    stack.add(img);
                    if (addition.valueImage instanceof CustomImage) {
                        CustomImage localImg = (CustomImage) addition.valueImage;
                        img.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {
                                if (e.getButton() == MouseEvent.BUTTON1) {
                                    try {
                                        Main.saveFile(FilePrototype.fromImageIcon(localImg));
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }
                                }
                                super.mousePressed(e);
                            }
                        });
                    }
                }
            }
            this.add(additionsPanel, BorderLayout.SOUTH);
        }
    }

}

/**
 * Это пришлось вырезать из программы, т.к. реализовать полностью это я не успел.
 */
class MsgPanel extends JPanel {
    List<String> messages = new ArrayList<>();
    ScrollablePane scrollablePane;
    AdvRun<String> sendAction = this::addMsg;
    CSendPanel sender;

    public MsgPanel(CustomFrame.CustomScroller scroller, Contact contact, Runner exitAct) {
        super();
        this.setLayout(new AdvancedLayouter());
        CPanel contactPnl = new CPanel(() -> new Rectangle(0, 0, this.getWidth(), 50));
        CPanel upperPanel = new CPanel(() -> new Rectangle(0, 50 + 1, this.getWidth(), 3 * this.getHeight() / 4 - 50));
        CPanel lowerPanel = new CPanel(() -> {
            if (this.getHeight() - (3 * this.getHeight() / 4) - 1 < 46)
                return new Rectangle(0, 3 * this.getHeight() / 4 + 1, this.getWidth(), 48);
            return new Rectangle(0, 3 * this.getHeight() / 4 + 1, this.getWidth(), this.getHeight() - (3 * this.getHeight() / 4) - 1);
        });
        contactPnl.setLayout(new BorderLayout());
        JPanel iconAndOutPnl = new JPanel();
        iconAndOutPnl.setBackground(new Color(50, 42, 42));
        iconAndOutPnl.setLayout(new BorderLayout());
        CButton outButt = new CButton("<<");
        outButt.setAction(exitAct::run);
        iconAndOutPnl.add(outButt, BorderLayout.WEST);
        if (contact.icon != null)
            iconAndOutPnl.add(SelectionPanel.createIcon(new ImageIcon(contact.icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH)), new Dimension(50, 50), 3.f, 2), BorderLayout.EAST);
        contactPnl.add(iconAndOutPnl, BorderLayout.WEST);
        CLabel nameLabel = new CLabel(contact.name);
        nameLabel.setBackground(new Color(50, 42, 42));
        nameLabel.label.setFont(new Font("", Font.PLAIN, 20));
        contactPnl.add(nameLabel, BorderLayout.CENTER);
        contactPnl.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        this.add(contactPnl, "");
        this.add(upperPanel, "");
        this.add(lowerPanel, "");
        upperPanel.setLayout(new BorderLayout());
        scrollablePane = new ScrollablePane(ScrollablePane.createTable(messages), scroller);
        upperPanel.add(scrollablePane, BorderLayout.CENTER);
        this.setBackground(new Color(50, 43, 43));
        lowerPanel.setLayout(new BorderLayout());
        sender = new CSendPanel();
        lowerPanel.add(sender, BorderLayout.CENTER);
        Main.frameGlobalLink.showScrollBar();

    }

    void sendMsg(String msg) {
        this.addMsg("Вы отправили:\n" + msg);
        sendAction.run(msg);
    }

    void addMsg(String msg) {
        messages.add(msg);
        scrollablePane.getViewport().removeAll();
        //scrollablePane.getViewport().add();
    }
}

class CPanel extends JPanel implements Layoutable {
    public Getter<Rectangle> boundsSetter;

    CPanel alsoToInit(AdvRun<CPanel> task) {
        task.run(this);
        return this;
    }

    public CPanel() {
        super();
        this.setBackground(new Color(63, 63, 78));
    }

    public CPanel(Getter<Rectangle> bounds) {
        super();
        boundsSetter = bounds;
        this.setBackground(new Color(63, 63, 78));
    }

    @Override
    public void advancedLayout() {
        this.setBounds(boundsSetter.get());
        this.doLayout();
    }

    public void addLayoutable(Component comp) {
        super.add(comp, " ");
    }

    @Override
    protected void paintComponent(Graphics g) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }

    void changeContent(Component comp) {
        this.removeAll();
        this.setLayout(new BorderLayout());
        this.add(comp, BorderLayout.CENTER);

    }
}

class CLabel extends CPanel {
    JLabel label;
    protected Color color = new Color(97, 95, 95);

    public CLabel(String text) {
        super();
        this.setBackground(color);
        label = new JLabel(text, JLabel.CENTER);
        label.setForeground(new Color(238, 230, 230));
        this.setLayout(new BorderLayout());
        this.add(label, BorderLayout.CENTER);
    }

    public CLabel(String text, Getter<Rectangle> boundsSetter) {
        this(text);
        super.boundsSetter = boundsSetter;
    }

    public CLabel(String text, Getter<Rectangle> boundsSetter, Color background) {
        this(text, boundsSetter);
        color = background;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    @Override
    public void advancedLayout() {
        Rectangle rect = boundsSetter.get();
        this.setBounds(rect);
        label.setFont(new Font("", Font.BOLD, Math.min(this.getHeight(), this.getWidth() / label.getText().length())));

    }
}

class CButton extends CLabel {
    Color activeColor = new Color(94, 90, 90);
    boolean inActive = false;
    protected Runner act;

    public void performAsPressedAction() {
        act.run();
    }

    public CButton(String text) {
        super(text);
        init();
    }

    public CButton(String text, Getter<Rectangle> boundsSetter) {
        super(text, boundsSetter);
        init();
    }

    private void init() {
        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!inActive) {
                    inActive = true;
                    setBackground(activeColor);
                    repaint();
                }
                super.mouseMoved(e);
            }

        });
        color = new Color(75, 75, 75);
        activeColor = new Color(94, 90, 90);
        this.setBackground(color);
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    act.run();
                }
                super.mouseClicked(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                inActive = false;
                setBackground(color);
                repaint();
            }
        });
    }

    public CButton(String text, Getter<Rectangle> boundsSetter, Color background, Color active) {
        this(text, boundsSetter);
        color = background;
        this.setBackground(color);
        activeColor = active;
    }

    void setAction(Runner action) {
        act = action;
    }
}


class AdvancedTextArea extends JTextArea {
    private void init() {
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        this.setBackground(new Color(67, 67, 75));
        this.setForeground(Color.white);
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    }

    /*
        @Override
        public void setSize(Dimension d) {
            //this.setColumns(d.width / this.getColumnWidth());
            super.setSize(d);
        }*/
/*

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(new Rectangle(r.x, r.y, r.width - r.width % this.getColumnWidth(), r.height));
    }*/
    @Override
    public Dimension getPreferredSize() {

        return super.getPreferredSize();
    }

    public AdvancedTextArea(String text) {
        super(text);
        this.setFont(new Font("", Font.PLAIN, 12));
        init();
    }

    public AdvancedTextArea() {
        super();
        init();
        this.setFont(new Font("", Font.PLAIN, 14));
        this.setDoubleBuffered(true);
        this.setVerifyInputWhenFocusTarget(true);
        this.getCaret().setVisible(false);
    }
}

interface AdvRun<T> {
    void run(T value);

}

class SelectionPanel extends CPanel {
    public List<Contact> connections = new ArrayList<>();
    private ScrollablePane listPanel;

    void testInit() {
        if (Main.contactGlobalList != null) {
            connections = new ArrayList<>(Main.contactGlobalList);
            if (Main.contactsMapGlobal.containsKey(Main.loginGlobal)) {
                connections.remove(Main.contactGlobalList.indexOf(Main.contactsMapGlobal.get(Main.loginGlobal)));
            }

        }
    }

    CustomFrame.CustomScroller scroller;

    private void initListPanel() {
        CPanel contactsPanel = new CPanel();
        contactsPanel.setBackground(new Color(30, 30, 36));
        contactsPanel.setLayout(new AdvancedLayouter());
        for (int i = 0; i < connections.size(); i++) {
            int ind = i;
            CButton button = new CButton(connections.get(i).name, () -> {
                int width0 = this.getWidth() / 10;
                int height0 = 100;
                return new Rectangle(width0, (height0 + 5) * (ind + 1) - height0 / 2, width0 * 8, height0);
            }, new Color(50, 42, 42), new Color(59, 51, 59));

            button.add(createIcon(connections.get(i).icon, new Dimension(75, 100), 5f, 12), BorderLayout.WEST);
            button.setAction(() -> {
                showMsgPanel(ind);
            });
            contactsPanel.addLayoutable(button);
        }
        contactsPanel.setPreferredSize(new Dimension(contactsPanel.getWidth(), (connections.size() + 1) * 110));
        listPanel = new ScrollablePane(contactsPanel, scroller);
    }

    public SelectionPanel(CustomFrame.CustomScroller scroller) {
        super();
        this.scroller = scroller;
        testInit();
        initListPanel();
        this.setLayout(new BorderLayout());

        this.add(listPanel, BorderLayout.CENTER);
        Main.frameGlobalLink.showScrollBar();

    }

    void showMsgPanel(int contactIndex) {
        this.removeAll();
        MsgPanel msgPnl = new MsgPanel(scroller, connections.get(contactIndex), () -> {
            this.removeAll();
            initListPanel();
            this.add(listPanel, BorderLayout.CENTER);
            this.getParent().setVisible(false);
            this.getParent().setVisible(true);
        });
        this.add(msgPnl, BorderLayout.CENTER);
        this.getParent().setVisible(false);
        this.getParent().setVisible(true);
        //this.setVisible(true);
        this.repaint();

    }

    static public JLabel createIcon(ImageIcon iconImg, Dimension Prefsize, float fatContent, int space) {
        //ImageIcon img = new ImageIcon(iconImg.getImage().getScaledInstance(150,150,Image.SCALE_SMOOTH));
        JLabel icon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;

                g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = this.getWidth();
                int h = this.getHeight();
                int a = this.getHeight() / 10;
                ((Graphics2D) g).setStroke(new BasicStroke(fatContent + 2.f));
                g.setColor(new Color(0, 0, 0, 255));
                ((Graphics2D) g).draw((new Polygon(new int[]{2 + 2, a + 2 + 2, w - a - 2 - 2, w - 2 - 2, w - 2 - 2, w - a - 2 - 2, a + 2 + 2, 2 + 2}, new int[]{a + space, 0 + space, 0 + space, a + space, h - a - space, h - space, h - space, h - a - space}, 8)));
                g.setColor(new Color(101, 101, 116, 255));
                ((Graphics2D) g).setStroke(new BasicStroke(fatContent));
                ((Graphics2D) g).draw(new Polygon(new int[]{2 + 2, a + 2 + 2, w - a - 2 - 2, w - 2 - 2, w - 2 - 2, w - a - 2 - 2, a + 2 + 2, 2 + 2}, new int[]{a + space, 0 + space, 0 + space, a + space, h - a - space, h - space, h - space, h - a - space}, 8));
                ((Graphics2D) g).clip(new Polygon(new int[]{2 + 2, a + 2 + 2, w - a - 2 - 2, w - 2 - 2, w - 2 - 2, w - a - 2 - 2, a + 2 + 2, 2 + 2}, new int[]{a + space, 0 + space, 0 + space, a + space, h - a - space, h - space, h - space, h - a - space}, 8));
                super.paintComponent(g);
                if (iconImg != null)
                    g.drawImage(iconImg.getImage(), 0, 0, w, h, this);
                ((Graphics2D) g).setStroke(new BasicStroke(fatContent + 2.f));
                g.setColor(new Color(0, 0, 0, 255));
                ((Graphics2D) g).draw((new Polygon(new int[]{2 + 2, a + 2 + 2, w - a - 2 - 2, w - 2 - 2, w - 2 - 2, w - a - 2 - 2, a + 2 + 2, 2 + 2}, new int[]{a + space, 0 + space, 0 + space, a + space, h - a - space, h - space, h - space, h - a - space}, 8)));
                g.setColor(new Color(101, 101, 116, 255));
                ((Graphics2D) g).setStroke(new BasicStroke(fatContent));
                ((Graphics2D) g).draw(new Polygon(new int[]{2 + 2, a + 2 + 2, w - a - 2 - 2, w - 2 - 2, w - 2 - 2, w - a - 2 - 2, a + 2 + 2, 2 + 2}, new int[]{a + space, 0 + space, 0 + space, a + space, h - a - space, h - space, h - space, h - a - space}, 8));
            }
        };
        icon.setPreferredSize(Prefsize);
        icon.setSize(Prefsize);

        return icon;
    }
}

class NewsPanel extends CPanel {
    private List<CPanel> stack = new ArrayList<>();
    private CPanel nwsPnl;
    ScrollablePane body;
    Boolean inLoading = false;
    int counter = 0;
    boolean isEnded = false;

    // to free memory

    /**
     * Этим методом я пофиксил java heap space
     **/
    void removeLinks() {
        body.setViewport(null);
        body = null;
        nwsPnl = null;
        stack = null;

    }

    public void startUpdatingNews() {
        if (!isEnded && !inLoading && Main.core != null && Main.core.isActive && stack.size() > 0) {
            inLoading = true;
            int finalCounter = counter;
            new Thread(() -> {
                Message newNews = Main.core.getNews(finalCounter);
                SwingUtilities.invokeLater(() -> {
                    if (newNews != null && nwsPnl != null) {
                        if (newNews.isNewsEnded) {
                            Main.notifyUser("Новости закончились :(");
                            isEnded = true;
                        } else {
                            if (Main.contactsMapGlobal.containsKey(newNews.author.name))
                                newNews.author = Main.contactsMapGlobal.get(newNews.author.name);
                            addToStackByNews(newNews);
                            counter++;
                        }
                    }
                    inLoading = false;
                });
            }).start();
        }
    }

    boolean send(Message message) {
        return Main.sendNews(message);
    }

    private CPanel getContentPnl(Message newsValue) {
        CPanel contentPanel = new CPanel() {
        };
        contentPanel.setLayout(new BorderLayout());
        CPanel authorPanel = new CPanel();
        authorPanel.setLayout(new BorderLayout());
        authorPanel.setBorder(BorderFactory.createLineBorder(new Color(118, 104, 104, 60), 1));
        JLabel iconLabel = SelectionPanel.createIcon(newsValue.author.icon, new Dimension(75, 75), 5.f, 2);
        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Main.showInfo(newsValue.author.description);
                    super.mouseEntered(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Main.hideInfo();
                super.mouseExited(e);
            }

        });
        authorPanel.add(iconLabel, BorderLayout.WEST);
        authorPanel.add(new CPanel().alsoToInit(it -> it.setLayout(new BorderLayout())).
                alsoToInit(l -> l.setBackground(new Color(21, 21, 26)))
                .alsoToInit(it -> it.add(new CLabel(newsValue.author.name)
                        .alsoToInit(e -> ((CLabel) e).label.setFont(new Font("", Font.PLAIN, 18)))
                        .alsoToInit(l -> l.setBackground(new Color(21, 21, 26))), BorderLayout.WEST)));
        contentPanel.add(authorPanel, BorderLayout.NORTH);
        authorPanel.setBackground(new Color(21, 21, 26));
        ContentElement contentElement = new ContentElement(newsValue);
        contentPanel.add(contentElement, BorderLayout.CENTER);
        contentElement.setBackground(new Color(30, 30, 36));
        contentElement.setBorder(BorderFactory.createLineBorder(new Color(118, 104, 104, 60), 1));
        contentElement.contentValue.setFont(new Font("", Font.PLAIN, 16));
        contentElement.contentValue.setBackground(new Color(30, 30, 36));
        if (contentElement.additionsPanel != null)
            contentElement.additionsPanel.setBackground(new Color(30, 30, 36));
        contentElement.contentValue.setBorder(BorderFactory.createLineBorder(new Color(30, 30, 36), 1));
        return contentPanel;
    }

    void addToStackByNews(Message newsValue) {
        CPanel contentPanel = getContentPnl(newsValue);
        CPanel prev = stack.get(stack.size() - 1);
        contentPanel.boundsSetter = () -> new Rectangle(nwsPnl.getWidth() / 8, prev.getY() + prev.getHeight() + 20, 3 * nwsPnl.getWidth() / 4, contentPanel.getPreferredSize().height);
        stack.add(contentPanel);
        nwsPnl.addLayoutable(contentPanel);
        nwsPnl.setSize(nwsPnl.getPreferredSize());
        body.getViewport().doLayout();
        body.syncBack();

    }

    public int getMaxY() {
        if (stack.size() < 1) return 0;
        int i = stack.size() - 1;
        while (i > 0 && stack.get(i).getY() + stack.get(i).getHeight() == 0)
            i--;
        return stack.get(i).getY() + stack.get(i).getHeight();
    }

    public NewsPanel(CustomFrame.CustomScroller scroller) {
        super();
        this.setLayout(new BorderLayout());
        nwsPnl = new CPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(body.getWidth(), getMaxY());
            }
        };
        nwsPnl.setLayout(new AdvancedLayouter());
        nwsPnl.setBackground(new Color(30, 30, 36));
        NewsPanel link = this;
        CSendPanel addNews = new CSendPanel() {
            @Override
            protected boolean send(Message message) {
                return link.send(message);
            }
        };
        addNews.boundsSetter = () -> new Rectangle(nwsPnl.getWidth() / 8, 10, 3 * nwsPnl.getWidth() / 4, 100);
        nwsPnl.addLayoutable(addNews);
        stack.add(addNews);

        body = new ScrollablePane(nwsPnl, scroller);
        body.getViewport().addChangeListener(e -> {
            SwingUtilities.invokeLater(() -> {
                if (nwsPnl!= null && stack.size() <= 2 || body.getViewport().getViewPosition().y + body.getViewport().getHeight() > stack.get(stack.size() - 3).getY()) {
                    startUpdatingNews();
                }
            });
        });
        body.syncBackLogic = () -> {
            body.scroller.yp = (int) ((((double) body.getViewport().getViewPosition().y) / (getMaxY() - body.getHeight())) * 1000);
            scroller.repaint();
        };
        body.scroller.setScrollerEvent(() -> {
            body.scroller.setScrollMultiplier(Math.abs((double) body.scroller.getParent().getHeight()) / (float) getMaxY() * 2);
            body.getVerticalScrollBar().setValue((int) ((float) body.scroller.yp * (((float) getMaxY() - body.getHeight()) / (float) 1000)));
        });
        body.maxValueGetter = this::getMaxY;

        this.add(body, BorderLayout.CENTER);
        Main.frameGlobalLink.showScrollBar();
    }
}

class OptionsPanel extends CPanel {
    static private class LocalLabel extends CLabel {
        public LocalLabel(String text) {
            super(text);
            setBackground(new Color(83, 83, 88));
            super.label.setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    protected String ip;
    protected String port;
    protected String login;
    protected String password;
    protected String confirmPassword;

    void performAction() {

    }

    public OptionsPanel() {
        String loginStr = "";
        String passwordStr = "";
        String ipStr = "";
        String portStr = "";
        LocalAccountStorage storage = LocalAccountStorage.tryToLoad();
        if (storage != null) {
            loginStr = storage.login;
            passwordStr = storage.password;
            ipStr = storage.ip;
            portStr = storage.port;
        }

        this.setLayout(new AdvancedLayouter());
        this.setBackground(new Color(83, 83, 88));
        CPanel panel = new CPanel(() -> new Rectangle(this.getWidth() / 4, this.getHeight() / 4, this.getWidth() / 2, this.getHeight() / 2));
        panel.setBackground(new Color(83, 83, 88));
        this.addLayoutable(panel);
        panel.setLayout(new GridLayout(7, 2, 10, 10));
        CTextArea ipField = new CTextArea();
        ipField.textArea.setText(ipStr);
        panel.add(new LocalLabel("ip:"));
        panel.add(ipField);
        panel.add(new LocalLabel("port:"));
        CTextArea portField = new CTextArea();
        portField.textArea.setText(portStr);
        panel.add(portField);
        panel.add(new LocalLabel("login:"));
        CTextArea login = new CTextArea();
        login.textArea.setText(loginStr);
        panel.add(login);
        panel.add(new LocalLabel("password:"));
        CTextArea password = new CTextArea();
        password.textArea.setText(passwordStr);
        panel.add(password);
        panel.add(new LocalLabel("confirm password:"));
        CTextArea confirmPassword = new CTextArea();
        confirmPassword.textArea.setText(passwordStr);
        panel.add(confirmPassword);
        CButton activator = new CButton("Try connect to core", () -> new Rectangle(3 * this.getWidth() / 8, panel.getY() + panel.getHeight(), this.getWidth() / 2, this.getHeight() / 8));
        activator.setAction(() -> {
            this.ip = ipField.textArea.getText();
            this.password = password.textArea.getText();
            this.login = login.textArea.getText();
            this.port = portField.textArea.getText();
            this.confirmPassword = confirmPassword.textArea.getText();
            if (this.password.equals(this.confirmPassword)) {
                new LocalAccountStorage(this.login, this.password, this.ip, this.port).saveToFile();
            }
            performAction();
        });
        this.addLayoutable(activator);

    }
}

class CTextArea extends CPanel {
    JTextField textArea;

    public CTextArea(Getter<Rectangle> boundGetter) {
        super(boundGetter);
        this.setLayout(new BorderLayout());
        textArea = new JTextField();
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textArea.setForeground(Color.white);
        textArea.setBackground(new Color(40, 40, 43));
        this.add(textArea, BorderLayout.CENTER);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        //textArea.addListe
    }

    public CTextArea() {
        super();
        this.setLayout(new BorderLayout());
        textArea = new JTextField();
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textArea.setForeground(Color.white);
        textArea.setBackground(new Color(40, 40, 43));
        this.add(textArea, BorderLayout.CENTER);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        //textArea.addListe
        CTextArea link = this;
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, link.getHeight() / 2));
            }
        });
    }
}

class CImagePanel extends CPanel {
    private final Image image;

    public CImagePanel(ImageIcon img) {
        super();
        image = img.getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g2);
        //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);


        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
    }
}

class CTextInput extends CPanel {
    JTextArea out;

    CTextInput() {
        super();

        out = new JTextArea() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(getParent().getWidth(), super.getPreferredSize().height);
            }
        };
        out.setLineWrap(true);
        out.setWrapStyleWord(true);
        out.setBackground(new Color(21, 21, 26));
        out.setForeground(Color.CYAN);
        this.setLayout(new BorderLayout());
        CustomFrame.CustomScroller scroller2 = new CustomFrame.CustomScroller();
        scroller2.setBackground(new Color(21, 21, 26));
        scroller2.paintBackground = true;
        scroller2.setPreferredSize(new Dimension(35, 200));
        scroller2.setSize(new Dimension(35, 200));
        this.add(scroller2, BorderLayout.EAST);
        ScrollablePane textScrollPane = new ScrollablePane(out, scroller2);
        this.add(textScrollPane, BorderLayout.CENTER);
        out.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                textScrollPane.syncBack();
            }
        });

    }
}

class CSendPanel extends CPanel {
    JTextArea out;
    List<Message.Addition> additions = new ArrayList<>();

    private Message createMessage() {
        Message message = new Message();
        if (Main.contactsMapGlobal.containsKey(Main.loginGlobal))
            message.author = Main.contactsMapGlobal.get(Main.loginGlobal);
        else {
            Main.notifyUser("Вы не залогинились!");
            return null;
        }
        message.text = out.getText();
        message.additions = this.additions;
        return message;
    }

    protected boolean send(Message message) {
        return false;
    }

    public CSendPanel() {
        this.setLayout(new BorderLayout());
        CTextInput helperPanel = new CTextInput();
        out = helperPanel.out;
        ArrowButton addAddition = new ArrowButton("+");
        this.add(helperPanel, BorderLayout.CENTER);
        CButton sendButt = new CButton("отправить");
        sendButt.setAction(() -> {
            Message message = createMessage();
            if (message != null)
                if (send(message)) {
                    out.setText("");
                    additions = new ArrayList<>();
                    addAddition.label.setText("+");
                }
        });
        CPanel link = this;

        CPanel right = new CPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, link.getHeight());
            }
        };
        right.setLayout(new BorderLayout());
        right.add(sendButt, BorderLayout.CENTER);
        addAddition.setAction(() ->
                SwingUtilities.invokeLater(() -> {
                    File file = Main.getFile();
                    if (!Main.fileIsImage(file)) {
                        Main.notifyUser("Это не картинка :(");
                    } else {
                        this.additions.add(new Message.Addition(new CustomImage(file.getPath())));
                        addAddition.label.setText("+ (" + additions.size() + ")");
                    }
                }));
        addAddition.setDirection(ArrowButton.Direction.LEFT);
        addAddition.setBackground(new Color(21, 21, 26));
        addAddition.label.setFont(new Font("", Font.BOLD, 25));
        right.add(addAddition, BorderLayout.SOUTH);
        right.setBackground(new Color(21, 21, 26));
        this.add(right, BorderLayout.EAST);
    }
}

class MyProfile extends CPanel {
    ImageIcon icon;

    boolean syncProfile(Contact newProfile) {
        return false;
    }

    MyProfile() {
        super();
        this.setLayout(new BorderLayout());

        String description = "";
        if (Main.contactsMapGlobal.containsKey(Main.loginGlobal)) {
            icon = Main.contactsMapGlobal.get(Main.loginGlobal).icon;
            description = Main.contactsMapGlobal.get(Main.loginGlobal).description;
        } else icon = null;

        JLabel iconLabel = SelectionPanel.createIcon(icon, new Dimension(200, 200), 5f, 6);

        CPanel iconPnl = new CPanel();
        iconPnl.setLayout(new BorderLayout());
        iconPnl.add(iconLabel, BorderLayout.CENTER);
        iconPnl.setBackground(new Color(50, 42, 42));
        iconPnl.add(new CLabel(Main.loginGlobal).alsoToInit(it -> it.setBackground(new Color(50, 42, 42))).alsoToInit(it -> ((CLabel) it).label.setFont(new Font("", Font.PLAIN, 24))), BorderLayout.NORTH);

        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                File file = Main.getFile();
                if (!Main.fileIsImage(file))
                    Main.notifyUser("Это не картинка!");
                else {
                    icon = new CustomImage(file.getPath());
                    JLabel newIconLabel = SelectionPanel.createIcon(icon, new Dimension(200, 200), 5f, 6);
                    newIconLabel.addMouseListener(this);
                    iconPnl.remove(e.getComponent());
                    iconPnl.add(newIconLabel, BorderLayout.CENTER);
                    iconPnl.setVisible(false);
                    iconPnl.setVisible(true);
                }
                super.mousePressed(e);
            }
        });
        this.add(new CPanel().alsoToInit(it -> {
            it.setLayout(new FlowLayout());
            it.setMinimumSize(new Dimension(200, 200));
            it.add(iconPnl);
            it.setBackground(new Color(50, 42, 42));
        }), BorderLayout.NORTH);

        CPanel link = this;
        CTextInput textInput = new CTextInput() {
            @Override
            public Dimension getPreferredSize() {
                if (link.getSize().height / 2 > 100)
                    return new Dimension(link.getSize().width / 2, link.getSize().height / 2);
                else return new Dimension(link.getSize().width / 2, 100);
            }
        };
        textInput.out.setText(description);
        textInput.add(new CLabel("Описание вашего профиля:").alsoToInit(it -> {
            it.setBackground(new Color(90, 90, 104));
            ((CLabel) it).label.setFont(new Font("", Font.BOLD, 16));
            it.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 255)));
        }), BorderLayout.NORTH);
        this.add(textInput, BorderLayout.CENTER);
        this.add(new CPanel().alsoToInit(it -> {
            it.setBackground(new Color(21, 21, 26));
            it.setBorder(BorderFactory.createLineBorder(new Color(123, 122, 122, 122)));
            it.setLayout(new FlowLayout());
            it.add(new CButton("Синхронизировать") {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(3 * link.getSize().width / 8, link.getSize().height / 8);
                        }
                    }.alsoToInit(bt -> {
                        ((CButton) bt).setAction(() -> {
                            Contact contact = new Contact(Main.loginGlobal);
                            contact.description = textInput.out.getText();
                            contact.maxIcon = icon;
                            contact.icon = icon;
                            if (!syncProfile(contact))
                                Main.notifyUser("Ошибка!");
                            else
                                Main.notifyUser("OK!");
                        });
                    })
            );
        }), BorderLayout.SOUTH);

    }
}