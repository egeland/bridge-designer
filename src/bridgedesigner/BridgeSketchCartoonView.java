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

/**
 * A view of a bridge template sketch used in the SetupWizard.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeSketchCartoonView extends BridgeSketchView {

    /**
     * Paint the cartoon template sketch.
     * 
     * @param g java graphics context
     * @param viewportTransform viewport transform from world to screen/printer coordinates
     */
    @Override
    public void paint(Graphics2D g, ViewportTransform viewportTransform) {
        if (model == null) {
            return;
        }
        g.setColor(Color.GRAY);
        for (int i = 0; i < model.getSketchMemberCount(); i++) {
            g.drawLine(
                    viewportTransform.worldToViewportX(model.getSketchMember(i).jointA.x),
                    viewportTransform.worldToViewportY(model.getSketchMember(i).jointA.y),
                    viewportTransform.worldToViewportX(model.getSketchMember(i).jointB.x),
                    viewportTransform.worldToViewportY(model.getSketchMember(i).jointB.y));
        }
        for (int i = 0; i < model.getJointLocationCount(); i++) {
            int x = viewportTransform.worldToViewportX(model.getJointLocation(i).x);
            int y = viewportTransform.worldToViewportY(model.getJointLocation(i).y);
            g.setColor(Color.WHITE);
            g.fillOval(x - BridgeCartoonView.jointRadius, y - BridgeCartoonView.jointRadius, 
                    2 * BridgeCartoonView.jointRadius, 2 * BridgeCartoonView.jointRadius);
            g.setColor(Color.BLACK);
            g.drawOval(x - BridgeCartoonView.jointRadius, y - BridgeCartoonView.jointRadius, 
                    2 * BridgeCartoonView.jointRadius, 2 * BridgeCartoonView.jointRadius);
        }
    }
}
