/*
 * BridgeView.java  
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import javax.swing.JLabel;

/**
 * Common functionality for several different 2D views of a bridge.
 * 
 * @author Eugene K. Ressler
 */
public abstract class BridgeView {

    /**
     * Bridge in the drawing.
     */
    protected BridgeModel bridge;
    /**
     * Convenience storage for bridge.getDesignConditions().
     */
    protected DesignConditions conditions;
    /**
     * Template sketch attached to this view or null if none.
     */
    protected BridgeSketchView bridgeSketchView;
    /**
     * Value added to true y-coordinates of arch step to tag them for adjustment during drawing
     */
    protected static final double ARCH_OFFSET = 1000.0;
    /**
     * Threshold value for discriminating whether a point has the ARCH_OFFSET tag or not.
     */
    protected static final double ARCH_OFFSET_WINDOW_EDGE = 500.0;
    /**
     * Distance at infinity for drawing polygon points in the bridge world certain to be outside the drawn image.
     */
    protected static final double INF = 100.0;
    /**
     * Value added to true y-coordinates of pier top to tag them for adjustment during drawing
     */
    protected static final double PIER_OFFSET = 1000.0;
    /**
     * Threshold value for discriminating whether a point has the PIER_OFFSET tag or not.
     */
    protected static final double PIER_OFFSET_WINDOW_EDGE = 500.0;
    /**
     * Amount deck should cantilever into the recess of the pier at the first and last loaded joints.
     */
    public static final double deckCantilever = 0.32;
    /**
     * Y-coordinate of the wear surface, including deck beams and deck surface.
     */
    public static final double wearSurfaceHeight = 0.80;
    /**
     * Height of 2d drawings of deck beam cross-sections.
     */
    protected static final double beamHeight = 0.90;
    /**
     * X-offset of rear (earth contact) surface of abutment from standard abutment joint.  
     */
    public static final double abutmentInterfaceOffset = 1.0;
    /**
     * Points on a piecewise curve of the access road in its own coordinate system.  The curve's low point is (0,0) and
     * it extends in a parabolic section to the right and upward until its slope matches the access slope.  This is
     * carried right in a straight slope out to a large x-value <code>accessLength</code> so that it can be used to 
     * form a polygon for the earth cross-section that certainly extends beyond any reasonable view.
     */
    public static final Affine.Point[] accessCurve = initializeAccessCurve();
    /**
     * In the piecewise accessCurive, this is the distance from the origin to the transition point where the 
     * parabola ends and the straight slope begins. Since the parabola is also the origin, this is also an x-coordinate.
     */
    protected static final double tangentOffset = 8.0;
    /**
     * Length of the straight slope part of the curve.  Long enough to extend beyond any reasonable view of the bridge.
     */
    public static final double accessLength = INF - tangentOffset;
    /**
     * Slope of the straight slope part of the access curve.
     */
    public static final double accessSlope = 1.0 / 6.0;
    /**
     * X-offset from standard abutment joint to face of step.
     */
    public static final double abutmentStepInset = -0.45;
    /**
     * Y-offset from standard abutment joint to horizontal face of step.
     */
    public static final double abutmentStepHeight = -0.35;
    /**
     * X-offset from standard abutment joint to visible abutment face.
     */
    public static final double abutmentStepWidth = 0.25;
    /**
     * Polygon points for a standard abutment. First two points must be the wear surface.
     */
    protected static final Affine.Point[] standardAbutmentPoints = {
        new Affine.Point(-abutmentInterfaceOffset, wearSurfaceHeight), 
        new Affine.Point(abutmentStepInset, wearSurfaceHeight), 
        new Affine.Point(abutmentStepInset, abutmentStepHeight), 
        new Affine.Point(abutmentStepWidth, abutmentStepHeight),
        new Affine.Point(abutmentStepWidth, -5.0), 
        new Affine.Point(0.75, -5.0), 
        new Affine.Point(0.75, -5.5), 
        new Affine.Point(-2.0, -5.5), 
        new Affine.Point(-2.0, -5.0), 
        new Affine.Point(-abutmentInterfaceOffset, -5.0)};
    /**
     * Polygon points for an arch abutment. First two points must be the wear surface.  Y-coordinates that must be
     * adjusted for arch height are tagged by adding ARCH_OFFSET.
     */
    protected static final Affine.Point[] archAbutmentPoints = {
        new Affine.Point(-abutmentInterfaceOffset, wearSurfaceHeight), 
        new Affine.Point(abutmentStepInset, wearSurfaceHeight), 
        new Affine.Point(abutmentStepInset, abutmentStepHeight + ARCH_OFFSET), 
        new Affine.Point(abutmentStepWidth, abutmentStepHeight + ARCH_OFFSET), 
        new Affine.Point(abutmentStepWidth, -5.0 + ARCH_OFFSET), 
        new Affine.Point(0.75, -5.0 + ARCH_OFFSET), 
        new Affine.Point(0.75, -5.5 + ARCH_OFFSET), 
        new Affine.Point(-2.0, -5.5 + ARCH_OFFSET), 
        new Affine.Point(-2.0, -5.0 + ARCH_OFFSET), 
        new Affine.Point(-abutmentInterfaceOffset, -5.0 + ARCH_OFFSET)};
    /**
     * Polygon points for a pier. Y-coordinates that must be adjusted for pier height are tagged by adding PIER_OFFSET.
     */
    protected static final Affine.Point[] pierPoints = {
        new Affine.Point(0.5, -0.35), 
        new Affine.Point(0.5, -0.35 + PIER_OFFSET), 
        new Affine.Point(0.75, -0.35 + PIER_OFFSET), 
        new Affine.Point(0.75, -7.5 + PIER_OFFSET), 
        new Affine.Point(1.40, -7.5 + PIER_OFFSET), 
        new Affine.Point(1.40, -8.0 + PIER_OFFSET), 
        new Affine.Point(-1.40, -8.0 + PIER_OFFSET), 
        new Affine.Point(-1.40, -7.5 + PIER_OFFSET), 
        new Affine.Point(-0.75, -7.5 + PIER_OFFSET), 
        new Affine.Point(-0.75, -0.35 + PIER_OFFSET), 
        new Affine.Point(-0.5, -0.35 + PIER_OFFSET), 
        new Affine.Point(-0.5, -0.35)};
    /**
     * Minimum world x-units (meters) to allow when computing viewing window.
     */
    protected static final double drawingXMargin = 3;
    /**
     * Vertical distance from grade line, y=0, of terrain polygon to surface of polygon representing water in gap.
     */
    protected static final double waterBelowGrade = 26.4;
    /**
     * Maximum height above grade that the bridge can be accordint to the design specs.
     */
    public static final double overheadClearance = 8.0;
    /**
     * Index of the left shore point in the terrain elevation cross-section 
     * polygon <code>elevationTerrainPoints</code>.
     */
    protected static final int leftShoreIndex = 25;
    /**
     * Index of the right shore point in the terrain elevation cross-section 
     * polygon <code>elevationTerrainPoints</code>.
     */
    protected static final int rightShoreIndex = 16;
    /**
     * Width of gap before earthwork.
     */
    protected static final double halfNaturalGapWidth = 22.0;
    /**
     * Polygon points for terrain elevation cross-section.  Start at lower right and then clockwise.  Two points with 
     * y-coordinates of -waterBelowGrade and those between describe a polygon for the water in the bridge gap.
     */
    protected static final Affine.Point[] elevationTerrainPoints = {
        new Affine.Point(INF, -INF), 
        new Affine.Point(INF, 0.0), 
        new Affine.Point(25.03, 0.0), 
        new Affine.Point(24.51, -0.3), 
        new Affine.Point(22.75, -0.71), 
        new Affine.Point(21.93, -2.95), 
        new Affine.Point(21.33, -4.75),
        new Affine.Point(20.70, -6.85),
        new Affine.Point(19.56, -7.48), 
        new Affine.Point(19.11, -8.94),
        new Affine.Point(18.62, -10.81), 
        new Affine.Point(17.8, -12.84), 
        new Affine.Point(16.22, -14.0), 
        new Affine.Point(14.5, -17.66),
        new Affine.Point(12.36, -21.33), 
        new Affine.Point(10.98, -24.59), 
        new Affine.Point(9.58, -waterBelowGrade), 
        new Affine.Point(8.12, -27.66), 
        new Affine.Point(6.54, -28.63), 
        new Affine.Point(5.04, -29.64), 
        new Affine.Point(4.48, -29.83), 
        new Affine.Point(0.28, -30.47), 
        new Affine.Point(-4.18, -29.83), 
        new Affine.Point(-5.46, -29.19),
        new Affine.Point(-6.96, -27.32),
        new Affine.Point(-8.24, -waterBelowGrade), 
        new Affine.Point(-10.37, -25.6), 
        new Affine.Point(-12.14, -23.21), 
        new Affine.Point(-12.48, -21.89), 
        new Affine.Point(-13.04, -20.14), 
        new Affine.Point(-14.5, -17.25), 
        new Affine.Point(-16.04, -15.38),
        new Affine.Point(-16.53, -13.88),
        new Affine.Point(-18.20, -11.17),
        new Affine.Point(-19.9, -7.93),
        new Affine.Point(-21.85, -3.07),
        new Affine.Point(-22.57, -0.74),
        new Affine.Point(-23.92, 0.0),
        new Affine.Point(-INF, 0.0),
        new Affine.Point(-INF, -INF),
    };

    /**
     * The minimum essential extent of the drawing for preferred window calculation purposes.  The initial
     * value is meaningless, but sometimes useful for development.  This extent does <i>not</i> include the
     * anchorages, if any, so that views needing to compute a window big enough for anchorages, whether or not
     * they are present, have enough information to do so.
     */
    protected final Rectangle.Double drawingExtent = new Rectangle.Double(0, -24, 44, 32);
    /**
     * Half of the gap width after earthwork.  Also the actual length of bridge excluding the small deck cantilevers.
     */
    protected double halfCutGapWidth;
    /**
     * World x-coordinates for left access curve.
     */
    protected final int[] leftAccessX = new int[accessCurve.length];
    /**
     * World y-coordinates for left access curve.
     */
    protected final int[] leftAccessY = new int[accessCurve.length];
    /**
     * Number of valid points in the left access curve.  Normally 2 when there is no cut needed
     * for the access or <code>accessCurve.length</code> when it is.
     */
    protected int nLeftAccessPoints;
    /**
     * World x-coordinates for right access curve.
     */
    protected final int[] rightAccessX = new int[accessCurve.length];
    /**
     * World y-coordinates for right access curve.
     */
    protected final int[] rightAccessY = new int[accessCurve.length];
    /**
     * Number of valid points in the right access curve.  Normally 2 when there is no cut needed
     * for the access or <code>accessCurve.length</code> when it is.
     */
    protected int nRightAccessPoints;
    /**
     * Re-usable scratch polygon for various purposes.
     */
    protected Polygon polygon = new Polygon();
    /**
     * A preferred drawing window that must be computed by subclass versions of
     * <code>loadPreferredDrawingWindow()</code>. 
     */
    protected final Rectangle.Double preferredDrawingWindow = new Rectangle.Double(-12, -29, 68, 42);
    /**
     * Index of the point in the terrain elevation cross-section polygon that's hidden by the left abutment.
     */
    protected int leftAbutmentInterfaceTerrainIndex;
    /**
     * Index of the point in the terrain elevation cross-section polygon that's hidden by the right abutment.
     */
    protected int rightAbutmentInterfaceTerrainIndex;
    /**
     * X-coordinate of leftmost deck joint.
     */
    protected double xLeftmostDeckJoint;
    /**
     * X-coordinate of rightmost deck joint.
     */
    protected double xRightmostDeckJoint;
    /**
     * Y-coordinate of grade level.
     */
    protected double yGradeLevel;

    /**
     * Paint the abutment wear surface, which is a short linear feature.
     * 
     * @param g java graphics context
     * @param x0 first viewport x-coordinate of surface
     * @param x1 second viewport x-coordinate of surface
     * @param y viewport y-coordinate of the surface
     */
    protected void paintAbutmentWearSurface(Graphics2D g, int x0, int x1, int y) {} 
    
    /**
     * Paint the crossection of deck of the bridge view.
     * 
     * @param g java graphics context
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintDeck(Graphics2D g, ViewportTransform viewportTransform) {}

    /**
     * Paint the earth crossection of deck of the bridge view.
     * 
     * @param g java graphics context
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintEarthCrossSection(Graphics2D g, ViewportTransform viewportTransform) {}

    /**
     * Paint the peir of the bridge view.
     * 
     * @param g java graphics context
     * @param location where the top of the pier should be located
     * @param pierHeight height of pier, used for establishing base location
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintPier(Graphics2D g, Affine.Point location, double pierHeight, ViewportTransform viewportTransform) {}
    
    /**
     * Paint an arch abutment.
     * 
     * @param g java graphics context
     * @param location where the top of the pier should be located
     * @param mirror whether to mirror the abutment about the y-axis (for the right bank)
     * @param archHeight height of the abutment step
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintArchAbutment(Graphics2D g, Affine.Point location, boolean mirror, double archHeight, ViewportTransform viewportTransform) {}
    
    /**
     * Paint an anchorage.
     * 
     * @param g java graphics context
     * @param location where the anchorage joint is located
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintAnchorage(Graphics2D g, Affine.Point location,ViewportTransform viewportTransform) {}

    /**
     * Paint a standard abutment.
     *
     * @param g java graphics context
     * @param location location of joint on the abutment.
     * @param right whether this is right bank abutment (false is left bank)
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    public void paintStandardAbutment(Graphics2D g, Affine.Point location, boolean right, int nConstraints, ViewportTransform viewportTransform) {}

    /**
     * Paint the terrain profile.
     * 
     * @param g java graphics context
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintTerrainProfile(Graphics2D g, ViewportTransform viewportTransform) {}

    /**
     * Set contents of the rectangle <code>preferredDrawingWindow</code> to the preferred visible 
     * area for drawing the view.  It's only preferred because it's fine to ignore the aspect ratio
     * of the viewport here.
     * 
     * The value of <code>drawingExtent</code> is always available for use by this method.
     */
    protected abstract void loadPreferredDrawingWindow();

    /**
     * This loads a standard preferred window for drafting.  We put this
     * functionality in the base class for bridge views because multiple
     * subclasses need this standard way of setting the window.  Delegate
     * to this from loadPreferredDrawingWindow for such subclasses.
     */
    protected void loadStandardDraftingWindow() {
        preferredDrawingWindow.x = drawingExtent.x - drawingXMargin;
        preferredDrawingWindow.width = drawingExtent.width + 2 * drawingXMargin;
        if (conditions.isLeftAnchorage()) {
            preferredDrawingWindow.x -= DesignConditions.anchorOffset;
            preferredDrawingWindow.width += DesignConditions.anchorOffset;
        }
        if (conditions.isRightAnchorage()) {
            preferredDrawingWindow.width += DesignConditions.anchorOffset;
        }
        // Extra 3.5 shows bottom of lowest abutment position.
        preferredDrawingWindow.y = yGradeLevel - waterBelowGrade - 3.5;
        preferredDrawingWindow.height = yGradeLevel + overheadClearance + 1.0 - preferredDrawingWindow.y;
    }

    /**
     * Paint this view of the bridge.
     * 
     * @param g java graphics context
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected abstract void paint(Graphics2D g, ViewportTransform viewportTransform);
    
    /**
     * Pre-calculate a ramp down to the bridge with given access slope and a tangent parabolic
     * vertical curve at the origin.  The ramp should be longer than any possible drafting view could
     * portray.  We'll let Swing clip away the unused portion.
     *
     * @return array containing points on the curve
     */
    protected static Affine.Point[] initializeAccessCurve() {
        Affine.Point[] rtn = new Affine.Point[6];
        double xInc = tangentOffset / (rtn.length - 2);
        double A = 0.5 * accessSlope / tangentOffset;
        double x = 0;
        int i = 0;
        while (i < rtn.length - 1) {
            rtn[i] = new Affine.Point(x, A * x * x);
            x += xInc;
            i++;
        }
        rtn[i] = new Affine.Point(rtn[i - 1].x + accessLength, rtn[i - 1].y + accessSlope * accessLength);
        return rtn;
    }

    /**
     * Return the minimum essential extent of the bridge view.  This does not include anchorages.
     * 
     * @return rectangle including the minimum essential area needed to depict the bridge view
     */
    public Rectangle.Double getDrawingExtent() {
        return drawingExtent;
    }

    /**
     * Return the last design conditions used to initialize the view.
     * 
     * @return design conditions
     */
    public DesignConditions getConditions() {
        return conditions;
    }

    /**
     * Return the preferred drawing window last established by <code>loadPreferredDrawingWindow()</code>.
     * 
     * @return rectangle including the preferred drawing window
     */
    public Rectangle.Double getPreferredDrawingWindow() {
        return preferredDrawingWindow;
    }

    /**
     * Return the bridge that this view is depicting.
     * 
     * @return bridge model
     */
    public BridgeModel getBridgeModel() {
        return bridge;
    }

    /**
     * Return the template sketch attached to this view, if any.
     * 
     * @return template sketch or null if none is attached
     */
    public BridgeSketchView getBridgeSketchView() {
        return bridgeSketchView;
    }

    /**
     * Set the template sketch attached to this view.  Null means none. This probably ought to check 
     * to make sure the sketch design conditions match the view's bridge, but it doesn't.
     * 
     * @param bridgeSketchView new template sketch or null if none
     */
    public void setBridgeSketchView(BridgeSketchView bridgeSketchView) {
        this.bridgeSketchView = bridgeSketchView;
    }

    /**
     * Return the x-coordinate of the leftmost deck joint.
     * 
     * @return x-coordinate of leftmost deck joint
     */
    public double getLeftBankX() {
        return halfCutGapWidth - halfNaturalGapWidth;
    }
    
    /**
     * Return the x-coordinate of the rightmost deck joint.
     * 
     * @return x-coordinate of rightmost deck joint
     */
    public double getRightBankX() {
        return halfCutGapWidth + halfNaturalGapWidth;        
    }
    
    /**
     * Return the y-coordinate of the grade level in the view.
     * 
     * @return y-coorindinate of grade level
     */
    public double getYGradeLevel() {
        return yGradeLevel;
    }
    
    /**
     * A component for drawing a bridge view.
     */
    private class Drawing extends JLabel {
        private final ViewportTransform viewportTransform = new ViewportTransform();
        private double magFactor = 1;
        
        public Drawing(double magFactor, String noBridgeMessage) {
            super();
            this.magFactor = magFactor;
            setText(noBridgeMessage);
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);
            setOpaque(true);
        }
        
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D)g0;
            if (conditions == null) {
                super.paintComponent(g0);
                return;
            }
            AffineTransform savedTransform = g.getTransform();
            g.scale(1/magFactor, 1/magFactor);
            viewportTransform.setViewport(
                    0, magFactor * (getHeight() - 1), 
                    magFactor * getWidth(), -magFactor * getHeight());
            viewportTransform.setWindow(preferredDrawingWindow);
            BridgeView.this.paint(g, viewportTransform);
            g.setTransform(savedTransform);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    /**
     * Return a specialized label component that draws this view.
     * 
     * @param magFactor scale factor to apply; affects items not subject to viewport transforms, such as bitmaps and fill patterns
     * @return drawing component
     */
    public JLabel getDrawing(double magFactor) {
        return new Drawing(magFactor, null);
    }
    
    /**
     * Return a specialized label component that draws this view.
     * 
     * @param magFactor scale factor to apply; affects items not subject to viewport transforms, such as bitmaps and fill patterns
     * @param noBridgeMessage text that shows in the label if the bridge of this view is null
     * @return drawing component
     */
    public JLabel getDrawing(double magFactor, String noBridgeMessage) {
        return new Drawing(magFactor, noBridgeMessage);
    }

    /**
     * Initialize this view according to specified bridge design conditions.
     * 
     * @param conditions design conditions.
     */
    public void initialize(DesignConditions conditions) {
        this.conditions = conditions;
        if (conditions == null) {
            // Set rational values so we don't generate any spurious exceptions for divide by zero, etc.
            if (bridgeSketchView != null) {
                bridgeSketchView.setModel(null);
            }
            drawingExtent.x = drawingExtent.y = preferredDrawingWindow.x = preferredDrawingWindow.y = 0;
            drawingExtent.width = drawingExtent.height = preferredDrawingWindow.width = preferredDrawingWindow.height = 1;
            return;
        }
        
        // If an attached template sketch does not match the new conditions geometrically, remove it.
        if (bridgeSketchView != null) {
            BridgeSketchModel model = bridgeSketchView.getModel();
            if (model != null && !model.getDesignConditions().isGeometricallyIdentical(conditions)) {
                bridgeSketchView.setModel(null);
            }
        }

        // Precompute some useful values.
        xLeftmostDeckJoint = conditions.getXLeftmostDeckJoint();
        xRightmostDeckJoint = conditions.getXRightmostDeckJoint();
        yGradeLevel = DesignConditions.gapDepth - conditions.getDeckElevation() + wearSurfaceHeight;
        halfCutGapWidth = 0.5 * (xRightmostDeckJoint - xLeftmostDeckJoint);

        // Find indices in the terrain profile point array that are hidden by the abutments.  This gives us
        // a way to separate excavation area from remaining bank material.  Note: x coords of the elevation terrain
        // curve are descending (CCW order for the polygon).
        leftAbutmentInterfaceTerrainIndex = elevationTerrainPoints.length - 1;
        while (elevationTerrainPoints[leftAbutmentInterfaceTerrainIndex].x + halfCutGapWidth < xLeftmostDeckJoint - abutmentInterfaceOffset) {
            leftAbutmentInterfaceTerrainIndex--;
        }
        rightAbutmentInterfaceTerrainIndex = 0;
        while (elevationTerrainPoints[rightAbutmentInterfaceTerrainIndex].x + halfCutGapWidth > xRightmostDeckJoint + abutmentInterfaceOffset) {
            rightAbutmentInterfaceTerrainIndex++;
        }
        assert leftAbutmentInterfaceTerrainIndex > rightAbutmentInterfaceTerrainIndex;

        // Set essential extent to include entire are where joints can appear, excluding anchorages.
        drawingExtent.x = xLeftmostDeckJoint;
        drawingExtent.y = -conditions.getUnderClearance();
        drawingExtent.width = conditions.getSpanLength();
        drawingExtent.height = conditions.getUnderClearance() + conditions.getOverClearance();
        
        // Let subclasses establish preferred area to draw.
        loadPreferredDrawingWindow();
    }

    /**
     * Paint a standard abutment.
     * 
     * @param g java graphics context
     * @param fillPaint paint to use for filling inner area of abutment polygon
     * @param strokePaint paint to use for outer border and wear surface
     * @param location where the abutment joint should be located
     * @param mirror whether to mirror the abutment about the y-axis (for the right bank)
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintStandardAbutment(Graphics2D g, Paint fillPaint, Color strokePaint, 
            Affine.Point location, boolean mirror, ViewportTransform viewportTransform) {
        polygon.npoints = 0;
        for (int i = 0; i < standardAbutmentPoints.length; i++) {
            double xWorldAbutment = mirror ? -standardAbutmentPoints[i].x : standardAbutmentPoints[i].x;
            polygon.addPoint(viewportTransform.worldToViewportX(location.x + xWorldAbutment), viewportTransform.worldToViewportY(location.y + standardAbutmentPoints[i].y));
        }
        g.setPaint(fillPaint);
        g.fill(polygon);
        g.setPaint(strokePaint);
        g.draw(polygon);
        g.setColor(strokePaint);
        if (polygon.xpoints[0] < polygon.xpoints[1]) {
            paintAbutmentWearSurface(g, polygon.xpoints[0], polygon.xpoints[1], polygon.ypoints[1]);
        }
        else {
            paintAbutmentWearSurface(g, polygon.xpoints[1], polygon.xpoints[0], polygon.ypoints[1]);            
        }
    }

    /**
     * Paint an arch abutment.
     * 
     * @param g java graphics context
     * @param fillPaint paint to use for filling inner area of abutment polygon
     * @param strokePaint paint to use for outer border and wear surface
     * @param location where the top abutment joint should be located
     * @param mirror whether to mirror the abutment about the y-axis (for the right bank)
     * @param archHeight height of the abutment step
     * @param viewportTransform  viewport transform from world to screen/printer coordinates
     */
    protected void paintArchAbutment(Graphics2D g, Paint fillPaint, Color strokePaint,
            Affine.Point location, boolean mirror, double archHeight, ViewportTransform viewportTransform) {
        polygon.npoints = 0;
        for (int i = 0; i < archAbutmentPoints.length; i++) {
            double xWorldAbutment = mirror ? -archAbutmentPoints[i].x : archAbutmentPoints[i].x;
            double yWorldAbutment = archAbutmentPoints[i].y > ARCH_OFFSET_WINDOW_EDGE ? archAbutmentPoints[i].y - ARCH_OFFSET + archHeight : archAbutmentPoints[i].y;
            polygon.addPoint(viewportTransform.worldToViewportX(location.x + xWorldAbutment), viewportTransform.worldToViewportY(location.y + yWorldAbutment));
        }
        g.setPaint(fillPaint);
        g.fill(polygon);
        g.setPaint(strokePaint);
        g.draw(polygon);
        g.setColor(strokePaint);
        if (polygon.xpoints[0] < polygon.xpoints[1]) {
            paintAbutmentWearSurface(g, polygon.xpoints[0], polygon.xpoints[1], polygon.ypoints[1]);
        }
        else {
            paintAbutmentWearSurface(g, polygon.xpoints[1], polygon.xpoints[0], polygon.ypoints[1]);            
        }
    }

    /**
     * Paint a pier.
     * 
     * @param g java graphics context
     * @param fillPaint paint to use for filling inner area of abutment polygon
     * @param strokePaint paint to use for outer border and wear surface
     * @param location where the top abutment joint should be located
     * @param pierHeight height of pier, used to establish bottom location
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    protected void paintPier(Graphics2D g, Paint fillPaint, Color strokePaint, 
            Affine.Point location, double pierHeight, ViewportTransform viewportTransform) {
        polygon.npoints = 0;
        for (int i = 0; i < pierPoints.length; i++) {
            double xWorldPier = pierPoints[i].x;
            double yWorldPier = pierPoints[i].y > PIER_OFFSET_WINDOW_EDGE ? pierPoints[i].y - PIER_OFFSET - pierHeight : pierPoints[i].y;
            polygon.addPoint(viewportTransform.worldToViewportX(location.x + xWorldPier), viewportTransform.worldToViewportY(location.y + yWorldPier));
        }
        g.setPaint(fillPaint);
        g.fill(polygon);
        g.setPaint(strokePaint);
        g.draw(polygon);
    }

    /**
     * A helper routine to fills in access curves and the scratch polygon with the terrain 
     * elevation cross-section.  Called by subclasses who need these geometries, normally
     * for their implementations of <code>paintEarthCrossSection()</code>.
     * 
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    protected void setEarthProfilePolygonAndAccesses(ViewportTransform viewportTransform) {
        int x = 0;
        int y = 0;
        polygon.npoints = 0;
        // Trace left access from right to left.
        double xBase = xLeftmostDeckJoint - abutmentInterfaceOffset;
        if (conditions.isAtGrade()) {
            nLeftAccessPoints = 2;
            leftAccessX[0] = x = viewportTransform.worldToViewportX(xBase);
            leftAccessY[0] = y = viewportTransform.worldToViewportY(wearSurfaceHeight);
            polygon.addPoint(x, y);
            leftAccessX[1] = x = viewportTransform.worldToViewportX(xBase - tangentOffset - accessLength);
            leftAccessY[1] = y;
            polygon.addPoint(x, y);
        } else {
            nLeftAccessPoints = accessCurve.length;
            for (int i = 0; i < nLeftAccessPoints; i++) {
                leftAccessX[i] = x = viewportTransform.worldToViewportX(xBase - accessCurve[i].x);
                leftAccessY[i] = y = viewportTransform.worldToViewportY(wearSurfaceHeight + accessCurve[i].y);
                polygon.addPoint(x, y);
            }
        }
        // Now down deep below the screen.
        y = viewportTransform.worldToViewportY(-INF);
        polygon.addPoint(x, y);
        // Now to far right of right access.
        xBase = xRightmostDeckJoint + abutmentInterfaceOffset;
        x = viewportTransform.worldToViewportX(xBase + tangentOffset + accessLength);
        polygon.addPoint(x, y);
        // Now the right access curve from right to left.
        if (conditions.isAtGrade()) {
            nRightAccessPoints = 2;
            rightAccessX[0] = x;
            rightAccessY[0] = y = viewportTransform.worldToViewportY(wearSurfaceHeight);
            polygon.addPoint(x, y);
            rightAccessX[1] = x = viewportTransform.worldToViewportX(xBase);
            rightAccessY[1] = y;
            polygon.addPoint(x, y);
        } else {
            nRightAccessPoints = accessCurve.length;
            for (int i = nRightAccessPoints - 1; i >= 0; i--) {
                rightAccessX[i] = x = viewportTransform.worldToViewportX(xBase + accessCurve[i].x);
                rightAccessY[i] = y = viewportTransform.worldToViewportY(wearSurfaceHeight + accessCurve[i].y);
                polygon.addPoint(x, y);
            }
        }
        // Straight down to level of abutment interface vertex.  Must lie behind abutment.
        y = viewportTransform.worldToViewportY(elevationTerrainPoints[rightAbutmentInterfaceTerrainIndex].y + yGradeLevel);
        polygon.addPoint(x, y);
        // Now the portion of the elevation terrain between abutments.
        for (int i = rightAbutmentInterfaceTerrainIndex; i <= leftAbutmentInterfaceTerrainIndex; i++) {
            x = viewportTransform.worldToViewportX(elevationTerrainPoints[i].x + halfCutGapWidth);
            y = viewportTransform.worldToViewportY(elevationTerrainPoints[i].y + yGradeLevel);
            polygon.addPoint(x, y);
        }
        // Short segment left so final edge of polygon is vertical and certain to lie behind abutment.
        x = viewportTransform.worldToViewportX(xLeftmostDeckJoint - abutmentInterfaceOffset);
        polygon.addPoint(x, y);
    }

    /**
     * Return a point at the top of the pier. It's an error to call this for bridges with no pier.
     * 
     * @return location of top of pier in world coordinates
     */
    public Affine.Point getPierLocation() {
        return conditions.getPrescribedJointLocation(conditions.getPierJointIndex());
    }
    
    /**
     * Paint both abutments and the pier (if any) for this bridge view.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    protected void paintAbutmentsAndPier(Graphics2D g, ViewportTransform viewportTransform) {
        Affine.Point pLeft = conditions.getPrescribedJoint(0).getPointWorld();
        Affine.Point pRight =  conditions.getPrescribedJointLocation(conditions.getNLoadedJoints() - 1);
        if (conditions.isArch()) {
            double archHeight = -conditions.getUnderClearance();
            paintArchAbutment(g, pLeft, false, archHeight, viewportTransform);
            paintArchAbutment(g, pRight, true, archHeight, viewportTransform);
        } else {
            paintStandardAbutment(g, pLeft, false, conditions.isHiPier() ? 1 : 2, viewportTransform);
            paintStandardAbutment(g, pRight, true, 1, viewportTransform);
        }
        if (conditions.isPier()) {
            paintPier(g, getPierLocation(), conditions.getPierHeight(), viewportTransform);
        }
    }

    /**
     * Paint anchorages (if any) for this bridge view.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    protected void paintAnchorages(Graphics2D g, ViewportTransform viewportTransform) {
        if (conditions.isLeftAnchorage()) {
            paintAnchorage(g, conditions.getPrescribedJointLocation(conditions.getLeftAnchorageJointIndex()), viewportTransform);
        }
        if (conditions.isRightAnchorage()) {
            paintAnchorage(g, conditions.getPrescribedJointLocation(conditions.getRightAnchorageJointIndex()), viewportTransform);
        }
    }
    
    /**
     * Context to use for painting if user does not supply one.
     */
    private final BridgePaintContext defaultPaintContext = new BridgePaintContext();
    
    /** 
     * Paint the bridge to the given graphics.  Use the given viewport transform to take world to viewport 
     * coordinates.  Optionally label the drawing (with member numbers).
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     * @param ctx drawing context
     */    
    public void paintBridge(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext ctx) {
        if (bridge != null) {
            if (ctx == null) {
                ctx = this.defaultPaintContext;
            }
            Iterator<Member> me = bridge.getMembers().iterator();
            while (me.hasNext()) {
                me.next().paint(g, viewportTransform, ctx);
            }    
            Iterator<Joint> je = bridge.getJoints().iterator();
            while (je.hasNext()) {
                je.next().paint(g, viewportTransform, ctx);
            }
        }
    }
    
    /**
     * Paint the template sketch attached to this view, if any.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    protected void paintBridgeSketch(Graphics2D g, ViewportTransform viewportTransform) {
        if (bridgeSketchView != null) {
            bridgeSketchView.paint(g, viewportTransform);
        }
    }
}
