package bridgedesigner;

import java.awt.Component;
import java.awt.Color;
import java.util.Enumeration;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 * Special rendered for header of member table.
 * 
 * @author Eugene K. Ressler
 */
public class MemberTableHeaderRenderer extends DefaultTableCellRenderer implements UIResource {

    /**
     * Construct a new member table header renderer.
     */
    public MemberTableHeaderRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
        setHorizontalTextPosition(JLabel.CENTER);
        setVerticalTextPosition(JLabel.TOP);
    }

        /**
         * Return a componet for rendering a cell of the header
         * 
         * @param table table being rendered
         * @param value value being rendered
         * @param isSelected whether the cell is selected
         * @param hasFocus whether the cell has the focus
         * @param row row index
         * @param column column index
         * @return rendering component, always equal to this
         */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Icon sortIcon = null;

        boolean isPaintingForPrint = false;

        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                Color fgColor = null;
                Color bgColor = null;
                if (hasFocus) {
                    fgColor = UIManager.getColor("TableHeader.focusCellForeground");
                    bgColor = UIManager.getColor("TableHeader.focusCellBackground");
                }
                if (fgColor == null) {
                    fgColor = header.getForeground();
                }
                if (bgColor == null) {
                    bgColor = header.getBackground();
                }
                setForeground(fgColor);
                setBackground(bgColor);

                setFont(header.getFont());

                isPaintingForPrint = header.isPaintingForPrint();
            }

            if (!isPaintingForPrint && table.getRowSorter() != null) {
                SortOrder sortOrder = getColumnSortOrder(table, column);
                if (sortOrder == null) {
                    sortIcon = BDApp.getApplication().getIconResource("sortnull.png");
                } else {
                    switch (sortOrder) {
                        case ASCENDING:
                            sortIcon = BDApp.getApplication().getIconResource("sortascending.png");
                            break;
                        case DESCENDING:
                            sortIcon = BDApp.getApplication().getIconResource("sortdescending.png");
                            break;
                        case UNSORTED:
                            sortIcon = BDApp.getApplication().getIconResource("sortneutral.png");
                            break;
                    }
                }
            }
        }

        setText(value == null ? "" : value.toString());
        setIcon(sortIcon);

        Border border = null;
        if (hasFocus) {
            border = UIManager.getBorder("TableHeader.focusCellBorder");
        }
        if (border == null) {
            border = UIManager.getBorder("TableHeader.cellBorder");
        }
        setBorder(border);

        return this;
    }

    /**
     * If the first of table's sort keys corresponds to this column, then return 
     * the sort order for that column, else null.
     * 
     * @param table
     * @param column
     * @return sort order if this column corresponds to the first sort key, else null.
     */
    public static SortOrder getColumnSortOrder(JTable table, int column) {
        SortOrder rv = null;
        if (table.getRowSorter() != null) {
            List<? extends RowSorter.SortKey> sortKeys = table.getRowSorter().getSortKeys();
            if (sortKeys.size() > 0 && sortKeys.get(0).getColumn() == table.convertColumnIndexToModel(column)) {
                rv = sortKeys.get(0).getSortOrder();
            }
        }
        return rv;
    }

    /**
     * Put this renderer into all columns of the given table.
     * 
     * @param table
     */
    public void installIn(JTable table) {
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            e.nextElement().setHeaderRenderer(this);
        }
    }
}
