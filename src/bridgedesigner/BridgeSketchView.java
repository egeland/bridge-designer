/*
 * BridgeSketchView.java  
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

import java.awt.Graphics2D;

/**
 * View of a bridge template sketch.
 * 
 * @author Eugene K. Ressler
 */
public abstract class BridgeSketchView {
    
    /**
     * Sketch that this view draws.
     */
    protected BridgeSketchModel model;

    /**
     * Set the bridge template sketch for this view.
     * 
     * @param model template sketch
     */
    public void setModel(BridgeSketchModel model) {
        this.model = model;
    }

    /**
     * Return the bridge template sketch of this view.
     * 
     * @return bridge template sketch
     */
    public BridgeSketchModel getModel() {
        return model;
    }
    
    /**
     * Paint the sketch.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    public abstract void paint(Graphics2D g, ViewportTransform viewportTransform);
}
