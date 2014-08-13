package bridgedesigner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The drafting board drawing of the bridge being designed.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeDraftingView extends BridgeView {
    /**
     * Color used to draw earth cross-section.
     */
    protected final Color earthColor = new Color(128, 64, 64);
    /**
     * Color used to draw concrete objects: abutments, pier, deck.
     */
    protected final Color concreteColor = new Color(128, 128, 0);
    /**
     * Dash pattern used for terrain profile.
     */
    protected static final float terrainProfileDashes[] = {4.0f, 3.0f};
    /**
     * Stroke used for terrain profile.
     */
    protected static final BasicStroke terrainProfileStroke = new BasicStroke(
            0.0f, // thickness = thin as possible
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, // miterLimit
            terrainProfileDashes, // dash pattern
            0.0f);  // dash phase
    /**
     * Stroke used for wear surfaces: road, abutment, and deck.
     */
    protected static final Stroke wearSurfaceStroke = new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    /**
     * Stroke used to draw deck I-beams cross-section.
     */
    protected static final Stroke beamStroke = new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    /**
     * Texture paint used for earth cross-section.
     */
    protected final TexturePaint earthPaint = initializeEarth();
    /**
     * Texture paint used for concrete objects: abutments, pier, deck
     */
    protected final TexturePaint concretePaint = initializeConcrete();
    /**
     * Texture paint used for drawing the subgrade cross-section.
     */
    protected final TexturePaint subgradePaint = initializeSubgrade();
    /**
     * Whether to label the members in the view.
     */
    protected boolean label = false;
    /**
     * Whether the template sketch should be visible in the view.
     */
    protected boolean templateVisible = true;

    /**
     * Construct a default drafting view.  Attach a bridge sketch to show templates.
     */
    public BridgeDraftingView() {
        this.bridgeSketchView = new BridgeSketchDraftingView();        
    }
    
    /**
     * Construct a drafting view and attach a given bridge.
     * 
     * @param bridge bridge to attach
     */
    public BridgeDraftingView(BridgeModel bridge) {
        this();
        this.bridge = bridge;
    }

    /**
     * Say whether members should be labeled with their numbers.
     * 
     * @param label true iff labels should be drawn
     */
    public void setLabel(boolean label) {
        this.label = label;
    }

    /**
     * Return whether members are being labeled in the view.
     * 
     * @return true iff members are labeled
     */
    public boolean isLabel() {
        return label;
    }

    /**
     * Say whether the template, if any, should be drawn in the view.
     * 
     * @param templateVisible true iff template should be drawn.
     */
    public void setTemplateVisible(boolean templateVisible) {
        this.templateVisible = templateVisible;
    }
    
    private final BridgePaintContext ctx = new BridgePaintContext();
    
    public void paint(Graphics2D g, ViewportTransform viewportTransform) {
        final int w = viewportTransform.getAbsWidthViewport();
        final int h = viewportTransform.getAbsHeightViewport();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (conditions != null) {
            paintDeck(g, viewportTransform);
            paintEarthCrossSection(g, viewportTransform);
            paintTerrainProfile(g, viewportTransform);
            paintAbutmentsAndPier(g, viewportTransform);
            if (templateVisible) {
                paintBridgeSketch(g, viewportTransform);
            }
            ctx.label = label;
            ctx.allowableSlenderness = conditions.getAllowableSlenderness();
            paintBridge(g, viewportTransform, ctx);
        }
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, w, h + 1);
    }

    @Override
    public void paintStandardAbutment(Graphics2D g, Affine.Point location, boolean mirror,int nConstraints,ViewportTransform viewportTransform) {
        paintStandardAbutment(g, concretePaint, concreteColor, location, mirror, viewportTransform);
    }
    
    @Override
    public void paintArchAbutment(Graphics2D g, Affine.Point location, boolean mirror, double arch_height, ViewportTransform viewportTransform) {
        paintArchAbutment(g, concretePaint, concreteColor, location, mirror, arch_height, viewportTransform);
    }

    @Override
    public void paintPier(Graphics2D g, Affine.Point location, double pier_height, ViewportTransform viewportTransform) {
        paintPier(g, concretePaint, concreteColor, location, pier_height, viewportTransform);
    }
    
    @Override
    protected void paintEarthCrossSection(Graphics2D g, ViewportTransform viewportTransform) {
        Stroke savedStroke = g.getStroke();
        setEarthProfilePolygonAndAccesses(viewportTransform);
        g.setPaint(earthPaint);
        g.fill(polygon);
        // Now stroke the edge of the portion of the elevation terrain between abutments.
        g.setColor(earthColor);
        int x0 = viewportTransform.worldToViewportX(elevationTerrainPoints[rightAbutmentInterfaceTerrainIndex].x + halfCutGapWidth);
        int y0 = viewportTransform.worldToViewportY(elevationTerrainPoints[rightAbutmentInterfaceTerrainIndex].y + yGradeLevel);
        for (int i = rightAbutmentInterfaceTerrainIndex + 1; i <= leftAbutmentInterfaceTerrainIndex; i++) {
            int x1 = viewportTransform.worldToViewportX(elevationTerrainPoints[i].x + halfCutGapWidth);
            int y1 = viewportTransform.worldToViewportY(elevationTerrainPoints[i].y + yGradeLevel);
            g.drawLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
        // Now the polygon for the subgrade and thick line for wear surface of the access roads. Right bank first.
        int subgradeHeight = viewportTransform.worldToViewportDistance(0.3);
        polygon.npoints = 0;
        for (int i = 0; i < nRightAccessPoints; i++) {
            polygon.addPoint(rightAccessX[i], rightAccessY[i]);
        }
        for (int i = nRightAccessPoints - 1; i >= 0; i--) {
            polygon.addPoint(rightAccessX[i], rightAccessY[i] + subgradeHeight);
        }
        g.setPaint(subgradePaint);
        g.fill(polygon);
        g.setPaint(earthColor);
        g.draw(polygon);
        // Now right bank wear surface.
        g.setStroke(wearSurfaceStroke);
        g.setColor(concreteColor);
        for (int i = 1; i < polygon.npoints / 2; i++) {
            g.drawLine(polygon.xpoints[i - 1], polygon.ypoints[i - 1], polygon.xpoints[i], polygon.ypoints[i]);
        }
        g.setStroke(savedStroke);
        // Now left bank.
        polygon.npoints = 0;
        for (int i = 0; i < nLeftAccessPoints; i++) {
            polygon.addPoint(leftAccessX[i], leftAccessY[i]);
        }
        for (int i = nLeftAccessPoints - 1; i >= 0; i--) {
            polygon.addPoint(leftAccessX[i], leftAccessY[i] + subgradeHeight);
        }
        g.setPaint(subgradePaint);
        g.fill(polygon);
        g.setPaint(earthColor);
        g.draw(polygon);
        // And left bank wear surface.
        g.setStroke(wearSurfaceStroke);
        g.setColor(concreteColor);
        for (int i = 1; i < polygon.npoints / 2; i++) {
            g.drawLine(polygon.xpoints[i - 1], polygon.ypoints[i - 1], polygon.xpoints[i], polygon.ypoints[i]);
        }
        g.setStroke(savedStroke);
    }
    
    private final Rectangle slab = new Rectangle();
    private int xBeamFlangeAnchor;
    private int yBeamFlangeAnchor;
    private int yDeckAnchor;
    private int yRoadAnchor;

    /**
     * Return viewport x-coordinate of the beam flange to use as the subject of the positionable label in the view.
     * Valid after every call to <code>paintDeck()</code>.
     * 
     * @return x-coordinate of label anchor
     */
    public int getXBeamFlangeAnchor() {
        return xBeamFlangeAnchor;
    }

    /**
     * Return viewport y-coordinate of the beam flange to use as the subject of the positionable label in the view.
     * Valid after every call to <code>paintDeck()</code>.
     * 
     * @return y-coordinate of label anchor
     */
    public int getYBeamFlangeAnchor() {
        return yBeamFlangeAnchor;
    }

    /**
     * Return viewport y-coordinate of the deck point to use as the subject of the positionable label in the view.
     * Valid after every call to <code>paintDeck()</code>.
     * 
     * @return y-coordinate of label anchor
     */
    public int getYDeckAnchor() {
        return yDeckAnchor;
    }

    /**
     * Return viewport y-coordinate of the roadway point to use as the subject of the positionable label in the view.
     * Valid after every call to <code>paintDeck()</code>.
     * 
     * @return y-coordinate of label anchor
     */
    public int getYRoadAnchor() {
        return yRoadAnchor;
    }

    @Override
    protected void paintDeck(Graphics2D g, ViewportTransform viewportTransform) {
        // Calculate deck beam dimensions in viewport pixel coordinates.
        final int halfBeamFlangeWidth = viewportTransform.worldToViewportDistance(0.18);
        final int ySlabTop = viewportTransform.worldToViewportY(wearSurfaceHeight);
        final int ySlabBottom = viewportTransform.worldToViewportY(wearSurfaceHeight - conditions.getDeckThickness());
        final int yBeamTop = ySlabBottom + 2;
        final int yBeamBottom = yBeamTop + viewportTransform.worldToViewportDistance(beamHeight);
        
        final int xSlabLeft = viewportTransform.worldToViewportX(conditions.getXLeftmostDeckJoint() - deckCantilever);
        final int xSlabRight = viewportTransform.worldToViewportX(conditions.getXRightmostDeckJoint() + deckCantilever);
        final Stroke savedStroke = g.getStroke();
        // Draw the deck slab as a single polygon.
        slab.setFrameFromDiagonal(xSlabLeft, ySlabBottom, xSlabRight, ySlabTop);
        g.setPaint(concretePaint);
        g.fill(slab);
        g.setPaint(concreteColor);
        g.draw(slab);
        g.setStroke(wearSurfaceStroke);
        // Draw wear surface. -1 is offset of center of 3 pixel wide line
        g.drawLine(xSlabLeft, ySlabTop, xSlabRight + 1, ySlabTop);
        // Draw the deck beams and also the deck slab joints.
        for (int i = 0; i < conditions.getNLoadedJoints(); i++) {
            int x = viewportTransform.worldToViewportX(conditions.getPrescribedJointLocation(i).x);
            g.setStroke(beamStroke);
            g.setColor(Color.GRAY);
            g.drawLine(x - halfBeamFlangeWidth + 1, yBeamTop, x + halfBeamFlangeWidth, yBeamTop);
            g.drawLine(x - halfBeamFlangeWidth + 1, yBeamBottom, x + halfBeamFlangeWidth, yBeamBottom);
            g.drawLine(x, yBeamTop, x, yBeamBottom);
            if (i != 0 && i != conditions.getNLoadedJoints() - 1) {
                g.setColor(concreteColor);
                g.setStroke(savedStroke);
                g.drawLine(x, ySlabTop, x, ySlabBottom);
            }
        }
        g.setStroke(savedStroke);
        // Update exported label anchor locations.
        xBeamFlangeAnchor = viewportTransform.worldToViewportX(conditions.getPrescribedJointLocation(1).x) + halfBeamFlangeWidth;
        yBeamFlangeAnchor = yBeamTop;
        yDeckAnchor = (ySlabBottom + ySlabTop) / 2;
        yRoadAnchor = ySlabTop - 2;
    }

    @Override
    protected void paintAbutmentWearSurface(Graphics2D g, int x0, int x1, int y) {
        Stroke savedStroke = g.getStroke();
        g.setStroke(wearSurfaceStroke);
        g.drawLine(x0, y, x1 + 1, y);
        g.setStroke(savedStroke);
    }

    @Override
    protected void paintTerrainProfile(Graphics2D g, ViewportTransform viewportTransform) {
        // Draw the high water mark.
        g.setColor(Color.BLUE);
        int x0 = viewportTransform.worldToViewportX(elevationTerrainPoints[leftShoreIndex].x + halfCutGapWidth);
        int y0 = viewportTransform.worldToViewportY(elevationTerrainPoints[leftShoreIndex].y + yGradeLevel);
        int x1 = viewportTransform.worldToViewportX(elevationTerrainPoints[rightShoreIndex].x + halfCutGapWidth);
        int y1 = viewportTransform.worldToViewportY(elevationTerrainPoints[rightShoreIndex].y + yGradeLevel);
        for (int i = 0; i < 3; i++) {
            g.drawLine(x0, y0, x1, y1);
            x0 += 30;
            x1 -= 30;
            y0 += 4;
            y1 += 4;
        }
        // Set up earth color and dotted line stroke.
        g.setColor(earthColor);
        Stroke savedStroke = g.getStroke();
        g.setStroke(terrainProfileStroke);
        // Draw right bank profile up to right abutment. Skip [0] and [length-1] because they're
        // the lower left and lower right polygon points, which we don't need here.
        x0 = viewportTransform.worldToViewportX(elevationTerrainPoints[1].x + halfCutGapWidth);
        y0 = viewportTransform.worldToViewportY(elevationTerrainPoints[1].y + yGradeLevel);
        for (int i = 2; i <= rightAbutmentInterfaceTerrainIndex; i++) {
            x1 = viewportTransform.worldToViewportX(elevationTerrainPoints[i].x + halfCutGapWidth);
            y1 = viewportTransform.worldToViewportY(elevationTerrainPoints[i].y + yGradeLevel);
            g.drawLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
        // Draw left bank profile starting with left abutment.
        x0 = viewportTransform.worldToViewportX(elevationTerrainPoints[leftAbutmentInterfaceTerrainIndex].x + halfCutGapWidth);
        y0 = viewportTransform.worldToViewportY(elevationTerrainPoints[leftAbutmentInterfaceTerrainIndex].y + yGradeLevel);
        for (int i = leftAbutmentInterfaceTerrainIndex + 1; i < elevationTerrainPoints.length - 1; i++) {
            x1 = viewportTransform.worldToViewportX(elevationTerrainPoints[i].x + halfCutGapWidth);
            y1 = viewportTransform.worldToViewportY(elevationTerrainPoints[i].y + yGradeLevel);
            g.drawLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
        g.setStroke(savedStroke);
    }
    
    private TexturePaint initializeConcrete() {
        final int size = 64;
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = e.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        BufferedImage hatch = gc.createCompatibleImage(size, size, Transparency.OPAQUE);
        Graphics2D g = hatch.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(concreteColor);
        Random r = new Random();
        for (int i = 0; i < size * size / 4; i++) {
            int x = r.nextInt(size);
            int y = r.nextInt(size);
            g.drawLine(x, y, x, y);
        }
        return new TexturePaint(hatch, new Rectangle(0, 0, size, size));
    }

    private TexturePaint initializeEarth() {
        final int size = 8;
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = e.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        BufferedImage hatch = gc.createCompatibleImage(size, size, Transparency.OPAQUE);
        Graphics2D g = hatch.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(earthColor);
        // Can't use this:
        // g.drawLine(0, size - 1, size - 1, 0);
        // Rather need to phase shift the pattern from 0 for cases where we're scaling down. Else hatch disappears.
        g.drawLine(1, size - 1, size - 1, 1);
        g.drawLine(0, 0, 0, 0);
        return new TexturePaint(hatch, new Rectangle(0, 0, size, size));
    }

    private TexturePaint initializeSubgrade() {
        final int size = 8;
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = e.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        BufferedImage hatch = gc.createCompatibleImage(size, size, Transparency.OPAQUE);
        Graphics2D g = hatch.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(earthColor);
        g.drawLine(size / 2, 0, size / 2, size - 1);
        return new TexturePaint(hatch, new Rectangle(0, 0, size, size));
    }
    
    protected void loadPreferredDrawingWindow() {
        loadStandardDraftingWindow();
    }
}
