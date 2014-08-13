/*
 * BridgePaintContext.java  
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

/**
 * Little class used to hold context for painting bridge views.  See <code>BridgeView</code> and subclasses.
 * Could be a nested static class there.
 * 
 * @author Eugene K. Ressler
 */
public class BridgePaintContext {
    /**
     * Members more slender than this should be graphically tagged in some manner.
     */
    public double allowableSlenderness = 1e100;
    /**
     * Members should be labeled with their numbers iff true.
     */
    public boolean label = false;
    /**
     * This is a blueprint rendering (normally for printing purposes).
     */
    public boolean blueprint = false;
    /**
     * Gussets to use for joint rendering (normally for printing purposes).
     */
    public Gusset [] gussets = null;

    /**
     * Construct a default painting context.
     */
    public BridgePaintContext () {}
    
    /**
     * Construct a bridge view paint context that is default except for given values.
     * 
     * @param allowableSlenderness members more slender than this should be graphically tagged.
     * @param label member numbers should be shown iff true
     */
    public BridgePaintContext(double allowableSlenderness, boolean label) {
        this.allowableSlenderness = allowableSlenderness;
        this.label = label;
    }
    
    /**
     * Construct a blueprint view paint context with given gussets.
     * 
     * @param gussets gussets to use for drawing joints
     */
    public BridgePaintContext(Gusset [] gussets) {
        this.label = true;
        this.blueprint = true;
        this.gussets = gussets;
    }
}
