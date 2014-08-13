/*
 * CostReportTableModel.java  
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

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import javax.swing.table.DefaultTableModel;
import org.jdesktop.application.ResourceMap;

/**
 * Cost report table model.
 * 
 * @author Eugene K. Ressler
 */
public class CostReportTableModel extends DefaultTableModel {

    private static String [] columnIds;
    private static final ResourceMap resourceMap = BDApp.getResourceMap(CostReportTableModel.class);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);        
    private final NumberFormat intFormat = NumberFormat.getIntegerInstance(); // Locale-specific will be fine...

    /**
     * Construct a new cost report table model.
     */
    public CostReportTableModel() {
        super(0, 4); // Zero rows, 4 cols.
    }

    /**
     * Initialize the table model with the given cost summary.
     * 
     * @param costs cost summary
     */
    public void initialize(EditableBridgeModel.Costs costs) {
        if (columnIds == null) {
            columnIds = resourceMap.getString("columnIds.text").split("\\|");
        }
        setColumnIdentifiers(columnIds);
        int nMaterialRows = costs.materialSectionPairs.size();
        int nProductRows = costs.materialShapePairs.size();
        setRowCount(nMaterialRows + 1 + nProductRows + 5 + 1 + 4);
        
        int row = 0;
        Iterator<EditableBridgeModel.MaterialSectionPair> mtlSecIt = costs.materialSectionPairs.keySet().iterator();
        boolean initial = true;
        double totalMtlCost = 0.00;
        while (mtlSecIt.hasNext()) {
            EditableBridgeModel.MaterialSectionPair pair = mtlSecIt.next();
            // Column 0
            if (initial) {
                loadAt("materialCost.text", row, 0);            
                initial = false;
            }
            else {
                setValueAt(null, row, 0);
            }
            // Column 1
            setValueAt(pair, row, 1);
            // Column 2
            double weight = costs.materialSectionPairs.get(pair);
            double cost = pair.material.getCost(pair.section);
            setValueAt(resourceMap.getString("materialCostNote.text", weight, currencyFormat.format(cost)), row, 2);
            // Column 3
            double mtlCost = 2 * weight * cost;
            setValueAt(mtlCost, row, 3);
            totalMtlCost += mtlCost;
            row++;
        }
        // Blank row for double rule.
        for (int col = 0; col < 4; col++) {
            setValueAt(null, row, col);
        }
        row++;
        // Connection row
        loadAt("connectionCost.text",  row, 0);
        setValueAt(null, row, 1);
        int nConnections = costs.nConnections;
        final double connectionFee = costs.inventory.getConnectionFee();
        setValueAt(resourceMap.getString("connectionCostNote.text", nConnections, connectionFee), row, 2);
        final double connectionCost = 2 * nConnections * connectionFee;
        setValueAt(connectionCost, row, 3);
        row++;
        // Blank row for double rule.
        for (int col = 0; col < 4; col++) {
            setValueAt(null, row, col);
        }
        row++;
        // Product costs.
        Iterator<EditableBridgeModel.MaterialShapePair> mtlShpIt = costs.materialShapePairs.keySet().iterator();
        initial = true;
        while (mtlShpIt.hasNext()) {
            EditableBridgeModel.MaterialShapePair pair = mtlShpIt.next();
            // Column 0
            if (initial) {
                loadAt("productCost.text", row, 0);
                initial = false;
            }
            else {
                setValueAt(null, row, 0);
            }
            // Column 1
            String nUsed = costs.materialShapePairs.get(pair).toString();
            nUsed = "   ".substring(nUsed.length()) + nUsed;
            setValueAt(nUsed + " - " + pair, row, 1);
            // Column 2
            setValueAt(resourceMap.getString("productCostNote.text", 
                    currencyFormat.format(costs.inventory.getOrderingFee())), row, 2);
            // Column 3
            setValueAt(costs.inventory.getOrderingFee(), row, 3);
            row++;
        }
        // Blank row for double rule.
        for (int col = 0; col < 4; col++) {
            setValueAt(null, row, col);
        }
        row++;
        final double totalProductCost = nProductRows * costs.inventory.getOrderingFee();
        // Site costs.  Code is similar to SiteCostTableModel.  If you change this, you probably need to change that.
        loadAt("siteCost.text", row, 0);
        loadAt("deckCost.text", row, 1);
        setValueAt(resourceMap.getString("deckCostNote.text", 
                costs.conditions.getNPanels(), currencyFormat.format(costs.conditions.getDeckCostRate())), row, 2);
        final double deckCost = costs.conditions.getNPanels() * costs.conditions.getDeckCostRate();
        setValueAt(deckCost, row, 3);
        row++;
        setValueAt(null, row, 0);
        loadAt("excavationCost.text", row, 1);
        setValueAt(resourceMap.getString("excavationCostNote.text", 
                intFormat.format(costs.conditions.getExcavationVolume()), 
                currencyFormat.format(DesignConditions.excavationCostRate)), row, 2);
        setValueAt(costs.conditions.getExcavationCost(), row, 3);
        row++;
        setValueAt(null, row, 0);
        loadAt("abutmentCost.text", row, 1);
        final String abutmentType = resourceMap.getString(costs.conditions.isArch() ? "arch.text" : "standard.text");
        setValueAt(resourceMap.getString("abutmentCostNote.text", 
                abutmentType, currencyFormat.format(costs.conditions.getAbutmentCost())), row, 2);
        setValueAt(2 * costs.conditions.getAbutmentCost(), row, 3);
        row++;
        setValueAt(null, row, 0);
        loadAt("pierCost.text", row, 1);
        if (costs.conditions.isPier()) {
            setValueAt(resourceMap.getString("pierNote.text", 
                    intFormat.format(costs.conditions.getPierHeight())), row, 2);
        } else {
            setValueAt(resourceMap.getString("noPierNote.text"), row, 2);
        }
        setValueAt(costs.conditions.getPierCost(), row, 3);
        row++;
        setValueAt(null, row, 0);
        loadAt("anchorageCost.text", row, 1);
        final int nAnchorages = costs.conditions.getNAnchorages();
        if (nAnchorages == 0) {
            setValueAt(resourceMap.getString("noAnchoragesNote.text"), row, 2);
        } else {
            setValueAt(resourceMap.getString("anchorageNote.text", 
                    nAnchorages, currencyFormat.format(DesignConditions.anchorageCost)), row, 2);
        }
        final double anchorageCost = nAnchorages * DesignConditions.anchorageCost;
        setValueAt(anchorageCost, row, 3);
        row++;
        // Blank row for double rule.
        for (int col = 0; col < 4; col++) {
            setValueAt(null, row, col);
        }
        row++;
        loadAt("totalCost.text", row, 0);
        loadAt("sum.text", row, 1);
        setValueAt(resourceMap.getString("sumNote.text",
                currencyFormat.format(totalMtlCost),
                currencyFormat.format(connectionCost),
                currencyFormat.format(totalProductCost),
                currencyFormat.format(costs.conditions.getTotalFixedCost())), row, 2);
        setValueAt(totalMtlCost + connectionCost + totalProductCost + costs.conditions.getTotalFixedCost(), row, 3);        
    }

    /**
     * Load a resource string with tiven key at the specific row and column of the table model.
     * 
     * @param key resource key
     * @param row row index
     * @param column column index
     */
    private void loadAt(String key, int row, int column) {
        setValueAt(resourceMap.getString(key), row, column);
    }

    /**
     * Return true iff cells are editable.  In this table, none are.
     * @param row row index
     * @param column column index
     * @return false in all cases
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    /**
     * Return the class of data in the various columns in order to determine their rendering.
     * 
     * @param columnIndex index of column
     * @return class of column given by index
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex < 3 ? String.class : Double.class;
    }
}
