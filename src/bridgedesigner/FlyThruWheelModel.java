/*
 * FlyThruWheelModel.java
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

import com.jogamp.opengl.GL2;

/**
 * 3D model of truck wheel and tire in the animation.
 * @author Eugene K. Ressler
 */
class FlyThruWheelModel {
    
    // Number of segments in our approximation of a circle.
    private static final int segCount = 24;
    
    // Width of the tire tread.
    private static final float tireWidth = 0.2f;
    
    // Separation between dual (rear) wheels.
    private static final float dualSeparation = 0.03f;
    
    // Radii.
    public static final float tireRadius = 0.5f;
    private static final float tireInnerRadius = 0.3f;
    private static final float rimInnerRadius = 0.25f;
    private static final float spokeInnerRadius = 0.2f;  // also hub outer radius
    private static final float holeLocation = 0.6f;
    private static final float holeRadius = holeLocation * rimInnerRadius + (1f - holeLocation) * spokeInnerRadius;
    private static final float holeRadialSize = 0.02f;
    private static final float holeInnerRadius = holeRadius - 0.5f * holeRadialSize;
    private static final float holeOuterRadius = holeRadius + 0.5f * holeRadialSize;
    
    // Factor used to tilt sidewall normals outward at the tire outer radius and inward at the inner.
    private static final float sidewallBulge = .4f;
    
    // Same as above for rim.
    private static final float rimBulge = .4f;
    
    // Depth offsets are with respect to the tire sidewall.  These give heights of truncated cones that form
    // the rim, spokes, and hub. The hub itself is not truncated, but a full cone made to look rounded by bump mapping.
    private static final float innerRimDepthOffset = -.03f;
    private static final float innerSpokeDepthOffset = .04f;
    private static final float hubApexDepthOffset = .05f;
    
    // Some static calculations too tedious for hand work.
    private static final float spokeRadialWidth = rimInnerRadius - spokeInnerRadius;
    private static final float lengthHubNormal = (float)Math.sqrt(innerSpokeDepthOffset * innerSpokeDepthOffset + spokeRadialWidth * spokeRadialWidth);
    private static final float rHubNormal = innerSpokeDepthOffset / lengthHubNormal;
    private static final float zHubNormal = spokeRadialWidth / lengthHubNormal;
    private static final float hubSlope = (innerSpokeDepthOffset - innerRimDepthOffset) / spokeRadialWidth; 
    
    // Our spoke holes really aren't holes, but dark polygons that float this much in front of the truncated spoke cone.
    private static final float holeVisibilityOffset = 0.01f;
    
    // Compute intermediate offsets at spoke hole inner and outer radii.
    private static final float holeInnerOffset = innerRimDepthOffset + (rimInnerRadius - holeInnerRadius) * hubSlope + holeVisibilityOffset;
    private static final float holeOuterOffset = innerRimDepthOffset + (rimInnerRadius - holeOuterRadius) * hubSlope + holeVisibilityOffset;
    
    // Spoke holse are an interal number of circle segments, set here.
    private static final int holeWidthInSegs = 3;
    private static final int holeSpacingInSegs = 6;
    
    private static final float sidewallBulgeComplement = (float)Math.sqrt(1.0 - sidewallBulge * sidewallBulge);
    private static final float rimBulgeComplement =  (float)Math.sqrt(1.0 - rimBulge * rimBulge);
    private static float [] tireMaterial = { 0.3f, 0.3f, 0.3f, 1f };
    private static float [] rimMaterial = { 0.5f, 0.5f, 0.5f, 1f };
    private static float [] hubMaterial = { 1f, 0.549f, 0f , 1f };
    private static float [] capMaterial = { 0.4f, 0.4f, 0.4f, 1f };
    private static float [] flangeMaterial = { .5f * 1f, .5f * 0.549f, .5f * 0f , 1f };
    private static float [] holeMaterial = { 1f, 0.549f, 0f , 1f };
    
    // Precompute a unit circle for a bit of speed benefit.
    private static float [] xCircle = new float[segCount + 1];
    private static float [] yCircle = new float[segCount + 1];    
    static {
        for (int i = 0; i <= segCount; i++) {
            double theta = 2 * Math.PI * i / segCount;
            xCircle[i] = (float)Math.cos(theta);
            yCircle[i] = (float)Math.sin(theta);
        }
    }

    /**
     * Set the translucency of the wheel.
     * 
     * @param alpha alpha translucency of the wheel
     */
    public void setAlpha(float alpha) {
        tireMaterial[3] = rimMaterial[3] = hubMaterial[3] = capMaterial[3] = 
        capMaterial[3] = flangeMaterial[3] = holeMaterial[3] = alpha;
    }
    
    /**
     * Draw a tire only.  Z-axis is the axle, with x- and y-axis congruent to the rear (negative z) tire face.
     * Tire width then extends in positive z-direction.
     * 
     * @param gl GL2 context
     */
    private void drawTire(GL2 gl) {
        // tread
        gl.glColor4fv(tireMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(xCircle[i], yCircle[i], 0f);
            gl.glVertex3f(tireRadius * xCircle[i], tireRadius * yCircle[i], tireWidth);
            gl.glVertex3f(tireRadius * xCircle[i], tireRadius * yCircle[i], 0f);
        }
        gl.glEnd();
        // front sidewall
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(-sidewallBulge * xCircle[i], -sidewallBulge * yCircle[i], sidewallBulgeComplement);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], tireWidth);
            gl.glNormal3f(sidewallBulge * xCircle[i], sidewallBulge * yCircle[i], sidewallBulgeComplement);
            gl.glVertex3f(tireRadius * xCircle[i], tireRadius * yCircle[i], tireWidth);
        }
        gl.glEnd();
        // rear sidewall
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(sidewallBulge * xCircle[i], sidewallBulge * yCircle[i], -sidewallBulgeComplement);
            gl.glVertex3f(tireRadius * xCircle[i], tireRadius * yCircle[i], 0);
            gl.glNormal3f(-sidewallBulge * xCircle[i], -sidewallBulge * yCircle[i], -sidewallBulgeComplement);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], 0);
        }
        gl.glEnd();
        // flange
        gl.glColor4fv(flangeMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(-xCircle[i], -yCircle[i], 0f);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], 0f);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], tireWidth);
        }
        gl.glEnd();
    }
    
    /**
     * Draw a wheel only with no tire.  Z-axis is the axle with x- and y-axes congruent to the outer rim radius.
     * 
     * @param gl GL2 context
     */
    private void drawWheel(GL2 gl) {
        // rim
        gl.glColor4fv(rimMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(-rimBulge * xCircle[i], -rimBulge * yCircle[i], rimBulgeComplement);
            gl.glVertex3f(rimInnerRadius * xCircle[i], rimInnerRadius * yCircle[i], innerRimDepthOffset);
            gl.glNormal3f(rimBulge * xCircle[i], rimBulge * yCircle[i], rimBulgeComplement);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], 0f);
        }
        gl.glEnd();
        // rear closure
        // use rim material
        gl.glBegin(GL2.GL_POLYGON);
        gl.glNormal3f(0f, 0f, -1f);
        for (int i = segCount; i >= 0; i--) {
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], innerRimDepthOffset - holeVisibilityOffset);
        }
        gl.glEnd();
        // spokes
        gl.glColor4fv(hubMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(rHubNormal * xCircle[i], rHubNormal * yCircle[i], zHubNormal);
            gl.glVertex3f(spokeInnerRadius * xCircle[i], spokeInnerRadius * yCircle[i],innerSpokeDepthOffset);
            gl.glVertex3f(rimInnerRadius * xCircle[i], rimInnerRadius * yCircle[i], innerRimDepthOffset);
        }
        gl.glEnd();
        // hub
        gl.glColor4fv(capMaterial, 0);
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0f, 0f, 1f);
        gl.glVertex3f(0f, 0f, hubApexDepthOffset);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(rHubNormal * xCircle[i], rHubNormal * yCircle[i], zHubNormal);
            gl.glVertex3f(spokeInnerRadius * xCircle[i], spokeInnerRadius * yCircle[i], innerSpokeDepthOffset);
        }
        gl.glEnd();
        // holes
        gl.glColor4fv(holeMaterial, 0);
        for (int i = 0; i <= segCount; i += holeSpacingInSegs) {
            gl.glBegin(GL2.GL_QUAD_STRIP);
            gl.glNormal3f(0f, 0f, 0f);
            for (int j = 0; j <= holeWidthInSegs; ++j) {
                int k = (i + j) % segCount;
                gl.glVertex3f(holeInnerRadius * xCircle[k], holeInnerRadius * yCircle[k], holeInnerOffset);
                gl.glVertex3f(holeOuterRadius * xCircle[k], holeOuterRadius * yCircle[k], holeOuterOffset);
            }   
            gl.glEnd(); 
        }
    }
    
    /**
     * Display a single wheel with its tire.  Wheel is offset for rim to coincide with inner tire edge.
     * 
     * @param gl GL2 context for drawing.
     */
    public void displaySingle(GL2 gl) {
        drawTire(gl);
        gl.glPushMatrix();
        gl.glTranslatef(0f, 0f, tireWidth);
        drawWheel(gl);
        gl.glPopMatrix();
    }

    /**
     * Display a dual wheel with tires.  Z-axis is the axle.  Origin is between the wheels.
     * 
     * @param gl GL2 context for drawing.
     */
    public void displayDual(GL2 gl) {
        // join
        gl.glColor4fv(hubMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= segCount; i++) {
            gl.glNormal3f(xCircle[i], yCircle[i], 0f);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], dualSeparation);
            gl.glVertex3f(tireInnerRadius * xCircle[i], tireInnerRadius * yCircle[i], -dualSeparation);
        }
        gl.glEnd();
        // outer dual
        gl.glPushMatrix();
        gl.glTranslatef(0f, 0f, dualSeparation);
        drawWheel(gl);
        drawTire(gl);
        gl.glPopMatrix();
        // inner dual
        gl.glPushMatrix();
        gl.glTranslatef(0f, 0f, -dualSeparation - tireWidth);
        drawTire(gl);
        gl.glPopMatrix();
    }
};
