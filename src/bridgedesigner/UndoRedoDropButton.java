/*
 * UndoRedoDropButton.java  
 *   
 * Copyright (C) 2010 Eugene K. Ressler
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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 * Special button that drops an undo/redo list beneath it if pressed.
 * 
 * @author Eugene K. Ressler
 */
public class UndoRedoDropButton extends JToggleButton {

    private final UndoRedoDropList dropList;

    /**
     * Construct a new undo/redo drop button.
     * 
     * @param undoManager undo manager providing items for drop list affects enabled status of this button
     * @param icon icon to show on this button
     * @param width button width
     * @param height button height
     * @param redo true if this is a redo button; false if undo
     */
    private UndoRedoDropButton(ExtendedUndoManager undoManager, Icon icon, int width, int height, boolean redo) {
        super(icon);
        Dimension size = new Dimension(width, height);
        setMaximumSize(size);
        setPreferredSize(size);
        dropList = new UndoRedoDropList(this, undoManager, redo);
        addItemListener(buttonListener);
    }
    
    /**
     * Return a new undo drop button.
     * 
     * @param undoManager undo manager providing items for drop list affects enabled status of this button
     * @return undo drop button
     */
    public static JToggleButton getUndoDropButton(ExtendedUndoManager undoManager) {
        return new UndoRedoDropButton(undoManager, BDApp.getApplication().getIconResource("drop.png"), 14, 24, false);
    }
    
    /**
     * Return a new redo drop button.
     * 
     * @param undoManager undo manager providing items for drop list affects enabled status of this button
     * @return redo drop button
     */
    public static JToggleButton getRedoDropButton(ExtendedUndoManager undoManager) {
        return new UndoRedoDropButton(undoManager, BDApp.getApplication().getIconResource("drop.png"), 14, 24, true);
    }
    
    private final ItemListener buttonListener  = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                dropList.getSelectionModel().clearSelection();
                final Rectangle bounds = getBounds();
                dropList.show(getParent(), bounds.x, bounds.y + bounds.height);
            }
            else {
                // This is reduntant for all L&F's we know about because click outside menu causes automatic cancel.
                dropList.close();
            }
        }
    };
}
