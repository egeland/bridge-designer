/*
 * LoadTestReportTable.java  
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

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.jdesktop.application.ResourceMap;

/**
 * Special purpose table for the load test report.
 * 
 * @author Eugene K. Ressler
 */
public class LoadTestReportTable extends JTable {

    MemberTableCellRenderer specialCellRenderer = new MemberTableCellRenderer();
    
    public static String cvsHeaders = "";
    
    /**
     * Initialize the table contents from resources.
     */
    public void initialize() {
        ResourceMap resourceMap = BDApp.getResourceMap(LoadTestReportTable.class);
        for (int i = 0; i < LoadTestReportTableModel.columnNames.length; i++) {
            TableColumn column = getColumnModel().getColumn(i);
            column.setResizable(true);
            final String headerText = resourceMap.getString(LoadTestReportTableModel.columnNames[i] + "ColumnHeader.text");
            column.setHeaderValue(headerText);
            int width = resourceMap.getInteger(LoadTestReportTableModel.columnNames[i] + "Column.width");
            column.setPreferredWidth(width);
            if (headerText.length() == 0) {
                column.setMinWidth(width);
                column.setMaxWidth(width);
            }
            // Install our special column renderer.
            column.setCellRenderer(specialCellRenderer);
        }
        cvsHeaders = resourceMap.getString("cvsHeaders.text");
    }
    
    private class MemberTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            switch (column) {
                case 0: // Member number.
                case 6: // Compression force.
                case 7: // Comprssive strength.
                case 10: // Tension force.
                case 11: // Tensile strength;
                    setHorizontalAlignment(JLabel.RIGHT);
                    break;
                default:
                    setHorizontalAlignment(JLabel.CENTER);
                    break;
            }
            return this;
        }
        
    }
}
