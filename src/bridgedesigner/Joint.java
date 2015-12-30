/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bridgedesigner;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import com.jogamp.opengl.GL2;
import org.jdesktop.application.ResourceMap;

/**
 * Implements joints for bridge models.
 * 
 * @author Eugene K. Ressler
 */
public class Joint implements HotEditableItem<BridgePaintContext> {

    /**
     * Pixel pixelRadius of 2d joint bitmap.
     */
    public static final int pixelRadius = 8;
    /**
     * World coordinate radius of joint in world coordinates (meters).
     */
    public static final double radiusWorld = 0.1;
    /**
     * Material color for joint.
     */
    public final static float [] jointMaterial = { 0.5f, 0.25f, 0.0f, 1.0f };
    /**
     * Height of a joint cap in world coorinates (meters)
     */
    private static final float capHeight = 0.1f; 
    /**
     * Amount of joint cylinder that shows between gusset and joint cap in woorld coordinates (meters).
     */
    private static final float capProtrusion = 0.03f;
    /**
     * World coordinate of this joint.
     */
    private Affine.Point ptWorld = new Affine.Point();
    /**
     * Whether the joint is fixed and therefore non-editable.  True for deck, pier, arch, and anchorage joints.
     */
    private final boolean fixed;
    /**
     * True iff this joint is selected.
     */
    private boolean selected = false;
    /**
     * Index of this joint in joint vector or -1 if none.
     */
    private int index = -1;
    /*
     * Bitmap images taken from resources
     */
    public final static Image fixedJointImage = BDApp.getApplication().getImageResource("fixedjoint.png");
    public final static Image normalJointImage = BDApp.getApplication().getImageResource("normaljoint.png");
    public final static Image selectedJointImage = BDApp.getApplication().getImageResource("selectedjoint.png");
    public final static Image hotJointImage = BDApp.getApplication().getImageResource("hotjoint.png");
    public final static Image hotSelectedJointImage = BDApp.getApplication().getImageResource("hotselectedjoint.png");
    /**
     * Number of segements in the cylinders used to represent joints and caps.
     */
    private static final int nSegments = 16;
    /**
     * Joint cylinder geomtary.
     */
    private static final float [] cylinderNormals;
    private static final float [] cylinderFrontVertices;
    private static final float [] cylinderRearVertices;
    private static final float [] acornVertices;
    /**
     * Matrix constant used to rotate joint cylinders into correct orientation.
     */
    private static final Homogeneous.Matrix rotateAboutX = new Homogeneous.Matrix(
            1f, 0f, 0f, 0f, 
            0f,-1f, 0f, 0f, 
            0f, 0f,-1f, 0f, 
            0f, 0f, 0f, 1f);

    /**
     * Construct a joint with given index, location, and fix-edness.
     * 
     * @param index index of joint
     * @param ptWorld location of joint (contents will be copied)
     * @param fixed true iff this is a fixed joint (e.g. deck, arch, anchorage, pier).
     */
    public Joint(int index, Affine.Point ptWorld, boolean fixed) {
        this.index = index;
        this.fixed = fixed;
        this.ptWorld.setLocation(ptWorld);
    }

    /**
     * Construct a non-fixed joint with given index and location.
     * 
     * @param index index of joint
     * @param ptWorld location of joint (contents will be copied)
     */
    public Joint(int index, Affine.Point ptWorld) {
        this(index, ptWorld, false);
    }

    /**
     * Construct an unindexed, non-fixed joint with given location.
     * 
     * @param ptWorld location of joint (contenst will be copied)
     */
    public Joint(Affine.Point ptWorld) {
        this(-1, ptWorld, false);
    }

    /**
     * Swap all fields of this joint with another.
     * 
     * @param otherJoint other joint
     */
    public void swapContents(Editable otherJoint) {
        Joint other = (Joint) otherJoint;
        Affine.Point tmpPtWorld = ptWorld;
        ptWorld = other.ptWorld;
        other.ptWorld = tmpPtWorld;
    }

    /**
     * Return true iff this joint is fixed (e.g. deck, arch, pier, or anchorage).
     * @return true iff this joint is fixed  (e.g. deck, arch, pier, or anchorage)
     */
    public boolean isFixed() {
        return fixed;
    }

    /**
     * Return 2D point location of this joint.
     * 
     * @return location point
     */
    public Affine.Point getPointWorld() {
        return ptWorld;
    }

    /**
     * Set the index of this joint.
     * 
     * @param index joint index or -1 if none
     */
    public void setIndex(int index) {
        this.index = index;
    }
    
    /**
     * Return the index of this joint.
     * 
     * @return joint index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Return the number of this joint.
     * 
     * @return joint number
     */
    public int getNumber() {
        return index + 1;
    }

    /**
     * Return true iff this joint is selected.
     * 
     * @return true iff this joint is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set whether this joint is selected or not.
     * 
     * @param selected whether joint is selected or not
     * @return true iff the selection status of the joint changed
     */
    public boolean setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            return true;
        }
        return false;
    }

    /**
     * Return true iff this joint is at the given location.
     * 
     * @param ptWorld location to test for coincidence with this joint
     * @return true iff this joint is at the given point
     */
    boolean isAt(Affine.Point ptWorld) {
        return this.ptWorld.distanceSq(ptWorld) < Utility.smallSq;
    }
    
    private final Point ptViewport = new Point();

    private void drawJointImage(Graphics2D g, ViewportTransform viewportTransform, Image image) {
        viewportTransform.worldToViewport(ptViewport, ptWorld);
        g.drawImage(image, ptViewport.x - Joint.pixelRadius, ptViewport.y - Joint.pixelRadius, null);
    }

    /**
     * Paint the joint in 2D.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform between world and screnn coordinates
     * @param ctx painting context
     */
    public void paint(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext ctx) {
        if (ctx.blueprint) {
            // We're printing, so paint gusset with small + mark over joint location.
            viewportTransform.worldToViewport(ptViewport, ptWorld);
            int r = viewportTransform.worldToViewportDistance(radiusWorld);
            // Fill and draw gusset polygon.
            if (ctx.gussets != null) {
                ctx.gussets[index].paint(g, viewportTransform);
            }
            else {
                // Fill and draw circular pin.
                g.setColor(Color.WHITE);
                g.fillOval(ptViewport.x - r, ptViewport.y - r, 2 * r, 2 * r);
                g.setColor(Color.BLACK);
                g.drawOval(ptViewport.x - r, ptViewport.y - r, 2 * r, 2 * r);
            }
            // Draw small centerline mark.
            r *= 1.0/2.0;
            g.drawLine(ptViewport.x - r - 1, ptViewport.y, ptViewport.x + r, ptViewport.y);
            g.drawLine(ptViewport.x, ptViewport.y - r - 1, ptViewport.x, ptViewport.y + r);
        }
        else {
            // Screen drawing is a fixed-size bitmap blitted to the correct location.
            drawJointImage(g, viewportTransform, 
                    isFixed() ? fixedJointImage : 
                        isSelected() ? selectedJointImage : 
                            normalJointImage);
        }
    }

    /**
     * Paint the rollover appearance of the joint 
     * @param g
     * @param viewportTransform
     * @param ctx
     */
    public void paintHot(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext ctx) {
        drawJointImage(g, viewportTransform, isSelected() ? hotSelectedJointImage : hotJointImage);
    }

    /**
     * Set up static geometry for joint cylinder.
     */
    static {
        final int nNormalComponents = 3 * nSegments;
        cylinderNormals = new float [nNormalComponents];
        cylinderFrontVertices = new float [nNormalComponents + 3];
        cylinderRearVertices = new float [nNormalComponents + 3];
        acornVertices = new float [nNormalComponents + 3];
        
        cylinderFrontVertices[0] = cylinderRearVertices[0] = acornVertices[0] = 1.0f;
        cylinderFrontVertices[1] = cylinderRearVertices[1] = acornVertices[1] = 0.0f;
        cylinderFrontVertices[2] = 1.0f;
        cylinderRearVertices[2] = -1.0f;
        acornVertices[2] = 0.0f;
        final double dTheta = 2 * Math.PI / nSegments;
        double theta = 0.0f;
        for (int i = 0; i < nNormalComponents; i += 3) {
            theta += dTheta;
            cylinderFrontVertices[i + 3] = cylinderRearVertices[i + 3] = acornVertices[i + 3] = (float)Math.cos(theta);
            cylinderFrontVertices[i + 4] = cylinderRearVertices[i + 4] = acornVertices[i + 4] = (float)Math.sin(theta);
            cylinderFrontVertices[i + 5] = 1.0f; cylinderRearVertices[i + 5] = -1.0f; acornVertices[i + 5] = 0.0f;
            cylinderNormals[i + 0] = cylinderFrontVertices[i + 3];
            cylinderNormals[i + 1] = cylinderFrontVertices[i + 4];
            cylinderNormals[i + 2] = 0.0f;
        }
    }
    
    private void paintAcorn(GL2 gl) {
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0f, 0f, 1f);
        gl.glVertex3f(0, 0, capHeight);
        final float zNormal = (float)(radiusWorld * radiusWorld / capHeight);
        for (int i = 0; i < acornVertices.length; i += 3) {
            gl.glNormal3f(acornVertices[i], acornVertices[i+1], zNormal);
            gl.glVertex3fv(acornVertices, i);
        }
        gl.glNormal3f(acornVertices[0], acornVertices[1], zNormal);
        gl.glVertex3fv(acornVertices, 0);
        gl.glEnd();
    }
    
    private void paintRod(GL2 gl, double halfLength) {
        
        // Use smooth shading.
        gl.glShadeModel(GL2.GL_SMOOTH);
        
        // Draw the cylinder.
        gl.glPushMatrix();
        gl.glScaled(radiusWorld, radiusWorld, halfLength);
        gl.glColor3fv(jointMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i < cylinderNormals.length; i += 3) {
            gl.glNormal3fv(cylinderNormals, i);
            gl.glVertex3fv(cylinderFrontVertices, i);
            gl.glVertex3fv(cylinderRearVertices,  i);
        }
        gl.glNormal3fv(cylinderNormals, 0);
        gl.glVertex3fv(cylinderFrontVertices, 0);
        gl.glVertex3fv(cylinderRearVertices,  0);
        gl.glEnd();
        gl.glPopMatrix();

        // Rounded end caps.
        gl.glPushMatrix();
        gl.glTranslated(0.0, 0.0, halfLength);
        gl.glScaled(radiusWorld, radiusWorld, 1f);
        paintAcorn(gl);
        gl.glPopMatrix();
        
        gl.glPushMatrix();
        gl.glMultMatrixf(rotateAboutX.a, 0);
        gl.glTranslated(0.0, 0.0, halfLength);
        gl.glScaled(radiusWorld, radiusWorld, 1f);
        paintAcorn(gl);
        gl.glPopMatrix();        
    }

    public void paint(Graphics2D g, Affine.Vector disp, Affine.Point pt) {
        // Find displaced joint location available to caller.
        pt.x = ptWorld.x + disp.x;
        pt.y = ptWorld.y + disp.y;
    }

    /**
     * Paint the given joint detail in 3d.
     * 
     * @param gl Opengl object
     * @param disp joint displacement
     * @param halfWidth half-width of roadway gives truss displacement from z=0 plane.
     * @param gusset gusset to draw around the joint (in both trusses)
     * @param pt displaced joint location
     */
    public void paint(GL2 gl, Affine.Vector disp, float halfWidth, Gusset gusset, Affine.Point pt) {
        gl.glPushMatrix();
        
        // Find displaced joint location available to caller.
        pt.x = ptWorld.x + disp.x;
        pt.y = ptWorld.y + disp.y;
        
        //  Traslate in x and y.
        gl.glTranslated(pt.x, pt.y, 0.0);
        
        // Paint gussets on truss centerlines.
        gusset.paint(gl, halfWidth);
        gusset.paint(gl, -halfWidth);
        
        double gussetHalfDepth = gusset.getHalfDepth();
        
        if (0 < ptWorld.y && ptWorld.y < FlyThruAnimation.deckClearance) {
            gl.glTranslatef(0.0f, 0.0f, halfWidth);
            paintRod(gl, gussetHalfDepth + capProtrusion);
            gl.glTranslatef(0.0f, 0.0f, -2 * halfWidth);
            paintRod(gl, gussetHalfDepth + capProtrusion);
        }
        else {
            paintRod(gl, halfWidth + capProtrusion + gussetHalfDepth);
        }
        
        gl.glPopMatrix();
    }

    /**
     * Get a rectangle that contains this joint in viewport (screen) coordinates.
     * @param dst
     * @param viewportTransform
     */
    public void getViewportExtent(Rectangle dst, ViewportTransform viewportTransform) {
        if (dst == null) {
            dst = new Rectangle();
        }
        viewportTransform.worldToViewport(ptViewport, ptWorld);
        dst.setBounds(ptViewport.x, ptViewport.y, 0, 0);
        dst.grow(pixelRadius, pixelRadius);
    }

    /**
     * String version of joint is the tip text during rollovers.
     * 
     * @return string representation of joints
     */
    @Override
    public String toString() {
        return BDApp.getResourceMap(Joint.class).getString(
                isFixed() ? "fixedrollover.text" : "rollover.text",
                getPointWorld());
    }

    /**
     * Return a null cursor for rollovers.  This means the default cursor remains in effect.
     * 
     * @return null cursor
     */
    public Cursor getCursor() {
        return null;
    }
}
