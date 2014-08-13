/*
 * DesignIteration.java  
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

import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A bridge design captured as analysis is performed, so that the designer can later return to the captured state,
 * a kind of meta-undo facility.  Since each iteration except the first is a modification of a previous one, 
 * iterations naturally form a tree.  Consequently, we inherit from MutableTreeNode, which implements n-ary trees. 
 * In design iteration trees, there is a sentinel/dummy root node.  Each node's chidren are sequential iterations
 * following from it listed in order.  An iteration gets a child only when it is modified a second time to make
 * a (non-sequential) successor.  The dummy root's children are the initial sequence of iterations.
 * 
 * @author Eugene K. Ressler
 */
public class DesignIteration extends DefaultMutableTreeNode {
    private int number;
    private double cost;
    private String projectId;
    private byte [] bridgeModelAsBytes;
    private int analysisStatus;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

    /**
     * Construct a new design iteration.
     * 
     * @param number iteration number
     * @param cost iteration cost
     * @param projectId iteration project ID string
     * @param bridgeModelAsBytes bridge model as a byte string
     */
    public DesignIteration(int number, double cost, String projectId, byte[] bridgeModelAsBytes, int analysisStatus) {
        // Set mutable treenode user object to point here, 
        // which means our toString() will be used for rendering.
        super.setUserObject(this);
        initialize(number, cost, projectId, bridgeModelAsBytes, analysisStatus);
    }

    /**
     * Initialize the design iteration with given information.
     * 
     * @param number iteration number
     * @param cost cost of captured bridge
     * @param projectId project id for the bridge
     * @param bridgeModelAsBytes bridge as a byte array
     * @param analysisStatus status of bridge after analsysis. See values in EditableBridgeModel.
     */
    public final void initialize(int number, double cost, String projectId, byte[] bridgeModelAsBytes, int analysisStatus) {
        this.number = number;
        this.cost = cost;
        this.projectId = projectId;
        this.bridgeModelAsBytes = bridgeModelAsBytes;
        this.analysisStatus = analysisStatus;
    }

    /**
     * Set the status of the iteration. Used to update status after an existing
     * iteration is tested when initially it wasn't.  Should only be called
     * when analysisStatus is unknown.
     *
     * @param analysisStatus status of bridge after analsysis. See values in EditableBridgeModel.
     */
    public void setAnalysisStatus(int analysisStatus) {
        this.analysisStatus = analysisStatus;
    }
        
    /**
     * Empty iteration used merely to hold top sequence of iterations in its child vector.
     * Done this way to for ease of interface to default table model.
     */
    public DesignIteration() {
        super.setUserObject(null);
        this.number = -1;
        this.cost = 0;
        this.projectId = "<root>";
        this.bridgeModelAsBytes = null;
    }
    
    /**
     * Return the bridge captured in the iteration.
     * 
     * @return bridge as a byte array
     */
    public byte[] getBridgeModelAsBytes() {
        return bridgeModelAsBytes;
    }

    /**
     * Return cost of bridge captured in the iteration.
     * 
     * @return captured bridge cost
     */
    public double getCost() {
        return cost;
    }

    /**
     * Return the iteration number.
     * 
     * @return iteration number
     */
    public int getNumber() {
        return number;
    }

    /**
     * Return the project id for the captured bridge.
     * 
     * @return project id
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Return the analysis status of the bridge when it was captured  as
     * an iteration.  See wpbd.EditableBridgeModel for status constants.
     * 
     * @return analysis status
     */
    public int getBridgeStatus() {
        return analysisStatus;
    }

    /**
     * Return a string representation of the iteration, which can
     * be used by list and tree model renderers.
     * 
     * @return string representation of iteration
     */
    @Override
    public String toString() {
        String rtn = number + " - " + currencyFormatter.format(cost);
        if (projectId.trim().length() > 0) {
            rtn += " - " + projectId;
        }
        return rtn;
    }
}
