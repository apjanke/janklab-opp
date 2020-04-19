/*
 * @(#)RaisedBorder.java 2/12/2005
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.plaf.eclipse;

import com.jidesoft.plaf.XPUtils;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.*;

public class RaisedBorder implements Border, UIResource {
    protected Color _highlight;
    protected Color _lightHighlight;
    protected Color _shadow;
    protected Color _darkShadow;
    protected Insets _insets;

    public RaisedBorder(Color highlight, Color lightHighlight,
                        Color shadow, Color darkShadow, Insets insets) {
        _highlight = highlight;
        _lightHighlight = lightHighlight;
        _shadow = shadow;
        _darkShadow = darkShadow;
        _insets = insets;
    }

    /**
     * Returns the insets of the border.
     *
     * @param c the component for which this border insets value applies
     */
    public Insets getBorderInsets(Component c) {
        return _insets;
    }

    /**
     * Returns whether or not the border is opaque.  If the border
     * is opaque, it is responsible for filling in it's own
     * background when painting.
     */
    public boolean isBorderOpaque() {
        return true;
    }

    public void paintBorder(Component c, Graphics g, int x, int y,
                            int width, int height) {
        if (XPUtils.isXPStyleOn()) {
            if (_insets.top > 0) {
                g.setColor(_shadow);
                g.drawLine(x, y, x + width, y);
            }
            if (_insets.left > 0) {
                g.setColor(_shadow);
                g.drawLine(x, y, x, y + height);
            }
            if (_insets.bottom > 0) {
                g.setColor(_shadow);
                g.drawLine(x, y + height - 1, x + width, y + height - 1);
            }
            if (_insets.right > 0) {
                g.setColor(_shadow);
                g.drawLine(x + width - 1, y, x + width - 1, y + height);
            }
        }
        else {
            if (_insets.top > 0) {
                g.setColor(_highlight);
                g.drawLine(x, y, x + width, y);
                g.setColor(_lightHighlight);
                g.drawLine(x + 1, y + 1, x + width - 1, y + 1);
            }
            if (_insets.left > 0) {
                g.setColor(_highlight);
                g.drawLine(x, y, x, y + height);
                g.setColor(_lightHighlight);
                g.drawLine(x + 1, y + 1, x + 1, y + height - 1);
            }
            if (_insets.bottom > 0) {
                g.setColor(_shadow);
                g.drawLine(x + 1, y + height - 2, x + width - 1, y + height - 2);
                g.setColor(_darkShadow);
                g.drawLine(x, y + height - 1, x + width, y + height - 1);
            }
            if (_insets.right > 0) {
                g.setColor(_shadow);
                g.drawLine(x + width - 2, y + 1, x + width - 2, y + height - 2);
                g.setColor(_darkShadow);
                g.drawLine(x + width - 1, y, x + width - 1, y + height);
            }
        }
    }
}
