/*
 * BridgeSketchDraftingView.java  
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * A view of a bridge template sketch used for the drafting board.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeSketchDraftingView extends BridgeSketchView {

    /**
     * Paint the drafting board template sketch.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    @Override
    public void paint(Graphics2D g, ViewportTransform viewportTransform) {
        if (model == null) {
            return;
        }
        Stroke savedStroke = g.getStroke();
        // We'll use light gray rather than a dashed line because dashes look bad in Swing.
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < model.getSketchMemberCount(); i++) {
            g.drawLine(
                    viewportTransform.worldToViewportX(model.getSketchMember(i).jointA.x),
                    viewportTransform.worldToViewportY(model.getSketchMember(i).jointA.y),
                    viewportTransform.worldToViewportX(model.getSketchMember(i).jointB.x),
                    viewportTransform.worldToViewportY(model.getSketchMember(i).jointB.y));
        }
        for (int i = model.getDesignConditions().getNPrescribedJoints(); i < model.getJointLocationCount(); i++) {
            int x = viewportTransform.worldToViewportX(model.getJointLocation(i).x) - Joint.pixelRadius;
            int y = viewportTransform.worldToViewportY(model.getJointLocation(i).y) - Joint.pixelRadius;
            int size = 2 * Joint.pixelRadius;
            g.setColor(Color.WHITE);
            g.fillOval(x, y, size, size);
            g.setColor(Color.LIGHT_GRAY);
            g.drawOval(x, y, size, size);
        }
        g.setStroke(savedStroke);
    }
}
