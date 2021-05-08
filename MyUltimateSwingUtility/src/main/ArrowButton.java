package main;

import customFrame.Runner;

import java.awt.*;
import java.awt.event.*;

public class ArrowButton extends CLabel {

    Polygon shape;
    boolean inActive = false;
    Direction direction = Direction.LEFT;


    Runner action;

    public void setAction(Runner act) {
        this.action = act;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    enum Direction {
        RIGHT,
        LEFT,
    }

    public ArrowButton(String text) {
        super(text);
        this.setBackground(new Color(238, 237, 237));
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                computeShape();
                super.componentResized(e);
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (shape.contains(e.getPoint())) {
                    if (!inActive) {
                        inActive = true;
                        repaint();
                    }
                } else if (inActive) {

                    inActive = false;
                    repaint();
                }
                super.mouseMoved(e);
            }
        });
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                action.run();
                super.mouseClicked(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {

                inActive = false;
                repaint();
            }
        });
    }

    void computeShape() {
        int h = this.getHeight() - 1;
        int w = this.getWidth() - 1;
        int a = (w + h) / 30;
        if (a >= w / 2 || a >= h / 2)
            a = Math.min(w / 2, h / 2);
        if (direction == Direction.RIGHT)
            shape = new Polygon(new int[]{0, 0, a, w - a, w, w - 2 * a, a}, new int[]{h - a, a, 0, 0, a, h, h,}, 7);
        else if (direction == Direction.LEFT)
            shape = new Polygon(new int[]{0, a, w - a, w, w, w - a, 2 * a}, new int[]{a, 0, 0, a, h - a, h, h}, 7);
        ;
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        computeShape();

        //g2.setColor(Color.WHITE);
        //g2.drawPolygon(shape);
        if (inActive) g2.setColor(new Color(94, 90, 90));
        else g2.setColor(new Color(75, 75, 75));
        g2.fillPolygon(shape);
    }

}
