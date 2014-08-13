/*
 * CrossSectionSketch.java  
 *   
 * Copyright (C) 2009 Eugene K. Ressler
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
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import javax.swing.JLabel;
import org.jdesktop.application.ResourceMap;

/**
 * A component to render a sketch of a stock inventory shape.
 * 
 * @author Eugene K. Ressler
 */
public class CrossSectionSketch extends JLabel {

    private int widthDimension = 500;
    private int heightDimension = 500;
    private int thicknessDimension = 10;
    private boolean tube = true;
    private String message;

    private float labelWidth = -1;
    private float labelHeight = -1;
    private static final String longestString = "888";
    private static final float minMargin = 12;
    private static final float labelSep = 4;
    private static final float aspectRatio = 1;
    private static final int tickSep = 4;
    private static final int tickSize = 9;
    private static final int halfTickSize = tickSize / 2;
    private static final int thicknessDimSize = 16;
    private final ResourceMap resourceMap = BDApp.getResourceMap(CrossSectionSketch.class);

    /**
     * Initialize the sketch using the given section.
     * 
     * @param shape shape to sketch
     */
    public void initialize(Shape shape) {
        if (shape == null) {
            message = resourceMap.getString("noCurrent.text");
        }
        else {
            heightDimension = widthDimension = shape.getNominalWidth();
            thicknessDimension = (int)Math.round(shape.getThickness());
            tube = shape.getSection() instanceof TubeCrossSection;
            message = null;        
        }
        repaint();
    }
    
    /**
     * Initialize the message labeling the sketch.
     * 
     * @param message message text
     */
    public void initialize(String message) {
        this.message = message;
        repaint();
    }
    
    private void hTick(Graphics2D g, float xRight, float y) {
        int ix = Math.round(xRight) - tickSep;
        int iy = Math.round(y);
        g.drawLine(ix, iy, ix - tickSize, iy);
    }

    private void vTick(Graphics2D g, float x, float yTop) {
        int ix = Math.round(x);
        int iy = Math.round(yTop) + tickSep;
        g.drawLine(ix, iy, ix, iy + tickSize);
    }
    
    private void hDim(Graphics2D g, float x0, float x1, float y) {
        int ix0 = Math.round(x0);
        int ix1 = Math.round(x1);
        int iy = Math.round(y) + tickSep + halfTickSize;
        Utility.drawDoubleArrow(g, ix0, iy, ix1, iy);
    }
    
    private void vDim(Graphics2D g, float x, float y0, float y1) {
        int ix = Math.round(x) - tickSep - halfTickSize;
        int iy0 = Math.round(y0);
        int iy1 = Math.round(y1);
        Utility.drawDoubleArrow(g, ix, iy0, ix, iy1);
    }

    /**
     * Paint the sketch of the shape.
     * 
     * @param g0 java graphics context
     */
    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        Color savedColor = g.getColor();
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (labelWidth < 0) {
            FontRenderContext frc = g.getFontRenderContext();
            Font font = Labeler.getFont();
            LineMetrics metrics = font.getLineMetrics(longestString, frc);
            labelWidth = (float) font.getStringBounds(longestString, frc).getWidth();
            labelHeight = metrics.getAscent() + metrics.getDescent();       
        }
        if (message != null) {
            Labeler.drawJustified(g0, message, w/2, h/2, Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_CENTER, null);
            return;
        }
        float widthAvailable = w - (minMargin + labelWidth + labelSep + thicknessDimSize + minMargin);
        float heightAvailable = h - (minMargin + labelSep + labelHeight + minMargin);
        float xMargin = minMargin;
        float yMargin = minMargin;
        float widthSection = 0;
        float heightSection = 0;
        if (widthAvailable > aspectRatio * heightAvailable) {
            xMargin += (widthAvailable - aspectRatio * heightAvailable) / 2;
            widthSection = aspectRatio * heightAvailable;
            heightSection = heightAvailable;
        }
        else {
            yMargin += (aspectRatio * heightAvailable - widthAvailable) / 2;
            widthSection = widthAvailable;
            heightSection = widthAvailable / aspectRatio;
        }
        float xSection = xMargin + labelWidth + labelSep;
        float ySection = yMargin;
        g.setColor(Color.GRAY);
        final int tubeThickness = 5;
        g.fillRoundRect(Math.round(xSection), Math.round(ySection), 
                1 + Math.round(widthSection), 1 + Math.round(heightSection), 
                tubeThickness, tubeThickness);
        if (tube) {
            g.setColor(Color.WHITE);
            g.fillRect(Math.round(xSection) + tubeThickness, Math.round(ySection) + tubeThickness, 
                    1 + Math.round(widthSection) - 2 * tubeThickness, 1 + Math.round(heightSection) - 2 * tubeThickness);
            int y = Math.round(ySection + 0.75f * heightSection);
            int xRight = Math.round(xSection + widthSection) + 2;
            int xLeft = xRight - tubeThickness - 1;
            g.setColor(Color.BLACK);
            Utility.drawArrow(g, xLeft - thicknessDimSize, y, xLeft, y);
            Utility.drawArrow(g, xRight + thicknessDimSize, y, xRight, y);
            Labeler.drawJustified(g, Integer.toString(thicknessDimension), xRight + 3, y - 3, 
                    Labeler.JUSTIFY_LEFT, Labeler.JUSTIFY_BOTTOM, null); // no fill or border
        }
        else {
            g.setColor(Color.BLACK);
        }
        hTick(g, xSection, ySection);
        hTick(g, xSection, ySection + heightSection);
        vDim(g, xSection, ySection, ySection + heightSection);
        Labeler.drawJustified(g, 
                Integer.toString(heightDimension), 
                Math.round(xSection - labelSep), Math.round(ySection + heightSection / 2), 
                Labeler.JUSTIFY_RIGHT, Labeler.JUSTIFY_CENTER, 
                Color.WHITE, // white fill
                null); // no border
        vTick(g, xSection, ySection + heightAvailable);
        vTick(g, xSection + widthSection, ySection + heightSection);
        hDim(g, xSection, xSection + widthSection, ySection + heightSection);
        Labeler.drawJustified(g, 
                Integer.toString(widthDimension),
                Math.round(xSection + widthSection / 2), Math.round(ySection + heightSection + labelSep + tickSize),
                Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null); // no fill or border
        g.setColor(savedColor);
    }
}
