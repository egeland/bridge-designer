/*
 * CostReportTable.java  
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
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Cost report table used in cost report dialog.
 * 
 * @author Eugene K. Ressler
 */
public class CostReportTable extends JTable {

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    private Font bold = UIManager.getFont("Label.font").deriveFont(Font.BOLD);

    /**
     * Initialize the table.
     * 
     * @return width resulting from automatic sizing to fit contents
     */
    public int initalize() {
        for (int i = 0; i < 4; i++) {
            getColumnModel().getColumn(i).setCellRenderer(specialRender);
        }
        // Assume any row with empty column 3 is a double rule.  Set tiny row height.
        for (int i = 0; i < getRowCount(); i++) {
            setRowHeight(i, getValueAt(i, 3) == null ? 2 : getRowHeight());
        }
        return AutofitTableColumns.autoResizeTable(this, true, 4);
    }
    
    private DefaultTableCellRenderer specialRender = new DefaultTableCellRenderer() {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 3 && value != null) {
                value = currencyFormatter.format(value);
            }
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            switch (column) {
                case 0:
                case 1:
                    setHorizontalAlignment(JLabel.LEADING);
                    break;
                case 2:
                case 3:
                    setHorizontalAlignment(JLabel.RIGHT);
                    break;
            }
            if (row == table.getRowCount() - 1) {
                setFont(bold);
            }
            return this;
        }
    };
}
