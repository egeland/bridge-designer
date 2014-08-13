/*
 * LoadTestReportTableModel.java  
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.table.AbstractTableModel;

/**
 * Special purpose table model for the load test report.
 * 
 * @author Eugene K. Ressler
 */
public class LoadTestReportTableModel extends AbstractTableModel {

    private EditableBridgeModel bridge;
    
    /*
     * Column names used to retrieve resources for column headers, widths, etc.
     */
    public static final String[] columnNames = new String[]{
        "number",
        "material",
        "crossSection",
        "size",
        "length",
        "slenderness",
        "SPACER", // spacer
        "compressionForce",
        "compressionStrength",
        "compressionStatus",
        "SPACER",
        "tensionForce",
        "tensionStrength",
        "tensionStatus",
    };
    
    /**
     * Construct a new load test report table model.
     * 
     * @param bridge bridge model containing report information
     */
    public LoadTestReportTableModel(EditableBridgeModel bridge) {
        this.bridge = bridge;
    }

    /**
     * Return the number of rows in the table.
     * 
     * @return number of rows
     */
    public int getRowCount() {
        return bridge.getMembers().size();
    }

    /**
     * Return the number of columns in the table.
     * 
     * @return number of columns
     */
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Return the names of the columns in the table.
     * 
     * @param column column index
     * @return name of indexed column
     */
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    private final NumberFormat doubleFormatter = new DecimalFormat("0.00");

    /**
     * Return value of table at given row and column.
     * 
     * @param rowIndex row index
     * @param columnIndex column index
     * @return value in table at given row and column
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        Member member = bridge.getMembers().get(rowIndex);
        // Can't read analysis unless it's valid (should never happen).
        if (columnIndex >= 6 && columnIndex != 9 && !bridge.isAnalysisValid()) {
            return "--";
        }
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
                return doubleFormatter.format(member.getLength());
            case 5:
                return doubleFormatter.format(member.getSlenderness());
            case 6:
                return "";
            case 7:
                return doubleFormatter.format(bridge.getAnalysis().getMemberCompressiveForce(member.getIndex()));
            case 8:
                return doubleFormatter.format(bridge.getAnalysis().getMemberCompressiveStrength(member.getIndex()));
            case 9:
                return MemberTable.getMemberStatusString(member.getCompressionForceStrengthRatio() <= 1);          
            case 10:
                return "";
            case 11:
                return doubleFormatter.format(bridge.getAnalysis().getMemberTensileForce(member.getIndex()));
            case 12:
                return doubleFormatter.format(bridge.getAnalysis().getMemberTensileStrength(member.getIndex()));
            case 13:
                return MemberTable.getMemberStatusString(member.getTensionForceStrengthRatio() <= 1);          
        }
        return null;
    }
}
