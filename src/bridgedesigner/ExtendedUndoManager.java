/*
 * ExtendedUndoManager.java  
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
import java.util.Iterator;
import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * Undo manager with extended functionality to support the Bridge Designer.
 * This includes: 
 * <ol>
 * <li>Tracking whether the current undo manager state is consistent with the
 * last time the command target was loaded or stored (making another store superfluous).
 * In the Bridge Designer, this supports various user interface cues that a save is not needed.</li>
 * <li>Providing list models for of undoable and redoable commands</li>
 * </ol>
 * 
 * @author Eugene K. Ressler
 */
public class ExtendedUndoManager extends UndoManager {

    private Object checkpointMark;
    private boolean stored = false;
    private int tailTrimSerial = 0;
    private ArrayList<UndoableEditListener> afterListeners = new ArrayList<UndoableEditListener>();
    private boolean enablePosting = true;

    /**
     * Construct a new extended undo manager.
     */
    public ExtendedUndoManager() {
        setLimit(-1);
        checkpointMark = getMark();
    }

    /**
     * Add a new listener called after an edit is done or undone.
     * 
     * @param listener edit listener to add
     */
    public synchronized void addUndoableAfterEditListener(UndoableEditListener listener) {
        afterListeners.add(listener);
    }

    /**
     * Remove an edit listener.
     * 
     * @param listener edit listener to remove
     */
    public synchronized void removeUndoableAfterEditListener(UndoableEditListener listener) {
        afterListeners.remove(listener);
    }

    /*
     * Instances of TailMark serve as marks when there are no commands in the undo buffer yet.  By storing a serial
     * number for the number of times the tail of the undo buffer has been trimmed, we can tell if a
     * mark for a fresh, empty session is still valid.
     */
    private static class TailMark {
        int trimSerial;
        public TailMark(int trimSerial) {
            this.trimSerial = trimSerial;
        }
    }
    
    /**
     * Return a mark that indicates the current session state.  If undo/redo ever gets the session
     * back to this state, then isAtMark(mark) will return true.
     * 
     * @return a mark for the current session state
     */
    public final Object getMark() {
        Object mark = editToBeUndone();
        if (mark == null) {
            mark = new TailMark(tailTrimSerial);
        }
        return mark;
    }
    
    /**
     * Return an indicator of whether the undomanager state matches the one that existed 
     * when the given mark was returned by <code>getMark()</code>
     * 
     * @param mark state mark to match
     * @return true iff current state matches mark state
     * @see #getMark
     */
    public boolean isAtMark(Object mark) {
        if (mark == null) {
            return false;
        }
        Object current = editToBeUndone();
        if (mark instanceof UndoableEdit) {
            return mark == current;
        }
        // We can assume the mark is either null or a tail mark.
        return current == null && ((TailMark)mark).trimSerial == tailTrimSerial;
    }
    
    /**
     * Update the undomanager to reflect that the target has been saved, so the backing store is
     * now consistent with the undo manager.
     */
    public void save() {
        checkpointMark = getMark();
        stored = true;
        postEdit(null, afterListeners);
    }

    /**
     * Clear and update the undomanager to reflect that the target has been loaded from
     * backing store, so the backing store is now consistent with the undo manager.
     */
    public void load() {
        discardAllEdits();
        checkpointMark = getMark();
        stored = true;
        postEdit(null, afterListeners);
    }
    
    /**
     * Clear all edits.  This has the side effect of causing any exiting marks to be unmatchable.
     */
    public void clear() {
        discardAllEdits();
        postEdit(null, afterListeners);        
    }
    
    /**
     * Clear and update the undomanager to reflect that a new 
     * session has begun with no backing store yet available.
     */
    public void newSession() {
        discardAllEdits();
        // No save file, and if at start of undo buffer, there's nothing to save.
        stored = false;
        checkpointMark = new TailMark(tailTrimSerial);
        postEdit(null, afterListeners);
    }

    /**
     * Return true iff this undo manager has a backing store.
     * 
     * @return true iff this undo manager has a backing store
     */
    public boolean isStored() {
        return stored;
    }
    
    /**
     * Return an indicator of whether editing has occurred since initialization or the last save, load, or newSession.
     * 
     * @return true iff the session is dirty
     */
    public boolean isDirty() {
        return !isAtMark(checkpointMark);
    }
    
    /**
     * Fire an adter edit event to all listeners.
     * 
     * @param edit edit that has been executed
     */
    public synchronized void fireAfter(UndoableEdit edit) {
        postEdit(edit, afterListeners);
    }
    
    /**
     * Redo the current edit to be redone and post an event to listeners.
     * 
     * @throws javax.swing.undo.CannotUndoException
     */
    @Override
    public synchronized void redo() throws CannotRedoException {
        UndoableEdit edit = this.editToBeRedone();
        super.redo();
        postEdit(edit, afterListeners);
    }

    /**
     * Undo the current edit to be undone and post an event to listeners.
     * 
     * @throws javax.swing.undo.CannotUndoException
     */
    @Override
    public synchronized void undo() throws CannotUndoException {
        UndoableEdit edit = this.editToBeUndone();
        super.undo();
        postEdit(edit, afterListeners);
    }
    
    /**
     * Discard all edits. Internally, we assign a new serial number because
     * the first element is being trimmed away, since the trimming means a 
     * null returned for the next undoable command has new meaning.
     */
    @Override
    public synchronized void discardAllEdits() {
        super.discardAllEdits();
        tailTrimSerial++;
    }

    /**
     * Trim the edits list by removing the given range.  Internally, we assign a serial number every time
     * the first element is trimmed away, since the trimming means a null returned for the next undoable 
     * command has a new meaning.
     * 
     * @param from start index of range to trim
     * @param to end index of range to trip
     */
    @Override
    protected void trimEdits(int from, int to) {
        super.trimEdits(from, to);
        if (from == 0 && to >= 0) {
            tailTrimSerial++;
        }
    }
    
    /**
     * Post a given edit to all given listeners unless this is a delayed post. In that case, do nothing.
     * 
     * @param edit edit to post.
     * @param listeners listener list
     */
    protected void postEdit(UndoableEdit edit, ArrayList<UndoableEditListener> listeners) {
        if (enablePosting) {
            UndoableEditEvent ev = new UndoableEditEvent(this, edit);
            Iterator<UndoableEditListener> le = new ArrayList<UndoableEditListener>(listeners).iterator();
            while (le.hasNext()) {
                le.next().undoableEditHappened(ev);	    
            }
        }
    }    
    
    /**
     * This hack is needed because UndoManager hides the index separating undo 
     * from redo lists, <code>indexOfNextAdd</code>.
     */
    private int indexOfEditToBeUndone = -1;
    private void updateIndexOfEditToBeUndone() {
        indexOfEditToBeUndone = edits.indexOf(editToBeUndone());
    }
    
    private final AbstractListModel sharedUndoInstance = new AbstractListModel() {
       public Object getElementAt(int index) {
            return edits.get(indexOfEditToBeUndone - index);
        }
        public int getSize() {
            return indexOfEditToBeUndone + 1;
        }
    };
    
    private final AbstractListModel sharedRedoInstance = new AbstractListModel() {
        public Object getElementAt(int index) {
            return edits.get(indexOfEditToBeUndone + index + 1);
        }
        public int getSize() {
            return edits.size() - indexOfEditToBeUndone - 1;
        }
    };
    
    /**
     * Return a <code>ListModel</code> containing a list of all currently undoable commands
     * in the order they should be undone.  This must be called each time a command is
     * executed in order to update the shared instance.  This should not be necessary,
     * but the UndoManager interface does not expose enough internals to prevent it. It's
     * not much of a problem for popup lists of commands, but it would be for lists
     * that are always visible.
     * 
     * @return list model of undoable commands
     */
    public ListModel getUndoListModel() {
        updateIndexOfEditToBeUndone();
        return sharedUndoInstance;
    }
    
    /**
     * Return a <code>ListModel</code> containing a list of all currently redoable commands
     * in the order they should be redone.  This must be called each time a command is
     * executed in order to update the shared instance.  This should not be necessary,
     * but the UndoManager interface does not expose enough internals to prevent it. It's
     * not much of a problem for popup lists of commands, but it would be for lists
     * that are always visible.
     * 
     * @return list model of redoable commands
     */
    public ListModel getRedoListModel() {
        updateIndexOfEditToBeUndone();
        return sharedRedoInstance;
    }
    
    /**
     * Undo commands through the one given as a parameter.  If no command matches the 
     * parameter, all undoable commands are executed.  As an efficiency measure, we 
     * post only one edit event after all the commands are executed. This prevents a 
     * gajillion UI updates when multiple commands are being processed.
     * 
     * @param edit command to stop undoing at
     */
    @Override
    public void undoTo(UndoableEdit edit) {
        enablePosting = false;
        super.undoTo(edit);
        enablePosting = true;
        postEdit(edit, afterListeners);
    }

    /**
     * Redo commands through the one given as a parameter.  If no command matches the 
     * parameter, all redoable commands are executed. As an efficiency measure, we post 
     * only one edit event after all the commands are executed. This prevents a gajillion 
     * UI updates when multiple commands are being processed.
     * 
     * @param edit command to stop redoing at
     */
    @Override
    public void redoTo(UndoableEdit edit) {
        enablePosting = false;
        super.redoTo(edit);
        enablePosting = true;
        postEdit(edit, afterListeners);
    }
}
