/*
 * Help.java  
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

import java.net.URL;
import javax.help.HelpBroker;
import javax.help.HelpSet;

/**
 * Consolidated support for access to the JavaHelp set for the Bridge Designer.
 * 
 * @author Eugene K. Ressler
 */
public class Help {

    private static final String helpsetFilename = "/bridgedesigner/help/help.hs";
    private static HelpSet helpSet;
    private static HelpBroker helpBroker;
    
    /**
     * Initialize access to the help set, which is stored in application resources.
     * 
     * @return true iff initialization succeeded
     */
    public static boolean initialize() {
        if (helpSet == null) {
            try {
                URL hsURL = BDApp.getApplication().getClass().getResource(helpsetFilename);
                helpSet = new HelpSet(null, hsURL);
            } catch (Exception ee) {
                System.err.println("Open Help Set: " + ee.getMessage());
                System.err.println("Help Set "+ helpsetFilename +" not found");
                return false;
            }
            if (helpBroker == null) {
                helpBroker = helpSet.createHelpBroker();
            }
        }
        return true;
    }

    /**
     * Return the application standard JavaHelp help broker.
     * 
     * @return help broker
     */
    public static HelpBroker getBroker() {
        return helpBroker;
    }

    /**
     * Return the WPBD help set.
     * 
     * @return help set
     */
    public static HelpSet getSet() {
        return helpSet;
    }
}
