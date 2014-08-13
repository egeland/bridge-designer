/*
 * InsertMemberCommand.java  
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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Undoable/redoable command to insert a new joint in a bridge.
 * 
 * @author Eugene K. Ressler
 */
public class InsertMemberCommand extends EditCommand {
    
    protected Member[] members;

    /**
     * Add a single member, but split as needed into multiple members due to transsecting joints.
     * 
     * @param bridge bridge model to add member in
     * @param member single member to add
     */
    public InsertMemberCommand(EditableBridgeModel bridge, Member member) {
        super(bridge);
        ArrayList<Member> toInsert = new ArrayList<Member>();
        ArrayList<Joint> transsected = new ArrayList<Joint>();
        bridge.getTranssectedJoints(transsected, member.getJointA(), member.getJointB());
        // Save a little garbage by handling the usual case separately.
        if (transsected.isEmpty()) {
            members = new Member [] { member };
        }
        else {
            transsected.add(member.getJointB());
            Joint a = member.getJointA();
            Iterator<Joint> j = transsected.iterator();
            while (j.hasNext()) {
                Joint b = j.next();
                if (bridge.getMember(a, b) == null){
                    toInsert.add(new Member(member, a, b));
                }
                a = b;
            }
            members = toInsert.toArray(new Member[toInsert.size()]);
        }
        assignMemberIndices();
        presentationName = getMembersMessage("insertMember.text", members);
    }

    /**
     * Add an array of members to the brdige.  The array is not copied.
     * 
     * @param bridge bridge model to add members in
     * @param members array of members to add
     */
    public InsertMemberCommand(EditableBridgeModel bridge, Member [] members) {
        super(bridge);
        this.members = members;
        assignMemberIndices();
        presentationName = getMembersMessage("autoInsertMember.text", members);
    }
    
    private void assignMemberIndices() {
        int n = bridge.getMembers().size();
        for (int i = 0; i < members.length; i++) {
            members[i].setIndex(n++);
        }        
    }
    
    /**
     * Go forward with member insertion.
     */
    public void go() {
        EditCommand.insert(bridge.getMembers(), members);        
    }
    /**
     * Go back to state prior to member insertion.
     */
    public void goBack() {
        EditCommand.delete(bridge.getMembers(), members);        
    }
}
