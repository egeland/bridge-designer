/*
 * FixupCommand.java  
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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Undoable/redoable command to repair violations of design rules prior to analysis.  This
 * is concerned with splitting members that are exactly transsected by joints.
 * 
 * @author Eugene K. Ressler
 */
public class FixupCommand extends EditCommand {

    /**
     * Transsected members to delete.
     */
    protected Member[] deleteMembers;
    /**
     * Replacement members to insert.
     */
    protected Member[] insertMembers;

    /**
     * Construct a fixup command for the given bridge.
     * 
     * @param bridge bridge to fix up
     */
    public FixupCommand(EditableBridgeModel bridge) {
        super(bridge);
        ArrayList<Member> toDelete = new ArrayList<Member>();
        ArrayList<Member> toInsert = new ArrayList<Member>();
        ArrayList<Joint> transsected = new ArrayList<Joint>();
        Iterator<Member> m = bridge.getMembers().iterator();
        while (m.hasNext()) {
            Member member = m.next();
            bridge.getTranssectedJoints(transsected, member.getJointA(), member.getJointB());
            if (transsected.size() > 0) {
                toDelete.add(member);
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
            }
        }
        int i = bridge.getMembers().size() - toDelete.size();
        m = toInsert.iterator();
        while (m.hasNext()) {
            m.next().setIndex(i++);
        }
        deleteMembers = toDelete.toArray(new Member[toDelete.size()]);
        insertMembers = toInsert.toArray(new Member[toInsert.size()]);
        presentationName = getMembersMessage("autofix.text", deleteMembers);
    }
    
    /**
     * Return the number of members that had to be split in order to effect the fix-up.
     * 
     * @return number of members split
     */
    public int revisedMemberCount() {
        return deleteMembers.length;
    }

    @Override
    public void go() {
        for (int i = 0; i < deleteMembers.length; i++) {
            deleteMembers[i].setSelected(false);
        }
        EditCommand.delete(bridge.getMembers(), deleteMembers);
        EditCommand.insert(bridge.getMembers(), insertMembers);
    }

    @Override
    public void goBack() {
        EditCommand.delete(bridge.getMembers(), insertMembers);
        EditCommand.insert(bridge.getMembers(), deleteMembers);
    }
}
