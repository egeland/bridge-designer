/*
 * HotMouseListener.java  
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;
import bridgedesigner.Affine.Point;

/**
 * Manages hot areas on a JComponent.  A hot area is an arbitrary program-defined patch of the component
 * surface that reacts by redrawing itself specially in response to mouse movement.  A rollover effect is 
 * an example of a hot area.  Hot areas are defined by HotItemListeners attached to the manager.  When a mouse 
 * movement is passed to a manager via handleMouseMoved, it polls all the listeners' getItem methods, 
 * providing the mouse location in world coordinates. The first non-null response is processed as the hot 
 * item. If it's the same as the current hot item, nothing happens. If there has been a change, the old
 * hot item, if it was non-null, is erased by repainting and the new one, if it's non-null, is drawn.
 * Tooltips and cursor may also be adjusted.
 * 
 * There is also a mechanism for dragging hot items. If drag events are passed to handleMouseDragged, the
 * manager goes into a special "dragging" state (where, incidentally, mouseMoved events are ignored). 
 * The single item callback drag() is required to provide all the dragging functionality for the hot item.
 * There are four kinds of calls to drag(): Start, Query, Update, and Stop.  Start and Query are 
 * identical requests for information about whether the hot item is ready to change locations based 
 * on the current mouse position (i.e. to be erased and redrawn).  If the hot item is ready to move,
 * it should return true for both Start and Query calls to drag().  The only difference between the two
 * is that Start is the initial call, which enables the dragged item to do any drag initiation chores.  
 * For example, if the dragged item is in the backing store, it repaints the backing store with itself
 * omitted. All further requests for readiness to move are Query calls.
 * 
 * After every Start/Query call returning true, the manager erases the hot object (by restoring its
 * extent rectangle from the backing store).  This is followed immediately by a drag(Update) call,
 * which is an invitation for the the hot item to update its internal location state to effect a move.
 * It is safe to cache a new location computed during the Start/Query and then merely copy to
 * the true state upon receiving the Update. Alternately, the new location can be computed independently
 * for each call.  The manager follows each drag(Update) call with paintHot() to paint the object in
 * its new location.  PaintHot is used because the item was hot when the drag started.
 * 
 * Any mouse listener feeding mouseDragged events to the manager must also feed mouseReleased in order
 * to terminate the drag state.  The release causes a final call to drag() with a Stop flag to allow
 * finalization of the drag, for example, but repainting the dragged item in the backing store.
 * 
 * Items that don't want to be dragged should just return false from drag().  The corresponding
 * mouse listener need not send drag or release events to the manager.
 * 
 * This class inevitably relies on the backing store mechanism established in <code>DraftingPanel</code>.
 * 
 * @author Eugene K. Ressler
 */
public class HotMouseListener<TContext> extends MouseInputAdapter {

    private final JComponent component;
    private final ArrayList<HotItemListener<TContext>> listeners = new ArrayList<HotItemListener<TContext>>();
    private final ViewportTransform viewportTransform;
    private final Cursor defaultCursor;
    private final Affine.Point ptWorld = new Affine.Point();
    private HotItem<TContext> hot = null;
    private HotItemListener<TContext> hotListener = null;
    private boolean dragging = false;
    private final Rectangle extentViewport = new Rectangle();

    /**
     * Construct a new hot item manager, which is a mouse listener.  
     * 
     * @param component component where the managed hot items are to be drawn
     * @param viewportTransform viewport transform from world to viewport (screen) coordinates.
     * @param defaultCursor cursor to install after a hot item rollover is complete
     */
    public HotMouseListener(JComponent component, ViewportTransform viewportTransform, Cursor defaultCursor) {
        this.component = component;
        this.viewportTransform = viewportTransform;
        this.defaultCursor = defaultCursor;
    }

    /**
     * Return the current hot item or null if there is none.
     * 
     * @return hot item or null if none
     */
    public HotItem<TContext> getHot() {
        return hot;
    }

    /**
     * Return world coordinate version of the last mouse event received.
     * 
     * @return world coordinate point
     */
    public Point getPtWorld() {
        return ptWorld;
    }

    /**
     * Return true iff a drag operation is in progress. This means drag events are being forwarded,
     * the mouse button has been pressed near a hot item and not yet released.
     * 
     * @return true iff a drag operation is in progress
     */
    public boolean isDragging() {
        return dragging;
    }

    /**
     * Get the default cursor installed after roll-overs are complete.
     * 
     * @return default roll-over cursor
     */
    public Cursor getDefaultCursor() {
        return defaultCursor;
    }

    /**
     * Forcefully clear the current hot item.
     */
    public void clear() {
        hot = null;
        component.setToolTipText(null);
        component.setCursor(defaultCursor);
    }

    /**
     * Add a listener to this manager.
     * 
     * @param listener listener to add
     */
    public void addListener(HotItemListener<TContext> listener) {
        listeners.add(listener);
    }

    /**
     * Remove the given listener from this manager.
     * 
     * @param listener listener to remove
     */
    public void removeListener(HotItemListener<TContext> listener) {
        listeners.remove(listener);
    }

    /**
     * Remove all the listeners to this manager.
     */
    public void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Handle a mouse drag event by erasing and redrawing the selected draggable item, if there is one.  THis
     * may entail starting the drag if this is the first event and a draggable item is selected.
     * 
     * @param event drag event
     */
    @Override
    public void mouseDragged(MouseEvent event) {
        if (hot != null && hot instanceof HotDraggableItem) {
            HotDraggableItem<TContext> draggable = (HotDraggableItem<TContext>) hot;
            if (!dragging ? draggable.startDrag(event.getPoint()) : draggable.queryDrag(event.getPoint())) {
                hot.getViewportExtent(extentViewport, viewportTransform);
                // Temporarily remove the hot item so painter can't see it.
                HotItem<TContext> savedHot = hot;
                hot = null;
                component.paintImmediately(extentViewport);
                hot = savedHot;
                // Update the hot item's state and paint in new configuration.
                draggable.updateDrag(event.getPoint());
                Graphics2D g = (Graphics2D) component.getGraphics();
                hot.paint(g, viewportTransform, hotListener.getContext());
                g.dispose();
                dragging = true;
            }
        }
    }

    /**
     * Handle a mouse release event by stopping the drag if one was in progress.
     * 
     * @param event mouse release event
     */
    @Override
    public void mouseReleased(MouseEvent event) {
        if (dragging && hot instanceof HotDraggableItem) {
            ((HotDraggableItem<TContext>)hot).stopDrag(event.getPoint());
            dragging = false;
            mouseMoved(event);
        }
    }

    /**
     * Handle a mouse move event to see if any of our listeners is interested
     * in changing the hot element by returning a new Item.  If so, erase and/or paint, then set or unset the cursor. 
     * If the listener has a non-null cursor, then it is used; otherwise, the item cursor is used.
     * 
     * @param event mouse move event to be processed
     */
    @Override
    public void mouseMoved(MouseEvent event) {
        // If we are dragging, don't change the hot item.
        if (dragging) {
            return;
        }
        viewportTransform.viewportToWorld(ptWorld, event.getPoint());
        Iterator<HotItemListener<TContext>> e = listeners.iterator();
        HotItem<TContext> item = null;
        HotItemListener<TContext> listener = null;
        // Find the first listener that returns a hot item.  Remember both.
        while (item == null && e.hasNext()) {
            listener = e.next();
            item = listener.getItem(event.getPoint(), ptWorld);
        }
        if (item != hot) {
            // Hot item has changed.
            Graphics2D g = (Graphics2D) component.getGraphics();
            // Redraw the old hot item from backing store (non-hot appearance) if there was one.
            if (hot != null) {
                hot.getViewportExtent(extentViewport, viewportTransform);
                // Remove the hot item so painter can't see it.
                hot = null;
                component.paintImmediately(extentViewport);
            }
            // Update.
            hot = item;
            if (hot == null) {
                // Nothing is hot now.  Reset tooltip and cursor.
                hotListener = null;
                if (defaultCursor != null) {
                    component.setCursor(defaultCursor);
                }
                component.setToolTipText(null);
            } else {
                // Something is hot.  Repaint and adjust tooltip and cursor.
                hotListener = listener;
                hot.paintHot(g, viewportTransform, listener.getContext());
                // Either listener or item can provide cursor.  
                // Single item gets preference so it can override listener.
                if (hot.getCursor() != null) {
                    component.setCursor(hot.getCursor());
                }
                else if (listener.getCursor() != null) {
                    component.setCursor(listener.getCursor());
                } 
                if (listener.showToolTip()) {
                    component.setToolTipText(hot.toString());
                }
            }
            g.dispose();
        }
    }
}
