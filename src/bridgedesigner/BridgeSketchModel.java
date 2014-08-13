/*
 * BridgeSketchModel.java  
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.jdesktop.application.ResourceMap;

/**
 * Bridge template sketches that can be created with heuristic algorithms or read from local storage.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeSketchModel {

    /**
     * Name of the sketch.
     */
    protected String name;
    /**
     * Design conditions for the bridge being sketched.
     */
    protected DesignConditions conditions;
    /**
     * Joint locations for the bridge being sketched.
     */
    protected Affine.Point[] jointLocations;
    /**
     * Traces of members for the bridge being sketched.
     */
    protected SketchMember[] memberLocations;
    /**
     * 2 raised to this power is the size of the drawing grid in world coordinates
     */
    protected static final int gridScale = -2;

    private ArrayList<Affine.Point> pts = new ArrayList<Affine.Point>();
    private ArrayList<SketchMember> mrs = new ArrayList<SketchMember>();

    /**
     * Representation of a sketched member.
     */
    public static class SketchMember {
        /**
         * First joint location.
         */
        public Affine.Point jointA;
        /**
         * Second joint location.
         */
        public Affine.Point jointB;

        /**
         * Construct a new sketch member with given joint locations.
         * 
         * @param a first joint location
         * @param b second joint location
         */
        public SketchMember(Affine.Point a, Affine.Point b) {
            jointA = a;
            jointB = b;
        }
    }

    /**
     * Return the design conditions for this sketch.
     */
    public DesignConditions getDesignConditions() {
        return conditions;
    }

    /**
     * Return the i'th joint location.
     * 
     * @param i joint location index
     * @return joint location
     */
    public Affine.Point getJointLocation(int i) {
        return jointLocations[i];
    }

    /**
     * Return the number of joints in the sketch.
     * 
     * @return number of joints in this sketch
     */
    public int getJointLocationCount() {
        return jointLocations.length;
    }

    /**
     * Return the i'th sketch member.
     * 
     * @param i sketch member index
     * @return sketch member
     */
    public SketchMember getSketchMember(int i) {
        return memberLocations[i];
    }

    /**
     * Return the number of members in the sketch.
     * 
     * @return number of sketch members
     */
    public int getSketchMemberCount() {
        return memberLocations.length;
    }
    
    /**
     * Return the smallest possible snap multiple needed by any joint in this sketch.
     * See <code>DraftingGrid</code> for meaning of a snap multiple.
     * 
     * @return snap multiple
     */
    public int getSnapMultiple() {
        int rtn = DraftingGrid.maxSnapMultiple;
        for (int i = 0; i < jointLocations.length; i++) {
            rtn = Math.min(rtn, Math.min(
                    DraftingGrid.snapMultipleOf(jointLocations[i].x), 
                    DraftingGrid.snapMultipleOf(jointLocations[i].y)));
        }
        return rtn;
    }

    /**
     * Copy prescribed joint locations from design conditions to this sketch and also install members along the deck.
     * 
     * @param conditions design conditions
     */
    private void setPrescribedJoints(DesignConditions conditions) {
        this.conditions = conditions;
        pts.clear();
        mrs.clear();
        final int nPrescribedJoints = conditions.getNPrescribedJoints();
        for (int i = 0; i < nPrescribedJoints; i++) {
            pts.add(conditions.getPrescribedJointLocation(i));
        }
        // Bottom chord.
        final int iFirstBottomJoint = 0;
        final int iLastBottomJoint = conditions.getNPanels();
        for (int i = iFirstBottomJoint; i < iLastBottomJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
    }

    /** 
     * Complete the heuristic sketch under construction by copying temporary vectors to permanent arrays.
     */
    private void closeSketch() {
        jointLocations = pts.toArray(new Affine.Point[pts.size()]);
        memberLocations = mrs.toArray(new SketchMember[mrs.size()]);
    }

    /*
     * Following are many heuristic routines to create trusses of various standard types.
     */
    private void setPrattThruTrussImpl(DesignConditions conditions, double jointY) {
        setPrescribedJoints(conditions);
        final int iFirstBottomJoint = 0;
        final int iLastBottomJoint = conditions.getNPanels();
        final int iFirstTopJoint = pts.size();
        for (int i = iFirstBottomJoint + 1; i < iLastBottomJoint; i++) {
            pts.add(new Affine.Point(pts.get(i).x, jointY));
        }
        final int iLastTopJoint = pts.size() - 1;
        mrs.add(new SketchMember(pts.get(iFirstBottomJoint), pts.get(iFirstTopJoint)));
        for (int i = iFirstTopJoint; i < iLastTopJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iLastBottomJoint), pts.get(iLastTopJoint)));
        int iLeftTop = iFirstTopJoint;
        int iRightTop = iLastTopJoint;
        int iLeftBottom = iFirstBottomJoint + 1;
        int iRightBottom = iLastBottomJoint - 1;
        while (iLeftTop < iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom + 1)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom - 1)));
            iLeftTop++;
            iRightTop--;
            iLeftBottom++;
            iRightBottom--;
        }
        if (iLeftTop == iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
        }
        closeSketch();
    }

    private void setHoweThruTrussImpl(DesignConditions conditions, double jointY) {
        setPrescribedJoints(conditions);
        final int iFirstBottomJoint = 0;
        final int iLastBottomJoint = conditions.getNPanels();
        final int iFirstTopJoint = pts.size();
        for (int i = iFirstBottomJoint + 1; i < iLastBottomJoint; i++) {
            pts.add(new Affine.Point(pts.get(i).x, jointY));
        }
        final int iLastTopJoint = pts.size() - 1;
        mrs.add(new SketchMember(pts.get(iFirstBottomJoint), pts.get(iFirstTopJoint)));
        for (int i = iFirstTopJoint; i < iLastTopJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iLastBottomJoint), pts.get(iLastTopJoint)));
        int iLeftTop = iFirstTopJoint;
        int iRightTop = iLastTopJoint;
        int iLeftBottom = iFirstBottomJoint + 1;
        int iRightBottom = iLastBottomJoint - 1;
        while (iLeftTop < iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iLeftTop + 1), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop - 1), pts.get(iRightBottom)));
            iLeftTop++;
            iRightTop--;
            iLeftBottom++;
            iRightBottom--;
        }
        if (iLeftTop == iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
        }
        closeSketch();
    }

    private void setWarrenTrussImpl(DesignConditions conditions, double jointY) {        
        setPrescribedJoints(conditions);
        final int iFirstBottomJoint = 0;
        final int iLastBottomJoint = conditions.getNPanels();
        final int iFirstTopJoint = pts.size();
        for (int i = iFirstBottomJoint; i < iLastBottomJoint; i++) {
            pts.add(new Affine.Point(pts.get(i).x + DesignConditions.panelSizeWorld / 2, jointY));
        }
        final int iLastTopJoint = pts.size() - 1;
        // Top chord.
        for (int i = iFirstTopJoint; i < iLastTopJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iFirstBottomJoint), pts.get(iFirstTopJoint)));
        mrs.add(new SketchMember(pts.get(iLastBottomJoint), pts.get(iLastTopJoint)));
        int iTop = iFirstTopJoint;
        int iBottom = iFirstBottomJoint;
        while (iTop <= iLastTopJoint) {
            mrs.add(new SketchMember(pts.get(iTop), pts.get(iBottom)));
            mrs.add(new SketchMember(pts.get(iTop), pts.get(iBottom + 1)));
            iTop++;
            iBottom++;
        }
    }
    
    private void setWarrenTruss(DesignConditions conditions, double jointY) {
        setWarrenTrussImpl(conditions, jointY);
        closeSketch();
    }
    
    private void setCableStayedWarrenTruss(DesignConditions conditions, double jointY) {
        setWarrenTrussImpl(conditions, jointY);
        if (conditions.getNAnchorages() > 0) {
            Affine.Point leftAbutmentJoint = pts.get(0);
            Affine.Point leftAnchorageJoint = pts.get(conditions.getNPanels() + 1);
            Affine.Point mastJoint = new Affine.Point(leftAbutmentJoint.x, leftAbutmentJoint.y + 2 * DesignConditions.panelSizeWorld);
            pts.add(mastJoint);
            mrs.add(new SketchMember(mastJoint, leftAbutmentJoint));
            mrs.add(new SketchMember(mastJoint, leftAnchorageJoint));
            for (int i = 1; i <= conditions.getNPanels() / 2; i++) {
                mrs.add(new SketchMember(mastJoint, pts.get(i)));
            }
        }
        if (conditions.getNAnchorages() > 1) {
            Affine.Point rightAbutmentJoint = pts.get(conditions.getNPanels());
            Affine.Point rightAnchorageJoint = pts.get(conditions.getNPanels() + 2);
            Affine.Point mastJoint = new Affine.Point(rightAbutmentJoint.x, rightAbutmentJoint.y + 2 * DesignConditions.panelSizeWorld);
            pts.add(mastJoint);
            mrs.add(new SketchMember(mastJoint, rightAbutmentJoint));
            mrs.add(new SketchMember(mastJoint, rightAnchorageJoint));
            for (int i = 1; i <= conditions.getNPanels() / 2; i++) {
                mrs.add(new SketchMember(mastJoint, pts.get(conditions.getNPanels() - i)));
            }
        }
        closeSketch();
    }

    private void setSuspendedWarrenTruss(DesignConditions conditions, double jointY) {
        setWarrenTrussImpl(conditions, jointY);
        if (conditions.getNAnchorages() == 2) {
            Affine.Point leftAbutmentJoint = pts.get(0);
            Affine.Point leftAnchorageJoint = pts.get(conditions.getNPanels() + 1);
            Affine.Point leftMastJoint = new Affine.Point(leftAbutmentJoint.x, leftAbutmentJoint.y + 2 * DesignConditions.panelSizeWorld);
            pts.add(leftMastJoint);
            mrs.add(new SketchMember(leftMastJoint, leftAbutmentJoint));
            mrs.add(new SketchMember(leftMastJoint, leftAnchorageJoint));
            Affine.Point rightAbutmentJoint = pts.get(conditions.getNPanels());
            Affine.Point rightAnchorageJoint = pts.get(conditions.getNLoadedJoints() + 1);
            Affine.Point rightMastJoint = new Affine.Point(rightAbutmentJoint.x, rightAbutmentJoint.y + 2 * DesignConditions.panelSizeWorld);
            pts.add(rightMastJoint);
            mrs.add(new SketchMember(rightMastJoint, rightAbutmentJoint));
            mrs.add(new SketchMember(rightMastJoint, rightAnchorageJoint));
            
            double x0 = 0.5 * (leftMastJoint.x + rightMastJoint.x);
            double xb = rightMastJoint.x - x0;
            double yb = rightMastJoint.y;
            Affine.Point aPt = pts.get((conditions.getNPanels() + 1) / 2);
            double xa = aPt.x - x0;
            double ya = aPt.y + 1.0;
            double a = (ya - yb) / (xa * xa - xb * xb);
            double b = ya - a * xa * xa;
            double x = xb - DesignConditions.panelSizeWorld;
            Affine.Point lastJoint = rightMastJoint;
            for (int i = conditions.getNPanels() - 1; i > 0; i--) {
                double y = a * x * x + b;
                Affine.Point currentJoint = new Affine.Point(x0 + x, roundToScale(y, gridScale));
                pts.add(currentJoint);
                mrs.add(new SketchMember(pts.get(i), currentJoint));
                mrs.add(new SketchMember(lastJoint, currentJoint));
                x -= DesignConditions.panelSizeWorld;
                lastJoint = currentJoint;
            }
            mrs.add(new SketchMember(lastJoint, leftMastJoint));
        }
        closeSketch();
    }

    /**
     * Set a bridge sketch name string from the application resource bundle.
     *
     * @param key key of string resource to load
     */
    private void setNameFromResource(String key) {
        name = BDApp.getResourceMap(BridgeSketchModel.class).getString(key);
    }

    /**
     * Set this sketch to be a standard Pratt through truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setPrattThruTruss(DesignConditions conditions) {
        setNameFromResource("prattThruTruss.text");
        setPrattThruTrussImpl(conditions, DesignConditions.panelSizeWorld);
        return this;
    }

    /**
     * Set this sketch to be a standard Pratt deck truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setPrattDeckTruss(DesignConditions conditions) {
        setNameFromResource("prattDeckTruss.text");
        // Pratt deck geometry is inverted Howe.
        setHoweThruTrussImpl(conditions, -DesignConditions.panelSizeWorld);
        return this;
    }

    /**
     * Set this sketch to be a standard Howe through truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setHoweThruTruss(DesignConditions conditions) {
        setNameFromResource("howeThruTruss.text");
        setHoweThruTrussImpl(conditions, DesignConditions.panelSizeWorld);
        return this;
    }

    /**
     * Set this sketch to be a standard Howe deck truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setHoweDeckTruss(DesignConditions conditions) {
        setNameFromResource("howeDeckTruss.text");
        // Howe deck geometry is inverted Pratt.
        setPrattThruTrussImpl(conditions, -DesignConditions.panelSizeWorld);
        return this;
    }

    /**
     * Set this sketch to be a standard Warren through truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setWarrenThruTruss(DesignConditions conditions) {
        setNameFromResource("warrenThruTruss.text");
        setWarrenTruss(conditions, DesignConditions.panelSizeWorld);
        return this;
    }

    /**
     * Set this sketch to be a standard Warren deck truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setWarrenDeckTruss(DesignConditions conditions) {
        setNameFromResource("warrenDeckTruss.text");
        setWarrenTruss(conditions, -DesignConditions.panelSizeWorld);
        return this;
    }

    /**
     * Set this sketch to be a standard cable-stayed Warren deck truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setCableStayedWarrenTruss(DesignConditions conditions) {
        setNameFromResource("warrenCableStayed.text");
        setCableStayedWarrenTruss(conditions, -DesignConditions.panelSizeWorld / 2);
        return this;
    }

    /**
     * Set this sketch to be a standard suspended Warren deck truss based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setSuspendedWarrenTruss(DesignConditions conditions) {
        setNameFromResource("warrenSuspended.text");
        setSuspendedWarrenTruss(conditions, -DesignConditions.panelSizeWorld / 2);
        return this;
    }

    /**
     * Round an arbitrary coordinate to the drawing grid.
     * 
     * @param x arbitrary coordinate
     * @param scale power of 2 scale factor for rounding
     * @return rounded coordinate
     */
    private static double roundToScale(double x, int scale) {
        return Math.scalb(Math.rint(Math.scalb(x, -scale)), scale);
    }

    /**
     * Set this sketch to be a standard Pratt continuous arch based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setPrattContArch(DesignConditions conditions) {
        setNameFromResource("prattContArch.text");
        setPrescribedJoints(conditions);
        int iAbutmentArchJoints = conditions.getArchJointIndex();
        final int iFirstDeckJoint = 0;
        final int iLastDeckJoint = conditions.getNPanels();
        // p1 and p2 are used to find a parabolic arc.
        Affine.Point p1 = pts.get(iAbutmentArchJoints);
        Affine.Point p2 = pts.get((iFirstDeckJoint + iLastDeckJoint) / 2);
        Affine.Point p3 = pts.get(iAbutmentArchJoints + 1);
        double xMid = 0.5 * (p1.x + p3.x);
        double x1 = p1.x - xMid;
        double y1 = p1.y;
        double x2 = p2.x - xMid;
        // double y2 = p2.y - 1.0;
        // Peak of arc is 1/4 of arch height below deck.
        double y2 = p2.y - 0.25 * (p2.y - p1.y);
        // Find coefficients for y = a * x^2 + b;
        double a = (y2 - y1) / (x2 * x2 - x1 * x1);
        double b = y1 - a * x1 * x1;
        // Add joints on the parabola, one beneath each deck joint.
        final int iFirstArchJoint = pts.size();
        for (int i = iFirstDeckJoint + 1; i < iLastDeckJoint; i++) {
            double x = pts.get(i).x - xMid;
            double y = a * x * x + b;
            pts.add(new Affine.Point(pts.get(i).x, roundToScale(y, gridScale)));
        }
        final int iLastArchJoint = pts.size() - 1;
        // Add arch memberLocations and also the two verticals between deck and arch at each abutment.
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints), pts.get(iFirstArchJoint)));
        for (int i = iFirstArchJoint; i < iLastArchJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iLastArchJoint), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints + 1), pts.get(iLastDeckJoint)));
        // Add the remaining verticals and diagonals.
        int iLeftTop = iFirstDeckJoint + 1;
        int iRightTop = iLastDeckJoint - 1;
        int iLeftBottom = iFirstArchJoint;
        int iRightBottom = iLastArchJoint;
        while (iLeftTop < iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iLeftTop - 1), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop + 1), pts.get(iRightBottom)));
            iLeftTop++;
            iRightTop--;
            iLeftBottom++;
            iRightBottom--;
        }
        // Final two diagonals.
        mrs.add(new SketchMember(pts.get(iLeftTop - 1), pts.get(iLeftBottom)));
        mrs.add(new SketchMember(pts.get(iRightTop + 1), pts.get(iRightBottom)));
        // Center vertical, if any.
        if (iLeftTop == iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
        }
        closeSketch();
        return this;
    }

    /**
     * Set this sketch to be a standard Howe continuous arch based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setHoweContArch(DesignConditions conditions) {
        setNameFromResource("howeContArch.text");
        setPrescribedJoints(conditions);
        int iAbutmentArchJoints = conditions.getArchJointIndex();
        final int iFirstDeckJoint = 0;
        final int iLastDeckJoint = conditions.getNPanels();
        // p1 and p2 are used to find a parabolic arc.
        Affine.Point p1 = pts.get(iAbutmentArchJoints);
        Affine.Point p2 = pts.get((iFirstDeckJoint + iLastDeckJoint) / 2);
        Affine.Point p3 = pts.get(iAbutmentArchJoints + 1);
        double xMid = 0.5 * (p1.x + p3.x);
        double x1 = p1.x - xMid;
        double y1 = p1.y;
        double x2 = p2.x - xMid;
        // Peak of arc is 3/4 of arch height.
        double y2 = p2.y - 0.25 * (p2.y - p1.y);
        // Find coefficients for y = a * x^2 + b;
        double a = (y2 - y1) / (x2 * x2 - x1 * x1);
        double b = y1 - a * x1 * x1;
        // Add joints on the parabola, one beneath each deck joint.
        final int iFirstArchJoint = pts.size();
        for (int i = iFirstDeckJoint + 1; i < iLastDeckJoint; i++) {
            double x = pts.get(i).x - xMid;
            double y = a * x * x + b;
            pts.add(new Affine.Point(pts.get(i).x, roundToScale(y, gridScale)));
        }
        final int iLastArchJoint = pts.size() - 1;
        // Add arch memberLocations and also the two verticals between deck and arch at each abutment.
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint + 1), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iFirstArchJoint), pts.get(iAbutmentArchJoints)));
        for (int i = iFirstArchJoint; i < iLastArchJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iLastArchJoint), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iLastDeckJoint - 1), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iLastDeckJoint), pts.get(iAbutmentArchJoints + 1)));
        // Add the remaining verticals and diagonals.
        int iLeftTop = iFirstDeckJoint + 1;
        int iRightTop = iLastDeckJoint - 1;
        int iLeftBottom = iFirstArchJoint;
        int iRightBottom = iLastArchJoint;
        while (iLeftTop < iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iLeftTop + 1), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop - 1), pts.get(iRightBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom)));
            iLeftTop++;
            iRightTop--;
            iLeftBottom++;
            iRightBottom--;
        }
        // Center vertical, if any.
        if (iLeftTop == iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
        }
        closeSketch();
        return this;
    }

    /**
     * Set this sketch to be a standard Warren continuous arch based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setWarrenContArch(DesignConditions conditions) {
        setNameFromResource("warrenContArch.text");
        setPrescribedJoints(conditions);
        int iAbutmentArchJoints = conditions.getArchJointIndex();
        final int iFirstDeckJoint = 0;
        final int iLastDeckJoint = conditions.getNPanels();
        // p1 and p2 are used to find a parabolic arc.
        Affine.Point p1 = pts.get(iAbutmentArchJoints);
        Affine.Point p2 = pts.get((iFirstDeckJoint + iLastDeckJoint) / 2);
        Affine.Point p3 = pts.get(iAbutmentArchJoints + 1);
        double xMid = 0.5 * (p1.x + p3.x);
        double x1 = p1.x - xMid;
        double y1 = p1.y;
        double x2 = p2.x - xMid;
        // Peak of arc is 1.0 meters below deck.
        // double y2 = p2.y - 1.0;
        // Peak of arc is 1/4 of arch height below deck.
        double y2 = p2.y - 0.25 * (p2.y - p1.y);
        // Find coefficients for y = a * x^2 + b;
        double a = (y2 - y1) / (x2 * x2 - x1 * x1);
        double b = y1 - a * x1 * x1;
        // Add joints on the parabola, one beneath each deck joint.
        final int iFirstArchJoint = pts.size();
        for (int i = iFirstDeckJoint; i < iLastDeckJoint; i++) {
            double x0 = pts.get(i).x + 0.5 * DesignConditions.panelSizeWorld;
            double x = x0 - xMid;
            double y = a * x * x + b;
            pts.add(new Affine.Point(x0, roundToScale(y, gridScale)));
        }
        final int iLastArchJoint = pts.size() - 1;
        // Add arch memberLocations and also the two verticals between deck and arch at each abutment.
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints), pts.get(iFirstArchJoint)));
        for (int i = iFirstArchJoint; i < iLastArchJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iLastArchJoint), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints + 1), pts.get(iLastDeckJoint)));
        int iDeck = iFirstDeckJoint;
        int iArch = iFirstArchJoint;
        while (iDeck < iLastDeckJoint) {
            mrs.add(new SketchMember(pts.get(iDeck), pts.get(iArch)));
            mrs.add(new SketchMember(pts.get(iDeck + 1), pts.get(iArch)));
            iDeck++;
            iArch++;
        }
        closeSketch();
        return this;
    }

    /**
     * Set this sketch to be a standard Pratt 3-hinge arch based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setPratt3HingeArch(DesignConditions conditions) {
        setNameFromResource("pratt3HingeArch.text");
        setPrescribedJoints(conditions);
        int iAbutmentArchJoints = conditions.getArchJointIndex();
        final int iFirstDeckJoint = 0;
        final int iLastDeckJoint = conditions.getNPanels();
        // p1 and p2 are used to find a parabolic arc.
        Affine.Point p1 = pts.get(iAbutmentArchJoints);
        Affine.Point p2 = pts.get((iFirstDeckJoint + iLastDeckJoint) / 2);
        Affine.Point p3 = pts.get(iAbutmentArchJoints + 1);
        double xMid = 0.5 * (p1.x + p3.x);
        double x1 = p1.x - xMid;
        double y1 = p1.y;
        double x2 = p2.x - xMid;
        // double y2 = p2.y - 1.0;
        // Peak of arc is 1/4 of arch height below deck.
        double y2 = p2.y - 0.25 * (p2.y - p1.y);
        // Find coefficients for y = a * x^2 + b;
        double a = (y2 - y1) / (x2 * x2 - x1 * x1);
        double b = y1 - a * x1 * x1;
        // Add joints on the parabola, one beneath each deck joint.
        final int iFirstArchJoint = pts.size();
        int iArchJointLeftOfHinge = 0;
        int iHinge = (iFirstDeckJoint + iLastDeckJoint) / 2;
        for (int i = iFirstDeckJoint + 1; i < iLastDeckJoint; i++) {
            if (i == iHinge) {
                iArchJointLeftOfHinge = pts.size() - 1;
            } else {
                double x = pts.get(i).x - xMid;
                double y = a * x * x + b;
                pts.add(new Affine.Point(pts.get(i).x, roundToScale(y, gridScale)));
            }
        }
        final int iLastArchJoint = pts.size() - 1;
        // Add arch memberLocations and also the two verticals between deck and arch at each abutment.
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints), pts.get(iFirstArchJoint)));
        for (int i = iFirstArchJoint; i < iArchJointLeftOfHinge; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iArchJointLeftOfHinge), pts.get(iHinge)));
        mrs.add(new SketchMember(pts.get(iHinge), pts.get(iArchJointLeftOfHinge + 1)));
        for (int i = iArchJointLeftOfHinge + 1; i < iLastArchJoint; i++) {
            mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
        }
        mrs.add(new SketchMember(pts.get(iLastArchJoint), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints + 1), pts.get(iLastDeckJoint)));
        // Add the remaining verticals and diagonals.
        int iLeftTop = iFirstDeckJoint + 1;
        int iRightTop = iLastDeckJoint - 1;
        int iLeftBottom = iFirstArchJoint;
        int iRightBottom = iLastArchJoint;
        while (iLeftTop < iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iLeftTop - 1), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop + 1), pts.get(iRightBottom)));
            iLeftTop++;
            iRightTop--;
            iLeftBottom++;
            iRightBottom--;
        }
        closeSketch();
        return this;
    }

    /**
     * Set this sketch to be a standard Howe 3-hinged arch based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setHowe3HingeArch(DesignConditions conditions) {
        setNameFromResource("howe3HingeArch.text");
        setPrescribedJoints(conditions);
        int iAbutmentArchJoints = conditions.getArchJointIndex();
        final int iFirstDeckJoint = 0;
        final int iLastDeckJoint = conditions.getNPanels();
        // p1 and p2 are used to find a parabolic arc.
        Affine.Point p1 = pts.get(iAbutmentArchJoints);
        Affine.Point p2 = pts.get((iFirstDeckJoint + iLastDeckJoint) / 2);
        Affine.Point p3 = pts.get(iAbutmentArchJoints + 1);
        double xMid = 0.5 * (p1.x + p3.x);
        double x1 = p1.x - xMid;
        double y1 = p1.y;
        double x2 = p2.x - xMid;
        // Peak of arc is 3/4 of arch height.
        double y2 = p2.y - 0.25 * (p2.y - p1.y);
        // Find coefficients for y = a * x^2 + b;
        double a = (y2 - y1) / (x2 * x2 - x1 * x1);
        double b = y1 - a * x1 * x1;
        // Add joints on the parabola, one beneath each deck joint.
        final int iFirstArchJoint = pts.size();
        for (int i = iFirstDeckJoint + 1; i < iLastDeckJoint; i++) {
            if (i != (iFirstDeckJoint + iLastDeckJoint) / 2) {
                double x = pts.get(i).x - xMid;
                double y = a * x * x + b;
                pts.add(new Affine.Point(pts.get(i).x, roundToScale(y, gridScale)));
            }
        }
        final int iLastArchJoint = pts.size() - 1;
        // Add arch memberLocations and also the two verticals between deck and arch at each abutment.
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint + 1), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iFirstArchJoint), pts.get(iAbutmentArchJoints)));
        int iLeftArch = iFirstArchJoint + 1;
        int iRightArch = iLastArchJoint - 1;
        while (iLeftArch < iRightArch) {
            mrs.add(new SketchMember(pts.get(iLeftArch - 1), pts.get(iLeftArch)));
            mrs.add(new SketchMember(pts.get(iRightArch + 1), pts.get(iRightArch)));
            iLeftArch++;
            iRightArch--;
        }
        mrs.add(new SketchMember(pts.get(iLastArchJoint), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iLastDeckJoint - 1), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iLastDeckJoint), pts.get(iAbutmentArchJoints + 1)));
        // Add the remaining verticals and diagonals.
        int iLeftTop = iFirstDeckJoint + 1;
        int iRightTop = iLastDeckJoint - 1;
        int iLeftBottom = iFirstArchJoint;
        int iRightBottom = iLastArchJoint;
        while (iLeftTop < iRightTop) {
            mrs.add(new SketchMember(pts.get(iLeftTop), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iLeftTop + 1), pts.get(iLeftBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop - 1), pts.get(iRightBottom)));
            mrs.add(new SketchMember(pts.get(iRightTop), pts.get(iRightBottom)));
            iLeftTop++;
            iRightTop--;
            iLeftBottom++;
            iRightBottom--;
        }
        closeSketch();
        return this;
    }

    /**
     * Set this sketch to be a standard Warren 3-hinged arch based on the given design conditions.
     * 
     * @param conditions design conditions
     * @return bridge sketch
     */
    public BridgeSketchModel setWarren3HingeArch(DesignConditions conditions) {
        setNameFromResource("warren3HingeArch.text");
        setPrescribedJoints(conditions);
        int iAbutmentArchJoints = conditions.getArchJointIndex();
        final int iFirstDeckJoint = 0;
        final int iLastDeckJoint = conditions.getNPanels();
        // p1 and p2 are used to find a parabolic arc.
        Affine.Point p1 = pts.get(iAbutmentArchJoints);
        Affine.Point p2 = pts.get((iFirstDeckJoint + iLastDeckJoint) / 2);
        Affine.Point p3 = pts.get(iAbutmentArchJoints + 1);
        double xMid = 0.5 * (p1.x + p3.x);
        double x1 = p1.x - xMid;
        double y1 = p1.y;
        double x2 = p2.x - xMid;
        // Peak of arc is 1.0 meters below deck.
        // double y2 = p2.y - 1.0;
        // Peak of arc is 1/4 of arch height below deck.
        double y2 = p2.y - 0.25 * (p2.y - p1.y);
        // Find coefficients for y = a * x^2 + b;
        double a = (y2 - y1) / (x2 * x2 - x1 * x1);
        double b = y1 - a * x1 * x1;
        // Add joints on the parabola, one beneath each deck joint.
        final int iFirstArchJoint = pts.size();
        for (int i = iFirstDeckJoint; i < iLastDeckJoint; i++) {
            double x0 = pts.get(i).x + 0.5 * DesignConditions.panelSizeWorld;
            double x = x0 - xMid;
            double y = a * x * x + b;
            pts.add(new Affine.Point(x0, roundToScale(y, gridScale)));
        }
        final int iLastArchJoint = pts.size() - 1;
        // Add arch memberLocations and also the two verticals between deck and arch at each abutment.
        mrs.add(new SketchMember(pts.get(iFirstDeckJoint), pts.get(iAbutmentArchJoints)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints), pts.get(iFirstArchJoint)));
        for (int i = iFirstArchJoint; i < iLastArchJoint; i++) {
            // Skip the middle member to make the hinge.
            if (i != (iFirstArchJoint + iLastArchJoint) / 2) {
                mrs.add(new SketchMember(pts.get(i), pts.get(i + 1)));
            }
        }
        mrs.add(new SketchMember(pts.get(iLastArchJoint), pts.get(iAbutmentArchJoints + 1)));
        mrs.add(new SketchMember(pts.get(iAbutmentArchJoints + 1), pts.get(iLastDeckJoint)));
        int iDeck = iFirstDeckJoint;
        int iArch = iFirstArchJoint;
        while (iDeck < iLastDeckJoint) {
            mrs.add(new SketchMember(pts.get(iDeck), pts.get(iArch)));
            mrs.add(new SketchMember(pts.get(iDeck + 1), pts.get(iArch)));
            iDeck++;
            iArch++;
        }
        closeSketch();
        return this;
    }

    /**
     * Set the sketch from a template sketch represented as a string.
     * 
     * @param name name of the template
     * @param template string representation of the bridge sketch
     * @return bridge sketch
     */
    public BridgeSketchModel setFromTemplate(String name, String template) {
        this.name = name;
        String[] tokens = template.split("\\|");
        int i = 0;
        // Give up if the template does not match the attached conditions.
        conditions = DesignConditions.getDesignConditions(tokens[i++]);
        // Joint and member counts.
        int nJoints = Integer.parseInt(tokens[i++]);
        jointLocations = new Affine.Point[nJoints];
        int nMembers = Integer.parseInt(tokens[i++]);
        memberLocations = new SketchMember[nMembers];
        for (int j = 0; j < nJoints; j++) {
            double x = Double.parseDouble(tokens[i++]);
            double y = Double.parseDouble(tokens[i++]);
            jointLocations[j] = new Affine.Point(x, y);
        }
        for (int j = 0; j < nMembers; j++) {
            // -1 is due to 1-based joint numbering convention.
            int ia = Integer.parseInt(tokens[i++]) - 1;
            int ib = Integer.parseInt(tokens[i++]) - 1;
            memberLocations[j] = new SketchMember(jointLocations[ia], jointLocations[ib]);
        }
        return this;
    }
    /**
     * Cache of previously computed bridge sketch models indexd on design condition tag number (first two
     * numeric characters of tag).  The final alpha for deck and load conditions is not included.
     */
    private static final HashMap<String, Object[]> sketchModelListCache = new HashMap<String, Object[]>();

    /**
     * A factory for lists of bridge sketch models applicable to given conditions.  If the tag number of the
     * given conditions (ignoring the final alpha for site and load conditions) is the same as for a 
     * previous call, then the same array of sketch models is returned. Arrays returned are suitable for a
     * Swing list model because the string representation of a sketch is just its name.
     * 
     * @param conditions design conditions to retrieve applicable bridge sketches for
     * @return array of sketch models.  The first element is always a string &lt;none&gt; (or other-language
     * equivalent, so the list is a suitable input for setListData() of a JList.
     */
    public static Object[] getList(DesignConditions conditions) {
        if (conditions == null) {
            return new Object [0];
        }
        final String conditionsTagNumber = conditions.getTag().substring(0, 2);
        Object[] rtn = sketchModelListCache.get(conditionsTagNumber);
        if (rtn == null) {
            // Cache miss.  Fill a vector with applicable templates.
            final ArrayList<Object> list = new ArrayList<Object>();
            // Add placeholder for <none> entry.
            list.add(BDApp.getResourceMap(BridgeSketchModel.class).getString("noTemplate.text"));
            // Add all available computed sketches.
            if (conditions.isArch()) {
                if (conditions.getUnderClearance() <= 16) {
                    list.add(new BridgeSketchModel().setPrattContArch(conditions));
                    list.add(new BridgeSketchModel().setHoweContArch(conditions));
                    list.add(new BridgeSketchModel().setWarrenContArch(conditions));
                    // Allow for 3-hinge computed arches if odd number of deck joints.
                    if (conditions.getNLoadedJoints() % 2 == 1) {
                        list.add(new BridgeSketchModel().setPratt3HingeArch(conditions));
                        list.add(new BridgeSketchModel().setHowe3HingeArch(conditions));
                        list.add(new BridgeSketchModel().setWarren3HingeArch(conditions));
                    }
                }
            } else if (conditions.getNAnchorages() == 0 && !conditions.isPier()) {
                if (conditions.getOverClearance() >= DesignConditions.panelSizeWorld) {
                    list.add(new BridgeSketchModel().setHoweThruTruss(conditions));
                    list.add(new BridgeSketchModel().setPrattThruTruss(conditions));
                    list.add(new BridgeSketchModel().setWarrenThruTruss(conditions));                    
                }
                if (conditions.getUnderClearance() >= DesignConditions.panelSizeWorld) {
                    list.add(new BridgeSketchModel().setHoweDeckTruss(conditions));
                    list.add(new BridgeSketchModel().setPrattDeckTruss(conditions));
                    list.add(new BridgeSketchModel().setWarrenDeckTruss(conditions));
                }
            } else if (conditions.getNAnchorages() > 0 && !conditions.isPier() && 
                    conditions.getUnderClearance() >= DesignConditions.panelSizeWorld / 2) {
                list.add(new BridgeSketchModel().setCableStayedWarrenTruss(conditions));
                list.add(new BridgeSketchModel().setSuspendedWarrenTruss(conditions));
            }
            ResourceMap resourceMap = BDApp.getResourceMap(BridgeSketchModel.class);
            Iterator<String> i = resourceMap.keySet().iterator();
            while (i.hasNext()) {
                String nameKey = i.next();
                if (nameKey.endsWith(".bridgeSketchName") && conditionsTagNumber.equals(nameKey.substring(0, 2))) {
                    String sketchKey = nameKey.substring(0, nameKey.lastIndexOf('.')).concat(".bridgeSketch");
                    list.add(new BridgeSketchModel().setFromTemplate(resourceMap.getString(nameKey), resourceMap.getString(sketchKey)));
                }
            }
            // Add all the resource sketches for these design conditions, neglecting deck and load conditions.
            rtn = list.toArray();
            sketchModelListCache.put(conditionsTagNumber, rtn);
        }
        return rtn;
    }

    /**
     * Return a string representation of this template sketch, which is just its name.
     * @return string representation
     */
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * Development test main for printing conditions that don't have any templates at all.
     * 
     * @param argv unused parameters
     */
    public static void main(String [] argv) {
        for (int i = 0; i < DesignConditions.conditions.length; i++) {
            Object [] list = getList(DesignConditions.conditions[i]);
            if (list.length <= 1) {
                System.out.println(DesignConditions.conditions[i].getTag() + ": " + list.length);
            }
        }
        System.out.println();
    }
}
