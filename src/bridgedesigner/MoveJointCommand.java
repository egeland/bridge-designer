/*
 * MoveJointCommand.java  
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
 * Undoable/redoable command to move a joint to a new location.
 * 
 * @author Eugene K. Ressler
 */
public class MoveJointCommand extends JointCommand {

    protected Joint joint;
    private Affine.Point ptWorld;

    public MoveJointCommand(EditableBridgeModel bridge, Joint joint, Affine.Point ptWorld) {
        super(bridge);
        this.joint = new Joint(joint.getIndex(), ptWorld);
        this.ptWorld = new Affine.Point(ptWorld);
        fixUpMembers(ptWorld, joint);
        presentationName = String.format(getString("moveJoint.text"), joint.getNumber(), ptWorld.toString());
    }

    @Override
    public void go() {
        super.go();
        EditCommand.exchange(bridge.getJoints(), joint);
    }
    
    @Override
    public void goBack() {
        EditCommand.exchange(bridge.getJoints(), joint);           
        super.goBack();
    }
}
