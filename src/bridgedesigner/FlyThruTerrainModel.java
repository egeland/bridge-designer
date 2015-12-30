/*
 * FlyThruTerrainModel.java
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
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.GL2;
import static bridgedesigner.TerrainModel.halfTerrainSize;

/**
 * <p>3d terrain model for the Bridge Designer.</p>
 * <p>Uses the diamond algorithm to establish basic terrain
 * shape, then carves a river valley and inserts abutments and pier.</p>
 * 
 * @author Eugene K. Ressler
 */
public class FlyThruTerrainModel extends TerrainModel {
        
    private final FlyThruTowerModel towerModel = new FlyThruTowerModel();
    private final PierModel pierModel = new PierModel();
    
    private final FlyThruAnimation.Config config;
    
    private Texture pierTexture = null;
    private Texture waterTexture = null;
    private String skyTextureNames [] = { "skyup.jpg", "skye.jpg", "skyn.jpg", "skyw.jpg", "skys.jpg" };
    private Texture skyTextures [] = new Texture [5];
    private static final float skyQuadsInTerrainCoords [] = {
        // up
         halfTerrainSize, halfTerrainSize, halfTerrainSize,
        -halfTerrainSize, halfTerrainSize, halfTerrainSize,
        -halfTerrainSize, halfTerrainSize,-halfTerrainSize,
         halfTerrainSize, halfTerrainSize,-halfTerrainSize,
        // east
         halfTerrainSize, halfTerrainSize, halfTerrainSize,
         halfTerrainSize, halfTerrainSize,-halfTerrainSize,
         halfTerrainSize,-halfTerrainSize,-halfTerrainSize,
         halfTerrainSize,-halfTerrainSize, halfTerrainSize,
        // north
         halfTerrainSize, halfTerrainSize,-halfTerrainSize,
        -halfTerrainSize, halfTerrainSize,-halfTerrainSize,
        -halfTerrainSize,-halfTerrainSize,-halfTerrainSize,
         halfTerrainSize,-halfTerrainSize,-halfTerrainSize,
        // west
        -halfTerrainSize, halfTerrainSize,-halfTerrainSize,
        -halfTerrainSize, halfTerrainSize, halfTerrainSize,
        -halfTerrainSize,-halfTerrainSize, halfTerrainSize,
        -halfTerrainSize,-halfTerrainSize,-halfTerrainSize,
        // south
        -halfTerrainSize, halfTerrainSize, halfTerrainSize,         
         halfTerrainSize, halfTerrainSize, halfTerrainSize,
         halfTerrainSize,-halfTerrainSize, halfTerrainSize,
        -halfTerrainSize,-halfTerrainSize, halfTerrainSize,
    };
    // True sky quads are offset from terrain coords by half span in x during initialization.
    private final float [] skyQuads = new float[skyQuadsInTerrainCoords.length];
    
    private float skyQuadTexCoords [] = {
        /*
        // up
        1, 0,
        0, 0,
        0, 1,
        1, 1,
        // east
        1, 0,
        0, 0,
        0, 1,
        1, 1,
        // north
        1, 0,
        0, 0,
        0, 1,
        1, 1,
        // west
        1, 0,
        0, 0,
        0, 1,
        1, 1,
        // south
        1, 0,
        0, 0,
        0, 1,
        1, 1,
        */
        // JPEG reader flips vertical coordinate. This compensates.
        // up
        1, 1,
        0, 1,
        0, 0,
        1, 0,
        // east
        1, 1,
        0, 1,
        0, 0,
        1, 0,
        // north
        1, 1,
        0, 1,
        0, 0,
        1, 0,
        // west
        1, 1,
        0, 1,
        0, 0,
        1, 0,
        // south
        1, 1,
        0, 1,
        0, 0,
        1, 0,        
    };

    /**
     * Construct a new terrain model using the given animation configuration, which specifies
     * whether certain terrain features should be drawn or not.
     * 
     * @param config animation configuration
     */
    public FlyThruTerrainModel(FlyThruAnimation.Config config) {
        // super(64);  // use for looking around fixed eye version of model
        this.config = config;
    }
    
    /**
     * Initialize parts of the model that require an OpenGL context.
     * 
     * @param gl OpenGL graphics context
     */
    public void initializeTerrainTextures(GL2 gl) {
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        pierTexture = BDApp.getApplication().getTextureResource("bricktile.png", true, TextureIO.PNG);
        pierTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        pierTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        pierTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        pierTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_NEAREST);

        waterTexture = BDApp.getApplication().getTextureResource("water.jpg", true, TextureIO.JPG);
        waterTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        waterTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        waterTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        waterTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_NEAREST);
        
        for (int i = 0; i < 5; i++) {
            skyTextures[i] = BDApp.getApplication().getTextureResource(skyTextureNames[i], false, TextureIO.JPG);
            skyTextures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
            skyTextures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
            skyTextures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            skyTextures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        }
    }

    /**
     * Initialize the terrain model with given parameters.
     * 
     * @param conditions design conditions for the bridge site
     * @param trussCenterOffset truss center offset used to position anchorages with respect to roadway axis
     * @param abutmentHalfWidth half width of abutment
     */
    @Override
    public void initializeTerrain(DesignConditions conditions, float trussCenterOffset, float abutmentHalfWidth) {

        // First initialize the common parts of the model.
        super.initializeTerrain(conditions, trussCenterOffset, abutmentHalfWidth);

        // Now the parts specific to the fly through.
        if (conditions.isPier()) {
            pierModel.initialize((float)conditions.getPierHeight(), abutmentHalfWidth, stoneTextureSize);
        }

        // Fill in true sky quads from the template that's in terrain coords.
        for (int i = 0; i < skyQuads.length; i += 3) {
            skyQuads[i+0] = skyQuadsInTerrainCoords[i+0] + halfSpanLength;
            skyQuads[i+1] = skyQuadsInTerrainCoords[i+1];
            skyQuads[i+2] = skyQuadsInTerrainCoords[i+2];
        }
    }
  
    /**
     * Paint an abutment in a model coordinate system where the supported deck joint is at (0,0).
     * We do this here rather than in FlyThruAnimation because we need the road centerline elevations.
     * 
     * @param gl OpenGL object
     */
    private void paintAbutment(GL2 gl) {
        gl.glColor3fv(abutmentMaterial, 0);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        pierTexture.enable(gl);
        pierTexture.bind(gl);
        
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0f, 0f, 1f);
        int i2 = 0;
        for (int i3 = 0; i3 < abutmentFrontFlank.length; i3 += 3, i2 += 2) {
            gl.glTexCoord2fv(abutmentFrontFlankTexture, i2);
            gl.glVertex3fv(abutmentFrontFlank, i3);
        }
        gl.glEnd();

        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0f, 0f, -1f);
        i2 = 0;
        for (int i3 = 0; i3 < abutmentRearFlank.length; i3 += 3, i2 += 2) {
            gl.glTexCoord2fv(abutmentRearFlankTexture, i2);
            gl.glVertex3fv(abutmentRearFlank, i3);            
        }
        gl.glEnd();
        
        gl.glBegin(GL2.GL_QUADS);
        i2 = 0;
        for (int i3 = 0; i3 < abutmentFaceNormals.length; i3 += 3, i2 += 2) {
            gl.glNormal3fv(abutmentFaceNormals, i3);
            
            gl.glTexCoord2fv(abutmentRearFaceTexture, i2);
            gl.glVertex3fv(abutmentRearFace, i3);
            
            gl.glTexCoord2fv(abutmentFrontFaceTexture, i2);
            gl.glVertex3fv(abutmentFrontFace, i3);
            
            gl.glTexCoord2fv(abutmentFrontFaceTexture, i2 + 2);
            gl.glVertex3fv(abutmentFrontFace, i3 + 3);
            
            gl.glTexCoord2fv(abutmentRearFaceTexture, i2 + 2);
            gl.glVertex3fv(abutmentRearFace, i3 + 3);
        }
        gl.glEnd();
        pierTexture.disable(gl);
        
        // Top shoulder.
        gl.glColor3fv(flatTerrainMaterial, 0);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        gl.glNormal3f(0f, 1f, 0f);
        for (int i = 0; i < abutmentFrontTop.length; i += 3) {
            gl.glVertex3fv(abutmentRearTop, i);
            gl.glVertex3fv(abutmentFrontTop, i);
        }
        gl.glEnd();
        
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

    /**
     * Completely re-implemented 28 November 2011 to match super class
     * re-implementation for legacy graphics.
     * 
     * @param gl OpenGL context
     */
    private void drawPowerLines(GL2 gl) {
        // Position the towers.
        for (int iTower = 0; iTower < towerCount; ++iTower) {
            gl.glPushMatrix();
            Homogeneous.Point p = towerPt[iTower];
            gl.glTranslatef(p.x(), p.y(), p.z());
            gl.glRotatef(thetaTower, 0.0f, 1.0f, 0.0f);
            towerModel.display(gl);
            gl.glPopMatrix();
        }
        // Won't draw wires into shadow buffer as their shadows
        // look too coarse most of the time.
        if (!drawingShadows) {
            gl.glColor3fv(wireColor, 0);
            for (int iOffset = 0; iOffset < wireOffsets.length; ++iOffset) {
                float xOfs = xUnitPerpTower * wireOffsets[iOffset].x();
                float yOfs = wireOffsets[iOffset].y();
                float zOfs = zUnitPerpTower * wireOffsets[iOffset].x();
                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int iTower = 0; iTower < wirePt.length; ++iTower) {
                    for (int iWire = (iTower == 0) ? 0 : 1; iWire < wirePt[0].length; ++iWire) {
                        Homogeneous.Point p = wirePt[iTower][iWire];
                        gl.glVertex3f(p.x() + xOfs, p.y() + yOfs, p.z() + zOfs);
                    }
                }
                gl.glEnd();
            }
        }
    }

    private void drawStaticTerrain(GL2 gl) {
        if (config.showTerrain) {
            // FlyThruTerrainModel surface as triangle strips.
            float zNorth = zGridToWorld(0);
            float zSouth = zNorth + metersPerGrid;
            gl.glShadeModel(GL2.GL_SMOOTH);
            gl.glColor3fv(flatTerrainMaterial, 0);
            for (int iNorth = 0; iNorth < gridCount; iNorth++) {
                int iSouth = iNorth + 1;
                gl.glBegin(GL2.GL_TRIANGLE_STRIP);
                float x = xGridToWorld(0);
                for (int j = 0; j < postCount; j++) {
                    TerrainPost n = posts[iNorth][j];
                    TerrainPost s = posts[iSouth][j];
                    // Some NVIDIA drivers barf on material changes inside 
                    // glBegin() .. glEnd() even thought OpenGL says it's fine.
                    if (config.showErrosion) {
                        gl.glColor3fv(n.yNormal > yNormalMaterialThreshhold ? flatTerrainMaterial : verticalTerrainMaterial, 0);
                    }
                    gl.glNormal3f(n.xNormal, n.yNormal, n.zNormal);
                    gl.glVertex3f(x, n.elevation, zNorth);
                    if (config.showErrosion) {
                        gl.glColor3fv(s.yNormal > yNormalMaterialThreshhold ? flatTerrainMaterial : verticalTerrainMaterial, 0);
                    }
                    gl.glNormal3f(s.xNormal, s.yNormal, s.zNormal);
                    gl.glVertex3f(x, s.elevation, zSouth);
                    x += metersPerGrid;
                }
                gl.glEnd();
                zNorth = zSouth;
                zSouth += metersPerGrid;
            }

            // Roadway western approach.
            gl.glColor3fv(roadMaterial, 0);
            gl.glBegin(GL2.GL_QUAD_STRIP);
            float x = xGridToWorld(0);
            for (int j = 0; j < postCount; j++) {
                gl.glNormal3f(roadCenterline[j].xNormal, roadCenterline[j].yNormal, 0f);
                if (x >= abutmentStepInset) {
                    gl.glVertex3f(abutmentStepInset, wearSurfaceHeight, -deckHalfWidth);
                    gl.glVertex3f(abutmentStepInset, wearSurfaceHeight, deckHalfWidth);         
                    break;
                }
                else {
                    gl.glVertex3f(x, roadCenterline[j].elevation, -deckHalfWidth);
                    gl.glVertex3f(x, roadCenterline[j].elevation, deckHalfWidth);
                }
                x += metersPerGrid;
            }
            gl.glEnd();

            // Roadway eastern approach.
            gl.glColor3fv(roadMaterial, 0);
            gl.glBegin(GL2.GL_QUAD_STRIP);
            x = xGridToWorld(gridCount);
            float xDeckEnd = 2f * halfSpanLength - abutmentStepInset;
            for (int j = gridCount; j >= 0; j--) {
                gl.glNormal3f(roadCenterline[j].xNormal, roadCenterline[j].yNormal, 0f);
                if (x <= xDeckEnd) {
                    gl.glVertex3f(xDeckEnd, wearSurfaceHeight, deckHalfWidth);
                    gl.glVertex3f(xDeckEnd, wearSurfaceHeight, -deckHalfWidth);         
                    break;
                }
                else {
                    gl.glVertex3f(x, roadCenterline[j].elevation, deckHalfWidth);
                    gl.glVertex3f(x, roadCenterline[j].elevation, -deckHalfWidth);
                }
                x -= metersPerGrid;
            }
            gl.glEnd();
            
            drawPowerLines(gl);
        }
        
        if (config.showAbutments) {
            // Western abutment.
            paintAbutment(gl);

            // Eastern abutment.
            gl.glPushMatrix();
            gl.glTranslatef(2 * halfSpanLength, 0f, 0f);
            gl.glRotatef(180f, 0f, 1f, 0f);
            paintAbutment(gl);
            gl.glPopMatrix();

            // Pier if present
            if (pierLocation != null) {
                gl.glPushMatrix();
                gl.glTranslated(pierLocation.x, pierLocation.y - Joint.radiusWorld, 0.0);
                pierModel.display(gl, pierTexture);
                gl.glPopMatrix();
            }
        }
    }
    
    private void drawEnvironment(GL2 gl) {
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        
        if (config.showTerrain) {
            long t = System.nanoTime();
            float elapsed = (tLast >= 0) ? (t - tLast) * 1e-9f : 0f;
            tLast = t;  
            waterTexture.enable(gl);
            waterTexture.bind(gl);
            gl.glColor3fv(white, 0);
            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0f, 1f, 0f);
            float x0 = xGridToWorld(0);
            float x1 = xGridToWorld(postCount - 1);
            float z0 = zGridToWorld(0);
            float z1 = zGridToWorld(postCount - 1);
            float tf = 0.2f;
            dWater += elapsed * 0.2f;
            while (dWater > 1f) {
                dWater -= 1f;
            }
            float x0t = tf * x0 - dWater;
            float x1t = tf * x1 - dWater;
            float z0t = tf * z0 - dWater;
            float z1t = tf * z1 - dWater;
            gl.glTexCoord2f(x0t, z0t);
            gl.glVertex3f(x0, yWater, z0);
            gl.glTexCoord2f(x0t, z1t);
            gl.glVertex3f(x0, yWater, z1);
            gl.glTexCoord2f(x1t, z1t);
            gl.glVertex3f(x1, yWater, z1);
            gl.glTexCoord2f(x1t, z0t);
            gl.glVertex3f(x1, yWater, z0);
            gl.glEnd();
            waterTexture.disable(gl);
        }

        // Draw the sky box.
        if (config.showSky && !drawingShadows) {
            int iTex = 0;
            int iQuad = 0;
            gl.glColor3fv(white, 0);
            //gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);        
            for (int i = 0; i < skyTextureNames.length; i++) {
                skyTextures[i].enable(gl);
                // Paint the skybox with no worry about lighting or shading or drawingShadows.
                skyTextures[i].bind(gl);
                gl.glBegin(GL2.GL_QUADS);
                for (int j = 0; j < 4; j++, iTex += 2, iQuad += 3) {
                    gl.glTexCoord2fv(skyQuadTexCoords, iTex);
                    gl.glVertex3fv(skyQuads, iQuad);
                }
                gl.glEnd();
                skyTextures[i].disable(gl);
            }
            //gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
        }
    }
    
    /**
     * Paint the 3d terrain model.
     * @param gl OpenGL graphics context
     * @param shadows whether this rendering is into the shadow buffer
     */
    public void paint(GL2 gl, boolean shadows) {
        this.drawingShadows = shadows;
        // Try disabling this for efficiency.  All our normals are already unit vectors.
        gl.glDisable(GL2.GL_NORMALIZE);
        drawStaticTerrain(gl);
        drawEnvironment(gl);
        gl.glEnable(GL2.GL_NORMALIZE);
    }
}
