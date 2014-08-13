/*
 * HotDraggableItem.java  
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

import java.awt.Point;

/**
 * Interface for hot draggable items, which are those that are re-displayed in an alternate form when a
 * <code>HotItemListener</code> indicates that the mouse is close enough, and which are also equipped to 
 * be dragged by mouse events.
 * 
 * @author Eugene K. Ressler
 */
public interface HotDraggableItem<TContext> extends HotItem<TContext> {

    /**
     * Start a drag operation.
     * 
     * @param point viewport point where mouse was clicked to start the drag
     * 
     * @return true iff the item is ready to be dragged
     */
    public boolean startDrag(Point point);

    /**
     * Query the item to see if it's ready to move to a new location during a drag operation.
     * 
     * @param point viewport point where mouse was clicked to start the drag
     * 
     * @return true iff the item is ready to be dragged
     */
    public boolean queryDrag(Point point);

    /**
     * Update the location of the item using a new mouse point.
     * 
     * @param point new mouse point
     */
    public void updateDrag(Point point);

    /**
     * Terminate a drag operation at the given point.
     * 
     * @param point drag termination point
     */
    public void stopDrag(Point point);
}
