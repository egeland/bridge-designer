/*
 * FlyThruAnimation.java
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

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.logging.Level;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import jogamp.common.Debug;
import bridgedesigner.Analysis.Interpolation;

/**
 * User-controllable 3D animation of a dynamically loaded bridge and surrounding terrain.
 * 
 * @author Eugene K. Ressler
 */
public class FlyThruAnimation extends Animation {

     /**
     * The OpenGL canvas we're drawing the animation on.
     */
    private AnimationCanvas canvas;
    /**
     * Terrain model model we're drawing.
     */
    private final FlyThruTerrainModel terrain;
    /**
     * Meters needed above deck to clear truck load.  Controls whether there are cross-members at joints. Exported
     * for joint and member drawing routines to determine when to skip X-braces and cross members (Joint and Member).
     */
    public static final double deckClearance = 5.0;  // height of truck
    /**
     * Width in meters of a deck beam.  Height is established based on deck thickness and height of wear surface
     * above load-bearing joints.
     */
    private static final double deckBeamWidth = 12 * .01;
    /**
     * Height of driver's eye above road surface.
     */
    private static final double driverEyeHeight = 2.4;
    /**
     * Distance from front axel (reference point) forward to driver's eye.
     */
    private static final double driverEyeLead = 0.6;
    /**
     * Number of panel lengths truck moves while "fading in" with alpha changing from 0 to 1.
     */
    private static final double loadFadeInDistance = 2.0; /* panel lengths */
    /**
     * Max radians of look up-down angle.
     */
    private static final double maxTilt = 0.5 * Math.PI * 0.75;
    /**
     * Meters per second per pixel of mouse drag
     */
    private static final double linearUIRate = 10.0 / 100.0;
    /**
     * Radians per second rotation.
     */
    private static final double rotationalUIRate = 0.05 * 2.0 * Math.PI / 100.0;
    /**
     * Radians per pixel of mouse movement.
     */
    private static final double tiltUIRate = 5.0 * 2.0 * Math.PI / 100.0;
    /**
     * Prototypical vertices and normals for a deck slab.
     */
    private static final float[] deckVerticesLeft = {
        0.0f, 0f, +.5f,
        0.0f, 0f, -.5f,
        0.0f, -1f, -.5f,
        0.0f, -1f, +.5f,
        0.0f, 0f, +.5f,};
    private static final float[] deckVerticesRight = {
        1.0f, 0f, +.5f,
        1.0f, 0f, -.5f,
        1.0f, -1f, -.5f,
        1.0f, -1f, +.5f,
        1.0f, 0f, +.5f,};
    private static final float[] deckNormals = {
        0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, 0.0f, 1.0f,};
    /**
     * For calculating texture matrix for projection, this matrix
     * takes us from eye space to the light's clip space. It is
     * postmultiplied by the inverse of the current view matrix when
     * specifying texgen bias from [-1, 1] to [0, 1].
     */
    private static final Homogeneous.Matrix biasMatrix = new Homogeneous.Matrix(
            0.5f, 0.0f, 0.0f, 0.5f,
            0.0f, 0.5f, 0.0f, 0.5f,
            0.0f, 0.0f, 0.5f, 0.5f,
            0.0f, 0.0f, 0.0f, 1.0f);

    /**
     * Data controlled by the animation control dialog in WPBDView.
     */
    public static class Config extends Animation.Config {

        public boolean showForcesAsColors = true;
        public float lightBrightness[] = {0.6f, 0.6f, 0.6f, 1f};
        public boolean canShowShadows = !Debug.isPropertyDefined("wpbd.noshadows", false);
        public boolean showShadows = false;
        public boolean showTruck = true;
        public boolean showSky = true;
        public boolean showTerrain = true;
        public boolean showAbutments = true;
        public boolean showErrosion = false;
    }
    /**
     * FlyThruAnimation configuration for access by control dialog.
     */
    private final Config config;
    /**
     * Controls dialog for the animation.
     */
    private final AnimationControls controls;

    /**
     * Return the controls dialog for this animation.
     * 
     * @return controls dialog
     */
    public final AnimationControls getControls() {
        return controls;
    }

    /**
     * Constructor for a flythru animation, which is private so that the
     * static factory must be used to create a new one.
     *
     * @param bridge bridge that should be animated
     * @param terrain terrain surrounding the bridge
     * @param config configuration describing current animation options
     */
    private FlyThruAnimation(Frame frame, EditableBridgeModel bridge, FlyThruTerrainModel terrain, Config config) {
        super(bridge, terrain, config);
        this.terrain = terrain;
        this.config = config;
        this.controls = new FlyThruControls(frame, this);
        canvas = new AnimationCanvas();
    }

    /**
     * A factory to create a new flythru animation for a bridge.  We need this
     * rather than a simple constructor due to initialization order constraints.
     *
     * @param bridge bridge to animate, which also is used to initialize terrain
     * @return new flythru animation
     */
    public static FlyThruAnimation create(Frame frame, EditableBridgeModel bridge) {
        Config config = new Config();
        FlyThruTerrainModel terrain = new FlyThruTerrainModel(config);
        return new FlyThruAnimation(frame, bridge, terrain, config);
    }

    @Override
    public AnimationCanvas getCanvas() {
        return canvas;
    }

    @Override
    public void start() {
        canvas.start();
    }

    @Override
    public void stop() {
        canvas.stop();
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
     * Return the capabilies we need from the graphics system to do
     * the animation. It turns out the defaults are fine.
     * @return capabilies needed for the animation
     */
    private static GLCapabilities getAnimationCapabilities() {
        GLCapabilities capabilities = new GLCapabilities(BDApp.getGLProfile());
        capabilities.setAccumAlphaBits(0);
        capabilities.setAccumRedBits(0);
        capabilities.setAccumGreenBits(0);
        capabilities.setAccumBlueBits(0);
        capabilities.setAlphaBits(8);
        capabilities.setRedBits(8);
        capabilities.setGreenBits(8);
        capabilities.setBlueBits(8);
        capabilities.setDepthBits(24);
        capabilities.setDoubleBuffered(true);
        capabilities.setHardwareAccelerated(true);
        capabilities.setOnscreen(true);
        capabilities.setPBuffer(false);
        capabilities.setSampleBuffers(false);
        capabilities.setStencilBits(0);
        capabilities.setStereo(false);
        capabilities.setBackgroundOpaque(true);
        return capabilities;
    }

    /**
     * Helper to get an integer value from the OpenGL context object.
     *
     * @param gl OpenGL object
     * @param pname value's property name
     * @return integer value
     */
    private static int getInteger(GL2 gl, int pname) {
        int[] val = new int[1];
        gl.glGetIntegerv(pname, val, 0);
        return val[0];
    }

    /**
     * Helper to get an integer texture value from the OpenGL context object.
     *
     * @param gl OpenGL object
     * @param target target texture
     * @param level mipmap level
     * @param pname value's property name
     * @return integer value
     */
    private static int getTexLevelParameteri(GL2 gl, int target, int level, int pname) {
        int[] val = new int[1];
        gl.glGetTexLevelParameteriv(target, level, pname, val, 0);
        return val[0];
    }

    /**
     * Helper to generate a new frame buffer handle.
     *
     * @param gl OpenGL object
     * @return fresh frame buffer handle
     */
    private static int genFramebuffer(GL2 gl) {
        int[] val = new int[1];
        gl.glGenFramebuffers(1, val, 0);
        return val[0];
    }

    /**
     * Helper to delete the given fraem buffer object.
     *
     * @param gl OpenGL object
     * @param bufferId FBO identifier
     */
    private static void deleteFramebuffer(GL2 gl, int bufferId) {
        gl.glDeleteFramebuffers(1, new int[]{bufferId}, 0);
    }

    /**
     * Return the integer i &lt;= x such that i=2^n for some n &gt;=0
     *
     * @param x see above
     * @return power of 2 with value at most x
     */
    private static int powerOf2AtMost(int x) {
        int i = 1;
        while (i <= x) {
            i <<= 1;
        }
        return i >> 1;
    }

    public class AnimationCanvas extends GLCanvas implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {

        /**
         * Largest number of pixels to attempt allocating the shadow buffer at.
         */
        private int maxShadowTextureSize = 4 * 1024;
        /**
         * Texture object for the shadow map.
         */
        private Texture shadowMapTexture;
        /**
         * Shadow map dimensions.
         */
        private int shadowMapWidth, shadowMapHeight;
        /**
         * Handle for shadow FBO.
         */
        private int shadowFrameBuffer = -1;
        /**
         * Window rectangle information.
         */
        private int windowX, windowY, windowWidth, windowHeight;
        /**
         * General purpose colors.
         */
        private final float[] white = {1f, 1f, 1f, 1f};
        private final float[] lowWhite = {.4f, .4f, .45f, 1f};
        private final float[] black = {0f, 0f, 0f, 1f};
        /**
         * Color of the deck material.
         */
        private final float[] deckColor = {0.6f, 0.6f, 0.6f, 1.0f};
        /**
         * Color of the deck beams.
         */
        private final float[] beamMaterial = {0.3f, .2f, 0.1f, 1.0f};
        /**
         * Color of the fog.
         */
        private final float[] fogColor = {0.6f, 0.6f, 0.8f, 1.0f};
        /**
         * Projection from the eye point.
         */
        private final Homogeneous.Matrix eyeProjectionMatrix = new Homogeneous.Matrix();
        /**
         * Projection maintaining screen coordinates for mouse event processing.
         */
        private final Homogeneous.Matrix mouseProjectionMatrix = new Homogeneous.Matrix();
        /**
         * Projection from the light position for shadow computations.
         */
        private final Homogeneous.Matrix trapezoidalProjectionMatrix = new Homogeneous.Matrix();
        // private final Homogeneous.Matrix offsetTrapezoidalProjectionMatrix = new Homogeneous.Matrix();
        /**
         * Projection used for the debugging view of the camera frustum and trapezoid.
         */
        private final Homogeneous.Matrix lightProjectionMatrix = new Homogeneous.Matrix();
        /**
         * View from the light for shadow computations.
         */
        private final Homogeneous.Matrix lightViewMatrix = new Homogeneous.Matrix();
        /**
         * View from eye point.
         */
        private final Homogeneous.Matrix eyeViewMatrix = new Homogeneous.Matrix();
        /**
         * Shadow texture mapping transformation.
         */
        private final Homogeneous.Matrix textureMatrix = new Homogeneous.Matrix();
        /**
         * Temp storage for a displaced joint location.
         */
        private final Affine.Point ptDisplacedJoint = new Affine.Point();
        /**
         * The model for the truck body we're animating.
         */
        private FlyThruTruckModel truckModel;
        private FlyThruTruckCabModel truckCabModel;
        /**
         * Model for the wheels of the truck.
         */
        private FlyThruWheelModel wheelModel;
        /**
         * Animator to trigger redisplays at specified frame rate.
         */
        private FPSAnimator animator;
        /**
         * State  shadow offset: on or off.
         */
        // private boolean offsetShadows = true;
        /**
         *
         */
        private boolean ignoreBoundaries = false;
        /**
         * Graphics utilities library accessors.
         */
        private GLU glu;
        private GLUT glut;
        /**
         * Click and drag locations of mouse respectively.
         */
        private int x0Mouse = 0;
        private int y0Mouse = 0;
        private int x1Mouse = 0;
        private int y1Mouse = 0;
        /**
         * Java bean for view location that can be saved to user's local storage.
         */
        private AnimationViewLocalStorable view = new AnimationViewLocalStorable();
        /**
         * Name of local storage file.
         */
        private final String animationViewStorage = "animationViewState.xml";
        /**
         * Dependent view parameters.
         */
        private double xLookAt = 30.0;
        private double yLookAt = 0.0;
        private double zLookAt = 0.0;
        private double xEyeMin = 0.0;
        private double xEyeMax = 0.0;
        private double yEyeMin = 0.0;
        private double yEyeMax = 0.0;
        private double zEyeMin = 0.0;
        private double zEyeMax = 0.0;
        private double thetaEyeRate = 0.0;
        private double phiEyeRate = 0.0;
        private double xzEyeVelocity = 0.0;
        private double yEyeVelocity = 0.0;
        private double phiDriverHead = 0.0;
        private double thetaDriverHead = 0.0;
        /**
         * Current alpha value truck is being rendered at for fade in and fade out.
         */
        private float alpha;
        /**
         * Whether the line of data about the current animation is displayed or hidden.
         */
        private boolean dataDisplay = false;
        /**
         * View the scene from the position of the light.
         */
        private static final int LIGHT_VIEW_NONE = 0;
        private static final int LIGHT_VIEW_ORTHO = 1;
        private static final int LIGHT_VIEW_TRAPEZIOD = 2;
        private int lightView = LIGHT_VIEW_NONE;
        /**
         * Offset the truss center needs to be at from center of deck to make insides
         * of thickest members coincide with deck edges.  A pre-computed value.
         */
        private double trussCenterOffset = deckHalfWidth;
        /**
         * Gussets at bridge joints.
         */
        private Gusset[] gussets;
        /**
         * View control: Walk forward back left right.
         */
        private final Overlay walkOverlay = new Overlay();
        /**
         * View control: Tilt head up down left right.
         */
        private final Overlay headTiltOverlay = new Overlay();
        /**
         * View control: Strafe up down left right.
         */
        private final Overlay strafeOverlay = new Overlay();
        /**
         * Home control: Return the view to a default (and then walk).
         */
        private final Overlay homeOverlay = new Overlay();
        /**
         * Drive control: View from the driver's seat with head tilt.
         */
        private final Overlay driveOverlay = new Overlay();
        /**
         * Lists of overlays and their image file names.
         */
        private final Overlay[] overlays = {walkOverlay, strafeOverlay, headTiltOverlay, homeOverlay, driveOverlay};
        private final String[] overlayImageFileNames = {"walk.png", "strafe.png", "headtilt.png", "home.png", "drive.png"};
        /**
         * Overlay that has brightened due to mouseover or null if none.
         */
        private Overlay activeOverlay = null;
        /**
         * Trapezoidal shadow map-supporting frustum for the animation.
         */
        private final Frustum frustum = new Frustum();

        /**
         * Construct an animation canvas.  This is a delegate merely so
         * alternative animations can use a non-OpenGL canvas for drawing.
         *
         * @param bridge bridge model to be animated
         */
        public AnimationCanvas() {
            super(getAnimationCapabilities());
            // setIgnoreRepaint(true);
            this.truckModel = new FlyThruTruckModel();
            this.truckCabModel = new FlyThruTruckCabModel();
            this.wheelModel = new FlyThruWheelModel();
            if (BDApp.isLegacyGraphics()) {
                config.canShowShadows = false;
            }
            // For Mac retina displays.  This is only a request, so it's
            // not guaranteed to work.  TODO: Handle fail.
            setSurfaceScale(new float[] {1, 1});
            addGLEventListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            addKeyListener(this);
        }

        /**
         * Start the animation.  Window should be already visible.
         */
        public void start() {
            if (gussets != null) {
                // Already started.
                return;
            }

            // Build the 3d gussets for the current bridge geometry.
            gussets = Gusset.getGussets(bridge);
            double deckInterferingHalfGussetDepth = 0;
            for (int i = 0; i < gussets.length; i++) {
                if (gussets[i].isInterferingWithLoad()) {
                    deckInterferingHalfGussetDepth = Math.max(deckInterferingHalfGussetDepth, gussets[i].getHalfDepth());
                }
            }
            // Set the needed truss offset.
            trussCenterOffset = deckInterferingHalfGussetDepth + deckHalfWidth + 0.03; // 3cm gap between deck panel and truss

            DesignConditions conditions = bridge.getDesignConditions();

            // Establish view limits.
            yEyeMax = conditions.getOverMargin() + 15.0;
            xEyeMin = -100.0;
            xEyeMax = 100 + conditions.getSpanLength();
            zEyeMin = -100.0;
            zEyeMax = 100.0;

            // Find the thinnest gusset that touches an abutment to set the abutment width.
            final int[] abutmentJointIndices = conditions.getAbutmentJointIndices();
            double leastAbutmentGussetHalfDepth = gussets[abutmentJointIndices[0]].getHalfDepth();
            for (int i = 1; i < abutmentJointIndices.length; i++) {
                leastAbutmentGussetHalfDepth = Math.min(leastAbutmentGussetHalfDepth, gussets[abutmentJointIndices[i]].getHalfDepth());
            }

            // Build the terrain for these site conditions.
            terrain.initializeTerrain(conditions,
                    (float) trussCenterOffset,
                    (float) (trussCenterOffset + leastAbutmentGussetHalfDepth - 0.03));

            // Reset the animation state.
            resetState();

            // Fetch animation view from local storage and use it if it matches the current scenario.
            String tag = conditions.getTag();
            AnimationViewLocalStorable v = AnimationViewLocalStorable.load(animationViewStorage);
            if (v != null && tag.equals(v.getScenarioTag())) {
                logger.log(Level.INFO, "Loaded view for {0} ({1})", new Object[]{tag, v});
                view = v;
            } else {
                logger.log(Level.INFO, "Reset view for {0} ({1})", new Object[]{tag, v});
                resetView();
            }

            // Remember scenario tag we're viewing.  Doing this in stop() is
            // too late because the new bridge may already have been loaded.
            view.setScenarioTag(tag);

            // Don't start in paused condition.
            config.paused = false;

            // Calling start()/stop() for each card flip bringning the GLCanvas
            // to the front manifest a bug in Intel HD Graphics cards. The
            // pause/resume protocal keeps the same timer thread for the life
            // of the canvas.  This works around the bug.
            if (animator == null) {
                animator = new FPSAnimator(this, 40);
                // JOGL 2 frame counter (replaces our FPSTracker).
                animator.setUpdateFPSFrames(30, null);
                animator.resetFPSCounter();
                animator.start();
            }
            else {
                animator.resume();
            }
        }

        /**
         * Stop the animation.
         */
        public void stop() {
            if (gussets != null) {
                animator.pause();
                String tag = bridge.getDesignConditions().getTag();
                logger.log(Level.INFO, "Save view for {0}", tag);
                view.save(animationViewStorage);
                // Allow garbage collector to have the gussets.
                gussets = null;
            }
        }
        
        private final byte[] alphaTestEnabled = new byte[1];

        private double thetaWheels = 0.0;

        /**
         * Draw the scene.
         * @param gl OpenGL object
         * @param interpolation interpolation to draw
         * @param shadows whether we are drawing into the shadow map or not
         */
        private void drawScene(final GL2 gl, final Interpolation interpolation, int pass) {
            terrain.paint(gl, pass == 1);

            int nLoadedJoints = bridge.getDesignConditions().getNLoadedJoints();
            Iterator<Joint> je = bridge.getJoints().iterator();
            double xa = 0;
            double ya = 0;
            double dxa = 1;
            double dya = 0;
            final double deckWidth = 2 * deckHalfWidth;
            final double deckThickness = bridge.getDesignConditions().getDeckThickness();
            final double beamHeight = BridgeView.wearSurfaceHeight - deckThickness - Joint.radiusWorld;
            while (je.hasNext()) {
                Joint joint = je.next();
                int i = joint.getIndex();
                Gusset gusset = gussets[i];
                joint.paint(gl, interpolation.getDisplacement(i), (float) trussCenterOffset, gusset, ptDisplacedJoint);
                double xb = ptDisplacedJoint.x;
                double yb = ptDisplacedJoint.y;
                double dxb = 1;
                double dyb = 0;
                if (0 < i && i < nLoadedJoints) {
                    // Find unit vector in direction of slab.
                    dxb = xb - xa;
                    dyb = yb - ya;
                    double len = Math.sqrt(dxb * dxb + dyb * dyb);
                    dxb /= len;
                    dyb /= len;
                    // Use the vector for first and last beams. Else use average of this and last
                    if (i == 1) {
                        paintDeckBeam(gl, xa, ya, dxb, dyb, dxb, dyb, beamHeight, deckWidth);
                    } else {
                        paintDeckBeam(gl, xa, ya, dxa, dya, dxb, dyb, beamHeight, deckWidth);
                    }
                    if (i == nLoadedJoints - 1) {
                        paintDeckBeam(gl, xb, yb, dxb, dyb, dxb, dyb, beamHeight, deckWidth);
                    }
                    // Tack the overhangs on the first and last deck segments.
                    if (i == 1) {
                        xa -= BridgeView.deckCantilever * dxb;
                        ya -= BridgeView.deckCantilever * dyb;
                        len += BridgeView.deckCantilever;
                    } else if (i == nLoadedJoints - 1) {
                        len += BridgeView.deckCantilever;
                    }
                    paintDeckPanel(gl, xa, ya, dxb, dyb, len, deckWidth, deckThickness);
                }
                xa = xb;
                ya = yb;
                dxa = dxb;
                dya = dyb;
            }

            if (state >= LOAD_MOVING_STATE) {
                Affine.Point ptLoad = interpolation.getPtLoad();
                if (config.showTruck) {
                    
                    // Update the wheels rotation for the new interpolation.
                    thetaWheels += (180.0 / Math.PI) * (1.0 / FlyThruWheelModel.tireRadius) * getDistanceMoved();
                    while (thetaWheels > 360.0) {
                        thetaWheels -= 360.0;
                    }

                    gl.glPushMatrix();
                    gl.glTranslated(ptLoad.x, ptLoad.y, 0.0);
                    gl.glMultMatrixf(interpolation.getLoadRotationMatrix(), 0);

                    alpha = 1f;
                    float leftAlpha = (float) ((loadLocation + loadLocationRunup) / loadFadeInDistance);
                    if (leftAlpha < alpha) {
                        alpha = leftAlpha;
                    }
                    float rightAlpha = (float) ((loadEndDistanceTraveled - loadLocation) / loadFadeInDistance);
                    if (rightAlpha < alpha) {
                        alpha = rightAlpha;
                    }
                    alpha = Math.max(alpha, 0f);

                    // set up for fade-in and -out
                    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                    gl.glEnable(GL2.GL_BLEND);
                    // Hack around shadow mapping and overlays...  Could fix with shader.
                    gl.glGetBooleanv(GL2.GL_ALPHA_TEST, alphaTestEnabled, 0);
                    final boolean disableAlpha = alpha < 1f && pass > 1 && alphaTestEnabled[0] != 0;
                    if (disableAlpha) {
                        gl.glDisable(GL2.GL_ALPHA_TEST);
                    }
                    if (driving()) {
                        truckCabModel.display(gl);
                    } else {
                        truckModel.setAlpha(alpha);
                        wheelModel.setAlpha(alpha);

                        // right front
                        gl.glPushMatrix();
                        gl.glTranslated(0f, 0.5f, 0.95f);
                        gl.glRotated(thetaWheels, 0, 0, -1);
                        wheelModel.displaySingle(gl);
                        gl.glPopMatrix();

                        // right rear
                        gl.glPushMatrix();
                        gl.glTranslated(-DesignConditions.panelSizeWorld, 0.5f, 0.95f);
                        gl.glRotated(thetaWheels, 0, 0, -1);
                        wheelModel.displayDual(gl);
                        gl.glPopMatrix();

                        // left front
                        gl.glPushMatrix();
                        gl.glTranslated(0f, 0.5f, -0.95f);
                        gl.glRotatef(180.0f, 1f, 0f, 0f);
                        gl.glRotated(thetaWheels, 0, 0, 1);
                        wheelModel.displaySingle(gl);
                        gl.glPopMatrix();

                        // left rear
                        gl.glPushMatrix();
                        gl.glTranslated(-DesignConditions.panelSizeWorld, 0.5f, -0.95f);
                        gl.glRotatef(180.0f, 1f, 0f, 0f);
                        gl.glRotated(thetaWheels, 0, 0, 1);
                        wheelModel.displayDual(gl);
                        gl.glPopMatrix();

                        truckModel.display(gl);
                        if (disableAlpha) {
                            gl.glEnable(GL2.GL_ALPHA_TEST);
                        }
                        gl.glDisable(GL2.GL_BLEND);
                    }
                    gl.glPopMatrix();
                }
            } 

            Iterator<Member> me = bridge.getMembers().iterator();
            while (me.hasNext()) {
                Member m = me.next();
                int im = m.getIndex();
                m.paint(gl,
                        interpolation.getDisplacement(m.getJointA().getIndex()),
                        interpolation.getDisplacement(m.getJointB().getIndex()),
                        trussCenterOffset,
                        interpolation.getForceRatio(im),
                        config.showForcesAsColors,
                        interpolation.getMemberStatus(im));
            }
        }

        private void resetView() {
            Rectangle2D extent = bridge.getExtent(null);
            double width = extent.getWidth();
            double height = extent.getHeight();
            view.zEye = 1.2 * Math.max(width, 1.75 * height);
            view.xEye = xLookAt = extent.getMinX() + 0.5 * width;
            // Direct gaze at middle of vertical extent.
            yLookAt = extent.getMinY() + 0.5 * height;
            // Always put eye at height of a person on the road.
            view.yEye = 1;
            // Move eye right a bit to account for slant of river.
            view.xEye -= view.zEye * 0.1;
            zLookAt = 0.0;
            yEyeVelocity = 0;

            // The angles are actually the independent values for lookAt, so compute them here.
            view.thetaEye = Math.atan2(xLookAt - view.xEye, view.zEye - zLookAt);
            thetaEyeRate = 0;

            view.phiEye = Math.atan2(yLookAt - view.yEye, view.zEye - zLookAt);
            phiEyeRate = 0;
            lightView = LIGHT_VIEW_NONE;
        }

        public void init(GLAutoDrawable glDrawable) {

            // For debugging openGL problems; prints error info to console:
            // glDrawable.setGL(new DebugGL2(glDrawable.getGL().getGL2()));
            glu = new GLU();
            glut = new GLUT();

            GL2 gl = glDrawable.getGL().getGL2();

            // If we don't have 2 texture units, we can't render shadows.
            if (config.canShowShadows && getInteger(gl, GL2.GL_MAX_TEXTURE_UNITS) < 2) {
                logger.log(Level.INFO, "No shadows possible with {0} TUs", getInteger(gl, GL2.GL_MAX_TEXTURE_UNITS));
                config.canShowShadows = false;
            }

            // Set up the terrain textures.
            terrain.initializeTerrainTextures(gl);

            // Set up the overlays.
            for (int i = 0; i < overlays.length; i++) {
                overlays[i].initialize(gl, overlayImageFileNames[i]);
            }

            // JOGL docs say GLContext was destroyed, so we must mirror here.  Verified by testing on
            // NVIDIA Quadro FX 360M
            if (config.canShowShadows) {
                shadowMapTexture = null;
                shadowFrameBuffer = -1;
                tryShadowFrameBufferAllocation(gl);
            }

            // Load identity modelview
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();

            // Shading states
            gl.glShadeModel(GL2.GL_SMOOTH);
            gl.glClearColor(0.0f, 0.75f, 1.0f, 1.0f); // cyan
            gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

            // Fog
            gl.glEnable(GL2.GL_FOG);
            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
            gl.glFogfv(GL2.GL_FOG_COLOR, fogColor, 0);
            gl.glFogf(GL2.GL_FOG_DENSITY, 3.8e-3f);
            gl.glHint(GL2.GL_FOG_HINT, GL2.GL_DONT_CARE);

            // Depth states
            gl.glClearDepth(1.0f);
            gl.glDepthFunc(GL2.GL_LEQUAL);

            gl.glEnable(GL2.GL_CULL_FACE);

            // We use glScale when drawing the scene
            gl.glEnable(GL2.GL_NORMALIZE);

            // Calculate & save matrices
            gl.glPushMatrix();

            // Light view
            gl.glLoadIdentity();
            glu.gluLookAt(0f, 0f, 0f,
                    -lightPosition.x(), -lightPosition.y(), -lightPosition.z(),
                    0f, 1f, 0f);
            gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, lightViewMatrix.a, 0);

            // Let material color tract glColor.
            gl.glEnable(GL2.GL_COLOR_MATERIAL);

            // Projection used to draw TSM view.
            gl.glLoadIdentity();
            gl.glOrtho(-150f, 150f, -150f, 150f, -150, 150f);
            gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, lightProjectionMatrix.a, 0);
            gl.glPopMatrix();
        }

        
        public void display(GLAutoDrawable glDrawable) {
            // If no gussets, this is a frame after the stop() call.  Ignore it.
            if (gussets == null) {
                return;
            }

            GL2 gl = glDrawable.getGL().getGL2();
            
            long time = System.nanoTime();

            // Update the animation state based on elapsed time.  Get an interpolation of bridge analyses that
            // describes current loading condition.
            final Analysis.Interpolation interpolation = interpolate(time);

            if (config.canShowShadows && config.showShadows) {

                // Build depth buffer.

                gl.glEnable(GL2.GL_DEPTH_TEST);
                // Some cards seem to clear this back to LT when disabled, so set here.
                gl.glDepthFunc(GL2.GL_LEQUAL);
                if (config.showSky) {
                    // No need to clear color buffer if sky is being drawn.
                    if (!isShadowFrameBuffer()) {
                        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
                    }
                } else {
                    gl.glClear(isShadowFrameBuffer() ? GL2.GL_COLOR_BUFFER_BIT : (GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT));
                }

                // Set up the view frustum and use it to obtain the trapezoidal projection.
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                setLookAtMatrix(gl, interpolation);
                gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, eyeViewMatrix.a, 0);
                frustum.getTrapezoidalProjection(
                        trapezoidalProjectionMatrix,
                        eyeViewMatrix, lightViewMatrix, -750f, 750f);
                
                // Set either offset or basic projection matrix for drawing depth map.
                /*
                if (offsetShadows) {
                    offsetTrapezoidalProjectionMatrix.set(trapezoidalProjectionMatrix);
                    float [] a = offsetTrapezoidalProjectionMatrix.a;
                    float eps = .0018f;
                    a[2] += eps * a[3];
                    a[6] += eps * a[7];
                    a[14] += eps * a[15];
                    gl.glMatrixMode(GL2.GL_PROJECTION);
                    gl.glLoadMatrixf(offsetTrapezoidalProjectionMatrix.a, 0);
                }
                else {
                    gl.glMatrixMode(GL2.GL_PROJECTION);
                    gl.glLoadMatrixf(trapezoidalProjectionMatrix.a, 0);
                }
                 */
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadMatrixf(trapezoidalProjectionMatrix.a, 0);
                
                // Now draw from the light's point of view with trapezoidal projection.
                gl.glMatrixMode(GL2.GL_PROJECTION);
//              gl.glLoadMatrixf(offsetShadows ? offsetTrapezoidalProjectionMatrix.a : trapezoidalProjectionMatrix.a, 0);
                gl.glLoadMatrixf(trapezoidalProjectionMatrix.a, 0);
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glLoadMatrixf(lightViewMatrix.a, 0);

                // Use viewport the same size as the shadow map
                gl.glViewport(0, 0, shadowMapWidth, shadowMapHeight);

                // Draw back faces into the shadow map
                gl.glCullFace(GL2.GL_FRONT);

                // Disable color writes, and use flat shading for speed
                gl.glColorMask(false, false, false, false);

                // Draw the scene with details decided by availableity of FBOs.
                if (isShadowFrameBuffer()) {
                    // Draw a large depth mask straight to the texture through the FBO.
                    gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, shadowFrameBuffer);
                    gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
                    drawScene(gl, interpolation, 1);
                    gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
                } else {
                    // Draw onto the usual back buffer.
                    drawScene(gl, interpolation, 1);
                    // Read the depth buffer into the shadow map texture
                    gl.glActiveTexture(GL2.GL_TEXTURE1);
                    shadowMapTexture.bind(gl);
                    gl.glCopyTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, 0, 0, shadowMapWidth, shadowMapHeight);
                }

                // Restore states
                gl.glCullFace(GL2.GL_BACK);
                gl.glShadeModel(GL2.GL_SMOOTH);
                gl.glColorMask(true, true, true, true);
                gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);

                // 2nd pass - Draw from camera's point of view
                if (lightView > LIGHT_VIEW_NONE) {
                    gl.glDisable(GL2.GL_FOG);
                    gl.glMatrixMode(GL2.GL_PROJECTION);
                    gl.glLoadMatrixf(lightView == LIGHT_VIEW_TRAPEZIOD
                            ? trapezoidalProjectionMatrix.a : lightProjectionMatrix.a, 0);

                    gl.glMatrixMode(GL2.GL_MODELVIEW);
                    gl.glLoadMatrixf(lightViewMatrix.a, 0);
                } else {
                    gl.glEnable(GL2.GL_FOG);
                    gl.glMatrixMode(GL2.GL_PROJECTION);
                    gl.glLoadMatrixf(eyeProjectionMatrix.a, 0);

                    gl.glMatrixMode(GL2.GL_MODELVIEW);
                    gl.glLoadMatrixf(eyeViewMatrix.a, 0);
                }
                gl.glViewport(windowX, windowY, windowWidth, windowHeight);

                // Use ambient light only for shadows. It's important to use
                // exactly this value so you can't see acne on light space
                // backfaces.
                gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, black, 0);
                gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, config.lightBrightness, 0);
                gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition.a, 0);
                gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, black, 0);
                gl.glEnable(GL2.GL_LIGHT0);
                gl.glEnable(GL2.GL_LIGHTING);

                drawScene(gl, interpolation, 2);

                // 3rd pass
                // Draw with bright light
                gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, white, 0);

                // Build the texture matrix.
                gl.glPushMatrix();
                gl.glLoadMatrixf(biasMatrix.a, 0);
                gl.glMultMatrixf(trapezoidalProjectionMatrix.a, 0);
                gl.glMultMatrixf(lightViewMatrix.a, 0);
                gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, textureMatrix.a, 0);
                textureMatrix.transposeInPlace();
                gl.glPopMatrix();

                // Set up texture coordinate generation in texture unit 1.
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

                // Bind & enable shadow map texture
                shadowMapTexture.bind(gl);
                shadowMapTexture.enable(gl);

                // Enable shadow comparison
                shadowMapTexture.setTexParameteri(gl, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);

                // Shadow comparison should be true (ie not in shadow) if r<=texture
                shadowMapTexture.setTexParameteri(gl, GL2.GL_TEXTURE_COMPARE_FUNC, GL2.GL_LEQUAL);

                // Shadow comparison should generate an INTENSITY result
                shadowMapTexture.setTexParameteri(gl, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_INTENSITY);

                // Set alpha test to discard intensities less than 1 (the false comparisons)
                gl.glAlphaFunc(GL2.GL_GEQUAL, 1f);
                gl.glEnable(GL2.GL_ALPHA_TEST);

                drawScene(gl, interpolation, 3);

                // Disable depth texture and texgen
                gl.glActiveTexture(GL2.GL_TEXTURE1);
                shadowMapTexture.disable(gl);
                gl.glDisable(GL2.GL_TEXTURE_GEN_S);
                gl.glDisable(GL2.GL_TEXTURE_GEN_T);
                gl.glDisable(GL2.GL_TEXTURE_GEN_R);
                gl.glDisable(GL2.GL_TEXTURE_GEN_Q);

                // Restore other states
                gl.glDisable(GL2.GL_ALPHA_TEST);
                gl.glDisable(GL2.GL_LIGHTING);
                if (lightView == LIGHT_VIEW_ORTHO) {
                    frustum.draw(gl, lightViewMatrix, lightProjectionMatrix);
                }
            } else { // Simple, no shadows pipeline.
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
                gl.glEnable(GL2.GL_DEPTH_TEST);
                // Some cards seem to clear this back to LT when disabled.
                gl.glDepthFunc(GL2.GL_LEQUAL);

                gl.glEnable(GL2.GL_FOG);
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadMatrixf(eyeProjectionMatrix.a, 0);

                gl.glMatrixMode(GL2.GL_MODELVIEW);
                setLookAtMatrix(gl, interpolation);

                gl.glViewport(windowX, windowY, windowWidth, windowHeight);

                gl.glShadeModel(GL2.GL_SMOOTH);
                gl.glEnable(GL2.GL_LIGHTING);
                gl.glEnable(GL2.GL_LIGHT0);
                gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition.a, 0);
                gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, white, 0);
                gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, config.lightBrightness, 0);

                drawScene(gl, interpolation, 4);
            }
            // Prepare for mouse coordinate drawing.
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadMatrixf(mouseProjectionMatrix.a, 0);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();

            // Draw the march vector.
            gl.glBegin(GL2.GL_LINES);
            gl.glColor3fv(white, 0);
            gl.glVertex2d(x0Mouse, y0Mouse);
            gl.glVertex2d(x1Mouse, y1Mouse);
            gl.glEnd();

            for (int i = 0; i < overlays.length; i++) {
                overlays[i].display(gl);
            }

            if (dataDisplay) {
                // Frames per second indicator.
                String msg = String.format("%.1f fps eye=(%.1f,%.1f,%.1f) look=(%.1f,%.1f,%.1f) yTerrain=%.1f",
                        animator.getLastFPS(), view.xEye, view.yEye, view.zEye, xLookAt, yLookAt, zLookAt, yEyeMin);
                gl.glRasterPos2i(4, windowHeight - 4);
                glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, msg);
            }
        }

        private void setLookAtMatrix(GL2 gl, final Interpolation interpolation) {
            gl.glLoadIdentity();
            if (driving()) {
                Affine.Point eye = interpolation.getPtLoad();
                Affine.Vector look = interpolation.getLoadRotation();
                gl.glRotated(phiDriverHead, -1, 0, 0);
                gl.glRotated(thetaDriverHead, 0, 1, 0);
                glu.gluLookAt(eye.x + driverEyeLead, eye.y + driverEyeHeight, 0, eye.x + look.x, eye.y + look.y + driverEyeHeight, 0, 0.0, 1.0, 0.0);
            } else {
                glu.gluLookAt(view.xEye, view.yEye, view.zEye, xLookAt, yLookAt, zLookAt, 0.0, 1.0, 0.0);
            }
            // Record the model-view matrix so we can draw the frustum in light view mode.
        }
        private static final int controlMargin = 200;
        private static final int controlSeparation = 64;

        /**
         * Handle reshaping of the animation window.
         *
         * @param glDrawable OpenGL drawable object (this canvas).
         * @param x new viewport x-coordinate of upper left corner
         * @param y new viewport y-coordinate of upper left corner
         * @param width new viewport width
         * @param height new viewport height
         */
        public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
            GL2 gl = glDrawable.getGL().getGL2();

            if (x == windowX && y == windowY && width == windowWidth && height == windowHeight) {
                logger.log(Level.INFO, "Redundant reshape.");
            }
            
            // Save new viewport
            windowX = x;
            windowY = y;
            windowWidth = width;
            windowHeight = height;

            // Reposition the overlays
            for (int i = 0; i < overlays.length; i++) {
                overlays[i].setPosition(controlMargin, controlMargin + (i + 1) * controlSeparation);
            }

            // Recalculate the eye and mouse projections.
            gl.glPushMatrix();

            frustum.set(45.0f, (float) width / height, .333333f, 400f, 0.5f);
            gl.glLoadIdentity();
            frustum.apply(gl);
            gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, eyeProjectionMatrix.a, 0);

            gl.glLoadIdentity();
            glu.gluOrtho2D(0.0, width - 1, height - 1, 0.0);
            gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mouseProjectionMatrix.a, 0);

            gl.glPopMatrix();

            // If no frame buffer, we're using back buffer, so must ensure shadow mask is allocated no larger than window.
            if (config.canShowShadows && !isShadowFrameBuffer()) {
                logger.log(Level.INFO, "No frame buffer: shadows using back buffer.");
                reallocateShadowMapTexture(gl, powerOf2AtMost(width), powerOf2AtMost(height), false);
            }
            applyCanvasResizeBugWorkaround();
            logger.log(Level.INFO, "Reshape {0} to [{1},{2};{3},{4}" + "]" + "min=" + "{5}",
                    new Object[]{glDrawable, x, y, width, height, getMinimumSize()});
        }

        public void displayChanged(GLAutoDrawable glDrawable, boolean arg1, boolean arg2) {
        }

        public void mouseClicked(MouseEvent e) {
        }

        private boolean moveLateral() {
            return (activeOverlay == strafeOverlay);
        }

        private boolean lookUpDown() {
            return (activeOverlay == headTiltOverlay);
        }

        private boolean walking() {
            return (activeOverlay == walkOverlay);
        }

        private boolean home() {
            return (activeOverlay == homeOverlay);
        }

        private boolean driving() {
            return (activeOverlay == driveOverlay);
        }
        /**
         * Prototypical cube to stretch and rotate to form 3D deck beam.
         */
        private final float[] tubeVerticesLeft = {
            -.5f, 1.0f, +.5f,
            -.5f, 1.0f, -.5f,
            -.5f, 0.0f, -.5f,
            -.5f, 0.0f, +.5f,
            -.5f, 1.0f, +.5f,};
        private final float[] tubeVerticesRight = {
            +.5f, 1.0f, +.5f,
            +.5f, 1.0f, -.5f,
            +.5f, 0.0f, -.5f,
            +.5f, 0.0f, +.5f,
            +.5f, 1.0f, +.5f,};
        private final float[] tubeNormals = {
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 1.0f,};

        /**
         * Paint 3D representation of a deck beam.  Rotate to average of two vectors.
         *
         * @param gl OpenGL object
         * @param x x-coordinate of vertex at base of deck beam
         * @param y u-coordinate of vertex at base of deck beam
         * @param uxa x-coordinate of unit vector in direction of deck joint to left of this one
         * @param uya y-coordinate of unit vector in direction of deck joint to left of this one
         * @param uxb x-coordinate of unit vector in direction of deck joint to right of this one
         * @param uyb y-coordinate of unit vector in direction of deck joint to right of this one
         * @param height height of deck beam
         * @param length length of deck beam
         */
        private void paintDeckBeam(GL2 gl, double x, double y, double uxa, double uya, double uxb, double uyb, double height, double length) {
            gl.glPushMatrix();
            gl.glTranslated(x, y, 0);
            // Average the two deck vectors to get the beam rotation.
            double dx = 0.5 * (uxa + uxb);
            double dy = 0.5 * (uya + uyb);
            double len = Math.sqrt(dx * dx + dy * dy);
            gl.glMultMatrixf(Utility.rotateAboutZ(dx / len, dy / len), 0);
            gl.glTranslated(0, Joint.radiusWorld, 0);
            gl.glScaled(deckBeamWidth, height, length);
            gl.glColor3fv(beamMaterial, 0);
            gl.glBegin(GL2.GL_QUADS);
            for (int i = 0; i < tubeNormals.length; i += 3) {
                gl.glNormal3fv(tubeNormals, i);
                gl.glVertex3fv(tubeVerticesLeft, i + 0);
                gl.glVertex3fv(tubeVerticesRight, i + 0);
                gl.glVertex3fv(tubeVerticesRight, i + 3);
                gl.glVertex3fv(tubeVerticesLeft, i + 3);
            }
            gl.glNormal3f(-1f, 0, 0);
            for (int i = 0; i < 4 * 3; i += 3) {
                gl.glVertex3fv(tubeVerticesLeft, i);
            }
            gl.glNormal3f(1f, 0, 0);
            for (int i = (4 - 1) * 3; i >= 0; i -= 3) {
                gl.glVertex3fv(tubeVerticesRight, i);
            }
            gl.glEnd();
            gl.glPopMatrix();
        }

        /**
         * Paint one deck panel.
         *
         * @param gl OpenGL object
         * @param x x-coordinate of left edge of deck panel
         * @param y y-coordinate of left edge of deck panel
         * @param ux x-coordinate of unit vector pointing in direction of deck panel
         * @param uy y-coordinate of unit vector pointing in direction of deck panel
         * @param length length of deck panel
         * @param width width of one deck panel
         * @param thickness thickness of deck panel
         */
        private void paintDeckPanel(GL2 gl, double x, double y, double ux, double uy, double length, double width, double thickness) {
            gl.glPushMatrix();
            gl.glTranslated(x, y + BridgeView.wearSurfaceHeight, 0.0);
            gl.glMultMatrixf(Utility.rotateAboutZ(ux, uy), 0);
            gl.glScaled(length, thickness, width);
            gl.glShadeModel(GL2.GL_FLAT);
            gl.glColor3fv(deckColor, 0);
            gl.glBegin(GL2.GL_QUADS);
            for (int i = 0; i < deckNormals.length; i += 3) {
                gl.glNormal3fv(deckNormals, i);
                gl.glVertex3fv(deckVerticesLeft, i + 0);
                gl.glVertex3fv(deckVerticesRight, i + 0);
                gl.glVertex3fv(deckVerticesRight, i + 3);
                gl.glVertex3fv(deckVerticesLeft, i + 3);
            }
            gl.glEnd();
            gl.glPopMatrix();
        }

        /**
         * Handle a mouse press event.
         *
         * @param e mouse event
         */
        public void mousePressed(MouseEvent e) {
            for (int i = 0; i < overlays.length; i++) {
                if (overlays[i].inside(e.getX(), e.getY())) {
                    activeOverlay = overlays[i];
                    x0Mouse = x1Mouse = activeOverlay.getX();
                    y0Mouse = y1Mouse = activeOverlay.getY();
                    if (home()) {
                        resetView();
                    }
                    display();
                    return;
                }
            }
            activeOverlay = null;
        }

        /**
         * Handle a mouse drag event.
         *
         * @param e mouse event
         */
        public void mouseDragged(MouseEvent e) {
            if (activeOverlay != null) {
                x1Mouse = e.getX();
                y1Mouse = e.getY();
                final int dx = x1Mouse - x0Mouse;
                final int dy = y0Mouse - y1Mouse;
                if (moveLateral()) {
                    xzEyeVelocity = dx * linearUIRate;
                    yEyeVelocity = dy * linearUIRate;
                } else if (lookUpDown()) {
                    phiEyeRate = dy * rotationalUIRate;
                    thetaEyeRate = dx * rotationalUIRate;
                } else if (walking()) {
                    xzEyeVelocity = dy * linearUIRate;
                    thetaEyeRate = dx * rotationalUIRate;
                } else if (home()) {
                    xzEyeVelocity = dy * linearUIRate;
                    thetaEyeRate = dx * rotationalUIRate;
                } else if (driving()) {
                    phiDriverHead = Utility.clamp(dy * tiltUIRate, -45, 20);
                    thetaDriverHead = Utility.clamp(1.5 * dx * tiltUIRate, -100, 100);
                }
            }
        }

        /**
         * Handle a mouse release event.
         *
         * @param e mouse event
         */
        public void mouseReleased(MouseEvent e) {
            if (activeOverlay != null) {
                x0Mouse = x1Mouse;
                y0Mouse = y1Mouse;
                activeOverlay = null;
                thetaEyeRate = phiEyeRate = xzEyeVelocity = yEyeVelocity = phiDriverHead = thetaDriverHead = 0;
                display();
            }
        }

        /**
         * Handle a mouse entered the window event.
         *
         * @param e mouse event
         */
        public void mouseEntered(MouseEvent e) {
            mouseMoved(e);
        }

        /**
         * Handle a mouse exited the window event.
         *
         * @param e mouse event
         */
        public void mouseExited(MouseEvent e) {
            mouseMoved(e);
        }

        /**
         * Handle a mouse movement event.
         *
         * @param e mouse event.
         */
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            for (int i = 0; i < overlays.length; i++) {
                overlays[i].mouseMoved(x, y);
            }
        }

        public void keyTyped(KeyEvent e) {
            switch (e.getKeyChar()) {
                case 'd':
                case 'D':
                    dataDisplay = !dataDisplay;
                    break;
                case 'l':
                case 'L':
                    lightView = (lightView + 1) % 3;
                    break;
                case 'r':
                case 'R':
                    resetView();
                    break;
                /*
                case 'o':
                case 'O':
                    offsetShadows = !offsetShadows;
                    break;
                */
                case 'i':
                case 'I':
                    ignoreBoundaries = !ignoreBoundaries;
                    break;
            }
        }

        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    break;
                case KeyEvent.VK_DOWN:
                    break;
                case KeyEvent.VK_LEFT:
                    break;
                case KeyEvent.VK_RIGHT:
                    break;
            }
        }

        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    break;
                case KeyEvent.VK_DOWN:
                    break;
                case KeyEvent.VK_LEFT:
                    break;
                case KeyEvent.VK_RIGHT:
                    break;
            }
        }

        /**
         * Allocate or re-allocate a shadow map with as much flexibility for different kinds of hardware as possible.
         *
         * @param gl OpenGL object
         * @param width width of shadow map
         * @param height height of shadow map
         * @param bindFBO bind the FBO to the shadow map if possible
         * @throws com.jogamp.opengl.GLException
         */
        private void reallocateShadowMapTexture(GL2 gl, int width, int height, boolean bindFBO) throws GLException {

            if (shadowMapTexture != null) {
                if (width == shadowMapWidth && height == shadowMapHeight) {
                    return;
                }
                shadowMapTexture.destroy(gl);
                shadowMapTexture = null;
            }

            logger.log(Level.INFO, "Reallocating shadow map texture...");

            // Create the shadow map texture in unit 1.
            gl.glActiveTexture(GL2.GL_TEXTURE1);
            for (;;) {
                shadowMapTexture = TextureIO.newTexture(GL2.GL_TEXTURE_2D);
                shadowMapTexture.bind(gl);

                // Attempt texture allocation.
                gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT, width, height, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_UNSIGNED_BYTE, null);
                int actualWidth = getTexLevelParameteri(gl, GL2.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_WIDTH);
                int actualHeight = getTexLevelParameteri(gl, GL2.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_HEIGHT);
                logger.log(Level.INFO, "... @ {0} x {1}, got {2} x {3}",
                        new Object[]{width, height, actualWidth, actualHeight});
                if (actualWidth > 0) {
                    shadowMapTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
                    shadowMapTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    // Clamp to border so border color decides shadow behavior for objects outside the map.
                    shadowMapTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
                    shadowMapTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
                    // Set the shadow map's border to infinite depth so fragments outside the shadow map appear unshadowed.
                    // Only the initial 1 in the color is used because this is a depth texture.
                    shadowMapTexture.setTexParameterfv(gl, GL2.GL_TEXTURE_BORDER_COLOR, new float[]{1f, 1f, 1f, 1f}, 0);

                    if (bindFBO) {
                        if (isShadowFrameBuffer()) {
                            deleteFramebuffer(gl, shadowFrameBuffer);
                            shadowFrameBuffer = -1;
                        }
                        shadowFrameBuffer = genFramebuffer(gl);
                        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, shadowFrameBuffer);
                        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_TEXTURE_2D, shadowMapTexture.getTextureObject(gl), 0);
                        // Turn off drawing planes.
                        gl.glDrawBuffer(GL2.GL_NONE);
                        gl.glReadBuffer(GL2.GL_NONE);
                        int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
                        if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
                            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
                            logger.log(Level.INFO, "Success binding frame buffer");
                            break;
                        }
                        logger.log(Level.WARNING, "Frame buffer status incomplete @ {0}",
                                Integer.toHexString(status));
                        deleteFramebuffer(gl, shadowFrameBuffer);
                        shadowFrameBuffer = -1;
                    }
                } else {
                    shadowMapTexture.destroy(gl);
                }
                width /= 2;
                height /= 2;
                if (width <= 1024 || height <= 512) {
                    width = height = 0;
                    config.canShowShadows = false;
                    shadowMapTexture = null;
                    shadowFrameBuffer = -1;
                    break;
                }
            }
            // Allocate buffer to hold the depth values to be rendered as luminosity (optionally).
            shadowMapWidth = width;
            shadowMapHeight = height;
        }

        /**
         * These may or may not be needed. GL2 documentation isn't clear.
         * But I don't think they can do any harm.
         *
         * @param drawable OpenGL drawable object
         */
        public void dispose(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            if (shadowMapTexture != null) {
                shadowMapTexture.destroy(gl);
                shadowMapTexture = null;
            }
            if (shadowFrameBuffer != -1) {
                deleteFramebuffer(gl, shadowFrameBuffer);
                shadowFrameBuffer = -1;
            }
            if (animator != null) {
                animator.stop();
            }
            // How about textures?
        }

        /**
         * Return true iff a shadow frame buffer object has been allocated.
         *
         * @return indicator of previous frame buffer allocation
         */
        private boolean isShadowFrameBuffer() {
            return shadowFrameBuffer >= 0;
        }

        /**
         * Try to allocate a shadow frame buffer object allocation.
         *
         * @param gl OpenGL object
         * @throws com.jogamp.opengl.GLException
         */
        private void tryShadowFrameBufferAllocation(GL2 gl) throws GLException {

            // Set up a frame buffer for the shadow texture if the capability exists.
            if (gl.isExtensionAvailable("GL_EXT_framebuffer_object") && gl.isFunctionAvailable("glFramebufferTexture2DEXT")) {
                logger.log(Level.INFO, "Frame buffer is available");
                reallocateShadowMapTexture(gl, maxShadowTextureSize, maxShadowTextureSize, true);
                logger.log(Level.INFO, "Frame buffer allocation complete");
            }
        }

        /**
         * Update the view using independent parameters to set dependent ones assuming a given amount of time has
         * elapsed.
         *
         * @param elapsed elapsed time
         */
        public void updateView(double elapsed) {
            view.phiEye = Utility.clamp(view.phiEye + phiEyeRate * elapsed, -maxTilt, maxTilt);
            double dy = Math.sin(view.phiEye);
            double c = Math.cos(view.phiEye);
            view.thetaEye = Math.IEEEremainder(view.thetaEye + thetaEyeRate * elapsed, 2 * Math.PI);
            double dx = +c * Math.sin(view.thetaEye);
            double dz = -c * Math.cos(view.thetaEye);
            if (ignoreBoundaries) {
                if (moveLateral()) {
                    view.xEye = view.xEye - dz * xzEyeVelocity * elapsed;
                    view.zEye = view.zEye + dx * xzEyeVelocity * elapsed;
                } else { // Rotate
                    view.xEye = view.xEye + dx * xzEyeVelocity * elapsed;
                    view.zEye = view.zEye + dz * xzEyeVelocity * elapsed;
                }
                view.yEye = view.yEye + yEyeVelocity * elapsed;
            }
            else {
                if (moveLateral()) {
                    view.xEye = Utility.clamp(view.xEye - dz * xzEyeVelocity * elapsed, xEyeMin, xEyeMax);
                    view.zEye = Utility.clamp(view.zEye + dx * xzEyeVelocity * elapsed, zEyeMin, zEyeMax);
                } else { // Rotate
                    view.xEye = Utility.clamp(view.xEye + dx * xzEyeVelocity * elapsed, xEyeMin, xEyeMax);
                    view.zEye = Utility.clamp(view.zEye + dz * xzEyeVelocity * elapsed, zEyeMin, zEyeMax);
                }
                yEyeMin = terrain.getElevationAt((float) view.xEye, (float) view.zEye) + 2.0f;
                view.yEye = Utility.clamp(view.yEye + yEyeVelocity * elapsed, yEyeMin, yEyeMax);
            }
            xLookAt = view.xEye + dx;
            zLookAt = view.zEye + dz;
            yLookAt = view.yEye + dy;
        }
    }

    /**
     * Delegate base class view update request from the state machine to the canvas.
     *
     * @param elapsed
     */
    public void updateView(double elapsed) {
        canvas.updateView(elapsed);
    }
}
