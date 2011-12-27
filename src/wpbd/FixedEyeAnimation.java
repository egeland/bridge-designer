/*
 * FixedEyeAnimation.java
 *
 * Copyright (C) 2008 Eugene K. Ressler
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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferStrategy;

/**
 *
 * @author Eugene K. Ressler
 */
public class FixedEyeAnimation extends Animation {

    /**
     * The OpenGL canvas we're drawing the animation on.
     */
    private Canvas canvas;
    /**
     * Runnable body of frame generator thread.
     */
    private FrameGenerator frameGenerator;
    /**
     * Place to draw updated animation frame.
     */
    private BufferStrategy backBuffer;
    /**
     * The animation view of the bridge being drawn.
     */
    private final Bridge3dView bridgeView;
    /**
     * Custom transformations between world coordinates in meters and viewport coordinates in pixels,
     * where the y-axis origin may be at upper left (with negative viewport height).
     */
    private final ViewportTransform viewportTransform;
    /**
     * Scene background redrawn only when window is resized and blitted the back buffer
     * in lieu of a clear operation.
     */
    private Image background;
    /**
     * Whether background is in need of repainting.
     */
    private boolean backgroundValid = false;
    /**
     * FlyThruAnimation configuration for access by control dialog.
     */
    private final Config config;
    /**
     * Controls dialog for the animation.
     */
    private final AnimationControls controls;
    /**
     * Model of terrain for background.
     */
    private final FixedEyeTerrainModel terrain;
    /**
     * Data controlled by the animation control dialog in WPBDView.
     */
    public static class Config extends Animation.Config {
        public boolean showForcesAsColors = true;
        public boolean showBackground = true;
        public boolean showTruck = true;
        public boolean showAbutments = true;
        public boolean showSmoothTerrain = true;
    }
    /**
     * Return the current animation configuration.
     *
     * @return the configuration
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Return the controls dialog for this animation.
     *
     * @return controls dialog
     */
    public final AnimationControls getControls() {
        return controls;
    }

    private FixedEyeAnimation(Frame frame, EditableBridgeModel bridge, FixedEyeTerrainModel terrain, Config config) {
        super(bridge, terrain, config);
        this.terrain = terrain;
        this.config = config;
        // Reset default 3 for fade-in/out of truck in flythru.
        loadLocationRunup = 8;
        canvas = new Canvas() {
            @Override
            public void paint(Graphics g) { }
            @Override
            public void update(Graphics g) { }
        };
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setViewport();
            }
        });
        // canvas.setIgnoreRepaint(true);
        controls = new FixedEyeControls(frame, this);
        viewportTransform = new ViewportTransform();
        bridgeView = new Bridge3dView(bridge, terrain, config);
    }

    /**
     * A factory to create a new fixed eye animation for a bridge.  We need this
     * rather than a simple constructor due to initialization order constraints.
     *
     * @param bridge bridge to animate, which also is used to initialize terrain
     * @return new fixed eye animation
     */
    public static FixedEyeAnimation create(Frame frame, EditableBridgeModel bridge) {
        Config config = new Config();
        FixedEyeTerrainModel terrain = new FixedEyeTerrainModel(config);
        return new FixedEyeAnimation(frame, bridge, terrain, config);
    }

    // These four methods serialize access to the viewport transform
    // and backgroundValid, which are touched by the event dispatch thread.
    public synchronized void paintBridgeView(Graphics2D g, Analysis.Interpolation interpolation) {
        bridgeView.paint(g, viewportTransform, interpolation, getDistanceMoved());
    }

    public synchronized void setViewport() {
        viewportTransform.setWindow(bridgeView.getPreferredDrawingWindow());
        viewportTransform.setZScale(0.026);
        viewportTransform.setVanishingPoint(0.5, 0.5, FlyThruAnimation.deckHalfWidth);
        final int w = canvas.getWidth();
        final int h = canvas.getHeight();
        viewportTransform.setViewport(0, h - 1, w - 1, 1 - h);
        // Set up sky as gradient from light cyan at top to white at vanishing point.
        final Point vp = viewportTransform.getVanishingPoint(null);
        sky = new GradientPaint(new Point(vp.x,0), new Color(128,255,255), vp, Color.WHITE);
        backgroundValid = false;
    }

    private Paint sky = null;

    private synchronized void paintBackground() {
        if (!backgroundValid) {
            if (background == null) {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                background = canvas.createImage(screenSize.width, screenSize.height);
            }
            Graphics2D g = (Graphics2D) background.getGraphics();
            final int w = viewportTransform.getAbsWidthViewport();
            final int h = viewportTransform.getAbsHeightViewport();
            g.setPaint(sky);
            g.fillRect(0, 0, w, h);
            if (config.showBackground) {
                terrain.paint(g, viewportTransform);
            }
            g.dispose();
            backgroundValid = true;
        }
    }

    public synchronized void invalidateBackground() {
        backgroundValid = false;
    }

    private void restoreFromBackingStore(Graphics2D g, Rectangle b) {
        paintBackground();
        if (b == null) {
            g.drawImage(background, 0, 0, canvas);
        } else {
            int x1 = b.x;
            int y1 = b.y;
            int x2 = b.x + b.width;
            int y2 = b.y + b.height;
            g.drawImage(background,
                    x1, y1, x2, y2,
                    x1, y1, x2, y2, null);
        }
    }

    private static final long clockSecond = 1000000000;
    private static final long frameDuration = clockSecond / 50;

    private class FrameGenerator implements Runnable {

        private volatile boolean running = true;
        private Thread myThread = null;

        public void run() {
            myThread = Thread.currentThread();
            long lastFrameClock = System.nanoTime();
            while (running) {
                // Open frame.
                long clock = System.nanoTime();
                Graphics2D g = (Graphics2D)backBuffer.getDrawGraphics();

                // Paint frame.
                restoreFromBackingStore(g, null);
                paintBridgeView(g, interpolate(clock));

                // Close frame.
                g.dispose();
                backBuffer.show();

                // Synchronize frame.
                final long sleepTime = lastFrameClock + frameDuration - clock;
                lastFrameClock = clock;
                if (sleepTime > clockSecond / 500) {
                    try {
                        Thread.sleep(sleepTime / 1000000);
                    } catch (InterruptedException ex) {  }
                }
            }
        }

        public synchronized void stop() {
            if (running) {
                running = false;
                if (myThread != null) {
                    try {
                        myThread.join();
                    } catch (InterruptedException ex) { }
                }
            }
        }
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    public void updateView(double elapsed) {
        // Nothing to do here because view is fixed.
    }

    @Override
    public void start() {
        stop();
        DesignConditions conditions = bridge.getDesignConditions();
        bridgeView.initialize(conditions);
        terrain.initializeTerrain(conditions, 0f, 6f);
        // We can't do his earlier because canvas must be visible.
        if (backBuffer == null) {
            canvas.createBufferStrategy(2);
            backBuffer = canvas.getBufferStrategy();
        }
        setViewport();
        resetState();
        frameGenerator = new FrameGenerator();
        Thread frameGeneratorThread = new Thread(frameGenerator);
        frameGeneratorThread.setPriority(Thread.MIN_PRIORITY);
        frameGeneratorThread.start();
    }

    @Override
    public void stop() {
        // Cause frame generator thread to terminate.
        if (frameGenerator != null) {
            frameGenerator.stop();
            frameGenerator = null;
        }
    }
}
