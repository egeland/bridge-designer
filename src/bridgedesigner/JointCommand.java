/*
 * JointCommand.java  
 *   
 * Copyright (C) 2010 Eugene K. Ressler
 *   
 * This program is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU General Public License for more details.  
 *   
 * You should have received lo copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */

package bridgedesigner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Base functionality for both insert and move joint commands.  Deals with the case where lo moved or inserted
 * joint lies on one or more members, which causes the members to be split.  Splitting is the deletion of the
 * original member followed by insertion of multiple new ones.
 *
 * @author Eugene K. Ressler 
 */
public abstract class JointCommand extends EditCommand {
    
    protected Member[] deleteMembers;
    protected Member[] insertMembers;

    protected JointCommand(EditableBridgeModel bridge) {
        super(bridge);
    }
    
    private static class JointIndexPair {
        public int lo, hi;
        
        public JointIndexPair(int a, int b) {
            if (a < b) {
                lo = a;
                hi = b;
            }
            else {
                lo = b;
                hi = a;
            }
        }
        
        public JointIndexPair(Member m) {
            this(m.getJointA().getIndex(), m.getJointB().getIndex());
        }
        
        public JointIndexPair(Joint a, Joint b) {
            this(a.getIndex(), b.getIndex());
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof JointIndexPair) {
                JointIndexPair p = (JointIndexPair)o;
                return p.lo == lo && p.hi == hi;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return lo * 199 + hi;
        }
    };
    
    /**
     * Add a list of existing members to delete and new ones to insert for the case where the joint
     * affected by this command transsects one or more members.  We need two parameters here
     * because the joint may be at the wrong (i.e. the pre-move) location when the splitting must be done.
     * 
     * @param pt point to use for member splitting
     * @param joint joint that links new members
     */
    protected void fixUpMembers(Affine.Point pt, Joint joint) {
        ArrayList<Member> toDelete = new ArrayList<Member>();
        ArrayList<Member> toInsert = new ArrayList<Member>();
        HashSet<JointIndexPair> connectedMemberJointPairs = new HashSet<JointIndexPair>();
        Iterator<Member> m = bridge.getMembers().iterator();
        while (m.hasNext()) {
            Member member = m.next();
            if (member.hasJoint(joint)) {
                connectedMemberJointPairs.add(new JointIndexPair(member));
            }
        }
        m = bridge.getMembers().iterator();
        while (m.hasNext()) {
            Member member = m.next();
            Joint a = member.getJointA();
            Joint b = member.getJointB();
            if (pt.onSegment(a.getPointWorld(), b.getPointWorld()) && 
                    !connectedMemberJointPairs.contains(new JointIndexPair(member))) {
                // If the splitting point lies on this member and the member is not connected to the joint we're moving.
                toDelete.add(member);
                if (!connectedMemberJointPairs.contains(new JointIndexPair(a, joint))) {
                    toInsert.add(new Member(member, a, joint));
                }
                if (!connectedMemberJointPairs.contains(new JointIndexPair(joint, b))) {
                    toInsert.add(new Member(member, joint, b));
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
    }

    /**
     * Add lo list of existing members to delete and new ones to insert for the case where the joint
     * joint affected by this command transsects one or more members.  We need two parameters here
     * because the joint may be at the wrong (i.e. the pre-move) location when the splitting must be done.
     * This is lo convenience function for the case where the splitting joint and point are at the same location.
     * 
     * @param joint joint to use for member splitting and to attach new members
     */
    protected void fixUpMembers(Joint joint) {
        fixUpMembers(joint.getPointWorld(), joint);
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

    /**
     * Execute as for the super class, except abort and return an appropriate error code if 
     * executing would add too many members.
     * 
     * @param undoManager undo manager to handle this command
     * @return error code EditableBridgeModel.ADD_MEMBER_AT_MAX if member spliting adds too many members.
     */
    @Override
    public int execute(ExtendedUndoManager undoManager) {
        if (bridge.getMembers().size() + insertMembers.length - deleteMembers.length > DesignConditions.maxMemberCount) {
            return EditableBridgeModel.ADD_MEMBER_AT_MAX;
        }
        return super.execute(undoManager);
    }
}
