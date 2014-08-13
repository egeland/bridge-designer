/*
 * TipTextPane.java  
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

import java.awt.Font;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;

/**
 * Special read-only text pane that uses the standard label font and is able to load text from resources.
 * 
 * @author Eugene K. Ressler
 */
public class TipTextPane extends JTextPane {

    /**
     * Construct a new tip text pane.
     */
    public TipTextPane() {
        super();
        setOpaque(false);
        setEditable(false);
        setContentType("text/html");
        setFocusable(false);
        HTMLDocument doc = (HTMLDocument)getDocument();
        Font font = UIManager.getFont("Label.font");
        doc.getStyleSheet().addRule("body { font-family:" + font.getFamily() + ";" + "font-size:" + font.getSize() + "pt; }");
        doc.setBase(BDApp.getApplication().getClass().getResource("/bridgedesigner/resources/"));
    }
}
