/*
 * DeleteJointCommand.java  
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
 * Undoable/redoable command do delete a joint and associated members.
 * 
 * @author Eugene K. Ressler
 */
public class DeleteJointCommand extends EditCommand {

    /**
     * Joint to delete in an array of one element.
     */
    protected Joint[] joints;
    /**
     * Members to delete.
     */
    protected Member[] members;

    /**
     * Construct a command to delete one specified joint.
     * 
     * @param bridge bridge containing the joint
     * @param joint joint to delete
     */
    public DeleteJointCommand(EditableBridgeModel bridge, Joint joint) {
        super(bridge);
        members = bridge.findMembersWithJoint(joint);
        joints = new Joint[]{ joint };
        presentationName = getString("deleteJoint.text") + " " + joints[0].getPointWorld() + ".";
    }

    /**
     * Construct a command to delete the selected joint.  It's an error to do this 
     * if no joint is selected.
     * 
     * @param bridge bridge containing the selected joint
     */
    public DeleteJointCommand(EditableBridgeModel bridge) {
        this(bridge, bridge.getSelectedJoint());
    }

    @Override
    public void go() {
        joints[0].setSelected(false);
        EditCommand.delete(bridge.getJoints(), joints);
        EditCommand.delete(bridge.getMembers(), members);
    }

    @Override
    void goBack() {
        EditCommand.insert(bridge.getJoints(), joints);        
        EditCommand.insert(bridge.getMembers(), members);        
    }
}
