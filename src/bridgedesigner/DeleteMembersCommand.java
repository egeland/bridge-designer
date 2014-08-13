/*
 * DeleteMembersCommand.java  
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

/**
 * Undoable/redoable command to delete 1 or many members.
 * 
 * @author Eugene K. Ressler
 */
public class DeleteMembersCommand extends EditCommand {

    /**
     * Joints to delete because all connected members are being deleted.
     */
    protected Joint[] joints;
    /**
     * Members to delete.
     */
    protected Member[] members;

    /**
     * Construct a command to delete selected members.
     * 
     * @param bridge bridge containing selected members
     */
    public DeleteMembersCommand(EditableBridgeModel bridge) {
        super(bridge);
        members = bridge.getSelectedMembers();
        joints = bridge.getJointsToDeleteWithSelectedMembers();
        presentationName = getMembersMessage("deleteMember.text", members);
    }

    /**
     * Construct a command to delete a specific member.
     * 
     * @param bridge bridge containing the member to delete
     * @param member member to delete
     */
    public DeleteMembersCommand(EditableBridgeModel bridge, Member member) {
        super(bridge);
        members = new Member[] { member };
        joints = new Joint[0];
        presentationName = getMembersMessage("deleteMember.text", members);
    }
    
    @Override
    public void go() {
        for (int i = 0; i < members.length; i++) {
            members[i].setSelected(false);
        }
        EditCommand.delete(bridge.getJoints(), joints);
        EditCommand.delete(bridge.getMembers(), members);
    }

    @Override
    public void goBack() {
        EditCommand.insert(bridge.getJoints(), joints);
        EditCommand.insert(bridge.getMembers(), members);
    }
}
