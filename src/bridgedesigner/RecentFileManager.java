/*
 * RecentFileManager.java  
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Manage a list of recently used file names attached to a menu.
 * 
 * @author Eugene K. Ressler
 */
public class RecentFileManager implements ActionListener {
    // This should be a vector of files, but XMLEncoder doesn't know how to do the File class.
    @SuppressWarnings("UseOfObsoleteCollectionType")
    private Vector<String> recentPaths;
    private int maxFileCount = 0;
    private static final String recentFileStorageName = "recentFiles.xml";
    private Listener listener;
    
    /**
     * Interface to listeners that want to know when the user requests 
     * opening of a recently used file managed by a <code>RecentFileManager</code>.
     */
    public interface Listener {
        public void openRecentFile(File file);
    }
    
    /**
     * Construct a new recent file manager.
     * 
     * @param maxFileCount maximum number of recently used files to manage
     * @param listener a listener that should be informed of open file requests
     */
    public RecentFileManager(int maxFileCount, Listener listener) {
        this.maxFileCount = maxFileCount;
        this.listener = listener;
        load();
    }

    /**
     * Load the managed file names from local storage.
     */
    @SuppressWarnings({"unchecked", "UseOfObsoleteCollectionType"})  // turn off an unchecked cast error that's really meaningless
    public final void load() {
        final Vector<String> paths = (Vector<String>)BDApp.loadFromLocalStorage(recentFileStorageName);
        if (paths != null) {
            // Remove any unreadable files. Presumably they were moved or deleted since last session.
            Iterator<String> i = paths.iterator();
            while (i.hasNext()) {
                if (!new File(i.next()).canRead()) {
                    i.remove();
                }
            }
        }
        recentPaths = (paths == null) ? new Vector<String>(maxFileCount) : paths;
    }
    
    /**
     * Save the current recent file list as XML in the user's local storage.
     */
    public void save() {
        BDApp.saveToLocalStorage(recentPaths, recentFileStorageName);
    }

    /**
     * Add a new file to this recently used manager.  Shifts all others down and may result 
     * in the last file being removed from the list.  File must be readable to be added.
     * 
     * @param file file to add.
     */
    public void addNew(File file) {
        if (file != null && file.canRead()) {
            String path = file.getAbsolutePath();
            recentPaths.remove(path);
            recentPaths.add(0, path);
            while (recentPaths.size() > maxFileCount) {
                recentPaths.remove(recentPaths.size() - 1);
            }
        }
    }   
    
    /**
     * Menu item that holds an extra field for the index into the recentPaths array
     * that this item represents.  Subclass also provides a way to identify the item
     * for removal.
     */
    private class MenuItem extends JMenuItem { 
        
        final int index;
        /** 
         * Construct a new menu item with mneumonic of the file number
         * @param s file name
         * @param index index into recentPaths array this menu item represents
         */
        public MenuItem(String s, int index) {
            super((index + 1) + " - " + s, index + '1');
            this.index = index;
        }
    }
    
    /**
     * Subclass of popup separator that allows removal method to identify it.
     */
    private class Separator extends JPopupMenu.Separator { }

    /**
     * Return a simplified version of a file path relative to a given
     * base path.  At present, just removes the base path from file path
     * if the base matches a file path prefix.
     * 
     * @param filePath path to file
     * @param basePath base path to a current directory or similar (null causes filePath to be returned)
     * @return simplified path
     */
    public String getSimplifiedPath(String filePath, String basePath) {
        if (basePath == null || basePath.length() >= filePath.length()) {
            return filePath;
        }
        String filePathCaseAdjusted = filePath;
        String basePathCaseAdjusted = basePath;
        if (System.getProperty ("os.name").contains("Windows")) {
            filePathCaseAdjusted = filePath.toLowerCase();
            basePathCaseAdjusted = basePath.toLowerCase();
        }
        // below assumes separator is 1 char long, but Java does, too.
        return filePathCaseAdjusted.startsWith(basePathCaseAdjusted) ? 
            filePath.substring(basePath.length() + 1) : filePath;
    }
    
    /**
     * Removes any previously installed recent file menu items and separators from given menu.
     * 
     * @param menu menu to remove items from
     */
    public void removeRecentFileMenuItems(JMenu menu) {
        Component [] items = menu.getPopupMenu().getComponents();
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof MenuItem || items[i] instanceof Separator) {
                menu.remove(items[i]);
            }
        }
    }
    
    /**
     * Remove all previously installed special items and separators from the menu and 
     * then add the recent files at a given location, preceeded by a separator if the 
     * given location index is greater than zero.  Special menu items and separators 
     * are used so they can be identified for removal later.
     * 
     * @param menu menu to add the recent file items to
     * @param atIndex location to place new items (and possibly a separator)
     * @param base source of path to simplify full paths shown in menu items.  Null causes no simplification
     */
    public void addRecentFileMenuItemsAt(JMenu menu, int atIndex, File base) {
        removeRecentFileMenuItems(menu);
        if (recentPaths.size() > 0) {
            JPopupMenu popup = menu.getPopupMenu();
            if (atIndex > 0) {
                popup.insert(new Separator(), atIndex++);
            }
            Iterator<String> e = recentPaths.iterator();
            int i = 0;
            while (e.hasNext()) {
                MenuItem menuItem = new MenuItem(getSimplifiedPath(e.next(), base.getAbsolutePath()), i++);
                menuItem.addActionListener(this);
                popup.insert(menuItem, atIndex++);
            }
        }
    }

    /**
     * Find a separator in the given menu using its count (1=first, etc.).  Then 
     * remove an pre-existing recent file menu items and insert new ones at that 
     * location, including a separator if it's not the first in the menu. 
     * 
     * @param menu menu to insert recent file items in
     * @param atSepCount count of separator to look for (1=first, etc.)
     * @param base source of path to simplify full paths shown in menu items.  Null causes no simplification.
     */
    public void addRecentFileMenuItemsAtSep(JMenu menu, int atSepCount, File base) {
        int sepCount = 0;
        JPopupMenu popup = menu.getPopupMenu();
        int itemCount = popup.getComponentCount();
        for (int index = 0; index < itemCount; index++) {
            Component component = popup.getComponent(index);
            if (component instanceof JSeparator) {
                ++sepCount;
            }
            if (sepCount == atSepCount) {
                addRecentFileMenuItemsAt(menu, index, base);
                return;
            }
        }
        // Should never happen, but it's best to clean the menu now that something has gone wrong.
        removeRecentFileMenuItems(menu);
    }

    /**
     * Called when a recent file menu item is selected.  Moves the selected item to the most recently
     * used position and calls the listener to handle the file.
     * 
     * @param e event produced by selection of menu item
     */
    public void actionPerformed(ActionEvent e) {
        MenuItem menuItem = (MenuItem)e.getSource();
        String path = recentPaths.get(menuItem.index);
        recentPaths.remove(menuItem.index);
        recentPaths.add(0, path);
        listener.openRecentFile(new File(path));
    }
}
