/*
 * PreviewAccessory.java  
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jdesktop.application.ResourceMap;

/**
 * A bridge preview accessory that can be attached to a JFileChooser.
 * 
 * @author Eugene K. Ressler
 */
public class PreviewAccessory extends JPanel implements PropertyChangeListener {

    final private BridgeModel cartoonBridge;
    final private BridgeView cartoonView;
    final private JLabel drawing;

    /**
     * Create a preview accessory. This must be installed with both setAccessory() and addPropertyChangeListener().
     */
    public PreviewAccessory() {
        Dimension size = new Dimension(300, 100);
        setMinimumSize(size);
        setPreferredSize(size);
        ResourceMap resourceMap = BDApp.getResourceMap(PreviewAccessory.class);
        JLabel previewLabel = new JLabel(resourceMap.getString("preview.text"));
        final int h = previewLabel.getPreferredSize().height;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createMatteBorder(0, h, h, h, getBackground())));
        cartoonBridge = new BridgeModel();
        cartoonView = new BridgeDraftingView(cartoonBridge);
        drawing = cartoonView.getDrawing(2, resourceMap.getString("noBridge.text"));
        setLayout(new BorderLayout());
        add(previewLabel, BorderLayout.NORTH);
        add(drawing, BorderLayout.CENTER);
    }

    /**
     * Handle property change events from the file chooser. Ignores all but SELECTED_FILE_CHANGED, 
     * which indicates that the user has changed the selected file in the chooser.
     * 
     * @param evt the property change event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
            File f = (File)evt.getNewValue();
            if (f != null && f.getName().toLowerCase().endsWith(".bdc")) {
                try {
                    cartoonBridge.read(f);
                    cartoonView.initialize(cartoonBridge.getDesignConditions());
                } catch (IOException ex) {  
                    cartoonView.initialize(null);
                }               
            }
            else {
                cartoonView.initialize(null);
            }
            drawing.repaint();
        }
    }
}
