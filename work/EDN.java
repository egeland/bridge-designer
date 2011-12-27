/*
 * EDN.java  
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

import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

/**
 *
 * @author Eugene K. Ressler
 */
public class EDN implements GLEventListener, KeyListener {

    public static final String xFile = "weddinggammacorrected.jpg";
    public static final float initialStateRandomMagnitude = .05f;
    //public static final float convergenceConstant = 0.045f;
    public static final float convergenceConstant = 0.075f;
    public static final float rhoR = 2f;
    
    public static final double betaR = 0.30f;
    public static final double betaG = 0.59f;
    public static final double betaB = 0.11f;
    
    private static String [] enhancedEdnCalculationShaderSrc = {
        // this is 7 x 7 minus the middle element.
        "const int kernelSize = 48;",
        
        // width of smooth step function
        "const float stepWidth = 0.01;",
        
        // offsets of kernel entries
        "uniform vec2 offsets[kernelSize];",

        // kernel weights
        "uniform float weights[kernelSize];",
        
        // convergence constant and width of smooth step function
        "uniform float alpha;",
        
        // Huang algorithm cross-plane mixing weights.
        "uniform mat3 kInv;",
        
        // switch to control type of processing
        "uniform int phase;",

        // textures for picture and edn state
        "uniform sampler2D xTex, uTexA, uTexB;",
      
        "void main() {",
            "if (phase == 0) {",
                // edn iteration forward phase
                "vec3 err = vec3(0.0);",
                "vec3 x = texture2D(xTex, gl_TexCoord[1].st).rgb;",
                //"vec3 x = vec3(0.08);",
                "for (int i = 0; i < kernelSize; i++) {",
                    "vec3 u = texture2D(uTexA, gl_TexCoord[0].st + offsets[i]).rgb;",
                    //"vec3 y = smoothstep(0.5 - stepWidth, 0.5 + stepWidth, u);",
                    "vec3 y = step(0.5, u);",
                    "err += weights[i] * (y - u);",
                "}",
                "vec3 u0 = texture2D(uTexA, gl_TexCoord[0].st).rgb;",
                "vec3 y0 = smoothstep(0.5 - stepWidth, 0.5 + stepWidth, u0);",
                "vec3 err0 = err - (y0 - u0);",
                "err += kInv * err0;",
                "gl_FragColor.rgb = mix(u0, x - err, alpha);",
            "} else if (phase == 1) {",
                // edn iteration backward phase
                "vec3 err = vec3(0.0);",
                "vec3 x = texture2D(xTex, gl_TexCoord[1].st).rgb;",
                //"vec3 x = vec3(0.08);",
                "for (int i = 0; i < kernelSize; i++) {",
                    "vec3 u = texture2D(uTexB, gl_TexCoord[0].st + offsets[i]).rgb;",
                    //"vec3 y = smoothstep(0.5 - stepWidth, 0.5 + stepWidth, u);",
                    "vec3 y = step(0.5, u);",
                    "err += weights[i] * (y - u);",
                "}",
                "vec3 u0 = texture2D(uTexB, gl_TexCoord[0].st).rgb;",
                "vec3 y0 = smoothstep(0.5 - stepWidth, 0.5 + stepWidth, u0);",
                "vec3 err0 = err - (y0 - u0);",
                "err += kInv * err0;",
                "vec3 rgb = mix(u0, x - err, alpha);",
                "gl_FragColor.rgb = mix(u0, x - err, alpha);",
            "} else if (phase == 2) {",
                "gl_FragColor = step(0.5, texture2D(uTexA, gl_TexCoord[0].st));",
                //"gl_FragColor = vec4(10.0) * (texture2D(uTexA, gl_TexCoord[0].st) - vec4(0.5)) + vec4(.5);",
            "} else {",
                "gl_FragColor = step(0.5, texture2D(uTexB, gl_TexCoord[0].st));",
                //"gl_FragColor = vec4(10.0) * (texture2D(uTexB, gl_TexCoord[0].st) - vec4(0.5)) + vec4(.5);",
            "}",
        "}"
    };
    
    private final float weights[] = {
        .0005f, .0020f, .0052f, .0069f, .0052f, .0020f, .0005f,
        .0020f, .0104f, .0249f, .0329f, .0249f, .0104f, .0020f,
        .0052f, .0249f, .0584f, .0767f, .0584f, .0249f, .0052f,
        .0069f, .0329f, .0767f,         .0767f, .0329f, .0069f,
        .0052f, .0249f, .0584f, .0767f, .0584f, .0249f, .0052f,
        .0020f, .0104f, .0249f, .0329f, .0249f, .0104f, .0020f,
        .0005f, .0020f, .0052f, .0069f, .0052f, .0020f, .0005f,
    };
    
    private final float offsets [] = new float [weights.length * 2]; 
    
    private final float kInv [] = new float[9];
    
    private final GLCanvas canvas;
    private final JFrame frame;
    private Texture xTex;
    private Texture uTexA, uTexB;
    private int uFrameBufferA, uFrameBufferB;
    private int ednCalculationShader;
    private int ednCalculationShaderProgram;
    private int pendingIterationCount = 0;
    private int totalIterationCount = 0;
    private int lastCalculationTime = 0;
    private int lastIterationCount = 0;
    private GLUT glut;

    // Shader uniform variable locations.
    int offsetsLoc, weightsLoc, alphaLoc, phaseLoc, xTexLoc, uTexALoc, uTexBLoc, kInvLoc;
    
    public EDN() {
        canvas = new GLCanvas();
        canvas.setSize(1032, 1056);
        frame = new JFrame("Fast Error Diffusion Network");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
    }
    
    private void initializeOffsets(int pitch) {
        final int size = (int)Math.sqrt(weights.length + 1);
        final int rad = size / 2;
        int k = 0;
        for (int i = -rad; i <= rad; i++) {
            for (int j = -rad; j <= rad; j++) {
                if (i != 0 || j != 0) {
                    offsets[k++] = (float)j / pitch;
                    offsets[k++] = (float)i / pitch;
                }
            }
        }
    }
    
    /**
     * Set up the K^-1 matrix given rhoR, which is the weight of ther
     * red component with respect to the luminosity.
     * @param rhoR
     */
    private void initializeKInv(float rhoR) {
        double r = 1 + rhoR / (betaR * betaR);
        double A = betaG * betaG * r;
        double B = betaB * betaB - betaR * betaR * r * r - betaG * betaG;
        double C = r * (betaR * betaR - betaB * betaB);
        double g = (Math.sqrt(B * B - 4 * A * C) - B) / (2 * A);
        double b = (betaG * betaG - betaR * betaR) / (betaG * betaG * g - betaR * betaR * r);
        double rhoG = (g - 1) * (betaG * betaG);
        double rhoB = (b - 1) * (betaB * betaB);
        double d  = (g * b - 1) / (betaR * betaR);
        double dG = (r * b - 1) / (betaG * betaG);
        double dB = (r * g - 1) / (betaB * betaB);
        float kRG = (float) ((1 - b) / (d * betaR * betaG));
        float kRB = (float) ((1 - g) / (d * betaR * betaB));
        float kGB = (float) ((1 - r) / (d * betaG * betaB));
        
        kInv[0] = 0f;  kInv[3] = kRG; kInv[6] = kRB;
        kInv[1] = kRG; kInv[4] = 0f;  kInv[7] = kGB;
        kInv[2] = kRB; kInv[5] = kGB; kInv[8] = 0f;
        
        // Sanity check.
        System.err.format("beta=%.2f\n", betaR + betaG + betaB);
        System.err.format("rhoR=%.2f rhoG=%.2f rhoB=%.2f\n", rhoR, rhoG, rhoB);
        System.err.format("d = %.2f %.2f %.2f\n", d, dG, dB);
        System.err.format("kRG =%.4f kRB =%.4f kGB =%.4f\n", kRG, kRB, kGB);
        
        float sum = 0;
        float sumSqr = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            sumSqr += weights[i] * weights[i];
        }
        System.err.format("wSum=%.4f wSumSqr=%.4f 2norm=%.4f\n", sum, sumSqr, Math.sqrt(sumSqr));
    }
            
    public void start() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize( frame.getContentPane().getPreferredSize() );
        frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
        frame.setVisible(true);        
        canvas.requestFocus();
    }
    
    public void init(GLAutoDrawable drawable) {
        glut = new GLUT();
        GL2 gl = drawable.getGL();

        ednCalculationShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        gl.glShaderSource(ednCalculationShader, enhancedEdnCalculationShaderSrc.length, enhancedEdnCalculationShaderSrc, null, 0);
        gl.glCompileShader(ednCalculationShader);
        printShaderLog(gl, ednCalculationShader);
        ednCalculationShaderProgram = gl.glCreateProgram();
        gl.glAttachShader(ednCalculationShaderProgram, ednCalculationShader);        
        gl.glLinkProgram(ednCalculationShaderProgram);
        gl.glValidateProgram(ednCalculationShaderProgram);
        gl.glUseProgram(ednCalculationShaderProgram); 

        offsetsLoc  = gl.glGetUniformLocation(ednCalculationShaderProgram, "offsets");
        weightsLoc  = gl.glGetUniformLocation(ednCalculationShaderProgram, "weights");
        alphaLoc    = gl.glGetUniformLocation(ednCalculationShaderProgram, "alpha");
        kInvLoc     = gl.glGetUniformLocation(ednCalculationShaderProgram, "kInv");
        phaseLoc    = gl.glGetUniformLocation(ednCalculationShaderProgram, "phase");
        xTexLoc     = gl.glGetUniformLocation(ednCalculationShaderProgram, "xTex");
        uTexALoc    = gl.glGetUniformLocation(ednCalculationShaderProgram, "uTexA");
        uTexBLoc    = gl.glGetUniformLocation(ednCalculationShaderProgram, "uTexB");

        // Connect samplers to correct texture units.
        gl.glUniform1i(xTexLoc, 0);
        gl.glUniform1i(uTexALoc, 1);
        gl.glUniform1i(uTexBLoc, 2);
                
        // Create and load the picture we want to halftone in texture unit 0.
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        xTex = WPBDApp.getApplication().getTextureResource(xFile, false, TextureIO.JPG);
        xTex.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        xTex.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        
        // Fill float buffer with numbers dithered about 0.5 .
        int w = xTex.getImageWidth();
        int h = xTex.getImageHeight();
        FloatBuffer buf = FloatBuffer.allocate(w * h * 3);
        int k = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ///* Random initialization
                for (int m = 0; m < 3; m++) {
                    buf.put(k++, initialStateRandomMagnitude * ((float)Math.random() - 0.5f) + 0.5f ) ;
                }
                //*/
                /* Hi frequency initialization 
                float val = (((i + j) & 1) == 0) ? 
                    0.5f + initialStateRandomMagnitude * 0.5f : 0.5f - initialStateRandomMagnitude * 0.5f;
                buf.put(k++, val);
                buf.put(k++, 1f - val);
                buf.put(k++, val);
                //*/
            }
        }

        // Create and load the state A texture in unit 1 with initial random values.
        gl.glActiveTexture(GL2.GL_TEXTURE1);
        uTexA = TextureIO.newTexture(GL2.GL_TEXTURE_2D);
        uTexA.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        uTexA.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        uTexA.setTexParameterf(GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        uTexA.setTexParameterf(GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB32F_ARB, xTex.getImageWidth(), xTex.getImageHeight(), 0, GL2.GL_RGB, GL2.GL_FLOAT, buf);

        // Create and load the state B texture in unit 2 with initial random values.
        gl.glActiveTexture(GL2.GL_TEXTURE2);
        uTexB = TextureIO.newTexture(GL2.GL_TEXTURE_2D);
        uTexB.setTexParameteri(GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        uTexB.setTexParameteri(GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        uTexB.setTexParameterf(GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        uTexB.setTexParameterf(GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB32F_ARB, xTex.getImageWidth(), xTex.getImageHeight(), 0, GL2.GL_RGB, GL2.GL_FLOAT, buf);
        
        // Connect offsets.
        initializeOffsets(xTex.getImageWidth());
        gl.glUniform2fv(offsetsLoc, weights.length, offsets, 0);
        
        // Connect weights.
        gl.glUniform1fv(weightsLoc, weights.length, weights, 0);
        
        // Set alpha.
        gl.glUniform1f(alphaLoc, convergenceConstant);
 
        // Set kInv.
        initializeKInv(rhoR);
        gl.glUniformMatrix3fv(kInvLoc, 1, false, kInv, 0);
        
        // Set phase.
        gl.glUniform1i(phaseLoc, 0);
         
        printProgramLog(gl, ednCalculationShaderProgram);
        
        uFrameBufferA = genFramebuffer(gl);
        gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, uFrameBufferA);
        gl.glFramebufferTexture2DEXT(GL2.GL_FRAMEBUFFER_EXT, GL2.GL_COLOR_ATTACHMENT0_EXT, GL2.GL_TEXTURE_2D, uTexA.getTextureObject(), 0);
        if (gl.glCheckFramebufferStatusEXT(GL2.GL_FRAMEBUFFER_EXT) != GL2.GL_FRAMEBUFFER_COMPLETE_EXT) {
            System.err.println("Frame buffer A is incomplete.");
            System.exit(1);
        }
        
        uFrameBufferB = genFramebuffer(gl);
        gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, uFrameBufferB);
        gl.glFramebufferTexture2DEXT(GL2.GL_FRAMEBUFFER_EXT, GL2.GL_COLOR_ATTACHMENT0_EXT, GL2.GL_TEXTURE_2D, uTexB.getTextureObject(), 0);
        if (gl.glCheckFramebufferStatusEXT(GL2.GL_FRAMEBUFFER_EXT) != GL2.GL_FRAMEBUFFER_COMPLETE_EXT) {
            System.err.println("Frame buffer B is incomplete.");
            System.exit(1);
        }

        gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, 0);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    private void sendCoord(GL2 gl, float x, float y) {
        // Use texture unit 0's interpolator texture coords with origin at lower left...
        gl.glMultiTexCoord2f(GL2.GL_TEXTURE0, x, y);
        // ... and unit 1 for coords with the y-axis reversed.  We'll use this for access to xTex, 
        // because JOGL jpeg TextureData reader puts last row, image bottom, at y=0.
        gl.glMultiTexCoord2f(GL2.GL_TEXTURE1, x, 1f - y);
        gl.glVertex2f(x, y);
    }
    
    private void runShader(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);
        sendCoord(gl, 0f, 0f);
        sendCoord(gl, 1f, 0f);
        sendCoord(gl, 1f, 1f);
        sendCoord(gl, 0f, 1f);
        gl.glEnd();
    }

    private int phase = 0;
    
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL();
        
        gl.glUseProgram(ednCalculationShaderProgram); 
        
        // Do calculations if user has requested any.
        if (pendingIterationCount > 0) {
            final long start = System.nanoTime();
            for (int i = 0; i < pendingIterationCount; i++) {

                // Select the forward convolution phase of the shader.
                gl.glUniform1i(phaseLoc, phase);

                // Send output to the state B texture.
                gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, phase == 0 ? uFrameBufferB : uFrameBufferA);

                // Do the forward calculation
                runShader(gl);
                
                // Flip phase bit.
                phase ^= 1;
            }
            
            // Flush all the graphics ops before clicking the stop watch.
            gl.glFinish();
            final long end = System.nanoTime();
            
            // Find time in miliseconds.
            lastCalculationTime = (int)((end - start) / 1000000);
            
            // Update counts.
            totalIterationCount += pendingIterationCount;
            lastIterationCount = pendingIterationCount;
            pendingIterationCount = 0;
        }
        
        // Select the thresholding phase of the shader and send output to the window system.
        gl.glUniform1i(phaseLoc, phase + 2);        
        gl.glBindFramebufferEXT(GL2.GL_FRAMEBUFFER_EXT, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        runShader(gl);

        // Turn off the shader to draw some text.
        gl.glUseProgram(0);         
        gl.glRasterPos2f(0.01f, 0.01f);
        gl.glColor3f(1f, 0f, 0f);
        String note = String.format("rhoR=%.1f %.1fms per for %d iterations (%d total)", 
                rhoR, (float)lastCalculationTime / lastIterationCount, lastIterationCount, totalIterationCount);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, note);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL2 gl = drawable.getGL();
        gl.glViewport(x, y, 1024, 1024);
    }

    public static void printShaderLog(GL2 gl, int shader) {
        System.err.println("Shader " + shader + " log:");
        int [] val = new int[1];
        gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, val, 0);
        if (val[0] > 0) {
            byte text [] = new byte[val[0]];
            gl.glGetShaderInfoLog(shader, val[0], val, 0, text, 0);
            System.err.println(new String(text, 0, Math.max(0, text.length - 1)));
        }
    }
    
    public static void printProgramLog(GL2 gl, int prog) {
        System.err.println("Program " + prog + " log:");
        int [] val = new int[1];
        gl.glGetShaderiv(prog, GL2.GL_INFO_LOG_LENGTH, val, 0);
        if (val[0] > 0) {
            byte text [] = new byte[val[0]];
            gl.glGetProgramInfoLog(prog, val[0], val, 0, text, 0);
            System.err.println(new String(text, 0, Math.max(0, text.length - 1)));
        }
    }
    
    private static int genFramebuffer(GL2 gl) {
        int [] val = new int [1];
        gl.glGenFramebuffersEXT(1, val, 0);
        return val[0];
    }

    public void keyTyped(KeyEvent e) {
        char ch = e.getKeyChar();
        int shiftedNum = "!@#$%^&*()".indexOf(ch) + 1;
        
        if (shiftedNum > 0) {
            pendingIterationCount += shiftedNum * 10;
        }
        else if ('1' <= ch && ch <= '9') {
            pendingIterationCount += (ch - '0');
        }
        else switch (ch) {
            case ' ':
                pendingIterationCount += 1;
                break;
            case '\u001b':
                System.exit(0);
                break;
        }
        canvas.repaint();
    }
    
    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        /* ignore */
    }

    public static void main(String [] argv) {
        new EDN().start();
    }
}
