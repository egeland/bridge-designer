/*
 * Dispatcher.java  
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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Dispatch events flowing among UI components throughout the program.
 * Each kind of user interaction requires a distinct pattern of event flow.  This is complicated by the fact
 * that the default list selection model of Swing, used by the member table and stock selection combo boxes,
 * sends events to its listeners even if selections are made programmatically.  This isn't a good thing for
 * our application.  Subclassing the model with a wrapper would be a different option, but there are other
 * places where event flow control is needed, so this Dispatcher class handles them all at one place.
 * 
 * @author Eugene K. Ressler
 */
public class Dispatcher {

    private EditableBridgeModel bridge;
    private MemberTable memberTable;
    private MemberDetail memberDetail;
    private StockSelector stockSelector;
    private StockSelector popupStockSelector;
    private DraftingPanel draftingPanel;
    private ListSelectionListener memberTableSelectionListener;
    private ChangeListener stockSelectorListener;
    private ChangeListener popupStockSelectorListener;
    private ChangeListener bridgeSelectionChangeListener;
    private ChangeListener bridgeStructureChangeListener;
    private ChangeListener bridgeAnalysisChangeListener;

    private void enable() {
        memberTable.getSelectionModel().addListSelectionListener(memberTableSelectionListener);
        stockSelector.addChangeListener(stockSelectorListener);
        popupStockSelector.addChangeListener(popupStockSelectorListener);
        bridge.addSelectionChangeListener(bridgeSelectionChangeListener);
        bridge.addStructureChangeListener(bridgeStructureChangeListener);
        bridge.addAnalysisChangeListener(bridgeAnalysisChangeListener);
    }

    private void disable() {
        bridge.removeAnalysisChangeListener(bridgeAnalysisChangeListener);
        bridge.removeStructureChangeListener(bridgeStructureChangeListener);
        bridge.removeSelectionChangeListener(bridgeSelectionChangeListener);
        popupStockSelector.removeChangeListener(popupStockSelectorListener);
        stockSelector.removeChangeListener(stockSelectorListener);
        memberTable.getSelectionModel().removeListSelectionListener(memberTableSelectionListener);
    }

    /**
     * Connect this dispatcher to all the components whose events we will dispatch here.
     * 
     * @param b bridge being edited
     * @param mt member table component
     * @param md member detail component
     * @param ss stock selector, a composite containing three components
     * @param pss a second stock selector in a popup dialog
     * @param dp drafting panel component
     */
    public void initialize(EditableBridgeModel b, MemberTable mt, MemberDetail md, StockSelector ss, StockSelector pss, DraftingPanel dp) {
        bridge = b;
        memberTable = mt;
        memberDetail = md;
        stockSelector = ss;
        popupStockSelector = pss;
        draftingPanel = dp;
        memberTableSelectionListener = new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                // Ignore range selection final messages with no (set to -1) indices.
                // Also ignore adjusting messages while sorting.  The final one will do.
                if (!(e.getValueIsAdjusting() && memberTable.isSorting()) && e.getFirstIndex() >= 0) {
                    // Propagate actions due to member table selection.
                    disable();
                    // Make the selection in the bridge elements.
                    bridge.selectMembers(memberTable, e.getFirstIndex(), e.getLastIndex());
                    
                    // A couple things are only worth doing at mouse up.
                    if (!e.getValueIsAdjusting()) {
                        // Adjust the stock selector to the new selection.
                        stockSelector.matchSelection(bridge);
                        // Update the member detail. This might use the stock selector, so must come after.
                        memberDetail.update(true);
                    }
                    // Repaint the drafting panel.
                    draftingPanel.paintBackingStore();
                    draftingPanel.repaint();
                    enable();
                }
            }
        };
        stockSelectorListener = new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                // Propagate actions due to stock selection change.
                // Change the selection in the bridge.
                disable();
                bridge.changeSelectedMembers(stockSelector.getMaterialIndex(),
                        stockSelector.getSectionIndex(), stockSelector.getSizeIndex());
                memberTable.fireTableDataChanged();
                memberTable.loadSelection();
                memberDetail.update(false);
                draftingPanel.paintBackingStore();
                draftingPanel.repaint();
                enable();
            }
        };
        popupStockSelectorListener = new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                // Propagate actions due to stock selection change.
                // Change the selection in the bridge.
                disable();
                bridge.changeSelectedMembers(popupStockSelector.getMaterialIndex(),
                        popupStockSelector.getSectionIndex(), popupStockSelector.getSizeIndex());
                stockSelector.match(popupStockSelector);
                memberTable.fireTableDataChanged();
                memberTable.loadSelection();
                memberDetail.update(false);
                draftingPanel.paintBackingStore();
                draftingPanel.repaint();
                enable();
            }
        };
        bridgeSelectionChangeListener = new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                disable();
                // Propagate actions due to bridge selection change.
                // Adjust the stock selector to the new selection.
                stockSelector.matchSelection(bridge);
                // Change the member table selection to match the bridge.
                memberTable.loadSelection();
                memberDetail.update(true);
                // Repaint the drafting panel.  // DEBUG: Try painting first.
                draftingPanel.paintBackingStore();
                draftingPanel.repaint();
                enable();
            }
        };
        bridgeStructureChangeListener = new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                disable();
                // Adjust selection because old one may have been deleted.
                stockSelector.matchSelection(bridge);
                // Propagate actions due to brige structure change.
                memberTable.fireTableDataChanged();
                memberTable.loadSelection();
                memberDetail.update(true);
                draftingPanel.paintBackingStore(); // DEBUG: Try painting first.
                draftingPanel.repaint();
                // DEBUG: Probably have to load stock selector here for case
                // where bridge change is an undo of a member stock change.
                enable();
            }
        };
        bridgeAnalysisChangeListener = new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                disable();
                // Propagate actions due to analysis change.
                memberTable.fireTableDataChanged();
                memberTable.loadSelection();
                memberDetail.update(false);
                draftingPanel.paintBackingStore(); 
                draftingPanel.repaint();
                enable();
            }
        };
        // Initiallize listeners in "enabled" state.
        enable();
    }

    /**
     * Handle increment and decrement button presses as pseudo-events from the view window.
     * 
     * @param inc +1 or -1 for increment or decrement member size
     */
    public void incrementMemberSize(int inc) {
        disable();
        bridge.incrementMemberSize(inc);
        stockSelector.matchSelection(bridge);
        popupStockSelector.match(stockSelector);
        memberTable.fireTableDataChanged();
        memberTable.loadSelection();
        memberDetail.update(false);
        draftingPanel.paintBackingStore();
        draftingPanel.repaint();
        enable();
    }
}
