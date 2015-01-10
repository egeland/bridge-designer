/*
 * Analysis.java  
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Perform an analysis of forces acting on a BridgeModel.
 * Includes both correct analysis and an intentionally perturbed one
 * for the failure animation.
 * 
 * @author Eugene K. Ressler
 */
public class Analysis {

    /**
     * Steel code factors.
     */
    private static final double deadLoadFactor = 1.35;
    private static final double liveLoadFactor = 1.75 * 1.33;
    
    /**
     * Multiplier for degrading the strength of members to animate the bridge failure.
     */
    private static final double failedMemberDegradation = 1.0 / 50.0;
    
    /**
     * Special values so that a double can be used to encode a flag for a 
     * failed member, an intact member, or (for the case where we are computing
     * a failure animation), the base getLength of a failed member so that it
     * can be drawn with a parabola or broken section.
     */
    private static final double NOT_FAILED = -1;
    private static final double FAILED = 1e6;
    
    /**
     * Analysis has not been initialized yet.
     */
    public static final int NO_STATUS = 0;
    /**
     * Analysis was completed, but the final slenderness check failed.
     * Even though the analysis was completed, it's bogus. We use value 1
     * because the load test report button should be disabled when the check
     * fails. The UNSTABLE case is the dividing line.
     */
    public static final int FAILS_SLENDERNESS = 1;
    /**
     * Analysis could not be completed because bridge is unstable.
     */
    public static final int UNSTABLE = 2;
    /**
     * Analysis was completed, but the load test failed.
     */
    public static final int FAILS_LOAD_TEST = 3;
    /**
     * Analysis was completed, and the bridge passed.
     */
    public static final int PASSES = 4;

    /**
     * Bridge for which analysis was last initialized.
     */
    private BridgeModel bridge;
    
    /**
     * Results matrices:
     * For member forces, indexes are [load index] [member index]
     * For joint displacements, indexes are [load index] [joint index]
     * 
     * In turn, there is one load index for each loaded (deck) joint.
     */
    private double[][] memberForce;
    private double[][] jointDisplacement;
    private boolean[][] memberFails;
    private double[] memberCompressiveStrength;
    private double[] memberTensileStrength;
    private double[] maxMemberCompressiveForces;
    private double[] maxMemberTensileForces;
    private int status = NO_STATUS;

    /**
     * Return the analysis status.
     * <pre>
     * NO_STATUS = 0;          analysis has not been performed
     * UNSTABLE = 1;           analysis could not complete because bridge is unstable
     * FAILS_SLENDERNESS = 2;  analysis completed, but at least one member is too slender
     * FAILS_LOAD_TEST = 3;    analysis completed, but bridge could not carry the load
     * PASSES = 4;             analysis completed, and bridge carries load
     * </pre>
     * @return analysis status indicator
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * Return the member force of a given member and load case.  It is the caller's responsibility
     * to ensure the analysis is valid and indices are in range.
     * 
     * @param ilc load case index
     * @param im member index
     * @return member force
     */
    public double getMemberForce(int ilc, int im) {
        return memberForce[ilc][im];
    }

    /**
     * Return the x-component of the displacement of a given joint and load case.  It is the caller's responsibility
     * to ensure the analysis is valid and indices are in range.
     * 
     * @param ilc load case index
     * @param ij joint index
     * @return x-component of displacement
     */
    public double getXJointDisplacement(int ilc, int ij) {
        return jointDisplacement[ilc][2 * ij];
    }

    /**
     * Return the y-component of the displacement of a given joint and load case. It is the caller's responsibility
     * to ensure the analysis is valid and indices are in range.
     * 
     * @param ilc load case index
     * @param ij joint index
     * @return y-component of displacement
     */
    public double getYJointDisplacement(int ilc, int ij) {
        return jointDisplacement[ilc][2 * ij + 1];
    }

    /**
     * Return the maximum compressive force acting on a given member among all load cases.  It is the caller's 
     * responsibility to ensure the analysis is valid and indices are in range.
     * 
     * @param i member index
     * @return maximum compressive force
     */
    public double getMemberCompressiveForce(int i) {
        return maxMemberCompressiveForces[i];
    }
    
    /**
     * Return the maximum tensile force acting on a given member among all load cases.  It is the caller's 
     * responsibility to ensure the analysis is valid and indices are in range.
     * 
     * @param i member index
     * @return maximum tensile force
     */
    public double getMemberTensileForce(int i) {
        return maxMemberTensileForces[i];
    }
    
    /**
     * Return the max allowable compressive force that may act on a given member before it fails.
     * 
     * This computation ignores slenderness.  Slenderness failures are considered separately.
     * 
     * @param i member index
     * @return compressive strength
     */
    public double getMemberCompressiveStrength(int i) {
        return memberCompressiveStrength[i];
    }
    
    /**
     * Return the max allowable tensile force that may act on a given member before it fails.
     * 
     * @param i member index
     * @return tensile strength
     */
    public double getMemberTensileStrength(int i) {
        return memberTensileStrength[i];
    }
    
    /**
     * Analyze the given bridge and store the results internally for future queries.
     * This mimics the WPBD code exactly. There exists a more precise and efficient algorithm.
     * 
     * @param bridge bridge to analyze
     */
    public void initialize(BridgeModel bridge) {
        initialize(bridge, null);
    }

    /**
     * Analyze the given bridge and store the results internally for future queries.
     * Artificially decrease the strength of failed members to support the failure animation.
     * This mimics the original WPBD code exactly. There exists a more precise and efficient algorithm.
     * 
     * @param bridge bridge to analyze
     * @param failureStatus status of failed members: FAILED, NOT_FAILED, base member getLength, which implies FAILED.
     */
    public void initialize(BridgeModel bridge, double [] failureStatus) {
        this.bridge = bridge;
        DesignConditions conditions = bridge.getDesignConditions();
        status = NO_STATUS;
        int nJoints = bridge.getJoints().size();
        int nEquations = 2 * nJoints;
        int nMembers = bridge.getMembers().size();
        Member[] members = bridge.getMembers().toArray(new Member[nMembers]);
        double[] length = new double[members.length];
        double[] cosX = new double[members.length];
        double[] cosY = new double[members.length];
        for (int i = 0; i < members.length; i++) {
            Affine.Point a = members[i].getJointA().getPointWorld();
            Affine.Point b = members[i].getJointB().getPointWorld();
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            length[i] = hypot(dx, dy);
            cosX[i] = dx / length[i];
            cosY[i] = dy / length[i];
        }
        final int nLoadInstances = conditions.getNLoadedJoints();
        final double pointLoads[][] = new double[nLoadInstances][nEquations];
        for (int im = 0; im < nMembers; im++) {
            double deadLoad =
                    deadLoadFactor *
                    members[im].getShape().getArea() *
                    length[im] *
                    members[im].getMaterial().getDensity() * 9.8066 / 2.0 / 1000.0;
            int dof1 = 2 * members[im].getJointA().getIndex() + 1;
            int dof2 = 2 * members[im].getJointB().getIndex() + 1;
            for (int ilc = 0; ilc < nLoadInstances; ilc++) {
                pointLoads[ilc][dof1] -= deadLoad;
                pointLoads[ilc][dof2] -= deadLoad;
            }
        }
        final double pointDeadLoad = (conditions.getDeckType() == DesignConditions.MEDIUM_STRENGTH_DECK) ? 
            deadLoadFactor * 120.265 + 33.097 : 
            deadLoadFactor * 82.608 + 33.097;
        for (int ij = 0; ij < conditions.getNLoadedJoints(); ij++) {
            int dof = 2 * ij + 1;
            for (int ilc = 0; ilc < nLoadInstances; ilc++) {
                double load = pointDeadLoad;
                if (ij == 0 || ij == conditions.getNLoadedJoints() - 1) {
                    load /= 2;
                }
                pointLoads[ilc][dof] -= load;
            }
        }
        // Standard (light) truck.
        double frontAxleLoad = 44;
        double rearAxleLoad = 181;
        if (conditions.getLoadType() != DesignConditions.STANDARD_TRUCK) {
            // Heavy truck.
            frontAxleLoad = 124;
            rearAxleLoad = 124;
        }
        for (int ilc = 1; ilc < nLoadInstances; ilc++) {
            int iFront = 2 * ilc + 1;
            int iRear = iFront - 2;
            pointLoads[ilc][iFront] -= liveLoadFactor * frontAxleLoad;
            pointLoads[ilc][iRear] -= liveLoadFactor * rearAxleLoad;
        }
        boolean xRestraint[] = new boolean[nJoints];
        boolean yRestraint[] = new boolean[nJoints];
        xRestraint[0] = yRestraint[0] = yRestraint[conditions.getNLoadedJoints() - 1] = true;
        if (conditions.isPier()) {
            int i = conditions.getPierJointIndex();
            xRestraint[i] = yRestraint[i] = true;
            if (conditions.isHiPier()) {
                xRestraint[0] = false;
            }
        }
        if (conditions.isArch()) {
            int i = conditions.getArchJointIndex();
            xRestraint[0] = yRestraint[0] = yRestraint[conditions.getNLoadedJoints() - 1] = false;
            xRestraint[i] = yRestraint[i] = true;
            xRestraint[i + 1] = yRestraint[i + 1] = true;
        }
        if (conditions.isLeftAnchorage()) {
            int i = conditions.getLeftAnchorageJointIndex();
            xRestraint[i] = yRestraint[i] = true;
        }
        if (conditions.isRightAnchorage()) {
            int i = conditions.getRightAnchorageJointIndex();
            xRestraint[i] = yRestraint[i] = true;
        }
        double stiffness[][] = new double[nEquations][nEquations];
        for (int im = 0; im < nMembers; im++) {
            double e = members[im].getMaterial().getE();
            if (failureStatus != null && failureStatus[im] != NOT_FAILED) {
                e *= failedMemberDegradation;
            }
            double aEOverL = members[im].getShape().getArea() * e / length[im];
            double xx = aEOverL * sqr(cosX[im]);
            double yy = aEOverL * sqr(cosY[im]);
            double xy = aEOverL * cosX[im] * cosY[im];
            int j1 = members[im].getJointA().getIndex();
            int j2 = members[im].getJointB().getIndex();
            int j1x = 2 * j1;
            int j1y = 2 * j1 + 1;
            int j2x = 2 * j2;
            int j2y = 2 * j2 + 1;
            stiffness[j1x][j1x] += xx;
            stiffness[j1x][j1y] += xy;
            stiffness[j1x][j2x] -= xx;
            stiffness[j1x][j2y] -= xy;
            stiffness[j1y][j1x] += xy;
            stiffness[j1y][j1y] += yy;
            stiffness[j1y][j2x] -= xy;
            stiffness[j1y][j2y] -= yy;
            stiffness[j2x][j1x] -= xx;
            stiffness[j2x][j1y] -= xy;
            stiffness[j2x][j2x] += xx;
            stiffness[j2x][j2y] += xy;
            stiffness[j2y][j1x] -= xy;
            stiffness[j2y][j1y] -= yy;
            stiffness[j2y][j2x] += xy;
            stiffness[j2y][j2y] += yy;
        }
        for (int ilc = 0; ilc < nLoadInstances; ilc++) {
            for (int ij = 0; ij < nJoints; ij++) {
                if (xRestraint[ij]) {
                    int ix = 2 * ij;
                    for (int ie = 0; ie < nEquations; ie++) {
                        stiffness[ix][ie] = stiffness[ie][ix] = 0;
                    }
                    stiffness[ix][ix] = 1;
                    pointLoads[ilc][ix] = 0;
                }
                if (yRestraint[ij]) {
                    int iy = 2 * ij + 1;
                    for (int ie = 0; ie < nEquations; ie++) {
                        stiffness[iy][ie] = stiffness[ie][iy] = 0;
                    }
                    stiffness[iy][iy] = 1;
                    pointLoads[ilc][iy] = 0;
                }
            }
        }
        for (int ie = 0; ie < nEquations; ie++) {
            double pivot = stiffness[ie][ie];
            if (Math.abs(pivot) < 0.99) {
                status = UNSTABLE;
                return;
            }
            double pivr = 1.0 / pivot;
            for (int k = 0; k < nEquations; k++) {
                stiffness[ie][k] /= pivot;
            }
            for (int k = 0; k < nEquations; k++) {
                if (k != ie) {
                    pivot = stiffness[k][ie];
                    for (int j = 0; j < nEquations; j++) {
                        stiffness[k][j] -= stiffness[ie][j] * pivot;
                    }
                    stiffness[k][ie] = -pivot * pivr;
                }
            }
            stiffness[ie][ie] = pivr;
        }
        memberForce = new double[nLoadInstances][nMembers];
        memberFails = new boolean[nLoadInstances][nMembers];
        jointDisplacement = new double[nLoadInstances][nEquations];
        for (int ilc = 0; ilc < nLoadInstances; ilc++) {
            for (int ie = 0; ie < nEquations; ie++) {
                double tmp = 0;
                for (int je = 0; je < nEquations; je++) {
                    tmp += stiffness[ie][je] * pointLoads[ilc][je];
                }
                jointDisplacement[ilc][ie] = tmp;
            }
            // Compute member forces.
            for (int im = 0; im < nMembers; im++) {
                double e = members[im].getMaterial().getE();
                if (failureStatus != null && failureStatus[im] != NOT_FAILED) {
                    e *= failedMemberDegradation;
                }
                double aeOverL = members[im].getShape().getArea() * e / length[im];
                int ija = members[im].getJointA().getIndex();
                int ijb = members[im].getJointB().getIndex();
                memberForce[ilc][im] = aeOverL *
                        ((cosX[im] * (getXJointDisplacement(ilc, ijb) - getXJointDisplacement(ilc, ija))) +
                        (cosY[im] * (getYJointDisplacement(ilc, ijb) - getYJointDisplacement(ilc, ija))));
            }
        }
        
        memberCompressiveStrength = new double[nMembers];
        memberTensileStrength = new double[nMembers];
        maxMemberCompressiveForces = new double[nMembers];
        maxMemberTensileForces = new double[nMembers];
        
        for (int im = 0; im < nMembers; im++) {
            final Material material = members[im].getMaterial();
            final Shape shape = members[im].getShape();
            memberCompressiveStrength[im] = Inventory.compressiveStrength(material, shape, length[im]);
            memberTensileStrength[im] = Inventory.tensileStrength(material, shape);
        }
        status = PASSES;
        for (int im = 0; im < nMembers; im++) {
            double maxCompression = 0;
            double maxTension = 0;
            for (int ilc = 0; ilc < nLoadInstances; ilc++) {
                double force = memberForce[ilc][im];
                if (force < 0) {
                    force = -force;
                    if (force > maxCompression) {
                        maxCompression = force;
                    }
                    memberFails[ilc][im] = (force / memberCompressiveStrength[im] > 1.0);
                } else {
                    if (force > maxTension) {
                        maxTension = force;
                    }
                    memberFails[ilc][im] = (force / memberTensileStrength[im] > 1.0);
                }
            }
            double cRatio = maxCompression / memberCompressiveStrength[im];
            double tRatio = maxTension / memberTensileStrength[im];
            // A fail for any member of any kind is a fail overall.
            if (cRatio > 1 || tRatio > 1) {
                status = FAILS_LOAD_TEST;
            }
            // Copy ratio information back to the bridge unless we're computing the intentionally distorted 
            // failure bridge.
            if (failureStatus == null) {
                members[im].setCompressionForceStrengthRatio(cRatio);
                members[im].setTensionForceStrengthRatio(tRatio);
            }
            maxMemberCompressiveForces[im] = maxCompression;
            maxMemberTensileForces[im] = maxTension;
        }
        if (!bridge.isPassingSlendernessCheck()) {
            status = FAILS_SLENDERNESS;
        }
    }

    // This is about 50 times faster than Math.hypot() !.
    private static double hypot(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }
        
    public static boolean isStatusBaseLength(double status) {
        return status != FAILED && status != NOT_FAILED;
    }
        
    /**
     * Interpolator used for animation purposes. Could also be used to decide failure case
     * at other than panel point resolution.  We are interpolating either between two load cases
     * corresponding to load location at given joint indices (including a pseudo-joint that puts
     * the load entirely right of the bridge) or we are interpolating between two different
     * interpolations with the load at the same place.  The latter supports the failure animation.
     */
    public class Interpolation {
        private final TerrainModel terrain;
        private final Affine.Vector [] displacement = initialDisp();
        private final double forceRatio [] = new double [DesignConditions.maxMemberCount];
        private final Affine.Point ptLoad = new Affine.Point();
        private final Affine.Vector zeroDisp = new Affine.Vector(0.0, 0.0);
        private final Affine.Point ptRightApproach = new Affine.Point(-100.0, 0.0);
        private final Affine.Vector loadRotation = new Affine.Vector();
        private double xLoadParameter;
        private int nFailures = 0;
        private final double [] failureStatus = new double [DesignConditions.maxMemberCount];
        
        /**
         * Make a new interpolation with roadway elevations taken from the given terrain.
         * 
         * @param terrain terrain describing roadway elevation
         */
        public Interpolation(TerrainModel terrain) {
            this.terrain = terrain;
        }

        /**
         * Return the base getLength of a failed member with given index.
         * 
         * @param i index of member
         * @return base getLength of failed member
         */
        public double getMemberStatus(int i) {
            return failureStatus[i];
        }
        
        /**
         * Create an initial array of displacement vectors.
         * 
         * @return array of zero displacement vectors
         */
        private Affine.Vector [] initialDisp() {
            Affine.Vector [] v = new Affine.Vector [DesignConditions.maxJointCount];
            for (int i = 0; i < v.length; i++) {
                v[i] = new Affine.Vector();
            }
            return v;
        }

        /**
         * Return true at least one of the members is failing.
         * 
         * @return member failure indication
         */
        public boolean isFailure() {
            return nFailures > 0;
        }
        
        /**
         * Return true iff the member with given index has failed.
         * 
         * @param i index of member
         * @return true iff member with index i has failed
         */
        public boolean isFailure(int i) {
            return failureStatus[i] >= 0;
        }
        
        /**
         * Return the load location.
         * 
         * @return location of the front axle in world coordintes.
         */
        public Affine.Point getPtLoad() {
            return ptLoad;
        }

        /**
         * Return a rotation-about-z matrix to put the load at the right angle from horizontal.
         * 
         * @return load rotation matrix.
         */
        public float [] getLoadRotationMatrix() {
            return Utility.rotateAboutZ(loadRotation.x, loadRotation.y);
        }
        
        /**
         * Return a unit direction vector signifying the load rotation with respect to the positive x-axis.
         * 
         * @return load rotation direction vector ( [1,0] is no rotation at all ).
         */
        public Affine.Vector getLoadRotation() {
            return loadRotation;
        }

        /**
         * Get a 2d vector describing interpolated displacement of the joint with index i.
         * 
         * @param i index of joint
         * @return 2d vector displacement
         */
        public Affine.Vector getDisplacement(int i) {
            return displacement[i];
        }
  
        /**
         * Return an array that quantities indicating which members are failed in the
         * interpolation.  Test for equality with the public constant NOT_FAILED.
         * 
         * @return boolean array true at indices of failed members, else false
         */
        public double [] getFailureStatus() {
            return failureStatus;
        }
        
        /**
         * Get the interpolated scalar force ratio value for the member with index i.  The ratio is
         * -1 for 100% compression capacity and +1 for 100% tension capacity. 
         * 
         * @param i index of member
         * @return force ratio
         */
        public double getForceRatio(int i) {
            return forceRatio[i];
        }

        /**
         * Helper routine to determine how far the load must rotate from
         * horizontal to have both contact points on the bridge.  This does
         * the calculation for one bridge panel given by its end points and
         * their displacements.  We use a binary search that stops when
         * a small tolerance is reached.
         * 
         * @param pt point of front contact
         * @param p0 point at right (greatest x-coord) of panel
         * @param d0 displacement of p0 due to load deflection
         * @param p1 point at left (least x-coord) of panel
         * @param d1 displacement of p1 due to load deflection
         * @param dist distance from front contact to rear
         * @return true if search for rear contact was successful
         */
        private boolean setLoadRotation(Affine.Point pt, 
                Affine.Point p0, Affine.Vector d0, 
                Affine.Point p1, Affine.Vector d1, double dist) {
            double xP0 = (p0.x + d0.x);
            double yP0 = (p0.y + d0.y);
            if (hypot(pt.x - xP0, pt.y - yP0) < dist) {
                return false;
            }
            double xP1 = (p1.x + d1.x);
            double yP1 = (p1.y + d1.y);
            double t0 = -.5; // Theoretically 0 would work, but this is numerically safer.
            double t1 = 1.5; // Theoretically 1 would work, but this is numerically safer.
            for (int i = 0; i < 20; i++) {
                double t = (t0 + t1) / 2;
                double x = (1 - t) * xP0 + t * xP1;
                double y = (1 - t) * yP0 + t * yP1;
                double eps = pt.distance(x, y) - dist;
                if (eps > .01) {
                    t0 = t;
                }
                else if (eps < -.01) {
                    t1 = t;
                }
                else {
                    loadRotation.x = pt.x - x;
                    loadRotation.y = pt.y - y;
                    double len = loadRotation.length();
                    if (len > 1e-6) {
                        loadRotation.x /= len;
                        loadRotation.y /= len;
                    }
                    else {
                        loadRotation.x = 1;
                        loadRotation.y = 0;
                    }
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Initialize an interpolation for the special case where we are interpolating between zero load
         * and dead load only.  Sets the load location for fade in effects.
         * 
         * @param deadLoadApplied the fraction of dead load to be applied in [0..1]
         * @param xLoadParameter x-location of the load with respect to joint 0.
         * @param displacementExaggeration amount that joint displacements should be exaggerated
         */
        public void initializeDeadLoadOnly(double deadLoadApplied, double xLoadParameter, double displacementExaggeration) {     
            this.xLoadParameter = xLoadParameter;
            Iterator<Joint> je = bridge.getJoints().iterator();
            while (je.hasNext()) {
                Joint joint = je.next();
                int i = joint.getIndex();
                displacement[i].x = deadLoadApplied * displacementExaggeration * getXJointDisplacement(0, i);
                displacement[i].y = deadLoadApplied * displacementExaggeration * getYJointDisplacement(0, i);
            }
            nFailures = 0;
            Iterator<Member> me = bridge.getMembers().iterator();
            while (me.hasNext()) {
                Member member = me.next();
                int i = member.getIndex();
                double force = deadLoadApplied * getMemberForce(0, i);
                double ratio = (force > 0) ? force / getMemberTensileStrength(i) : force / getMemberCompressiveStrength(i);
                forceRatio[i] = ratio * deadLoadApplied;
                if (forceRatio[i] < -1.0 || forceRatio[i] > 1.0) {
                    failureStatus[i] = FAILED;
                    ++nFailures;
                }
                else {
                    failureStatus[i] = NOT_FAILED;
                }
            }
            ptLoad.x = xLoadParameter * DesignConditions.panelSizeWorld + bridge.getJoints().get(0).getPointWorld().x + displacement[0].x;
            ptLoad.y = terrain.getRoadCenterlineElevation((float)ptLoad.x);
            // Initialize rotation for fixed eye case where truck isn't hidden.
            double x = ptLoad.x - DesignConditions.panelSizeWorld;
            Affine.Point ptSearchLeft = new Affine.Point(x, terrain.getRoadCenterlineElevation((float)x));
            loadRotation.setLocation(1, 0);
            setLoadRotation(ptLoad, ptSearchLeft, zeroDisp, ptLoad, zeroDisp, DesignConditions.panelSizeWorld);
        }

        /**
         * Initialize an interpolation for the current position of the load.  This will refer either to the
         * displacement sets of the two adjacent joints or to the location of the approach roadway.
         * 
         * @param xLoadParameter location of load in panel lengths
         * @param displacementExaggeration exaggeration factor for displacements
         */
        public void initialize(double xLoadParameter, double displacementExaggeration) {            
            this.xLoadParameter = xLoadParameter;
            final DesignConditions dc = bridge.getDesignConditions();
            ptRightApproach.x = dc.getXRightmostDeckJoint() + 100.0;
            int nLoadedJoints = dc.getNLoadedJoints();
            loadRotation.setLocation(1, 0);

            // Handle case where truck is not on bridge.
            if (xLoadParameter <= 0 || xLoadParameter >= nLoadedJoints) {
                Iterator<Joint> je = bridge.getJoints().iterator();
                while (je.hasNext()) {
                    Joint joint = je.next();
                    int i = joint.getIndex();
                    displacement[i].x = displacementExaggeration * getXJointDisplacement(0, i);
                    displacement[i].y = displacementExaggeration * getYJointDisplacement(0, i);
                }
                nFailures = 0;
                Iterator<Member> me = bridge.getMembers().iterator();
                while (me.hasNext()) {
                    Member member = me.next();
                    int i = member.getIndex();
                    double force = getMemberForce(0, i);
                    forceRatio[i] = (force > 0) ? force / getMemberTensileStrength(i) : force / getMemberCompressiveStrength(i);
                    if (forceRatio[i] < -1.0 || forceRatio[i] > 1.0) {
                        failureStatus[i] = FAILED;
                        ++nFailures;
                    }
                    else {
                        failureStatus[i] = NOT_FAILED;
                    }
                }
                // Truck position.
                if (xLoadParameter <= 0) {
                    ptLoad.x = xLoadParameter * DesignConditions.panelSizeWorld + 
                            bridge.getJoints().get(0).getPointWorld().x + displacement[0].x;
                }
                else {
                    final int iLast = nLoadedJoints - 1;
                    ptLoad.x = (xLoadParameter - iLast) * DesignConditions.panelSizeWorld + 
                            bridge.getJoints().get(iLast).getPointWorld().x + displacement[iLast].x;
                }
                ptLoad.y = terrain.getRoadCenterlineElevation((float)ptLoad.x);
                double x = ptLoad.x - DesignConditions.panelSizeWorld;
                Affine.Point ptSearchLeft = new Affine.Point(x, terrain.getRoadCenterlineElevation((float)x));
                setLoadRotation(ptLoad, ptSearchLeft, zeroDisp, ptLoad, zeroDisp, DesignConditions.panelSizeWorld);
                return;
            }
            
            // General case.  Truck has at least one axle on the bridge. 
            final int ilcLeft = (int)xLoadParameter;
            final int ilcRight = (ilcLeft < nLoadedJoints - 1) ? ilcLeft + 1 : 0;
            final double t1 = xLoadParameter - ilcLeft;
            final double t0 = 1 - t1;
            Iterator<Joint> je = bridge.getJoints().iterator();
            while (je.hasNext()) {
                int i = je.next().getIndex();
                displacement[i].x = displacementExaggeration * (t0 * getXJointDisplacement(ilcLeft, i) + t1 * getXJointDisplacement(ilcRight, i));
                displacement[i].y = displacementExaggeration * (t0 * getYJointDisplacement(ilcLeft, i) + t1 * getYJointDisplacement(ilcRight, i));
            }
            nFailures = 0;
            Iterator<Member> me = bridge.getMembers().iterator();
            while (me.hasNext()) {
                Member member = me.next();
                int i = member.getIndex();
                final double force = t0 * memberForce[ilcLeft][i] + t1 * memberForce[ilcRight][i];
                final double ratio = (force > 0) ? force / memberTensileStrength[i] : force / memberCompressiveStrength[i];
                // We allow left force to trigger failures so we can't step past one.
                if (memberFails[ilcLeft][i] || ratio < -1.0 || ratio > 1.0) {
                    failureStatus[i] = FAILED;
                    ++nFailures;
                }
                else {
                    failureStatus[i] = NOT_FAILED;
                }
                forceRatio[i] = ratio;
            }
            final Affine.Point ptLeft = bridge.getJoints().get(ilcLeft).getPointWorld();
            final Affine.Point ptRight = bridge.getJoints().get(ilcRight).getPointWorld();
            if (ilcLeft < nLoadedJoints - 1) {
                // Interpolate between left and right points.
                ptLoad.x = t0 * (ptLeft.x + displacement[ilcLeft].x) + t1 * (ptRight.x + displacement[ilcRight].x);
                ptLoad.y = t0 * (ptLeft.y + displacement[ilcLeft].y) + t1 * (ptRight.y + displacement[ilcRight].y) + BridgeView.wearSurfaceHeight;
            }
            else {
                // Interpolate between height of roadway and left point.
                ptLoad.x = (ptLeft.x + displacement[ilcLeft].x) + t1 * DesignConditions.panelSizeWorld;
                ptLoad.y = t0 * (ptLeft.y + displacement[ilcLeft].y + BridgeView.wearSurfaceHeight) + t1 * terrain.getRoadCenterlineElevation((float)ptLoad.x);
            }

            Affine.Point ptSearchRight;
            Affine.Vector dispSearchRight;
            // If truck is still on bridge, start search for rear axle height at point right of front wheel.
            // Else start it at the front tire contact point with the approach road.
            if (ilcLeft < nLoadedJoints - 1) {
                ptSearchRight = ptRight;
                dispSearchRight = displacement[ilcRight];
            }
            else {
                ptSearchRight = ptLoad;
                dispSearchRight = zeroDisp;                
            }
            // Search left for a (displaced) panel that includes the rear axle.  When found (should always
            // be successful, set the load rotation.
            for (int i = ilcLeft; i >= -1; i--) {
                Affine.Point ptSearchLeft;
                Affine.Vector dispSearchLeft;
                if (i < 0) {
                    double x = ptLoad.x - DesignConditions.panelSizeWorld;
                    ptSearchLeft = new Affine.Point(x, terrain.getRoadCenterlineElevation((float)x));
                    dispSearchLeft = zeroDisp;
                }
                else {
                    ptSearchLeft = bridge.getJoints().get(i).getPointWorld().plus(0, BridgeView.wearSurfaceHeight);
                    dispSearchLeft = displacement[i];
                }
                if (setLoadRotation(ptLoad, ptSearchLeft, dispSearchLeft, ptSearchRight, dispSearchRight, DesignConditions.panelSizeWorld)) {
                    break;
                }
                ptSearchRight = ptSearchLeft;
                dispSearchRight = dispSearchLeft;
            }
        }
  
        /**
         * Initialize this interpolation by interpolating two others.  A base interpolation is used for result
         * load location, forces, failures, and a weighted fraction of displacements.  A target interpolation
         * gives the other weighted component of the displacements only.  A displacement parameter determines linear
         * interpolation of base and target displacements with 0 => base, 1 => target.
         * 
         * @param base base interpolation 
         * @param target target interpolation
         * @param displacementParameter parameter 
         */
        public void initialize(Interpolation base, Interpolation target, double displacementParameter) {
            xLoadParameter = base.xLoadParameter;
            final DesignConditions dc = bridge.getDesignConditions();
            ptRightApproach.x = dc.getXRightmostDeckJoint() + 100.0;
            int nLoadedJoints = dc.getNLoadedJoints();
            loadRotation.setLocation(1, 0);
            final double ta = 1.0 - displacementParameter;
            final double tf = displacementParameter;
            final int nJoints = bridge.getJoints().size();
            for (int i = 0; i < nJoints; i++) {
                displacement[i].x = ta * base.displacement[i].x + tf * target.displacement[i].x;
                displacement[i].y = ta * base.displacement[i].y + tf * target.displacement[i].y;
            }
            final int nMembers = bridge.getMembers().size();
            for (int i = 0; i < nMembers; i++) {
                forceRatio[i] = base.forceRatio[i];
                if (base.failureStatus[i] == NOT_FAILED) {
                    failureStatus[i] = NOT_FAILED;
                }
                else {
                    // All this is just to calculate the lengths of members
                    // with displacements and not generate any garbage.
                    final Member m = bridge.getMembers().get(i);
                    final Joint a = m.getJointA();
                    final Joint b = m.getJointB();
                    final Affine.Point pa = a.getPointWorld();
                    final Affine.Point pb = b.getPointWorld();
                    final Affine.Vector da = base.displacement[a.getIndex()];
                    final Affine.Vector db = base.displacement[b.getIndex()];
                    final double dx = (pa.x + da.x) - (pb.x + db.x);
                    final double dy = (pa.y + da.y) - (pb.y + db.y);
                    failureStatus[i] = Math.sqrt(dx * dx + dy * dy);
                }
            }
            nFailures = base.nFailures;

            // Handle case where truck is not on bridge.
            if (xLoadParameter <= 0 || xLoadParameter >= nLoadedJoints) {
                // Truck position.
                if (xLoadParameter <= 0) {
                    ptLoad.x = xLoadParameter * DesignConditions.panelSizeWorld + 
                            bridge.getJoints().get(0).getPointWorld().x + displacement[0].x;
                }
                else {
                    final int iLast = nLoadedJoints - 1;
                    ptLoad.x = (xLoadParameter - iLast) * DesignConditions.panelSizeWorld + 
                            bridge.getJoints().get(iLast).getPointWorld().x + displacement[iLast].x;
                }
                ptLoad.y = terrain.getRoadCenterlineElevation((float)ptLoad.x);
                double x = ptLoad.x - DesignConditions.panelSizeWorld;
                Affine.Point ptSearchLeft = new Affine.Point(x, terrain.getRoadCenterlineElevation((float)x));
                setLoadRotation(ptLoad, ptSearchLeft, zeroDisp, ptLoad, zeroDisp, DesignConditions.panelSizeWorld);
                return;
            }
            
            // General case.  Truck has at least one axle on the bridge. 
            final int ilcLeft = (int)xLoadParameter;
            final int ilcRight = (ilcLeft < nLoadedJoints - 1) ? ilcLeft + 1 : 0;
            final double t1 = xLoadParameter - ilcLeft;
            final double t0 = 1 - t1;
            final Affine.Point ptLeft = bridge.getJoints().get(ilcLeft).getPointWorld();
            final Affine.Point ptRight = bridge.getJoints().get(ilcRight).getPointWorld();
            if (ilcLeft < nLoadedJoints - 1) {
                // Interpolate between left and right points.
                ptLoad.x = t0 * (ptLeft.x + displacement[ilcLeft].x) + t1 * (ptRight.x + displacement[ilcRight].x);
                ptLoad.y = t0 * (ptLeft.y + displacement[ilcLeft].y) + t1 * (ptRight.y + displacement[ilcRight].y) + BridgeView.wearSurfaceHeight;
            }
            else {
                // Interpolate between height of roadway and left point.
                ptLoad.x = (ptLeft.x + displacement[ilcLeft].x) + t1 * DesignConditions.panelSizeWorld;
                ptLoad.y = t0 * (ptLeft.y + displacement[ilcLeft].y + BridgeView.wearSurfaceHeight) + t1 * terrain.getRoadCenterlineElevation((float)ptLoad.x);
            }

            Affine.Point ptSearchRight;
            Affine.Vector dispSearchRight;
            // If truck is still on bridge, start search for rear axle height at point right of front wheel.
            // Else start it at the front tire contact point with the approach road.
            if (ilcLeft < nLoadedJoints - 1) {
                ptSearchRight = ptRight.plus(0, BridgeView.wearSurfaceHeight);
                dispSearchRight = displacement[ilcRight];
            }
            else {
                ptSearchRight = ptLoad;
                dispSearchRight = zeroDisp;                
            }
            // Search left for a (displaced) panel that includes the rear axle.  When found (should always
            // be successful, set the load rotation.
            for (int i = ilcLeft; i >= -1; i--) {
                Affine.Point ptSearchLeft;
                Affine.Vector dispSearchLeft;
                if (i < 0) {
                    double x = ptLoad.x - DesignConditions.panelSizeWorld;
                    ptSearchLeft = new Affine.Point(x, terrain.getRoadCenterlineElevation((float)x));
                    dispSearchLeft = zeroDisp;
                }
                else {
                    ptSearchLeft = bridge.getJoints().get(i).getPointWorld().plus(0, BridgeView.wearSurfaceHeight);
                    dispSearchLeft = displacement[i];
                }
                if (setLoadRotation(ptLoad, ptSearchLeft, dispSearchLeft, ptSearchRight, dispSearchRight, DesignConditions.panelSizeWorld)) {
                    break;
                }
                ptSearchRight = ptSearchLeft;
                dispSearchRight = dispSearchLeft;
            }
        }         
    };

    /**
     * Return a new interpolation.
     * 
     * @param terrain terrain used for roadway dimensions of the interpolation.
     * 
     * @return the new interpolation
     */
    public Interpolation getNewInterpolation(TerrainModel terrain) {
        return new Interpolation(terrain);
    }
    
    /**
     * Square the parameter.
     * 
     * @param x quantity to be squared
     * @return the square
     */
    private static double sqr(double x) {
        return x * x;
    }
    
    // Simple command line utility for checking a bridge's pass-fail status.
    private static class Runnable {

        private BridgeModel bridge = new BridgeModel();
        private Analysis analysis = new Analysis();

        private void run(String fileName) {
            try {
                bridge.read(new File(fileName));
                analysis.initialize(bridge);
                if (analysis.getStatus() >= FAILS_SLENDERNESS) {
                    System.out.print(fileName + ": ");
                    System.out.println(analysis.getStatus() == PASSES ? "passes." : "fails.");
                }
            } catch (IOException ex) {
                System.err.println("could not open '" + fileName + "' as a bridge file.");
            }
        }
    }
   
    public static void main(String [] args) {
        if (args.length != 1) {
            System.err.println("usage: java Analysis FileName");
        }
        else {
            new Runnable().run(args[0]);
        }
    }
}
