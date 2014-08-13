/*
 * AutofitTableColumns.java  
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Component;


import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;

/**
 * Provides couple of static functions that will resize the columns of a table based on content width.
 * 
 * @author Eugene K. Ressler
 */
public class AutofitTableColumns {

    private static final int DEFAULT_COLUMN_PADDING = 5;

    /**
     * Automatically resize the columns of the given table to fit contents.
     * 
     * @param table the JTable to autoresize columns 
     * @param includeColumnHeaderWidth use the Column Header width as a minimum width
     * @return The table width, just in case the caller wants it...
     */
    public static int autoResizeTable(JTable table, boolean includeColumnHeaderWidth) {
        return (autoResizeTable(table, includeColumnHeaderWidth, DEFAULT_COLUMN_PADDING));
    }

    /**
     * Automatically resize the columns of the given table to fit contents.
     * 
     * @param table the JTable to autoresize columns
     * @param includeColumnHeaderWidth use the Column Header width as a minimum width
     * @param columnPadding how many extra pixels do you want on the end of each column
     * @return The table width, just in case the caller wants it...
     */
    public static int autoResizeTable(JTable table, boolean includeColumnHeaderWidth, int columnPadding) {
        int columnCount = table.getColumnCount();
        int tableWidth = 0;

        Dimension cellSpacing = table.getIntercellSpacing();

        if (columnCount > 0) // must have columns !
        {
            int columnWidth[] = new int[columnCount];

            for (int i = 0; i < columnCount; i++) {
                columnWidth[i] = getMaxColumnWidth(table, i, includeColumnHeaderWidth, columnPadding);
                tableWidth += columnWidth[i];
            }

            // account for cell spacing
            tableWidth += ((columnCount - 1) * cellSpacing.width);

            // try changing the size of the column names area
            JTableHeader tableHeader = table.getTableHeader();

            Dimension headerDim = tableHeader.getPreferredSize();

            // headerDim.height = tableHeader.getHeight();
            headerDim.width = tableWidth;
            tableHeader.setPreferredSize(headerDim);

            TableColumnModel tableColumnModel = table.getColumnModel();
            TableColumn tableColumn;

            for (int i = 0; i < columnCount; i++) {
                tableColumn = tableColumnModel.getColumn(i);
                tableColumn.setPreferredWidth(columnWidth[i]);
            }

            table.invalidate();
            table.doLayout();
            table.repaint();
        }

        return (tableWidth);
    }

    /**
     * Get the largest cell width in given column.
     * 
     * @param table the JTable to autoresize the columns on
     * @param columnNo the column number, starting at zero, to calculate the maximum width on
     * @param includeColumnHeaderWidth use the Column Header width as a minimum width
     * @param columnPadding how many extra pixels do you want on the end of each column
     * @return cell width of widest cell in column
     */
    private static int getMaxColumnWidth(JTable table, int columnNo,
            boolean includeColumnHeaderWidth,
            int columnPadding) {
        TableColumn column = table.getColumnModel().getColumn(columnNo);
        Component comp = null;
        int maxWidth = 0;
        if (includeColumnHeaderWidth) {
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer != null) {
                comp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, columnNo);
                if (comp instanceof JTextComponent) {
                    JTextComponent jtextComp = (JTextComponent) comp;

                    String text = jtextComp.getText();
                    Font font = jtextComp.getFont();
                    FontMetrics fontMetrics = jtextComp.getFontMetrics(font);

                    maxWidth = SwingUtilities.computeStringWidth(fontMetrics, text);
                } else {
                    maxWidth = comp.getPreferredSize().width;
                }
            } else {
                try {
                    String headerText = (String) column.getHeaderValue();
                    JLabel defaultLabel = new JLabel(headerText);

                    Font font = defaultLabel.getFont();
                    FontMetrics fontMetrics = defaultLabel.getFontMetrics(font);

                    maxWidth = SwingUtilities.computeStringWidth(fontMetrics, headerText);
                } catch (ClassCastException ce) {
                    // Can't work out the header column width..
                    maxWidth = 0;
                }
            }
        }
        TableCellRenderer tableCellRenderer;
        // Component comp;
        int cellWidth = 0;
        for (int i = 0; i < table.getRowCount(); i++) {
            tableCellRenderer = table.getCellRenderer(i, columnNo);

            comp = tableCellRenderer.getTableCellRendererComponent(table, table.getValueAt(i, columnNo), false, false, i, columnNo);

            if (comp instanceof JTextComponent) {
                JTextComponent jtextComp = (JTextComponent) comp;

                String text = jtextComp.getText();
                Font font = jtextComp.getFont();
                FontMetrics fontMetrics = jtextComp.getFontMetrics(font);

                int textWidth = SwingUtilities.computeStringWidth(fontMetrics, text);

                maxWidth = Math.max(maxWidth, textWidth);
            } else {
                cellWidth = comp.getPreferredSize().width;

                // maxWidth = Math.max ( headerWidth, cellWidth );
                maxWidth = Math.max(maxWidth, cellWidth);
            }
        }

        return (maxWidth + columnPadding);
    }
}
