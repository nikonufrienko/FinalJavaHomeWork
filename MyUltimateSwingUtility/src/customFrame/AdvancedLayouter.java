package customFrame;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvancedLayouter implements LayoutManager {
    final List<Component> components = new ArrayList<>();
    boolean enabled = true ;
    private Getter<Dimension> prefGetter = null;
    void disable(){
        enabled = false;
    }
    void enable() {
        enabled = true;
    }
    @Override
    public void addLayoutComponent(String name, Component comp) {
        components.add(comp);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        components.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        if(prefGetter != null)
            return prefGetter.get();
        return parent.getSize();
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, 0);
    }

    @Override
    public void layoutContainer(Container parent) {
        if(enabled)
            components.forEach(it -> ((Layoutable) it).advancedLayout());

    }
    public void setPreferredSizeGetter(Getter<Dimension> getter) {
        prefGetter = getter;
    }
}
