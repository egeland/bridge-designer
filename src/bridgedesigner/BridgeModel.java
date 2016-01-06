/*
 * BridgeModel.java  
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

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.ResourceMap;

/**
 * Read-only bridge model.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeModel {
    /**
     * Current bridge designer version year.
     */
    public static final int version = 2016;
    /**
     * Field separator in text bridge representation.
     */
    protected static final char DELIM = '|';
    /**
     * Byte length of a grid coordinate of a joint field in a text bridge representation.
     */
    protected static final int JOINT_COORD_LEN = 3;
    /**
     * Byte length of a joint number of a member field in a text bridge representation.
     */
    protected static final int MEMBER_JOINT_LEN = 2;
    /**
     * Byte length of the stock material index of a member field in a text bridge representation.
     */
    protected static final int MEMBER_MATERIAL_LEN = 1;
    /**
     * Byte length of the stock section index if a member field in a text bridge representation.
     */
    protected static final int MEMBER_SECTION_LEN = 1;
    /**
     * Byte length of the stock size index of a member field in a text bridge representation.
     */
    protected static final int MEMBER_SIZE_LEN = 2;
    /**
     * Byte length of the number of joints field in a text bridge representation.
     */
    protected static final int N_JOINTS_LEN = 2;
    /**
     * Byte length of the number of members field in a text bridge representation.
     */
    protected static final int N_MEMBERS_LEN = 3;
    /**
     * Byte length of the design conditions scenario code field in a text bridge representation.
     */
    protected static final int SCENARIO_CODE_LEN = 10;
    /**
     * Byte length of a the version year number field in a text bridge representation.
     */
    protected static final int YEAR_LEN = 4;
    /**
     * Design conditions for this bridge.
     */
    protected DesignConditions designConditions;
    /**
     * Name of designer of this bridge.
     */
    protected String designedBy = "";
    /**
     * Formatter for quantities with two decimal place accuracy.
     */
    protected final NumberFormat decimalFormatter = new DecimalFormat("0.00");
    /**
     * Stock inventory to use for members in this bridge.
     */
    protected final Inventory inventory = new Inventory();
    /**
     * Number of current iteration.
     */
    protected int iterationNumber = 1;
    /**
     * Vector of joints in this bridge.
     */
    protected final ArrayList<Joint> joints = new ArrayList<Joint>();
    /**
     * Vector of members in this bridge.
     */
    protected final ArrayList<Member> members = new ArrayList<Member>();
    /**
     * Map taking material-section pairs to their respective total cost in this bridge.
     */
    protected TreeMap<MaterialSectionPair, Double> materialSectionPairs = new TreeMap<MaterialSectionPair, Double>();
    /**
     * Map taking material-shape pairs to respective counts of members as needed for cost reporting.
     */
    protected TreeMap<MaterialShapePair, Integer> materialShapePairs = new TreeMap<MaterialShapePair, Integer>();
    /**
     * Set of material-shape combinations used for counting combinations in cost calculations.
     */
    protected HashSet<MaterialShapePair> stockSet = new HashSet<MaterialShapePair>();
    /**
     * World y-coordinate of labels in the model.  Strange to have this here, but it's a field in bridge files.
     */
    protected double labelPosition = 2;
    /**
     * Project id for this bridge.
     */
    protected String projectId;
    /**
     * Project name for this bridge.
     */
    protected String projectName = "Dennis H. Mahan Memorial Bridge";
    /**
     * Current read buffer used by the parser.
     */
    private byte[] readBuf;
    /**
     * Current position in the read buffer used by the parser.
     */
    private int readPtr;
    /**
     * Shared read-only summary of costs of this bridge, initialized by <code>getCosts</code>.
     */
    protected final Costs costs = new Costs();
    /**
     * Handy formatters.
     */
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);        
    private final NumberFormat intFormat = NumberFormat.getIntegerInstance(); // Locale-specific will be fine...    

    /**
     * Clear the entire bridge structure.
     */
    public void clearStructure() {
        members.clear();
        joints.clear();
    }

    /**
     * Pair used for hashing material/shape combinations for cost computation and report generation purposes.
     */
    public static class MaterialShapePair implements Comparable {
        /**
         * Material part of this pair.
         */
        public Material material;

        /**
         * Shape part of this pair.
         */
        public Shape shape;

        /**
         * Construct a fresh material-shape pair.
         * 
         * @param material material part of this pair
         * @param shape shape part of this pair
         */
        public MaterialShapePair(Material material, Shape shape) {
            this.material = material;
            this.shape = shape;
        }

        /**
         * Compute an integer hash code for this pair.
         * 
         * @return integer hash code
         */
        @Override
        public int hashCode() {
            return material.hashCode() + shape.hashCode();
        }

        /**
         * Equality relation between pairs.
         * 
         * @param o object to test for equality with this pair
         * @return true iff the provided object is equal this pair
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof MaterialShapePair) {
                MaterialShapePair other = (MaterialShapePair) o;
                return this.material == other.material && this.shape == other.shape;
            }
            return false;
        }
        
        /**
         * Return a string representation of this pair suitable for reports.
         * 
         * @return string representation suitable for reports
         */
        @Override
        public String toString() {
            return shape.getName() + " mm " + material + " " + shape.getSection();
        }
        
        /**
         * Comparison operator between pairs for sorted sets and maps.
         * 
         * @param o object to compare with this
         * @return result of comparison as integer: negative, zero, positive
         */
        public int compareTo(Object o) {
            if (o == null) {
                return 1;
            }
            MaterialShapePair other = (MaterialShapePair)o;
            int cmp = this.shape.getSizeIndex() - other.shape.getSizeIndex();
            if (cmp != 0) {
                return cmp;
            }
            cmp = this.material.getIndex() - other.material.getIndex();
            if (cmp != 0) {
                return cmp;
            }
            return this.shape.getSection().getIndex() - other.shape.getSection().getIndex();
        }
    }

    /**
     * Pair used for hashing material/section combinations for cost computation and report generation purposes.
     */
    public static class MaterialSectionPair implements Comparable {
        /**
         * Material part of this pair.
         */
        public Material material;
        /**
         * Section part of this pair.
         */
        public CrossSection section;

        /**
         * Construct a pair with given material and section parts.
         * 
         * @param material material part of this pair
         * @param section section part of this pair
         */
        public MaterialSectionPair(Material material, CrossSection section) {
            this.material = material;
            this.section = section;
        }

        /**
         * Compute an integer hash code for this pair.
         * 
         * @return integer hash code
         */
        @Override
        public int hashCode() {
            return material.hashCode() + section.hashCode();
        }

        /**
         * Equality relation between pairs.
         * 
         * @param o object to test for equality with this pair
         * @return true iff the provided object is equal this pair
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof MaterialSectionPair) {
                MaterialSectionPair other = (MaterialSectionPair) o;
                return this.material == other.material && this.section == other.section;
            }
            return false;
        }

        /**
         * Return a string representation of this pair suitable for reports.
         * 
         * @return string representation suitable for reports
         */
        @Override
        public String toString() {
            return material + " " + section.getName();
        }

        /**
         * Comparison operator between pairs for sorted sets and maps.
         * 
         * @param o object to compare with this
         * @return result of comparison as integer: negative, zero, positive
         */
        public int compareTo(Object o) {
            if (o == null) {
                return 1;
            }
            MaterialSectionPair other = (MaterialSectionPair)o;
            int cmp = this.material.getIndex() - other.material.getIndex();
            if (cmp != 0) {
                return cmp;
            }
            return this.section.getIndex() - other.section.getIndex();
        }
    }

    /**
     * Summary of costs of this bridge with report generators.
     */
    public class Costs {
        /**
         * Map from material-shape combinations to counts of the number of members composed of each.
         */
        public TreeMap<MaterialShapePair, Integer> materialShapePairs;
        /**
         * Map from material-section combinations to total cost of members composed of each.
         */
        public TreeMap<MaterialSectionPair, Double> materialSectionPairs;
        /**
         * Design conditions of bridge these costs are for.
         */
        public DesignConditions conditions;
        /**
         * Stock inventory used by bridge these costs are for.
         */
        public Inventory inventory;
        /**
         * Number of connections in the bridge as per cost model.
         */
        public int nConnections;
        /**
         * Notes on this bridge.
         */
        public String [] notes;

        /**
         * Generate a tab delimited text representation of the cost information.  This loads cleanly into Excel.
         * 
         * @return tab delimited text
         */
        String toTabDelimitedText() {
            ResourceMap resourceMap = BDApp.getResourceMap(CostReportTableModel.class);
            StringBuilder str = new StringBuilder();
            Formatter formatter = new Formatter(str, Locale.US);
            if (notes != null) {
                for (int i = 0; i < notes.length; i++) {
                    formatter.format("%s\n", notes[i]);
                }
            }
            formatter.format("%s\n", resourceMap.getString("columnIds.text").replace('|', '\t'));
            int nProductRows = costs.materialShapePairs.size();

            Iterator<EditableBridgeModel.MaterialSectionPair> mtlSecIt = costs.materialSectionPairs.keySet().iterator();
            boolean initial = true;
            double totalMtlCost = 0.00;
            while (mtlSecIt.hasNext()) {
                EditableBridgeModel.MaterialSectionPair pair = mtlSecIt.next();
                // Column 0
                if (initial) {
                    formatter.format("%s\t", resourceMap.getString("materialCost.text"));            
                    initial = false;
                }
                else {
                    formatter.format("\t");
                }
                // Column 1
                formatter.format("%s\t", pair.toString());
                // Column 2
                double weight = costs.materialSectionPairs.get(pair);
                double cost = pair.material.getCost(pair.section);
                formatter.format("%s\t", resourceMap.getString("materialCostNote.text", weight, currencyFormat.format(cost)));
                // Column 3
                double mtlCost = 2 * weight * cost;
                formatter.format("%s\n", currencyFormat.format(mtlCost));
                totalMtlCost += mtlCost;
            }
            // Blank row (double rule)
            formatter.format("\n");
            // Connection row
            formatter.format("%s\t\t", resourceMap.getString("connectionCost.text"));
            final double connectionFee = costs.inventory.getConnectionFee();
            formatter.format("%s\t", resourceMap.getString("connectionCostNote.text", costs.nConnections, connectionFee));
            final double connectionCost = 2 * nConnections * connectionFee;
            formatter.format("%s\n", currencyFormat.format(connectionCost));
            // Blank row (double rule)
            formatter.format("\n");
            // Product costs.
            Iterator<EditableBridgeModel.MaterialShapePair> mtlShpIt = costs.materialShapePairs.keySet().iterator();
            initial = true;
            while (mtlShpIt.hasNext()) {
                EditableBridgeModel.MaterialShapePair pair = mtlShpIt.next();
                // Column 0
                if (initial) {
                    formatter.format("%s\t", resourceMap.getString("productCost.text"));            
                    initial = false;
                }
                else {
                    formatter.format("\t");
                }
                // Column 1
                formatter.format("%2d - %s\t", costs.materialShapePairs.get(pair), pair.toString());
                // Column 2
                formatter.format("%s\t", resourceMap.getString("productCostNote.text"));
                // Column 3
                formatter.format("%s\n", currencyFormat.format(costs.inventory.getOrderingFee()));
            }
            // Blank row (double rule)
            formatter.format("\n");
            final double totalProductCost = nProductRows * costs.inventory.getOrderingFee();
            // Site costs.  Code is similar to SiteCostTableModel.  If you change this, you probably need to change that.
            formatter.format("%s\t", resourceMap.getString("siteCost.text"));
            formatter.format("%s\t", resourceMap.getString("deckCost.text"));
            formatter.format("%s\t", resourceMap.getString("deckCostNote.text", 
                    costs.conditions.getNPanels(), currencyFormat.format(costs.conditions.getDeckCostRate())));
            final double deckCost = costs.conditions.getNPanels() * costs.conditions.getDeckCostRate();
            formatter.format("%s\n\t", currencyFormat.format(deckCost));
            formatter.format("%s\t", resourceMap.getString("excavationCost.text"));
            formatter.format("%s\t", resourceMap.getString("excavationCostNote.text",
                    intFormat.format(costs.conditions.getExcavationVolume()), 
                    currencyFormat.format(DesignConditions.excavationCostRate)));
            formatter.format("%s\n\t", currencyFormat.format(costs.conditions.getExcavationCost()));
            formatter.format("%s\t", resourceMap.getString("abutmentCost.text"));
            final String abutmentType = resourceMap.getString(costs.conditions.isArch() ? "arch.text" : "standard.text");
            formatter.format("%s\t", resourceMap.getString("abutmentCostNote.text", 
                    abutmentType, currencyFormat.format(costs.conditions.getAbutmentCost())));
            formatter.format("%s\n\t", currencyFormat.format(2 * costs.conditions.getAbutmentCost()));
            formatter.format("%s\t", resourceMap.getString("pierCost.text"));
            if (costs.conditions.isPier()) {
                formatter.format("%s\t", resourceMap.getString("pierNote.text", 
                        intFormat.format(costs.conditions.getPierHeight())));
            } else {
                formatter.format("%s\t", resourceMap.getString("noPierNote.text"));
            }
            formatter.format("%s\n\t", currencyFormat.format(costs.conditions.getPierCost()));
            formatter.format("%s\t", resourceMap.getString("anchorageCost.text"));
            final int nAnchorages = costs.conditions.getNAnchorages();
            if (nAnchorages == 0) {
                formatter.format("%s\t", resourceMap.getString("noAnchoragesNote.text"));
            } else {
                formatter.format("%s\t", resourceMap.getString("anchorageNote.text", 
                        nAnchorages, currencyFormat.format(DesignConditions.anchorageCost)));
            }
            final double anchorageCost = nAnchorages * DesignConditions.anchorageCost;
            formatter.format("%s\n\n", currencyFormat.format(anchorageCost));
            // Blank row (double rule) above
            formatter.format("%s\t", resourceMap.getString("totalCost.text"));
            formatter.format("%s\t", resourceMap.getString("sum.text"));
            formatter.format("%s\t", resourceMap.getString("sumNote.text",
                    currencyFormat.format(totalMtlCost),
                    currencyFormat.format(connectionCost),
                    currencyFormat.format(totalProductCost),
                    currencyFormat.format(costs.conditions.getTotalFixedCost())));
            formatter.format("%s\n", currencyFormat.format(
                    totalMtlCost + connectionCost + totalProductCost + costs.conditions.getTotalFixedCost()));
            return str.toString();
        }
    }

    /**
     * (Re)Initialize and return the shared costs instance for this bridge.
     * Notes are left null.
     * 
     * @return cost summary
     */
    public Costs getCosts() {
        materialShapePairs.clear();
        materialSectionPairs.clear();
        Iterator<Member> e = members.iterator();
        while (e.hasNext()) {
            Member member = e.next();
            MaterialShapePair msPair = new MaterialShapePair(member.getMaterial(), member.getShape());
            Integer iVal = materialShapePairs.get(msPair);
            materialShapePairs.put(msPair, new Integer(iVal == null ? 1 : iVal + 1));
            double weight = member.getShape().getArea() * member.getLength() * member.getMaterial().getDensity();
            MaterialSectionPair mcPair = new MaterialSectionPair(member.getMaterial(), member.getShape().getSection());
            Double dVal = materialSectionPairs.get(mcPair);
            materialSectionPairs.put(mcPair, new Double(dVal == null ? weight : dVal + weight));
        }
        costs.materialShapePairs = materialShapePairs;
        costs.materialSectionPairs = materialSectionPairs;
        costs.conditions = designConditions;
        costs.inventory = inventory;
        costs.nConnections = joints.size();
        costs.notes = null;
        return costs;
    }

    /**
     * Initialize and return the notes for this cost summary or null if none.
     * Notes include project name, id, designer, and iteration number.  A blank
     * designer name is not returned at all, so the array has either three or
     * four elements.
     * 
     * @return array of notes strings
     */
    public String [] getNotes() {
        final ResourceMap resourceMap = BDApp.getResourceMap(BridgeModel.class);
        final Calendar calendar = Calendar.getInstance();
        final String fmt = resourceMap.getString("dateNote.text");
        final SimpleDateFormat dateFormat = new SimpleDateFormat(fmt == null ? "(EEE, d MMM yyyy)" : fmt);
        final String note1 = resourceMap.getString("projectNameNote.text", projectName);
        final String note2 = resourceMap.getString("projectIdNote.text", projectId);
        final String note3 = resourceMap.getString("iterationNote.text", iterationNumber, dateFormat.format(calendar.getTime()));
        return designedBy.trim().length() == 0 ? 
            new String [] { note1, note2, note3 } :
            new String [] { note1, note2, note3,
                resourceMap.getString("iterationNote.text", iterationNumber, dateFormat.format(calendar.getTime())),
            };
    }
    
    /**
     * (Re)Initialize and return the shared costs instance for this bridge.  Notes are filled in as well.
     * 
     * @return cost summary with notes.
     */
    public Costs getCostsWithNotes() {
        getCosts();
        costs.notes = getNotes();
        return costs;
    }

    /**
     * Return the total cost of this bridge.
     * 
     * @return total cost
     */
    public double getTotalCost() {
        double mtlCost = 0.0;
        stockSet.clear();
        Iterator<Member> e = members.iterator();
        while (e.hasNext()) {
            Member member = e.next();
            stockSet.add(new MaterialShapePair(member.getMaterial(), member.getShape()));
            mtlCost += member.getMaterial().getCost(member.getShape().getSection()) * 
                    member.getShape().getArea() * member.getLength() * member.getMaterial().getDensity();
        }
        double productCost = stockSet.size() * inventory.getOrderingFee();
        double connectionCost = joints.size() * inventory.getConnectionFee();
        return 2 * (mtlCost + connectionCost) + productCost + designConditions.getTotalFixedCost();
    }

    /**
     * Return a descriptor for the stock that appears most commonly in the bridge or a reasonable default if there
     * are no members in the bridge.  Used to initialize stock selectors upon reading a new bridge.  Also to 
     * for the stock of autofix-generated members.
     * 
     * @return descriptor for most common stock in the bridge or null if there are no members
     */
    public StockSelector.Descriptor getMostCommonStock() {
        HashMap<StockSelector.Descriptor, Integer> map = new HashMap<StockSelector.Descriptor, Integer>();
        Iterator<Member> e = members.iterator();
        StockSelector.Descriptor mostCommon = null;
        int nMostCommon = -1;
        while (e.hasNext()) {
            Member member = e.next();
            StockSelector.Descriptor descriptor = new StockSelector.Descriptor(member);
            Integer n = map.get(descriptor);
            int nNew = (n == null) ? 1 : n + 1;
            if (nNew > nMostCommon) {
                mostCommon = descriptor;
                nMostCommon = nNew;
            }
            map.put(descriptor, nNew);
        }
        return mostCommon == null ? new StockSelector.Descriptor(0, 0, 16) : mostCommon;
    }

    /**
     * Return an array of arrays of selected members, where each internal array consists of members
     * made of the same stock.  The internal lists are sorted by member number, and the outer list
     * is sorted by first member number of element lists.
     * 
     * @return array of common stock member arrays sorted lexicographicaly by member number
     */
    public Member [] [] getSelectedStockLists() {
        // Build a map from unique materia/shape pairs to vectors of members with that material/shape
        HashMap<MaterialShapePair, ArrayList<Member>> map = new HashMap<MaterialShapePair, ArrayList<Member>>();
        Iterator<Member> me = members.iterator();
        while (me.hasNext()) {
            Member member = me.next();
            if (member.isSelected()) {
                MaterialShapePair msp = new MaterialShapePair(member.getMaterial(), member.getShape());
                ArrayList<Member> v = map.get(msp);
                if (v == null) {
                    v = new ArrayList<Member>();
                    v.add(member);
                    map.put(msp, v);
                }
                else {
                    v.add(member);                
                }
            }
        }
        // Convert the map/vector data structure to array of arrays and sort the outer array.
        Member [] [] lists = new Member [map.size()] [];
        Iterator<ArrayList<Member>> mi = map.values().iterator();
        for (int i = 0; i < lists.length; i++) {
            ArrayList<Member> v = mi.next();
            lists[i] = v.toArray(new Member [v.size()]);
        }
        Arrays.sort(lists, new Comparator<Member[]>() {
            public int compare(Member[] m1, Member[] m2) {
                return m1[0].getNumber() - m2[0].getNumber();
            }
        });
        return lists;
    }
    
    /**
     * Return the design conditions for this bridge.
     * 
     * @return design conditions
     */
    public DesignConditions getDesignConditions() {
        return designConditions;
    }

    /** 
     * Set the name of the designer of this bridge.  Error to set to null.
     * 
     * @param designedBy designer's name
     */
    public void setDesignedBy(String designedBy) {
        this.designedBy = designedBy;
    }

    /**
     * Return the name of the designer of this bridge.  Never null.
     * 
     * @return designer's name
     */
    public String getDesignedBy() {
        return designedBy;
    }

    /**
     * Get the stock inventory used in materials for this bridge.
     * 
     * @return stock inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Return the joint at the given world point, allowing for possible slight numerical errors.
     * 
     * @param ptWorld the pick point
     * @return the joint at the pick point, if there is one; null otherwise
     */
    public Joint findJointAt(Affine.Point ptWorld) {
        Iterator<Joint> e = joints.iterator();
        while (e.hasNext()) {
            Joint joint = e.next();
            if (joint.isAt(ptWorld)) {
                return joint;
            }
        }
        return null;
    }

    /**
     * Return the current iterationNumber count of the bridge design.
     *
     * @return iterationNumber count
     */
    public int getIteration() {
        return iterationNumber;
    }

    /**
     * Return the vector of joints for this bridge.
     * 
     * @return joint vector
     */
    public ArrayList<Joint> getJoints() {
        return joints;
    }

    /**
     * Get the label position of the bridge.
     *
     * @return label position
     */
    public double getLabelPosition() {
        return labelPosition;
    }

    /**
     * Return the vector of members for this bridge.
     * 
     * @return member vector
     */
    public ArrayList<Member> getMembers() {
        return members;
    }

    /**
     * Set the project id.  Should never be null.
     * 
     * @param projectId project id
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Return the project ID for this design.  Never null.
     * 
     * @return project id
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Set the project name for this design.  Should never be null.
     * 
     * @param projectName project name
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Return the project name for this design.  Never null.
     * 
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Encode a force/strength ratio as a string.
     * 
     * @param r force/strength ratio
     * @return encoding
     */
    protected String getRatioEncoding(double r) {
        return (r < 0) ? "--" : String.format((Locale)null, "%.2f", r);
    }

    /**
     * Parse the string version of the tension or compression force/strength ratio encoding to obtain
     * corresponding numeric representation. This is a lossy process because we don't save with full
     * precision. Must consequently mark freshly read bridge as not a current analysis for this reason.
     * Parsed ratios are unreliable for deciding member failure coloration.
     *
     * @param s string representation of ratio
     * @return numeric representation of ratio
     */
    protected static double parseRatioEncoding(String s) {
        try {
            // double dash causes exception
            // (along with previous locale problem--comma decimals)
            return Double.parseDouble(s);
        }
        catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Return an optionally provided rectangle that exactly includes
     * all the joint locations of the bridge.
     * 
     * @param extent optional (may be null) rectangle to use for the result
     * @return extent rectangle
     */
    public Rectangle2D getExtent(Rectangle2D extent) {
        if (extent == null) {
            extent = new Rectangle2D.Double();
        }
        boolean first = true;
        Iterator<Joint> je = joints.iterator();
        while (je.hasNext()) {
            Joint j = je.next();
            if (first) {
                first = false;
                Affine.Point pt = j.getPointWorld();
                extent.setRect(pt.x, pt.y, 0, 0);
            }
            else {
                extent.add(j.getPointWorld());
            }
        }
        return extent;
    }

    /**
     * Initialize this bridge model with given design conditions and an empty structure.
     * 
     * @param conditions design conditions for the bridge
     * @param projectId new project id; if null, project id remains the same
     * @param designedBy new designer's name; if null, designer's name remains the same
     */
    public void initialize(DesignConditions conditions, String projectId, String designedBy) {
        this.designConditions = conditions;
        if (projectId != null) {
            this.projectId = projectId;
        }
        if (designedBy != null) {
            this.designedBy = designedBy;        
        }
        clearStructure();
        // Insert the prescribed joints for these design cconditions.
        for (int i = 0; i < conditions.getNPrescribedJoints(); i++) {
            joints.add(conditions.getPrescribedJoint(i));
        }
    }

    /**
     * Return true iff all elements of the bridge pass the slenderness test.
     * 
     * @return true iff the bridge passes the slenderness test
     */
    public boolean isPassingSlendernessCheck () {
        Iterator<Member> me = members.iterator();
        double allowableSlenderness = designConditions.getAllowableSlenderness();
        while (me.hasNext()) {
            if (me.next().getSlenderness() > allowableSlenderness) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Return the initialization status of the bridge model.
     *
     * @return true iff the bridge model has been initialized.
     */
    public boolean isInitialized() {
        return designConditions != null;
    }

    /**
     * Read the encrypted bridge in the given file into this one.
     * 
     * @param f bridge file to read
     * @throws java.io.IOException something went wrong with the read operation
     */
    public void read(File f) throws IOException {
        byte [] bytes = Utility.getBytesFromFile(f);
        RC4 rc4 = new RC4();
        rc4.setKey(RC4Key.getScrambleKey());
        rc4.endecrypt(bytes);
        // System.out.println(new String(bytes));
        parseBytes(bytes);
    }

    /**
     * Encrypt and write this bridge to the given file.
     * 
     * @param f bridge file to write
     * @throws java.io.IOException something went wrong with the write operation
     */
    public void write(File f) throws IOException {
        // Build up the result as a string.  Get bytes. Scramble.  Write to file.
        byte[] rtn = toBytes();
        RC4 rc4 = new RC4();
        rc4.setKey(RC4Key.getScrambleKey());
        rc4.endecrypt(rtn);
        OutputStream os = new FileOutputStream(f);
        os.write(rtn);
        os.close();
    }

    /**
     * Parse the given clear text byte array as a bridge.  
     * 
     * @param readBuf bytes containing ASCII representation of bridge
     * @throws java.io.IOException something went wrong with parsing
     */
    protected void parseBytes(byte[] readBuf) throws IOException {
        this.readBuf = readBuf;
        readPtr = 0;
        DraftingGrid grid = new DraftingGrid(DraftingGrid.FINE_GRID);
        clearStructure();
        if (scanUnsigned(YEAR_LEN, "bridge designer version") != version) {
            throw new IOException("bridge design file version is not " + version);
        }
        long scenarioCode = scanUnsignedLong(SCENARIO_CODE_LEN, "scenario code");
        designConditions = DesignConditions.getDesignConditions(scenarioCode);
        if (designConditions == null) {
            throw new IOException("invalid scenario " + scenarioCode);            
        }
        int n_joints = scanUnsigned(N_JOINTS_LEN, "number of joints");
        int n_members = scanUnsigned(N_MEMBERS_LEN, "number of members");
        for (int i = 0, n = 1; i < n_joints; i++, n++) {
            int x = scanInt(JOINT_COORD_LEN, "joint " + n + " x-coordinate");
            int y = scanInt(JOINT_COORD_LEN, "joint " + n + " y-coordinate");
            if (i < designConditions.getNPrescribedJoints()) {
                Joint joint = designConditions.getPrescribedJoint(i);
                if (x != grid.worldToGridX(joint.getPointWorld().x) || y != grid.worldToGridY(joint.getPointWorld().y)) {
                    throw new IOException("bad prescribed joint " + n);
                }
                joints.add(joint);
            } else {
                joints.add(new Joint(i, new Affine.Point(grid.gridToWorldX(x), grid.gridToWorldY(y))));
            }
        }
        for (int i = 0, n = 1; i < n_members; i++, n++) {
            int jointANumber = scanUnsigned(MEMBER_JOINT_LEN, "first joint of member " + n);
            int jointBNumber = scanUnsigned(MEMBER_JOINT_LEN, "second joint of member " + n);
            int materialIndex = scanUnsigned(MEMBER_MATERIAL_LEN, "material index of member " + n);
            int sectionIndex = scanUnsigned(MEMBER_SECTION_LEN, "section index of member " + n);
            int sizeIndex = scanUnsigned(MEMBER_SIZE_LEN, "size index of member " + n);
            members.add(new Member(i, joints.get(jointANumber - 1), joints.get(jointBNumber - 1),
                    inventory.getMaterial(materialIndex), inventory.getShape(sectionIndex, sizeIndex)));
        }
        Iterator<Member> e = members.iterator();
        while (e.hasNext()) {
            Member member = e.next();
            member.setCompressionForceStrengthRatio(parseRatioEncoding(scanToDelimiter("compression/strength ratio")));
            member.setTensionForceStrengthRatio(parseRatioEncoding(scanToDelimiter("tension/strength ratio")));            
        }
        designedBy = scanToDelimiter("name of designer");
        projectId = scanToDelimiter("project ID");
        iterationNumber = Integer.parseInt(scanToDelimiter("iteration"));
        labelPosition = Double.parseDouble(scanToDelimiter("label position"));
    }

    /*
     * Following are useful scanner sub-functions.
     */
    private int scanInt(int width, String what) throws IOException {
        int val = 0;
        boolean negate_p = false;

        // Skip whitespace.
        while (width > 0 && readBuf[readPtr] == ' ') {
            width--;
            readPtr++;
        }
        // Process negative sign, if present.
        if (width >= 2 && readBuf[readPtr] == '-') {
            width--;
            readPtr++;
            negate_p = true;
        }
        while (width > 0) {
            if ('0' <= readBuf[readPtr] && readBuf[readPtr] <= '9') {
                val = val * 10 + (readBuf[readPtr] - '0');
                width--;
                readPtr++;
            } else {
                throw new IOException("couldn\'t scan " + what);
            }
        }
        return negate_p ? -val : val;
    }

    private String scanToDelimiter(String what) {
        StringBuilder buf = new StringBuilder(16);
        while (readBuf[readPtr] != DELIM) {
            buf.append((char) readBuf[readPtr]);
            readPtr++;
        }
        readPtr++;
        return buf.toString();
    }

    private int scanUnsigned(int width, String what) throws IOException {
        int val = 0;
        while (width > 0 && readBuf[readPtr] == ' ') {
            width--;
            readPtr++;
        }
        while (width > 0) {
            if ('0' <= readBuf[readPtr] && readBuf[readPtr] <= '9') {
                val = val * 10 + (readBuf[readPtr] - '0');
                width--;
                readPtr++;
            } else {
                throw new IOException("couldn\'t scan " + what);
            }
        }
        return val;
    }

    private long scanUnsignedLong(int width, String what) throws IOException {
        long val = 0;
        while (width > 0 && readBuf[readPtr] == ' ') {
            width--;
            readPtr++;
        }
        while (width > 0) {
            if ('0' <= readBuf[readPtr] && readBuf[readPtr] <= '9') {
                val = val * 10 + (readBuf[readPtr] - '0');
                width--;
                readPtr++;
            } else {
                throw new IOException("couldn\'t scan " + what);
            }
        }
        return val;
    }

    /**
     * Set the label position of the bridge.
     *
     * @param labelPosition label position.
     */
    public void setLabelPosition(double labelPosition) {
        this.labelPosition = labelPosition;
    }

    /**
     * Return a byte array containing an ASCII text representation of this bridge.
     * @return ASCII text as bytes
     */
    protected byte[] toBytes() {
        byte[] bytes = null;
        try {
            bytes = toString().getBytes("ASCII");
        } catch (UnsupportedEncodingException ex) { }
        return bytes;
    }

    /**
     * Return the bridge as a string in the bridge file format, but unencrypted.
     * 
     * @return bridge as string
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        Formatter f = new Formatter(s, Locale.US);
        DraftingGrid grid = new DraftingGrid(DraftingGrid.FINE_GRID);
        f.format("%" + YEAR_LEN + "d", version);
        f.format("%" + SCENARIO_CODE_LEN + "d", designConditions.getCodeLong());
        f.format("%" + N_JOINTS_LEN + "d", joints.size());
        f.format("%" + N_MEMBERS_LEN + "d", members.size());
        Iterator<Joint> ej = joints.iterator();
        while (ej.hasNext()) {
            Joint joint = ej.next();
            f.format("%" + JOINT_COORD_LEN + "d", grid.worldToGridX(joint.getPointWorld().x));
            f.format("%" + JOINT_COORD_LEN + "d", grid.worldToGridY(joint.getPointWorld().y));
        }
        Iterator<Member> em = members.iterator();
        while (em.hasNext()) {
            Member member = em.next();
            f.format("%" + MEMBER_JOINT_LEN + "d", member.getJointA().getNumber());
            f.format("%" + MEMBER_JOINT_LEN + "d", member.getJointB().getNumber());
            f.format("%" + MEMBER_MATERIAL_LEN + "d", member.getMaterial().getIndex());
            f.format("%" + MEMBER_SECTION_LEN + "d", member.getShape().getSection().getIndex());
            f.format("%" + MEMBER_SIZE_LEN + "d", member.getShape().getSizeIndex());
        }
        em = members.iterator();
        while (em.hasNext()) {
            Member member = em.next();
            s.append(getRatioEncoding(member.getCompressionForceStrengthRatio()));
            s.append(DELIM);
            s.append(getRatioEncoding(member.getTensionForceStrengthRatio()));
            s.append(DELIM);
        }
        s.append(designedBy);
        s.append(DELIM);
        s.append(projectId);
        s.append(DELIM);
        s.append(getIteration());
        s.append(DELIM);
        f.format("%.3f", getLabelPosition());
        s.append(DELIM);
        return s.toString();
    }

    /**
     * Developer's routine to write this bridge to a file in the format used for samples stored in resource strings.
     * The file name is <code>samples.bdf</code> in the current directory.
     * 
     * @param id a unique resource id for this sample
     * @throws java.io.IOException something went wrong with the file writing.
     */
    public void writeSample(String id) throws IOException {
        File f = new File("samples.bdf");
        FileOutputStream os = new FileOutputStream(f, true);
        PrintWriter ps = new PrintWriter(os);
        String tag = designConditions.getTag();
        ps.println(tag + '_' + id + ".bridgeSampleName=" + projectId.substring(0, projectId.lastIndexOf('-')));
        ps.println(tag + '_' + id + ".bridgeSample=" + toString());
        ps.close();
    }
    
    /**
     * Read a bridge file represented as a sample string into this model.
     * 
     * @param s
     */
    public void read(String s) {
        try {
            parseBytes(s.getBytes("ASCII"));
        } 
        catch (IOException ex) { 
            Logger.getLogger(BridgeModel.class.getName()).log(Level.SEVERE, "syntax error in bridge as string", ex);
        }
    }
    
    /**
     * Developer's routine to write this bridge to a file in the format used to store template sketches.
     *  
     * The file name is <code>templates.bdf</code> in the current directory.
     * 
     * @param id a unique resource id for this template sketch
     * @throws java.io.IOException something went wrong with the file writing.
     */
    public void writeTemplate(String id) throws IOException {
        File f = new File("templates.bdf");
        FileOutputStream os = new FileOutputStream(f, true);
        PrintWriter ps = new PrintWriter(os);
        String tag = designConditions.getTag();
        ps.println(tag + '_' + id + ".bridgeSketchName=" + projectId.substring(0, projectId.lastIndexOf('-')));
        ps.print(tag + '_' + id + ".bridgeSketch=");
        ps.print(tag);
        ps.print(DELIM);
        ps.print(joints.size());
        ps.print(DELIM);
        ps.print(members.size());
        ps.print(DELIM);
        Iterator<Joint> ej = joints.iterator();
        while (ej.hasNext()) {
            Affine.Point pt = ej.next().getPointWorld();
            ps.print(pt.x);
            ps.print(DELIM);
            ps.print(pt.y);
            ps.print(DELIM);
        }
        Iterator<Member> em = members.iterator();
        if (em.hasNext()) {
            while (true) {
                Member member = em.next();
                ps.print(member.getJointA().getNumber());
                ps.print(DELIM);
                ps.print(member.getJointB().getNumber());
                if (em.hasNext()) {
                    ps.print(DELIM);
                } else {
                    break;
                }
            }
        }
        ps.println();
        ps.close();
    }
}
