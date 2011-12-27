package wpbd;

import com.jogamp.opengl.util.FPSAnimator;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Test extends JFrame implements GLEventListener {

    private static final int CANVAS_WIDTH = 640;  // Width of the drawable
    private static final int CANVAS_HEIGHT = 480; // Height of the drawable
    private static final int FPS = 60;   // Animator's target frames per second
    private CardLayout cards;
    private static final String LABEL = "label";
    private static final String CANVAS = "canvas";
    private String selected = LABEL;
    private FPSAnimator animator;

    private class Drawing extends JPanel {

        final Renderer3d r;

        public Drawing() {
            r = new Renderer3d();
            r.setGouraudColor(new int [] { 100, 200, 255 });
            setBackground(Color.DARK_GRAY);
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            r.drawGouraudTriangle(this, g,
                    new int [] { 50, 200,   0 },
                    new int [] { 90,   0, 250 },
                    new float [] { .2f, 1f, .7f },
                    new float [] { 0f, 0f, 0f } );
            r.drawGouraudTriangle(this, g,
                    new int [] { 200, 300, 0 },
                    new int [] { 0, 100, 250 },
                    new float [] { 1f, .2f, .7f },
                    new float [] { 0f, 0f, 0f } );
        }
    }

    public void run() {
        // Get the default OpenGL profile that best reflect your running platform.
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        // Specifies a set of OpenGL capabilities, based on your profile.
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAccumAlphaBits(0);
        caps.setAccumRedBits(0);
        caps.setAccumGreenBits(0);
        caps.setAccumBlueBits(0);
        caps.setAlphaBits(8);
        caps.setRedBits(8);
        caps.setGreenBits(8);
        caps.setBlueBits(8);
        caps.setDepthBits(32);
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);
        caps.setOnscreen(true);
        caps.setPBuffer(false);
        caps.setSampleBuffers(false);
        caps.setStencilBits(0);
        caps.setStereo(false);
        caps.setBackgroundOpaque(true);
        // Allocate a GLDrawable, based on your OpenGL caps.
        GLCanvas canvas = new GLCanvas(caps);
        canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
        canvas.addGLEventListener(this);

        // Create a animator that drives canvas' display() at 60 fps.
        animator = new FPSAnimator(canvas, FPS);

        addWindowListener(new WindowAdapter() {     // For the close button

            @Override
            public void windowClosing(WindowEvent e) {
                // Use a dedicate thread to run the stop() to ensure that the
                // animator stops before program exits.
                new Thread() {

                    @Override
                    public void run() {
                        animator.stop();
                        System.exit(0);
                    }
                }.start();
            }
        });

        JButton button = new JButton("Switch Cards");
        add(button, BorderLayout.NORTH);
        final JPanel cardHolder = new JPanel();
        cards = new CardLayout();
        cardHolder.setLayout(cards);
        //cardHolder.add(new JLabel("A label to cover the canvas"), LABEL);
        cardHolder.add(new Drawing(), LABEL);
        cardHolder.add(canvas, CANVAS);
        add(cardHolder, BorderLayout.CENTER);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selected.equals(LABEL)) {
                    animator.start();
                    cards.show(cardHolder, CANVAS);
                    selected = CANVAS;
                } else {
                    animator.stop();
                    cards.show(cardHolder, LABEL);
                    selected = LABEL;
                }
            }
        });
        pack();
        setTitle("OpenGL 2 Test");
        setVisible(true);
    }

    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    new Test().run();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }
    float spin = 0;

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glPushMatrix();
        gl.glRotatef(spin, 0.0f, 0.0f, 1.0f);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glRectf(-25.0f, -25.0f, 25.0f, 25.0f);
        gl.glPopMatrix();
        gl.glFlush();
        spin += 1;
        while (spin > 360) {
            spin -= 360;
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL2 gl = drawable.getGL().getGL2();
        GLContext ctx = gl.getContext();
        System.err.println(
                "Handle="+ctx.getHandle()
                +" Current="+GLContext.getCurrent().getHandle()
                +ctx
                +animator.getThread()
                +gl.getContext().isCurrent());
        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        if (w <= h) {
            gl.glOrtho(-50.0, 50.0,//
                    -50.0 * (float) h / (float) w, //
                    50.0 * (float) h / (float) w, //
                    -1.0, 1.0);
        } else {
            gl.glOrtho(-50.0 * (float) w / (float) h,
                    50.0 * (float) w / (float) h, -50.0, 50.0, //
                    -1.0, 1.0);
        }

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Hardly used.
    }
}
