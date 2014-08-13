
package bridgedesigner;

/**
 * Undoable/redoable command to move labels to a new location.
 * 
 * @author Eugene K. Ressler
 */
public class MovelLabelsCommand extends EditCommand {

    private double positionOld, positionNew;
    private DraftingPanel.Labels labels;
    
    /**
     * Construct a command to move the informational labels on the drafting board to a new vertical location.
     * 
     * @param bridge bridge that holds the current label location
     * @param labels labels to move
     * @param positionNew new position of labels as a world y-coordinate
     */
    public MovelLabelsCommand(EditableBridgeModel bridge, DraftingPanel.Labels labels, double positionNew) {
        super(bridge);
        this.labels = labels;
        this.positionNew = positionNew;
        this.positionOld = bridge.getLabelPosition();
        presentationName = getString("moveLabels.text");
    }

    public void go() {
        bridge.setLabelPosition(positionNew);
        labels.initialize();
    }
    
    public void goBack() {
        bridge.setLabelPosition(positionOld);
        labels.initialize();        
    }
}
