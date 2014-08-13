/*
 * BridgeCartoonView.java  
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import org.jdesktop.application.ResourceMap;
import bridgedesigner.Affine.Point;

/**
 * The cartoon used in <code>SetupWizard</code> to depict design conditions
 * dynamically and in various levels of completeness.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeCartoonView extends BridgeView {

    /**
     * Color used for sky area.
     */
    protected final Color skyColor = new Color(192, 255, 255);
    /**
     * Color used for concrete objects in the view: deck, abutments, pier.
     */
    protected final Color concreteColor = new Color(153, 153, 153);
    /**
     * Color used for earth cross-section in the view.
     */
    protected final Color earthColor = new Color(220, 208, 188);
    /**
     * Fill pattern used for excavated areas in the view.
     */
    protected final TexturePaint excavatationPaint = initializeExcavation();
    /**
     * Stroke used for the wear surfaces in the view: road, deck, abutment top.
     */
    protected final BasicStroke wearSurfaceStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    /**
     * Scratch rectangle used to hold the outline of the deck slab.
     */
    protected final Rectangle slab = new Rectangle();
    /**
     * Image for an iconic title block to overlay on the cartoon.
     */
    protected final Image titleBlock = BDApp.getApplication().getImageResource("titleblock.png");
    /**
     * Radius we'll use to depict joints, measured in pixels.
     */
    public static final int jointRadius = 2;
    /**
     * Drawing mask bit: show only the terrain.
     */
    public static final int MODE_TERRAIN_ONLY = 0;
    /**
     * Drawing mask bit: show only the terrain with measurements.
     */
    public static final int MODE_MEASUREMENTS = 1;
    /**
     * Drawing mask bit: show bridge, excavation, roadway, and bridge.
     */
    public static final int MODE_STANDARD_ITEMS = 2;
    /**
     * Drawing mask bit: show title block.
     */
    public static final int MODE_TITLE_BLOCK = 4;
    /**
     * Drawing mask bit: show selected joints only.
     */
    public static final int MODE_JOINTS = 8;
    /**
     * Drawing mode.  Default is to show only standard items.
     */
    private int mode = MODE_STANDARD_ITEMS;

    /**
     * Construct a default cartoon view.  We attach a sketch to show templates.
     */
    public BridgeCartoonView() {
        this.bridgeSketchView = new BridgeSketchCartoonView();
    }

    /**
     * Construct a default cartoon and attach the given bridge to draw.
     * 
     * @param bridge bridge to draw
     */
    public BridgeCartoonView(BridgeModel bridge) {
        this();
        this.bridge = bridge;
    }

    /**
     * Set the drawing mode for the cartoon.  See drawing mode mask bits, <code>MODE_...</code>, which can
     * be "or"ed to get different effects.
     * 
     * @param mode bitwise drawing mode
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    public void paint(Graphics2D g, ViewportTransform viewportTransform) {
        g.setColor(skyColor);
        g.fillRect(0, 0, viewportTransform.getAbsWidthViewport(), viewportTransform.getAbsHeightViewport());
        if (conditions != null) {
            paintTerrainProfile(g, viewportTransform);
            if ((mode & MODE_MEASUREMENTS) != 0) {
                paintMeasurements(g, viewportTransform);
            }
            if ((mode & MODE_STANDARD_ITEMS) != 0) {
                paintEarthCrossSection(g, viewportTransform);
                paintAbutmentsAndPier(g, viewportTransform);
                paintDeck(g, viewportTransform);
                paintBridgeSketch(g, viewportTransform);
                paintBridge(g, viewportTransform, null);
            }
            if ((mode & MODE_TITLE_BLOCK) != 0) {
                g.drawImage(titleBlock, 
                        viewportTransform.getAbsWidthViewport() - titleBlock.getWidth(null) - 4,
                        viewportTransform.getAbsHeightViewport() - titleBlock.getHeight(null) - 4,
                        null);
            }
        }
    }

    private void paintMeasurements(Graphics2D g, ViewportTransform viewportTransform) {
        ResourceMap resourceMap = BDApp.getResourceMap(BridgeCartoonView.class);
        g.setColor(Color.GRAY);
        // Grade line height.
        final int yGrade = viewportTransform.worldToViewportY(yGradeLevel);
        // Design height of water.
        final int yWater = viewportTransform.worldToViewportY(yGradeLevel - 24.0);
        // Useful gap x coordinates.
        final int xGapLeft = viewportTransform.worldToViewportX(getLeftBankX());
        final int xGapRight = viewportTransform.worldToViewportX(getRightBankX());
        final int xGapMiddle = (xGapLeft + xGapRight) / 2;
        // Generally useful tick dimension.
        final int tickHalfSize = 3;
        // Horizontal dimensino line, two ticks on the ends, and a label.
        final int yGapDim = yGrade - 20;
        final int yTickTop = yGapDim - tickHalfSize;
        final int yTickBottom = yGrade + tickHalfSize;
        Utility.drawDoubleArrow(g, xGapLeft, yGapDim, xGapRight, yGapDim);
        g.drawLine(xGapLeft, yTickTop, xGapLeft, yTickBottom);
        g.drawLine(xGapRight, yTickTop, xGapRight, yTickBottom);
        Labeler.drawJustified(g, resourceMap.getString("gapDimension.text"), xGapMiddle, yGapDim - 3,
                Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_BOTTOM, null);
        // Vertical dimension line, long tick on top, short tick on bottom, 
        final int xGapHeightDim = xGapMiddle - 40; 
        Utility.drawDoubleArrow(g, xGapHeightDim, yGrade, xGapHeightDim, yWater);
        final int yAir = (yGrade + yWater) / 2;
        Labeler.drawJustified(g, resourceMap.getString("gapHeightDimension.text"), xGapHeightDim + 3, yAir,
                Labeler.JUSTIFY_LEFT, Labeler.JUSTIFY_CENTER, null);
        g.drawLine(xGapHeightDim - tickHalfSize, yWater, xGapHeightDim + tickHalfSize, yWater);
        g.drawLine(xGapLeft - 3, yGrade, xGapHeightDim + tickHalfSize, yGrade);

        final int xSlopeIcon = xGapMiddle + 75;
        final int widthSlopeIcon = 24;
        final int heightSlopeIcon = widthSlopeIcon * 2;
        final int ySlopeIcon = yAir + heightSlopeIcon / 2;
        final int xSlopeIconTop = xSlopeIcon + widthSlopeIcon;
        final int ySlopeIconTop = ySlopeIcon - heightSlopeIcon;
        final int widthSlopeIconTail = 4;
        g.drawLine(xSlopeIcon, ySlopeIcon, xSlopeIcon, ySlopeIconTop);
        Labeler.drawJustified(g, "2", xSlopeIcon - 3, yAir, Labeler.JUSTIFY_RIGHT, Labeler.JUSTIFY_CENTER, null);
        g.drawLine(xSlopeIcon, ySlopeIconTop, xSlopeIconTop, ySlopeIconTop);
        Labeler.drawJustified(g, "1", xSlopeIcon + widthSlopeIcon / 2, ySlopeIconTop - 3,
                Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_BOTTOM, null);
        g.drawLine(xSlopeIcon - widthSlopeIconTail, ySlopeIcon + 2 * widthSlopeIconTail,
                xSlopeIconTop + widthSlopeIconTail, ySlopeIconTop - 2 * widthSlopeIconTail);
    }

    @Override
    public void paintStandardAbutment(Graphics2D g, Point location, boolean mirror,int nConstraints,ViewportTransform viewportTransform) {
        paintStandardAbutment(g, concreteColor, Color.BLACK, location, mirror, viewportTransform);
    }

    @Override
    public void paintArchAbutment(Graphics2D g, Point location, boolean mirror, double arch_height, ViewportTransform viewportTransform) {
        paintArchAbutment(g, concreteColor, Color.BLACK, location, mirror, arch_height, viewportTransform);
    }

    @Override
    public void paintPier(Graphics2D g, Point location, double pier_height, ViewportTransform viewportTransform) {
        paintPier(g, concreteColor, Color.BLACK, location, pier_height, viewportTransform);
    }

    @Override
    protected void paintAbutmentWearSurface(Graphics2D g, int x0, int x1, int y) {
        Stroke savedStroke = g.getStroke();
        g.setStroke(wearSurfaceStroke);
        g.drawLine(x0, y, x1 + 1, y);
        g.setStroke(savedStroke);
    }

    @Override
    protected void paintDeck(Graphics2D g, ViewportTransform viewportTransform) {
        final int ySlabTop = viewportTransform.worldToViewportY(wearSurfaceHeight);
        final int ySlabBottom = ySlabTop + 2;
        final int yBeamBottom = ySlabBottom + 3; //viewportTransform.worldToViewportDistance(beamHeight);

        final int xSlabLeft = viewportTransform.worldToViewportX(conditions.getXLeftmostDeckJoint() - deckCantilever);
        final int xSlabRight = viewportTransform.worldToViewportX(conditions.getXRightmostDeckJoint() + deckCantilever);
        // Draw the deck slab as a single polygon.
        slab.setFrameFromDiagonal(xSlabLeft, ySlabBottom, xSlabRight, ySlabTop);
        g.setPaint(Color.WHITE);
        g.fill(slab);
        g.setPaint(Color.BLACK);
        g.draw(slab);
        final Stroke savedStroke = g.getStroke();
        g.setStroke(wearSurfaceStroke);
        // Draw wear surface. +1 takes care of silly end cap asymmetry.
        g.drawLine(xSlabLeft, ySlabTop, xSlabRight + 1, ySlabTop);
        g.setStroke(savedStroke);
        // Draw the deck beams.
        for (int i = 0; i < conditions.getNLoadedJoints(); i++) {
            int x = viewportTransform.worldToViewportX(conditions.getPrescribedJointLocation(i).x);
            g.drawLine(x, ySlabTop, x, yBeamBottom);
            if ((mode & MODE_JOINTS) != 0) {
                g.setColor(Color.WHITE);
                g.fillOval(x - jointRadius, ySlabBottom, 2 * jointRadius, 2 * jointRadius);
                g.setColor(Color.BLACK);
                g.drawOval(x - jointRadius, ySlabBottom, 2 * jointRadius, 2 * jointRadius);
            }
        }

        // Draw the prescribed joints other than those on the deck.
        for (int i = conditions.getNLoadedJoints(); i < conditions.getNPrescribedJoints(); i++) {
            if ((mode & MODE_JOINTS) != 0 || i == conditions.getLeftAnchorageJointIndex() || i == conditions.getRightAnchorageJointIndex()) {
                drawJoint(g, viewportTransform, conditions.getPrescribedJointLocation(i));
            }
        }
    }

    private void drawJoint(Graphics2D g, ViewportTransform viewportTransform, Affine.Point pt) {
        int x = viewportTransform.worldToViewportX(pt.x);
        int y = viewportTransform.worldToViewportY(pt.y);
        g.setColor(Color.WHITE);
        g.fillOval(x - jointRadius, y - jointRadius, 2 * jointRadius, 2 * jointRadius);
        g.setColor(Color.BLACK);
        g.drawOval(x - jointRadius, y - jointRadius, 2 * jointRadius, 2 * jointRadius);
    }

    @Override
    protected void paintEarthCrossSection(Graphics2D g, ViewportTransform viewportTransform) {
        setEarthProfilePolygonAndAccesses(viewportTransform);
        g.setPaint(earthColor);
        g.fill(polygon);
        g.setPaint(Color.BLACK);
        g.draw(polygon);
        Stroke savedStroke = g.getStroke();
        g.setStroke(wearSurfaceStroke);
        for (int i = 1; i < nLeftAccessPoints; i++) {
            g.drawLine(leftAccessX[i - 1], leftAccessY[i - 1], leftAccessX[i], leftAccessY[i]);
        }
        for (int i = 1; i < nRightAccessPoints; i++) {
            g.drawLine(rightAccessX[i - 1], rightAccessY[i - 1], rightAccessX[i], rightAccessY[i]);
        }
        g.setStroke(savedStroke);
    }

    @Override
    protected void paintTerrainProfile(Graphics2D g, ViewportTransform viewportTransform) {
        polygon.npoints = 0;
        for (int i = rightShoreIndex; i <= leftShoreIndex; i++) {
            int x = viewportTransform.worldToViewportX(elevationTerrainPoints[i].x + halfCutGapWidth);
            int y = viewportTransform.worldToViewportY(elevationTerrainPoints[i].y + yGradeLevel);
            polygon.addPoint(x, y);
        }
        g.setPaint(Color.BLUE);
        g.fill(polygon);
        polygon.npoints = 0;
        for (int i = 0; i < elevationTerrainPoints.length; i++) {
            polygon.addPoint(
                    viewportTransform.worldToViewportX(elevationTerrainPoints[i].x + halfCutGapWidth),
                    viewportTransform.worldToViewportY(elevationTerrainPoints[i].y + yGradeLevel));
        }
        if ((mode & MODE_STANDARD_ITEMS) != 0) {
            g.setPaint(excavatationPaint);
        } else {
            g.setPaint(earthColor);
        }
        g.fill(polygon);
        g.setPaint(Color.BLACK);
        g.draw(polygon);
        if ((mode & MODE_STANDARD_ITEMS) != 0 && conditions.isArch() && bridgeSketchView.getModel() == null) {
            // Trace the arch line.
            g.setColor(Color.LIGHT_GRAY);
            int iArchJoints = conditions.getArchJointIndex();
            Affine.Point p1 = conditions.getPrescribedJointLocation(iArchJoints);
            Affine.Point p2 = conditions.getPrescribedJointLocation(conditions.getNPanels() / 2);
            Affine.Point p3 = conditions.getPrescribedJointLocation(iArchJoints + 1);
            double xMid = 0.5 * (p1.x + p3.x);
            double x1 = p1.x - xMid;
            double y1 = p1.y;
            double x2 = p2.x - xMid;
            double y2 = p2.y - 0.25 * (p2.y - p1.y);
            double a = (y2 - y1) / (x2 * x2 - x1 * x1);
            double b = y1 - a * x1 * x1;
            double xp, yp;
            xp = p1.x;
            yp = p1.y;
            int ixq = viewportTransform.worldToViewportX(xp);
            int iyq = viewportTransform.worldToViewportY(yp);
            while (xp < p3.x) {
                xp += 0.5 * DesignConditions.panelSizeWorld;
                double x = xp - xMid;
                yp = a * x * x + b;
                int ixp = viewportTransform.worldToViewportX(xp);
                int iyp = viewportTransform.worldToViewportY(yp);
                g.drawLine(ixq, iyq, ixp, iyp);
                ixq = ixp;
                iyq = iyp;
            }
        }
    }

    /**
     * Build a texture fill pattern to represent excavation.
     * 
     * @return excavation fill pattern
     */
    private TexturePaint initializeExcavation() {
        final int size = 8;
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = e.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        BufferedImage hatch = gc.createCompatibleImage(size, size, Transparency.OPAQUE);
        Graphics2D g = hatch.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(new Color(128, 64, 64));
        g.drawLine(0, 0, size - 1, size - 1);
        return new TexturePaint(hatch, new Rectangle(0, 0, size, size));
    }

    protected void loadPreferredDrawingWindow() {
        // TODO: Too many magic numbers here.
        final double xMargin = drawingXMargin + (44 - drawingExtent.width) / 2 + DesignConditions.anchorOffset;
        preferredDrawingWindow.x = drawingExtent.x - xMargin;
        preferredDrawingWindow.width = drawingExtent.width + 2 * xMargin;
        // Extra 3.5 shows bottom of lowest abutment position.
        preferredDrawingWindow.y = yGradeLevel - waterBelowGrade - 3.5;
        preferredDrawingWindow.height = yGradeLevel + overheadClearance + 1.0 - preferredDrawingWindow.y;
    }
}
