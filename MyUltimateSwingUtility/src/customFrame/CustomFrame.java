package customFrame;

import com.sun.awt.AWTUtilities;

import javax.swing.*;
import javax.swing.border.StrokeBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


/**
 * Lasciate ogni speranza, voi ch’entrate
 */

public class CustomFrame {
    public JFrame frame;
    public boolean scrollBarIsHidden = false;
    public CustomScroller scrollerOfCustomFrame;
    private LocalExpansion expansionWindow;


    public void hideScrollBar() {
        scrollBarIsHidden = true;
        scrollerOfCustomFrame.setVisible(false);

    }

    public void showScrollBar() {
        scrollBarIsHidden = false;
        scrollerOfCustomFrame.setVisible(true);
    }

    private void showExpansionWindow() {
        expansionWindow.setBounds(frame.getBounds());
        expansionWindow.setAlwaysOnTop(true);
        expansionWindow.setVisible(true);
    }

    private void hideExpansionWindow() {
        frame.setBounds(expansionWindow.getBounds());
        frame.setAlwaysOnTop(false);
        expansionWindow.setVisible(false);

    }

    private void checkExpansionWindow() {
    }

    public UserInterface getInterface() {
        frame = getFrame();
        UserInterface u1 = new UserInterface();
        frame.setContentPane(u1);
        LocalExpansion expansionWindow = new LocalExpansion(frame);
        this.expansionWindow = expansionWindow;


        CustomScroller scroller = new CustomScroller();
        scroller.setLocation(u1.getWidth() / 2, 0);
        scroller.setBoundGetter(() -> new Rectangle((u1.getWidth() - 50) + 1, 15, 34, u1.getHeight() - 30));
        u1.add(scroller, "");
        u1.doLayout();
        //frame.addMouseWheelListener(scroller.wheelMouseListener);
        scrollerOfCustomFrame = scroller;
        return u1;
    }

    public class UserInterface extends JPanel {
        private boolean checkPos = true;
        private int xDist = 0;
        int minWidth = 100;
        int minHeight = 100;
        boolean onClose = false;
        boolean actionClosed = true;
        Polygon p = new Polygon();
        Polygon p2 = new Polygon();
        //public MainPanel mainPanel = new MainPanel();

        class MainPanel extends JPanel implements Layoutable {
            public Runner boundsSetter;

            public void advancedLayout() {
                boundsSetter.run();
            }
        }

        class AdvancedButton extends JComponent implements Layoutable {
            Runner layouter;
            Runner actionDD;

            AdvancedButton() {
                this.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (expansionWindow.isShowing())
                            hideExpansionWindow();
                        super.mouseReleased(e);
                    }
                });
            }

            @Override
            public void advancedLayout() {
                layouter.run();
            }

            public void setDragAction(Runner actionD) {
                actionDD = actionD;
                this.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        actionDD.run();
                        super.mouseDragged(e);

                    }
                });
            }
        }

        private  AdvancedButton b1, b2, b3;


        @Override
        public void paintComponents(Graphics g) {
            super.paintComponents(g);
        }

        public UserInterface() {

            this.setLayout(new AdvancedLayouter());
            b1 = new AdvancedButton();
            b2 = new AdvancedButton();
            b3 = new AdvancedButton();
            b1.setDragAction(() -> {
                if (!expansionWindow.isShowing())
                    showExpansionWindow();
                checkExpansionWindow();
                Point p2 = MouseInfo.getPointerInfo().getLocation();
                if (!(p2.x - expansionWindow.getLocation().x + 10 < minWidth)) {
                    expansionWindow.setSize(p2.x - expansionWindow.getLocation().x + 10, expansionWindow.getSize().height);
                }
            });
            b2.setDragAction(() -> {

                if (checkPos) {
                    checkPos = false;
                    xDist = MouseInfo.getPointerInfo().getLocation().x - frame.getLocation().x;
                }
                if (!expansionWindow.isShowing())
                    showExpansionWindow();
                checkExpansionWindow();
                expansionWindow.setLocation(MouseInfo.getPointerInfo().getLocation().x - xDist, MouseInfo.getPointerInfo().getLocation().y - 5);
            });
            b2.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    checkPos = true;
                    super.mouseReleased(e);
                    if (expansionWindow.getY() <= 0) {
                        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    }
                }
            });
            b3.setDragAction(() -> {
                if (!expansionWindow.isShowing())
                    showExpansionWindow();
                checkExpansionWindow();
                int newHeight = MouseInfo.getPointerInfo().getLocation().y - expansionWindow.getLocation().y + 10;
                if (!(newHeight < minHeight)) {
                    expansionWindow.setSize(expansionWindow.getSize().width, MouseInfo.getPointerInfo().getLocation().y - expansionWindow.getLocation().y + 10);
                }
            });
            b1.setBackground(new Color(0, 0, 0, 0));
            b1.layouter = () -> b1.setBounds(frame.getWidth() - 15, 30, 15, frame.getHeight() - 60);
            b2.layouter = () -> b2.setBounds(30, 0, frame.getWidth() - 60, 20);
            b3.layouter = () -> b3.setBounds(30, frame.getHeight() - 10, frame.getWidth() - 60, 10);
            b1.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            b3.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            this.add(b1, "");
            this.add(b2, "");
            this.add(b3, "");
            b1.setOpaque(true);
            UserInterface link = this;
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                }
            });

            CloseButt closeButton = new CloseButt() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    if (onClose)
                        g2.setColor(Color.RED);
                    else
                        g2.setColor(new Color(144, 13, 13));
                    Polygon closePoly = getClosePolygon();
                    g2.fillPolygon(closePoly);
                    g2.setColor(Color.BLACK);
                    g2.draw(closePoly);
                }
            };
            closeButton.boundsGetter = () -> {
                return new Rectangle(this.getWidth() - 30, 0, 30, 30);
            };
            this.addLayoutable(closeButton);
            closeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (closeButton.getClosePolygon().contains(e.getPoint()))
                        System.exit(0);

                    super.mouseClicked(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (onClose) {
                        actionClosed = true;
                        onClose = false;
                        closeButton.repaint();
                    }
                    super.mouseExited(e);
                }
            });
            closeButton.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (closeButton.getClosePolygon().contains(e.getPoint())) {
                        onClose = true;
                        closeButton.repaint();
                    }
                    super.mouseMoved(e);

                }
            });
        }

        class CloseButt extends JComponent implements Layoutable {
            Getter<Rectangle> boundsGetter;
            Polygon getClosePolygon(){
                int width = getWidth();
                int w = width - 1;
                int sp = 10;
                int a = 15;
                return new Polygon(new int[]{w - a - sp, w - sp, w, w, w - a, w - a - sp}, new int[]{0, 0, sp, sp + a, sp + a, a}, 6);
            }
            @Override
            public void advancedLayout() {
                this.setBounds(boundsGetter.get());
            }
        }


        public void addLayoutable(Component comp) {
            super.add(comp, "");
        }

        protected void paintComponent(Graphics g) {

            int height = getHeight();
            int width = getWidth();

            int h = height - 1;
            int w = width - 1;
            int sp = 10;
            int a = 15;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setStroke(new BasicStroke(0.3f));
            g.setColor(new Color(101, 101, 116, 255));
            p = new Polygon(new int[]{0, 0, sp, w - sp, w, w, w - sp, sp},
                    new int[]{h - sp, sp, 0, 0, sp, h - sp, h, h}, 8);
            g2.fill(p);
            g2.setColor(new Color(63, 63, 78));
            p2 = new Polygon(new int[]{a, a, a + sp, w - sp - a, w - a, w - a, w - sp - a, sp + a},
                    new int[]{h - sp - a, sp + a, a, a, sp + a, h - sp - a, h - a, h - a}, 8);
            g2.fill(p2);
        }



    }


    public static class CustomScroller extends JComponent implements Layoutable {

        boolean scrollerSelected = false;
        Polygon bar = new Polygon();
        Polygon first = new Polygon();
        Polygon second = new Polygon();
        public int yp = 0;// 0 ... 1000
        public int paintedPos = 0;
        double opacity = 1.0;
        private Getter<Rectangle> boundGetter;
        private boolean scIsSet = false;
        private Runner scEvent;
        private double scrollMultiplier = 1.0;
        public boolean paintBackground = false;
        /*
        public void update(int value){
            yp = value;
            scEvent.run();
            repaint();
        };*/

        public void setScrollMultiplier(double scrollMultiplier) {
            this.scrollMultiplier = scrollMultiplier;
        }

        private void repaintIfNeed() {
            if (5 + yp * getHeight() / 1000 != paintedPos) {
                repaint();
            }
        }

        public void setBoundGetter(Getter<Rectangle> getter) {
            boundGetter = getter;
        }

        @Override
        public void advancedLayout() {
            this.setBounds(boundGetter.get());
        }

        private CustomScroller link = this;
        public MouseWheelListener wheelMouseListener = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int newYp = link.yp + (int) (e.getPreciseWheelRotation() * 10.0 * (scrollMultiplier + 0.1));
                if (newYp >= 0 && newYp < 1000) link.yp = newYp;
                else if (newYp < 0) link.yp = 0;
                else link.yp = 1000;
                super.mouseWheelMoved(e);
                repaintIfNeed();
                if (scIsSet)
                    scEvent.run();

            }
        };

        public CustomScroller() {
            super();
            this.addMouseWheelListener(wheelMouseListener);
            this.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (scrollerSelected || second.contains(e.getX(), e.getX())) {
                        if (e.getY() - 15 > 0 && e.getY() < getHeight() - 30)
                            yp = (e.getY() - 15) * 1000 / (getHeight() - 45);
                        else if (e.getY() - 15 <= 0) yp = 0;
                        else yp = 1000;
                        scrollerSelected = true;
                        repaintIfNeed();
                        if (scIsSet)
                            scEvent.run();
                    }
                    super.mouseClicked(e);
                }
            });
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {

                    if (bar.contains(e.getX(), e.getY()) && requestFocusInWindow()) {
                        scrollerSelected = true;
                        link.repaint();
                    } else if (second.contains(e.getX(), e.getX()) && e.getY() - 15 > 0 && e.getY() < getHeight() - 30) {
                        yp = (e.getY() - 15) * 1000 / (getHeight() - 45);
                        repaintIfNeed();
                        if (scIsSet)
                            scEvent.run();
                    }
                    super.mouseClicked(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (scrollerSelected) {
                        scrollerSelected = false;
                        repaint();
                    }
                    super.mouseReleased(e);
                }
            });
        }

        public void setScrollerEvent(Runner event) {
            scIsSet = true;
            scEvent = event;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int h = this.getHeight();
            int w = this.getWidth();
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(0.5f));
            if (paintBackground) {
                g2.setColor(getBackground());
                g2.fill(new Rectangle(0, 0, getWidth(), getHeight()));
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            first = new Polygon(new int[]{0, 25, w - 1, w - 1, 25, 0}, new int[]{0, 0, 10, h - 11, h - 1, h - 1}, 6);
            second = new Polygon(new int[]{5, w - 6, w - 6, 5}, new int[]{5, 5 + 10, h - 15, h - 5}, 4);
            g2.setColor(new Color(101, 101, 116, (int) (255 * opacity)));
            g2.fill(first);
            g2.setColor(new Color(0, 0, 0, (int) (255 * opacity)));
            g2.draw(first);
            g2.setColor(new Color(63, 63, 78, (int) (255 * opacity)));
            g2.fill(second);
            if (!scrollerSelected)
                g2.setColor(new Color(68, 67, 67, (int) (255 * opacity)));
            else
                g2.setColor(new Color(78, 77, 77, 255));
            bar = new Polygon(new int[]{5, w - 6, w - 6, 5}, new int[]{5 + yp * (getHeight() - 46) / 1000, 15 + yp * (getHeight() - 46) / 1000, 30 + yp * (getHeight() - 46) / 1000, 40 + yp * (getHeight() - 46) / 1000}, 4);
            paintedPos = 5 + yp * getHeight() / 1000;
            g2.fillPolygon(bar);
            g2.setColor(new Color(0, 0, 0, (int) (255 * opacity)));

            g2.drawPolygon(bar);
            super.paintComponent(g2);
        }
    }

    static JFrame getFrame() {
        JFrame frame = new LocalFrame();
        return frame;
    }


    private static class LocalFrame extends JFrame {
        @Override
        public Dimension getPreferredSize() {
            return getSize();
        }

        public LocalFrame() {
            super();
            JFrame link = this;
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int height = link.getHeight();
                    int width = link.getWidth();
                    int h = height - 1;
                    int w = width - 1;
                    int sp = 10;
                    link.setShape(new Polygon(new int[]{0, 0, sp, w - sp, w, w, w - sp, sp},
                            new int[]{h - sp, sp, 0, 0, sp, h - sp, h, h}, 8));
                    super.componentResized(e);
                }
            });
            this.setUndecorated(true);
            this.setBackground(new Color(63, 63, 78));
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension dimension = toolkit.getScreenSize();
            this.setTitle("Sagittarius");
            Container pane = this.getContentPane();
            this.setSize(dimension.width / 2, dimension.height / 2);
            this.setLocation(dimension.width / 2 - this.getWidth() / 2, dimension.height / 2 - this.getHeight() / 2);
            this.setIconImage(new ImageIcon("icon.jpg").getImage());
        }
    }

    public static class LocalExpansion extends JDialog {
        @Override
        public Dimension getPreferredSize() {
            return getSize();
        }

        public LocalExpansion(JFrame parent) {
            super(parent);
            JDialog link = this;
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int height = link.getHeight();
                    int width = link.getWidth();
                    int h = height - 1;
                    int w = width - 1;
                    int sp = 10;
                    link.setShape(new Polygon(new int[]{0, 0, sp, w - sp, w, w, w - sp, sp},
                            new int[]{h - sp, sp, 0, 0, sp, h - sp, h, h}, 8));
                    super.componentResized(e);
                }
            });
            this.setUndecorated(true);
            setBackground(new Color(2, 2, 3, 75));
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            super.paint(g);
            g2.setColor(new Color(255, 255, 255, 109));
            int height = this.getHeight();
            int width = this.getWidth();
            int h = height - 2;
            int w = width - 2;
            int sp = 10;
            g2.drawPolygon(new Polygon(new int[]{0, 0, sp, w - sp, w, w, w - sp, sp},
                    new int[]{h - sp, sp, 0, 0, sp, h - sp, h, h}, 8));
        }
    }

}

