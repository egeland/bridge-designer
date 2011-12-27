/*
 * SketchupExportViewer.java  
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
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

/**
 *
 * @author Eugene K. Ressler
 */
public class SketchupExportViewer implements GLEventListener, KeyListener {

    private GLU glu;
    private GLUT glut;
    private final GLCanvas canvas;
    private final JFrame frame;
    private final FPSAnimator animator;
    private final Homogeneous.Point white = new Homogeneous.Point(1f, 1f, 1f);
    private final Homogeneous.Point offWhite = new Homogeneous.Point(0f,0f,0f);
    private final Homogeneous.Point cameraPosition = new Homogeneous.Point(-2.5f, 2.5f,-2.5f);
    private final Homogeneous.Point lightPosition = new Homogeneous.Point(1000f, 10f, 1000f, 1f);
    private final Homogeneous.Matrix cameraProjectionMatrix = new Homogeneous.Matrix();
    private final Homogeneous.Matrix mouseProjectionMatrix = new Homogeneous.Matrix();
    private BitmapOverlay overlay;
    
    private PierModel model = new PierModel();
    
    private long lastTime = -1;
    private int windowX, windowY, windowWidth, windowHeight;
    
    private final FPSTracker fpsTracker = new FPSTracker();

    public Texture LoadTexture(String name) {
        Texture texture = WPBDApp.getApplication().getTextureResource(name + ".jpg", true, TextureIO.JPG);
        texture.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_NEAREST);
        texture.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        texture.setTexParameteri(GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        texture.setTexParameteri(GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        return texture;
    }
        
    public SketchupExportViewer() {
        canvas = new GLCanvas();
        canvas.setSize(1024, 800);
        canvas.setIgnoreRepaint(false);
        frame = new JFrame("Sketchup Object Example");
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
    
    Texture pierTexture;
    
    public void init(GLAutoDrawable drawable) {
        
        glu = new GLU();
        glut = new GLUT();
        
        GL2 gl = drawable.getGL();

        pierTexture = WPBDApp.getApplication().getTextureResource("bricktile.png", true, TextureIO.PNG);
        pierTexture.setTexParameteri(GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        pierTexture.setTexParameteri(GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        pierTexture.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        pierTexture.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_NEAREST);
        model.initialize(10f, 3f, .2f);

        overlay = new BitmapOverlay("splash.png");

        // Load identity modelview
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Shading states
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(0.0f, 0.7f, 0.7f, 1.0f);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        // Turn on the light
	gl.glEnable(GL2.GL_LIGHTING);
	gl.glEnable(GL2.GL_LIGHT0);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, offWhite.a, 0);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, white.a, 0);
	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, white.a, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition.a, 0);

        // Depth states
        gl.glClearDepth(1.0f);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        gl.glEnable(GL2.GL_CULL_FACE);

        // We'll use not use glScale when drawing.i
        gl.glDisable(GL2.GL_NORMALIZE);

        // Calculate & save matrices

        // Camera projection
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float)windowWidth/windowHeight, 1.0f, 100.0f);
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, cameraProjectionMatrix.a, 0);
    }

    private float alpha = 1.0f;
    private float dAlpha = 1/240f;
    private float [] texColor = { 1f , 1f, 1f, 1f };
    
    public void drawScene(GL2 gl, float angle) {
        
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_BLEND);
        gl.glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, texColor, 0);
        pierTexture.bind();

        model.display(gl);
        
        gl.glDisable(GL2.GL_TEXTURE_2D);

        /*
        // Prepare for mouse coordinate drawing.
        
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
	gl.glLoadMatrixf(mouseProjectionMatrix.a, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
         
        overlay.setAlpha(alpha);
        overlay.display(gl);
        alpha += dAlpha;
        if (alpha > 1.0f) {
            alpha = 1.0f;
            dAlpha = -dAlpha;
        }
        else if (alpha < .5f) {
            alpha = .5f;
            dAlpha = -dAlpha;            
        }
   
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glEnable(GL2.GL_DEPTH_TEST);
         */
        /*
        // Torus
	gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.8f, 0.0f);
	gl.glRotatef(angle * 1.2f, 1.   0f, 0.0f, 0.0f);
        gl.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        glut.glutSolidTorus(0.2, 0.5, 24, 48);
	gl.glPopMatrix();
        */
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
	cameraPosition.setX(30.0f * (float)Math.cos(cameraAngle));
	cameraPosition.setZ(30.0f * (float)Math.sin(cameraAngle));

	// Draw from camera's point of view
	gl.glClear(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT);

	gl.glMatrixMode(GL2.GL_PROJECTION);
	gl.glLoadMatrixf(cameraProjectionMatrix.a, 0);
	
	gl.glMatrixMode(GL2.GL_MODELVIEW);
	gl.glLoadIdentity();
	glu.gluLookAt(  cameraPosition.x(), cameraPosition.y(), cameraPosition.z(),
			0.0f, 7.0f, 0.0f,
			0.0f, 1.0f, 0.0f);
        
	gl.glViewport(windowX, windowY, windowWidth, windowHeight);

	drawScene(gl, angle);
	
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

        //gl.glPushMatrix();
	gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, windowWidth - 1, 0.0, windowHeight - 1);
	gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mouseProjectionMatrix.a, 0);
	gl.glPopMatrix();        
        
	// Update the camera's projection matrix
	gl.glPushMatrix();
	gl.glLoadIdentity();
	glu.gluPerspective(45.0f, (float)windowWidth/windowHeight, 1.0f, 100.0f);
	gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, cameraProjectionMatrix.a, 0);
	gl.glPopMatrix();        
    }

    public static void main(String [] argv) {
        new SketchupExportViewer().start();
    }

    public void keyTyped(KeyEvent e) {
        switch (e.getKeyChar()) {
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
}
