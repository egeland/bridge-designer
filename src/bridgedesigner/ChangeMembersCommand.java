/*
 * ChangeMembersCommand.java  
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
 * Command to change the stock used for the selected members in various ways.
 * 
 * @author Eugene K. Ressler
 */
public class ChangeMembersCommand extends EditCommand {

    private Member[] members;
    
    /**
     * Construct a command that changes all selected members' stock to a new, prescribed one.
     * 
     * @param bridge bridge containing the selected members
     * @param materialIndex material of the new member stock
     * @param sectionIndex section of the new member stock
     * @param sizeIndex size of the new member stock
     */
    public ChangeMembersCommand(EditableBridgeModel bridge, int materialIndex, int sectionIndex, int sizeIndex) {
        super(bridge);
        members = bridge.getSelectedMembers();
        for (int i = 0; i < members.length; i++) {
            members[i] = new Member(members[i], bridge.getInventory(), materialIndex, sectionIndex, sizeIndex);
            // Want to select these because a subsequent change command should work on the same set.
            members[i].setSelected(true);
        }
        presentationName = getMembersMessage("changeMaterial.text", members);
    }
    
    /**
     * Construct a command that increments or decrements sizes of all selected members by a given amount.
     * 
     * @param bridge bridge containing the selected members
     * @param sizeOffset + or - 1 to cause increment or decrement respectively
     */
    public ChangeMembersCommand(EditableBridgeModel bridge, int sizeOffset) {
        super(bridge);
        members = bridge.getSelectedMembers();
        for (int i = 0; i < members.length; i++) {
            members[i] = new Member(members[i], bridge.getInventory().getShape(members[i].getShape(), sizeOffset));
            members[i].setSelected(true);
        }
        presentationName = getMembersMessage(sizeOffset > 0 ? "increaseSize.text" : "decreaseSize.text", members);
    }

    @Override 
    public void go() {
        EditCommand.exchange(bridge.getMembers(), members);        
    }

    @Override
    void goBack() {
        EditCommand.exchange(bridge.getMembers(), members);        
    }
}
