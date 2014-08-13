/*
 * Labeler.java  
 *   
 * Copyright (C) 2008 Eugene K. Ressler
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import javax.swing.UIManager;

/**
 * Text label service provider.
 * 
 * @author Eugene K. Ressler
 */
public class Labeler {

    /**
     * Left justify text.
     */
    public static final int JUSTIFY_LEFT = 1;
    /**
     * Center text either horizontally or vertically.
     */
    public static final int JUSTIFY_CENTER = 2;
    /**
     * Right justify text.
     */
    public static final int JUSTIFY_RIGHT = 3;
    /**
     * Align text to top.
     */
    public static final int JUSTIFY_TOP = 4;
    /**
     * Align text to baseline.
     */
    public static final int JUSTIFY_BASELINE = 5;
    /**
     * Align text to bottom.
     */
    public static final int JUSTIFY_BOTTOM = 6;
    
    private static final int xMargin = 2;
    private static final int yMargin = 1;
    private static Font font;

    /**
     * Draw text justified horizontally and vertically.
     * 
     * @param g0 java graphics context
     * @param s text to draw
     * @param x0 x-coordinate of reference point
     * @param y0 y-coordinate of reference point
     * @param h horizontal justification
     * @param v vertical justification
     * @param background background color
     */
    public static void drawJustified(Graphics g0, String s, int x0, int y0, int h, int v, Color background) {
        drawJustified(g0, s, x0, y0, h, v, background, Color.BLACK, null);
    }

    /**
     * Draw text justified horizontally and vertically.
     * 
     * @param g0 java graphics context
     * @param s text to draw
     * @param x0 x-coordinate of reference point
     * @param y0 y-coordinate of reference point
     * @param h horizontal justification
     * @param v vertical justification
     * @param background background color
     * @param bounds returned bounds of the label
     */
    public static void drawJustified(Graphics g0, String s, int x0, int y0, int h, int v, Color background, Rectangle bounds) {
        drawJustified(g0, s, x0, y0, h, v, background, null, bounds);
    }
    
    /**
     * Return the label font.
     * 
     * @return label font
     */
    public static Font getFont() {
        if (font == null) {
            font = UIManager.getFont("Label.font");
            font = font.deriveFont(font.getSize() - 2f);
        }
        return font;
    }

    /**
     * Draw text justified horizontally and vertically and also rotated about the reference point.
     * 
     * @param g0 java graphics context
     * @param s text to draw
     * @param angle rotation angle in degrees
     * @param x0 x-coordinate of reference point
     * @param y0 y-coordinate of reference point
     * @param h horizontal justification
     * @param v vertical justification
     * @param background background color
     * @param bounds returned bounds of the label
     */
    public static void drawRotatedAndJustified(Graphics g0, String s, 
            double angle, int x0, int y0, int h, int v, 
            Color background, Color border, Rectangle bounds) {            
        Graphics2D g = (Graphics2D) g0;
        AffineTransform savedXform = g.getTransform();
        g.rotate(Math.toRadians(angle), x0, y0);
        drawJustified(g, s, x0, y0, h, v, background, border, bounds);
        g.setTransform(savedXform);
    }

    /**
     * Draw text justified horizontally and vertically.
     * 
     * @param g0 java graphics context
     * @param s text to draw
     * @param x0 x-coordinate of reference point
     * @param y0 y-coordinate of reference point
     * @param h horizontal justification
     * @param v vertical justification
     * @param background background color
     * @param border border color
     * @param bounds returned bounds of the label
     */
    public static void drawJustified(Graphics g0, 
            String s, int x0, int y0, int h, int v, 
            Color background, Color border, Rectangle bounds) {
        getFont();
        Graphics2D g = (Graphics2D) g0;
        Color savedColor = g.getColor();
        FontRenderContext frc = g.getFontRenderContext();
        LineMetrics metrics = font.getLineMetrics(s, frc);
        float width = (float) font.getStringBounds(s, frc).getWidth();
        float ascent = metrics.getAscent();
        float height = metrics.getDescent() + ascent;
        int x = x0; // left is default
        switch (h) {
            case JUSTIFY_LEFT:
                if (background != null) {
                    x += xMargin;
                }
                break;
            case JUSTIFY_CENTER:
                x = Math.round(x0 - width * 0.5f);
                break;
            case JUSTIFY_RIGHT:
                x = Math.round(x0 - width);
                if (background != null) {
                    x -= xMargin;
                }
                break;
        }
        int y = y0; // baseline is default
        switch (v) {
            case JUSTIFY_TOP:
                y = Math.round(y0 + ascent);
                if (background != null) {
                    y += yMargin;
                }
                break;
            case JUSTIFY_CENTER:
                y = Math.round(y0 + ascent - height * 0.5f);
                break;
            case JUSTIFY_BOTTOM:
                y = Math.round(y0 + ascent - height);
                if (background != null) {
                    y -= yMargin;
                }
                break;
        }
        if (background != null) {
            int xBg = Math.round(x - xMargin);
            int yBg = Math.round(y - ascent - yMargin + 1);
            int wBg = Math.round(width + 2 * xMargin);
            int hBg = Math.round(height + 2 * yMargin);
            g.setColor(background);
            g.fillRect(xBg, yBg, wBg - 1, hBg - 1);
            if (border != null) {
                g.setColor(border);
                g.drawRect(xBg - 1, yBg - 1, wBg, hBg);
            }
        }
        if (bounds != null) {
            bounds.setBounds(x, Math.round(y - ascent), 1 + (int) width, 1 + (int) height);
        }
        g.setFont(font);
        g.setColor(savedColor);
        // Replace this:  g.drawString(s, x, y);
        // with this to fix obscure Mac text-drawing bug that manfests in vertical text.
        frc = new FontRenderContext(g.getTransform(), true, true);
        g.drawGlyphVector(font.createGlyphVector(frc, s), x, y);
    }
}
