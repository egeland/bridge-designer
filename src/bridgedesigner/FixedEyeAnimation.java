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
package bridgedesigner;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferStrategy;
import javax.swing.Timer;

/**
 *
 * @author Eugene K. Ressler
 */
public class FixedEyeAnimation extends Animation {
    /**
     * Controls dialog for the animation.
     */
    private final AnimationControls controls;
    /**
     * FlyThruAnimation configuration for access by control dialog.
     */
    private final Config config;
    /**
     * Canvas owned by this animation.
     */
    private final FixedEyeAnimationCanvas canvas;

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * Nothing to update because the view is fixed.
     * 
     * @param elapsed time elapsed in nanoseconds
     */
    @Override
    public void updateView(double elapsed) { }

    /**
     * Start the animation.  Delegate to the canvas.
     */
    @Override
    public void start() {
        canvas.start();
    }

    /**
     * Stop the animation.  Delegate to the canvas.
     */
    @Override
    public void stop() {
        canvas.stop();
    }

    /**
     * Tell the canvas that the background is now invalid.
     * Used by the animation controls to turn background on and off.
     */
    public void invalidateBackground() {
        canvas.invalidateBackground();
    }

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
        this.config = config;
        // Reset default 3 for fade-in/out of truck in flythru.
        loadLocationRunup = 8;
        canvas = new FixedEyeAnimationCanvas(terrain);
        // canvas.setIgnoreRepaint(true);
        controls = new FixedEyeControls(frame, this);
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

    /**
     * Custom canvas for the animation.
     */
    private class FixedEyeAnimationCanvas extends Canvas {

        private final ViewportTransform viewportTransform;
        private final Bridge3dView bridgeView;
        private final Timer timer;
        private Image background;
        private Paint sky;
        private BufferStrategy backBuffer;
        private final FixedEyeTerrainModel terrain;
        private final int frameRate = 1000 / 50;

        FixedEyeAnimationCanvas(FixedEyeTerrainModel terrain) {
            this.terrain = terrain;
            viewportTransform = new ViewportTransform();
            bridgeView = new Bridge3dView(bridge, terrain, config);
            timer = new Timer(frameRate, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    repaint();
                }
            });
            timer.setCoalesce(true);
            timer.setInitialDelay(0);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setViewport();
                }
            });
        }

        private void paintBackground() {
            // Do background image allocation lazily so there's no allocation
            // at all if we don't use this version of animation.
            if (background == null) {
                Dimension screenSize = Utility.getMaxScreenSize();
                background = canvas.createImage(screenSize.width, screenSize.height);
            }
            if (sky == null) {
                // Set up sky as gradient from light cyan at top to white at vanishing point.
                final Point vp = viewportTransform.getVanishingPoint(null);
                sky = new GradientPaint(new Point(vp.x,0), new Color(128,255,255), vp, Color.WHITE);
                Graphics2D g = (Graphics2D) background.getGraphics();
                final int w = viewportTransform.getAbsWidthViewport();
                final int h = viewportTransform.getAbsHeightViewport();
                g.setPaint(sky);
                g.fillRect(0, 0, w, h);
                if (config.showBackground) {
                    terrain.paint(g, viewportTransform);
                }
                g.dispose();
            }
        }

        private void restoreBackground(Graphics2D g, Rectangle b) {
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
                        x1, y1, x2, y2, canvas);
            }
        }

        void drawFrame() {
            final long clock = System.nanoTime();
            final Analysis.Interpolation interpolation = interpolate(clock);
            // This protocol is taken directly from the SE 6 API docs for
            // BufferStrategy.  For most hardware the checking is probably
            // unnecessary because we're not in full screen mode.
            do {
                do {
                    Graphics2D g = (Graphics2D)backBuffer.getDrawGraphics();
                    try {
                        restoreBackground(g, null);
                        bridgeView.paint(g, viewportTransform, interpolation, getDistanceMoved());
                    }
                    finally {
                        g.dispose();
                    }
                } while (backBuffer.contentsRestored());
                backBuffer.show();
                // Probably only useful for XWindows-based systems.
                Toolkit.getDefaultToolkit().sync();
            } while (backBuffer.contentsLost());
        }

        void invalidateBackground() {
            sky = null;
        }
        
        void setViewport() {
            final int w = getWidth();
            final int h = getHeight();
            viewportTransform.setWindow(bridgeView.getPreferredDrawingWindow());
            viewportTransform.setZScale(0.026);
            viewportTransform.setVanishingPoint(0.5, 0.5, FlyThruAnimation.deckHalfWidth);
            viewportTransform.setViewport(0, h - 1, w - 1, 1 - h);
            sky = null;  // invalidate the background
        }

        void start() {
            stop();
            DesignConditions conditions = bridge.getDesignConditions();
            bridgeView.initialize(conditions);
            terrain.initializeTerrain(conditions, 0f, 6f);
            // We can't do this earlier because canvas must be visible.
            if (backBuffer == null) {
                canvas.createBufferStrategy(2);
                backBuffer = canvas.getBufferStrategy();
            }
            setViewport();
            resetState();
            timer.start();
        }

        void stop() {
            timer.stop();
        }

        @Override
        public void paint(Graphics g0) {
            drawFrame();
        }

        @Override
        public void update(Graphics g) {
            drawFrame();
        }
    }
}
