/*
 * PierModel.java  
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

import com.jogamp.opengl.util.texture.Texture;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import com.jogamp.opengl.GL2;

/**
 * Graphical model of the pier in the 3d image.
 * 
 * @author Eugene K. Ressler
 */
public class PierModel {
    
    private static final float pierHalfWidth = 0.5f;
    private static final float pierCusp = 0.3f;
    private static final float pierTaper = 1.3f;
    private static final float pierBaseShoulder = 0.3f;
    private static final float pillowHeight = 0.4f;
    private float height;
    private float pillowFrontFace [] = new float[9];
    private float pillowRearFace [] = new float[9];
    private final static float [] pillowMaterial   = { .4f, .4f, .4f, 1f };
    private final static float [] pierMaterial = { .8f, .8f, .8f, 1f };
    private final Renderer3d renderer = new Renderer3d();
    
    /**
     * Prisms used to form the pier  base and pillar.
     */
    private class Prism {
        private final float [] topPolygon = new float[21];
        private final float [] bottomPolygon = new float[21];
        private final float [] sideNormals = new float[18];
        private final float [] topTexCoords = new float[14];
        private final float [] bottomTexCoords = new float[14];
        private final float [] capTexCoords = new float [12];
        private float texScale = 1f;
        
        public Prism(float w, float h, float d, float c, float taper, float texSize) {
            initialize(w, h, d, c, taper, texSize);
        }

        public Prism(float w, float h, float d, float c, float taper) {
            initialize(w, h, d, c, taper);
        }
        
        public Prism() {
        }
        
        /**
         * Initialization of all geometry, but with without texture coordinates.
         * 
         * @param w
         * @param h
         * @param d
         * @param c
         * @param taper
         */
        final public void initialize(float w, float h, float d, float c, float taper)
        {
            topPolygon[ 0] = 0; topPolygon[ 1] = 0; topPolygon[ 2] =-(d + c); // 0
            topPolygon[ 3] =-w; topPolygon[ 4] = 0; topPolygon[ 5] =-d;       // 1
            topPolygon[ 6] =-w; topPolygon[ 7] = 0; topPolygon[ 8] = d;       // 2
            topPolygon[ 9] = 0; topPolygon[10] = 0; topPolygon[11] = d + c;   // 3
            topPolygon[12] = w; topPolygon[13] = 0; topPolygon[14] = d;       // 4
            topPolygon[15] = w; topPolygon[16] = 0; topPolygon[17] =-d;       // 5
            topPolygon[18] = 0; topPolygon[19] = 0; topPolygon[20] =-(d + c); // 6

            for (int i = 0; i < topPolygon.length; i += 3) {
                bottomPolygon[i + 0] = taper * topPolygon[i + 0];
                bottomPolygon[i + 1] = -h;
                bottomPolygon[i + 2] = taper * topPolygon[i + 2];
            }
            for (int i = 3; i < topPolygon.length; i += 3) {
                float ax = bottomPolygon[i + 0 - 0] - bottomPolygon[i + 0 - 3];
                float ay = bottomPolygon[i + 1 - 0] - bottomPolygon[i + 1 - 3];
                float az = bottomPolygon[i + 2 - 0] - bottomPolygon[i + 2 - 3];
                float bx = topPolygon[i + 0 - 3] - bottomPolygon[i + 0 - 3];
                float by = topPolygon[i + 1 - 3] - bottomPolygon[i + 1 - 3];
                float bz = topPolygon[i + 2 - 3] - bottomPolygon[i + 2 - 3];
                float nx = ay * bz - az * by;
                float ny = az * bx - ax * bz;
                float nz = ax * by - ay * bx;
                float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                sideNormals[i + 0 - 3] = nx / len;
                sideNormals[i + 1 - 3] = ny / len;
                sideNormals[i + 2 - 3] = nz / len;
            }
        }

        /**
         * Initialization of geometry and including texture coordinates.
         *
         * @param w
         * @param h
         * @param d
         * @param c
         * @param taper
         * @param texSize
         */
        final public void initialize(float w, float h, float d, float c, float taper, float texSize) {
            initialize(w, h, d, c, taper);
            float len = 0f;
            for (int i = 3; i < topPolygon.length; i += 3) {
                float dx = topPolygon[i + 0] - topPolygon[i + 0 - 3];
                float dz = topPolygon[i + 2] - topPolygon[i + 2 - 3];
                len += (float)Math.sqrt(dx * dx + dz * dz);
            }
            // Find a factor to convert linear distance into texture (at top)
            texScale = len / Math.round(len / texSize);
            float s = 0f;
            topTexCoords[0] = 0; topTexCoords[1] = 0;
            bottomTexCoords[0] = 0; bottomTexCoords[1] = -h * texScale;
            int j = 2;
            for (int i = 3; i < topPolygon.length; i += 3, j += 2) {
                float dx = topPolygon[i + 0] - topPolygon[i + 0 - 3];
                float dz = topPolygon[i + 2] - topPolygon[i + 2 - 3];
                s += (float)Math.sqrt(dx * dx + dz * dz);
                topTexCoords[j + 0] = s * texScale; topTexCoords[j + 1] = 0;
                bottomTexCoords[j + 0] = s * texScale; bottomTexCoords[j + 1] = -h * texScale;
            }
            j = 0;
            for (int i = 0; i < topPolygon.length - 3; i += 3, j += 2) {
                capTexCoords[j + 0] =  texScale * topPolygon[i + 0];
                capTexCoords[j + 1] = -texScale * topPolygon[i + 2];
            }
        }

        private Paint [] prismFaceColors = {
            Bridge3dView.gray00, // dummy
            Bridge3dView.gray25,
            Bridge3dView.gray30,
            Bridge3dView.gray50,
            Bridge3dView.gray75,
            Bridge3dView.gray00, // dummy
        };

        private final Homogeneous.Matrix offset = new Homogeneous.Matrix();

        public void draw(Graphics2D g, ViewportTransform viewportTransform, 
                float dx, float dy, float dz, int beginFlag) {
            offset.setTranslation(dx, dy, dz);
            renderer.enableModelTransform(offset);
            renderer.begin(beginFlag);
            renderer.setRuleFlags(Renderer3d.RULE_V);
            renderer.setRulePaint(Color.WHITE);
            if(!viewportTransform.isRightOfVanishingPoint(dx)) {
                renderer.addVertex(g, viewportTransform,
                                topPolygon, 15,
                                bottomPolygon, 15,
                                prismFaceColors, 5,
                                -4);
            }
            else {
                renderer.addVertex(g, viewportTransform,
                                topPolygon, 3,
                                bottomPolygon, 3,
                                prismFaceColors, 0,
                                4);
            }
            renderer.end(g);
            if (!viewportTransform.isAboveVanishingPoint(dy)) {
                renderer.setPaint(Bridge3dView.gray75);
                renderer.begin(Renderer3d.POLYGON);
                renderer.addVertex(g, viewportTransform, topPolygon, 0, 6);
                renderer.end(g);
            }
            renderer.disableModelTransform();
        }

        /**
         * Special case code to fix upstream faces of base that are
         * overwritten by water patch.
         *
         * @param g
         * @param viewportTransform
         * @param dx
         * @param dy
         * @param dz
         */
        public void patch(Graphics2D g, ViewportTransform viewportTransform,
                float dx, float dy, float dz) {
            offset.setTranslation(dx, dy, dz);
            renderer.enableModelTransform(offset);
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.setRuleFlags(Renderer3d.RULE_V);
            renderer.setRulePaint(Color.WHITE);
            renderer.addVertex(g, viewportTransform,
                            topPolygon, 6,
                            bottomPolygon, 6,
                            prismFaceColors, 1,
                            3);
            renderer.end(g);
            renderer.disableModelTransform();
        }

        public void display(GL2 gl) {
            gl.glBegin(GL2.GL_QUADS);
            int i2 = 2;
            for (int i3 = 3; i3 < pier.topPolygon.length; i3 += 3, i2 += 2) {
                gl.glNormal3fv(sideNormals, i3 - 3);
                gl.glTexCoord2fv(topTexCoords, i2 - 2);
                gl.glVertex3fv(topPolygon, i3 - 3);
                gl.glTexCoord2fv(bottomTexCoords, i2 - 2);
                gl.glVertex3fv(bottomPolygon, i3 - 3);
                gl.glTexCoord2fv(bottomTexCoords, i2);
                gl.glVertex3fv(bottomPolygon, i3);
                gl.glTexCoord2fv(topTexCoords, i2);
                gl.glVertex3fv(topPolygon, i3);
            }
            gl.glEnd();
            gl.glBegin(GL2.GL_POLYGON);
            gl.glNormal3f(0f, 1f, 0f);
            i2 = 0;
            for (int i = 0; i2 < capTexCoords.length; i += 3, i2 += 2) {
                gl.glTexCoord2fv(capTexCoords, i2);
                gl.glVertex3fv(topPolygon, i);
            }
            gl.glEnd();
        }
    }
    
    private final Prism pier = new Prism();
    private final Prism base = new Prism();

    private void initializePillow(float halfDepth) {
        pillowFrontFace[0] = -pierHalfWidth; pillowFrontFace[1] = -pillowHeight; pillowFrontFace[2] = halfDepth;
        pillowFrontFace[3] = 0f;             pillowFrontFace[4] = 0f;            pillowFrontFace[5] = halfDepth;
        pillowFrontFace[6] = +pierHalfWidth; pillowFrontFace[7] = -pillowHeight; pillowFrontFace[8] = halfDepth;
        pillowRearFace[0] = -pierHalfWidth;  pillowRearFace[1]  = -pillowHeight; pillowRearFace[2] = -halfDepth;
        pillowRearFace[3] = 0f;              pillowRearFace[4]  = 0f;            pillowRearFace[5] = -halfDepth;
        pillowRearFace[6] = +pierHalfWidth;  pillowRearFace[7]  = -pillowHeight; pillowRearFace[8] = -halfDepth;
    }

    /**
     * Initialize the pier with given geometric parameters.
     * 
     * @param height pier height
     * @param halfDepth pier half-depth (parallel to river)
     * @param texSize scale factor for texture
     */
    public void initialize(float height, float halfDepth, float texSize) {
        this.height = height;
        initializePillow(halfDepth);
        pier.initialize(pierHalfWidth, height - pillowHeight, halfDepth, pierCusp, pierTaper, texSize);
        base.initialize(
                pierHalfWidth * pierTaper + pierBaseShoulder, 
                2f, 
                halfDepth * pierTaper + pierBaseShoulder * .5f,
                pierCusp * pierTaper + pierBaseShoulder * .5f,
                1f, 
                texSize);
    }

    /**
     * Initialize the pier with given geometric parameters, skipping
     * all the texture provisions. Assume we're painting with Graphics2D.
     *
     * @param height pier height
     * @param halfDepth pier half-depth (parallel to river)
     */
    public void initialize(float height, float halfDepth) {
        this.height = height;
        pier.initialize(pierHalfWidth, height - pillowHeight, halfDepth, pierCusp, pierTaper);
        base.initialize(
                pierHalfWidth * pierTaper + pierBaseShoulder,
                2f,
                halfDepth * pierTaper + pierBaseShoulder * .5f,
                pierCusp * pierTaper + pierBaseShoulder * .5f,
                1f);
    }

    public void paint(Graphics2D g, ViewportTransform viewportTransform, float dx, float dy, float dz) {
        base.draw(g, viewportTransform, dx, dy - height, dz, Renderer3d.RULED_QUAD_STRIP);
        pier.draw(g, viewportTransform, dx, dy - pillowHeight, dz, Renderer3d.QUAD_STRIP);
    }

    public void patch(Graphics2D g, ViewportTransform viewportTransform, float dx, float dy, float dz) {
        base.patch(g, viewportTransform, dx, dy - height, dz);
    }

    /**
     * Display the pier.
     * 
     * @param gl OpenGL graphics context
     */
    public void display(GL2 gl, Texture texture) {
        gl.glColor3fv(pierMaterial, 0);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        texture.enable(gl);
        texture.bind(gl);
        gl.glPushMatrix();
        gl.glTranslatef(0f, -pillowHeight, 0f);
        pier.display(gl);
        gl.glPopMatrix();
        gl.glPushMatrix();
        gl.glTranslatef(0f, -height, 0f);
        base.display(gl);
        gl.glPopMatrix();
        texture.disable(gl);
        
        // Pillow
        gl.glColor3fv(pillowMaterial, 0);
        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glNormal3f(0f, 0f, 1f);
        for (int i = pillowFrontFace.length - 3; i >= 0; i -= 3) {
            gl.glVertex3fv(pillowFrontFace, i);
        }
        gl.glNormal3f(0f, 0f, -1f);
        for (int i = 0; i < pillowRearFace.length; i += 3) {
            gl.glVertex3fv(pillowRearFace, i);
        }
        gl.glEnd();
        gl.glBegin(GL2.GL_QUADS);
        int j = 0;
        for (int i = 3; i < pillowFrontFace.length; j = i, i += 3) {
            final float dx = pillowFrontFace[i + 0] - pillowFrontFace[j + 0];
            final float dy = pillowFrontFace[i + 1] - pillowFrontFace[j + 1];
            gl.glNormal3f(-dy, dx, 0f);
            gl.glVertex3fv(pillowRearFace, j);
            gl.glVertex3fv(pillowFrontFace, j);
            gl.glVertex3fv(pillowFrontFace, i);
            gl.glVertex3fv(pillowRearFace, i);
        }
        gl.glEnd();        
    }    
}
