/*
 * InsertJointCommand.java  
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
 * Undoable/redoable command to insert a new joint in a bridge.
 * 
 * @author Eugene K. Ressler
 */
public class InsertJointCommand extends JointCommand {

    /**
     * Joint to insert stored in an array of one element.
     */
    protected Joint[] joints;
    
    /**
     * Construct a command to insert the given joint in the given bridge.  It is assumed that the
     * joint's index has already been set to the desired location.
     * 
     * @param bridge bridge where the joint should be inserted
     * @param joint joint to insert
     */
    public InsertJointCommand(EditableBridgeModel bridge, Joint joint) {
        super(bridge);
        joint.setIndex(bridge.getJoints().size());
        joints = new Joint[] { joint };
        fixUpMembers(joint);
        presentationName = getString("insertJoint.text") + " " + joints[0].getPointWorld() + ".";
    }
    
    /**
     * Go forward with the joint insertion.
     */
    @Override
    public void go() {
        EditCommand.insert(bridge.getJoints(), joints);
        // super takes care of member splitting
        super.go();
    }
    
    /**
     * Go back to the state prior to joint insertion.
     */
    @Override
    public void goBack() {
        // super takes care of member unsplitting
        super.goBack();
        EditCommand.delete(bridge.getJoints(), joints);        
    }    
}
