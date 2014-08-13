/*
 * BridgeBlueprintView.java  
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import bridgedesigner.Affine.Point;

/**
 * View of a bridge as a blueprint meant for printing.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeBlueprintView extends BridgeView {

    /**
     * Various dimensions in twips.
     */
    private static final int arrowHalfWidth = 1 * 20;
    private static final int arrowLength = 4 * 20;
    private static final int tickInside = 72 * 20 * 1 / 8;
    private static final int tickOutside = 72 * 20 * 1 / 16;
    private static final int dimensionOffset = 72 * 20 * 3 / 16;
    
    /**
     * Construct a new bridge blueprint view.
     * 
     * @param bridge bridge to view
     */
    public BridgeBlueprintView(BridgeModel bridge) {
        this.bridge = bridge;
    }

    /**
     * Return the extent of the painted image as a rectangle.
     * 
     * @param extent result or null, which causes alloction of a new rectangle to return
     * @param viewportTransform viewport transform
     * @return extent
     */
    public Rectangle getPaintedExtent(Rectangle extent,  ViewportTransform viewportTransform) {
        if (extent == null) {
            extent = new Rectangle();
        }
        final int x0 = viewportTransform.worldToViewportX(preferredDrawingWindow.getMinX());
        final int y0 = viewportTransform.worldToViewportY(preferredDrawingWindow.getMinY());
        final int x1 = viewportTransform.worldToViewportX(preferredDrawingWindow.getMaxX());
        final int y1 = viewportTransform.worldToViewportY(preferredDrawingWindow.getMaxY());
        extent.x = x0 - dimensionOffset - tickOutside - arrowHalfWidth;
        extent.y = y0 + dimensionOffset + tickOutside + arrowHalfWidth;
        extent.width = x1 - extent.x;
        extent.height = y1 - extent.y;        
        return extent;
    }

    /**
     * Load the extent of the bridge into the preferred drawing window rectangle.
     */
    @Override 
    protected void loadPreferredDrawingWindow() {
        bridge.getExtent(preferredDrawingWindow);
    }

    /**
     * Paint a label on the view, centered on the given coordinate.
     * Can't us Labeler because graphics are in twips.
     * 
     * @param g graphics object
     * @param text text to draw
     * @param x x-coordinate of label
     * @param y y-coordinate of label
     */
    private void paintLabel(Graphics2D g, String text, int x, int y) {
        AffineTransform savedTransform = g.getTransform();
        g.translate(x, y);
        g.scale(20, 20);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
        g.translate(-0.5 * bounds.getWidth(), 0.3 * bounds.getHeight());
        g.setColor(Color.white);
        g.fill(bounds);
        g.setColor(Color.black);
        g.drawString(text, 0, 0);
        g.setTransform(savedTransform);        
    }
    
    /**
     * Paint the view.
     * 
     * @param g graphics object
     * @param viewportTransform viewport transform
     */
    @Override 
    protected void paint(Graphics2D g, ViewportTransform viewportTransform) {
        
        g.setStroke(new BasicStroke(0.25f * 20f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        paintAnchorages(g, viewportTransform);
        paintAbutmentsAndPier(g, viewportTransform);
        paintBridge(g, viewportTransform, new BridgePaintContext(Gusset.getGussets(bridge)));

        final double x0World = preferredDrawingWindow.getMinX();
        final double y0World = preferredDrawingWindow.getMinY();
        final double x1World = preferredDrawingWindow.getMaxX();
        final double y1World = preferredDrawingWindow.getMaxY();
        
        final int x0Viewport = viewportTransform.worldToViewportX(x0World);
        final int y0Viewport = viewportTransform.worldToViewportY(y0World);
        final int x1Viewport = viewportTransform.worldToViewportX(x1World);
        final int y1Viewport = viewportTransform.worldToViewportY(y1World);
        
        // Draw the vertical dimensions.
        int xDimension = x0Viewport - dimensionOffset;
        if (y0World < -0.5 && y1World > 0.5) {
            //  If there's room and drawing is split by x-axis, draw dimensions from zero.
            final int yDeckViewport = viewportTransform.worldToViewportY(0);
            Utility.drawDoubleArrow(g, xDimension, y0Viewport, xDimension, yDeckViewport, arrowHalfWidth, arrowLength);
            Utility.drawDoubleArrow(g, xDimension, yDeckViewport, xDimension, y1Viewport, arrowHalfWidth, arrowLength);
            g.drawLine(xDimension - tickOutside, yDeckViewport, xDimension + tickInside, yDeckViewport);
            paintLabel(g, String.format(Locale.US, "%.2fm", -y0World), xDimension, (y0Viewport + yDeckViewport) / 2);
            paintLabel(g, String.format(Locale.US, "%.2fm", y1World), xDimension, (yDeckViewport + y1Viewport) / 2);
            xDimension -= dimensionOffset;
        }
        Utility.drawDoubleArrow(g, xDimension, y0Viewport, xDimension, y1Viewport, arrowHalfWidth, arrowLength);
        g.drawLine(xDimension - tickOutside, y0Viewport, x0Viewport - dimensionOffset + tickInside, y0Viewport);
        g.drawLine(xDimension - tickOutside, y1Viewport, x0Viewport - dimensionOffset + tickInside, y1Viewport);
        paintLabel(g, String.format(Locale.US, "%.2fm", preferredDrawingWindow.height), xDimension, (y0Viewport + y1Viewport) / 2);
        
        // Draw the horizontal dimensions.
        int yDimension = y0Viewport + dimensionOffset;
        int yTickInside = yDimension - tickInside;
        
        final DesignConditions dc = bridge.getDesignConditions();
        if (dc.isLeftAnchorage()) {
            // If there are anchorages, draw sub-dimensions to left and (possibly) right banks of the gap.
            double x0DeckWorld = dc.getXLeftmostDeckJoint();
            double x1DeckWorld = dc.getXRightmostDeckJoint();
            final int x0DeckViewport = viewportTransform.worldToViewportX(x0DeckWorld);
            final int x1DeckViewport =  viewportTransform.worldToViewportX(x1DeckWorld);
            
            Utility.drawDoubleArrow(g, x0Viewport, yDimension, x0DeckViewport, yDimension, arrowHalfWidth, arrowLength);
            g.drawLine(x0Viewport, yDimension + tickOutside, x0Viewport, yTickInside);
            paintLabel(g, String.format(Locale.US, "%.2fm", x0DeckWorld - x0World), (x0Viewport + x0DeckViewport) / 2, yDimension);
            
            Utility.drawDoubleArrow(g, x0DeckViewport, yDimension, x1DeckViewport, yDimension, arrowHalfWidth, arrowLength);
            g.drawLine(x0DeckViewport, yDimension + tickOutside, x0DeckViewport, yTickInside);
            g.drawLine(x1DeckViewport, yDimension + tickOutside, x1DeckViewport, yTickInside);
            paintLabel(g, String.format(Locale.US, "%.2fm", x1DeckWorld - x0DeckWorld), (x0DeckViewport + x1DeckViewport) / 2, yDimension);
            
            if (dc.isRightAnchorage()) {
                Utility.drawDoubleArrow(g, x1DeckViewport, yDimension, x1Viewport, yDimension, arrowHalfWidth, arrowLength);
                g.drawLine(x1Viewport, yDimension + tickOutside, x1Viewport, yTickInside);
                paintLabel(g, String.format(Locale.US, "%.2fm", x1World - x1DeckWorld), (x1DeckViewport + x1Viewport) / 2, yDimension);
            }

            yDimension += dimensionOffset;
        }        
        Utility.drawDoubleArrow(g, x0Viewport, yDimension, x1Viewport, yDimension, arrowHalfWidth, arrowLength);
        g.drawLine(x0Viewport, yDimension + tickOutside, x0Viewport, yTickInside);
        g.drawLine(x1Viewport, yDimension + tickOutside, x1Viewport, yTickInside);
        paintLabel(g, String.format(Locale.US, "%.2fm", preferredDrawingWindow.width), (x0Viewport + x1Viewport) / 2, yDimension);
    }

    /**
     * Anchor dimensions.
     */
    private static final int rollerHalfSize = 5 * 4;
    private static final int rollerSize = 2 * rollerHalfSize;
    private static final int groundHalfWidth = 5 * 26;
    private static final int groundHeight = 5 * 7;
    private static final int halfTickSpacing = 5 * 3;
    private static final int tickSpacing = 2 * halfTickSpacing;
    private static final int tickHalfSlant = 5 * 3;
    private static final int anchorHalfWidth = 5 * 16;
    private static final int anchorHeight = 5 * 28;
    private static final int rollerHalfSpacing = 5 * 6;
    private static final int rollerSpacing = 2 * rollerHalfSpacing;
    
    /**
     * Anchor triangle shape.
     */
    private static final int [] xAnchor = { -anchorHalfWidth, 0, anchorHalfWidth };
    private static final int [] yAnchor = { anchorHeight, 0, anchorHeight };
    private static final Polygon anchor = new Polygon(xAnchor, yAnchor, xAnchor.length);

    /**
     * Paint an anchor with given number of constraints.
     * 
     * @param g graphics object
     * @param location location of the anchor
     * @param nConstraints number of constraints of the anchor
     * @param viewportTransform viewport transform
     */
    private void paintAnchor(Graphics2D g, Affine.Point location, int nConstraints, ViewportTransform viewportTransform) {
        AffineTransform savedTransform = g.getTransform();
        Stroke savedStroke = g.getStroke();
        final int x = viewportTransform.worldToViewportX(location.x);
        final int y = viewportTransform.worldToViewportY(location.y);
        g.translate(x, y);
        g.setStroke(new BasicStroke(5f));
        g.setColor(Color.white);
        g.fill(anchor);
        g.setColor(Color.black);
        g.draw(anchor);
        g.drawLine(-groundHalfWidth, anchorHeight, groundHalfWidth, anchorHeight);
        switch (nConstraints) {
            case 1:
                // Draw rollers and ground beneath.
                for (int xRoller = rollerHalfSpacing; xRoller < groundHalfWidth; xRoller += rollerSpacing) {
                    g.drawOval(+xRoller - rollerHalfSize, anchorHeight, rollerSize, rollerSize);        
                    g.drawOval(-xRoller - rollerHalfSize, anchorHeight, rollerSize, rollerSize);        
                }
                g.drawLine(-groundHalfWidth, anchorHeight + rollerSize, 
                           +groundHalfWidth, anchorHeight + rollerSize);
                for (int xTick = halfTickSpacing; xTick < groundHalfWidth; xTick += tickSpacing) {
                    g.drawLine(+xTick + tickHalfSlant, anchorHeight + rollerSize, 
                               +xTick - tickHalfSlant, anchorHeight + rollerSize + groundHeight);
                    g.drawLine(-xTick + tickHalfSlant, anchorHeight + rollerSize, 
                               -xTick - tickHalfSlant, anchorHeight + rollerSize + groundHeight);
                }
                break;
            case 2:
                // Draw the ground beneath.
                for (int xTick = halfTickSpacing; xTick < groundHalfWidth; xTick += tickSpacing) {
                    g.drawLine(+xTick + tickHalfSlant, anchorHeight, 
                               +xTick - tickHalfSlant, anchorHeight + groundHeight);
                    g.drawLine(-xTick + tickHalfSlant, anchorHeight, 
                               -xTick - tickHalfSlant, anchorHeight + groundHeight);
                }
                break;
        }
        g.setTransform(savedTransform);
        g.setStroke(savedStroke);        
    }
    
    /**
     * Paint an anchor to represent a standard abutment.
     * 
     * @param g graphics object
     * @param location location of the abutment joint
     * @param right true iff this is the right abutment; else it's the left
     * @param nConstraints number of constraints provided by the the abutment
     * @param viewportTransform viewport transform
     */
    @Override
    public void paintStandardAbutment(Graphics2D g, Affine.Point location, boolean right, int nConstraints, ViewportTransform viewportTransform) {
        paintAnchor(g, location, nConstraints, viewportTransform);
    }
    
    /**
     * Paint an anchor to represent a pier.
     *
     * @param g graphics object
     * @param location location of the pier joint
     * @param pierHeight height of the pier (ignored)
     * @param viewportTransform viewport transform
     */
    @Override
    protected void paintPier(Graphics2D g, Affine.Point location, double pierHeight, ViewportTransform viewportTransform) 
    {
        paintAnchor(g, location, 2, viewportTransform);
    }

    /**
     * Paint an anchor to represent an arch abutment.
     * 
     * @param g graphics object
     * @param location location of the abutment joint
     * @param right true iff this is the right abutment; else it's the left one 
     * @param archHeight vertical distance from arch joint to deck joint 
     * @param viewportTransform viewport transform
     */
    @Override
    protected void paintArchAbutment(Graphics2D g, Affine.Point location, boolean right, double archHeight, ViewportTransform viewportTransform) 
    {
        paintAnchor(g, location.plus(0, archHeight), 2, viewportTransform);
    }

    /**
     * Paint an anchor to represent anchorage.
     * 
     * @param g graphics object
     * @param location location of the anchorage joint
     * @param viewportTransform viewport transform
     */
    @Override
    protected void paintAnchorage(Graphics2D g, Point location, ViewportTransform viewportTransform) {
        paintAnchor(g, location, 2, viewportTransform);
    }
}
