/*
 * DesignConditions.java
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Design conditions for a bridge and the singleton table of all standard design conditions.
 *
 * @author Eugene K. Ressler
 */
public class DesignConditions implements Cloneable {

    private final String tag;
    private final byte[] code;
    private long codeLong;
    private boolean hiPier;
    private int leftAnchorageJointIndex;
    private int rightAnchorageJointIndex;
    private int pierPanelIndex;
    private double underClearance;
    private double overClearance;
    private double overMargin;
    private int nPanels;
    private int nPrescribedJoints;
    private int loadType;
    private int deckType;
    private double deckElevation;
    private double archHeight;
    private double pierHeight;
    private int nAnchorages;
    private double excavationVolume;
    private double abutmentCost;
    private double pierCost;
    private double deckCostRate;
    private double totalFixedCost;
    private int pierJointIndex;
    private int archJointIndex;
    private double spanLength;
    private int nLoadedJoints;
    private int nJointRestraints;
    private Joint[] prescribedJoints;
    private int [] abutmentJointIndices;
    private double xLeftmostDeckJoint;
    private double xRightmostDeckJoint;
    private double allowableSlenderness;

    /**
     * Horizontal distance from ends of deck and anchorage joints.
     */
    public static final double anchorOffset = 8.0;
    /**
     * Horizontal length of bridge panels.
     */
    public static final double panelSizeWorld = 4.0;
    /**
     * Depth of bridge gap in meters, before earthwork.
     */
    public static final double gapDepth = 24.0;
    /**
     * Required overhead clearance above gap.
     */
    public static final double minOverhead = 8.0;
    /**
     * Maximum slenderness allowed in non-anchorage bridges.
     */
    public static final double maxSlenderness = 300.0;
    /**
     * Integer value corresponding to standard truck load.
     */
    public static final int STANDARD_TRUCK = 0;
    /**
     * Integer value corresponding to heavy truck load.
     */
    public static final int HEAVY_TRUCK = 1;
    /**
     * Integer value corresponding to medium strength deck.
     */
    public static final int MEDIUM_STRENGTH_DECK = 0;
    /**
     * Integer value corresponding to high strength deck.
     */
    public static final int HI_STRENGTH_DECK = 1;
    /**
     * Maximum allowable number of joints in a bridge.
     */
    public static final int maxJointCount = 100;
    /**
     * Maximum allowable number of members in a bridge.
     */
    public static final int maxMemberCount = 200;
    /**
     * Tag to use when a key code is used to construct design conditions.
     */
    public static final String fromKeyCodeTag = "99Z";
    /**
     * Dollars per cubic meter of earthwork.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double excavationCostRate = 1.0;
    /**
     * Cost of an anchorage.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double anchorageCost = 6000.00;
    /**
     * Cost of one panel length of of medium strength deck.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double deckCostPerPanelMedStrength = 4700;
    /**
     * Cost of one panel length of of high strength deck.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double deckCostPerPanelHiStrength = 5100;
    /**
     * Basic cost of a standard abutment.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double standardAbutmentBaseCost = 6000;
    /**
     * Cost increment for every bridge panel supported by a standard abutment.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double standardAbutmentIncrementalCostPerDeckPanel = 500;
    /**
     * Cost increment for every bridge panel supported by an arch abutment.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double archIncrementalCostPerDeckPanel = 3300;
    /**
     * Cost increment arch.  Parameter A in Ax^2+Bx+C where x is arch height.
     * Make an integral number of cents to avoid roundoff problems.
     */
    // No longer used: public static final double archCostPerMeterHeightParamA = 41.10;
    /**
     * Cost increment arch.  Parameter B in Ax^2+Bx+C where x is arch height.
     * Make an integral number of cents to avoid roundoff problems.
     */
    // No longer used: public static final double archCostPerMeterHeightParamB = 1605.7;
    /**
     * Cost increment arch.  Parameter C in Ax^2+Bx+C where x is arch height.
     * Make an integral number of cents to avoid roundoff problems.
     */
    // No longer used: public static final double archCostPerMeterHeightParamC = -7077;
    /**
     * Cost increment per deck pan for pier support.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double pierIncrementalCostPerDeckPanel = 4500;
    /**
     * Cost increment for every meter of pier height.
     * Make an integral number of cents to avoid roundoff problems.
     */
    public static final double pierBaseCost = 0;
    /**
     * Basic cost of a pier.
     * Make an integral number of cents to avoid roundoff problems.
     */
    // No longer used: public static final double pierIncrementalCostPerMeterHeight = 700;
    /**
     * Conversion table taking a deck elevation index to an excavation volume.
     */
    public static final double [] deckElevationIndexToExcavationVolume = { 106500, 90000, 71500, 54100, 38100, 19400, 0 };
    /**
     * Conversion table taking a deck elevation index to
     * an abutment cost for case of keycode design conditions with piers.
     */
    public static final double [] deckElevationIndexToKeycodeAbutmentCosts = { 7000, 7000, 7500, 7500, 8000, 8000, 8500 };

    /**
     * Return the abutment cost for these conditions.
     *
     * @return abutment cost
     */
    public double getAbutmentCost() {
        return abutmentCost;
    }

    /**
     * Return true iff these conditions have arch abutments.
     *
     * @return true iff the conditions have arch abutments
     */
    public boolean isArch() {
        return archHeight >= 0;
    }

    /**
     * Return the height of the step of arch abutments or -1 if none.
     *
     * @return arch height
     */
    public double getArchHeight() {
        return archHeight;
    }

    /**
     * Get the design conditions code as a byte string.
     *
     * @return design conditions code
     */
    public byte[] getCode() {
        return code;
    }

    /**
     * Return the design conditions code as a string.
     *
     * @return design conditions code
     */
    public String getCodeString() {
        return String.format("%010d", codeLong);
    }

    /**
     * Return the design conditions code as a long.
     *
     * @return design conditions code
     */
    public long getCodeLong() {
        return codeLong;
    }

    /**
     * Return the deck cost per panel.
     *
     * @return cost rate
     */
    public double getDeckCostRate() {
        return deckCostRate;
    }

    /**
     * Return the deck elevation as listed in setup wizard.
     *
     * @return deck elevation
     */
    public double getDeckElevation() {
        return deckElevation;
    }

    /**
     * Return the deck type as an integer constant, either
     * <code>MEDIUM_STRENGTH_DECK</code> or <code>HI_STRENGTH_DECK</code>.
     *
     * @return deck type
     */
    public int getDeckType() {
        return deckType;
    }

    /**
     * Return the thickness of the deck in meters.
     *
     * @return deck thickness
     */
    public double getDeckThickness() {
        return (deckType == MEDIUM_STRENGTH_DECK) ? 0.23 : 0.15;
    }

    /**
     * Return the excavation volume in cubic meters.
     *
     * @return excavation volume
     */
    public double getExcavationVolume() {
        return excavationVolume;
    }

    /**
     * Return the excavation cost.
     *
     * @return excavation cost
     */
    public double getExcavationCost() {
        return excavationVolume * excavationCostRate;
    }

    /**
     * Return true iff these design conditions include a pier.
     *
     * @return true iff there is a pier
     */
    public boolean isPier() {
        return pierPanelIndex >= 0;
    }

    /**
     * Return true iff these design conditions include a highpier.
     *
     * @return true iff there is a high pier
     */
    public boolean isHiPier() {
        return hiPier;
    }

    /**
     * Return true iff these design conditions include a left shore anchorage.
     *
     * @return true iff there is a left shore anchorage
     */
    public boolean isLeftAnchorage() {
        return leftAnchorageJointIndex >= 0;
    }

    /**
     * Return true iff these design conditions include a right shore anchorage.
     *
     * @return true iff there is a right shore anchorage
     */
    public boolean isRightAnchorage() {
        return rightAnchorageJointIndex >= 0;
    }

    /**
     * Get the type of load in these design conditions as an integer constant,
     * either <code>HEAVY_TRUCK</code> or <code>STANDARD_TRUCK</code>
     *
     * @return load type
     */
    public int getLoadType() {
        return loadType;
    }

    /**
     * Number of anchorages, 1 or 2, in these design conditions.
     *
     * @return number of anchorages
     */
    public int getNAnchorages() {
        return nAnchorages;
    }

    /**
     * Number of panels in the bridge in these design conditions.
     *
     * @return number of panels
     */
    public int getNPanels() {
        return nPanels;
    }

    /**
     * Number of panels in the bridge in these design conditions.
     *
     * @return number of prescribed joints
     */
    public int getNPrescribedJoints() {
        return nPrescribedJoints;
    }

    /**
     * Height over deck that joints can be placed.
     *
     * @return clearance over deck
     */
    public double getOverClearance() {
        return overClearance;
    }

    /**
     * Return height of entire design space above deck.
     *
     * @return vertical margin in meters
     */
    public double getOverMargin() {
        return overMargin;
    }

    /**
     * Return pier cost in dollars.
     *
     * @return pier cost
     */
    public double getPierCost() {
        return pierCost;
    }

    /**
     * Return pier height.
     *
     * @return pier height
     */
    public double getPierHeight() {
        return pierHeight;
    }

    /**
     * Return index of joint over pier.
     *
     * @return index of pier joint
     */
    public int getPierPanelIndex() {
        return pierPanelIndex;
    }

    /**
     * Return the 3-character tag for this scenario.
     *
     * @return tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Return the total fixed cost.
     *
     * @return total fixed cost
     */
    public double getTotalFixedCost() {
        return totalFixedCost;
    }

    /**
     * Return the under clearance in meters.
     *
     * @return under clearance in meters
     */
    public double getUnderClearance() {
        return underClearance;
    }

    /**
     * Return true iff the bridge is at grade level.
     *
     * @return true iff the bridge is at grade level
     */
    public boolean isAtGrade() {
        return deckElevation == gapDepth;
    }

    /**
     * Return index of left anchorage joint or -1 if none.
     *
     * @return left anchorage joint
     */
    public int getLeftAnchorageJointIndex() {
        return leftAnchorageJointIndex;
    }

    /**
     * Return index of right anchorage joint or -1 if none.
     *
     * @return left anchorage joint
     */
    public int getRightAnchorageJointIndex() {
        return rightAnchorageJointIndex;
    }

    /**
     * Return index of first arch joint.  Second is one more.
     *
     * @return first arch joint index
     */
    public int getArchJointIndex() {
        return archJointIndex;
    }

    /**
     * Return index of pier joint.
     *
     * @return pier joint index
     */
    public int getPierJointIndex() {
        return pierJointIndex;
    }

    /**
     * Return an array containing the abutment joint indices.
     *
     * @return abutment joint indices
     */
    public int[] getAbutmentJointIndices() {
        return abutmentJointIndices;
    }

    /**
     * Return x-coordinate of the leftmost deck joint.
     *
     * @return x-ccordinate of leftmost deck joint
     */
    public double getXLeftmostDeckJoint() {
        return xLeftmostDeckJoint;
    }

    /**
     * Return x-coordinate of the rightmost deck joint.
     *
     * @return x-ccordinate of rightmost deck joint
     */
    public double getXRightmostDeckJoint() {
        return xRightmostDeckJoint;
    }

    /**
     * Return the i'th prescribed joint.
     *
     * @param i index of prescribed joint
     * @return prescribed joint
     */
    public Joint getPrescribedJoint(int i) {
        return prescribedJoints[i];
    }

    /**
     * Return the location of the i'th prescribed joint.
     *
     * @param i index of prescribed joint
     * @return location of joint
     */
    public Affine.Point getPrescribedJointLocation(int i) {
        return prescribedJoints[i].getPointWorld();
    }

    /**
     * Return the number of loaded joints.
     *
     * @return number of loaded joints
     */
    public int getNLoadedJoints() {
        return nLoadedJoints;
    }

    /**
     * Return the length of the bridge span in meters.
     *
     * @return span length
     */
    public double getSpanLength() {
        return spanLength;
    }

    /**
     * Return the total number of joint restraints due to abutments and pier.
     *
     * @return number of restraints
     */
    public int getNJointRestraints() {
        return nJointRestraints;
    }

    /**
     * Return the allowable slenderness for this scenario.  Infinity if there
     * are anchorages, else 300.
     *
     * @return allowable slenderness ratio
     */
    public double getAllowableSlenderness() {
        return allowableSlenderness;
    }

    /**
     * Return  design conditions based on a key code.  This will return
     * the standard conditions from the table if the code corresponds.
     * Else it creates a new code dynamically.
     *
     * @param keyCode key code for design conditions
     * @return design conditions
     */
    public static DesignConditions fromKeyCode(String keyCode) {
        long codeLong = 0;
        try {
            codeLong = Long.parseLong(keyCode);
        }
        catch (NumberFormatException ex) {
            return null;
        }
        int errCode = getCodeError(getCode(codeLong));
        if (errCode != 0) {
            return null;
        }
        return getDesignConditions(codeLong);
    }

    /**
     * Return true iff these design conditions were constructed from a key code.
     *
     * @return true iff these design conditions were constructed from a key code
     */
    public final boolean isFromKeyCode() {
        return tag == fromKeyCodeTag;
    }

    private static boolean inRange(int b, int lo, int hi) {
        return lo <= b && b <= hi;
    }

    /*
     * Character 1 - Load case scenario (1=Case A, 2=Case B, 3=Case C, 4=Case D); entry of any character other than 1, 2, 3, or 4 is illegal.
     * Characters 2,3 - Span length, expressed as the number of 4-meter panels; any integer from 1 to 20 is allowed
     * Characters 4,5 - upper height of the design space, expressed as number of meters over the deck level; any integer from 0 to 40 is allowed
     * Characters 6,7 - lower height of the design space, expressed as number of meters below the deck level; any integer from 0 to 32 is allowed
     * Character 8 - arch & anchorage status (0=no arch, no anchorage; 1=arch, no anchorage; 2=no arch, single left-side anchorage; 3=no arch,
     *   two anchorages).  Base of arch is  always at bottom of drawing space.
     * Character 9 - Intermediate pier location, expressed as the numbered deck-level joint(i.e., 1=left end of bridge; 2=4 meters from left end; 3=8 meters from left end...)
     * Character 10 - Hi interior pier (0=not a high pier, elevation of pier top is the bottom of drawing space; 1=high pier, elevation is at deck level)
     * A few nuances:
     * - It is possible to have arch supports and an intermediate pier, even though there is no standard scenario that includes both.
     * - It is possible to put an intermediate pier at the left end of the bridge (even though this doesn't really make sense)
     * - For an arch, deck height is driven by the specified span length (char 2-3), the lower height of the design space (char 6,7), and the
     *   shape of the river valley.  Thus it is possible to have a deck level above the normal roadway level.  Extreme cases of this (which I have
     *   never bothered to make illegal) push the deck completely off the screen.
     */
    private static int getCodeError(byte [] code) {
        if (code == null) {
            return -1;
        }
        // Character 1 - Load case scenario (1=Case A, 2=Case B, 3=Case C, 4=Case D); entry of any character other than 1, 2, 3, or 4 is illegal.
        if (!inRange(code[1 - 1], 1, 4)) {
            return 1;
        }
        // Characters 2,3 - Span length, expressed as the number of 4-meter panels; any integer from 1 to 20 is allowed
        final int nPanels = 10 * (int) code[2 - 1] + code[3 - 1];
        if (!inRange(nPanels, 1, 20)) {
            return 2;
        }
        // Characters 4,5 - upper height of the design space, expressed as number of meters over the deck level; any integer from 0 to 40 is allowed
        final int over = 10 * (int) code[4 - 1] + code[5 - 1];
        if (!inRange(over, 0, 40)) {
            return 4;
        }
        if (!inRange(code[10 - 1], 0, 1)) {
            return 10;
        }
        // Characters 6,7 - lower height of the design space, expressed as number of meters below the deck level; any integer from 0 to 32 is allowed
        final int under = 10 * (int) code[6 - 1] + code[7 - 1];
        if (!inRange(under, 0, 32)) {
            return 6;
        }
        // Character 8 - arch & anchorage status (0=no arch, no anchorage; 1=arch, no anchorage; 2=no arch, single left-side anchorage; 3=no arch,
        // two anchorages).  Base of arch is always at bottom of drawing space.
        if (!inRange(code[8 - 1], 0, 3)) {
            return 8;
        }
        final boolean arch = (code[8 - 1] == 1);
        // Character 9 - Intermediate pier location, expressed as the numbered deck-level joint(i.e., 1=left end of bridge; 2=4 meters from left end; 3=8 meters from left end...)
        final int pierPanelIndex = code[9 - 1] - 1;
        final boolean pier = pierPanelIndex >= 0;
        // Character 10 - Hi interior pier (0=not a high pier, elevation of pier top is the bottom of drawing space; 1=high pier, elevation is at deck level)
        final boolean hiPier = code[10 - 1] > 0;

        // Consistency checks.
        if (hiPier && !pier) {
            return 90;
        }
        if (pierPanelIndex >= nPanels) {
            return 91;
        }

        // 1.  Span length constrained to 5 through 11 panels.  This takes the deck from just above the water to existing
        // grade level.  It also ensures arch supports are above water.
        if (nPanels < 5 || nPanels > 11) {
            return 92;
        }

        // 2.  Constrain deck elevation to 0 meters (24 meters below grade) through 24 meters (at grade).  These are as
        // measured in the Wizard.
        final double deckElev = arch ? 4 * (nPanels - 5) + under : 4 * (nPanels - 5);
        if (deckElev < 0 || deckElev > gapDepth) {
            return 93;
        }

        // 3.  Restrict upper design space height (above deck level) so that DeckElevation + UpperDesignSpaceHeight <= 32.
        // This is consistent with the power line height specified in the scenario and prevents graphical messes in the
        // drafting view of the bridge.
        if (deckElev + over > gapDepth + minOverhead) {
            return 94;
        }

        // 4.  For non-arches, restrict lower design space height (below deck level) so that
        // DeckElevation - LowerDesignSpaceHeight >= 0.  Prevents structure being under water.
        // (For arches, lower design space height is implicitly restricted by the deck elevation constraint 1.)
        if (!arch && deckElev - under < 0) {
            return 95;
        }

        // 5.  Restrict the pier to be in the range [left abutment joint + 4 .. right abutment joint - 4].
        // Prevents pier conflict with abutment.
        if (pier && pierPanelIndex == 0 || pierPanelIndex >= nPanels - 1) {
            return 96;
        }

        // 6.  Prevent both arch and pier, primarily because the cost model is not well-defined for this case.
        if (arch && pier) {
            return 97;
        }

        // 7.  Check that if high pier is specified a pier joint index is also given.  Simple consistency.
        // Done above.

        // 8.  Check low pier joint is above ground as follows.  If (xL,yL) is the left abutment joint and (xp,yp) is the
        // pier joint, then check
        //        xp >= xL + (yL - yp) * .5
        // Similarly check
        //        xp <= xR - (yR - yp) * .5
        // for the right abutment (xR, yR).
        if (pier && !hiPier) {
            final double xp = pierPanelIndex * panelSizeWorld;
            final double yp = deckElev - under;
            final double xL = 0;
            final double yL = deckElev;
            final double xR = nPanels * panelSizeWorld;
            final double yR = yL;
            if (xp < xL + (yL - yp) * 0.5) {
                return 98;
            }
            if (xp > xR - (yR - yp) * 0.5) {
                return 99;
            }
        }
        return 0;
    }

    private static byte [] getCode(long codeLong) {
        if (codeLong < 0) {
            return null;
        }
        byte [] rtnCode = new byte[10];
        for (int i = 9; i >= 0; i--) {
            rtnCode[i] = (byte) (codeLong % 10);
            codeLong /= 10;
        }
        if (codeLong > 0) {
            return null;
        }
        return rtnCode;
    }

    /**
     * Return true iff the other design conditions are geometrically the same,
     * i.e. may differ only in deck and load conditions.
     *
     * @param other other design conditions to compare with
     * @return result of comparison for geometrical equality
     */
    public boolean isGeometricallyIdentical(DesignConditions other) {
        if (other == null) {
            return false;
        }
        // code[0] contains the deck and load conditions; all others must be same.
        for (int i = 1; i < code.length; i++) {
            if (code[i] != other.code[i]) {
                return false;
            }
        }
        return true;
    }

    private static double [] underClearanceIndexToCost = { 200, 11300, 20800, 30300, 39000, 49700 };

    private double archCost(double underClearance) {
        return underClearanceIndexToCost[(int) underClearance / 4 - 1];
    }
    
    private static double [] pierHeightToCost =  { 0, 2800, 5600, 8400, 10200, 12500, 14800 };
    
    private double pierHeightCost(double pierHeight) {
        return pierHeightToCost[(int) pierHeight / 4];
    }
    
    /**
     * Constructor used to create the static table of standard conditions.
     *
     * @param tag tag for the conditions
     * @param codeLong code for the conditions
     */
    private DesignConditions(String tag, long codeLong) {
        this.tag = tag;
        this.codeLong = codeLong;
        this.code = getCode(codeLong);
        if (getCodeError(this.code) != 0) {
            return;
        }
        // code dependencies
        // digit 10 => (0 = low pier, 1 = high pier)
        hiPier = code[9] > 0;
        // digit 9 => panel point at which pier is located. (-1 = no pier).
        pierJointIndex = pierPanelIndex = code[8] - 1;
        final boolean pier = pierPanelIndex >= 0;
        // digit 8 => (0 = simple, 1 = arch, 2 = cable left, 3 = cable both)
        final boolean arch = (code[7] == 1);
        final boolean leftCable = (code[7] == 2 || code[7] == 3);
        final boolean rightCable = (code[7] == 3);
        // digits 6 and 7 => under span clearance
        underClearance = 10 * (int) code[5] + code[6];
        // digits 4 and 5 => overhead clearance
        overClearance = 10 * (int) code[3] + code[4];
        // digits 2 and 3 => number of bridge panels
        nPanels = 10 * (int) code[1] + code[2];
        // digit 1 is the load case, 1-based
        // -1 correction for 0-based load_case table
        int loadCaseIndex = code[0] - 1;
        loadType = (loadCaseIndex & 1) == 0 ? STANDARD_TRUCK : HEAVY_TRUCK;
        deckType = (loadCaseIndex & 2) == 0 ? MEDIUM_STRENGTH_DECK : HI_STRENGTH_DECK;

        ////////////////////////////
        // Second tier dependencies.
        ////////////////////////////

        // Work space dimensions.
        if (arch) {
            deckElevation = 4 * (nPanels - 5) + underClearance;
            archHeight = underClearance;
        }
        else {
            deckElevation =  4 * (nPanels - 5);
            archHeight = -1;
        }
        overMargin = gapDepth + minOverhead - deckElevation;
        pierHeight = hiPier ? deckElevation :
                     pier ? deckElevation - underClearance :
                     -1;

        // Prescribed joint information.
        nPrescribedJoints = nPanels + 1;
        archJointIndex = leftAnchorageJointIndex = rightAnchorageJointIndex = -1;
        // Add one prescribed joint for the intermediate support, if any.
        if (pier && !hiPier) {
            pierJointIndex = nPrescribedJoints;
            nPrescribedJoints++;
        }

        // Another two for the arch bases, if we have an arch.
        if (arch) {
            archJointIndex = nPrescribedJoints;
            nPrescribedJoints += 2;
        }
        // And more for the anchorages, if any.
        nAnchorages = 0;
        if (leftCable) {
            leftAnchorageJointIndex = nPrescribedJoints;
            nAnchorages++;
            nPrescribedJoints++;
        }
        if (rightCable) {
            assert leftCable;
            rightAnchorageJointIndex = nPrescribedJoints;
            nAnchorages++;
            nPrescribedJoints++;
        }

        spanLength = nPanels * panelSizeWorld;
        nLoadedJoints = nPanels + 1;
        prescribedJoints = new Joint[nPrescribedJoints];

        int i;
        double x = 0;
        double y = 0;
        for (i = 0; i < nLoadedJoints; i++) {
            prescribedJoints[i] = new Joint(i, new Affine.Point(x, y), true);
            x += panelSizeWorld;
        }
        xLeftmostDeckJoint = prescribedJoints[0].getPointWorld().x;
        xRightmostDeckJoint = prescribedJoints[nLoadedJoints - 1].getPointWorld().x;
        // Standard abutments, no pier, no anchorages = 3 restraints.
        nJointRestraints = 3;
        if (pier) {
            if (hiPier) {
                // Pier joint has 2, but we make the left support a roller.
                nJointRestraints += 2 - 1;
            }
            else {
                prescribedJoints[i] = new Joint(i, new Affine.Point(pierPanelIndex * panelSizeWorld, -underClearance), true);
                i++;
                nJointRestraints += 2;
            }
        }
        if (arch) {
            prescribedJoints[i] = new Joint(i, new Affine.Point(xLeftmostDeckJoint, -underClearance), true);
            i++;
            prescribedJoints[i] = new Joint(i, new Affine.Point(xRightmostDeckJoint, -underClearance), true);
            i++;
            // Both abutment joints are fully constrained, but the deck joints become unconstrained.
            nJointRestraints += 4 - 3;
        }
        if (leftCable) {
            prescribedJoints[i] = new Joint(i, new Affine.Point(xLeftmostDeckJoint - anchorOffset, 0), true);
            i++;
            nJointRestraints += 2;
        }
        if (rightCable) {
            prescribedJoints[i] = new Joint(i, new Affine.Point(xRightmostDeckJoint + anchorOffset, 0), true);
            i++;
            nJointRestraints += 2;
        }

        // Slenderness limit.
        allowableSlenderness = (leftCable || rightCable) ? 1e100 : maxSlenderness;

        // Cost calculations.
        excavationVolume = deckElevationIndexToExcavationVolume[(int)deckElevation / 4];
        deckCostRate = (deckType == MEDIUM_STRENGTH_DECK) ? deckCostPerPanelMedStrength : deckCostPerPanelHiStrength;

        // For the rest of the costs there are two separate models for standard and non-standard design conditions.
        if (isFromKeyCode()) {
            // Non-standard case.
            totalFixedCost = 170000.00;
            if (pier) {
                abutmentCost = deckElevationIndexToKeycodeAbutmentCosts[(int)deckElevation / 4];
                pierCost = totalFixedCost
                        - nPanels * deckCostRate
                        - excavationVolume * excavationCostRate
                        - abutmentCost
                        - nAnchorages * anchorageCost;
            }
            else {
                abutmentCost = totalFixedCost
                        - nPanels * deckCostRate
                        - excavationVolume * excavationCostRate
                        - nAnchorages * anchorageCost;
                pierCost = 0;
            }
        }
        else {
            // Standard case.
            abutmentCost =
                    // New for 2011: Quadratic arch site cost relationship.
                    arch ? nPanels * archIncrementalCostPerDeckPanel + archCost(underClearance) :
                    pier ? standardAbutmentBaseCost 
                            + Math.max(pierPanelIndex, nPanels - pierPanelIndex) * standardAbutmentIncrementalCostPerDeckPanel
                        : standardAbutmentBaseCost
                            + nPanels * standardAbutmentIncrementalCostPerDeckPanel;
            pierCost = pier ? Math.max(pierPanelIndex, nPanels - pierPanelIndex) * pierIncrementalCostPerDeckPanel
                            + pierHeightCost(pierHeight)
                            + pierBaseCost
                        : 0;
            totalFixedCost = excavationVolume * excavationCostRate +
                    abutmentCost +
                    pierCost +
                    nPanels * deckCostRate +
                    nAnchorages * anchorageCost;
        }
        abutmentCost *= 0.5; // Steve's calcs are for both abutments. UI presents unit cost.

        // Abutment joints.
        abutmentJointIndices =
                arch ? new int [] { 0, nPanels, archJointIndex, archJointIndex + 1 } :
                pier ? new int [] { 0, nPanels, pierJointIndex } :
                new int [] { 0, nPanels };
    }

    @Override
    public String toString() {
        return "["+tag+","+codeLong+
                ",hiPier="+hiPier +
                ",leftCableIndex="+leftAnchorageJointIndex+
                ",rightCableIndex="+rightAnchorageJointIndex+
                ",pierJointIndex="+pierPanelIndex+
                ",underClearance="+underClearance+
                ",overClearance="+overClearance+
                ",nPanels="+nPanels+
                ",loadType="+loadType+
                ",deckType="+deckType+
                ",deckElevation="+deckElevation+
                ",archHeight="+archHeight+
                ",pierHeight="+pierHeight+
                ",nAnchorages="+nAnchorages+
                ",excavationVolume="+excavationVolume+
                ",abutmentCost="+abutmentCost+
                ",pierCost="+pierCost+
                ",deckCostRate="+deckCostRate+
                ",totalFixedCost="+totalFixedCost+"]";
    }

    /**
     * Table of standard design conditions.
     */
    public static final DesignConditions conditions[] = {
        new DesignConditions("01A", 1110824000L),
        new DesignConditions("01B", 2110824000L),
        new DesignConditions("01C", 3110824000L),
        new DesignConditions("01D", 4110824000L),
        new DesignConditions("02A", 1101220000L),
        new DesignConditions("02B", 2101220000L),
        new DesignConditions("02C", 3101220000L),
        new DesignConditions("02D", 4101220000L),
        new DesignConditions("03A", 1091616000L),
        new DesignConditions("03B", 2091616000L),
        new DesignConditions("03C", 3091616000L),
        new DesignConditions("03D", 4091616000L),
        new DesignConditions("04A", 1082012000L),
        new DesignConditions("04B", 2082012000L),
        new DesignConditions("04C", 3082012000L),
        new DesignConditions("04D", 4082012000L),
        new DesignConditions("05A", 1072408000L),
        new DesignConditions("05B", 2072408000L),
        new DesignConditions("05C", 3072408000L),
        new DesignConditions("05D", 4072408000L),
        new DesignConditions("06A", 1062804000L),
        new DesignConditions("06B", 2062804000L),
        new DesignConditions("06C", 3062804000L),
        new DesignConditions("06D", 4062804000L),
        new DesignConditions("07A", 1053200000L),
        new DesignConditions("07B", 2053200000L),
        new DesignConditions("07C", 3053200000L),
        new DesignConditions("07D", 4053200000L),
        new DesignConditions("08A", 1110824200L),
        new DesignConditions("08B", 2110824200L),
        new DesignConditions("08C", 3110824200L),
        new DesignConditions("08D", 4110824200L),
        new DesignConditions("09A", 1101220200L),
        new DesignConditions("09B", 2101220200L),
        new DesignConditions("09C", 3101220200L),
        new DesignConditions("09D", 4101220200L),
        new DesignConditions("10A", 1091616200L),
        new DesignConditions("10B", 2091616200L),
        new DesignConditions("10C", 3091616200L),
        new DesignConditions("10D", 4091616200L),
        new DesignConditions("11A", 1082012200L),
        new DesignConditions("11B", 2082012200L),
        new DesignConditions("11C", 3082012200L),
        new DesignConditions("11D", 4082012200L),
        new DesignConditions("12A", 1072408200L),
        new DesignConditions("12B", 2072408200L),
        new DesignConditions("12C", 3072408200L),
        new DesignConditions("12D", 4072408200L),
        new DesignConditions("13A", 1062804200L),
        new DesignConditions("13B", 2062804200L),
        new DesignConditions("13C", 3062804200L),
        new DesignConditions("13D", 4062804200L),
        new DesignConditions("14A", 1053200200L),
        new DesignConditions("14B", 2053200200L),
        new DesignConditions("14C", 3053200200L),
        new DesignConditions("14D", 4053200200L),
        new DesignConditions("15A", 1110824300L),
        new DesignConditions("15B", 2110824300L),
        new DesignConditions("15C", 3110824300L),
        new DesignConditions("15D", 4110824300L),
        new DesignConditions("16A", 1101220300L),
        new DesignConditions("16B", 2101220300L),
        new DesignConditions("16C", 3101220300L),
        new DesignConditions("16D", 4101220300L),
        new DesignConditions("17A", 1091616300L),
        new DesignConditions("17B", 2091616300L),
        new DesignConditions("17C", 3091616300L),
        new DesignConditions("17D", 4091616300L),
        new DesignConditions("18A", 1082012300L),
        new DesignConditions("18B", 2082012300L),
        new DesignConditions("18C", 3082012300L),
        new DesignConditions("18D", 4082012300L),
        new DesignConditions("19A", 1072408300L),
        new DesignConditions("19B", 2072408300L),
        new DesignConditions("19C", 3072408300L),
        new DesignConditions("19D", 4072408300L),
        new DesignConditions("20A", 1062804300L),
        new DesignConditions("20B", 2062804300L),
        new DesignConditions("20C", 3062804300L),
        new DesignConditions("20D", 4062804300L),
        new DesignConditions("21A", 1053200300L),
        new DesignConditions("21B", 2053200300L),
        new DesignConditions("21C", 3053200300L),
        new DesignConditions("21D", 4053200300L),
        new DesignConditions("22A", 1100804100L),
        new DesignConditions("22B", 2100804100L),
        new DesignConditions("22C", 3100804100L),
        new DesignConditions("22D", 4100804100L),
        new DesignConditions("23A", 1090808100L),
        new DesignConditions("23B", 2090808100L),
        new DesignConditions("23C", 3090808100L),
        new DesignConditions("23D", 4090808100L),
        new DesignConditions("24A", 1080812100L),
        new DesignConditions("24B", 2080812100L),
        new DesignConditions("24C", 3080812100L),
        new DesignConditions("24D", 4080812100L),
        new DesignConditions("25A", 1070816100L),
        new DesignConditions("25B", 2070816100L),
        new DesignConditions("25C", 3070816100L),
        new DesignConditions("25D", 4070816100L),
        new DesignConditions("26A", 1060820100L),
        new DesignConditions("26B", 2060820100L),
        new DesignConditions("26C", 3060820100L),
        new DesignConditions("26D", 4060820100L),
        new DesignConditions("27A", 1050824100L),
        new DesignConditions("27B", 2050824100L),
        new DesignConditions("27C", 3050824100L),
        new DesignConditions("27D", 4050824100L),
        new DesignConditions("28A", 1091204100L),
        new DesignConditions("28B", 2091204100L),
        new DesignConditions("28C", 3091204100L),
        new DesignConditions("28D", 4091204100L),
        new DesignConditions("29A", 1081208100L),
        new DesignConditions("29B", 2081208100L),
        new DesignConditions("29C", 3081208100L),
        new DesignConditions("29D", 4081208100L),
        new DesignConditions("30A", 1071212100L),
        new DesignConditions("30B", 2071212100L),
        new DesignConditions("30C", 3071212100L),
        new DesignConditions("30D", 4071212100L),
        new DesignConditions("31A", 1061216100L),
        new DesignConditions("31B", 2061216100L),
        new DesignConditions("31C", 3061216100L),
        new DesignConditions("31D", 4061216100L),
        new DesignConditions("32A", 1051220100L),
        new DesignConditions("32B", 2051220100L),
        new DesignConditions("32C", 3051220100L),
        new DesignConditions("32D", 4051220100L),
        new DesignConditions("33A", 1081604100L),
        new DesignConditions("33B", 2081604100L),
        new DesignConditions("33C", 3081604100L),
        new DesignConditions("33D", 4081604100L),
        new DesignConditions("34A", 1071608100L),
        new DesignConditions("34B", 2071608100L),
        new DesignConditions("34C", 3071608100L),
        new DesignConditions("34D", 4071608100L),
        new DesignConditions("35A", 1061612100L),
        new DesignConditions("35B", 2061612100L),
        new DesignConditions("35C", 3061612100L),
        new DesignConditions("35D", 4061612100L),
        new DesignConditions("36A", 1051616100L),
        new DesignConditions("36B", 2051616100L),
        new DesignConditions("36C", 3051616100L),
        new DesignConditions("36D", 4051616100L),
        new DesignConditions("37A", 1072004100L),
        new DesignConditions("37B", 2072004100L),
        new DesignConditions("37C", 3072004100L),
        new DesignConditions("37D", 4072004100L),
        new DesignConditions("38A", 1062008100L),
        new DesignConditions("38B", 2062008100L),
        new DesignConditions("38C", 3062008100L),
        new DesignConditions("38D", 4062008100L),
        new DesignConditions("39A", 1052012100L),
        new DesignConditions("39B", 2052012100L),
        new DesignConditions("39C", 3052012100L),
        new DesignConditions("39D", 4052012100L),
        new DesignConditions("40A", 1062404100L),
        new DesignConditions("40B", 2062404100L),
        new DesignConditions("40C", 3062404100L),
        new DesignConditions("40D", 4062404100L),
        new DesignConditions("41A", 1052408100L),
        new DesignConditions("41B", 2052408100L),
        new DesignConditions("41C", 3052408100L),
        new DesignConditions("41D", 4052408100L),
        new DesignConditions("42A", 1052804100L),
        new DesignConditions("42B", 2052804100L),
        new DesignConditions("42C", 3052804100L),
        new DesignConditions("42D", 4052804100L),
        new DesignConditions("43A", 1110824060L),
        new DesignConditions("43B", 2110824060L),
        new DesignConditions("43C", 3110824060L),
        new DesignConditions("43D", 4110824060L),
        new DesignConditions("44A", 1110820060L),
        new DesignConditions("44B", 2110820060L),
        new DesignConditions("44C", 3110820060L),
        new DesignConditions("44D", 4110820060L),
        new DesignConditions("45A", 1110816060L),
        new DesignConditions("45B", 2110816060L),
        new DesignConditions("45C", 3110816060L),
        new DesignConditions("45D", 4110816060L),
        new DesignConditions("46A", 1110812060L),
        new DesignConditions("46B", 2110812060L),
        new DesignConditions("46C", 3110812060L),
        new DesignConditions("46D", 4110812060L),
        new DesignConditions("47A", 1110808060L),
        new DesignConditions("47B", 2110808060L),
        new DesignConditions("47C", 3110808060L),
        new DesignConditions("47D", 4110808060L),
        new DesignConditions("48A", 1110804060L),
        new DesignConditions("48B", 2110804060L),
        new DesignConditions("48C", 3110804060L),
        new DesignConditions("48D", 4110804060L),
        new DesignConditions("49A", 1110824061L),
        new DesignConditions("49B", 2110824061L),
        new DesignConditions("49C", 3110824061L),
        new DesignConditions("49D", 4110824061L),
        new DesignConditions("50A", 1101220060L),
        new DesignConditions("50B", 2101220060L),
        new DesignConditions("50C", 3101220060L),
        new DesignConditions("50D", 4101220060L),
        new DesignConditions("51A", 1101216060L),
        new DesignConditions("51B", 2101216060L),
        new DesignConditions("51C", 3101216060L),
        new DesignConditions("51D", 4101216060L),
        new DesignConditions("52A", 1101212060L),
        new DesignConditions("52B", 2101212060L),
        new DesignConditions("52C", 3101212060L),
        new DesignConditions("52D", 4101212060L),
        new DesignConditions("53A", 1101208060L),
        new DesignConditions("53B", 2101208060L),
        new DesignConditions("53C", 3101208060L),
        new DesignConditions("53D", 4101208060L),
        new DesignConditions("54A", 1101204060L),
        new DesignConditions("54B", 2101204060L),
        new DesignConditions("54C", 3101204060L),
        new DesignConditions("54D", 4101204060L),
        new DesignConditions("55A", 1101220061L),
        new DesignConditions("55B", 2101220061L),
        new DesignConditions("55C", 3101220061L),
        new DesignConditions("55D", 4101220061L),
        new DesignConditions("56A", 1091616050L),
        new DesignConditions("56B", 2091616050L),
        new DesignConditions("56C", 3091616050L),
        new DesignConditions("56D", 4091616050L),
        new DesignConditions("57A", 1091612050L),
        new DesignConditions("57B", 2091612050L),
        new DesignConditions("57C", 3091612050L),
        new DesignConditions("57D", 4091612050L),
        new DesignConditions("58A", 1091608050L),
        new DesignConditions("58B", 2091608050L),
        new DesignConditions("58C", 3091608050L),
        new DesignConditions("58D", 4091608050L),
        new DesignConditions("59A", 1091604050L),
        new DesignConditions("59B", 2091604050L),
        new DesignConditions("59C", 3091604050L),
        new DesignConditions("59D", 4091604050L),
        new DesignConditions("60A", 1091616051L),
        new DesignConditions("60B", 2091616051L),
        new DesignConditions("60C", 3091616051L),
        new DesignConditions("60D", 4091616051L),
        new DesignConditions("61A", 1082012050L),
        new DesignConditions("61B", 2082012050L),
        new DesignConditions("61C", 3082012050L),
        new DesignConditions("61D", 4082012050L),
        new DesignConditions("62A", 1082008050L),
        new DesignConditions("62B", 2082008050L),
        new DesignConditions("62C", 3082008050L),
        new DesignConditions("62D", 4082008050L),
        new DesignConditions("63A", 1082004050L),
        new DesignConditions("63B", 2082004050L),
        new DesignConditions("63C", 3082004050L),
        new DesignConditions("63D", 4082004050L),
        new DesignConditions("64A", 1082012051L),
        new DesignConditions("64B", 2082012051L),
        new DesignConditions("64C", 3082012051L),
        new DesignConditions("64D", 4082012051L),
        new DesignConditions("65A", 1072408040L),
        new DesignConditions("65B", 2072408040L),
        new DesignConditions("65C", 3072408040L),
        new DesignConditions("65D", 4072408040L),
        new DesignConditions("66A", 1072404040L),
        new DesignConditions("66B", 2072404040L),
        new DesignConditions("66C", 3072404040L),
        new DesignConditions("66D", 4072404040L),
        new DesignConditions("67A", 1072408041L),
        new DesignConditions("67B", 2072408041L),
        new DesignConditions("67C", 3072408041L),
        new DesignConditions("67D", 4072408041L),
        new DesignConditions("68A", 1062804040L),
        new DesignConditions("68B", 2062804040L),
        new DesignConditions("68C", 3062804040L),
        new DesignConditions("68D", 4062804040L),
        new DesignConditions("69A", 1062804041L),
        new DesignConditions("69B", 2062804041L),
        new DesignConditions("69C", 3062804041L),
        new DesignConditions("69D", 4062804041L),
        new DesignConditions("70A", 1053200031L),
        new DesignConditions("70B", 2053200031L),
        new DesignConditions("70C", 3053200031L),
        new DesignConditions("70D", 4053200031L),
        new DesignConditions("71A", 1110824360L),
        new DesignConditions("71B", 2110824360L),
        new DesignConditions("71C", 3110824360L),
        new DesignConditions("71D", 4110824360L),
        new DesignConditions("72A", 1110820360L),
        new DesignConditions("72B", 2110820360L),
        new DesignConditions("72C", 3110820360L),
        new DesignConditions("72D", 4110820360L),
        new DesignConditions("73A", 1110816360L),
        new DesignConditions("73B", 2110816360L),
        new DesignConditions("73C", 3110816360L),
        new DesignConditions("73D", 4110816360L),
        new DesignConditions("74A", 1110812360L),
        new DesignConditions("74B", 2110812360L),
        new DesignConditions("74C", 3110812360L),
        new DesignConditions("74D", 4110812360L),
        new DesignConditions("75A", 1110808360L),
        new DesignConditions("75B", 2110808360L),
        new DesignConditions("75C", 3110808360L),
        new DesignConditions("75D", 4110808360L),
        new DesignConditions("76A", 1110804360L),
        new DesignConditions("76B", 2110804360L),
        new DesignConditions("76C", 3110804360L),
        new DesignConditions("76D", 4110804360L),
        new DesignConditions("77A", 1110824361L),
        new DesignConditions("77B", 2110824361L),
        new DesignConditions("77C", 3110824361L),
        new DesignConditions("77D", 4110824361L),
        new DesignConditions("78A", 1101220360L),
        new DesignConditions("78B", 2101220360L),
        new DesignConditions("78C", 3101220360L),
        new DesignConditions("78D", 4101220360L),
        new DesignConditions("79A", 1101216360L),
        new DesignConditions("79B", 2101216360L),
        new DesignConditions("79C", 3101216360L),
        new DesignConditions("79D", 4101216360L),
        new DesignConditions("80A", 1101212360L),
        new DesignConditions("80B", 2101212360L),
        new DesignConditions("80C", 3101212360L),
        new DesignConditions("80D", 4101212360L),
        new DesignConditions("81A", 1101208360L),
        new DesignConditions("81B", 2101208360L),
        new DesignConditions("81C", 3101208360L),
        new DesignConditions("81D", 4101208360L),
        new DesignConditions("82A", 1101204360L),
        new DesignConditions("82B", 2101204360L),
        new DesignConditions("82C", 3101204360L),
        new DesignConditions("82D", 4101204360L),
        new DesignConditions("83A", 1101220361L),
        new DesignConditions("83B", 2101220361L),
        new DesignConditions("83C", 3101220361L),
        new DesignConditions("83D", 4101220361L),
        new DesignConditions("84A", 1091616350L),
        new DesignConditions("84B", 2091616350L),
        new DesignConditions("84C", 3091616350L),
        new DesignConditions("84D", 4091616350L),
        new DesignConditions("85A", 1091612350L),
        new DesignConditions("85B", 2091612350L),
        new DesignConditions("85C", 3091612350L),
        new DesignConditions("85D", 4091612350L),
        new DesignConditions("86A", 1091608350L),
        new DesignConditions("86B", 2091608350L),
        new DesignConditions("86C", 3091608350L),
        new DesignConditions("86D", 4091608350L),
        new DesignConditions("87A", 1091604350L),
        new DesignConditions("87B", 2091604350L),
        new DesignConditions("87C", 3091604350L),
        new DesignConditions("87D", 4091604350L),
        new DesignConditions("88A", 1091616351L),
        new DesignConditions("88B", 2091616351L),
        new DesignConditions("88C", 3091616351L),
        new DesignConditions("88D", 4091616351L),
        new DesignConditions("89A", 1082012350L),
        new DesignConditions("89B", 2082012350L),
        new DesignConditions("89C", 3082012350L),
        new DesignConditions("89D", 4082012350L),
        new DesignConditions("90A", 1082008350L),
        new DesignConditions("90B", 2082008350L),
        new DesignConditions("90C", 3082008350L),
        new DesignConditions("90D", 4082008350L),
        new DesignConditions("91A", 1082004350L),
        new DesignConditions("91B", 2082004350L),
        new DesignConditions("91C", 3082004350L),
        new DesignConditions("91D", 4082004350L),
        new DesignConditions("92A", 1082012351L),
        new DesignConditions("92B", 2082012351L),
        new DesignConditions("92C", 3082012351L),
        new DesignConditions("92D", 4082012351L),
        new DesignConditions("93A", 1072408340L),
        new DesignConditions("93B", 2072408340L),
        new DesignConditions("93C", 3072408340L),
        new DesignConditions("93D", 4072408340L),
        new DesignConditions("94A", 1072404340L),
        new DesignConditions("94B", 2072404340L),
        new DesignConditions("94C", 3072404340L),
        new DesignConditions("94D", 4072404340L),
        new DesignConditions("95A", 1072408341L),
        new DesignConditions("95B", 2072408341L),
        new DesignConditions("95C", 3072408341L),
        new DesignConditions("95D", 4072408341L),
        new DesignConditions("96A", 1062804340L),
        new DesignConditions("96B", 2062804340L),
        new DesignConditions("96C", 3062804340L),
        new DesignConditions("96D", 4062804340L),
        new DesignConditions("97A", 1062804341L),
        new DesignConditions("97B", 2062804341L),
        new DesignConditions("97C", 3062804341L),
        new DesignConditions("97D", 4062804341L),
        new DesignConditions("98A", 1053200331L),
        new DesignConditions("98B", 2053200331L),
        new DesignConditions("98C", 3053200331L),
        new DesignConditions("98D", 4053200331L),
    };

    /**
     * Return true iff the given string is a tag for some standard design conditions.
     *
     * @param s string to test
     * @return true iff the parameter is a tag for standard design conditions
     */
    public static boolean isTagPrefix(String s) {
        int lo = 0;
        int hi = conditions.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            int cmp = s.compareTo(conditions[mid].tag.substring(0, s.length()));
            if (cmp < 0) {
                hi = mid - 1;
            } else if (cmp > 0) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    private static final HashMap<Long, DesignConditions> codeIndex = getCodeIndex();

    private static HashMap<Long, DesignConditions> getCodeIndex() {
        HashMap<Long, DesignConditions> index = new HashMap<Long, DesignConditions>(1024);
        for (int i = 0; i < conditions.length; i++) {
            index.put(conditions[i].codeLong, conditions[i]);
        }
        return index;
    }

    /**
     * Return standard design conditions corresponding to a given code
     * or a dynamically created set of conditions if the code doesn't match
     * any standard.
     *
     * @param code code (assumed to be error-free)
     * @return design conditions
     */
    public static DesignConditions getDesignConditions(long code) {
        DesignConditions designConditions = codeIndex.get(code);
        return (designConditions == null) ? new DesignConditions(fromKeyCodeTag, code) : designConditions;
    }

    private static final HashMap<String, DesignConditions> tagIndex = getTagIndex();

    private static HashMap<String, DesignConditions> getTagIndex() {
        HashMap<String, DesignConditions> index = new HashMap<String, DesignConditions>(1024);
        for (int i = 0; i < conditions.length; i++) {
            index.put(conditions[i].tag, conditions[i]);
        }
        return index;
    }

    /**
     * Return standard design conditions corresponding to a given tag.
     *
     * @param tag tag
     * @return design conditions
     */
    public static DesignConditions getDesignConditions(String tag) {
        return tagIndex.get(tag);
    }

    private static class SetupKey {
        double deckElevation;
        double archHeight;
        double pierHeight;
        int nAnchorages;
        int loadType;
        int deckType;

        public SetupKey(double deckElevation, double archHeight, double pierHeight, int nAnchorages, int loadType, int deckType) {
            this.deckElevation = deckElevation;
            this.archHeight = archHeight;
            this.pierHeight = pierHeight;
            this.nAnchorages = nAnchorages;
            this.loadType = loadType;
            this.deckType = deckType;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SetupKey)) {
                return false;
            }
            SetupKey key = (SetupKey)obj;
            return  deckElevation == key.deckElevation &&
                    archHeight == key.archHeight &&
                    pierHeight == key.pierHeight &&
                    nAnchorages == key.nAnchorages &&
                    loadType == key.loadType &&
                    deckType == key.deckType;
        }

        @Override
        public int hashCode() {
            long val = Double.valueOf(deckElevation).hashCode();
            val = (val << 5) + Double.valueOf(archHeight).hashCode();
            val = (val << 5) + Double.valueOf(pierHeight).hashCode();
            val = (val << 5) + nAnchorages;
            val = (val << 5) + loadType;
            val = (val << 5) + deckType;
            return Long.valueOf(val).hashCode();
        }

        @Override
        public String toString() {
            return "[deckElevation=" + deckElevation +
                    ",archHeight=" + archHeight +
                    ",pierHeight=" + pierHeight +
                    ",nAnchorages=" + nAnchorages +
                    ",loadType=" + loadType +
                    ",deckType=" + deckType + "]";
        }
    }

    private static final HashMap<SetupKey, DesignConditions> setupIndex = getSetupIndex();

    private static HashMap<SetupKey, DesignConditions> getSetupIndex() {
        HashMap<SetupKey, DesignConditions> index = new HashMap<SetupKey, DesignConditions>(1024);
        for (int i = 0; i < conditions.length; i++) {
            DesignConditions d = conditions[i];
            index.put(new SetupKey(
                d.deckElevation,
                d.archHeight,
                d.pierHeight,
                d.nAnchorages,
                d.loadType,
                d.deckType), d);
        }
        return index;
    }

    /**
     * Return design conditions given selected values that constitute a primary key.
     *
     * @param deckElevation deck elevation in meters
     * @param archHeight arch height in meters
     * @param pierHeight pier height in meters
     * @param nAnchorages number of anchorages
     * @param loadType type of load
     * @param deckType type of deck
     * @return design conditions
     */
    public static DesignConditions getDesignConditions(double deckElevation, double archHeight, double pierHeight, int nAnchorages, int loadType, int deckType) {
        return setupIndex.get(new SetupKey(deckElevation, archHeight, pierHeight, nAnchorages, loadType, deckType));
    }

    /**
     * Development procedure to print the site costs table for the Judge.
     */
    public static void printSiteCostsTable() {
        DesignConditions[] sortedConditions = new DesignConditions [conditions.length];
        System.arraycopy(conditions, 0, sortedConditions, 0, conditions.length);
        Arrays.sort(sortedConditions, new Comparator<DesignConditions>() {

            public int compare(DesignConditions a, DesignConditions b) {
                return (a.codeLong < b.codeLong) ? -1 :
                       (a.codeLong > b.codeLong) ? 1 : 0;
            }
        });
        final String dest = "eg/"+ BridgeModel.version + "/scenario_descriptors.h";
        try {
            Writer out = new OutputStreamWriter(new FileOutputStream(dest));
            out.write("static TScenarioDescriptor scenario_descriptor_tbl[] = {\n");
            for (int i = 0; i < sortedConditions.length; i++) {
                DesignConditions c = sortedConditions[i];
                out.write(String.format("    {%4d, \"%d\", \"%s\", %9.2f },\n",
                                        i, c.codeLong, c.tag, c.totalFixedCost));
                // Sanity check for error code checker.
                int err = getCodeError(c.getCode());
                if (err != 0) {
                    System.out.println("ERROR:" + err);
                    System.exit(1);
                }
            }
            out.write("};\n");
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(DesignConditions.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Site costs table written to " + dest);
    }
}
