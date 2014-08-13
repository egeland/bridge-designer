/*
 * ContextComponentProvider.java  
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
 * Interface for providers of popups to the drafting panel.  Could be nested therein.
 * 
 * @author de8827
 */
public interface ContextComponentProvider {
    /**
     * Show a member edit popup.
     * 
     * @param x screen x-coordinate of upper left corner
     * @param y screen y-coordinate of upper left corner
     */
    public void showMemberEditPopup(int x, int y);
    /**
     * Show a drafting popup.
     * 
     * @param x screen x-coordinate of upper left corner
     * @param y screen y-coordinate of upper left corner
     */
    public void showDraftingPopup(int x, int y);
}
