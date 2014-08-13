/*
 * ExtendedComboBoxModel.java  
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

/**
 * Model for use with ExtendedCombobox.  Makes visible only items starting at a given base index.
 * 
 * @author Eugene K. Ressler
 */
public class ExtendedComboBoxModel extends AbstractListModel implements MutableComboBoxModel, Serializable {

    private int base = 0;
    private Object selectedObject;
    private ArrayList<Object> items = new ArrayList<Object>();

    /**
     * Return a default extended combobox model.
     */
    public ExtendedComboBoxModel() {}
            
    /**
     * Return an extended combobox model containing the given items.
     */
    public ExtendedComboBoxModel(final Object items[]) {
        initialize(items);
    }

    /**
     * Initialize this combobox model with the given array of items.
     * 
     * @param items combobox model initial items
     */
    public final void initialize(final Object items[]) {
        this.items.clear();
        this.items.addAll(Arrays.asList(items));
        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
        fireContentsChanged(this, 0, items.length - 1);
    }
    
    /**
     * Set the base index.  Only items starting at index <code>base</code>  through the end of the items vector
     * are accessible to the combobox.  The selection is preserved if it still lies in the accessible range.
     * 
     * @param base base index
     */
    public void setBase(int base) {
        if (base != this.base) {
            this.base = base;
            // Increase size in degenerate cases to prevent range errors.
            while (items.size() < base) {
                items.add(null);
            }
            int index = items.indexOf(selectedObject);
            if (0 <= index && index < base) {
                setSelectedItem(base < items.size() ? items.get(base) : null);
            }
            fireContentsChanged(this, 0, items.size() - base - 1);
        }
    }

    /**
     * Return the base index.  Only items starting at index <code>base</code>  through the end of the items vector
     * are accessible to the combobox.
     * 
     * @return base index
     */
    public int getBase() {
        return base;
    }

    /**
     * Return the size of the model, which is the number of accessible items.
     * 
     * @return number of accessible items
     */
    public int getSize() {
        return items.size() - base;
    }

    /**
     * Return the combobox model element at the given index or null if the index is out of range.
     * 
     * @param index index of item to return
     * @return item at given index or null if none
     */
    public Object getElementAt(int index) {
        index += base;
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        } else {
            return null;
        }
    }

    /**
     * Add a new item to the end of the combobox model.  If it's the only item in the combobox model, select it.
     * 
     * @param anObject item to add
     */
    public void addElement(Object anObject) {
        items.add(anObject);
        fireIntervalAdded(this, items.size() - base - 1, items.size() - base - 1);
        if (items.size() == base + 1 && selectedObject == null && anObject != null) {
            setSelectedItem(anObject);
        }
    }

    /**
     * Return the index of a given item adjusted so that the item at the base is zero.
     * 
     * @param anObject object to find the index of
     * @return index of object
     */
    public int getIndexOf(Object anObject) {
        int index = items.indexOf(anObject);
        return index < base ? -1 : index - base;
    }

    /**
     * Remove an item from the combobox model.
     * 
     * @param anObject item to remove
     */
    public void removeElement(Object anObject) {
        int index = getIndexOf(anObject);
        if (index != -1) {
            removeElementAt(index);
        }
    }

    /**
     * Insert an item into the combobox model at the given index.  Other items, if any, are shifted down.
     * 
     * @param anObject item to add
     * @param index index to add at
     */
    public void insertElementAt(Object anObject, int index) {
        items.add(index + base + 1, anObject);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Remove the item at the given index from the combobox model.
     * 
     * @param index index of item to remove
     */
    public void removeElementAt(int index) {
        if (getElementAt(index) == selectedObject) {
            if (index == 0) {
                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
            } else {
                setSelectedItem(getElementAt(index - 1));
            }
        }
        items.remove(index + base);
        fireIntervalRemoved(this, index, index);
    }

    /**
     * Select the given item in the combobox model.
     * 
     * @param anObject item to select
     */
    public void setSelectedItem(Object anObject) {
        if ((selectedObject != null && !selectedObject.equals(anObject)) ||
                selectedObject == null && anObject != null) {
            selectedObject = anObject;
            fireContentsChanged(this, -1, -1);
        }
    }

    /**
     * Return the selected item from the combobox model or null if none.
     * 
     * @return selected item or null if no item is selected
     */
    public Object getSelectedItem() {
        return selectedObject;
    }
}
