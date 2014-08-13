package bridgedesigner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.Iterator;

/**
 * A bridge view specialized to draw the fixed eye animation.  FlyThru animation
 * has no similar view because its requirements diverge so far from the
 * BridgeView functionality, which is essentially 2d.  The fixed eye view is
 * more like 2.5d.
 *
 * @author 
 */
public class Bridge3dView extends BridgeView {

    private final FixedEyeTerrainModel terrain;
    private final FixedEyeAnimation.Config config;
    private final Point jointViewportCoordsFront [] = new Point[DesignConditions.maxJointCount];
    private final Point jointViewportCoordsRear [] = new Point[DesignConditions.maxJointCount];
    private int jointRadius = 3;
    private Stroke crossMemberStroke;
    private int deckThickness = 12;
    private int deckBeamHeight = 12;
    public static final Color darkRed = new Color (92, 0, 0);
    public static final Color darkBlue = new Color (0, 0, 92);
    public static final Color gray00 = Color.BLACK;
    public static final Color gray25 = Color.DARK_GRAY;
    public static final Color gray30 = new Color(80, 80, 80);
    public static final Color gray40 = new Color(95, 95, 95);
    public static final Color gray50 = Color.GRAY;
    public static final Color gray75 = Color.LIGHT_GRAY;
    public static final Color gray99 = Color.WHITE;
    private final int [] vpXmember = new int[4];
    private final int [] vpYmember = new int[4];
    private static final float deckHalfWidth = (float)FlyThruAnimation.deckHalfWidth;
    private static final PierModel pierModel = new PierModel();
    private final FixedEyeTruckModel truck = new FixedEyeTruckModel();

    Bridge3dView(BridgeModel bridge, FixedEyeTerrainModel terrain, FixedEyeAnimation.Config config) {
        this.bridge = bridge;
        this.terrain = terrain;
        this.config = config;
        for (int i = 0; i < jointViewportCoordsFront.length; i++) {
            jointViewportCoordsFront[i] = new Point();
            jointViewportCoordsRear[i] = new Point();
        }
    }

    @Override
    public void initialize(DesignConditions conditions) {
        super.initialize(conditions);
        if (conditions.isPier()) {
            pierModel.initialize((float)conditions.getPierHeight(), 4.8f);
        }
    }

    @Override
    protected void loadPreferredDrawingWindow() {
        loadStandardDraftingWindow();
    }

    private Color interpolatedColor(double forceRatio) {
        int r, g, b;
        final float f = Utility.clamp((float)forceRatio, -1f, 1f);
        if (f < 0.0f) {
            r = 255;
            g = b = (int)(255f * (1.0f + f));
        }
        else {
            r = g = (int)(255f * (1.0f - f));
            b = 255;
        }
        return new Color(r, g, b);
    }

    private void xformJoints(ViewportTransform viewportTransform,
            Analysis.Interpolation interpolation,
            float z, Point [] ptBuf) {
        // Fill buffer with transformed offset joint locations.
        final Iterator<Joint> je = bridge.getJoints().iterator();
        while (je.hasNext()) {
            final Joint joint = je.next();
            final int jointIndex = joint.getIndex();
            final Affine.Vector disp = interpolation.getDisplacement(jointIndex);
            final Affine.Point ptWorld = joint.getPointWorld();
            viewportTransform.worldToViewport(ptBuf[jointIndex],
                    ptWorld.x + disp.x, ptWorld.y + disp.y, z);
        }
    }

    private void paintMembers(Graphics2D g, ViewportTransform viewportTransform, float z,
            Analysis.Interpolation interpolation, Point [] ptBuf, Color color) {
        final Iterator<Member> me = bridge.getMembers().iterator();
        while (me.hasNext()) {
            final Member member = me.next();
            final int width = Math.max(6, viewportTransform.worldToViewportDistance(2 * member.getWidthInMeters()));
            final int memberIndex = member.getIndex();
            final double status = interpolation.getMemberStatus(memberIndex);
            final double forceRatio = interpolation.getForceRatio(memberIndex);
            if (!Analysis.isStatusBaseLength(status)) {
                paintMember(g,
                        ptBuf[member.getJointA().getIndex()], ptBuf[member.getJointB().getIndex()],
                        /* pixel of width */ width,
                        /* main color */ color == null ? interpolatedColor(forceRatio) : color,
                        /* rule color */ color == null ? forceRatio < 0 ? darkRed : darkBlue : gray25,
                        false);
            }
            else if (forceRatio < 0.0) {
                // Compression failure.
                paintParabola(g, viewportTransform, interpolation, z, 
                        member.getJointA(), member.getJointB(), 
                        width, (float)status,
                        /* main color */ color == null ? Color.RED : color,
                        /* rule color */ color == null ? darkRed : gray25);
            }
            else {
                // Tension failure.
                final Color mainColor = color == null ? Color.BLUE : color;
                final Color ruleColor = color == null ? darkBlue : gray25;
                final Joint ja = member.getJointA();
                final Joint jb = member.getJointB();
                final Affine.Point pa = ja.getPointWorld();
                final Affine.Point pb = jb.getPointWorld();
                // Transform end points in undisplaced positions
                // to get native length in viewport coords.
                viewportTransform.worldToViewport(vpBreakA, pa.x, pa.y, z);
                viewportTransform.worldToViewport(vpBreakB, pb.x, pb.y, z);
                final Point a = ptBuf[ja.getIndex()];
                final Point b = ptBuf[jb.getIndex()];
                double tBreak;
                try {
                    tBreak = 0.5 * vpBreakA.distance(vpBreakB) / a.distance(b);
                }
                catch(Exception e) {
                    tBreak = 0.5;
                }
                vpBreakA.x = (int)(0.5 + a.x + tBreak * (b.x - a.x));
                vpBreakA.y = (int)(0.5 + a.y + tBreak * (b.y - a.y));
                paintMember(g, a, vpBreakA, width, mainColor, ruleColor, true);
                vpBreakB.x = (int)(0.5 + b.x + tBreak * (a.x - b.x));
                vpBreakB.y = (int)(0.5 + b.y + tBreak * (a.y - b.y));
                paintMember(g, b, vpBreakB, width, mainColor, ruleColor, true);
            }
        }
    }

    /**
     * Paint a filled and ruled thick line. To make sure the fill and rule
     * match exactly, we'll use a polygon rather than a Swing thick stroke.
     * 
     * match e
     * @param g
     * @param a
     * @param b
     * @param width
     * @param color
     * @param ruleColor
     */
    private void paintMember(Graphics2D g, Point a, Point b, int width, Color color, Color ruleColor, boolean broken) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float halfWidthOverLength;
        try {
            halfWidthOverLength = .5f * width / (float)Math.sqrt(dx * dx + dy * dy);
        } catch (Exception e) {
            return;
        }
        // A vector of length = 1/2 * width that points from a to b.
        int tx = (int)(dx * halfWidthOverLength + .5f);
        int ty = (int)(dy * halfWidthOverLength + .5f);
        vpXmember[0] = a.x + ty;
        vpYmember[0] = a.y - tx;
        vpXmember[1] = b.x + ty;
        vpYmember[1] = b.y - tx;
        vpXmember[2] = b.x - ty;
        vpYmember[2] = b.y + tx;
        vpXmember[3] = a.x - ty;
        vpYmember[3] = a.y + tx;
        if (broken) {
            vpXmember[1] += tx;
            vpYmember[1] += ty;
            vpXmember[2] -= tx;
            vpYmember[2] -= ty;
        }
        g.setPaint(color);
        g.fillPolygon(vpXmember, vpYmember, 4);
        g.setPaint(ruleColor);
        g.drawPolygon(vpXmember, vpYmember, 4);
    }

    // Can't be smaller than 16 for broken member.
    // For parabola must be 2(2n + 1) for some n to have a point at the peak.
    private final float [] pts = new float[Math.max(16, 2 * (2 * 8 + 1))];
    private final float [] normals = new float[pts.length];
    private final int [] vpXfailedMember = new int[pts.length];
    private final int [] vpYfailedMember = new int[pts.length];
    private final double clx [] = new double[1];
    private final double cly [] = new double[1];
    private final Point vpBreakA = new Point();
    private final Point vpBreakB = new Point();

    private void paintParabola(Graphics2D g, ViewportTransform viewportTransform,
            Analysis.Interpolation interpolation,
            double z, Joint a, Joint b, int width, float arcLen,
            Color color, Color ruleColor)
    {
        Affine.Point pa = a.getPointWorld();
        Affine.Point pb = b.getPointWorld();
        Affine.Vector da = interpolation.getDisplacement(a.getIndex());
        Affine.Vector db = interpolation.getDisplacement(b.getIndex());
        final float xa = (float)(pa.x + da.x);
        final float ya = (float)(pa.y + da.y);
        final float xb = (float)(pb.x + db.x);
        final float yb = (float)(pb.y + db.y);
        final float dx = xb - xa;
        final float dy = yb - ya;
        final float len = (float)Math.sqrt(dx * dx + dy * dy);
        Member.makeParabola(pts, normals, len, Member.getParabolaHeight(len, arcLen));
        final float cos = dx / len;
        final float sin = dy / len;
        for (int i2 = 0; i2 < pts.length; i2 += 2) {
            float x = cos * pts[i2+0] - sin * pts[i2+1];
            float y = cos * pts[i2+1] + sin * pts[i2+0];
            pts[i2+0] = x + xa;
            pts[i2+1] = y + ya;
            x = cos * normals[i2+0] - sin * normals[i2+1];
            y = cos * normals[i2+1] + sin * normals[i2+0];
            normals[i2+0] = x;
            normals[i2+1] = y;
        }
        int i0 = 0;
        int i1 = vpXfailedMember.length - 1;
        for (int i2 = 0; i2 < pts.length; i2 += 2, ++i0, --i1) {
            viewportTransform.worldToViewport(clx, cly, 0, pts[i2+0], pts[i2+1], z);
            float px = (float)clx[0];
            float py = (float)cly[0];
            // Flip normal y because we're in viewport coordinates.
            float nx = normals[i2 + 0] * +0.5f * width;
            float ny = normals[i2 + 1] * -0.5f * width;
            vpXfailedMember[i0] = (int)(0.5f + px + nx);
            vpYfailedMember[i0] = (int)(0.5f + py + ny);
            vpXfailedMember[i1] = (int)(0.5f + px - nx);
            vpYfailedMember[i1] = (int)(0.5f + py - ny);
        }
        g.setPaint(color);
        g.fillPolygon(vpXfailedMember, vpYfailedMember, vpXfailedMember.length);
        g.setPaint(ruleColor);
        g.drawPolygon(vpXfailedMember, vpYfailedMember, vpXfailedMember.length);
    }

    private void paintJoint(Graphics2D g, Point p) {
        g.setPaint(Color.LIGHT_GRAY);
        final int d = 2 * jointRadius;
        g.fillOval(p.x - jointRadius, p.y - jointRadius, d, d);
        g.setPaint(Color.BLACK);
        g.drawOval(p.x - jointRadius, p.y - jointRadius, d, d);
    }

    private static final Stroke deckBeamStroke = new BasicStroke(2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    
    private void drawDeckBeam(Graphics2D g, Point a, Point b) {
        vpXmember[0] = a.x;
        vpYmember[0] = a.y;
        vpXmember[1] = b.x;
        vpYmember[1] = b.y;
        vpXmember[2] = b.x;
        vpYmember[2] = b.y - deckBeamHeight;
        vpXmember[3] = a.x;
        vpYmember[3] = a.y - deckBeamHeight;
        g.setPaint(gray30);
        g.fillPolygon(vpXmember, vpYmember, 4);
        Stroke savedStroke = g.getStroke();
        g.setStroke(deckBeamStroke);
        g.setPaint(gray00);
        g.drawPolygon(vpXmember, vpYmember, 4);
        g.setStroke(savedStroke);
    }

    private void paintCrossMember(Graphics2D g, Point a, Point b) {
        Stroke savedStroke = g.getStroke();
        g.setStroke(crossMemberStroke);
        g.setPaint(gray40);
        g.drawLine(a.x, a.y, b.x, b.y);
        g.setStroke(savedStroke);
    }

    private void paintDeckSurface(Graphics2D g, Color color, int dy) {
        g.setPaint(color);
        vpXmember[0] = jointViewportCoordsFront[0].x;
        vpYmember[0] = jointViewportCoordsFront[0].y - dy;
        vpXmember[1] = jointViewportCoordsRear[0].x;
        vpYmember[1] = jointViewportCoordsRear[0].y - dy;
        final int nPanels = conditions.getNPanels();
        for (int i = 0; i < nPanels; i++) {
            vpXmember[2] = jointViewportCoordsRear[i + 1].x;
            vpYmember[2] = jointViewportCoordsRear[i + 1].y - dy;
            vpXmember[3] = jointViewportCoordsFront[i + 1].x;
            vpYmember[3] = jointViewportCoordsFront[i + 1].y - dy;
            g.fillPolygon(vpXmember, vpYmember, 4);
            vpXmember[1] = vpXmember[2];
            vpYmember[1] = vpYmember[2];
            vpXmember[0] = vpXmember[3];
            vpYmember[0] = vpYmember[3];
        }
    }

    private void paintDeckEdge(Graphics2D g) {
        g.setPaint(gray75);
        final int nPanels = conditions.getNPanels();
        vpXmember[0] = jointViewportCoordsFront[0].x;
        vpYmember[0] = jointViewportCoordsFront[0].y - deckBeamHeight;
        vpXmember[1] = jointViewportCoordsFront[0].x;
        vpYmember[1] = jointViewportCoordsFront[0].y - deckBeamHeight - deckThickness;
        for (int i = 0; i < nPanels; i++) {
            vpXmember[2] = jointViewportCoordsFront[i+1].x;
            vpYmember[2] = jointViewportCoordsFront[i+1].y - deckBeamHeight - deckThickness;
            vpXmember[3] = jointViewportCoordsFront[i+1].x;
            vpYmember[3] = jointViewportCoordsFront[i+1].y - deckBeamHeight;
            g.fillPolygon(vpXmember, vpYmember, 4);
            vpXmember[1] = vpXmember[2];
            vpYmember[1] = vpYmember[2];
            vpXmember[0] = vpXmember[3];
            vpYmember[0] = vpYmember[3];
        }
    }

    private void paintDiagonalCrossMembers(Graphics2D g) {
        g.setPaint(Color.DARK_GRAY);
        final Iterator<Member> me = bridge.getMembers().iterator();
        while (me.hasNext()) {
            final Member member = me.next();
            Joint a = member.getJointA();
            Joint b = member.getJointB();
            Affine.Point aPtWorld = a.getPointWorld();
            Affine.Point bPtWorld = b.getPointWorld();
            // Diagonals where they won't interfere with roadway.
            if ((aPtWorld.y >= FlyThruAnimation.deckClearance &&
                 bPtWorld.y >= FlyThruAnimation.deckClearance)
                 || (aPtWorld.y <= 0 && bPtWorld.y <= 0)) {
                Point af = jointViewportCoordsFront[a.getIndex()];
                Point bf = jointViewportCoordsFront[b.getIndex()];
                Point ar = jointViewportCoordsRear[a.getIndex()];
                Point br = jointViewportCoordsRear[b.getIndex()];
                g.drawLine(ar.x, ar.y, bf.x, bf.y);
                g.drawLine(br.x, br.y, af.x, af.y);
            }
        }
    }

    private void paintTransverseMembers(Graphics2D g) {
        Iterator<Joint> je = bridge.getJoints().iterator();
        final int nLoadedJoints = conditions.getNLoadedJoints();
        final int nPrescribedJoints = conditions.getNPrescribedJoints();
        while (je.hasNext()) {
           Joint joint = je.next();
           final int i = joint.getIndex();
           if (i < nLoadedJoints) {
               drawDeckBeam(g, jointViewportCoordsFront[i], jointViewportCoordsRear[i]);
           }
           else {
               final double y = joint.getPointWorld().y;
               if (i >= nPrescribedJoints && (y < 0 || y > FlyThruAnimation.deckClearance)) {
                    paintCrossMember(g, jointViewportCoordsFront[i], jointViewportCoordsRear[i]);
               }
           }
        }
    }

    /**
     * Do nothing.  Bridge view was originally designed to draw only views
     * of the bridge without the truck or terrain. Painting is more complicated
     * for an animation, so we need custom parameters.  This is unused.
     * 
     * @param g Java graphics context
     * @param viewportTransform viewport transformation
     */
    @Override
    protected void paint(Graphics2D g, ViewportTransform viewportTransform) {}

    protected void paint(Graphics2D g, ViewportTransform viewportTransform,
            Analysis.Interpolation interpolation, double distanceTraveled) {

        // These could be computed only on repaint if generating too much garbage.
        jointRadius = viewportTransform.worldToViewportDistance(.2);
        crossMemberStroke = new BasicStroke(2 * jointRadius - 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);

        deckThickness = viewportTransform.worldToViewportDistance(conditions.getDeckThickness());
        deckBeamHeight = viewportTransform.worldToViewportDistance(BridgeView.wearSurfaceHeight) - deckThickness;
        final boolean lowView = viewportTransform.isAboveVanishingPoint(bridge.getJoints().get(0).getPointWorld().y);
        final int nJoints = bridge.getJoints().size();
        
        xformJoints(viewportTransform, interpolation, -deckHalfWidth, jointViewportCoordsRear);
        xformJoints(viewportTransform, interpolation, deckHalfWidth, jointViewportCoordsFront);

        // Rear members.
        paintMembers(g, viewportTransform, -deckHalfWidth, interpolation, jointViewportCoordsRear, Color.GRAY);

        // Different sequences depending on whether we're viewing from above or below roadway.
        if (lowView) {
            if (config.showTruck) {
                truck.paint(g, viewportTransform,
                        interpolation.getPtLoad(),
                        interpolation.getLoadRotation(),
                        distanceTraveled);
            }
            // This covers some anchorage sites.
            terrain.drawAbutmentFaces(g, viewportTransform);
            // Deck bottom.
            paintDeckSurface(g, gray00, deckBeamHeight);
            // Diagonals.
            paintDiagonalCrossMembers(g);
            // Transverse members at joints.
            paintTransverseMembers(g);
            // Edge of deck.
            paintDeckEdge(g);
            // Pier
            if (terrain.pierLocation != null && config.showAbutments) {
                pierModel.paint(g, viewportTransform, (float)terrain.pierLocation.x, (float)terrain.pierLocation.y, 0f);
            }
        }
        else { // High view...
            if (terrain.pierLocation != null && config.showAbutments) {
                pierModel.paint(g, viewportTransform, (float)terrain.pierLocation.x, (float)terrain.pierLocation.y, 0f);
            }
            // Diagonals.
            paintDiagonalCrossMembers(g);
            // Transverse members at joints.
            paintTransverseMembers(g);
            // Again covering anchorage sites.
            terrain.drawAbutmentTops(g, viewportTransform);
            terrain.patchRoadway(g, viewportTransform);
            // Deck top.
            paintDeckSurface(g, gray00, deckBeamHeight + deckThickness);
            if (config.showTruck) {
                truck.paint(g, viewportTransform,
                        interpolation.getPtLoad(),
                        interpolation.getLoadRotation(),
                        distanceTraveled);
            }
            // Edge of deck.
            paintDeckEdge(g);
        }

        // Front members.
        paintMembers(g, viewportTransform, deckHalfWidth, interpolation, jointViewportCoordsFront, 
                config.showForcesAsColors ? null : gray99);

        // Joints on front truss. Do this before terrain patch to cover anchorage joints
        // and more importantly joints of failed bridges.
        for (int i = 0; i < nJoints; i++) {
            paintJoint(g, jointViewportCoordsFront[i]);
        }

        // Patch the background to cover front anchorages.
        terrain.patchTerrain(g, viewportTransform);

        // Fixes overwrite of pier base by water.
        if (terrain.pierLocation != null && config.showAbutments) {
            pierModel.patch(g, viewportTransform, (float)terrain.pierLocation.x, (float)terrain.pierLocation.y, 0f);
        }
    }
}
