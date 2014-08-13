/*
 * DesignIterationTableModel.java  
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

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import org.jdesktop.application.ResourceMap;

/**
 * Table of design iterations.
 * 
 * @author Eugene K. Ressler
 */
public class DesignIterationTableModel extends AbstractTableModel {

    private final EditableBridgeModel bridge;
    private final BridgeModel cartoonBridge;
    private final BridgeDraftingView cartoonView;
    private int iterationCount;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);        
    private final String [] headers;
    
    /**
     * Construct a design iteration table for the given bridge.
     * 
     * @param bridge bridge containing row data
     */
    public DesignIterationTableModel(final EditableBridgeModel bridge) {
        this.bridge = bridge;
        ResourceMap resourceMap = BDApp.getResourceMap(DesignIterationTableModel.class);
        headers = resourceMap.getString("tableHeaders.text").split(";");
        cartoonBridge = new BridgeModel();
        cartoonView = new BridgeDraftingView(cartoonBridge);
        iterationCount = bridge.getIterationCount();
        bridge.addIterationChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                int newIterationCount = bridge.getIterationCount();
                int delta = newIterationCount - iterationCount;
                if (delta < 0) {
                    fireTableRowsDeleted(iterationCount + delta, iterationCount - 1);
                }
                else if (delta > 0) {
                    fireTableRowsInserted(newIterationCount - delta, newIterationCount - 1);
                }
                iterationCount = newIterationCount;
            }
        });
    }

    /**
     * Return the bridge containing row data for this table.
     * 
     * @return bridge containing row data
     */
    public EditableBridgeModel getBridge() {
        return bridge;
    }

    /**
     * Return the view for drawing the small cartoon view of the iteration.
     * 
     * @return cartoon view
     */
    public BridgeDraftingView getBridgeView() {
        return cartoonView;
    }
    
    /**
     * Load the cartoon with the bridge captured in the iteration with given index.
     * 
     * @param index index of iteration in the list to load
     */
    public void loadCartoon(int index) {
        if (0 <= index && index < bridge.getIterationCount()) {
            try {
                cartoonBridge.parseBytes(bridge.getDesignIteration(index).getBridgeModelAsBytes());
                cartoonView.initialize(cartoonBridge.getDesignConditions());
            } catch (IOException ex) {  }
        }
    }
    
    public int getRowCount() {
        return bridge.getIterationCount();
    }

    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return IconFactory.bridgeStatus(bridge.getDesignIteration(rowIndex).getBridgeStatus());
            case 1:
                return bridge.getDesignIteration(rowIndex).getNumber();
            case 2:
                return currencyFormat.format(bridge.getDesignIteration(rowIndex).getCost()) + " ";
            case 3:
                return " " + bridge.getDesignIteration(rowIndex).getProjectId();
        }
        return null;
    }
}
