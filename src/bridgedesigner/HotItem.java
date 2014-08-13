/*
 * HotItem.java  
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

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Interface for hot items, which are those that are re-displayed in an alternate form when a
 * <code>HotItemListener</code> indicates that the mouse is close enough. Note that the
 * string representation of a hot item returned by <code>toString()</code> is used as its
 * tool tip text, optionally shown during a rollover.
 * 
 * @author Eugene K. Ressler
 */
public interface HotItem<PaintContext> {

    /**
     * Paint the hot item with its normal appearance.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform between world and screen coordinates
     * @param context drawing context object
     */
    public void paint(Graphics2D g, ViewportTransform viewportTransform, PaintContext context);

    /**
     * Paint the hot item with its hot or rollover appearance.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform between world and screen coordinates
     * @param context drawing context object
     */
    public void paintHot(Graphics2D g, ViewportTransform viewportTransform, PaintContext context);

    /**
     * Set an extent rectangle with the geometric extent in viewport coordinates of this hot item.
     * This is used to limit backing store operations for efficiency.
     * 
     * @param dst viewport rectangle containing the entire item
     * @param viewportTransform vieport transform provided originally to the hot item manager mouse listener
     * that is handling this hot item
     */
    public void getViewportExtent(Rectangle dst, ViewportTransform viewportTransform);

    /**
     * Return a cursor to install when the a hot item manager mouse listener says this item is hot.
     * Allows a unique rollover cursor for each item.
     * 
     * @return rollover cursor
     */
    public Cursor getCursor();
}
