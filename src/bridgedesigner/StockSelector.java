/*
 * StockSelector.java  
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
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A composite pattern implementation of components representing a stock selector.
 * 
 * @author Eugene K. Ressler
 */
public class StockSelector {

    private final JComboBox materialBox;
    private final JComboBox sectionBox;
    private final JComboBox sizeBox;
    private final ItemListener materialBoxSelectionListener;
    private final ItemListener sectionBoxSelectionListener;
    private final ItemListener sizeBoxSelectionListener;
    private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

    /**
     * Construct a new stock selector with given components.
     * 
     * @param material material selection combobox
     * @param section section selection combobox
     * @param size size selection combobox
     */
    public StockSelector(JComboBox material, JComboBox section, JComboBox size) {
        this.materialBox = material;
        this.sectionBox = section;
        this.sizeBox = size;

        // We need the disable() / enable() mechanism because programmatically changing the combobox
        // selections causes change events, and we can't afford these to propagate because we can
        // the send multiple change events for each stock selector change due to the action of
        // synchSizeBoxToSelection(), e.g.
        materialBoxSelectionListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    disable();
                    fireStateChanged();
                    enable();
                }
            }
        };
        sectionBoxSelectionListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    disable();
                    synchSizeBoxToSection();
                    fireStateChanged();
                    enable();
                }
            }
        };
        sizeBoxSelectionListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    disable();
                    fireStateChanged();
                    enable();
                }
            }
        };
        enable();
    }

    /**
     * Return the selector's material index.
     * 
     * @return material index
     */
    public int getMaterialIndex() {
        return materialBox.getSelectedIndex();
    }

    /**
     * Return the selector's section index.
     * 
     * @return section index
     */
    public int getSectionIndex() {
        return sectionBox.getSelectedIndex();
    }

    /**
     * Return the selector's size index.
     * 
     * @return size index
     */
    public int getSizeIndex() {
        return sizeBox.getSelectedIndex();
    }

    /**
     * Clear the selection.  All comboboxes show blank.
     */
    public void clear() {
        materialBox.setSelectedIndex(-1);
        sectionBox.setSelectedIndex(-1);
        sizeBox.setSelectedIndex(-1);
    }

    /**
     * Add a change listener to this stock selector.
     * 
     * @param listener change listener to add
     */
    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Remove a change listener from this stock selector.
     * 
     * @param listener change listener to remove
     */
    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    /**
     * Return a flag that indicates whether size selector can be
     * incremented or decremented (or neither).
     * 
     * @return flag with bits <code>Inventory.SHAPE_DECREASE_SIZE</code> or 
     * <code>Inventory.SHAPE_INCREASE_SIZE</code> or neither set. 
     */
    public int getAllowedShapeChanges() {
        int sizeIndex = sizeBox.getSelectedIndex();
        if (sizeIndex < 0) {
            return 0;
        }
        int mask = 0;
        if (sizeIndex > 0) {
            mask |= Inventory.SHAPE_DECREASE_SIZE;
        }
        if (sizeIndex < sizeBox.getItemCount() - 1) {
            mask |= Inventory.SHAPE_INCREASE_SIZE;
        }
        return mask;
    }
    
    /**
     * Increment the selection of the size box if possible.  If nothing was selected, selects first item.
     * 
     * @param inc
     */
    public void incrementSize(int inc) {
        int index = sizeBox.getSelectedIndex();
        if (index >= 0 && 0 <= index + inc && index + inc < sizeBox.getItemCount()) {
            sizeBox.setSelectedIndex(index + inc);
        }
    }
    
    /**
     * Descriptor for state of the stock selector.
     */
    public static class Descriptor {
        /**
         * Selected material index.
         */
        public int materialIndex;
        /**
         * Selected section index.
         */
        public int sectionIndex;
        /**
         * Selected size index.
         */
        public int sizeIndex;
        
        /**
         * Construct a new default descriptor of stock selector state. Nothing is selected.
         */
        public Descriptor() {            
            materialIndex = sectionIndex = sizeIndex = -1;
        }
        
        /**
         * Construct a new descriptor of stock selector state with given indices.
         * 
         * @param materialIndex selected material index
         * @param sectionIndex selected section index
         * @param sizeIndex selecgted size index
         */
        public Descriptor(int materialIndex, int sectionIndex, int sizeIndex) {
            this.materialIndex = materialIndex;
            this.sectionIndex = sectionIndex;
            this.sizeIndex = sizeIndex;
        }

        /**
         * Construct a new desciptor of stock composing the given member.
         * 
         * @param member member with stock used for descritor state
         */
        public Descriptor(Member member) {
            this.materialIndex = member.getMaterial().getIndex();
            this.sectionIndex = member.getShape().getSection().getIndex();
            this.sizeIndex = member.getShape().getSizeIndex();
        }

        /**
         * Return true iff this descriptor is equal to another.
         * 
         * @param obj other descriptor
         * @return true iff this descriptor is equal to another
         * @exception CastError if called on an object that is not a descriptor
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Descriptor) {
                Descriptor other = (Descriptor)obj;
                return materialIndex == other.materialIndex &&
                        sectionIndex == other.sectionIndex &&
                        sizeIndex == other.sizeIndex;
            }
            return false;
        }

        /**
         * Return a hash code for this descriptor.
         * 
         * @return hash code
         */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.materialIndex;
            hash = 79 * hash + this.sectionIndex;
            hash = 79 * hash + this.sizeIndex;
            return hash;
        }
    }

    /**
     * Inform the table model to reload the size box.
     */ 
    private void synchSizeBoxToSection() {
        ((Inventory.SizeBoxModel) sizeBox.getModel()).setSectionIndex(sectionBox.getSelectedIndex());        
    }

    /**
     * Set the selection in this stock selector to match another one.
     * 
     * @param other the other stock selector to match
     */
    public void match(StockSelector other) {
        disable();
        materialBox.setSelectedIndex(other.getMaterialIndex());
        sectionBox.setSelectedIndex(other.getSectionIndex());
        synchSizeBoxToSection();
        sizeBox.setSelectedIndex(other.getSizeIndex());
        enable();
    }

    private void initialize(Descriptor descriptor) {
        if (descriptor != null) {
            materialBox.setSelectedIndex(descriptor.materialIndex);
            sectionBox.setSelectedIndex(descriptor.sectionIndex);
            synchSizeBoxToSection();
            sizeBox.setSelectedIndex(descriptor.sizeIndex);
        }
    }

    /**
     * Set the selection in this stock selector to match the members selected in the given bridge.
     * If the selection includes multiple materials, shapes, or sizes, the respective combobox is set to blank.
     * 
     * @param bridge bridge whose selection to match in this selector
     */
    public void matchSelection(EditableBridgeModel bridge) {
        disable();
        Descriptor descriptor = bridge.getSelectedStock();
        if (descriptor == null) {
            descriptor = bridge.getMostCommonStock();
            if (getMaterialIndex() != -1) {
                descriptor.materialIndex = getMaterialIndex();
            }
            if (getSectionIndex() != -1) {
                descriptor.sectionIndex = getSectionIndex();
            }
            if (getSizeIndex() != -1) {
                descriptor.sizeIndex = getSizeIndex();
            }
        }
        initialize(descriptor);
        enable();
    }

    /**
     * Set this selector to the most common stock used in the given bridge.
     * 
     * @param bridge bridge with most common stock used to set this selector
     */
    public void setMostCommonStockOf(BridgeModel bridge) {
        initialize(bridge.getMostCommonStock());
    }

    private void enable() {
        materialBox.addItemListener(materialBoxSelectionListener);
        sectionBox.addItemListener(sectionBoxSelectionListener);
        sizeBox.addItemListener(sizeBoxSelectionListener);
    }

    private void disable() {
        sizeBox.removeItemListener(sizeBoxSelectionListener);
        sectionBox.removeItemListener(sectionBoxSelectionListener);
        materialBox.removeItemListener(materialBoxSelectionListener);
    }

    private void fireStateChanged() {
        Iterator<ChangeListener> e = new ArrayList<ChangeListener>(changeListeners).iterator();
        while (e.hasNext()) {
            e.next().stateChanged(new ChangeEvent(this));
        }
    }
}
