/*
 * Copyright (c) 2017 Andreas Signer <asigner@gmail.com>
 *
 * This file is part of programmablefun.
 *
 * programmablefun is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * programmablefun is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with programmablefun.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jfree.swt;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.graphics.Transform;

/**
 * An implementation of the {@code Graphics2D} API targeting an SWT graphics
 * context.
 */
public class SWTGraphics2D extends Graphics2D {

    /** The SWT graphic composite */
    private GC gc;

    /**
     * The rendering hints.  For now, these are not used, but at least the
     * basic mechanism is present.
     */
    private RenderingHints hints;

    /**
     * A reference to the compositing rule to apply. This is necessary
     * due to the poor compositing interface of the SWT toolkit.
     */
    private java.awt.Composite composite;

    /** A HashMap to store the SWT color resources. */
    private Map colorsPool = new HashMap();

    /** A HashMap to store the SWT font resources. */
    private Map fontsPool = new HashMap();

    /** A HashMap to store the SWT transform resources. */
    private Map transformsPool = new HashMap();

    /** A List to store the SWT resources. */
    private List resourcePool = new ArrayList();

    /**
     * Creates a new instance.
     *
     * @param gc  the graphics context.
     */
    public SWTGraphics2D(GC gc) {
        super();
        this.gc = gc;
        this.hints = new RenderingHints(null);
        this.composite = AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f);
        setStroke(new BasicStroke());
    }

    /**
     * Not implemented yet - see {@link Graphics#create()}.
     *
     * @return {@code null}.
     */
    public Graphics create() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Not implemented yet - see {@link Graphics2D#getDeviceConfiguration()}.
     *
     * @return {@code null}.
     */
    public GraphicsConfiguration getDeviceConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the current value for the specified hint key, or
     * {@code null} if no value is set.
     *
     * @param hintKey  the hint key ({@code null} permitted).
     *
     * @return The hint value, or {@code null}.
     *
     * @see #setRenderingHint(RenderingHints.Key, Object)
     */
    @Override
    public Object getRenderingHint(Key hintKey) {
        return this.hints.get(hintKey);
    }

    /**
     * Sets the value for a rendering hint.  For now, this graphics context
     * ignores all hints.
     *
     * @param hintKey  the key ({@code null} not permitted).
     * @param hintValue  the value (must be compatible with the specified key).
     *
     * @throws IllegalArgumentException if {@code hintValue} is not
     *         compatible with the {@code hintKey}.
     *
     * @see #getRenderingHint(RenderingHints.Key)
     */
    @Override
    public void setRenderingHint(Key hintKey, Object hintValue) {
        this.hints.put(hintKey, hintValue);
    }

    /**
     * Returns a copy of the hints collection for this graphics context.
     *
     * @return A copy of the hints collection.
     */
    @Override
    public RenderingHints getRenderingHints() {
        return (RenderingHints) this.hints.clone();
    }

    /**
     * Adds the hints in the specified map to the graphics context, replacing
     * any existing hints.  For now, this graphics context ignores all hints.
     *
     * @param hints  the hints ({@code null} not permitted).
     *
     * @see #setRenderingHints(Map)
     */
    @Override
    public void addRenderingHints(Map hints) {
        this.hints.putAll(hints);
    }

    /**
     * Replaces the existing hints with those contained in the specified
     * map.  Note that, for now, this graphics context ignores all hints.
     *
     * @param hints  the hints ({@code null} not permitted).
     *
     * @see #addRenderingHints(Map)
     */
    @Override
    public void setRenderingHints(Map hints) {
        if (hints == null) {
            throw new NullPointerException("Null 'hints' argument.");
        }
        this.hints = new RenderingHints(hints);
    }

    /**
     * Returns the current paint for this graphics context.
     *
     * @return The current paint.
     *
     * @see #setPaint(Paint)
     */
    @Override
    public Paint getPaint() {
        // TODO: it might be a good idea to keep a reference to the color
        // specified in setPaint() or setColor(), rather than creating a
        // new object every time getPaint() is called.
        return SWTUtils.toAwtColor(this.gc.getForeground());
    }

    /**
     * Sets the paint for this graphics context.  For now, this graphics
     * context only supports instances of {@link Color} or
     * {@link GradientPaint} (in the latter case there is no real gradient
     * support, the paint used is the {@code Color} returned by
     * {@code getColor1()}).
     *
     * @param paint  the paint ({@code null} permitted, ignored).
     *
     * @see #getPaint()
     * @see #setColor(Color)
     */
    @Override
    public void setPaint(Paint paint) {
        if (paint == null) {
            return;  // to be consistent with other Graphics2D implementations
        }
        if (paint instanceof Color) {
            setColor((Color) paint);
        }
        else if (paint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) paint;
            setColor(gp.getColor1());
        }
        else {
            throw new RuntimeException("Can only handle 'Color' at present.");
        }
    }

    /**
     * Returns the current color for this graphics context.
     *
     * @return The current color.
     *
     * @see #setColor(Color)
     */
    @Override
    public Color getColor() {
        // TODO: it might be a good idea to keep a reference to the color
        // specified in setPaint() or setColor(), rather than creating a
        // new object every time getPaint() is called.
        return SWTUtils.toAwtColor(this.gc.getForeground());
    }

    /**
     * Sets the current color for this graphics context.
     *
     * @param color  the color ({@code null} permitted but ignored).
     *
     * @see #getColor()
     */
    @Override
    public void setColor(Color color) {
        if (color == null) {
            return;
        }
        org.eclipse.swt.graphics.Color swtColor = getSwtColorFromPool(color);
        this.gc.setForeground(swtColor);
        // handle transparency and compositing.
        if (this.composite instanceof AlphaComposite) {
            AlphaComposite acomp = (AlphaComposite) this.composite;
            switch (acomp.getRule()) {
                case AlphaComposite.SRC_OVER:
                    this.gc.setAlpha((int) (color.getAlpha() * acomp.getAlpha()));
                    break;
                default:
                    this.gc.setAlpha(color.getAlpha());
                    break;
            }
        }
    }

    private Color backgroundColor;

    /**
     * Sets the background color.
     *
     * @param color  the color.
     */
    @Override
    public void setBackground(Color color) {
        // since this is only used by clearRect(), we don't update the GC yet
        this.backgroundColor = color;
    }

    /**
     * Returns the background color.
     *
     * @return The background color (possibly {@code null})..
     */
    @Override
    public Color getBackground() {
        return this.backgroundColor;
    }

    /**
     * Not implemented - see {@link Graphics#setPaintMode()}.
     */
    @Override
    public void setPaintMode() {
        // TODO Auto-generated method stub
    }

    /**
     * Not implemented - see {@link Graphics#setXORMode(Color)}.
     *
     * @param color  the color.
     */
    @Override
    public void setXORMode(Color color) {
        // TODO Auto-generated method stub
    }

    /**
     * Returns the current composite.
     *
     * @return The current composite.
     *
     * @see #setComposite(Composite)
     */
    @Override
    public Composite getComposite() {
        return this.composite;
    }

    /**
     * Sets the current composite.  This implementation currently supports
     * only the {@link AlphaComposite} class.
     *
     * @param comp  the composite ({@code null} not permitted).
     */
    @Override
    public void setComposite(Composite comp) {
        if (comp == null) {
            throw new IllegalArgumentException("Null 'comp' argument.");
        }
        this.composite = comp;
        if (comp instanceof AlphaComposite) {
            AlphaComposite acomp = (AlphaComposite) comp;
            int alpha = (int) (acomp.getAlpha() * 0xFF);
            this.gc.setAlpha(alpha);
        }
    }

    /**
     * Returns the current stroke for this graphics context.
     *
     * @return The current stroke.
     *
     * @see #setStroke(Stroke)
     */
    @Override
    public Stroke getStroke() {
        return new BasicStroke(this.gc.getLineWidth(),
                toAwtLineCap(this.gc.getLineCap()),
                toAwtLineJoin(this.gc.getLineJoin()));
    }

    /**
     * Sets the stroke for this graphics context.  For now, this implementation
     * only recognises the {@link BasicStroke} class.
     *
     * @param stroke  the stroke ({@code null} not permitted).
     *
     * @see #getStroke()
     */
    @Override
    public void setStroke(Stroke stroke) {
        if (stroke == null) {
            throw new IllegalArgumentException("Null 'stroke' argument.");
        }
        if (stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) stroke;
            this.gc.setLineWidth((int) bs.getLineWidth());
            this.gc.setLineJoin(toSwtLineJoin(bs.getLineJoin()));
            this.gc.setLineCap(toSwtLineCap(bs.getEndCap()));

            // set the line style to solid by default
            this.gc.setLineStyle(SWT.LINE_SOLID);

            // apply dash style if any
            float[] dashes = bs.getDashArray();
            if (dashes != null) {
                int[] swtDashes = new int[dashes.length];
                for (int i = 0; i < swtDashes.length; i++) {
                    swtDashes[i] = (int) dashes[i];
                }
                this.gc.setLineDash(swtDashes);
            }
        }
        else {
            throw new RuntimeException(
                    "Can only handle 'Basic Stroke' at present.");
        }
    }

    /**
     * Applies the specified clip.
     *
     * @param s  the shape for the clip.
     */
    @Override
    public void clip(Shape s) {
        Path path = toSwtPath(s);
        this.gc.setClipping(path);
        path.dispose();
    }

    /**
     * Returns the clip bounds.
     *
     * @return The clip bounds.
     */
    @Override
    public Rectangle getClipBounds() {
        org.eclipse.swt.graphics.Rectangle clip = this.gc.getClipping();
        return new Rectangle(clip.x, clip.y, clip.width, clip.height);
    }

    /**
     * Sets the clipping to the intersection of the current clip region and
     * the specified rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {
        org.eclipse.swt.graphics.Rectangle clip = this.gc.getClipping();
        org.eclipse.swt.graphics.Rectangle r
                = new org.eclipse.swt.graphics.Rectangle(x, y, width, height);
        clip.intersect(r);
        this.gc.setClipping(clip);
    }

    /**
     * Returns the current clip.
     *
     * @return The current clip.
     */
    @Override
    public Shape getClip() {
        return SWTUtils.toAwtRectangle(this.gc.getClipping());
    }

    /**
     * Sets the clip region.
     *
     * @param clip  the clip.
     */
    @Override
    public void setClip(Shape clip) {
        if (clip == null) {
            return;
        }
        Path clipPath = toSwtPath(clip);
        this.gc.setClipping(clipPath);
        clipPath.dispose();
    }

    /**
     * Sets the clip region to the specified rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        this.gc.setClipping(x, y, width, height);
    }

    /**
     * Returns the current transform.
     *
     * @return The current transform.
     */
    @Override
    public AffineTransform getTransform() {
        Transform swtTransform = new Transform(this.gc.getDevice());
        this.gc.getTransform(swtTransform);
        AffineTransform awtTransform = toAwtTransform(swtTransform);
        swtTransform.dispose();
        return awtTransform;
    }

    /**
     * Sets the current transform.
     *
     * @param t  the transform.
     */
    @Override
    public void setTransform(AffineTransform t) {
        Transform transform = getSwtTransformFromPool(t);
        this.gc.setTransform(transform);
    }

    /**
     * Concatenates the specified transform to the existing transform.
     *
     * @param t  the transform.
     */
    @Override
    public void transform(AffineTransform t) {
        Transform swtTransform = new Transform(this.gc.getDevice());
        this.gc.getTransform(swtTransform);
        swtTransform.multiply(getSwtTransformFromPool(t));
        this.gc.setTransform(swtTransform);
        swtTransform.dispose();
    }

    /**
     * Applies a translation.
     *
     * @param x  the translation along the x-axis.
     * @param y  the translation along the y-axis.
     */
    @Override
    public void translate(int x, int y) {
        Transform swtTransform = new Transform(this.gc.getDevice());
        this.gc.getTransform(swtTransform);
        swtTransform.translate(x, y);
        this.gc.setTransform(swtTransform);
        swtTransform.dispose();
    }

    /**
     * Applies a translation.
     *
     * @param tx  the translation along the x-axis.
     * @param ty  the translation along the y-axis.
     */
    @Override
    public void translate(double tx, double ty) {
        translate((int) tx, (int) ty);
    }

    /**
     * Applies a rotation transform.
     *
     * @param theta  the angle of rotation.
     */
    @Override
    public void rotate(double theta) {
        AffineTransform t = getTransform();
        t.rotate(theta);
        setTransform(t);
    }

    /**
     * Not implemented - see {@link Graphics2D#rotate(double, double, double)}.
     *
     * @see java.awt.Graphics2D#rotate(double, double, double)
     */
    @Override
    public void rotate(double theta, double x, double y) {
        translate(x, y);
        rotate(theta);
        translate(-x, -y);
    }

    /**
     * Applies a scale transform.
     *
     * @param scaleX  the scale factor along the x-axis.
     * @param scaleY  the scale factor along the y-axis.
     */
    @Override
    public void scale(double scaleX, double scaleY) {
        Transform swtTransform = new Transform(this.gc.getDevice());
        this.gc.getTransform(swtTransform);
        swtTransform.scale((float) scaleX, (float) scaleY);
        this.gc.setTransform(swtTransform);
        swtTransform.dispose();
    }

    /**
     * Applies a shear transform.
     *
     * @param shearX  the x-factor.
     * @param shearY  the y-factor.
     */
    @Override
    public void shear(double shearX, double shearY) {
        transform(AffineTransform.getShearInstance(shearX, shearY));
    }

    /**
     * Draws the outline of the specified shape using the current stroke and
     * paint settings.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @see #getPaint()
     * @see #getStroke()
     * @see #fill(Shape)
     */
    @Override
    public void draw(Shape shape) {
        Path path = toSwtPath(shape);
        this.gc.drawPath(path);
        path.dispose();
    }

    /**
     * Draws a line from (x1, y1) to (x2, y2) using the current stroke
     * and paint settings.
     *
     * @param x1  the x-coordinate for the starting point.
     * @param y1  the y-coordinate for the starting point.
     * @param x2  the x-coordinate for the ending point.
     * @param y2  the y-coordinate for the ending point.
     *
     * @see #draw(Shape)
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.gc.drawLine(x1, y1, x2, y2);
    }

    /**
     * Draws the outline of the polygon specified by the given points, using
     * the current paint and stroke settings.
     *
     * @param xPoints  the x-coordinates.
     * @param yPoints  the y-coordinates.
     * @param npoints  the number of points in the polygon.
     *
     * @see #draw(Shape)
     */
    @Override
    public void drawPolygon(int [] xPoints, int [] yPoints, int npoints) {
        drawPolyline(xPoints, yPoints, npoints);
        if (npoints > 1) {
            this.gc.drawLine(xPoints[npoints - 1], yPoints[npoints - 1],
                    xPoints[0], yPoints[0]);
        }
    }

    /**
     * Draws a sequence of connected lines specified by the given points, using
     * the current paint and stroke settings.
     *
     * @param xPoints  the x-coordinates.
     * @param yPoints  the y-coordinates.
     * @param npoints  the number of points in the polygon.
     *
     * @see #draw(Shape)
     */
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int npoints) {
        if (npoints > 1) {
            int x0 = xPoints[0];
            int y0 = yPoints[0];
            int x1 = 0, y1 = 0;
            for (int i = 1; i < npoints; i++) {
                x1 = xPoints[i];
                y1 = yPoints[i];
                this.gc.drawLine(x0, y0, x1, y1);
                x0 = x1;
                y0 = y1;
            }
        }
    }

    /**
     * Draws an oval that fits within the specified rectangular region.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     *
     * @see #fillOval(int, int, int, int)
     * @see #draw(Shape)
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {
        this.gc.drawOval(x, y, width - 1, height - 1);
    }

    /**
     * Draws an arc that is part of an ellipse that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcStart  the arc starting point, in degrees.
     * @param arcAngle  the extent of the arc.
     *
     * @see #fillArc(int, int, int, int, int, int)
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int arcStart,
                        int arcAngle) {
        this.gc.drawArc(x, y, width - 1, height - 1, arcStart, arcAngle);
    }

    /**
     * Draws a rectangle with rounded corners that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcWidth  the width of the arc defining the roundedness of the
     *         rectangle's corners.
     * @param arcHeight the height of the arc defining the roundedness of the
     *         rectangle's corners.
     *
     * @see #fillRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        this.gc.drawRoundRectangle(x, y, width - 1, height - 1, arcWidth,
                arcHeight);
    }

    /**
     * Fills the specified shape using the current paint.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @see #getPaint()
     * @see #draw(Shape)
     */
    @Override
    public void fill(Shape shape) {
        Path path = toSwtPath(shape);
        // Note that for consistency with the AWT implementation, it is
        // necessary to switch temporarily the foreground and background
        // colors
        switchColors();
        this.gc.fillPath(path);
        switchColors();
        path.dispose();
    }

    /**
     * Fill a rectangle area on the SWT graphic composite.
     * The {@code fillRectangle} method of the {@code GC}
     * class uses the background color so we must switch colors.
     * @see java.awt.Graphics#fillRect(int, int, int, int)
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {
        this.switchColors();
        this.gc.fillRectangle(x, y, width, height);
        this.switchColors();
    }

    /**
     * Fills the specified rectangle with the current background color.
     *
     * @param x  the x-coordinate for the rectangle.
     * @param y  the y-coordinate for the rectangle.
     * @param width  the width.
     * @param height  the height.
     *
     * @see #fillRect(int, int, int, int)
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {
        Color bgcolor = getBackground();
        if (bgcolor == null) {
            return;  // we can't do anything
        }
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(x, y, width, height);
        setPaint(saved);
    }

    /**
     * Fills the specified polygon.
     *
     * @param xPoints  the x-coordinates.
     * @param yPoints  the y-coordinates.
     * @param npoints  the number of points.
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int npoints) {
        int[] pointArray = new int[npoints * 2];
        for (int i = 0; i < npoints; i++) {
            pointArray[2 * i] = xPoints[i];
            pointArray[2 * i + 1] = yPoints[i];
        }
        switchColors();
        this.gc.fillPolygon(pointArray);
        switchColors();
    }

    /**
     * Draws a rectangle with rounded corners that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcWidth  the width of the arc defining the roundedness of the
     *         rectangle's corners.
     * @param arcHeight the height of the arc defining the roundedness of the
     *         rectangle's corners.
     *
     * @see #drawRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        switchColors();
        this.gc.fillRoundRectangle(x, y, width - 1, height - 1, arcWidth,
                arcHeight);
        switchColors();
    }

    /**
     * Fills an oval that fits within the specified rectangular region.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     *
     * @see #drawOval(int, int, int, int)
     * @see #fill(Shape)
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {
        switchColors();
        this.gc.fillOval(x, y, width - 1, height - 1);
        switchColors();
    }

    /**
     * Fills an arc that is part of an ellipse that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcStart  the arc starting point, in degrees.
     * @param arcAngle  the extent of the arc.
     *
     * @see #drawArc(int, int, int, int, int, int)
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int arcStart,
                        int arcAngle) {
        switchColors();
        this.gc.fillArc(x, y, width - 1, height - 1, arcStart, arcAngle);
        switchColors();
    }

    /**
     * Returns the font in form of an AWT font created
     * with the parameters of the font of the SWT graphic
     * composite.
     * @return The font.
     * @see java.awt.Graphics#getFont()
     */
    @Override
    public Font getFont() {
        // retrieve the swt font description in an os indept way
        FontData[] fontData = this.gc.getFont().getFontData();
        // create a new AWT font with the appropiate data
        return SWTUtils.toAwtFont(this.gc.getDevice(), fontData[0], true);
    }

    /**
     * Set the font SWT graphic composite from the specified
     * AWT font. Be careful that the newly created SWT font
     * must be disposed separately.
     * @see java.awt.Graphics#setFont(java.awt.Font)
     */
    @Override
    public void setFont(Font font) {
        org.eclipse.swt.graphics.Font swtFont = getSwtFontFromPool(font);
        this.gc.setFont(swtFont);
    }

    /**
     * Returns the font metrics.
     *
     * @param font the font.
     *
     * @return The font metrics.
     */
    @Override
    public FontMetrics getFontMetrics(Font font) {
        return SWTUtils.DUMMY_PANEL.getFontMetrics(font);
    }

    /**
     * Returns the font render context.
     *
     * @return The font render context.
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        FontRenderContext fontRenderContext = new FontRenderContext(
                new AffineTransform(), true, true);
        return fontRenderContext;
    }

    /**
     * Draws the specified glyph vector at the location {@code (x, y)}.
     *
     * @param g  the glyph vector ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        fill(g.getOutline(x, y));
    }

    /**
     * Draws a string at {@code (x, y)}.  The start of the text at the
     * baseline level will be aligned with the {@code (x, y)} point.
     *
     * @param text  the string ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     *
     * @see #drawString(java.lang.String, float, float)
     */
    @Override
    public void drawString(String text, int x, int y) {
        drawString(text, (float) x, (float) y);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param text  the string.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(String text, float x, float y) {
        if (text == null) {
            throw new NullPointerException("Null 'text' argument.");
        }
        float fm = this.gc.getFontMetrics().getAscent();
        this.gc.drawString(text, (int) x, (int) (y - fm), true);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param iterator  the string.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        // for now we simply want to extract the chars from the iterator
        // and call an unstyled text renderer
        StringBuffer sb = new StringBuffer();
        int numChars = iterator.getEndIndex() - iterator.getBeginIndex();
        char c = iterator.first();
        for (int i = 0; i < numChars; i++) {
            sb.append(c);
            c = iterator.next();
        }
        drawString(new String(sb),x,y);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param iterator  the string.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, float x,
                           float y) {
        drawString(iterator, (int) x, (int) y);
    }

    /**
     * Returns {@code true} if the rectangle (in device space) intersects
     * with the shape (the interior, if {@code onStroke} is false,
     * otherwise the stroked outline of the shape).
     *
     * @param rect  a rectangle (in device space).
     * @param s the shape.
     * @param onStroke  test the stroked outline only?
     *
     * @return A boolean.
     */
    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        AffineTransform transform = getTransform();
        Shape ts;
        if (onStroke) {
            Stroke stroke = getStroke();
            ts = transform.createTransformedShape(stroke.createStrokedShape(s));
        } else {
            ts = transform.createTransformedShape(s);
        }
        if (!rect.getBounds2D().intersects(ts.getBounds2D())) {
            return false;
        }
        Area a1 = new Area(rect);
        Area a2 = new Area(ts);
        a1.intersect(a2);
        return !a1.isEmpty();
    }

    /**
     * Not implemented - see {@link Graphics#copyArea(int, int, int, int, int,
     * int)}.
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        // TODO Auto-generated method stub
    }

    /**
     * Draws an image with the specified transform. Note that the
     * {@code observer} is ignored in this implementation.
     *
     * @param image  the image.
     * @param xform  the transform.
     * @param obs  the image observer (ignored).
     *
     * @return {@code true} if the image is drawn.
     */
    @Override
    public boolean drawImage(Image image, AffineTransform xform,
                             ImageObserver obs) {
        AffineTransform savedTransform = getTransform();
        transform(xform);
        boolean result = drawImage(image, 0, 0, obs);
        setTransform(savedTransform);
        return result;
    }

    /**
     * Draws the image resulting from applying the {@code BufferedImageOp}
     * to the specified image at the location {@code (x, y)}.
     *
     * @param image  the image.
     * @param op  the operation.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawImage(BufferedImage image, BufferedImageOp op, int x,
                          int y) {
        BufferedImage imageToDraw = op.filter(image, null);
        drawImage(imageToDraw, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
    }

    /**
     * Draws an SWT image with the top left corner of the image aligned to the
     * point (x, y).
     *
     * @param image  the image.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    public void drawImage(org.eclipse.swt.graphics.Image image, int x, int y) {
        this.gc.drawImage(image, x, y);
    }

    /**
     * Draws a rendered image.
     *
     * @param image  the rendered image.
     * @param xform  the transform.
     */
    @Override
    public void drawRenderedImage(RenderedImage image, AffineTransform xform) {
        BufferedImage bi = convertRenderedImage(image);
        drawImage(bi, xform, null);
    }

    /**
     * Converts a rendered image to a {@code BufferedImage}.  This utility
     * method has come from a forum post by Jim Moore at:
     * <p>
     * <a href="http://www.jguru.com/faq/view.jsp?EID=114602">
     * http://www.jguru.com/faq/view.jsp?EID=114602</a>
     *
     * @param img  the rendered image.
     *
     * @return A buffered image.
     */
    private static BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster,
                isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    /**
     * Draws the renderable image.
     *
     * @param image  the renderable image.
     * @param xform  the transform.
     */
    @Override
    public void drawRenderableImage(RenderableImage image,
                                    AffineTransform xform) {
        RenderedImage ri = image.createDefaultRendering();
        drawRenderedImage(ri, xform);
    }

    /**
     * Draws an image with the top left corner aligned to the point (x, y).
     *
     * @param image  the image.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param observer  ignored here.
     *
     * @return {@code true} if the image has been drawn.
     */
    @Override
    public boolean drawImage(Image image, int x, int y,
                             ImageObserver observer) {
        int w = image.getWidth(observer);
        if (w < 0) {
            return false;
        }
        int h = image.getHeight(observer);
        if (h < 0) {
            return false;
        }
        return drawImage(image, x, y, w, h, observer);
    }

    /**
     * Draws an image with the top left corner aligned to the point (x, y),
     * and scaled to the specified width and height.
     *
     * @param image  the image.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width for the rendered image.
     * @param height  the height for the rendered image.
     * @param observer  ignored here.
     *
     * @return {@code true} if the image has been drawn.
     */
    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height,
                             ImageObserver observer) {
        ImageData data = SWTUtils.convertAWTImageToSWT(image);
        if (data == null) {
            return false;
        }
        org.eclipse.swt.graphics.Image im = new org.eclipse.swt.graphics.Image(
                this.gc.getDevice(), data);
        org.eclipse.swt.graphics.Rectangle bounds = im.getBounds();
        this.gc.drawImage(im, 0, 0, bounds.width, bounds.height, x, y, width,
                height);
        im.dispose();
        return true;
    }

    /**
     * Draws an image.
     *
     * @param image ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param bgcolor  the background color.
     * @param observer  an image observer.
     *
     * @return A boolean.
     */
    @Override
    public boolean drawImage(Image image, int x, int y, Color bgcolor,
                             ImageObserver observer) {
        int w = image.getWidth(null);
        if (w < 0) {
            return false;
        }
        int h = image.getHeight(null);
        if (h < 0) {
            return false;
        }
        return drawImage(image, x, y, w, h, bgcolor, observer);
    }

    /**
     * Draws an image to the rectangle {@code (x, y, w, h)} (scaling it if
     * required), first filling the background with the specified color.  Note
     * that the {@code observer} is ignored.
     *
     * @param image  the image.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param bgcolor  the background color ({@code null} permitted).
     * @param observer  ignored.
     *
     * @return {@code true} if the image is drawn.
     */
    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height,
                             Color bgcolor, ImageObserver observer) {
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(x, y, width, height);
        setPaint(saved);
        return drawImage(image, x, y, width, height, observer);
    }

    /**
     * Draws part of an image (defined by the source rectangle
     * {@code (sx1, sy1, sx2, sy2)}) into the destination rectangle
     * {@code (dx1, dy1, dx2, dy2)}.  Note that the {@code observer}
     * is ignored.
     *
     * @param image  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1  the x-coordinate for the top left of the source.
     * @param sy1  the y-coordinate for the top left of the source.
     * @param sx2  the x-coordinate for the bottom right of the source.
     * @param sy2  the y-coordinate for the bottom right of the source.
     *
     * @return {@code true} if the image is drawn.
     */
    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        int w = dx2 - dx1;
        int h = dy2 - dy1;
        BufferedImage img2 = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        g2.drawImage(image, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
        return drawImage(img2, dx1, dy1, null);
    }

    /**
     * Draws part of an image (defined by the source rectangle
     * {@code (sx1, sy1, sx2, sy2)}) into the destination rectangle
     * {@code (dx1, dy1, dx2, dy2)}.  The destination rectangle is first
     * cleared by filling it with the specified {@code bgcolor}. Note that
     * the {@code observer} is ignored.
     *
     * @param image  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1  the x-coordinate for the top left of the source.
     * @param sy1  the y-coordinate for the top left of the source.
     * @param sx2  the x-coordinate for the bottom right of the source.
     * @param sy2  the y-coordinate for the bottom right of the source.
     * @param bgcolor  the background color ({@code null} permitted).
     * @param observer  ignored.
     *
     * @return {@code true} if the image is drawn.
     */
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2, Color bgcolor,
                             ImageObserver observer) {
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
        setPaint(saved);
        return drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                observer);
    }

    /**
     * Releases resources held by this instance (but note that the caller
     * must dispose of the 'GC' passed to the constructor).
     *
     * @see java.awt.Graphics#dispose()
     */
    @Override
    public void dispose() {
        // we dispose resources we own but user must dispose gc
        disposeResourcePool();
    }

    /**
     * Add given SWT resource to the resource pool. All resources added
     * to the resource pool will be disposed when {@link #dispose()} is called.
     *
     * @param resource the resource to add to the pool.
     * @return the SWT {@code Resource} just added.
     */
    private Resource addToResourcePool(Resource resource) {
        this.resourcePool.add(resource);
        return resource;
    }

    /**
     * Dispose the resource pool.
     */
    private void disposeResourcePool() {
        for (Iterator it = this.resourcePool.iterator(); it.hasNext();) {
            Resource resource = (Resource) it.next();
            resource.dispose();
        }
        this.fontsPool.clear();
        this.colorsPool.clear();
        this.transformsPool.clear();
        this.resourcePool.clear();
    }

    /**
     * Internal method to convert a AWT font object into
     * a SWT font resource. If a corresponding SWT font
     * instance is already in the pool, it will be used
     * instead of creating a new one. This is used in
     * {@link #setFont()} for instance.
     *
     * @param font The AWT font to convert.
     * @return The SWT font instance.
     */
    private org.eclipse.swt.graphics.Font getSwtFontFromPool(Font font) {
        org.eclipse.swt.graphics.Font swtFont = (org.eclipse.swt.graphics.Font)
                this.fontsPool.get(font);
        if (swtFont == null) {
            swtFont = new org.eclipse.swt.graphics.Font(this.gc.getDevice(),
                    SWTUtils.toSwtFontData(this.gc.getDevice(), font, true));
            addToResourcePool(swtFont);
            this.fontsPool.put(font, swtFont);
        }
        return swtFont;
    }

    /**
     * Internal method to convert a AWT color object into
     * a SWT color resource. If a corresponding SWT color
     * instance is already in the pool, it will be used
     * instead of creating a new one. This is used in
     * {@link #setColor()} for instance.
     *
     * @param awtColor The AWT color to convert.
     * @return A SWT color instance.
     */
    private org.eclipse.swt.graphics.Color getSwtColorFromPool(Color awtColor) {
        org.eclipse.swt.graphics.Color swtColor =
                (org.eclipse.swt.graphics.Color)
                        // we can't use the following valueOf() method, because it
                        // won't compile with JDK1.4
                        // this.colorsPool.get(Integer.valueOf(awtColor.getRGB()));
                        this.colorsPool.get(new Integer(awtColor.getRGB()));
        if (swtColor == null) {
            swtColor = SWTUtils.toSwtColor(this.gc.getDevice(), awtColor);
            addToResourcePool(swtColor);
            // see comment above
            //this.colorsPool.put(Integer.valueOf(awtColor.getRGB()), swtColor);
            this.colorsPool.put(new Integer(awtColor.getRGB()), swtColor);
        }
        return swtColor;
    }

    /**
     * Internal method to convert a AWT transform object into
     * a SWT transform resource. If a corresponding SWT transform
     * instance is already in the pool, it will be used
     * instead of creating a new one. This is used in
     * {@link #setTransform()} for instance.
     *
     * @param awtTransform The AWT transform to convert.
     * @return A SWT transform instance.
     */
    private Transform getSwtTransformFromPool(AffineTransform awtTransform) {
        Transform t = (Transform) this.transformsPool.get(awtTransform);
        if (t == null) {
            t = new Transform(this.gc.getDevice());
            double[] matrix = new double[6];
            awtTransform.getMatrix(matrix);
            t.setElements((float) matrix[0], (float) matrix[1],
                    (float) matrix[2], (float) matrix[3],
                    (float) matrix[4], (float) matrix[5]);
            addToResourcePool(t);
            this.transformsPool.put(awtTransform, t);
        }
        return t;
    }

    /**
     * Perform a switch between foreground and background
     * color of gc. This is needed for consistency with
     * the AWT behaviour, and is required notably for the
     * filling methods.
     */
    private void switchColors() {
        org.eclipse.swt.graphics.Color bg = this.gc.getBackground();
        org.eclipse.swt.graphics.Color fg = this.gc.getForeground();
        this.gc.setBackground(fg);
        this.gc.setForeground(bg);
    }

    /**
     * Converts an AWT {@code Shape} into a SWT {@code Path}.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @return The path.
     */
    private Path toSwtPath(Shape shape) {
        int type;
        float[] coords = new float[6];
        Path path = new Path(this.gc.getDevice());
        PathIterator pit = shape.getPathIterator(null);
        while (!pit.isDone()) {
            type = pit.currentSegment(coords);
            switch (type) {
                case (PathIterator.SEG_MOVETO):
                    path.moveTo(coords[0], coords[1]);
                    break;
                case (PathIterator.SEG_LINETO):
                    path.lineTo(coords[0], coords[1]);
                    break;
                case (PathIterator.SEG_QUADTO):
                    path.quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case (PathIterator.SEG_CUBICTO):
                    path.cubicTo(coords[0], coords[1], coords[2],
                            coords[3], coords[4], coords[5]);
                    break;
                case (PathIterator.SEG_CLOSE):
                    path.close();
                    break;
                default:
                    break;
            }
            pit.next();
        }
        return path;
    }

    /**
     * Converts an SWT transform into the equivalent AWT transform.
     *
     * @param swtTransform  the SWT transform.
     *
     * @return The AWT transform.
     */
    private AffineTransform toAwtTransform(Transform swtTransform) {
        float[] elements = new float[6];
        swtTransform.getElements(elements);
        AffineTransform awtTransform = new AffineTransform(elements);
        return awtTransform;
    }

    /**
     * Returns the AWT line cap corresponding to the specified SWT line cap.
     *
     * @param swtLineCap  the SWT line cap.
     *
     * @return The AWT line cap.
     */
    private int toAwtLineCap(int swtLineCap) {
        if (swtLineCap == SWT.CAP_FLAT) {
            return BasicStroke.CAP_BUTT;
        }
        else if (swtLineCap == SWT.CAP_ROUND) {
            return BasicStroke.CAP_ROUND;
        }
        else if (swtLineCap == SWT.CAP_SQUARE) {
            return BasicStroke.CAP_SQUARE;
        }
        else {
            throw new IllegalArgumentException("SWT LineCap " + swtLineCap
                    + " not recognised");
        }
    }

    /**
     * Returns the AWT line join corresponding to the specified SWT line join.
     *
     * @param swtLineJoin  the SWT line join.
     *
     * @return The AWT line join.
     */
    private int toAwtLineJoin(int swtLineJoin) {
        if (swtLineJoin == SWT.JOIN_BEVEL) {
            return BasicStroke.JOIN_BEVEL;
        }
        else if (swtLineJoin == SWT.JOIN_MITER) {
            return BasicStroke.JOIN_MITER;
        }
        else if (swtLineJoin == SWT.JOIN_ROUND) {
            return BasicStroke.JOIN_ROUND;
        }
        else {
            throw new IllegalArgumentException("SWT LineJoin " + swtLineJoin
                    + " not recognised");
        }
    }

    /**
     * Returns the SWT line cap corresponding to the specified AWT line cap.
     *
     * @param awtLineCap  the AWT line cap.
     *
     * @return The SWT line cap.
     */
    private int toSwtLineCap(int awtLineCap) {
        if (awtLineCap == BasicStroke.CAP_BUTT) {
            return SWT.CAP_FLAT;
        }
        else if (awtLineCap == BasicStroke.CAP_ROUND) {
            return SWT.CAP_ROUND;
        }
        else if (awtLineCap == BasicStroke.CAP_SQUARE) {
            return SWT.CAP_SQUARE;
        }
        else {
            throw new IllegalArgumentException("AWT LineCap " + awtLineCap
                    + " not recognised");
        }
    }

    /**
     * Returns the SWT line join corresponding to the specified AWT line join.
     *
     * @param awtLineJoin  the AWT line join.
     *
     * @return The SWT line join.
     */
    private int toSwtLineJoin(int awtLineJoin) {
        if (awtLineJoin == BasicStroke.JOIN_BEVEL) {
            return SWT.JOIN_BEVEL;
        }
        else if (awtLineJoin == BasicStroke.JOIN_MITER) {
            return SWT.JOIN_MITER;
        }
        else if (awtLineJoin == BasicStroke.JOIN_ROUND) {
            return SWT.JOIN_ROUND;
        }
        else {
            throw new IllegalArgumentException("AWT LineJoin " + awtLineJoin
                    + " not recognised");
        }
    }
}
