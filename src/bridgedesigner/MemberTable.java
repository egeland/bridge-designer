/*
 * MemberTable.java  
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

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import org.jdesktop.application.ResourceMap;

/**
 * Member table for the Bridge Designer.
 * 
 * @author Eugene K. Ressler
 */
public class MemberTable extends JTable {

    private static String ok;
    private static String fail;
    private boolean sorting = false;
    private static Color subduedCompressionColor = new Color(255, 192, 192);
    private static Color subduedTensionColor = new Color(192, 192, 255);

    /**
     * Return true iff the member table is currently being sorted.  A cue to event handlers
     * that change events due to sorting swaps can be ignored.
     * 
     * @return true iff the member table is currently being sorted
     */
    public boolean isSorting() {
        return sorting;
    }

    /**
     * Return a string that indicates whether some member is okay or failing.
     * 
     * @param isOk whether member is ok or failing
     * @return okay or failing string
     */
    public static String getMemberStatusString(boolean isOk) {
        return isOk ? ok : fail;
    }
    
    private static Color contrastingColor(Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue() < 3 *128) ? Color.WHITE : Color.BLACK;
    }

    /**
     * Initialize the member table.
     */
    public void initialize() {
        // Install our custom header renderer that allows sort arrows below centered text.
        new MemberTableHeaderRenderer().installIn(this);

        // Install the row sorter.
        MemberTableModel model = (MemberTableModel) getModel();
        TableRowSorter<MemberTableModel> rowSorter = new TableRowSorter<MemberTableModel>(model) {

            @Override
            public void toggleSortOrder(int column) {
                sorting = true;
                super.toggleSortOrder(column);
                sorting = false;
            }
        };
        setRowSorter(rowSorter);

        // Don't allow column re-ordering.
        getTableHeader().setReorderingAllowed(false);

        // Set multiple row interval selection.
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Look up column structures in class resources and set them.
        ResourceMap resourceMap = BDApp.getResourceMap(MemberTable.class);
        ok = resourceMap.getString("ok.text");
        fail = resourceMap.getString("fail.text");
        for (int i = 0; i < MemberTableModel.columnNames.length; i++) {
            TableColumn column = getColumnModel().getColumn(i);
            column.setResizable(true);
            final String headerText = resourceMap.getString(MemberTableModel.columnNames[i] + "ColumnHeader.text");
            column.setHeaderValue(headerText);
            int width = resourceMap.getInteger(MemberTableModel.columnNames[i] + "Column.width");
            column.setPreferredWidth(width);
            if (i < MemberTableModel.columnNames.length - 2) {
                column.setMinWidth(width);
                if (i == 5) {
                    column.setMaxWidth(width);
                }
            }
            // Install our special column renderer.
            column.setCellRenderer(specialCellRenderer);
        }
        
        // Use black on cyan for default selection values.  Last two columns remain white due to special renderer.
        setSelectionForeground(Color.BLACK);

        // Cause new height calculation after header loading.
        getTableHeader().resizeAndRepaint();
    }

    /**
     * Load the selection in the model's member vector into the JComboBox selection.
     */
    public void loadSelection() {
        MemberTableModel model = (MemberTableModel) getModel();
        model.loadSelection(this);
    }

    /**
     * Update the table's appearance to match a change in the model data.
     */
    public void fireTableDataChanged() {
        MemberTableModel model = (MemberTableModel) getModel();
        model.fireTableDataChanged();
    }

    /**
     * Set whether the table should adjust its appearance for displayed labels.
     * 
     * @param val true iff the appearance should be as for displayed labels
     */
    public void setLabel(boolean val) {
        specialCellRenderer.setLabel(val);
        repaint();
    }
    
    private final MemberTableCellRenderer specialCellRenderer = new MemberTableCellRenderer();

    private class MemberTableCellRenderer extends DefaultTableCellRenderer {

        private boolean label = false;

        public void setLabel(boolean label) {
            this.label = label;
        }

        private final NumberFormat doubleFormatter = new DecimalFormat("0.00");

        @Override
        public void setValue(Object value) {
            if (value instanceof Double) {
                setText((value == null) ? "" : doubleFormatter.format(value));
            }
            else {
                super.setValue(value);
            }
        }

        private Color selectionBackground(JTable table, int row) {
            return Member.selectedColors[getMember(table, row).getMaterial().getIndex()];
        }
        

        private Member getMember(JTable table, int row) {
            return ((MemberTableModel) getModel()).getMember(table.convertRowIndexToModel(row));
        }

        private void setValueForNullableDouble(Object value) {
            double x = (Double)value;
            setText(x < 0 ? "--" : String.format(Locale.US, "%.2f", x));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            switch (column) {
                case 0:
                    setHorizontalAlignment(JLabel.RIGHT);
                    setBackground(label || isSelected ? Member.labelBackground : table.getBackground());
                    setForeground(table.getForeground());
                    break;
                case 1:
                case 2:
                    setHorizontalAlignment(JLabel.CENTER);
                    setBackground(isSelected ? selectionBackground(table, row) : table.getBackground());
                    setForeground(contrastingColor(getBackground()));
                    break;
                case 3:
                case 4:
                    setHorizontalAlignment(JLabel.CENTER);
                    setBackground(isSelected ? selectionBackground(table, row) : table.getBackground());
                    setForeground(contrastingColor(getBackground()));
                    break;
                // 5 is the double separator
                case 6:
                    setHorizontalAlignment(JLabel.RIGHT);
                    setBackground(getMember(table, row).getSlenderness() > ((MemberTableModel) getModel()).getAllowableSlenderness() ? Color.MAGENTA : table.getBackground());
                    setForeground(table.getForeground());
                    break;
                case 7:
                    setHorizontalAlignment(JLabel.CENTER);
                    if (((MemberTableModel) getModel()).isAnalysisValid()) {
                        setBackground(getMember(table, row).getCompressionForceStrengthRatio() <= 1 ? table.getBackground() : Color.RED);
                        setForeground(table.getForeground());
                    }
                    else {
                        setBackground(getMember(table, row).getCompressionForceStrengthRatio() <= 1 ? table.getBackground() : subduedCompressionColor);
                        setForeground(Color.GRAY);                        
                    }
                    setValueForNullableDouble(value);
                    break;
                case 8:
                    setHorizontalAlignment(JLabel.CENTER);
                    if (((MemberTableModel) getModel()).isAnalysisValid()) {
                        setBackground(getMember(table, row).getTensionForceStrengthRatio() <= 1 ? table.getBackground() : Color.BLUE);
                        setForeground(table.getForeground());                    
                    }
                    else {
                        setBackground(getMember(table, row).getTensionForceStrengthRatio() <= 1 ? table.getBackground() : subduedTensionColor);
                        setForeground(Color.GRAY);                                            
                    }
                    setValueForNullableDouble(value);
                    break;
                default:
                    setHorizontalAlignment(JLabel.CENTER);
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                    break;
            }
            return this;
        }
    }
}
