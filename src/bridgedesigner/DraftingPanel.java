/*
 * DraftingPanel.java  
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.jdesktop.application.ResourceMap;

/**
 * Drafting panel for the bridge designer.  Manages most of the user interaction needed to design a bridge.
 * 
 * @author Eugene K. Ressler
 */
public class DraftingPanel extends JPanel implements RulerHost {

    /**
     * The bridge we are drafting.
     */
    private final EditableBridgeModel bridge;
    
    /**
     * View of the bridge used for drawing.
     */
    private final BridgeDraftingView bridgeView;
    /**
     * The provider of popup menus and perhaps other GUI context components.
     */
    ContextComponentProvider contextComponentProvider;
    /**
     * The drafting coordinate system, which includes snap-to-grid functionality for joint locations.
     */
    private final DraftingCoordinates draftingCoordinates;
    /**
     * Custom transformations between world coordinates in meters and viewport coordinates in pixels,
     * where the y-axis origin may be at upper left (with negative viewport height).
     */
    private final ViewportTransform viewportTransform;
    /**
     * Controls that are effectively part of the drafting panel, though located elsewhere.  These
     * provide the current member stock descriptions as a set of three integer indices, which are
     * defined by the Inventory of construction stock.
     */
    StockSelector stockSelector;
    /**
     * Mouse input listeners for each of the modes of the drafting panel.  Allocated once but used repeatedly.
     */
    private final EditJointsListener editJointsListener;
    private final EditMembersListener editMembersListener;
    private final EditSelectListener editSelectListener;
    private final EditEraseListener editEraseListener;
    /**
     * Symmetry guides controlled by the user.
     */
    private final HorizontalGuide horizontalGuide;
    private final VerticalGuide verticalGuide;
    /**
     * Hot listeners for the knobs that move the guides, which are used by multiple mouse listeners.
     */
    private final HotHorzontalGuideKnobListener hotHorzontalGuideKnobListener;
    private final HotVerticalGuideKnobListener hotVerticalGuideKnobListener;
    /**
     * Re-positionable labels on structural elements.
     */
    private final Labels labels;
    /**
     * Hot listener for the labels.
     */
    private final HotLabelsListener hotLabelsListener;
    /**
     * The mouse listener currently in use.
     */
    private HotMouseListener<BridgePaintContext> editListener;
    /**
     * Crosshairs used by various edit listeners.
     */
    private final Crosshairs crosshairs;
    /**
     * Backing store that buffers a complete image of the drafting panel screen.  Used to
     * quickly erase graphical cursors.  This effectively makes the drating panel triple-buffered,
     * since the normal double-buffering mechanism provided by swing is still used to prevent the
     * user from seeing the painting of cursors.
     */
    private Image backingStore;

    /**
     * A panel with interactive drawing tools for users to create bridges.
     * 
     * @param bridge
     */
    public DraftingPanel(EditableBridgeModel bridge, BridgeDraftingView bridgeView, ContextComponentProvider contextComponentProvider) {
        this.bridge = bridge;
        this.bridgeView = bridgeView;
        this.contextComponentProvider = contextComponentProvider;
        // Drafting coordinates and viewport must be initialized before the before guides and listeners.
        this.draftingCoordinates = new DraftingCoordinates(bridgeView);
        this.viewportTransform = new ViewportTransform();
        this.horizontalGuide = new HorizontalGuide();
        this.verticalGuide = new VerticalGuide();
        this.hotHorzontalGuideKnobListener = new HotHorzontalGuideKnobListener();
        this.hotVerticalGuideKnobListener = new HotVerticalGuideKnobListener();
        this.labels = new Labels();
        this.hotLabelsListener = new HotLabelsListener();
        this.editJointsListener = new EditJointsListener();
        this.editMembersListener = new EditMembersListener();
        this.editSelectListener = new EditSelectListener();
        this.editEraseListener = new EditEraseListener();
        this.crosshairs = new Crosshairs();
        addComponentListener(new DraftingPanelListener());
        Member.initializeDrawing(bridge.getInventory());
        // If this is an empty bridge, assume the user will want to create joints first, else select.
        if (bridge.getJoints().isEmpty()) {
            editJoints();
        } else {
            editSelect();
        }
        loadKeyBindings();
    }

    /**
     * Initialize the drafting panel.
     */
    public void initialize() {
        setViewport(true);
    }
    
    private class ArrowKeyAction extends AbstractAction {
        
        private int dx, dy;
        private final Affine.Point newLocation = new Affine.Point();
                
        public ArrowKeyAction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        /**
         * Handle an arrow key press in the drating window.  Note this works together with the
         * redispatcher in WPBDView.  If a joint is selected and an Arrow key is pressed _anywhere_
         * in the WPBD application, that keystroke is sent here.  (Typing Escape deselects the joint.)
         * Note that every mouse press handler erases the crosshairs, which is also part of the
         * cooperative management of the arrow key vernier movement.
         * 
         * @param e event resulting from arrow keystroke
         */
        public void actionPerformed(ActionEvent e) {
            Joint selectedJoint = bridge.getSelectedJoint();
            if (selectedJoint != null) {
                // Set finest grid temporarily.
                int savedSnapMultiple = draftingCoordinates.getSnapMultiple();
                draftingCoordinates.setSnapMultiple(1);
                draftingCoordinates.getNearbyPointOnGrid(newLocation, selectedJoint.getPointWorld(), dx, dy);
                if (bridge.moveJoint(selectedJoint, newLocation) == EditableBridgeModel.ADD_MEMBER_AT_MAX) {
                    ResourceMap resourceMap = BDApp.getResourceMap(DraftingPanel.class);
                    JOptionPane.showMessageDialog(BDApp.getFrame(),
                        resourceMap.getString("atMaxMembers.text", DesignConditions.maxMemberCount),
                        resourceMap.getString("atMaxMembersTitle.text"),
                        JOptionPane.WARNING_MESSAGE);                    
                }
                crosshairs.paintAt(selectedJoint.getPointWorld());
                draftingCoordinates.setSnapMultiple(savedSnapMultiple);
            }
        }
    }

    /**
     * Erase the crosshairs if any are showing.  Needed so that undo of joint moves in cursor mode
     * don't leave crosshairs behind as undone moves occur.
     */
    public void eraseCrosshairs() {
        crosshairs.erase();
    }
    
    private void loadKeyBindings() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        ActionMap actionMap = getActionMap();
        actionMap.put("left",  new ArrowKeyAction(-1, 0));
        actionMap.put("right", new ArrowKeyAction(1, 0));
        actionMap.put("up",    new ArrowKeyAction(0, 1));
        actionMap.put("down",  new ArrowKeyAction(0, -1));
    }

    /**
     * Set the viewport transformation to include the preferred drawing window for the bridge view.
     * After the transformation change, we make sure the backing store size matches the viewport size
     * and redraw everything.  Called on resize events, during initialization, etc.
     * 
     * @param reset whether to reset the symmetry guides or let them in their current positions
     */
    public void setViewport(boolean reset) {
        viewportTransform.setWindow(bridgeView.getPreferredDrawingWindow());
        int w = getWidth();
        int h = getHeight();
        viewportTransform.setViewport(0, h - 1, w - 1, 1 - h);
        if (backingStore == null) {
            Dimension screenSize = Utility.getMaxScreenSize();
            backingStore = createImage(screenSize.width, screenSize.height);
        }
        horizontalGuide.initialize(reset);
        verticalGuide.initialize(reset);
        labels.initialize();
        paintBackingStore();
        repaint();
    }

    /**
     * Listener for the drafting panel handles Resize events to keep backing store at same size
     * as panel window.
     */
    private class DraftingPanelListener extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {
            setViewport(false);
        }
    }

    /**
     * Implementation of hot item interface.  Return this drafting panel.
     * 
     * @return this drafting panel
     */
    public JComponent getComponent() {
        return this;
    }

    /**
     * A Stock Selector is a container for three controls used to represent member stock.
     * We keep a reference locally because it's needed frequently.
     * 
     * @param stockSelector
     */
    public void setMemberStockSelector(StockSelector stockSelector) {
        this.stockSelector = stockSelector;
    }

    /**
     * Show or hide labels on bridge members.
     * 
     * @param label
     */
    public void setLabel(boolean label) {
        bridgeView.setLabel(label);
        paintBackingStore();
        repaint();
    }

    /**
     * Provide a coordinate where drawing tools will appear in a useful location.  Currently about halfway
     * down the left edge of the frame.  We don't use the upper left corner because the Java lightweight component
     * menus appear behind the tool dialog.  It's a heavyweight, Always On Top component so that it can be moved
     * anywhere on the screen.
     * 
     * @return window location for the drawing tools
     */
    public Point getReasonableDrawingToolsLocation() {
        Point pt = this.getLocationOnScreen();
        //pt.y += this.getHeight() / 2;
        return pt;
    }

    /**
     * Return the drafting coordinates of the drafting panel.
     * 
     * @return drafting coordinates
     */
    public DraftingCoordinates getDraftingCoordinates() {
        return draftingCoordinates;
    }

    /**
     * Return the viewport transform of the drafting panel
     * 
     * @return vieport transform between world and screen coordinates
     */
    public ViewportTransform getViewportTransform() {
        return viewportTransform;
    }

    private final BridgePaintContext ctx = new BridgePaintContext();
    
    /**
     * Paint the drafting panel. Blits the backing store onto the screen and 
     * then paints the hot item and crosshairs, if any.  By overriding this
     * we defeat attempts of Swing to clear the panel for us.
     * 
     * @param g0 java graphics context
     */
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        restoreFromBackingStore(g, g.getClipBounds());
        if (editListener.getHot() != null) {
            ctx.label = bridgeView.isLabel();
            ctx.allowableSlenderness = bridge.getDesignConditions().getAllowableSlenderness();
            HotItem<BridgePaintContext> hot = editListener.getHot();
            hot.paintHot(g, viewportTransform, ctx);
        }
        crosshairs.paint(g);
    }

    /**
     * Paint drafting panel graphics into the backing store: bridge view, guides, then labels.
     */
    public void paintBackingStore() {
        if (backingStore == null) {
            return;
        }
        Graphics2D g = (Graphics2D) backingStore.getGraphics();
        bridgeView.paint(g, viewportTransform);
        if (horizontalGuide.isVisible()) {
            horizontalGuide.paint(g, viewportTransform, null);
        }
        if (verticalGuide.isVisible()) {
            verticalGuide.paint(g, viewportTransform, null);
        }
        if (labels.isVisible()) {
            labels.paint(g, viewportTransform, null);
        }
        g.dispose();
    }

    private void restoreFromBackingStore(Graphics2D g, Rectangle b) {
        if (b == null) {
            g.drawImage(backingStore, 0, 0, null);
            //g.drawImage(backingStore, 0, 0, getWidth(), getHeight(), this);
        } else {
            int x1 = b.x;
            int y1 = b.y;
            int x2 = b.x + b.width;
            int y2 = b.y + b.height;
            g.drawImage(backingStore,
                    x1, y1, x2, y2,
                    x1, y1, x2, y2, null);
        }
    }

    private void setMouseListener(HotMouseListener<BridgePaintContext> listener) {
        if (editListener != null) {
            removeMouseListener(editListener);
            removeMouseMotionListener(editListener);
        }
        addMouseListener(listener);
        addMouseMotionListener(listener);
        setCursor(listener.getDefaultCursor());
        editListener = listener;
    }

    private static Cursor getCustomCursor(String fileName, int hotSpotX, int hotSpotY, String tip) {
        return Toolkit.getDefaultToolkit().createCustomCursor(
                BDApp.getApplication().getImageResource(fileName),
                new Point(hotSpotX, hotSpotY), tip);
    }

    /**
     * Put the drafting panel in edit joints mode.
     */
    public final void editJoints() {
        bridge.select(null, false);
        crosshairs.erase();
        setMouseListener(editJointsListener);
    }

    /**
     * Put the drafting panel in edit members mode.
     */
    public void editMembers() {
        bridge.select(null, false);
        crosshairs.erase();
        setMouseListener(editMembersListener);
    }

    /**
     * Put the drafting panel in edit select mode.
     */
    public final void editSelect() {
        setMouseListener(editSelectListener);
    }

    /**
     * Put the drafting panel in edit erase mode.
     */
    public void editErase() {
        bridge.select(null, false);
        crosshairs.erase();
        setMouseListener(editEraseListener);
    }

    /**
     * Base class for all hot listeners for bridge elements: joints and members.  Takes
     * care of setting context as current label status and saying that tooltips and
     * painting should be done.
     */
    private abstract class HotBridgeElementListener implements HotItemListener<BridgePaintContext> {

        public BridgePaintContext getContext() {
            return new BridgePaintContext(bridge.getDesignConditions().getAllowableSlenderness(), bridgeView.isLabel());
        }

        public boolean showToolTip() {
            return true;
        }
    }

    /**
     * Base class for hot listeners that find the closest joint or member.
     */
    private abstract class HotJointOrMemberListener extends HotBridgeElementListener {

        private final int pointHotRadius;

        public HotJointOrMemberListener(int pointHotRadius) {
            this.pointHotRadius = pointHotRadius;
        }

        public HotItem<BridgePaintContext> getItem(Point ptViewport, Affine.Point ptWorld) {
            double jointRadius = viewportTransform.viewportToWorldDistance(pointHotRadius);
            Joint joint = bridge.findJoint(ptWorld, jointRadius, true);
            Member member = bridge.getMember(ptWorld, viewportTransform);
            if (member == null) {
                return joint;
            }
            if (joint == null) {
                return member;
            }
            double jointDist = joint.getPointWorld().distance(ptWorld);
            double memberDist = member.pickDistanceTo(ptWorld, jointRadius);
            return (jointDist <= memberDist) ? joint : member;
        }
    }

    private abstract class GuideKnobListener implements HotItemListener<BridgePaintContext> {

        public BridgePaintContext getContext() {
            return null;
        }

        public Cursor getCursor() {
            return null;
        }

        public boolean showToolTip() {
            return true;
        }
    }

    private class HotHorzontalGuideKnobListener extends GuideKnobListener {

        private Rectangle extent = new Rectangle();

        public HotItem<BridgePaintContext> getItem(Point ptViewport, Affine.Point ptWorld) {
            horizontalGuide.getKnobExtent(extent);
            return horizontalGuide.isVisible() && extent.contains(ptViewport) ? horizontalGuide : null;
        }        
    }

    private class HotLabelsListener implements HotItemListener<BridgePaintContext> {

        public HotItem<BridgePaintContext> getItem(Point ptViewport, Affine.Point ptWorld) {
            return labels.isVisible() && labels.getTextExtent().contains(ptViewport) ? labels : null;
        }

        public BridgePaintContext getContext() {
            return null;
        }

        public Cursor getCursor() {
            return null;
        }

        public boolean showToolTip() {
            return true;
        }
    }

    private class HotVerticalGuideKnobListener extends GuideKnobListener {

        private Rectangle extent = new Rectangle();

        public HotItem<BridgePaintContext> getItem(Point ptViewport, Affine.Point ptWorld) {
            verticalGuide.getKnobExtent(extent);
            return verticalGuide.isVisible() && extent.contains(ptViewport) ? verticalGuide : null;
        }

    }
    
    private static final int erasureMargin = 20;

    /**
     * Draggable labels for deck features in the drafting panel
     */
    public class Labels implements HotDraggableItem<BridgePaintContext> {

        private boolean visible = true;
        private String floorBeamString;
        private String deckString;
        private String roadString;
        private int yStartDrag;
        private int orgYLabels;
        private int newYLabels;
        private int yLabels;
        private int minYLabels;
        private int maxYLabels;
        private final Rectangle extent = new Rectangle();
        private final Rectangle textExtent = new Rectangle();
        private final Cursor cursor = getCustomCursor("verticalsize.png", 15, 15, "Move Horizontal Guide");
        private static final int xLabelGap = 16;
        private static final int xLabelOffset = 32;
        private static final int xLabelLeading = 16;
        private final String tip = BDApp.getResourceMap(DraftingPanel.class).getString("labelsTip.text");

        /**
         * Construct a new set of labels.
         */
        public Labels() {
            ResourceMap resourceMap = BDApp.getResourceMap(DraftingPanel.class);
            floorBeamString = resourceMap.getString("floorBeam.Label.text");
            deckString = resourceMap.getString("deck.Label.text");
            roadString = resourceMap.getString("road.Label.text");            
        }
        
        /**
         * Initialize the labels by setting their y-coordinate to the one 
         * specified in the bridge attached to the drafting panel.
         */
        public void initialize() {
            yLabels = viewportTransform.worldToViewportY(bridge.getLabelPosition());
        }
        
        /**
         * Return true iff the labels are visible.
         * 
         * @return true iff the labels are visible
         */
        public boolean isVisible() {
            return visible;
        }

        /**
         * Set the visibility of these labels.
         * 
         * @param visible true iff the labels are visible
         */
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /**
         * Return a rectangle containing the labels text.
         * 
         * @return text extent rectangle
         */
        public Rectangle getTextExtent() {
            return textExtent;
        }

        /**
         * Implementation of the start drag functionality of the <code>HotDraggableItems()</code> interface.
         * @param ptViewport point where the drag is to start in viewport screen coordinates
         * @return always true signifying labels are ready to drag
         */
        public boolean startDrag(Point ptViewport) {
            orgYLabels = newYLabels = yLabels;
            yStartDrag = ptViewport.y;
            DesignConditions conditions = bridge.getDesignConditions();
            minYLabels = viewportTransform.worldToViewportY(conditions.getOverMargin());
            maxYLabels = viewportTransform.worldToViewportY(-conditions.getUnderClearance());
            setVisible(false);
            paintBackingStore();
            return true;
        }

        /**
         * Implementation of the query drag functionality of the <code>HotDraggableItems()</code> interface.
         * We'll calculate the proposed new location of the labels and reply yes only if they need to move.
         * 
         * @param ptViewport point where the drag is to be next in viewport screen coordinates
         * @return true if the labels need to move
         */
        public boolean queryDrag(Point ptViewport) {
            newYLabels = Math.min(maxYLabels, Math.max(minYLabels, ptViewport.y - yStartDrag + orgYLabels));
            return (newYLabels != yLabels);
        }

        /**
         * Implementation of the update drag functionality of the <code>HotDraggableItems()</code> interface.
         * 
         * @param ptViewport point where the drag is to be next in viewport screen coordinates
         */
        public void updateDrag(Point ptViewport) {
            yLabels = newYLabels;
        }

        /**
         * Implementation of the stop drag functionality of the <code>HotDraggableItems()</code> interface.
         * 
         * @param ptViewport point where the drag is to stop in viewport screen coordinates
         */
        public void stopDrag(Point ptViewport) {
            setVisible(true);
            new MovelLabelsCommand(bridge, this, viewportTransform.viewportToWorldY(yLabels)).execute(bridge.getUndoManager());
        }
        private final Rectangle labelExtent = new Rectangle();

        /**
         * Paint just one of the labels.
         * 
         * @param g java graphics context
         * @param s label text
         * @param xAnchor viewport x-coordinate of the anchor point, where the arrow point
         * @param yAnchor viewport y-coordinate of the anchor point, where the arrow point
         * @param yLabels viewport y-coordinate of the label text
         */
        private void paintLabel(Graphics2D g, String s, int xAnchor, int yAnchor, int yLabels) {
            Utility.drawArrow(g, xAnchor + xLabelOffset, yLabels, xAnchor, yAnchor);
            int xText = xLabelOffset + xLabelLeading + xAnchor;
            g.drawLine(xAnchor + xLabelOffset, yLabels, xText, yLabels);
            Labeler.drawJustified(g, s, xText, yLabels, Labeler.JUSTIFY_LEFT, Labeler.JUSTIFY_CENTER, null, labelExtent);
            extent.add(xAnchor - Utility.arrowHalfHeadWidth, yAnchor);
            extent.add(labelExtent);
            textExtent.add(labelExtent);
        }

        private void paint(Graphics2D g) {
            extent.setBounds(0, 0, -1, -1);
            textExtent.setBounds(0, 0, -1, -1);
            paintLabel(g, floorBeamString, bridgeView.getXBeamFlangeAnchor(), bridgeView.getYBeamFlangeAnchor(), yLabels);
            paintLabel(g, deckString, labelExtent.x + labelExtent.width + xLabelGap, bridgeView.getYDeckAnchor(), yLabels);
            paintLabel(g, roadString, labelExtent.x + labelExtent.width + xLabelGap, bridgeView.getYRoadAnchor(), yLabels);
        }
        
        /**
         * Paint the labels with their normal appearance.
         * 
         * @param g java graphics context
         * @param viewportTransform viewport transform between world and screen coordinates
         * @param context painting context (unused)
         */
        public void paint(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext context) {
            g.setColor(Color.BLACK);
            paint(g);
        }

        /**
         * Paint the labels with their hot appearance.
         * 
         * @param g java graphics context
         * @param viewportTransform viewport transform between world and screen coordinates
         * @param context painting context (unused)
         */
        public void paintHot(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext context) {
            g.setColor(Color.GRAY);
            paint(g);
        }

        /**
         * Implementation of the viewport extent part of
         * the drag functionality of the <code>HotDraggableItems()</code> interface.
         * 
         * @param dst extent of rectangle that must be redrawn during dragging
         * @param viewportTransform viewport transform between world and screen coordinates (unused)
         */
        public void getViewportExtent(Rectangle dst, ViewportTransform viewportTransform) {
            dst.setBounds(extent);
        }

        /**
         * Implementation of the rollover cursor functionality of the <code>HotDraggableItems()</code> interface.
         * Our cursor is a vertical double arrow signifying that dragging is possible.
         */
        public Cursor getCursor() {
            return cursor;
        }

        /**
         * Return the tip text as the string representation of the labels.
         * 
         * @return tip text
         */
        @Override
        public String toString() {
            return tip;
        }
    }
    
    private class HorizontalGuide implements HotDraggableItem<BridgePaintContext> {

        private boolean visible = false;
        private int yMinGrid;
        private int yMaxGrid;
        private int yGrid;
        private final Cursor cursor = getCustomCursor("verticalsize.png", 15, 15, "Move Horizontal Guide");
        private final Image horizontalKnob = BDApp.getApplication().getImageResource("hguideknob.png");
        private final int knobLongDimension = horizontalKnob.getWidth(null);
        private final int knobShortDimension = horizontalKnob.getHeight(null);
        private final String tip = BDApp.getResourceMap(DraftingPanel.class).getString("horizontalGuideTip.text");

        public void initialize(boolean reset) {
            Rectangle.Double ex = draftingCoordinates.getExtent();
            yMinGrid = draftingCoordinates.worldToGridX(ex.getMinY());
            yMaxGrid = draftingCoordinates.worldToGridY(ex.getMaxY());
            if (reset) {
                yGrid = yMinGrid;
            }
            else {
                if (yGrid < yMinGrid) {
                    yGrid = yMinGrid;
                }
                if (yGrid > yMaxGrid) {
                    yGrid = yMaxGrid;
                }
            }
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void paint(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext context) {
            int yViewport = viewportTransform.worldToViewportY(draftingCoordinates.gridToWorldY(yGrid));
            g.setColor(Color.GREEN);
            g.drawLine(0, yViewport, getWidth() - 1, yViewport);
            g.drawImage(horizontalKnob, 0, yViewport - knobShortDimension / 2, null);
        }

        public void paintHot(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext context) {
            paint(g, viewportTransform, context);
        }

        public void getViewportExtent(Rectangle dst, ViewportTransform viewportTransform) {
            getKnobExtent(dst);
            dst.width = getWidth() - 1;
        }

        public Cursor getCursor() {
            return cursor;
        }

        public Rectangle getKnobExtent(Rectangle extent) {
            if (extent == null) {
                extent = new Rectangle();
            }
            int yViewport = viewportTransform.worldToViewportY(draftingCoordinates.gridToWorldY(yGrid));
            extent.x = 0;
            extent.y = yViewport - knobShortDimension / 2;
            extent.width = knobLongDimension;
            extent.height = knobShortDimension;
            return extent;
        }

        public boolean startDrag(Point ptViewport) {
            setVisible(false);
            paintBackingStore();
            //return queryDrag(ptViewport);
            return true;
        }
        private int newYGrid = -1;

        public boolean queryDrag(Point ptViewport) {
            newYGrid = draftingCoordinates.worldToGridY(viewportTransform.viewportToWorldY(ptViewport.y));
            if (newYGrid > yMaxGrid) {
                newYGrid = yMaxGrid;
            }
            if (newYGrid < yMinGrid) {
                newYGrid = yMinGrid;
            }
            return (newYGrid != yGrid);
        }

        public void updateDrag(Point point) {
            yGrid = newYGrid;
        }

        public void stopDrag(Point point) {
            setVisible(true);
            paintBackingStore();
            repaint();
        }
        
        @Override
        public String toString() {
            return tip;
        }
    }
    
    private class VerticalGuide implements HotDraggableItem<BridgePaintContext> {

        private boolean visible = false;
        private int xLeftMinGrid;
        private int xLeftMaxGrid;
        private int xRightMaxGrid;
        private int xLeftGrid;
        private int xRightGrid;
        private final Cursor cursor = getCustomCursor("horizontalsize.png", 15, 15, "Move Vertical Guide");
        private final Image verticalKnob = BDApp.getApplication().getImageResource("vguideknob.png");
        private final int knobLongDimension = verticalKnob.getHeight(null);
        private final int knobShortDimension = verticalKnob.getWidth(null);
        private final String tip = BDApp.getResourceMap(DraftingPanel.class).getString("verticalGuideTip.text");

        public void initialize(boolean reset) {
            Rectangle.Double ex = draftingCoordinates.getExtent();
            xLeftMinGrid = draftingCoordinates.worldToGridX(ex.getMinX());
            xRightMaxGrid = draftingCoordinates.worldToGridY(ex.getMaxX());
            xLeftMaxGrid = xLeftMinGrid;
            int xRightMinGrid = xRightMaxGrid;
            final int snap = draftingCoordinates.getSnapMultiple();
            while (xLeftMaxGrid + snap <= xRightMinGrid - snap) {
                xLeftMaxGrid += snap;
                xRightMinGrid -= snap;
            }
            if (reset) {
                xLeftGrid = xLeftMinGrid;
                xRightGrid = xRightMaxGrid;
            }
            else {
                if (xLeftGrid < xLeftMinGrid) {
                    xLeftGrid = xLeftMinGrid;
                }
                if (xLeftGrid > xLeftMaxGrid) {
                    xLeftGrid = xLeftMaxGrid;
                }
                if (xRightGrid < xRightMinGrid) {
                    xRightGrid = xRightMinGrid;
                }
                if (xRightGrid > xRightMaxGrid) {
                    xRightGrid = xRightMaxGrid;
                }
            }
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void paint(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext context) {
            int xLeftViewport = viewportTransform.worldToViewportX(draftingCoordinates.gridToWorldX(xLeftGrid));
            int xRightViewport = viewportTransform.worldToViewportX(draftingCoordinates.gridToWorldX(xRightGrid));
            g.setColor(Color.GREEN);
            g.drawLine(xLeftViewport, 0, xLeftViewport, getHeight() - 1);
            g.drawLine(xRightViewport, 0, xRightViewport, getHeight() - 1);
            g.drawImage(verticalKnob, xLeftViewport - knobShortDimension / 2, getHeight() - knobLongDimension, null);
        }

        public void paintHot(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext context) {
            paint(g, viewportTransform, context);
        }

        public void getViewportExtent(Rectangle dst, ViewportTransform viewportTransform) {
            int xLeftViewport = viewportTransform.worldToViewportX(draftingCoordinates.gridToWorldX(xLeftGrid));
            int xRightViewport = viewportTransform.worldToViewportX(draftingCoordinates.gridToWorldX(xRightGrid));
            dst.x = xLeftViewport - knobShortDimension / 2;
            dst.y = 0;
            dst.width = xRightViewport - xLeftViewport + knobShortDimension + 1;
            dst.height = getHeight();
        }

        public Cursor getCursor() {
            return cursor;
        }

        public Rectangle getKnobExtent(Rectangle extent) {
            if (extent == null) {
                extent = new Rectangle();
            }
            int xLeftViewport = viewportTransform.worldToViewportX(draftingCoordinates.gridToWorldX(xLeftGrid));
            extent.x = xLeftViewport - knobShortDimension / 2;
            extent.y = getHeight() - knobLongDimension;
            extent.width = knobShortDimension;
            extent.height = knobLongDimension;
            return extent;
        }

        public boolean startDrag(Point ptViewport) {
            setVisible(false);
            paintBackingStore();
            // return queryDrag(ptViewport);
            return true;
        }
        private int newXLeftGrid = -1;

        public boolean queryDrag(Point ptViewport) {
            newXLeftGrid = draftingCoordinates.worldToGridX(viewportTransform.viewportToWorldX(ptViewport.x));
            if (newXLeftGrid < xLeftMinGrid) {
                newXLeftGrid = xLeftMinGrid;
            }
            if (newXLeftGrid > xLeftMaxGrid) {
                newXLeftGrid = xLeftMaxGrid;
            }
            return (newXLeftGrid != xLeftGrid);
        }

        public void updateDrag(Point ptViewport) {
            xLeftGrid = newXLeftGrid;
            xRightGrid = xRightMaxGrid - (newXLeftGrid - xLeftMinGrid);
        }

        public void stopDrag(Point point) {
            setVisible(true);
            paintBackingStore();
            repaint();
        }

        @Override
        public String toString() {
            return tip;
        }
    }

    /**
     * Make the symmetry guides visible or invisible.
     * 
     * @param areVisible whether guides are visible
     */
    public void setGuidesVisible(boolean areVisible) {
        horizontalGuide.setVisible(areVisible);
        verticalGuide.setVisible(areVisible);
        paintBackingStore();
        repaint();
    }

    /**
     * Make the template sketch visible or invisible.
     * 
     * @param isVisible whether the template sketch is visible
     */
    public void setTemplateVisible(boolean isVisible) {
        bridgeView.setTemplateVisible(isVisible);
        paintBackingStore();
        repaint();        
    }
    
    private class Crosshairs {

        final private Point ptViewport = new Point();
        final private Point ptGrid = new Point();
        final private Affine.Point ptWorld = new Affine.Point();
        private final Point snapPtViewport = new Point();
        private boolean valid = false;

        public Point getPtViewport() {
            return ptViewport;
        }

        public Affine.Point getPtWorld() {
            return ptWorld;
        }

        public boolean areValid() {
            return valid;
        }

        // Snap from a world point, which is unaffected by the operation.
        public boolean snap(Affine.Point newPtWorld) {
            ptWorld.setLocation(newPtWorld);
            return snap();
        }
        
        // Snap from a viewport point, which is unaffected by the operation.
        public boolean snap(Point ptViewport) {
            viewportTransform.viewportToWorld(ptWorld, ptViewport);
            return snap();
        }
        
        // Snap from a viewport point, which is unaffected by the operation.
        // Move the given offset point by the snap displacement.
        public boolean snap(Point ptViewport, Point offsetViewport) {
            final boolean rtn = snap(ptViewport);
            offsetViewport.move(snapPtViewport.x - ptViewport.x, snapPtViewport.y - ptViewport.y);
            return rtn;
        }
        
        // Snap locally stored world point, modifying it along the way.
        private boolean snap() {
            draftingCoordinates.shiftToNearestValidWorldPoint(ptWorld, ptGrid, ptWorld);
            viewportTransform.worldToViewport(snapPtViewport, ptWorld);
            return (!snapPtViewport.equals(this.ptViewport));           
        }

        public void update(boolean valid) {
            ptViewport.setLocation(snapPtViewport);
            this.valid = valid;
        }

        public void paint(Graphics2D g) {
            if (valid) {
                Color savedColor = g.getColor();
                g.setColor(Color.BLUE);
                g.drawLine(0, ptViewport.y, getWidth() - 1, ptViewport.y);
                g.drawLine(ptViewport.x, 0, ptViewport.x, getHeight() - 1);
                g.drawOval(ptViewport.x - Joint.pixelRadius, ptViewport.y - Joint.pixelRadius, 2 * Joint.pixelRadius, 2 * Joint.pixelRadius);
                g.setColor(savedColor);
            }
        }

        public void paintAt(Point ptViewport) {
            if (snap(ptViewport)) {
                erase();
                update(true);
                Graphics2D g = (Graphics2D) getGraphics();
                paint(g);
                g.dispose();
            }
        }

        public void paintAt(Affine.Point ptWorld) {
            if (snap(ptWorld)) {
                erase();
                update(true);
                Graphics2D g = (Graphics2D) getGraphics();
                paint(g);
                g.dispose();
            }
        }
        
        public void erase() {
            if (valid) {
                // Invalidate crosshairs before painting so painter doesn't repaint them.
                valid = false;
                repaint(ptViewport.x - 1, 0, 3, getHeight() - 1);
                repaint(0, ptViewport.y - 1, getWidth() - 1, 3);
                repaint(ptViewport.x - Joint.pixelRadius, ptViewport.y - Joint.pixelRadius,
                        2 * Joint.pixelRadius + 1, 2 * Joint.pixelRadius + 1);
            }
        }
    }

    private class EditJointsListener extends HotMouseListener<BridgePaintContext> {

        public EditJointsListener() {
            super(getComponent(), viewportTransform, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            addListener(hotVerticalGuideKnobListener);
            addListener(hotHorzontalGuideKnobListener);
            addListener(hotLabelsListener);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
            if (getHot() == null) {
                crosshairs.paintAt(e.getPoint());
            } else {
                crosshairs.erase();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                int rtn = EditableBridgeModel.ADD_JOINT_OK;
                if (crosshairs.areValid()) {
                    rtn = bridge.addJoint(crosshairs.getPtWorld());
                }
                if (rtn != EditableBridgeModel.ADD_JOINT_OK) {
                    ResourceMap resourceMap = BDApp.getResourceMap(DraftingPanel.class);
                    switch (rtn) {
                        case EditableBridgeModel.ADD_JOINT_AT_MAX:
                            JOptionPane.showMessageDialog(BDApp.getFrame(),
                                resourceMap.getString("atMaxJoints.text", DesignConditions.maxJointCount),
                                resourceMap.getString("atMaxJointsTitle.text"),
                                JOptionPane.WARNING_MESSAGE);
                            break;
                        // case for existing joint passes with no action
                    }
                }
            }
            else if (e.isPopupTrigger()) {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());                
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                super.mouseReleased(e);
            }
            else if (e.isPopupTrigger()) {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());                
            }
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            if (!isDragging()) {
                crosshairs.paintAt(e.getPoint());
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (!isDragging()) {
                crosshairs.erase();
            }
        }
    }

    private class RubberBandMember {

        private Joint jointA;
        private Joint jointB;
        private final Affine.Point ptAWorld = new Affine.Point();
        private final Affine.Point ptBWorld = new Affine.Point();
        private final Point ptAViewport = new Point();
        private final Point ptBViewport = new Point();
        private final Rectangle dirty = new Rectangle();

        public Joint getJointA() {
            return jointA;
        }

        public Joint getJointB() {
            return jointB;
        }

        public void selectJointA(Joint joint) {
            jointA = joint;
            if (jointA != null) {
                viewportTransform.worldToViewport(ptAViewport, jointA.getPointWorld());
            }
        }

        public void selectJointB(Joint joint, Point ptViewport) {
            if (jointA != null) {
                if (joint == null) {
                    erase();
                    jointB = null;
                    ptBViewport.setLocation(ptViewport);
                    Graphics2D g = (Graphics2D) getGraphics();
                    Member.draw(g, ptAViewport, ptBViewport, -1, 0, Color.BLUE, null, null);
                    jointA.paintHot(g, viewportTransform, null);
                    g.dispose();
                } else if (joint != jointA && joint != jointB) {
                    erase();
                    jointB = joint;
                    viewportTransform.worldToViewport(ptBViewport, jointB.getPointWorld());
                    Graphics2D g = (Graphics2D) getGraphics();
                    Member.draw(g, ptAViewport, ptBViewport, -1, 0, Color.BLUE, null, null);
                    jointA.paintHot(g, viewportTransform, null);
                    jointB.paintHot(g, viewportTransform, null);
                    g.dispose();
                }
            }
        }

        public void clear() {
            erase();
            jointA = jointB = null;
        }

        private void erase() {
            if (jointA != null) {
                dirty.setFrameFromDiagonal(ptAViewport, ptBViewport);
                dirty.grow(erasureMargin, erasureMargin);
                paintImmediately(dirty);
            }
        }
    }

    private class EditMembersListener extends HotMouseListener<BridgePaintContext> {

        private final RubberBandMember rubberMember = new RubberBandMember();

        public EditMembersListener() {
            super(getComponent(), viewportTransform, getCustomCursor("pencil.png", 0, 31, "Pencil"));
            addListener(new HotBridgeElementListener() {

                public HotItem<BridgePaintContext> getItem(Point ptViewport, Affine.Point ptWorld) {
                    return bridge.findJoint(ptWorld, viewportTransform.viewportToWorldDistance(4 * Joint.pixelRadius), false);
                }

                public Cursor getCursor() {
                    return getDefaultCursor();
                }
            });
            addListener(hotVerticalGuideKnobListener);
            addListener(hotHorzontalGuideKnobListener);
            addListener(hotLabelsListener);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (getHot() instanceof Joint) {
                    rubberMember.selectJointA((Joint) getHot());
                }
            }
            else if (e.isPopupTrigger()) {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());                
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Joint jointA = rubberMember.getJointA();
                Joint jointB = rubberMember.getJointB();
                int rtn = EditableBridgeModel.ADD_MEMBER_OK;
                if (jointA != null && jointB != null && stockSelector != null) {
                    rtn = bridge.addMember(jointA, jointB,
                                           stockSelector.getMaterialIndex(),
                                           stockSelector.getSectionIndex(),
                                           stockSelector.getSizeIndex());
                }
                rubberMember.clear();
                super.mouseReleased(e);  // end hot item drag
                if (rtn != EditableBridgeModel.ADD_MEMBER_OK) {
                    ResourceMap resourceMap = BDApp.getResourceMap(DraftingPanel.class);
                    switch (rtn) {
                        case EditableBridgeModel.ADD_MEMBER_AT_MAX:
                            JOptionPane.showMessageDialog(BDApp.getFrame(),
                                resourceMap.getString("atMaxMembers.text", DesignConditions.maxMemberCount),
                                resourceMap.getString("atMaxMembersTitle.text"),
                                JOptionPane.WARNING_MESSAGE);
                            break;
                        case EditableBridgeModel.ADD_MEMBER_CROSSES_PIER:
                            JOptionPane.showMessageDialog(BDApp.getFrame(),
                                resourceMap.getString("memberCrossesPier.text"),
                                resourceMap.getString("memberCrossesPierTitle.text"),
                                JOptionPane.WARNING_MESSAGE);
                            break;
                        // cases for same joint and existing member pass with no action
                    }
                }
            }
            else if (e.isPopupTrigger()) {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());                
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (rubberMember.getJointA() == null) {
                super.mouseDragged(e);
            } else {
                super.mouseMoved(e);
                if (getHot() == null || getHot() instanceof Joint) {
                    rubberMember.selectJointB((Joint) getHot(), e.getPoint());
                }
            }
        }
    }
    /**
     * Dashed line stroke for selection rectangle.
     */
    private final static float dashes[] = {3.0f, 3.0f};
    private final static BasicStroke dashedStroke = new BasicStroke(
            0.0f, // thickness = thin as possible
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, // miterLimit
            dashes, // dash pattern
            0.0f);                  // dash phase

    private class Selector {

        private abstract class Cursor {

            abstract public void start(HotEditableItem<BridgePaintContext> hot, Point ptViewport, boolean extendSelection);

            abstract public void seek(Point ptViewport);

            abstract public void end(HotEditableItem<BridgePaintContext> hot);

            abstract public void clear();
        }
        RectangleCursor rectangleCursor = new RectangleCursor();
        JointMoveCursor jointMoveCursor = new JointMoveCursor();
        Cursor cursor;

        public void start(HotEditableItem<BridgePaintContext> hot, Point ptViewport, boolean extendSelection) {
            cursor = null;
            if (bridge.select(hot, extendSelection)) {
                if (hot instanceof Joint) {
                    cursor = jointMoveCursor;
                }
            } else {
                cursor = rectangleCursor;
            }
            if (cursor != null) {
                cursor.start(hot, ptViewport, extendSelection);
            }
        }

        public void seek(Point ptViewport) {
            if (cursor != null) {
                cursor.seek(ptViewport);
            }
        }

        public void end(HotEditableItem<BridgePaintContext> hot) {
            if (cursor != null) {
                cursor.end(hot);
                cursor.clear();
                cursor = null;
            }
        }

        private class RectangleCursor extends Cursor {

            private final Rectangle extent = new Rectangle();

            public Rectangle getExtent() {
                return extent;
            }
            private final Point anchor = new Point(-1, -1);

            public void start(HotEditableItem<BridgePaintContext> hot, Point pt, boolean extendSelection) {
                bridge.beginAreaSelection(extendSelection);
                anchor.setLocation(pt);
            }

            public void seek(Point pt) {
                erase();
                extent.setFrameFromDiagonal(anchor, pt);
                paint();
            }
            Rectangle.Double rectangleCursorWorld = new Rectangle.Double();

            public void end(HotEditableItem<BridgePaintContext> hot) {
                viewportTransform.viewportToWorld(rectangleCursorWorld, extent);
                if (!bridge.selectMembers(rectangleCursorWorld)) {
                    erase();
                }
            }

            public void clear() {
                anchor.setLocation(-1, -1);
            }

            private void paint() {
                if (anchor.x >= 0) {
                    Graphics2D g = (Graphics2D) getGraphics();
                    Stroke savedStroke = g.getStroke();
                    g.setStroke(dashedStroke);
                    Color savedColor = g.getColor();
                    g.setColor(Color.BLUE);
                    g.drawRect(extent.x, extent.y, extent.width - 1, extent.height - 1);
                    g.setStroke(savedStroke);
                    g.setColor(savedColor);
                    g.dispose();
                }
            }

            private void erase() {
                if (anchor.x >= 0) {
                    paintImmediately(extent);
                }
            }
        }

        class JointMoveCursor extends Cursor {

            private final ArrayList<Point> ptOther = new ArrayList<Point>();
            private final Rectangle partialCursorExtent = new Rectangle(0, 0, -1, -1);
            private final Rectangle cursorExtent = new Rectangle();
            private final Point snapOffset = new Point();

            public void start(HotEditableItem<BridgePaintContext> hot, Point ptViewport, boolean extendSelection) {
                Joint joint = (Joint) hot;
                Member[] membersOfJoint = bridge.findMembersWithJoint(joint);
                // Accumulate a list of all locations of joints connected to this one in viewport coordinates.
                ptOther.clear();
                for (int i = 0; i < membersOfJoint.length; i++) {
                    Member member = membersOfJoint[i];
                    Point newPtViewport = viewportTransform.worldToViewport(null, member.otherJoint(joint).getPointWorld());
                    ptOther.add(newPtViewport);
                    partialCursorExtent.add(newPtViewport);
                }
                partialCursorExtent.grow(erasureMargin, erasureMargin);
                // Snap to nearest grid so joint selection does not cause crosshairs to flash.
                // Also remember offset from pointer to grid so that the pointer acts like a handle
                // where the joint was selected and the joint center does not become the effective handle,
                // which causes it to jump if the selection is off-center and the grid is fine.
                crosshairs.snap(ptViewport, snapOffset);
                crosshairs.update(false);
            }

            private final Point adjustedPtViewport = new Point();
            
            public void seek(Point ptViewport) {
                adjustedPtViewport.move(ptViewport.x + snapOffset.x, ptViewport.y + snapOffset.y);
                paint(adjustedPtViewport);
            }

            public void end(HotEditableItem<BridgePaintContext> hot) {
                if (crosshairs.areValid()) {
                    switch (bridge.moveJoint((Joint) hot, crosshairs.getPtWorld())) {
                        case EditableBridgeModel.MOVE_JOINT_MEMBER_AT_MAX:
                            ResourceMap resourceMap = BDApp.getResourceMap(DraftingPanel.class);
                            JOptionPane.showMessageDialog(BDApp.getFrame(),
                                resourceMap.getString("atMaxMembers.text", DesignConditions.maxMemberCount),
                                resourceMap.getString("atMaxMembersTitle.text"),
                                JOptionPane.WARNING_MESSAGE);                    
                            break;
                        case EditableBridgeModel.MOVE_JOINT_JOINT_EXISTS:
                            Toolkit.getDefaultToolkit().beep();
                            break;
                    }
                }
            }

            public void clear() {
                erase();
                partialCursorExtent.setBounds(0, 0, -1, -1);
            }

            private void paint(Point ptViewport) {
                if (crosshairs.snap(ptViewport)) {
                    erase();
                    crosshairs.update(true);
                    Graphics2D g = (Graphics2D) getGraphics();
                    crosshairs.paint(g);
                    Stroke savedStroke = g.getStroke();
                    g.setStroke(dashedStroke);
                    Color savedColor = g.getColor();
                    g.setColor(Color.BLUE);
                    Iterator<Point> e = ptOther.iterator();
                    Point ptJoint = crosshairs.getPtViewport();
                    while (e.hasNext()) {
                        Point point = e.next();
                        g.drawLine(ptJoint.x, ptJoint.y, point.x, point.y);
                    }
                    g.setStroke(savedStroke);
                    g.setColor(savedColor);
                    g.dispose();
                }
            }

            /**
             * Erase the cursor by computing a bounding box that includes all the joints connected
             * to the hot joint and also the current crosshair intersection.  Then restore the bounding
             * box from the backing store.
             */
            private void erase() {
                if (crosshairs.areValid()) {
                    cursorExtent.setBounds(partialCursorExtent);
                    cursorExtent.add(crosshairs.getPtViewport());
                    cursorExtent.grow(1, 1); // needed due to rectangle and drawing coordinate convention
                    paintImmediately(cursorExtent);
                    crosshairs.erase();
                }
            }
        }
    }

    private class EditSelectListener extends HotMouseListener<BridgePaintContext> {

        private final Selector selector = new Selector();

        public EditSelectListener() {
            super(getComponent(), viewportTransform, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            addListener(new HotJointOrMemberListener(Joint.pixelRadius) {

                public Cursor getCursor() {
                    return getDefaultCursor();
                }
            });
            addListener(hotVerticalGuideKnobListener);
            addListener(hotHorzontalGuideKnobListener);
            addListener(hotLabelsListener);
        }

        private void popUpContextDialog(MouseEvent e) {
            if (getHot() instanceof Member && ((Member)getHot()).isSelected()) {
                contextComponentProvider.showMemberEditPopup(e.getXOnScreen(), e.getYOnScreen());
            }
            else {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());
            }       
        }
    
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (getHot() == null || getHot() instanceof HotEditableItem) {
                    selector.start((HotEditableItem<BridgePaintContext>) getHot(), e.getPoint(), e.isControlDown());
                }
            }
            else if (e.isPopupTrigger()) {
                popUpContextDialog(e);                
            } 
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (getHot() == null || getHot() instanceof Joint) {
                selector.seek(e.getPoint());
            } else { // It's a potentially dragged hot item
                super.mouseDragged(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                selector.seek(e.getPoint());
                if (getHot() == null || getHot() instanceof HotEditableItem) {
                    selector.end((HotEditableItem<BridgePaintContext>) getHot());
                    // Grab focus for keyboard movement of selected joint.
                    if (bridge.getSelectedJoint() != null) {
                        setFocusable(true);
                        setRequestFocusEnabled(true);
                        requestFocusInWindow();
                    }
                }
                super.mouseReleased(e);
            }
            else if (e.isPopupTrigger()) {
                popUpContextDialog(e);
            }
        }
    }

    private class EditEraseListener extends HotMouseListener<BridgePaintContext> {

        public EditEraseListener() {
            super(getComponent(), viewportTransform, getCustomCursor("pencilud.png", 2, 29, "Eraser"));
            addListener(new HotJointOrMemberListener(Joint.pixelRadius) {

                public Cursor getCursor() {
                    return getDefaultCursor();
                }
            });
            addListener(hotVerticalGuideKnobListener);
            addListener(hotHorzontalGuideKnobListener);
            addListener(hotLabelsListener);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (getHot() == null || getHot() instanceof Editable) {
                    Editable hot = (Editable) getHot();
                    clear();
                    bridge.delete(hot);
                }
            }
            else if (e.isPopupTrigger()) {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());                
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                super.mouseReleased(e);
            }
            else if (e.isPopupTrigger()) {
                contextComponentProvider.showDraftingPopup(e.getX(), e.getY());                
            }
        }
    }
}
