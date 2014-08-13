/*
 * ViewportTransform.java
 * 
 * Copyright (C) 2010 Eugene K. Ressler
 *   
 * This program is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU General Public License for more details.  
 *   
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */
         
package bridgedesigner;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Viewport transformations from world to screen or printer coordinates.
 * Supports viewports of negative height and width, which is needed e.g.
 * because screen coordinates usually have origin at upper left, while
 * window has origin at lower left.
 * 
 * @author Eugene K. Ressler
 */
public class ViewportTransform {

    /**
     * Align window left in viewport.
     */
    public static final int LEFT = -1;
    /**
     * Align window center in viewport, either vertically or horizontally.
     */
    public static final int CENTER = 0;
    /**
     * Align window right in viewport.
     */
    public static final int RIGHT = 1;
    /**
     * Align window at the top of the viewport.
     */
    public static final int TOP = -1;
    /**
     * Align window agt the bottom of the viewport.
     */
    public static final int BOTTOM = 1;
    
    private double xWindow = 0;
    private double yWindow = 0;
    private double zWindow = 0;
    private double widthWindow = 1;
    private double heightWindow = 1;
    private double vpTx = 0.5;
    private double vpTy = 0.5;
    private double vpX = 0.5;
    private double vpY = 0.5;
    private double xViewport = 0;
    private double yViewport = 0;
    private double widthViewport = 1;
    private double heightViewport = 1;
    private double xMargin = 0;
    private double yMargin = 0;
    private double xScaleFactor = 1;
    private double yScaleFactor = 1;
    private double zScaleFactor = 1;
    private int nWindowUpdates = 0;
    private int nViewportUpdates = 0;
    private int hAlign = CENTER;
    private int vAlign = CENTER;

    /**
     * Return the absolute viewport width in pixels.
     * 
     * @return width in pixels
     */
    public int getAbsWidthViewport() {
        return Math.abs((int)(0.5 + widthViewport));
    }

    /**
     * Return the absolute viewport height in pixels.
     * 
     * @return height in pixels
     */
    public int getAbsHeightViewport() {
        return Math.abs((int)(0.5 + heightViewport));
    }

    /**
     * Get width of viewport that's actually needed to render the window.
     * 
     * @return used viewport width in pixels
     */
    public int getUsedViewportWidth() {
        return Math.abs((int)(0.5 + widthViewport - xMargin));
    }
    
    /**
     * Get height of viewport that's actually needed to render the window.
     * 
     * @return used viewport width in pixels
     */
    public int getUsedHeightViewport() {
        return Math.abs((int)(0.5 + heightViewport - yMargin));
    }

    /**
     * Set horizontal alignment of window in viewport.
     * 
     * @param hAlign horizontal alignment flag
     */
    public void setHAlign(int hAlign) {
        this.hAlign = hAlign;
    }

    /**
     * Set vertical alignment of window in viewport.
     * 
     * @param vAlign vertical alignment flag
     */
    public void setVAlign(int vAlign) {
        this.vAlign = vAlign;
    }

    /**
     * Set the location of the one-point perspective vanishing point
     * parametrically within the window.
     *
     * @param vpTx x parameter for vanishing point
     * @param vpTy y parameter for vanishing point
     */
    public void setVanishingPoint(double vpTx, double vpTy, double zWindow) {
        this.vpTx = vpTx;
        this.vpTy = vpTy;
        this.zWindow = zWindow;
    }

    /**
     * Set the factor that z multiplied by before perspective (vanishing point)
     * division.
     *
     * @param zScale z scale factor
     */
    public void setZScale(double zScale) {
        zScaleFactor = zScale;
    }

    private void setScaleFactor() {
        if (widthWindow == 0 || heightWindow == 0) {
            xScaleFactor = yScaleFactor = 1;
            return;
        }
        double sfX = widthViewport / widthWindow;
        double sfY = heightViewport / heightWindow;
        if (Math.abs(sfX) < Math.abs(sfY)) {
            xMargin = 0;
            xScaleFactor = sfX;
            yScaleFactor = Math.copySign(sfX, sfY);
            double margin = heightViewport - heightWindow * yScaleFactor;
            switch (vAlign) {
                case BOTTOM:
                    yMargin = 0;
                    break;
                case TOP:
                    yMargin = margin;
                    break;
                default:
                    yMargin = 0.5 * margin;
                    break;
            }
        } else {
            yMargin = 0;
            yScaleFactor = sfY;
            xScaleFactor = Math.copySign(sfY, sfX);
            double margin = widthViewport - widthWindow * xScaleFactor;
            switch (hAlign) {
                case LEFT:
                    xMargin = 0;
                    break;
                case RIGHT:
                    xMargin = margin;
                    break;
                default:
                    xMargin = 0.5 * margin;
                    break;
            }
        }
        vpX = worldToViewportX(xWindow + vpTx * widthWindow);
        vpY = worldToViewportY(yWindow + vpTy * heightWindow);
    }

    /**
     * Set the window of the transformation.
     * 
     * @param x world x-coordinate of window origin
     * @param y world y-coordinate of window origin
     * @param width world width of window
     * @param height world height of window
     */
    public void setWindow(double x, double y, double width, double height) {
        xWindow = x;
        yWindow = y;
        widthWindow = width;
        heightWindow = height;
        nWindowUpdates++;
        setScaleFactor();
    }

    /**
     * Set the window of the transformation.
     * 
     * @param w window rectangle in world coordinates
     */
    public void setWindow(Rectangle.Double w) {
        this.setWindow(w.x, w.y, w.width, w.height);
    }

    /**
     * Set the viewport of the transformation.
     * 
     * @param x screen x-coordinate of window origin
     * @param y screen y-coordinate of window origin
     * @param width screen width of window
     * @param height screen height of window
     */
    public void setViewport(double x, double y, double width, double height) {
        xViewport = x;
        yViewport = y;
        widthViewport = width;
        heightViewport = height;
        nViewportUpdates++;
        setScaleFactor();
    }
    
    /**
     * Return true iff both viewport and window have been set.
     * 
     * @return true iff both viewport and window 
     */
    public boolean isSet() {
        return nViewportUpdates > 0 && nWindowUpdates > 0;
    }

    /**
     * Return a key that can be used to tell if this transformation
     * has been updated either by changing the window or viewport.
     * 
     * @return update key
     */
    public int getUpdateKey() {
        return (nWindowUpdates << 16 ) | nViewportUpdates;
    }
    
    /**
     * Return a viewport x-coordinate corresponding to given world x-coordinate.
     * 
     * @param x world x-coordinate
     * @return viewport x-coordinate
     */
    public int worldToViewportX(double x) {
        return (int)(0.5 + xMargin + xViewport + (x - xWindow) * xScaleFactor);
    }

    /**
     * Return a viewport y-coordinate corresponding to given world y-coordinate.
     * 
     * @param y world y-coordinate
     * @return viewport y-coordinate
     */
    public int worldToViewportY(double y) {
        return (int)(0.5 + yMargin + yViewport + (y - yWindow) * yScaleFactor);
    }

    /**
     * Store into a given destination point the viewport coordinate corresponding to the given 3d world
     * coordinate point using a one-point perspective transformation.
     * If the given destination is null, make a fresh point. Return the destination point.
     *
     * @param dst destination screen coordinate point or null to request that one be allocated here
     * @param dst_x destination screen x-coordinate array
     * @param dst_y destination screen y-coordinate array
     * @param dst_ofs offset to use in placing results in destination arrays
     * @param x world source point x coordinate
     * @param y world source point y coordinate
     * @param z world source point z coordinate
     */
    public boolean worldToViewport(int [] dst_x, int [] dst_y, int dst_ofs,
            double x, double y, double z) {
        final double xp = xMargin + xViewport + (x - xWindow) * xScaleFactor;
        final double yp = yMargin + yViewport + (y - yWindow) * yScaleFactor;
        final double zp = (z - zWindow) * zScaleFactor;
        if (zp > .99) {
            return false;
        }
        final double t = zp / (zp - 1);
        dst_x[dst_ofs] = (int)(0.5 + xp + t * (vpX - xp));
        dst_y[dst_ofs] = (int)(0.5 + yp + t * (vpY - yp));
        return true;
    }

    public boolean worldToViewport(double [] dst_x, double [] dst_y, int dst_ofs,
            double x, double y, double z) {
        final double xp = xMargin + xViewport + (x - xWindow) * xScaleFactor;
        final double yp = yMargin + yViewport + (y - yWindow) * yScaleFactor;
        final double zp = (z - zWindow) * zScaleFactor;
        if (zp > .99) {
            return false;
        }
        final double t = zp / (zp - 1);
        dst_x[dst_ofs] = xp + t * (vpX - xp);
        dst_y[dst_ofs] = yp + t * (vpY - yp);
        return true;
    }

    public boolean worldToViewport(Point dst, double x, double y, double z) {
        final double xp = xMargin + xViewport + (x - xWindow) * xScaleFactor;
        final double yp = yMargin + yViewport + (y - yWindow) * yScaleFactor;
        final double zp = (z - zWindow) * zScaleFactor;
        if (zp > .99) {
            return false;
        }
        final double t = zp / (zp - 1);
        dst.x = (int)(0.5 + xp + t * (vpX - xp));
        dst.y = (int)(0.5 + yp + t * (vpY - yp));
        return true;
    }

    public boolean isAboveVanishingPoint(double y) {
        return yMargin + yViewport + (y - yWindow) * yScaleFactor < vpY;
    }
    
    public boolean isRightOfVanishingPoint(double x) {
        return xMargin + xViewport + (x - xWindow) * xScaleFactor > vpX;
    }

    public Point getVanishingPoint(Point buf) {
        if (buf == null) {
            buf = new Point();
        }
        buf.x = (int)(0.5 + vpX);
        buf.y = (int)(0.5 + vpY);
        return buf;
    }
    
    /**
     * Store into a given destination point the viewport coordinate corresponding to the given world coordinate point.
     * If the given destination is null, make a fresh point. Return the destination point.
     * 
     * @param dst destination screen coordinate point or null to request that one be allocated here
     * @param src world coordinate point to convert
     * @return destination point
     */
    public Point worldToViewport(Point dst, Affine.Point src) {
        if (dst == null) {
            dst = new Point();
        }
        dst.x = worldToViewportX(src.x);
        dst.y = worldToViewportY(src.y);
        return dst;
    }

    /**
     * Store into a given destination rectangle the viewport rectangle corresponding to the given world rectangle.
     * If the given destination is null, make a fresh rectangle. Return the destination rectangle.
     * 
     * @param dst destination screen coordinate rectangle or null to request that one be allocated here
     * @param src world coordinate rectangle to convert
     * @return destination rectangle
     */
    public Rectangle worldToViewport(Rectangle dst, Rectangle.Double src) {
        if (dst == null) {
            dst = new Rectangle();
        }
        int x = worldToViewportX(src.x);
        int y = worldToViewportY(src.y);
        int width = (int)(0.5 + src.width * xScaleFactor);
        int height = (int)(0.5 + src.height * yScaleFactor);
        dst.setFrameFromDiagonal(x, y, x + width, y + height);
        return dst;
    }

    /**
     * Return a world  x-coordinate corresponding to given viewport x-coordinate.
     * 
     * @param x viewport x-coordinate
     * @return world x-coordinate
     */
    public double viewportToWorldX(int x) {
        return xWindow + (x - xMargin - xViewport) / xScaleFactor;
    }

    /**
     * Return a world  y-coordinate corresponding to given viewport y-coordinate.
     * 
     * @param y viewport y-coordinate
     * @return world y-coordinate
     */
    public double viewportToWorldY(int y) {
        return yWindow + (y - yMargin - yViewport) / yScaleFactor;
    }

    /**
     * Store into a given destination point the world coordinate corresponding to the given viewport  coordinate point.
     * If the given destination is null, make a fresh point. Return the destination point.
     * 
     * @param dst destination world coordinate point or null to request that one be allocated here
     * @param src screen coordinate point to convert
     * @return destination point
     */
    public Affine.Point viewportToWorld(Affine.Point dst, Point src) {
        if (dst == null) {
            dst = new Affine.Point();
        }
        dst.x = viewportToWorldX(src.x);
        dst.y = viewportToWorldY(src.y);
        return dst;
    }

    /**
     * Store into a given destination rectangle the world rectangle corresponding to the given viewport rectangle.
     * If the given destination is null, make a fresh rectangle. Return the destination rectangle.
     * 
     * @param dst destination world coordinate rectangle or null to request that one be allocated here
     * @param src screen coordinate rectangle to convert
     * @return destination rectangle
     */
    public Rectangle.Double viewportToWorld(Rectangle.Double dst, Rectangle src) {
        if (dst == null) {
            dst = new Rectangle.Double();
        }
        double x = viewportToWorldX(src.x);
        double y = viewportToWorldY(src.y);
        double width = src.width / xScaleFactor;
        double height = src.height / yScaleFactor;
        dst.setFrameFromDiagonal(x, y, x + width, y + height);
        return dst;
    }

    /**
     * Return the world distance corresponding to the given screen coordinate distance.
     * 
     * @param d screen coordinate distance
     * @return world distance
     */
    public double viewportToWorldDistance(int d) {
        return d / Math.abs(xScaleFactor);
    }

    /**
     * Return the screen coordinate distance corresponding to the given world coordinate distance.
     * 
     * @param d world coordinate distance
     * @return screen distance
     */
    public int worldToViewportDistance(double d) {
        return (int)(0.5 + d * Math.abs(xScaleFactor));
    }

    /**
     * Return a string representation of this transformation.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "Window:   [" + xWindow + "," + yWindow + ";" + widthWindow + "," + heightWindow + "]\n" +
                "Viewport: [" + xViewport + "," + yViewport + ";" + widthViewport + "," + heightViewport + "]\n" +
                "Scale:    [" + xScaleFactor + "," + yScaleFactor + "]\n" +
                "Margin:   [" + xMargin + "," + yMargin + "]\n";
    }
}
