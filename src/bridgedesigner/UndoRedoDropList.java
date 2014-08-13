/*
 * UndoRedoDropList.java  
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.undo.UndoableEdit;
import org.jdesktop.application.ResourceMap;

/**
 * A dropdown selection list of undoable or redoable commands.  This class is joined at the hip with 
 * UndoRedoDropButton and should probably be nested within it.
 * 
 * @author Eugene K. Ressler
 */
public class UndoRedoDropList extends JPopupMenu {
    /**
     * The drop button that will cause this popup menu to appear.
     */
    private final JToggleButton dropButton;
    /**
     * true iff this is a redo menu vs. an undo.
     */
    private final boolean redo;
    /**
     * The popup menu item in which the list of commands is embedded.
     */
    private final ListMenuItem listMenuItem;
    /**
     * Menu item beneath the list of commands that allows cancellation or provides a helpful message.
     */
    private final JMenuItem cancelMenuItem;
    /**
     * Helpful message text for the menu item.
     */
    private final String undoOrRedo;
    /**
     * Undo manager is source of commands to list.
     */
    private final ExtendedUndoManager undoManager;
    /**
     * Storage for message strings.
     */
    private final ResourceMap resourceMap = BDApp.getResourceMap(UndoRedoDropList.class);
    
    /**
     * Create a new undo or redo drop list.
     * 
     * @param button drop button that this list is attached to
     * @param undoMgr undo manager containing the commands that hsould appear in the drop list
     * @param redoVsUndo
     */
    public UndoRedoDropList(JToggleButton button, ExtendedUndoManager undoMgr, boolean redoVsUndo) {
        super();
        undoManager = undoMgr;
        redo = redoVsUndo;
        dropButton = button;
        listMenuItem = new ListMenuItem();
        add(listMenuItem);
        cancelMenuItem = new JMenuItem();
        add(cancelMenuItem);
        undoOrRedo = resourceMap.getString(redo ? "redo.text" : "undo.text");
        // Close the popup when the user clicks on the cancel menu item.
        cancelMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    /**
     * Close the drop list and deselect the drop button.
     */
    public void close() {
        setVisible(false);
        dropButton.setSelected(false);
    }
    
    /**
     * Special purpose list for commands pulled from the undo manager.
     */
    private class ActionList extends JList {
            
        private int mouseOverIndex = -1;
    
        public ActionList() {
            setPrototypeCellValue("Undo Redo Undo Redo Undo Redo");
            // Install a cell renderer that uses text from presentation name of command.
            setCellRenderer(new DefaultListCellRenderer() {

                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    return super.getListCellRendererComponent(list, 
                            value instanceof UndoableEdit ? ((UndoableEdit)value).getPresentationName() : value, 
                            index, isSelected, cellHasFocus);
                }
            });
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseListener);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        }
    
        /**
         * Provide subdued highlighting rather than selection colors.
         * 
         * @return a very light blue for highlighing
         */
        @Override
        public Color getSelectionBackground() {
            return new Color(224, 232, 248);
        }

        /**
         * Provide a black font to contrast our faded selection background.
         * 
         * @return black
         */
        @Override
        public Color getSelectionForeground() {
            return Color.BLACK;
        }
        
        private MouseAdapter mouseListener = new MouseAdapter() {
            
            private final Point pt = new Point();
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (mouseOverIndex != -1) {
                    getSelectionModel().clearSelection();
                    cancelMenuItem.setText(resourceMap.getString("cancelButton.text"));
                    mouseOverIndex = -1;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                pt.setLocation(e.getX(), e.getY());
                int index = locationToIndex(pt);
                if (!getCellBounds(index, index).contains(pt)) {
                    index = -1;
                }
                if (index != mouseOverIndex) {
                    if (index == -1) {
                        clearSelection();
                        cancelMenuItem.setText(resourceMap.getString("cancelButton.text"));
                    }
                    else {
                        getSelectionModel().setSelectionInterval(0, index);
                        cancelMenuItem.setText(resourceMap.getString("messageButton.text", undoOrRedo, index + 1));
                    }
                    mouseOverIndex = index;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                close();
                int index = getMaxSelectionIndex();
                if (index >= 0) {
                    UndoableEdit edit = (UndoableEdit)getModel().getElementAt(index);
                    if (redo) {
                        undoManager.redoTo(edit);
                    }
                    else {
                        undoManager.undoTo(edit);
                    }
                }
            }
        };                
    }
    
    private class ListMenuItem extends JScrollPane implements MenuElement {

        private final JList actionList;
        /**
         * This is a runnable for delayed popup closing due to cancellation.  Why delayed? 
         * The call to close() deselects the drop button.  We must avoid doing this in 
         * the case where the user is pressing the button. Again, why?  Popup cancelation 
         * occurs upon mouse press outside the popup. The drop button is a toggle button 
         * that changes state on mouse release.  If we allow a cancelation mouse press to 
         * deselect the drop button, the corresonding mouse release will return it to the 
         * selected state, causing the popup to reappear!  Thus we wrap the call in a test 
         * of whether the drop button is in the pressed state. This test is only valid after 
         * all mouse press events have been handled. Hence, the delay.
         */
        private final Runnable closePopup = new Runnable() {
            public void run() {
                if (!dropButton.getModel().isPressed()) {
                    close();
                }
            }
        };
        
        public ListMenuItem() {
            super(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            actionList = new ActionList();
            setViewportView(actionList);
            addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    actionList.setModel(redo ? undoManager.getRedoListModel() : undoManager.getUndoListModel());
                    actionList.setVisibleRowCount(Math.min(actionList.getModel().getSize(), 24));
                    // Invalidate so that geometry is re-computed based on new visible row count.
                    actionList.invalidate();
                    cancelMenuItem.setText(resourceMap.getString("cancelButton.text"));
                }
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
                public void popupMenuCanceled(PopupMenuEvent e) {
                    SwingUtilities.invokeLater(closePopup);
                }
            });
        }
    
        /**
         * Provider for popup interface that does nothing
         * 
         * @param event mouse event
         * @param path menu path
         * @param manager selection manager
         */
        public void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager) { }

        /**
         * Provider for popup interface that does nothing.
         * 
         * @param event mouse event
         * @param path menu path
         * @param manager selection manager
         */
        public void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager) { }

        /**
         * Provider for popup interface that does nothing.
         * 
         * @param isIncluded
         */
        public void menuSelectionChanged(boolean isIncluded) { }

        private final MenuElement [] nada = new MenuElement[0];

        /**
         * Provider for popup interface that does nothing.
         * 
         * @return empty list of elements
         */
        public MenuElement[] getSubElements() {
            return nada;
        }

        /**
         * Provider for popup interface that does nothing.
         * 
         * @return this component as popup component
         */
        public Component getComponent() {
            return this;
        }
    }
}
