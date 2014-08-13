/*
 * Animation.java
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
import java.awt.Component;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import jogamp.common.Debug;

abstract public class Animation {

    /**
     * Set up a logger for client debugging.
     */
    protected static final Logger logger;

    static {
        logger = Logger.getLogger(Animation.class.getName());
        logger.setLevel(Debug.isPropertyDefined("wpbd.develop", false) ? Level.ALL : Level.OFF);
    }

    /**
     * The bridge we're animating.
     */
    protected EditableBridgeModel bridge;

    /**
     * Homogeneous position vector denoting the sun.
     */
    public static final Homogeneous.Point lightPosition = new Homogeneous.Point(15f, 180f, 190f, 0f);
    /**
     * Half-width of deck in animation in meters.
     * Exported for terrain modeler so that cut cross-section and road
     * can match deck width (Terrain).
     */
    public static final double deckHalfWidth = 5.0;
    /**
     * Factor multiplied by deflections when exaggeration is turned on.
     * Exported for animation options dialog (WPBDView).
     */
    public static final double standardExaggeration = 30.0;
    /**
     * Default load speed in kilometers per hour.
     */
    private static final double defaultTruckSpeedKmPerHr = 25.0;
    /**
     * Seconds to animate the bridge failure.
     */
    private static final double failureDuration = 1.2;

    public static class Config {
        public double displacementExaggeration = standardExaggeration;
        public double truckSpeed = defaultTruckSpeedKmPerHr;
        public boolean paused = false;
    }

    private final Config config;

    public Animation(EditableBridgeModel bridge, TerrainModel terrain, Config config) {
        this.bridge = bridge;
        this.config = config;
        failureAnalysis = new Analysis();
        animationInterpolation = bridge.getAnalysis().getNewInterpolation(terrain);
        failureInterpolation = failureAnalysis.getNewInterpolation(terrain);
        failureAnimationInterpolation = bridge.getAnalysis().getNewInterpolation(terrain);
    }
 
    abstract public Canvas getCanvas();

    abstract public AnimationControls getControls();

    abstract public void updateView(double elapsed);

    abstract public void start();

    abstract public void stop();

    public void reset() {
        resetState();
    }

    /**
     * FlyThruAnimation controller FA state: animation is still uninitialized.
     */
    protected static final int UNINITIALIZED_STATE = 0;
    /**
     * FlyThruAnimation controller FA state: animation started; pausing before loading starts.
     */
    protected static final int UNLOADED_STATE = 1;
    /**
     * FlyThruAnimation controller FA state: dead loading is being applied.
     */
    protected static final int DEAD_LOADING_STATE = 2;
    /**
     * FlyThruAnimation controller FA state: load is moving over the bridge.
     */
    protected static final int LOAD_MOVING_STATE = 3;
    /**
     * FlyThruAnimation controller FA state: bridge is already failing.
     */
    protected static final int FAILING_STATE = 4;
    /**
     * FlyThruAnimation controller FA state: bridge failure is complete.
     */
    protected static final int FAILED_STATE = 5;
    /**
     * Load is moving up to failed bridge and must stop before traversing it.
     */
    protected static final int LOAD_MOVING_TO_DEAD_LOAD_FAILURE = 6;
    /**
     * Conversion factor taking km/hr to panels/second, which is
     * used as the internal unit of speed.
     */
    protected static final double kmPerHrToPanelsPerSec =
            (1000.0 / 1.0) /* m / km */ * (1.0 / DesignConditions.panelSizeWorld) /* panels / m */ * (1.0 / (60.0 * 60.0)) /* hr / sec */;
    /**
     * Number of panel lengths from starting front wheel location to bridge deck.
     * Also distance from back wheel location where truck disappears.
     */
    protected double loadLocationRunup = 3.0;
    /**
     * Time scene is portrayed at animation start before dead load application starts.
     */
    protected static final double initialPauseDuration = 0.5;
    /**
     * Seconds taken to apply the dead load at animation start.
     */
    protected static final double deadLoadingDuration = 1.2;
    /**
     * Place for load to stop if bridge failed under dead load only.
     */
    protected static final double emergencyStopLocation = -0.5;
    /**
     * Current state of the animation.
     */
    protected int state = UNINITIALIZED_STATE;
    /**
     * Fraction of dead load applied thus far.
     */
    protected double deadLoadApplied = 0.0;
    /**
     * Current lateral load location. Zero is leftmost deck joint.  Units are panels.
     */
    protected double loadLocation = -loadLocationRunup;
    /**
     * Distance traveled past rightmost deck joint. Units are panels.
     */
    protected double loadEndDistanceTraveled = loadLocationRunup;
    /**
     * The last time hack when the animation display started.
     */
    protected long lastDisplayTime = -1;
    /**
     * Last time hack when the FA that controls the animation changed its state.
     */
    protected long lastStateChangeTime = -1;
    /**
     * Linear x-y distance traveled by the truck accumulated since
     * start or previous call to getDistanceMoved.
     */
    private double distanceMoved = 0;
    /**
     * Interpolation object used to store current interpolation between two load
     * cases, including joint offsets and member forces.
     */
    private final Analysis.Interpolation animationInterpolation;
    /**
     * An analysis object used to hold the analysis of the artificially weakened
     * bridge that is the final failure animation configuration.
     */
    private final Analysis failureAnalysis;
    /**
     * Base interpolation that snapshots the instant where the bridge fails.  Includes
     * base member lengths for showing bucking and torn members.
     */
    private final Analysis.Interpolation failureInterpolation;
    /**
     * An interpolation between the normal interpolation where failure first occurred and
     * the analysis of the weakened bridge that is the final failure animation configuration.
     */
    private final Analysis.Interpolation failureAnimationInterpolation;
    /**
     *  Location of load when accumulator updater was last called.
     */
    private Affine.Point lastPtLoad = new Affine.Point();

    /**
     * Update the accumulator for distance moved by the load.  The
     * accumulator is zeroed when interpolate() is called
     * in its UNINITIALIZED state.
     */
    private void updateDistanceMoved() {
        final Affine.Point ptLoad = animationInterpolation.getPtLoad();
        distanceMoved += ptLoad.distance(lastPtLoad);
        lastPtLoad.setLocation(ptLoad);
    }

    /**
     * Return the value of the distance moved accumulator and zero it out.
     *
     * @return distance moved since last call to this function
     */
    public double getDistanceMoved() {
        double rtnVal = distanceMoved;
        distanceMoved = 0.0;
        return rtnVal;
    }

    /**
     * Standard load location update for given elapsed time. The algorithm
     * moves the truck quickly to the bridge slows down to cross, then
     * speeds away.
     *
     * @param elapsed time elapsed since last update in seconds
     */
    private void updateLoadLocation(double elapsed) {
        if (!config.paused) {
            double speed = config.truckSpeed * kmPerHrToPanelsPerSec;
            if (loadLocation <= 0 || loadLocation >= bridge.getDesignConditions().getNLoadedJoints()) {
                speed *= 3;
            }
            loadLocation += elapsed * speed;
            if (loadLocation >= loadEndDistanceTraveled) {
                loadLocation = -loadLocationRunup;
            }
        }
    }

    /**
     * Update the animation state based on pre-existing state and time elapsed since last call and then
     * return an interpolation of the bridge based on the new state.  This includes:
     * - Updating the eye and look-at points for the view.
     * - Initializing everything if state is currently uninitialized.
     * - Incrementing the amount of dead load applied if we are in the applying dead load state.
     * - Moving the load if we are in the dead loading state.
     * - Updating the failure if a failure is in progress.
     *
     * @param time current clock time in nanoseconds
     * @return interpolation structure describing the bridge and load states
     */
    protected Analysis.Interpolation interpolate(long time) {
        // Handle initialization.
        if (state == UNINITIALIZED_STATE) {
            lastDisplayTime = lastStateChangeTime = time;
            state = UNLOADED_STATE;
            loadLocation = -loadLocationRunup;
            // Initialize lookAt based on eye direction with no movement.
            updateView(0);
            animationInterpolation.initializeDeadLoadOnly(0.0, loadLocation, 0.0);
            getDistanceMoved(); // zero the accumulator
            return animationInterpolation;
        }

        // Update eye and look-at points.
        double elapsed = (time - lastDisplayTime) * 1e-9;
        lastDisplayTime = time;
        updateView(elapsed);

        // Determine state and resulting interpolations.
        double stateElapsed = (time - lastStateChangeTime) * 1e-9;
        switch (state) {
            case UNLOADED_STATE:
                if (stateElapsed >= initialPauseDuration) {
                    state = DEAD_LOADING_STATE;
                    lastStateChangeTime = time;
                }
                loadLocation = -loadLocationRunup;
                animationInterpolation.initializeDeadLoadOnly(0.0, loadLocation, 0.0);
                updateDistanceMoved();
                return animationInterpolation;
            case DEAD_LOADING_STATE:
                deadLoadApplied = stateElapsed / deadLoadingDuration;
                if (deadLoadApplied > 1.0) {
                    deadLoadApplied = 1.0;
                    state = LOAD_MOVING_STATE;
                    lastStateChangeTime = time;
                    loadEndDistanceTraveled = bridge.getDesignConditions().getNLoadedJoints() + loadLocationRunup;
                }
                loadLocation = -loadLocationRunup;
                animationInterpolation.initializeDeadLoadOnly(deadLoadApplied, loadLocation, config.displacementExaggeration);
                updateDistanceMoved();
                checkForFailure(animationInterpolation, time);
                return animationInterpolation;
            case LOAD_MOVING_STATE:
                updateLoadLocation(elapsed);
                animationInterpolation.initialize(loadLocation, config.displacementExaggeration);
                updateDistanceMoved();
                checkForFailure(animationInterpolation, time);
                return animationInterpolation;
            case LOAD_MOVING_TO_DEAD_LOAD_FAILURE:
                updateLoadLocation(elapsed);
                if (loadLocation >= emergencyStopLocation) {
                    state = FAILED_STATE;
                }
                // This is the easy way to implement this, but if we could go
                // faster with a specialized failure initialization.
                animationInterpolation.initialize(loadLocation, config.displacementExaggeration);
                failureAnimationInterpolation.initialize(animationInterpolation, failureInterpolation, 1.0);
                updateDistanceMoved();
                return failureAnimationInterpolation;
            case FAILING_STATE:
                double t = stateElapsed / failureDuration;
                if (t > 1) {
                    t = 1;
                    state = FAILED_STATE;
                }
                failureAnimationInterpolation.initialize(animationInterpolation, failureInterpolation, t);
                return failureAnimationInterpolation;
            case FAILED_STATE:
                if (loadLocation < emergencyStopLocation) {
                    state = LOAD_MOVING_TO_DEAD_LOAD_FAILURE;
                }
                return failureAnimationInterpolation;
        }
        return null;
    }

    /**
     * If the bridge has failed, adjust state accordingly.
     *
     * @param interpolation interpolation to check
     * @param time current time
     */
    private void checkForFailure(Analysis.Interpolation interpolation, long time) {
        // If the interpolation shows the bridge has failed and we're not already
        // handling this condition, go to the failing state.
        if (state < FAILING_STATE && interpolation.isFailure()) {
            failureAnalysis.initialize(bridge, interpolation.getFailureStatus());
            failureInterpolation.initialize(loadLocation, config.displacementExaggeration);
            state = FAILING_STATE;
            lastStateChangeTime = time;
        }
    }

    /**
     * Reset the animation state to case where the bridge is undeflected and
     * the truck is not yet visible.
     */
    protected void resetState() {
        state = UNINITIALIZED_STATE;
    }

    /**
     * Work around the well-known bug in canvas resizing by
     * setting minimum size to zero.
     */
    protected void applyCanvasResizeBugWorkaround() {
        Component c = getCanvas();
        while (c != null) {
            c.setMinimumSize(new Dimension(0, 0));
            c = c.getParent();
        }
    }
}
