/*
 * HotItemListener.java  
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

import java.awt.Cursor;
import java.awt.Point;

/**
 * Interface for a listener to say whether a point (normally the current mouse
 * location) is close to a hot item.
 *  
 * @author Eugene K. Ressler
 */
public interface HotItemListener<TContext> {
    /**
     * Query to the listener whether some hot item is close to the point given both in viewport (screen) and
     * world coordinates.
     * 
     * @param ptViewport query point given in viewport coordinates
     * @param ptWorld query point given in world coordinates
     * @return the hot item the point is close to or null if none.
     */
    HotItem<TContext> getItem(Point ptViewport, Affine.Point ptWorld);
    /**
     * Context information that should be used to render the hot item if one is returned by <code>getItem()</code>.
     * @return context object
     */
    TContext getContext();
    /**
     * Return a cursor that should be installed if <code>getItem()</code> returns non-null.
     * This allows all items returned by this listener to have the same rollover effect.
     * 
     * @return cursor to install
     */
    Cursor getCursor();
    /**
     * Whether the tool tip of hot item returned by <code>getItem()</code> should be shown.
     * 
     * @return true iff the tool tipe for the hot item should be shown
     */
    boolean showToolTip();
}
