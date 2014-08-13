/*
 * Editable.java  
 *   
 * Copyright (C) 2008 Eugene K. Ressler
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

/**
 * Interface for essential functionality of an editable item.  It is used by <code>ExtendedUndoManager</code>
 * and all related classes.  The command execurtion undo/redo mechanism requires three functionalities supported here: 
 * selection, indexing, and content swapping.  Selection allows commands to operate on the selected set.
 * Indexing allows commands to operate on vectors of items: insertion, deletion, modification, etc.
 * 
 * @author Eugene K. Ressler
 */
public interface Editable {
    /**
     * Return true iff this item is in a selected state.
     * 
     * @return selected state
     */
    public boolean isSelected();
    /**
     * Set the selected state of this item.
     * 
     * @param selected whether the item is selected
     * @return selected state
     */
    public boolean setSelected(boolean selected);
    /**
     * Return the zero-based index of this item. Value -1 is considered the null index.
     * 
     * @return index zero-based index of this item or -1 for a null index
     */
    public int getIndex();
    /**
     * Set the zero-based index of this item. Value -1 is considered the null index.
     * 
     * @param index zero-based index to set or -1 for null
     */
    public void setIndex(int index);
    /**
     * Swap all the contents of this object with another of exactly the same type.  Used for the undo/redo mechanism.
     * 
     * @param other editable to swap contents with
     */
    public void swapContents(Editable other);
}
