/*
 * ShadowMapExample.java  
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

package wpbd;

import com.sun.opengl.util.FPSAnimator;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.FloatBuffer;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCanvas;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.glu.GLU;
import javax.swing.JFrame;

/**
 *
 * @author Eugene K. Ressler
 */
public class ShadowMapExample implements GLEventListener, KeyListener {

    private GLU glu;
    private GLUT glut;
    private final GLCanvas canvas;
    private final JFrame frame;
    private final FPSAnimator animator;
    private final Homogeneous.Point white = new Homogeneous.Point(1f, 1f, 1f);
    private final Homogeneous.Point black = new Homogeneous.Point(0f, 0f, 0f);
    private final Homogeneous.Point cameraPosition = new Homogeneous.Point(-2.5f, 3.5f,-2.5f);
    private final Homogeneous.Point lightPosition = new Homogeneous.Point(2.0f, 5.0f,-2.0f);
    private final Homogeneous.Matrix cameraProjectionMatrix = new Homogeneous.Matrix();
    private final Homogeneous.Matrix lightProjectionMatrix = new Homogeneous.Matrix();
    private final Homogeneous.Matrix lightViewMatrix = new Homogeneous.Matrix();
    private final Homogeneous.Matrix textureMatrix = new Homogeneous.Matrix();
    
    // For calculating texture matrix for projection, this matrix 
    // takes us from eye space to the light's clip space. It is 
    // postmultiplied by the inverse of the current view matrix when 
    // specifying texgen bias from [-1, 1] to [0, 1].
    private static final Homogeneous.Matrix biasMatrix = new Homogeneous.Matrix(
	0.5f, 0.0f, 0.0f, 0.5f,
        0.0f, 0.5f, 0.0f, 0.5f, 
        0.0f, 0.0f, 0.5f, 0.5f,
        0.0f, 0.0f, 0.0f, 1.0f);

    private long lastTime = -1;
    private Texture shadowMapTexture;
    private int shadowMapWidth;
    private int shadowMapHeight;
    private FloatBuffer depthBuffer;
    private Texture waterTexture;
    private int windowX, windowY, windowWidth, windowHeight;
    
    private final FPSTracker fpsTracker = new FPSTracker();
    private boolean showShadowBuffer = false;
    private int lightType = POINT_LIGHT | REQUEST_LIGHT_BIT;
    private static final int PARALLEL_LIGHT = 0;
    private static final int POINT_LIGHT = 1;
    private static final int LIGHT_MASK = 1;
    private static final int REQUEST_LIGHT_BIT = 2;
    private static int maxShadowTextureSize = 2 * 1024;
    private int shadowFrameBuffer = -1;
    
    public ShadowMapExample() {
        canvas = new GLCanvas();
        canvas.setSize(1024, 800);
        canvas.setIgnoreRepaint(false);
        frame = new JFrame("Shadow Map Example");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        animator = new FPSAnimator(canvas, 120);
        animator.setRunAsFastAsPossible(false);
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
    }
    
    public void start() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize( frame.getContentPane().getPreferredSize() );
        frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
        frame.setVisible(true);        
        canvas.requestFocus();
        animator.start();
    }
    
    public void init(GLAutoDrawable drawable) {
        
        glu = new GLU();
        glut = new GLUT();
        
        GL2 gl = drawable.getGL();

        if (getIntegerv(gl, GL2.GL_MAX_TEXTURE_UNITS) < 2) {
            System.out.println("Need at least 2 texture units to run this app.");
            System.exit(1);
        }
        
        // Load identity modelview
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Shading states
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(0.0f, 0.7f, 0.7f, 1.0f);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        // Depth states
        gl.glClearDepth(1.0f);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        gl.glEnable(GL2.GL_CULL_FACE);

        // We use glScale when drawing the scene
        gl.glEnable(GL2.GL_NORMALIZE);

        // Create and map the water texture in unit 0.
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        waterTexture = WPBDApp.getApplication().getTextureResource("water.jpg", true, TextureIO.JPG);
        waterTexture.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_NEAREST);
        waterTexture.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        waterTexture.setTexParameteri(GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        waterTexture.setTexParameteri(GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        
        // Use the color as the ambient and diffuse material
        gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);

        // White specular material color, quite shiny. 
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, white.a, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 16.0f);

        // Calculate & save matrices
        gl.glPushMatrix();

        // Camera projection
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float)windowWidth/windowHeight, 1.0f, 100.0f);
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, cameraProjectionMatrix.a, 0);

        // Light view view
        gl.glLoadIdentity();
        glu.gluLookAt(lightPosition.x(), lightPosition.y(), lightPosition.z(),
                        0.0f, 0.0f, 0.0f,
                        0.0f, 1.0f, 0.0f);
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, lightViewMatrix.a, 0);

        gl.glPopMatrix();
        
        // Set up a frame buffer for the shadow texture if the capability exists.
        if (gl.isExtensionAvailable("GL_EXT_framebuffer_object") && gl.isFunctionAvailable("glFramebufferTexture2DEXT")) {
            maxShadowTextureSize = Math.min(maxShadowTextureSize, getIntegerv(gl, GL2.GL_MAX_TEXTURE_BUFFER_SIZE_EXT));
            allocateShadowMapTexture(gl, maxShadowTextureSize, maxShadowTextureSize);
            shadowFrameBuffer = genFramebuffer(gl);
            gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, shadowFrameBuffer);
            gl.glFramebufferTexture2DEXT(GL2.GL_FRAMEBUFFER_EXT, GL2.GL_DEPTH_ATTACHMENT_EXT, GL2.GL_TEXTURE_2D, shadowMapTexture.getTextureObject(), 0);
            // Turn off drawing planes.  We need only depth.
            gl.glDrawBuffer(GL2.GL_NONE);
            gl.glReadBuffer(GL2.GL_NONE);
            if (gl.glCheckFramebufferStatusEXT(GL2.GL_FRAMEBUFFER_EXT) != GL2.GL_FRAMEBUFFER_COMPLETE_EXT) {
                shadowFrameBuffer = -1;
            }
            gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, 0);
        }
    }

    public void drawScene(GL2 gl, float angle) {
        
	//Draw base with it's texture attached.
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        waterTexture.bind();
        waterTexture.enable();
        gl.glPushMatrix();
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawPrism(gl, 1.5f, .2f, 1.8f);
        gl.glPopMatrix();
        waterTexture.disable();

        // Torus
	gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.8f, 0.0f);
	gl.glRotatef(angle * 1.2f, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        glut.glutSolidTorus(0.2, 0.5, 24, 48);
	gl.glPopMatrix();
        
        // Spheres
	gl.glPushMatrix();
	gl.glRotatef(angle, 0.0f, 1.0f, 0.0f);
        gl.glTranslatef(0.45f, 1.5f, 0.45f);
        gl.glColor3f(1.0f, 1.0f, 0.0f);
        glut.glutSolidSphere(0.2, 24, 24);
        gl.glTranslatef(-0.9f, 0.0f, 0.0f);
        gl.glColor3f(0.0f, 1.0f, 0.0f);
        glut.glutSolidSphere(0.2, 24, 24);
        gl.glTranslatef(0.0f, 0.0f,-0.9f);
        gl.glColor3f(0.0f, 0.0f, 1.0f);
        glut.glutSolidSphere(0.2, 24, 24);
        gl.glTranslatef(0.9f, 0.0f, 0.0f);
        gl.glColor3f(0.0f, 1.0f, 1.0f);
        glut.glutSolidSphere(0.2, 24, 24);
	gl.glPopMatrix();
    }
    
    /**
     * Draw a prism including texture coordinates.
     * 
     * @param gl GL2 object
     * @param dx x-dimension of prism
     * @param dy y-dimension of prism
     * @param dz z-dimension of prism
     */
    public void drawPrism(GL2 gl, float dx, float dy, float dz) {
        final float yx = dy / dx;
        final float yz = dy / dz;
        final float zx = dz / dx;
        
        gl.glBegin(GL2.GL_QUADS);
        // Front
        gl.glNormal3f( 0, 0, 1);
        gl.glTexCoord2f(0, 0);
        gl.glVertex3f(-dx, -dy, dz);
        gl.glTexCoord2f(1, 0);
        gl.glVertex3f( dx, -dy, dz);
        gl.glTexCoord2f(1, yx);
        gl.glVertex3f( dx,  dy, dz);
        gl.glTexCoord2f(0, yx);
        gl.glVertex3f(-dx,  dy, dz);
        
        // Back
        gl.glNormal3f( 0, 0,-1);
        gl.glTexCoord2f(0, 0);
        gl.glVertex3f(-dx,-dy,-dz);
        gl.glTexCoord2f(0, yx);
        gl.glVertex3f(-dx, dy,-dz);
        gl.glTexCoord2f(1, yx);
        gl.glVertex3f( dx, dy,-dz);
        gl.glTexCoord2f(1, 0);
        gl.glVertex3f( dx,-dy,-dz);
                
        // Top
        gl.glNormal3f( 0, 1, 0);
        gl.glTexCoord2f(0, zx);
        gl.glVertex3f(-dx, dy, dz);
        gl.glTexCoord2f(1, zx);
        gl.glVertex3f( dx, dy, dz);
        gl.glTexCoord2f(1, 0);
        gl.glVertex3f( dx, dy,-dz);
        gl.glTexCoord2f(0, 0);
        gl.glVertex3f(-dx, dy,-dz);
        
        // Bottom
        gl.glNormal3f( 0,-1, 0);
        gl.glTexCoord2f(0, zx);
        gl.glVertex3f(-dx,-dy, dz);
        gl.glTexCoord2f(0, 0);
        gl.glVertex3f(-dx,-dy,-dz);
        gl.glTexCoord2f(1, 0);
        gl.glVertex3f( dx,-dy,-dz);
        gl.glTexCoord2f(1, zx);
        gl.glVertex3f( dx,-dy, dz);
        
        // Right
        gl.glNormal3f( 1, 0, 0);
        gl.glTexCoord2f(1, yz);
        gl.glVertex3f( dx, dy, dz);
        gl.glTexCoord2f(1, 0);
        gl.glVertex3f( dx,-dy, dz);
        gl.glTexCoord2f(0, 0);
        gl.glVertex3f( dx,-dy,-dz);
        gl.glTexCoord2f(0, yz);
        gl.glVertex3f( dx, dy,-dz);

        // Left
        gl.glNormal3f(-1, 0, 0);
        gl.glTexCoord2f(1, yz);
        gl.glVertex3f(-dx, dy, dz);
        gl.glTexCoord2f(0, yz);
        gl.glVertex3f(-dx, dy,-dz);
        gl.glTexCoord2f(0, 0);
        gl.glVertex3f(-dx,-dy,-dz);
        gl.glTexCoord2f(1, 0);
        gl.glVertex3f(-dx,-dy, dz);
        
        gl.glEnd();
    }
    
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL();
        
        long time = System.nanoTime();
        if (lastTime < 0) {
            lastTime = time;
        }
        float elapsed = (time - lastTime) * 1e-9f;
        
	//angle of spheres in scene. Calculate from time
	float angle = elapsed * 30f;

        double cameraAngle = Math.toRadians(angle * 0.5);
	cameraPosition.setX(3.0f * (float)Math.cos(cameraAngle));
	cameraPosition.setZ(3.0f * (float)Math.sin(cameraAngle));

        // See if there is a pending request to change the light.
        if ((lightType & REQUEST_LIGHT_BIT) != 0) {
            // Reset the request bit.
            lightType ^= REQUEST_LIGHT_BIT;
            // Make the change.
            gl.glPushMatrix();
            gl.glLoadIdentity();
            if (lightType == PARALLEL_LIGHT) {
                gl.glOrtho(-2f, 2f, -2f, 2f, 4f, 7f);
                // gl.glOrtho(-1f, 1f, -1f, 1f, -10f, 20f); // Force shadow map clamping.
            }
            else {
                glu.gluPerspective(45f, 1f, 4f, 7f);
                //glu.gluPerspective(45.0f, .5f, 1.0f, 20.0f); // Force shadow map clamping.
            }
            gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, lightProjectionMatrix.a, 0);
            gl.glPopMatrix();
        }
        
	//First pass - from light's point of view
	gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
	
	gl.glMatrixMode(GL2.GL_PROJECTION);
	gl.glLoadMatrixf(lightProjectionMatrix.a, 0);

	gl.glMatrixMode(GL2.GL_MODELVIEW);
	gl.glLoadMatrixf(lightViewMatrix.a, 0);

	//Use viewport the same size as the shadow map
	gl.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
        
	//Draw back faces into the shadow map
	gl.glCullFace(GL2.GL_FRONT);
        
	//Disable color writes, and use flat shading for speed
	gl.glShadeModel(GL2.GL_FLAT);
	gl.glColorMask(false, false, false, false);

	//Draw the scene with details decided by availableity of FBOs.
        if (shadowFrameBuffer > 0) {
            // Draw a large depth mask straight to the texture through the FBO.
            gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, shadowFrameBuffer);
            gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
            drawScene(gl, angle);
            // Optionally show the shadow buffer contents in grayscale.
            if (showShadowBuffer) {
                // Blit the depth buffer values to the image as a luminance signal.  Can glCopyPixels do this?
                gl.glReadPixels(0, 0, shadowMapWidth, shadowMapHeight, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, depthBuffer);
                gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, 0);
                gl.glWindowPos2f(0, 0);
                gl.glColorMask(true, true, true, true);
                gl.glPixelZoom(.25f, .25f);
                gl.glDrawPixels(shadowMapWidth, shadowMapHeight, GL2.GL_LUMINANCE, GL2.GL_FLOAT, depthBuffer);
                gl.glPixelZoom(1f, 1f);
                return;
            }
            gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, 0);
        }
        else {
            drawScene(gl, angle);
            // Optionally show the shadow buffer contants in grayscale.
            if (showShadowBuffer) {
                // Blit the depth buffer values to the image as a luminance signal.  Can glCopyPixels do this?
                gl.glReadPixels(0, 0, shadowMapWidth, shadowMapHeight, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, depthBuffer);
                gl.glWindowPos2f(0, 0);
                gl.glColorMask(true, true, true, true);
                gl.glDrawPixels(shadowMapWidth, shadowMapHeight, GL2.GL_LUMINANCE, GL2.GL_FLOAT, depthBuffer);
                return;
            }
            //Read the depth buffer into the shadow map texture
            gl.glActiveTexture(GL2.GL_TEXTURE1);
            shadowMapTexture.bind();
            gl.glCopyTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, 0, 0, shadowMapWidth, shadowMapHeight);        
        }

	//restore states
	gl.glCullFace(GL2.GL_BACK);
	gl.glShadeModel(GL2.GL_SMOOTH);
	gl.glColorMask(true, true, true, true);
	
	//2nd pass - Draw from camera's point of view
	gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);

	gl.glMatrixMode(GL2.GL_PROJECTION);
	gl.glLoadMatrixf(cameraProjectionMatrix.a, 0);
	
	gl.glMatrixMode(GL2.GL_MODELVIEW);
	gl.glLoadIdentity();
	glu.gluLookAt(  cameraPosition.x(), cameraPosition.y(), cameraPosition.z(),
			0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f);
        
	gl.glViewport(windowX, windowY, windowWidth, windowHeight);

	//Use dim light to represent shadowed areas
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition.a, 0);
        Homogeneous.Point lowWhite = white.scale(0.2f);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lowWhite.a, 0);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lowWhite.a, 0);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, black.a, 0);
	gl.glEnable(GL2.GL_LIGHT0);
	gl.glEnable(GL2.GL_LIGHTING);

	drawScene(gl, angle);
	
	//3rd pass
	//Draw with bright light
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, white.a, 0);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, white.a, 0);

        // Build the texture matrix.
        gl.glPushMatrix();
        gl.glLoadMatrixf(biasMatrix.a, 0);
        gl.glMultMatrixf(lightProjectionMatrix.a, 0);
        gl.glMultMatrixf(lightViewMatrix.a, 0);
	gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, textureMatrix.a, 0);
        textureMatrix.transposeInPlace();
        gl.glPopMatrix();
        
	//Set up texture coordinate generation in texture unit 1.
        gl.glActiveTexture(GL2.GL_TEXTURE1);

	gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_EYE_LINEAR);
	gl.glTexGenfv(GL2.GL_S, GL2.GL_EYE_PLANE, textureMatrix.a, 0);
	gl.glEnable(GL2.GL_TEXTURE_GEN_S);

	gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_EYE_LINEAR);
	gl.glTexGenfv(GL2.GL_T, GL2.GL_EYE_PLANE, textureMatrix.a, 4);
	gl.glEnable(GL2.GL_TEXTURE_GEN_T);

	gl.glTexGeni(GL2.GL_R, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_EYE_LINEAR);
	gl.glTexGenfv(GL2.GL_R, GL2.GL_EYE_PLANE, textureMatrix.a, 8);
	gl.glEnable(GL2.GL_TEXTURE_GEN_R);

	gl.glTexGeni(GL2.GL_Q, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_EYE_LINEAR);
	gl.glTexGenfv(GL2.GL_Q, GL2.GL_EYE_PLANE, textureMatrix.a, 12);
	gl.glEnable(GL2.GL_TEXTURE_GEN_Q);

	//Bind & enable shadow map texture
        shadowMapTexture.bind();
        shadowMapTexture.enable();

	//Enable shadow comparison
	shadowMapTexture.setTexParameteri(GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);

	//Shadow comparison should be true (ie not in shadow) if r<=texture
	shadowMapTexture.setTexParameteri(GL2.GL_TEXTURE_COMPARE_FUNC, GL2.GL_LEQUAL);

	//Shadow comparison should generate an INTENSITY result
	shadowMapTexture.setTexParameteri(GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_INTENSITY);

	//Set alpha test to discard intensities less than 1 (the false comparisons)
	gl.glAlphaFunc(GL2.GL_GEQUAL, 1f);
	gl.glEnable(GL2.GL_ALPHA_TEST);

	drawScene(gl, angle);

	//Disable depth texture and texgen
        gl.glActiveTexture(GL2.GL_TEXTURE1);
        shadowMapTexture.disable();
	gl.glDisable(GL2.GL_TEXTURE_GEN_S);
	gl.glDisable(GL2.GL_TEXTURE_GEN_T);
	gl.glDisable(GL2.GL_TEXTURE_GEN_R);
	gl.glDisable(GL2.GL_TEXTURE_GEN_Q);

	//Restore other states
	gl.glDisable(GL2.GL_LIGHTING);
	gl.glDisable(GL2.GL_ALPHA_TEST);

        //Print text
        String fps = fpsTracker.getFPSString(time);	
        gl.glWindowPos2i(3, 3);
	for (int i = 0; i < fps.length(); ++i) {
            glut.glutBitmapCharacter(GLUT.BITMAP_HELVETICA_18, fps.charAt(i));
        }
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL2 gl = drawable.getGL();

        //Save new window size
        windowX = x;
        windowY = y;
	windowWidth = w; 
        windowHeight = h;

	// Update the camera's projection matrix
	gl.glPushMatrix();
	gl.glLoadIdentity();
	glu.gluPerspective(45.0f, (float)windowWidth/windowHeight, 1.0f, 100.0f);
	gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, cameraProjectionMatrix.a, 0);
	gl.glPopMatrix();
        
        // If no frame buffer, we're using back buffer, so must ensure shadow mask is no larger than window.
        if (shadowFrameBuffer < 0) {
            allocateShadowMapTexture(gl, powerOf2AtMost(w), powerOf2AtMost(h));
        }
    }

    public static void main(String [] argv) {
        new ShadowMapExample().start();
    }

    public void keyTyped(KeyEvent e) {
        switch (e.getKeyChar()) {
            case 's':
            case 'S':
                showShadowBuffer = !showShadowBuffer;
                break;
            case 'l':
            case 'L':
                lightType = (lightType ^ LIGHT_MASK) | REQUEST_LIGHT_BIT;
                break;
            case '\u001b':
                System.exit(0);
        }
    }
    
    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        /* ignore */
    }

    private void allocateShadowMapTexture(GL2 gl, int width, int height) throws GLException {
        if (shadowMapTexture != null) {
            if (width == shadowMapWidth && height == shadowMapHeight) {
                return;
            }
            shadowMapTexture.dispose();
            shadowMapTexture = null;
        }
        //Create the shadow map texture in unit 1.
        gl.glActiveTexture(GL2.GL_TEXTURE1);
        shadowMapTexture = TextureIO.newTexture(GL2.GL_TEXTURE_2D);
        shadowMapTexture.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        shadowMapTexture.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        // Clamp to border so border color decides shadow behavior for objects outside the map.
        shadowMapTexture.setTexParameteri(GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        shadowMapTexture.setTexParameteri(GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        // Set the shadow map's border to infinite depth so fragments outside the shadow map appear unshadowed.
        // Only the initial 1 in the color is used because this is a depth texture.
        shadowMapTexture.setTexParameterfv(GL2.GL_TEXTURE_BORDER_COLOR, new float [] { 1f, 1f, 1f, 1f }, 0);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT, width, height, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_UNSIGNED_BYTE, null);
        
        // Allocate buffer to hold the detph values to be rendered as luminosity (optionally).
        depthBuffer = FloatBuffer.allocate(width * height);
        shadowMapWidth = width;
        shadowMapHeight = height;
    }
    
    private static int getIntegerv(GL2 gl, int pname) {
        int [] val = new int [1];
        gl.glGetIntegerv(pname, val, 0);
        return val[0];
    }
    
    private static int genFramebuffer(GL2 gl) {
        int [] val = new int [1];
        gl.glGenFramebuffersEXT(1, val, 0);
        return val[0];
    }

    private static int powerOf2AtMost(int x) {
        int i = 1;
        while (i <= x) {
            i <<= 1;
        }
        return i >> 1;
    }    
}
