/*
 * SiteCostTable.java  
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
 * Special table for site costs. Used in the setup wizard.
 * 
 * @author Eugene K. Ressler
 */
public class SiteCostTable extends JTable {

    final Font bold = UIManager.getFont("Label.font").deriveFont(Font.BOLD);

    /**
     * Initialize the site cost table.
     */
    public void initalize() {
        for (int i = 0; i < 3; i++) {
            getColumnModel().getColumn(i).setCellRenderer(specialRender);            
        }
    }
    
    private DefaultTableCellRenderer specialRender = new DefaultTableCellRenderer() {
        private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);        
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 2) {
                value = currencyFormatter.format(value);
            }
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            switch (column) {
                case 0:
                    setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 1:
                case 2:
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
