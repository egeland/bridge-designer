/*
 * MemberDetail.java  
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

import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdesktop.application.ResourceMap;

/**
 * A composite pattern implementation of components representing a member detail.
 * 
 * @author Eugene K. Ressler
 */
public class MemberDetail implements ChangeListener {

    private final EditableBridgeModel bridge;
    private final StockSelector stockSelector;
    private final JTabbedPane tabs;
    private final JPanel memberDetailPanel;
    private final JTable materialTable;
    private final JTable dimensionsTable;
    private final JTable costTable;
    private final CrossSectionSketch crossSectionSketch;
    private final JComboBox memberSelectBox;
    private final JCheckBox graphAllCheck;
    private final StrengthCurve strengthCurve;
    private final ResourceMap resourceMap;
    private Member [] [] memberLists;

    /**
     * Construct a fresh member detail.
     * 
     * @param bridge bridge model containing members to detail
     * @param stockSelector stock selector the member detail should respond to
     * @param tabs tabs of the member detail
     * @param memberDetailPanel the panel containing the member detail
     * @param materialTable table containing material information
     * @param dimensionsTable table containing material dimensions
     * @param costTable table containing member cost information
     * @param crossSectionSketch small drawing of the member cross-section
     * @param memberSelectBox combo box to select a member number when selected tab has more than one
     * @param graphAllCheck checkbox to indicate strength curves of all tab contents should be drawn simultaneously
     * @param strengthCurve strength curve drawing area
     */
    public MemberDetail(
            EditableBridgeModel bridge,
            StockSelector stockSelector,
            JTabbedPane tabs, 
            JPanel memberDetailPanel,
            JTable materialTable, 
            JTable dimensionsTable, 
            JTable costTable, 
            JLabel crossSectionSketch, 
            JComboBox memberSelectBox,
            JCheckBox graphAllCheck,
            JLabel strengthCurve) {
        this.bridge = bridge;
        this.stockSelector = stockSelector;
        this.tabs = tabs;
        this.memberDetailPanel = memberDetailPanel;
        this.materialTable = materialTable;
        this.dimensionsTable = dimensionsTable;
        this.costTable = costTable;
        this.crossSectionSketch = (CrossSectionSketch) crossSectionSketch;
        this.memberSelectBox = memberSelectBox;
        this.graphAllCheck = graphAllCheck;
        this.strengthCurve = (StrengthCurve)strengthCurve;
        this.resourceMap = BDApp.getResourceMap(MemberDetail.class);
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateTabDependencies();
            }
        });
    }

    private String tabLabel(Member [] members) {
        StringBuilder s = new StringBuilder(); 
        if (members.length == 1) {
            s.append(resourceMap.getString("tab1.text", members[0].getNumber()));
        }
        else {
            int startRange = members[0].getNumber();
            int endRange = startRange;
            s.append(resourceMap.getString("tabMany.text", startRange));
            for (int i = 1; i < members.length; i++) {
                int n = members[i].getNumber();
                if (n == endRange + 1) {
                    endRange = n;
                }
                else {
                    if (endRange > startRange) {
                        s.append((endRange == startRange + 1) ? ',' : '-');
                        s.append(endRange);
                    }
                    s.append("," + n);
                    startRange = endRange = n;
                } 
            }
            if (endRange > startRange) {
                s.append((endRange == startRange + 1) ? ',' : '-');
                s.append(endRange);
            }
        }
        return s.toString();
    }

    private String truncate(String s, int length) {
        for (int i = length; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                return s.substring(0, i) + "...";
            }
        }
        return s;
    }
    
    private void updateTabDependencies() {
        int tabSelectedIndex = tabs.getSelectedIndex();
        if (tabSelectedIndex >= 0) {
            Material material = null;
            Shape shape = null;
            Member [] members = null;
            if (memberLists == null || memberLists.length == 0) {
                Inventory inventory = bridge.getInventory();
                int materialIndex = stockSelector.getMaterialIndex();
                material = (materialIndex == -1) ? null : inventory.getMaterial(materialIndex);
                int sectionIndex = stockSelector.getSectionIndex();
                int sizeIndex = stockSelector.getSizeIndex();
                shape = (sectionIndex == -1 || sizeIndex == -1) ? null : inventory.getShape(sectionIndex, sizeIndex);
            }
            else {
                members = memberLists[tabSelectedIndex];
                material = members[0].getMaterial();
                shape = members[0].getShape();
            }
            crossSectionSketch.initialize(shape);
            if (material == null) {
                for (int i = 0; i < 4; i++) {
                    materialTable.setValueAt("--", i, 1);
                }
            }
            else {
                materialTable.setValueAt(material.getName(), 0, 1);
                materialTable.setValueAt(resourceMap.getString("fyFormat.text", material.getFy()), 1, 1);
                materialTable.setValueAt(resourceMap.getString("eFormat.text", material.getE()), 2, 1);
                materialTable.setValueAt(resourceMap.getString("densityFormat.text", material.getDensity()), 3, 1); 
            }
            if (shape == null) {
                for (int i = 0; i < 4; i++) {
                    dimensionsTable.setValueAt("--", i, 1);
                }
            }
            else {
                dimensionsTable.setValueAt(shape.getSection().getName(), 0, 1);
                dimensionsTable.setValueAt(shape.getName(), 1, 1);
                dimensionsTable.setValueAt(resourceMap.getString("areaFormat.text", shape.getArea()), 2, 1);
                dimensionsTable.setValueAt(resourceMap.getString("momentFormat.text", shape.getMoment()), 3, 1);
            }
            double memberLength = 0;
            if (members == null || members.length > 1) {
                dimensionsTable.setValueAt("--", 4, 1);
            }
            else {
                memberLength = members[0].getLength();
                dimensionsTable.setValueAt(resourceMap.getString("lengthFormat.text", memberLength), 4, 1);
            }
            if (material == null || shape == null) {
                costTable.setValueAt("--", 0, 1);
                costTable.setValueAt("--", 1, 1);
            }
            else {
                double cost = material.getCost(shape.getSection()) * shape.getArea() * material.getDensity();
                costTable.setValueAt(resourceMap.getString("costPerMeterFormat.text", cost), 0, 1);
                if (memberLength == 0) {
                    costTable.setValueAt("--", 1, 1);
                }
                else {
                    costTable.setValueAt(resourceMap.getString("totalCostFormat.text", cost * memberLength), 1, 1);                
                }
            }
            DesignConditions conditions = bridge.getDesignConditions();
            strengthCurve.initialize(material, shape, members, memberLists, 
                    bridge.isAnalysisValid() ? bridge.getAnalysis() : null, 
                    conditions == null ? false : (conditions.getNAnchorages() == 0));
            if (memberLists != null && tabSelectedIndex < memberLists.length && memberLists[tabSelectedIndex].length > 1) {
                memberSelectBox.setEnabled(true);
                Object [] memberNumbers = new Object [memberLists[tabSelectedIndex].length];
                for (int i = 0; i < memberNumbers.length; i++) {
                    memberNumbers[i] = memberLists[tabSelectedIndex][i].getNumber();
                }
                ((ExtendedComboBoxModel)memberSelectBox.getModel()).initialize(memberNumbers);
            }
            else {
                memberSelectBox.removeAllItems();
                memberSelectBox.setEnabled(false);                
            }
        }
    }

    private void initializeTabs(Member [] [] memberLists) {
        this.memberLists = memberLists;
        
        // Load the tabs.
        tabs.removeAll();
        if (memberLists == null || memberLists.length == 0) {
            tabs.addTab(resourceMap.getString("current.text"), memberDetailPanel);
            graphAllCheck.setEnabled(false);
            memberSelectBox.setEnabled(false);
        }
        else {
            for (int i = 0; i < memberLists.length; i++) {
                String label = tabLabel(memberLists[i]);
                tabs.addTab(truncate(label, 16), null, i == 0 ? memberDetailPanel : null, label);
            }
            graphAllCheck.setEnabled(memberLists.length > 1);
        }
    }

    /**
     * Update the member detail to be consistent with current selection and stock selection.
     * 
     * @param memberListChange whether there could be a change in the selection; false means stock change only
     */
    public void update(boolean memberListChange) {
        if (memberListChange) {
            initializeTabs(bridge.getSelectedStockLists());
        }
        updateTabDependencies();
        strengthCurve.repaint();
    }
    
    /**
     * Delegate for state changes in the show all checkbox.
     * 
     * @param e checkbox item event
     */
    public void handleShowAllStateChange(ItemEvent e) {
        strengthCurve.setShowAll(e.getStateChange() == ItemEvent.SELECTED);
    }
    
    /**
     * Delegate for state changes in the member selection combobox.
     * 
     * @param e member selection combobox item event
     */
    public void handleMemberSelectChange(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            strengthCurve.setSelectedMemberIndex(memberSelectBox.getSelectedIndex());
        }
    }

    /**
     * Implemetation of the state change handler for the change listener interface.
     * 
     * @param e state change event
     */
    public void stateChanged(ChangeEvent e) {
        update(true);
    }
}
