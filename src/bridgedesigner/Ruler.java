/*
 * Ruler.java  
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.JLabel;

/**
 * Rulers attachable to the drafting panel.
 * 
 * @author Eugene K. Ressler
 */
public class Ruler extends JLabel implements ComponentListener {

    private int side;
    private RulerHost host;
    private static final int tickSize[] = {1, 3, 5};
    private static final NumberFormat labelFormatter = new DecimalFormat("0");

    /**
     * Construct a new ruler with given host and side of attachment.
     * 
     * @param host host for this ruler
     * @param side side of drafing panel ruler is attached to
     */
    public Ruler(RulerHost host, int side) {
        super();
        this.side = side;
        this.host = host;

        // listen to what's happening to the ruler host (currently only resizing)
        host.getComponent().addComponentListener(this);
    }

    /**
     * Paint the ruler.
     * 
     * @param g java graphics context
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!host.getViewportTransform().isSet()) {
            return;
        }
        if (side == SOUTH) {
            g.setColor(Color.WHITE);
            g.drawLine(0, 0, this.getWidth() - 1, 0);
            double xWorld = host.getDraftingCoordinates().getExtent().getX();
            int snapMultiple = host.getDraftingCoordinates().getSnapMultiple();
            double dxWorld = host.getDraftingCoordinates().getGridSize();
            int i0 = host.getDraftingCoordinates().worldToGridX(host.getDraftingCoordinates().getExtent().getMinX());
            int i1 = host.getDraftingCoordinates().worldToGridX(host.getDraftingCoordinates().getExtent().getMaxX());
            final int y0 = 0;
            int i = i0;
            while (i <= i1) {
                int x = host.getViewportTransform().worldToViewportX(xWorld);
                g.setColor(Color.BLACK);
                int level = DraftingGrid.graduationLevel(i);
                int y1 = tickSize[level];
                g.drawLine(x, y0, x, y1);
                g.setColor(Color.WHITE);
                g.drawLine(x + 1, y0, x + 1, y1);
                if (level == 2) {
                    g.setColor(Color.BLACK);
                    Labeler.drawJustified(g, labelFormatter.format(xWorld), x, y1 + 2,
                            Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null);
                }
                xWorld += dxWorld;
                i += snapMultiple;
            }
        } else { // WEST
            double yWorld = host.getDraftingCoordinates().getExtent().getY();
            int snapMultiple = host.getDraftingCoordinates().getSnapMultiple();
            double dyWorld = host.getDraftingCoordinates().getGridSize();

            int i0 = host.getDraftingCoordinates().worldToGridY(host.getDraftingCoordinates().getExtent().getMinY());
            int i1 = host.getDraftingCoordinates().worldToGridY(host.getDraftingCoordinates().getExtent().getMaxY());
            final int x1 = this.getWidth() - 1;
            int i = i0;
            while (i <= i1) {
                int y = host.getViewportTransform().worldToViewportY(yWorld);
                g.setColor(Color.BLACK);
                int level = DraftingGrid.graduationLevel(i);
                int x0 = x1 - tickSize[level];
                g.drawLine(x0, y, x1, y);
                g.setColor(Color.WHITE);
                g.drawLine(x0, y + 1, x1, y + 1);
                if (level == 2) {
                    g.setColor(Color.BLACK);
                    Labeler.drawJustified(g, labelFormatter.format(yWorld), x0 - 2, y,
                            Labeler.JUSTIFY_RIGHT, Labeler.JUSTIFY_CENTER, null);
                }
                yWorld += dyWorld;
                i += snapMultiple;
            }
        }
    }

    /**
     * Handle component resize event.
     * 
     * @param e resize event
     */
    public void componentResized(ComponentEvent e) {
        repaint();
    }

    /**
     * Placeholder for component movement. Does nothing.
     * 
     * @param e movement event
     */
    public void componentMoved(ComponentEvent e) { }

    /**
     * Placeholder for component shown event. Does nothing.
     * 
     * @param e shown event
     */
    public void componentShown(ComponentEvent e) { }

    /**
     * Placeholder for component hidden event. Does nothing.
     * 
     * @param e hidden event
     */
    public void componentHidden(ComponentEvent e) { }
}
