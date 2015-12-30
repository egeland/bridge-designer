/*
 * Gusset.java  
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
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.jogamp.opengl.GL2;

/**
 * Gussets for joints of 3d bridge members.
 * 
 * @author Eugene K. Ressler
 */
public class Gusset {

    // Fatten members virtually by this amount for gusset calculations.  This
    // must be more than zero, else gussets will fight with member surfaces for
    // visibility when drawn
    public static final float gussetMaterialThickness = 0.02f;
   
    // Associated joint.
    private final Joint joint;
    
    // Factory for making the 2d convex hull that is the gusset's outer (x-y) shape.
    // Not thread safe!
    private static final ConvexHullFactory hullFactory = new ConvexHullFactory(); 
    
    // List of associated members.
    private final List<MemberGeometry> members = new ArrayList<MemberGeometry>(4);
    
    // After initialization, contains the 2d convex hull that is the gusset's outer shape.
    private Affine.Point [] hull = null;
    
    // After initialization, contains the gusset's half-depth (z-axis).
    private double halfDepth = 0;

    // True iff this gusset interferes with the load way.
    private boolean interferingWithLoad = false;
    
    /**
     * Construct a gusset for the given joint.
     * 
     * @param joint joint associated with this gusset
     */
    public Gusset(Joint joint) {
        this.joint = joint;
    }

    /**
     * Return the half-depth of the gusset.  Only valid after initialization.
     * 
     * @return half-depth of the gusset based on largest impinging member
     */
    public double getHalfDepth() {
        return halfDepth;
    }

    /**
     * Return an indication of wheither the gusset could interfere with the load.
     * 
     * @return true iff the gusset could interfere with the load
     */
    public boolean isInterferingWithLoad() {
        return interferingWithLoad;
    }
    
    /**
     * Factory that will build an array of gussets for a given bridge.
     * 
     * @param bridge bridge to compute gussets for.
     * @return gussets for this bridge.  Indices match bridge joint indices.
     */
    public static Gusset [] getGussets(BridgeModel bridge) {
    
        // Construct a gusset for each joint.
        Gusset [] gussets = new Gusset[bridge.getJoints().size()];
        Iterator<Joint> je = bridge.getJoints().iterator();
        while (je.hasNext()) {
            Joint joint = je.next();
            gussets[joint.getIndex()] = new Gusset(joint);
        }
        
        // Make one pass over members, adding to both of the connected gussets.
        Iterator<Member> me = bridge.getMembers().iterator();
        while (me.hasNext()) {
            Member member = me.next();
            gussets[member.getJointA().getIndex()].add(member);
            gussets[member.getJointB().getIndex()].add(member);
        }
        
        // Initialize gussets.  For each, this computes the convex 
        // hull of interesting points around the respective joint using
        // geometry of connected members.
        for (int i = 0; i < gussets.length; i++) {
            gussets[i].initialize();
        }
        return gussets;
    }
    
    /**
     * Geometry information for one member of the bridge.
     */
    private class MemberGeometry {
        
        // Three critical points on the member axis and two vectors.  If w/2 = halfWidth, then
        // it looks lke this:
        //   -----^---------------------------------------------------------------------------
        //  |     |uPerp                                  ^                                   |
        //  |     |                                      w/2                                  |
        //  o p0  --O joint o p1                    ---------------           o p2    O Other joint
        //  |       |---u--->                            w/2                                  |
        //  |<-w/2->|<-w/2->|                             v                   |<-w/2->|<-w/2->|
        //   ---------------------------------------------------------------------------------
        public Affine.Point p0, p1, p2;
        public Affine.Vector u, uPerp;
        public double halfWidth;
        
        /**
         * Construct geometry information for one member.
         * 
         * @param member member to use for this geometry
         */
        public MemberGeometry(Member member) {
            Joint otherJoint = member.otherJoint(joint);
            halfWidth =  0.5 * member.getWidthInMeters() + gussetMaterialThickness;
            Affine.Vector v = otherJoint.getPointWorld().minus(joint.getPointWorld());
            u = v.unit(halfWidth);
            uPerp = u.perp();
            p0 = u.minus().toPoint();
            p1 = u.toPoint();
            p2 = v.minus(u).toPoint();
        }
    }
    
    /**
     * Add a member to this gusset.
     * 
     * @param member member to add
     */
    public void add(Member member) {
        members.add(new MemberGeometry(member));
    }
    
    /**
     * Initialize the gusset using the members that have already been added and the joint
     * with which the gusset was associated when it was created.
     */
    public void initialize() {
        halfDepth = 0;
        Iterator<MemberGeometry> iMGLeft = members.iterator();
        while (iMGLeft.hasNext()) {
            MemberGeometry mgLeft = iMGLeft.next();
            
            // Update the half-depth of the gusset to accomodate this member.
            if (mgLeft.halfWidth > halfDepth) {
                halfDepth = mgLeft.halfWidth;
            }
            
            // Add the joint box.
            hullFactory.add(mgLeft.p0.plus(mgLeft.uPerp));
            hullFactory.add(mgLeft.p0.minus(mgLeft.uPerp));
            hullFactory.add(mgLeft.p1.plus(mgLeft.uPerp));
            hullFactory.add(mgLeft.p1.minus(mgLeft.uPerp));
            
            // Add left-right edge intersections and mirror points.
            Iterator<MemberGeometry> iMGRight = members.iterator();
            while (iMGRight.hasNext()) {
                MemberGeometry mgRight = iMGRight.next();
                if (iMGLeft != mgRight) {
                    Affine.Point isect = Utility.intersection(
                            mgLeft.p0.plus(mgLeft.uPerp), 
                            mgLeft.p2.plus(mgLeft.uPerp), 
                            mgRight.p0.minus(mgRight.uPerp), 
                            mgRight.p2.minus(mgRight.uPerp));
                    if (isect != null) {
                        hullFactory.add(isect);
                        hullFactory.add(isect.minus(mgLeft.uPerp.times(2)));
                        hullFactory.add(isect.plus(mgRight.uPerp.times(2)));
                    } 
                }
            }
        }
        
        // Fetch the convex hull vertices and clear the factory for reuse.
        hull = hullFactory.getHull(null);
        hullFactory.clear();
        
        if (hull.length > 0) {
            // Find vertical extreme points.
            double yTop = hull[0].y;
            double yBottom = yTop;
            for (int i = 1; i < hull.length; i++)  {
                yTop = Math.max(yTop, hull[i].y);
                yBottom = Math.min(yBottom, hull[i].y);
            }
            double yJoint = joint.getPointWorld().y;
            interferingWithLoad = (yJoint + yTop > BridgeView.wearSurfaceHeight && yJoint + yBottom < 2.5);
        }
    }

    public void paint(Graphics2D g, ViewportTransform viewportTransform) {
        // Don't try to paint trivial gussets.
        if (hull.length < 2) {
            return;
        }
        int j = hull.length - 1;
        int [] xPoints = new int [hull.length];
        int [] yPoints = new int [hull.length];
        Point ptViewport = new Point();
        Affine.Vector jointPosition = joint.getPointWorld().position();
        for (int i = 0; i < hull.length; i++) {
            viewportTransform.worldToViewport(ptViewport, hull[i].plus(jointPosition));
            xPoints[i] = ptViewport.x;
            yPoints[i] = ptViewport.y;
        }
        g.setColor(Color.WHITE);
        g.fillPolygon(xPoints, yPoints, hull.length);
        g.setColor(Color.BLACK);
        g.drawPolygon(xPoints, yPoints, hull.length);
    }
    
    /**
     * Paint the gusset for OpenGL at the given z-coordinate.  The joint location is the origin.
     * Consequently this assumes that a translation in x and y has already been 
     * concatenated to the model-view transform has already been
     * 
     * @param gl OpenGL object for drawing
     * @param z z-coordinate of center line
     */
    public void paint(GL2 gl, double z) {
        // Don't try to paint trivial gussets.
        if (hull.length < 2) {
            return;
        }
        // TODO: Need an extrusion object to make this simpler.
        // Front panel.
        gl.glColor3fv(Joint.jointMaterial, 0);
        final double zFront = z + halfDepth;
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3d(0, 0, 1);
        gl.glVertex3d(0, 0, zFront);
        for (int i = 0; i < hull.length; i++) {
            gl.glVertex3d(hull[i].x, hull[i].y, zFront);
        }
        gl.glVertex3d(hull[0].x, hull[0].y, zFront);
        gl.glEnd();

        // Rear panel.
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        final double zRear = z - halfDepth;
        gl.glNormal3d(0, 0, -1);
        gl.glVertex3d(0, 0, zRear);
        for (int i = hull.length - 1; i >= 0; i--) {
            gl.glVertex3d(hull[i].x, hull[i].y, zRear);
        }
        gl.glVertex3d(hull[hull.length - 1].x, hull[hull.length - 1].y, zRear);
        gl.glEnd();
        
        // Outer edge.
        gl.glBegin(GL2.GL_QUADS);
        int j = hull.length - 1;
        for (int i = 0; i < hull.length; i++) {
            gl.glNormal3d(hull[i].y - hull[j].y, hull[j].x - hull[i].x, 0);
            gl.glVertex3d(hull[i].x, hull[i].y, zFront);
            gl.glVertex3d(hull[j].x, hull[j].y, zFront);
            gl.glVertex3d(hull[j].x, hull[j].y, zRear);
            gl.glVertex3d(hull[i].x, hull[i].y, zRear);
            j = i;
        }
        gl.glEnd();
    }
}
