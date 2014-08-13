/*
 * EditCommand.java  
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

import java.util.ArrayList;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.jdesktop.application.ResourceMap;

/**
 * Basic functionality for bridge editing commands that are undoable/redoable.
 * 
 * @author Eugene K. Ressler
 */
public abstract class EditCommand extends AbstractUndoableEdit {
   
    /**
     * Bridge that this command operates on.
     */
    protected EditableBridgeModel bridge;
    /**
     * Undo manager where this command should be recorded.
     */
    protected ExtendedUndoManager undoManager;
    /**
     * Resource map for the edit command class should be used by all subclasses for their presentation name text.
     */
    protected static final ResourceMap resourceMap = BDApp.getResourceMap(EditCommand.class);
    /**
     * Presentation name for this command, used for tool tips and undo/redo drop lists.
     */
    protected String presentationName = defaultPresentationName;
    private static final String defaultPresentationName = getString("command.text");
    
    /**
     * Go ahead with this command.
     */
    abstract void go();
    
    /**
     * Go back to the way the bridge was before the command.
     */
    abstract void goBack();
    
    /**
     * Create a fresh edit command for the given bridge.  The bridge
     * is used only by subclasses.
     * 
     * @param bridge bridge this command is for
     */
    public EditCommand(EditableBridgeModel bridge) {
        this.bridge = bridge;
    }

    /**
     * Get a string from resource storage for edit command presentation text.
     * 
     * @param key key for the string to be fetched
     * @return string from resource storage
     */
    public static String getString(String key) {
        return resourceMap.getString(key);
    }

    @Override
    public String getPresentationName() {
        return presentationName;
    }
    
    /**
     * Execute a command in the given undo manager.
     * 
     * @param undoManager manager used to save the caommand for possible later undoing
     * @return zero if execution succeeded, else an error code specified by a subclass
     */
    int execute(ExtendedUndoManager undoManager) {
        this.undoManager = undoManager;
        go();
        undoManager.addEdit(this);
        // Fire after adding so handlers can see manager state change.
        undoManager.fireAfter(this);
        return 0;
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        go();
        undoManager.fireAfter(this);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        goBack();
        undoManager.fireAfter(this);
    }

    @Override
    public void die() {
        super.die();
        undoManager = null;
    }

    /**
     * Helper function to build presentation text for UI purposes when the text
     * is composed of a prefix followed by a list of member numbers.
     * Examples:
     *   Delete member.
     *   Delete member 3.
     *   Delete members 12 and 34.
     *   Delete members 1, 2, 5 and 7.
     * 
     * @param key resource key for prefix text
     * @param members array of members from which to take list numbers
     * @return message text
     */
    protected static String getMembersMessage(String key, Member[] members) {
        if (members.length <= 0) {
            return getString(key) + ".";
        }
        if (members.length == 1) {
            return getString(key) + " " + members[0].getNumber() + ".";
        } else {
            StringBuilder s = new StringBuilder(64);
            s.append(getString(key + ".many"));
            s.append(" ");
            s.append(members[0].getNumber());
            int i = 1;
            while (i < members.length - 1) {
                s.append(", ");
                s.append(members[i++].getNumber());
            }
            s.append(" ");
            s.append(getString("and.text"));
            s.append(" ");
            s.append(members[i].getNumber());
            s.append(".");
            return s.toString();
        }
    }

    // Repair a gross lack in the ArrayList API.
    private static void setSize(ArrayList<?> a, int size) {
        while (a.size() < size) {
            a.add(null);
        }
        while (a.size() > size) {
            a.remove(a.size() - 1);
        }
    }

    /**
     * Insert new items in a vector.  The items are Editables with index
     * fields assumed to be already set in ascending order. These are used
     * to place the new items.  The vector is re-indexed.
     * 
     * @param <T> inserted vector element type
     * @param v vector to do the insertion in
     * @param items items to insert
     */
    protected static <T extends Editable> void insert(ArrayList<T> v, T[] items) {
        int oldBmSize = v.size();
        int newBmSize = oldBmSize + items.length;
        setSize(v, newBmSize);
        int iSrc = oldBmSize - 1;
        int iDst = newBmSize - 1;
        for (int iCmd = items.length - 1; iCmd >= 0; iCmd--) {
            while (iDst > items[iCmd].getIndex()) {
                v.get(iSrc).setIndex(iDst);
                v.set(iDst--, v.get(iSrc--));
            }
            v.set(iDst--, items[iCmd]);
        }
    }

    /**
     * Delete items from a vector.  The items must be an array containing
     * a subset of elements in the given vector and in ascending order.
     * The items are removed from the vector and the remaining ones re-indexed.
     * 
     * @param <T> type of editable to be deleted
     * @param v vector of editables to be edited
     * @param items array items to be deleted in ascending order
     */
    protected static <T extends Editable> void delete(ArrayList<T> v, T[] items) {
        int oldBmSize = v.size();
        int newBmSize = oldBmSize - items.length;
        int iDst = 0;
        int iSrc = 0;
        for (int iCmd = 0; iCmd < items.length; iCmd++) {
            while (iSrc < items[iCmd].getIndex()) {
                v.get(iSrc).setIndex(iDst);
                v.set(iDst++, v.get(iSrc++));
            }
            items[iCmd] = v.get(iSrc++);
        }
        while (iSrc < oldBmSize) {
            v.get(iSrc).setIndex(iDst);
            v.set(iDst++, v.get(iSrc++));
        }
        setSize(v, newBmSize);
    }

    /**
     * Swap the contents of the given item with the contents of the same-index item in the given vector.
     * 
     * @param <T> type of editable to be swapped
     * @param v vector to be edited
     * @param item item to swap with same-index item in <code>v</code>.
     */
    protected static <T extends Editable> void exchange(ArrayList<T> v, T item) {
        v.get(item.getIndex()).swapContents(item);
    }

    /**
     * Swap the contents of the given items with the contents of the same-index items in the given vector.
     * 
     * @param <T> type of editable to be swapped
     * @param v vector to be edited
     * @param items items to swap with same-index items in <code>v</code>.
     */
    protected static <T extends Editable> void exchange(ArrayList<T> v, T[] items) {
        for (int i = 0; i < items.length; i++) {
            v.get(items[i].getIndex()).swapContents(items[i]);
        }
    }
}
