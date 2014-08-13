/*
 * RulerHost.java  
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

import java.awt.Component;

/**
 * Interface for providing the functionality needed to serve as the host for a Ruler.
 * 
 * @author Eugene K. Ressler
 */
public interface RulerHost {
    /**
     * Return the component we're measuring.
     * 
     * @return component
     */
    abstract public Component getComponent();
    /**
     * Return the viewport transform between world and screen coordinates of the host's component.
     * 
     * @return viewport transform
     */
    abstract public ViewportTransform getViewportTransform();
    /**
     * Return the drafing coordinates that establish the ruler tick locations and sizes.
     * 
     * @return drafting coordinates
     */
    abstract public DraftingCoordinates getDraftingCoordinates();
}
