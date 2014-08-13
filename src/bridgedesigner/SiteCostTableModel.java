/*
 * SiteCostTableModel.java  
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
import java.util.Locale;
import javax.swing.table.DefaultTableModel;
import org.jdesktop.application.ResourceMap;

/**
 * Special table model for site costs. Used in the setup wizard.
 * 
 * @author Eugene K. Ressler
 */
public class SiteCostTableModel extends DefaultTableModel {

    // Cost calculations always in US dollars.
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private static final ResourceMap resourceMap = BDApp.getResourceMap(SiteCostTableModel.class);

    /**
     * Construct a new site cost table model.
     */
    public SiteCostTableModel() {
        super(6, 3);
        loadAt("deckCost.text", 0, 0);
        loadAt("excavationCost.text", 1, 0);
        loadAt("abutmentCost.text", 2, 0);
        loadAt("pierCost.text", 3, 0);
        loadAt("anchorageCost.text", 4, 0);
        loadAt("totalSiteCostNote.text", 5, 1);
    }

    private void loadAt(String key, int row, int column) {
        String s = resourceMap.getString(key);
        setValueAt(s, row, column);
    }

    /**
     * Initialize the site cost table model for given design conditions.
     * 
     * @param conditions design conditions
     */
    public void initialize(DesignConditions conditions) {
        // Get a label connoting abutment type.
        String abutmentType = resourceMap.getString(conditions.isArch() ? "arch.text" : "standard.text");
        NumberFormat intFormat = NumberFormat.getIntegerInstance(); // Locale-specific will be fine...
        // Calculate deck cost.
        setValueAt(resourceMap.getString("deckCostNote.text", conditions.getNPanels(), currencyFormat.format(conditions.getDeckCostRate())), 0, 1);
        final double deckCost = conditions.getNPanels() * conditions.getDeckCostRate();
        setValueAt(deckCost, 0, 2);
        // Calculate excavation cost.
        setValueAt(resourceMap.getString("excavationCostNote.text", 
                intFormat.format(conditions.getExcavationVolume()), currencyFormat.format(DesignConditions.excavationCostRate)), 1, 1);
        setValueAt(conditions.getExcavationCost(), 1, 2);
        // Abutment cost.
        setValueAt(resourceMap.getString("abutmentCostNote.text", abutmentType, currencyFormat.format(conditions.getAbutmentCost())), 2, 1);
        setValueAt(2 * conditions.getAbutmentCost(), 2, 2);
        // Pier cost.
        if (conditions.isPier()) {
            setValueAt(resourceMap.getString("pierNote.text", intFormat.format(conditions.getPierHeight())), 3, 1);
        } else {
            setValueAt(resourceMap.getString("noPierNote.text"), 3, 1);
        }
        setValueAt(conditions.getPierCost(), 3, 2);
        // Anchorage cost.
        int nAnchorages = conditions.getNAnchorages();
        if (nAnchorages == 0) {
            setValueAt(resourceMap.getString("noAnchoragesNote.text"), 4, 1);
        } else {
            setValueAt(resourceMap.getString("anchorageNote.text", nAnchorages, currencyFormat.format(DesignConditions.anchorageCost)), 4, 1);
        }
        final double anchorageCost = nAnchorages * DesignConditions.anchorageCost;
        setValueAt(anchorageCost, 4, 2);
        // Total cost.
        double totalCost = conditions.getTotalFixedCost();
        setValueAt(totalCost, 5, 2);
    }

    /**
     * Return false signifying all table cells are non-editable.
     * 
     * @param row row index
     * @param column column index
     * @return false
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /**
     * Return class of given column to control rendering of table items.
     * 
     * @param columnIndex column index
     * @return class to use for column rendering
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex < 2 ? String.class : Double.class;
    }
}
