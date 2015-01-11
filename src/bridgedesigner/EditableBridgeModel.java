/*
 * Joint.java  
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

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import org.jdesktop.application.ResourceMap;
import bridgedesigner.Affine.Point;

/**
 * A class for representing bridges that are fully editable with undo/redo.  Listeners for various kinds of
 * changes occurring to the bridge are supported.
 */
public class EditableBridgeModel extends BridgeModel {

    /**
     * Attempt to add a joint to the bridge was successful.
     */
    public static final int ADD_JOINT_OK = 0;
    /**
     * Attempt to add a joint to the bridge failed: there was already a joint in the desired location.
     */
    public static final int ADD_JOINT_JOINT_EXISTS = 2;
    /**
     * Attempt to add a joint to the bridge failed: the bridge is already at the maximum number of joints.
     */
    public static final int ADD_JOINT_AT_MAX = 3;
    /**
     * Attempt to move a joint in the bridge was successful.
     */
    public static final int MOVE_JOINT_OK = 0;
    /**
     * Attempt to move a joint in the bridge was unnecessary: it was of zero distance.
     */
    public static final int MOVE_JOINT_ALREADY_THERE = 1;
    /**
     * Attempt to move a joint in the bridge failed: another joint was already there.
     */
    public static final int MOVE_JOINT_JOINT_EXISTS = 2;
    /**
     * Attempt to move a joint in the bridge failed: the joint was moved onto a member, transsecting it.
     * The bridge already had the maximum number of members; therefore the transsected member could not be split.
     */
    public static final int MOVE_JOINT_MEMBER_AT_MAX = 3;    
    /**
     * Attempt to add a member to the bridge was successful.
     */
    public static final int ADD_MEMBER_OK = 0;
    /**
     * Attempt to add a member to the bridge failed: both specified joints are the same.
     */
    public static final int ADD_MEMBER_SAME_JOINT = 1;
    /**
     * Attempt to add a member to the bridge failed: a member is already there.
     */
    public static final int ADD_MEMBER_MEMBER_EXISTS = 2;
    /**
     * Attempt to add a member to the bridge failed: the bridge already has the maximum number of members.
     */
    public static final int ADD_MEMBER_AT_MAX = 3;
    /**
     * Attempt to add a member to the bridge failed: the bridge has a high pier, and the member would cross through it.
     */
    public static final int ADD_MEMBER_CROSSES_PIER = 4;
    /**
     * Status of analysis at the current edit point is indeterminate.
     */
    public static final int STATUS_WORKING = Analysis.NO_STATUS;
    /**
     * Status of analysis at the current edit point is passing.
     */
    public static final int STATUS_PASSES = Analysis.PASSES;
    /**
     * Status of analysis at the current edit point is failing.
     */
    public static final int STATUS_FAILS = Analysis.FAILS_LOAD_TEST;
    /**
     * Undo manager for this bridge.
     */
    protected final ExtendedUndoManager undoManager = new ExtendedUndoManager();
    /**
     * List of iterations saved for this bridge.
     */
    protected final ArrayList<DesignIteration> iterationList = new ArrayList<DesignIteration>();
    /**
     * "Dummy" iteration used only to contain the topmost row of the iteration tree.
     */
    protected final DesignIteration iterationTree = new DesignIteration();
    /**
     * Listeners for changes to the selected items in the bridge.
     */
    protected final ArrayList<ChangeListener> selectionChangeListeners = new ArrayList<ChangeListener>();
    /**
     * Listeners for changes to the structure of the bridge.
     */
    protected final ArrayList<ChangeListener> structureChangeListeners = new ArrayList<ChangeListener>();
    /**
     * Listeners for changes to the status of the analysis of the bridge.
     */
    protected final ArrayList<ChangeListener> analysisChangeListeners = new ArrayList<ChangeListener>();
    /**
     * Listeners for changes to the list and tree of design iterations captured from this bridge.
     */
    protected final ArrayList<ChangeListener> iterationChangeListeners = new ArrayList<ChangeListener>();
    /**
     * Shared current analysis of this bridge.
     */
    protected final Analysis analysis = new Analysis();
    /**
     * The most recently selected joint or member or null if nothing is selected. Invariants:
     * <ul>
     * <li>0 or 1 joints may be selected</li>
     * <li>0 or more members may be selected</li>
     * <li>When members are selected, no joint is selected.</li>
     * <li>When a joint is selected, no members are selected.</li>
     * </ul>
     * Consequently, <code>lastSelected</code> tells us a a great deal: 
     * <ul>
     * <li>Whether the selection is a joint, a non-empty set of members, or nothing.</li>
     * <li>If a joint is selected, what that joint is.</li>
     * </ul>
     * Enforcement of these invariants is spread throughout this class.
     */
    protected Editable lastSelected;
    /**
     * A mark retrieved from the undo manager indicating the bridge state when the 
     * last successful analysis was performed.  Note that the bridge does not have to
     * pass for the analysis to be successful.  Null means no analysis has yet succeeded.
     */
    protected Object analysisValidMark = null;
    /**
     * Index of the iteration currently loaded in the bridge, when the bridge has not been edited since loading.
     */
    protected int loadedIterationIndex = -1;
    /**
     * The last-loaded iteration that has now been edited.
     */
    protected int editedIterationIndex = -1;
    /**
     * Whether the loaded iteration is one that was created as a snapshot, i.e. a capture of the current
     * edited state created merely because a caller requested a list of iterations.
     */
    protected boolean loadedIterationIsSnapshot = false;
    
    /**
     * Construct a fresh editable bridge model.
     */
    public EditableBridgeModel() {
        this.undoManager.addUndoableAfterEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent e) {
                if (e.getEdit() instanceof EditCommand) {
                    editIteration();
                    fireStructureChange();
                }
            }
        });
    }

    /**
     * Return the current number of iterationList.
     * 
     * @return the iteration count
     */
    public int getIterationCount() {
        return iterationList.size();
    }

    /**
     * Return the index of the current loaded iteration.
     * 
     * @return the loaded iteration index
     */
    public int getCurrentIterationIndex() {
        return loadedIterationIndex;
    }
    
    /**
     * Return a design iteration given its index.
     * 
     * @param index index of the desired iteration
     * @return design iteration with given index
     */
    public DesignIteration getDesignIteration(int index) {
        return iterationList.get(index);
    }
    
    /**
     * Return the list index of the given design iteration.
     * 
     * @param iteration
     * @return list index of the iteration
     */
    public int getDesignIterationIndex(Object iteration) {
        return iterationList.indexOf(iteration);
    }
    
    /**
     * Return the root of the design iteration tree.
     * 
     * @return the root
     */
    public DesignIteration getDesignIterationTreeRoot() {
        return iterationTree;
    }
    
    /**
     * Clear all stored iteration.  Fires iteration change event.
     */
    public void clearIterations() {
        iterationList.clear();
        iterationTree.removeAllChildren();
        loadedIterationIndex = editedIterationIndex = -1;
        loadedIterationIsSnapshot = false;
        iterationNumber = 0;
        fireIterationChange();
    }

    /**
     * Record the the current bridge has been edited, and therefore it doesn't correspond to any stored iteration.
     * Fires an iteration change event.
     */
    public void editIteration() {
        if (loadedIterationIndex >= 0 && !loadedIterationIsSnapshot) {
            iterationNumber = iterationList.get(iterationList.size() - 1).getNumber() + 1;
            editedIterationIndex = loadedIterationIndex;
            loadedIterationIndex = -1;
            loadedIterationIsSnapshot = false;
            fireIterationChange();
        }
    }
    
    /**
     * Load a previously saved iteration.  Since this is like loading a completely new bridge, all
     * the same events are fired: iteration, structure, selection, and analysis change events.
     * 
     * @param index index of the design iteration to load
     */
    public void loadIteration(int index) {
        if (index != loadedIterationIndex) {
            try {
                parseBytes(iterationList.get(index).getBridgeModelAsBytes());
                // Preserve stored flag to because save file status isn't changed by iteration loading.
                undoManager.clear();
                loadedIterationIndex = index;
                editedIterationIndex = -1;
                loadedIterationIsSnapshot = false;
                lastSelected = null;
                fireIterationChange();
                fireStructureChange();
                fireSelectionChange();
                fireAnalysisChange();
            } catch (IOException ex) { }
        }
    }

    /**
     * Capture a fresh iteration from current bridge and add it to the model's
     * design iteration list and also the tree at the correct location.
     */
    private void setNewIteration() {
        DesignIteration iteration = new DesignIteration(iterationNumber, getTotalCost(), projectId, toBytes(), getAnalysisStatus());
        int currentIterationIndex = loadedIterationIndex >= 0 ? loadedIterationIndex : editedIterationIndex;
        if (currentIterationIndex >= 0) {
            DesignIteration current = iterationList.get(currentIterationIndex);
            DesignIteration parent = (DesignIteration) current.getParent();
            if (parent.getLastChild() == current) {
                parent.add(iteration);
            }
            else  {
                current.add(iteration);
            }
        }
        else {
            assert iterationTree.getChildCount() == 0;
            iterationTree.add(iteration);
        }
        iterationList.add(iteration);
    }
    
    private void resetCurrentIteration() {
        iterationList.get(iterationList.size() - 1).initialize(iterationNumber, getTotalCost(), projectId, toBytes(), analysis.getStatus());
    }

    private void updateCurrentIterationStatus() {
        iterationList.get(loadedIterationIndex).setAnalysisStatus(analysis.getStatus());
    }

    /**
     * Save the current bridge as an iteration.  
     */
    private void saveIteration() {
        // Either update the topmost iteration or insert a new one 
        // depending on whether iteration number has increased.
        if (iterationList.size() > 0 && iterationNumber == iterationList.get(iterationList.size() - 1).getNumber()) {
            resetCurrentIteration();
            editedIterationIndex = -1;
            loadedIterationIndex = iterationList.size() - 1;
        }
        else if (iterationList.isEmpty() || iterationNumber > iterationList.get(iterationList.size() - 1).getNumber()) {
            setNewIteration();
            editedIterationIndex = -1;
            loadedIterationIndex = iterationList.size() - 1;
        }
        else if (loadedIterationIndex >= 0) {
            updateCurrentIterationStatus();
        }
        loadedIterationIsSnapshot = false;
    }

    /**
     * A snapshot is a saved version of the current design with the current iteration number, but which we don't
     * want to become permanent when editing continues.
     */
    public void saveSnapshot() {
        // If a new snapshot would match the existing one (i.e. no editing has occurred), ignore this request.
        // Additionally, an old snapshot must be updated.
        if (loadedIterationIndex == -1 || loadedIterationIsSnapshot) {
            saveIteration();
            loadedIterationIsSnapshot = true;
            fireIterationChange();
        }
    }
    
    /**
     * Return true iff an iteration with index offset from the current one by + or - 1 can be loaded.
     * 
     * @param inc Either +1 or -1.
     * @return true iff the iteration with index +1 or -1 from the current one can be loaded.
     */
    public boolean canLoadNextIteration(int inc) {
        return getNextIterationIndex(inc) != -1;
    }
    
    /**
     * Return the index of the iteration offset +1 or -1 from the index of the current one or -1 if no such
     * iteration exists.
     * 
     * @param inc offset +1 or -1.
     * @return index of next or previous iteration
     */
    public int getNextIterationIndex(int inc) {
        int nIterations = getIterationCount();
        if (loadedIterationIndex == -1) {
            return (inc < 0 && nIterations > 0) ? 0 : -1;
        }
        int nextIterationIndex = loadedIterationIndex + inc;
        if (inc > 0) {
            return (nextIterationIndex < nIterations) ? nextIterationIndex : -1;
        }
        if (inc < 0) {
            return (nextIterationIndex >= 0) ? nextIterationIndex : -1;
        }
        return loadedIterationIndex;
    }

    /**
     * Return true iff it should be possible to increment or decrement with respect to the current iteration.
     * 
     * @return true iff it's possible to increment or decrement from current iteration
     */
    public boolean canGotoIteration() {
        return iterationList.size() > 1 || 
                (iterationList.size() == 1 && iterationNumber > iterationList.get(iterationList.size() - 1).getNumber());
    }

    /**
     * Return the undo manager for commands affecting the bridge.
     * 
     * @return The undo manager
     */
    public ExtendedUndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Analyze the current bridge.
     */
    public void analyze() {
        analysis.initialize(this);
        analysisValidMark = analysis.getStatus() > Analysis.UNSTABLE ? undoManager.getMark() : null;
        fireAnalysisChange();
        saveIteration();
        fireIterationChange();
    }
    
    /**
     * Return true iff necessary conditions for the bridge to be analyzed have been met.
     * 
     * @return true if a bridge analysis might be successful
     */
    public boolean isAnalyzable() {
        return members.size() >=  2 * joints.size() - designConditions.getNJointRestraints();
    }
    
    /**
     * Return true iff the current state of the current analsis (if any) matches the current state of the
     * bridge as determined by undo manager state.
     * 
     * @return true iff the analysis is good for the current bridge
     */
    public boolean isAnalysisValid() {
        return undoManager.isAtMark(analysisValidMark);
    }
  
    /**
     * Return true iff the analysis is valid and passing.
     * 
     * @return true iff the analysis is valid and passing.
     */
    public boolean isPassing() {
        return isAnalysisValid() && analysis.getStatus() == Analysis.PASSES;
    }

    /**
     * Return a flag indicating the three possible states of the analysis
     * taking the edit point into account. Sub-categories of failure can
     * be obtained from the analysis itself after it's determined that the
     * editor thinks it's failing.
     *
     * @return status of the bridge analysis
     */
    public int getAnalysisStatus() {
        if (!isAnalysisValid()) {
            return STATUS_WORKING;
        }
        return analysis.getStatus() == Analysis.PASSES ? STATUS_PASSES : STATUS_FAILS;
    }
    
    /**
     * Return the current analysis.
     * 
     * @return analyisis the current analysis
     */
    public Analysis getAnalysis() {
        return analysis;
    }
    
    /**
     * Add a listener that will be informed of any change in the selection of the bridge.
     * 
     * @param l the new listener to add
     */
    public void addSelectionChangeListener(ChangeListener l) {
        selectionChangeListeners.add(l);
    }

    /**
     * Remove a given selection change listener from the bridge.
     * 
     * @param l the listener to remove
     */
    public void removeSelectionChangeListener(ChangeListener l) {
        selectionChangeListeners.remove(l);
    }

    /**
     * Notify all listeners of a change to the selection of the bridge.
     */
    public void fireSelectionChange() {
        Iterator<ChangeListener> e = new ArrayList<ChangeListener>(selectionChangeListeners).iterator();
        while (e.hasNext()) {
            e.next().stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Add a listener that will be informed of any change in the structure of the bridge.
     * 
     * @param l the new listener
     */
    public void addStructureChangeListener(ChangeListener l) {
        structureChangeListeners.add(l);
    }

    /**
     * Remove a given structure change listener from the bridge.
     * 
     * @param l the listener to remove
     */
    public void removeStructureChangeListener(ChangeListener l) {
        structureChangeListeners.remove(l);
    }

    /**
     * Notify all listeners of a change to the structure of the bridge.
     */
    public void fireStructureChange() {	
        Iterator<ChangeListener> e = new ArrayList<ChangeListener>(structureChangeListeners).iterator();
        while (e.hasNext()) {
            e.next().stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Add a listener that will be informed of any change in the analysis of the bridge.
     * 
     * @param l the new listener
     */
    public void addAnalysisChangeListener(ChangeListener l) {
        analysisChangeListeners.add(l);
    }

    /**
     * Remove a given structure change listener from the bridge.
     * 
     * @param l the listener to remove
     */
    public void removeAnalysisChangeListener(ChangeListener l) {
        analysisChangeListeners.remove(l);
    }

    /**
     * Notify all listeners of a change to the structure of the bridge.
     */
    public void fireAnalysisChange() {
        Iterator<ChangeListener> e = new ArrayList<ChangeListener>(analysisChangeListeners).iterator();
        while (e.hasNext()) {
            e.next().stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Add a listener that will be informed of any change in the analysis of the bridge.
     * 
     * @param l the new listener
     */
    public void addIterationChangeListener(ChangeListener l) {
        iterationChangeListeners.add(l);
    }

    /**
     * Remove a given structure change listener from the bridge.
     * 
     * @param l the listener to remove
     */
    public void removeIterationChangeListener(ChangeListener l) {
        iterationChangeListeners.remove(l);
    }

    /**
     * Notify all listeners of a change to the structure of the bridge.
     */
    public void fireIterationChange() {
        Iterator<ChangeListener> e = new ArrayList<ChangeListener>(iterationChangeListeners).iterator();
        while (e.hasNext()) {
            e.next().stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Initialize the bridge with given conditions and title block information.  Initializes the base class
     * and then sets up the user interface.
     * 
     * @param conditions site/load conditions
     * @param projectId project identifier
     * @param designedBy name of designer
     */
    @Override
    public void initialize(DesignConditions conditions, String projectId, String designedBy) {
        super.initialize(conditions, projectId, designedBy);
        lastSelected = null;
        undoManager.newSession();
        clearIterations();
        saveIteration();
        fireStructureChange();
        fireIterationChange();
        fireSelectionChange();
        fireAnalysisChange();
    }

    /**
     * Add a new joint to the bridge at the given world coordinate point.
     *
     * @param ptWorld point location of the joint
     * @return instance of ADD_JOINT_ with add status.
     */
    public int addJoint(Affine.Point ptWorld) {
        if (findJointAt(ptWorld) != null) {
            return ADD_JOINT_JOINT_EXISTS;
        }
        if (joints.size() >= DesignConditions.maxJointCount) {
            return ADD_JOINT_AT_MAX;
        }
        if (new InsertJointCommand(this, new Joint(ptWorld)).execute(undoManager) != 0) {
            return ADD_MEMBER_AT_MAX;
        }
        return ADD_JOINT_OK;
    }

    /**
     * Add a new member or members to the bridge.  If any joints lie exactly on the member, we connect them 
     * with multiple members.  If a member already exists between any pair of joints where we want to
     * add a member, nothing is done.i
     * 
     * @param jointA first joint
     * @param jointB second joint
     * @param materialIndex index for the material
     * @param sectionIndex index of cross-section
     * @param sizeIndex index of the size
     * @return true iff a member was actually added
     */
    public int addMember(Joint jointA, Joint jointB, int materialIndex, int sectionIndex, int sizeIndex) {
        if (jointA == jointB) {
            return ADD_MEMBER_SAME_JOINT;
        }
        if (getMember(jointA, jointB) != null) {
            return ADD_MEMBER_MEMBER_EXISTS;
        }
        // Reject members that intersect a pier.  This works in concert with DraftingCoordinates, which prevents 
        // joints from ever occurring on top of a pier.
        if (designConditions.isHiPier()) {
            final Affine.Point pierLocation = designConditions.getPrescribedJointLocation(designConditions.getPierJointIndex());
            final double eps = 1e-6;
            Affine.Point a = jointA.getPointWorld();
            Affine.Point b = jointB.getPointWorld();
            if ((a.x < pierLocation.x && pierLocation.x < b.x) ||
                (b.x < pierLocation.x && pierLocation.x < a.x)) {
                double dx = b.x - a.x;
                if (Math.abs(dx) > eps) {
                    double y = (pierLocation.x - a.x) * (b.y - a.y) / dx + a.y;
                    if (y < pierLocation.y - eps) {
                        return ADD_MEMBER_CROSSES_PIER;
                    }
                }
            }
        }
        if (members.size() >= DesignConditions.maxMemberCount) {
            return ADD_MEMBER_AT_MAX;
        }
        Material material = inventory.getMaterial(materialIndex);
        Shape shape = inventory.getShape(sectionIndex, sizeIndex);
        Member member = new Member(jointA, jointB, material, shape);
        new InsertMemberCommand(this, member).execute(undoManager);
        return ADD_MEMBER_OK;
    }

    /**
     * Change the point location of the given joint.
     * 
     * @param joint joint to move
     * @param ptWorld joint's new location
     * @return true iff a move occurred; can fail if there is already a joint at the the location
     */
    public int moveJoint(Joint joint, Point ptWorld) {
        if (joint.getPointWorld().equals(ptWorld)) {
            return MOVE_JOINT_ALREADY_THERE;
        }
        Joint existing = findJointAt(ptWorld);
        if (existing != null && existing != joint) {
            return MOVE_JOINT_JOINT_EXISTS;
        }
        if (new MoveJointCommand(this, joint, ptWorld).execute(undoManager) == EditableBridgeModel.ADD_MEMBER_AT_MAX) {
            return MOVE_JOINT_MEMBER_AT_MAX;
        }
        return MOVE_JOINT_OK;
    }

    /**
     * A helper routine to find any joints lying on the member from joint a to b and
     * return them in order of distance from a.
     * 
     * @param rtn vector to hold results
     * @param a the first joint of the potentially transsected member
     * @param b the second joint of the potentially transsected member
     */
    public void getTranssectedJoints(ArrayList<Joint> rtn, Joint a, Joint b) {
        rtn.clear();
        Iterator<Joint> j = joints.iterator();
        while (j.hasNext()) {
            Joint joint = j.next();
            if (joint.getPointWorld().onSegment(a.getPointWorld(), b.getPointWorld())) {
                double distSq = a.getPointWorld().distanceSq(joint.getPointWorld());
                boolean didInsert = false;
                for (int i = 0; i < rtn.size(); i++) {
                    if (a.getPointWorld().distanceSq(rtn.get(i).getPointWorld()) > distSq) {
                        rtn.add(i, joint);
                        didInsert = true;
                        break;
                    }
                }
                if (!didInsert) {
                    rtn.add(joint);
                }
            }
        }
    }

    /**
     * Return the joint closest to ptWorld and within the given search pixelRadius.  Optionally
     * ignore fixed vertices.
     * 
     * @param ptWorld pick point
     * @param searchRadius pixelRadius from pick point to search
     * @param ignoreFixed ignore fixed joints if value is true
     * @return the closest joint to the given point satisfying the allow fixed criterion
     */
    public Joint findJoint(Affine.Point ptWorld, double searchRadius, boolean ignoreFixed) {
        final double radiusSquared = searchRadius * searchRadius;
        Joint closest = null;
        double closestDistanceSquared = 1e100;
        Iterator<Joint> e = joints.iterator();
        while (e.hasNext()) {
            Joint joint = e.next();
            if (ignoreFixed && joint.isFixed()) {
                continue;
            }
            double distanceSquared = joint.getPointWorld().distanceSq(ptWorld);
            if (distanceSquared <= radiusSquared && distanceSquared < closestDistanceSquared) {
                closest = joint;
                closestDistanceSquared = distanceSquared;
            }
        }
        return closest;
    }

    /**
     * Return the joint closest to ptWorld that is not connected to from.  The point must also be
     * within the given search pixelRadius from the pick point to qualify.
     * 
     * @param ptWorld pick point
     * @param from joint used to rule out search joints if they are connected by members
     * @param searchRadius pixelRadius from pick point to search
     * @return the closest joint to the given point not connected to from
     */
    public Joint findUnconnectedJoint(Affine.Point ptWorld, Joint from, double searchRadius) {
        final double radiusSquared = searchRadius * searchRadius;
        Joint closest = null;
        double closestDistanceSquared = 1e100;
        Iterator<Joint> e = joints.iterator();
        while (e.hasNext()) {
            Joint joint = e.next();
            if (joint != from) {
                double distanceSquared = joint.getPointWorld().distanceSq(ptWorld);
                if (distanceSquared <= radiusSquared &&
                        distanceSquared < closestDistanceSquared &&
                        getMember(joint, from) == null) {
                    closest = joint;
                    closestDistanceSquared = distanceSquared;
                }
            }
        }
        return closest;
    }

    /**
     * Return the member closest to ptWorld and within the given search pixelRadius.
     * 
     * @param ptWorld pick point
     * @return the closest member
     */
    public Member getMember(Affine.Point ptWorld, ViewportTransform viewportTransform) {
        Member closest = null;
        double closestDistance = 1e100;
        Iterator<Member> e = members.iterator();
        double jointRadius = viewportTransform.viewportToWorldDistance(Joint.pixelRadius);
        while (e.hasNext()) {
            Member member = e.next();
            double searchRadius = viewportTransform.viewportToWorldDistance(Math.max(3, Math.round(0.5f * member.getStrokeWidth())));
            double distance = member.pickDistanceTo(ptWorld, jointRadius);
            if (distance <= searchRadius && distance < closestDistance) {
                closest = member;
                closestDistance = distance;
            }
        }
        return closest;
    }
    
    private boolean addMissingDeckMembers() {
        int nPanels = designConditions.getNPanels();
        int nMissing = 0;
        for (int i = 0; i < nPanels; i++) {
            if (getMember(joints.get(i), joints.get(i + 1)) == null) {
                ++nMissing;
            }
        }
        if (nMissing == 0) {
            return false;
        }
        StockSelector.Descriptor descriptor = getMostCommonStock();
        Member [] deckMembers = new Member [nMissing];
        int im = 0;
        for (int i = 0; i < nPanels; i++) {
            if (getMember(joints.get(i), joints.get(i + 1)) == null) {
                deckMembers[im++] = new Member(joints.get(i), joints.get(i + 1), 
                        inventory.getMaterial(descriptor.materialIndex),
                        inventory.getShape(descriptor.sectionIndex, descriptor.sizeIndex));
            }           
        }
        new InsertMemberCommand(this, deckMembers).execute(undoManager);
        return true;
    }
    
    /**
     * Apply heuristics to attempt to make a bridge determinate.
     * 
     * @return true iff the autofix made any changes
     */
    public boolean autofix() {
        return addMissingDeckMembers();
    }
    
    /**
     * Return the member from joint a to joint b
     * 
     * @param jointA the first joint
     * @param jointB the second joint
     * @return the member between the two joints, if there is one; null otherwise
     */
    public Member getMember(Joint jointA, Joint jointB) {
        Iterator<Member> e = members.iterator();
        while (e.hasNext()) {
            Member member = e.next();
            if (member.hasJoints(jointA, jointB)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Return an array of members connected to the given joint.
     * 
     * @param joint the joint to check
     * @return array of members connected on either end to joint
     */
    public Member[] findMembersWithJoint(Joint joint) {
        ArrayList<Member> v = new ArrayList<Member>();
        Iterator<Member> e = members.iterator();
        while (e.hasNext()) {
            Member member = e.next();
            if (member.hasJoint(joint)) {
                v.add(member);
            }
        }
        return v.toArray(new Member[v.size()]);
    }

    /**
     * Increment the size of all selected members by the given amount, which may be negative (normally +1 or -1). 
     * 
     * @param increment
     */
    public void incrementMemberSize(int increment) {
        if (lastSelected instanceof Member) {
            new ChangeMembersCommand(this, increment).execute(undoManager);
        }
    }

    /**
     * Select a given element (joint or member) of the bridge, possibly clearing the previous selection.  This
     * fires a selection change event to all listeners if anything about the selection actually changes.
     * 
     * @param element the element to select
     * @param extendSelection whether to extend any existing selection or clear it
     * @return true iff something that was formerly unselected became selected (possibly after clearing)
     */
    public boolean select(Editable element, boolean extendSelection) {
        if (element == null) {
            // Select nothing.  Cancel selection unless extending. 
            if (!extendSelection) {
                clearSelection(true);
            }
            return false;
        }
        if (element instanceof Member && extendSelection) {
            clearSelectedJoint(false);
            element.setSelected(!element.isSelected());
        } else {
            // In all other cases, clear other selections and then select this one.
            clearSelection(false);
            element.setSelected(true);
        }
        lastSelected = element;
        fireSelectionChange();
        return true;
    }

    /**
     * Clear the current selection if it was a joint.  Do nothing otherwise.
     * 
     * @param postChange whether to fire selection change events
     * @return true iff a joint selection was actually cleard
     */
    public boolean clearSelectedJoint(boolean postChange) {
        if (lastSelected instanceof Joint) {
            lastSelected.setSelected(false);
            lastSelected = null;
            if (postChange) {
                fireSelectionChange();
            }
            return true;
        }
        return false;
    }

    /**
     * Clear the current selection if it was a non-null set of members.  Do nothing otherwise.
     * 
     * @param postChange whether to fire selection change events
     * @return true iff a member selection was actually cleared
     */
    protected boolean clearSelectedMembers(boolean postChange) {
        boolean change = false;
        if (lastSelected instanceof Member) {
            Iterator<Member> me = members.iterator();
            while (me.hasNext()) {
                if (me.next().setSelected(false)) {
                    change = true;
                }
            }
            lastSelected = null;
        }
        if (change && postChange) {
            fireSelectionChange();
        }
        return change;
    }

    /**
     * Clear the selection entirely no matter what.  This optionally fires a selection change event.
     * 
     * @param postChange whether to fire selection change events
     * @return true iff a selection was actually cleared
     */
    protected boolean clearSelection(boolean postChange) {
        return clearSelectedJoint(postChange) || clearSelectedMembers(postChange);
    }

    /**
     * Take the correct action with the selection when beginning a rubberband rectangle.  THis consists 
     * of clearing the selected joint, if there was one.  If not (and therefore have a [possibly null] member
     * selection), then just clear the entire selection unless we are adding members.  In the last case
     * we do nothing.
     * 
     * This fires selection change events if needed.
     * 
     * @param extendSelection whether to retain the current selection and possibly add to it or clear it 
     */
    public void beginAreaSelection(boolean extendSelection) {
        if (!clearSelectedJoint(true) && !extendSelection) {
            clearSelection(true);
        }
    }

    /**
     * Select all the members in the given rectangular area.  This fires a selection change event if necessary.
     * 
     * @param rectangleCursorWorld area to search for members
     * @return true iff a change to the selection actually occurred
     */
    public boolean selectMembers(Rectangle.Double rectangleCursorWorld) {
        boolean change = false;
        Iterator<Member> me = members.iterator();
        while (me.hasNext()) {
            Member member = me.next();
            if (rectangleCursorWorld.contains(member.getJointA().getPointWorld()) &&
                    rectangleCursorWorld.contains(member.getJointB().getPointWorld())) {
                if (selectMember(member, true)) {
                    change = true;
                }
            }
        }
        if (change) {
            fireSelectionChange();
        }
        return change;
    }

    /**
     * Select a set of rows based on a range selected in the member table.  Since the member table is sorted
     * on arbitrary keys, the range may not be of contiguous member numbers.  The member table itself provides
     * the index translation.
     * 
     * @param memberTable table 
     * @param firstIndex first member table position to select
     * @param lastIndex last member table position to select
     * @return true iff the selection actually changed
     */
    public boolean selectMembers(MemberTable memberTable, int firstIndex, int lastIndex) {
        ListSelectionModel selectionModel = memberTable.getSelectionModel();
        // Assume at least one member is going to change, so deselect any joint.
        boolean change = clearSelectedJoint(false);
        for (int i = firstIndex; i <= lastIndex && i < members.size(); i++) {
            Member member = members.get(memberTable.convertRowIndexToModel(i));
            if (selectMember(member, selectionModel.isSelectedIndex(i))) {
                change = true;
            }
        }
        if (change) {
            fireSelectionChange();
        }
        return change;
    }

    /**
     * Select all members in the bridge.  This fires a selection change event.
     */
    public void selectAllMembers() {
        boolean change = false;
        if (lastSelected instanceof Joint) {
            clearSelection(false);
        }
        Iterator<Member> me = members.iterator();
        while (me.hasNext()) {
            Member member = me.next();
            if (selectMember(member, true)) {
                change = true;
            }
        }
        if (change) {
            fireSelectionChange();
        }
    }

    /**
     * Return true iff there is at least one item selected.
     * 
     * @return true iff there is a selected joint or member
     */
    public boolean isSelection() {
        return lastSelected != null;
    }
    
    /**
     * Return true iff the current selection is 1 or more members.
     * 
     * @return true iff a member is selected
     */
    public boolean isSelectedMember() {
        return lastSelected instanceof Member;
    }
    
    /**
     * Return a bit mask indicating whether it's okay to increase and/or decrease the size of a member shape.
     * 
     * @return mask
     */
    public int getAllowedShapeChanges() {
        int mask = 0;
        Iterator<Member> me = members.iterator();
        while (me.hasNext()) {
            Member member = me.next();
            if (member.isSelected()) {
                mask |= inventory.getAllowedShapeChanges(member.getShape());
            }
        }
        return mask;
    }
    
    /**
     * Return the currently selected joint if there is one.
     * 
     * @return the selected joint or null if no joint is selected
     */
    public Joint getSelectedJoint() {
        return (lastSelected instanceof Joint) ? (Joint) lastSelected : null;
    }

    /**
     * Select or unselect the given member.
     * 
     * @param member the member to select
     * @param select true to select, false to de-select
     * @return true iff a change to the selection actually occurred
     */
    public boolean selectMember(Member member, boolean select) {
        if (member.setSelected(select)) {
            lastSelected = member;
            return true;
        }
        return false;
    }

    /**
     * Return an array of all the selected members.
     * 
     * @return the array of selected members
     */
    public Member[] getSelectedMembers() {
        ArrayList<Member> memberList = new ArrayList<Member>();
        Iterator<Member> me = members.iterator();
        while (me.hasNext()) {
            Member member = me.next();
            if (member.isSelected()) {
                memberList.add(member);
            }
        }
        return memberList.toArray(new Member[memberList.size()]);
    }

    /**
     * Build a set of joints that are only connected to
     * Selected members. Also a set of joints connected to
     * any member at all.
     * 
     * @return a list of joints that would be orphaned by the deletion of the selected members
     */
    public Joint[] getJointsToDeleteWithSelectedMembers() {
        Set<Joint> jointsTouchingUnselectedMembers = new HashSet<Joint>();
        Set<Joint> jointsTouchingAnyMember = new HashSet<Joint>();
        Iterator<Member> me = members.iterator();
        while (me.hasNext()) {
            Member member = me.next();
            jointsTouchingAnyMember.add(member.getJointA());
            jointsTouchingAnyMember.add(member.getJointB());
            if (!member.isSelected()) {
                jointsTouchingUnselectedMembers.add(member.getJointA());
                jointsTouchingUnselectedMembers.add(member.getJointB());
            }
        }
        ArrayList<Joint> jointList = new ArrayList<Joint>();
        Iterator<Joint> je = joints.iterator();
        while (je.hasNext()) {
            Joint joint = je.next();
            if (!joint.isFixed() &&
                    jointsTouchingAnyMember.contains(joint) &&
                    !jointsTouchingUnselectedMembers.contains(joint)) {
                jointList.add(joint);
            }
        }
        return jointList.toArray(new Joint[jointList.size()]);
    }

    /**
     * Delete whatever is selected at the moment, either a joint or a set of members.
     */
    public void deleteSelection() {
        if (lastSelected instanceof Joint) {
            new DeleteJointCommand(this).execute(undoManager);
        } else if (lastSelected instanceof Member) {
            new DeleteMembersCommand(this).execute(undoManager);
        }
        lastSelected = null;
    }

    /**
     * Change the stock of the selected members to the given descriptors.
     * 
     * @param materialIndex index of the material of the members' new stock
     * @param sectionIndex index of the section (tube or bar) of the members' new stock
     * @param sizeIndex index of the size of the members' new stock
     */
    void changeSelectedMembers(int materialIndex, int sectionIndex, int sizeIndex) {
        if (lastSelected instanceof Member) {
            new ChangeMembersCommand(this, materialIndex, sectionIndex, sizeIndex).execute(undoManager);
        }
    }

    /**
     * Delete the given element, which may be either a joint or a member.  If the element is null, do nothing.
     * 
     * @param element the element to delete
     */
    public void delete(Editable element) {
        if (element instanceof Joint) {
            new DeleteJointCommand(this, (Joint) element).execute(undoManager);
        } else if (element instanceof Member) {
            new DeleteMembersCommand(this, (Member) element).execute(undoManager);
        }
    }
    
    /**
     * Get a descriptor for the stock of the selected members or return null if no
     * member is selected.  If the material, section, or size respectively differs among
     * selected members, then the corresponding selector index is set to -1.  Otherwise
     * it's set to the proper index from the bridge model inventory.  (DEBUG: Logically,
     * this belongs in the Editable model, since the read-only model doesn't touch
     * selections anywhere else.)
     * 
     * @return stock descriptor for the stock of the selected members
     */
    public StockSelector.Descriptor getSelectedStock() {
        Iterator<Member> e = members.iterator();
        StockSelector.Descriptor descriptor = null;
        while (e.hasNext()) {
            Member member = e.next();
            if (member.isSelected()) {
                if (descriptor == null) {
                    descriptor = new StockSelector.Descriptor(member);
                }
                else {
                    if (descriptor.materialIndex != member.getMaterial().getIndex()) {
                        descriptor.materialIndex = -1;
                    }
                    if (descriptor.sectionIndex != member.getShape().getSection().getIndex()) {
                        descriptor.sectionIndex = -1;
                    }
                    if (descriptor.sizeIndex != member.getShape().getSizeIndex()) {
                        descriptor.sizeIndex = -1;
                    }
                }
            }
        }
        return descriptor;
    }
    
    public int getSnapMultiple() {
        int rtn = DraftingGrid.maxSnapMultiple;
        Iterator<Joint> e = joints.iterator();
        while (e.hasNext()) {
            Affine.Point loc = e.next().getPointWorld();
            rtn = Math.min(rtn, Math.min(DraftingGrid.snapMultipleOf(loc.x), DraftingGrid.snapMultipleOf(loc.y)));
        }
        return rtn;
    }

    /**
     * Read a bridge represented a string (used for sample bridges) into this model.
     * 
     * @param s bridge represented as a string
     */
    @Override
    public void read(String s) {
        clearIterations();
        super.read(s);
        // Reset the session and ensure user will be asked for file name on save by resetting store flag.
        undoManager.newSession();
        lastSelected = null;
        saveIteration();
        fireIterationChange();
        fireStructureChange();
        fireSelectionChange();
        fireAnalysisChange();
    }

    @Override
    public void read(File f) throws IOException {
        clearIterations();
        super.read(f);
        undoManager.load();
        lastSelected = null;
        saveIteration();
        fireIterationChange();
        fireStructureChange();
        fireSelectionChange();
        fireAnalysisChange();
    }

    @Override
    public void write(File f) throws IOException {
        super.write(f);
        undoManager.save();
    }

    /**
     * Return a string representation of load test results as tab delimited text.
     * Should only be called if the analysis is valid.  Results paste nicely into Excel.
     * 
     * @return tab delimited text
     */
    public String toTabDelimitedText() {
        ResourceMap resourceMap = BDApp.getResourceMap(EditableBridgeModel.class);
        if (analysis.getStatus() <= Analysis.UNSTABLE) {
            return resourceMap.getString("invalid.text");
        }
        StringBuilder str = new StringBuilder();
        Formatter formatter = new Formatter(str, Locale.US);

        str.append(projectName);
        str.append('\n');

        str.append(resourceMap.getString("projectId.text"));
        str.append(projectId);
        str.append('\n');

        str.append(resourceMap.getString("designedBy.text"));
        str.append(designedBy);
        str.append('\n');

        str.append(resourceMap.getString("cvsHeaders.text"));
        str.append('\n');
        Iterator<Member> em = members.iterator();
        while (em.hasNext()) {
            Member member = em.next();
            int i = member.getIndex();
            formatter.format("%d\t%s\t%s\t%s\t%.2f\t%.2f\t%.2f\t%s\t%.2f\t%.2f\t%s\n", 
                    member.getNumber(), member.getMaterial().getShortName(), 
                    member.getShape().getSection().getName(), member.getShape().getName(), 
                    member.getLength(), analysis.getMemberCompressiveForce(i), analysis.getMemberCompressiveStrength(i), 
                    MemberTable.getMemberStatusString(member.getCompressionForceStrengthRatio() <= 1), 
                    analysis.getMemberTensileForce(i), analysis.getMemberTensileStrength(i), 
                    MemberTable.getMemberStatusString(member.getTensionForceStrengthRatio() <= 1));
        }
        return str.toString();
    }

    private static final String okHTML   = "<td bgcolor=\"#C0FFC0\" align=\"center\">OK</td>";
    private static final String failHTML = "<td bgcolor=\"#FFC0C0\" align=\"center\">Fail</td>";

    private static final String rowHTML =
        "<tr>\n" +
        " <td align=\"right\">%d</td>\n" +
        " <td align=\"right\">%s</td>\n" +
        " <td align=\"center\">%s</td>\n" +
        " <td align=\"center\">%s</td>\n" +
        " <td align=\"right\">%.2f</td>\n" +
        " <td>&nbsp;</td>\n" +
        " <td align=\"right\">%.1f</td>\n" +
        " <td align=\"right\">%.1f</td>\n" +
        " <td align=\"right\">%.4f</td>\n" +
        " %s\n" +
        " <td>&nbsp;</td>\n" +
        " <td align=\"right\">%.1f</td>\n" +
        " <td align=\"right\">%.1f</td>\n" +
        " <td align=\"right\">%.4f</td>\n" +
        " %s\n" +
        "</tr>\n";

    private static final String slendernessFailureRowHTML =
        "<tr>\n" +
        " <td align=\"right\">%d</td>\n" +
        " <td align=\"right\">%s</td>\n" +
        " <td align=\"center\">%s</td>\n" +
        " <td align=\"center\">%s</td>\n" +
        " <td align=\"right\">%.2f</td>\n" +
        " <td>&nbsp;</td>\n" +
        " <td align=\"right\">%.1f</td>\n" +
        " <td align=\"right\">0.0</td>\n" +
        " <td align=\"right\">&nbsp;oo</td>\n" +
        " <td bgcolor=\"#FF60FF\" align=\"center\">Fail</td>\n" +
        " <td>&nbsp;</td>\n" +
        " <td align=\"right\">%.1f</td>\n" +
        " <td align=\"right\">0.0</td>\n" +
        " <td align=\"right\">&nbsp;oo</td>\n" +
        " <td bgcolor=\"#FF60FF\" align=\"center\">Fail</td>\n" +
        "</tr>\n";

    private static final String headHTML =
        "<html><head><title>Load Test Results Report</title>\n" +
        "<style>\n" +
        "  table { font-size : 8pt; font-family: arial, helvetica, sans-serif }\n" +
        "  th { font-weight : bold }\n" +
        "</style></head><body>\n" +
        "<table border=1 cellspacing=0 cellpadding=2>\n" +
        "<tr><th colspan=15>Load Test Results Report (Design Iteration #%d, Scenario id %d, number %s, Cost $%.2f)</th></tr>\n" +
        "<tr>\n" +
        " <th colspan=5>Member</th><th>&nbsp;</th><th colspan=4>Compression</th><th>&nbsp;</th><th colspan=4>Tension</th></tr>\n" +
        "<tr>\n" +
        " <th>#</th><th>Size</th><th>Section</th><th>Matl.</th><th>Length<br>(m)</th>\n" +
        " <th>&nbsp;</th>\n" +
        " <th>Force<br>(kN)</th><th>Strength<br>(kN)</th><th>Force/<br>Strength</th><th>Status</th>\n" +
        " <th>&nbsp;</th>\n" +
        " <th>Force<br>(kN)</th><th>Strength<br>(kN)</th><th>Force/<br>Strength</th><th>Status</th>\n" +
        "</tr>\n";

    private static final String tailHTLM =
        "</table>\n" +
        "</body></html>";

    public String toHTML() {
        StringBuilder str = new StringBuilder();
        str.append(String.format(headHTML,
                iterationNumber,
                designConditions.getCodeLong(),
                designConditions.getTag(),
                analysis.getStatus() == Analysis.PASSES ? getTotalCost() : 0.0));
        Iterator<Member> mi = members.iterator();
        while (mi.hasNext()) {
            Member m = mi.next();
            int i = m.getIndex();
            double cr = m.getCompressionForceStrengthRatio();
            double tr = m.getTensionForceStrengthRatio();
            double s = m.getSlenderness();
            if (s > designConditions.getAllowableSlenderness()) {
                str.append(String.format(slendernessFailureRowHTML,
                        m.getNumber(),
                        m.getShape().getName(),
                        m.getShape().getSection().getShortName(),
                        m.getMaterial().getShortName(),
                        m.getLength(),
                        analysis.getMemberCompressiveForce(i),
                        analysis.getMemberTensileForce(i)));            }
            else {
                str.append(String.format(rowHTML,
                        m.getNumber(),
                        m.getShape().getName(),
                        m.getShape().getSection().getShortName(),
                        m.getMaterial().getShortName(),
                        m.getLength(),
                        analysis.getMemberCompressiveForce(i),
                        analysis.getMemberCompressiveStrength(i),
                        cr,
                        cr > 1 ? failHTML : okHTML,
                        analysis.getMemberTensileForce(i),
                        analysis.getMemberTensileStrength(i),
                        tr,
                        tr > 1 ? failHTML : okHTML));
            }
        }
        str.append(tailHTLM);
        return str.toString();
    }
    /*
    <html><head><title>Load Test Results Report</title>
    <style>
      table { font-size : 8pt; font-family: arial, helvetica, sans-serif }
      th { font-weight : bold }
    </style></head><body>
    <table border=1 cellspacing=0 cellpadding=2>
    <tr><th colspan=15>Load Test Results Report (Design Iteration #428, Scenario id 1052804100, number 42A, Cost $192742.14)</th></tr>
    <tr>
     <th colspan=5>Member</th><th>&nbsp;</th><th colspan=4>Compression</th><th>&nbsp;</th><th colspan=4>Tension</th></tr>
    <tr>
     <th>#</th><th>Size</th><th>Section</th><th>Matl.</th><th>Length<br>(m)</th>
     <th>&nbsp;</th>
     <th>Force<br>(kN)</th><th>Strength<br>(kN)</th><th>Force/<br>Strength</th><th>Status</th>
     <th>&nbsp;</th>
     <th>Force<br>(kN)</th><th>Strength<br>(kN)</th><th>Force/<br>Strength</th><th>Status</th>
    </tr>
    <tr>
     <td align="right">27</td>
     <td align="right">60x60</td>
     <td align="center">Bar</td>
     <td align="center">CS</td>
     <td align="right">3.18</td>
     <td>&nbsp;</td>
     <td align="right">0.0</td>
     <td align="right">166.8</td>
     <td align="right">0.0000</td>
     <td bgcolor="#C0FFC0" align="center">OK</td>
     <td>&nbsp;</td>
     <td align="right">233.0</td>
     <td align="right">855.0</td>
     <td align="right">0.2725</td>
     <td bgcolor="#C0FFC0" align="center">OK</td>
    </tr>
    </table>
    </body></html>
     *
>  <td align="right">0.0</td>
>  <td align="right">&nbsp;oo</td>
     *
     */
    /**
     * This is to generate a specially formatted table for testing
     * the C code in the Judge.  It's bare data.
     * 
     * @return 
     */
    public String toText() {
        StringBuilder str = new StringBuilder();
        str.append(iterationNumber);
        str.append('\t');
        str.append(designConditions.getCodeLong());
        str.append('\t');
        str.append(designConditions.getTag());
        str.append('\t');
        str.append(analysis.getStatus() == Analysis.PASSES ? getTotalCost() : 0.0);
        str.append('\n');
        Iterator<Member> mi = members.iterator();
        while (mi.hasNext()) {
            Member m = mi.next();
            int i = m.getIndex();
            double cr = m.getCompressionForceStrengthRatio();
            double tr = m.getTensionForceStrengthRatio();
            double s = m.getSlenderness();
            str.append(m.getNumber());
            str.append('\t');
            str.append(m.getShape().getName());
            str.append('\t');
            str.append(m.getShape().getSection().getShortName());
            str.append('\t');
            str.append(m.getMaterial().getShortName());
            str.append('\t');
            str.append(m.getLength());
            str.append('\t');
            str.append(analysis.getMemberCompressiveForce(i));
            str.append('\t');
            str.append(analysis.getMemberCompressiveStrength(i));
            str.append('\t');
            str.append(cr);
            str.append('\t');
            str.append(s > designConditions.getAllowableSlenderness() ? "Slenderness" : cr > 1 ? "Fail" : "OK");
            str.append('\t');
            str.append(analysis.getMemberTensileForce(i));
            str.append('\t');
            str.append(analysis.getMemberTensileStrength(i));
            str.append('\t');
            str.append(tr);
            str.append('\t');
            str.append(s > designConditions.getAllowableSlenderness() ? "Slenderness" : tr > 1 ? "Fail" : "OK");
            str.append('\n');
        }
        return str.toString();
    }

    public static void printTestTables () {
        final String year = Integer.toString(version);
        File egDir = new File("../bridgecontest/vendor/gems/WPBDC/test/eg/" + year);
        File [] files = egDir.listFiles();
        EditableBridgeModel bridge = new EditableBridgeModel();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile()) {
                    continue;
                }
                /* Single file debugging:
                if (! "A-arch-10-4-1.bdc".equals(files[i].getName())) {
                    continue;
                }
                */
                try {
                    System.err.println(files[i] + ":");
                    bridge.read(files[i]);
                    bridge.analyze();
                    String fullName = files[i].getName();
                    String baseName = fullName.substring(0, fullName.lastIndexOf('.'));
                    BufferedWriter out = new BufferedWriter(new FileWriter("eg/"+ year + "/html/" + baseName + ".htm"));
                    out.write(bridge.toHTML());
                    out.close();
                    out = new BufferedWriter(new FileWriter("eg/"+ year + "/log/" + baseName + ".txt"));
                    out.write(bridge.toText());
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(EditableBridgeModel.class.getName()).log(Level.SEVERE, "Failed to open example.", ex);
                }
            }
        }
    }
}
