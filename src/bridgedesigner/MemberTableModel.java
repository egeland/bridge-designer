package bridgedesigner;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

class MemberTableModel extends AbstractTableModel {

    private EditableBridgeModel bridge;
    
    public MemberTableModel(EditableBridgeModel bridge) {
        super();
        this.bridge = bridge;
    }

    public ArrayList<Member> getMembers() {
        return bridge.getMembers();
    }
    
    public Inventory getInventory() {
        return bridge.getInventory();
    }
    
    /**
     * Load the current member selection into the table.  We take some care to do this in a
     * way that will generate least memory churn for the row selection model.  Note that listeners
     * are notified of programmatic selections.  See Dispatcher, where we break the feedback loops 
     * this causes.
     */
    public void loadSelection(MemberTable memberTable) {
        ArrayList<Member> members = bridge.getMembers();
        // Find the initial selected interval and use "set" to send it to the table.
        int i0 = 0;
        while (true) {
            if (i0 == members.size()) {
                memberTable.clearSelection();
                return;
            }
            if (members.get(memberTable.convertRowIndexToModel(i0)).isSelected()) {
                break;
            }
            i0++;
        }
        int i1 = i0 + 1;
        while (i1 < members.size() && members.get(memberTable.convertRowIndexToModel(i1)).isSelected()) {
            i1++;
        }
        memberTable.setRowSelectionInterval(i0, i1 - 1);

        // Find successive selected intervals and send them with "add."
        while (true) {
            i0 = i1;
            for (;;) {
                if (i0 == members.size()) {
                    return;
                }
                if (members.get(memberTable.convertRowIndexToModel(i0)).isSelected()) {
                    break;
                }
                i0++;
            }
            i1 = i0 + 1;
            while (i1 < members.size() && members.get(memberTable.convertRowIndexToModel(i1)).isSelected()) {
                i1++;
            }
            memberTable.addRowSelectionInterval(i0, i1 - 1);
        }
    }
    /*
     * Column names used to retrieve resources for column headers, widths, etc.
     */
    public static final String[] columnNames = new String[]{
        "number",
        "material",
        "crossSection",
        "size",
        "length",
        "SPACER", // spacer
        "slenderness",
        "compression",
        "tension"
    };

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    private static final Class[] columnClasses = new Class[]{
        java.lang.Integer.class,
        java.lang.String.class,
        java.lang.String.class,
        java.lang.Integer.class,
        java.lang.Double.class,
        java.lang.String.class, // spacer
        java.lang.Double.class,
        java.lang.Double.class,
        java.lang.Double.class
    };
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    public int getRowCount() {
        return bridge.getMembers().size();
    }

    public int getColumnCount() {
        return columnClasses.length;
    }

    private final NumberFormat doubleFormatter = new DecimalFormat("0.00");

    public Object getValueAt(int rowIndex, int columnIndex) {
        Member member = bridge.getMembers().get(rowIndex);
        switch (columnIndex) {
            case 0:
                // number
                return member.getNumber();
            case 1:
                // material type
                return member.getMaterial().getShortName();
            case 2:
                // cross section
                return member.getShape().getSection().getShortName();
            case 3:
                // size
                return member.getShape().getNominalWidth();
            case 4:
                // getLength
                return member.getLength();
            case 5:
                return "";
            case 6:
                return member.getSlenderness();
            case 7:
                return member.getCompressionForceStrengthRatio();
            case 8:
                return member.getTensionForceStrengthRatio();
        }
        return null;
    }
    
    public Member getMember(int i) {
        return bridge.getMembers().get(i);
    }
    
    public boolean isAnalysisValid() {
        return bridge.isAnalysisValid();
    }
    
    public double getAllowableSlenderness() {
        return bridge.getDesignConditions().getAllowableSlenderness();
    }
}
