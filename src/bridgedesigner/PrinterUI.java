/*
 * PrinterUI.java  
 *   
 * Copyright (C) 2010 Eugene K. Ressler
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
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.MessageFormat;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.jdesktop.application.ResourceMap;

/**
 * Common user interface functionality for printing.
 * 
 * @author Eugene K. Ressler
 */
public class PrinterUI {

    private static PrintRequestAttributeSet attr = null;
    
    /**
     * Print the given table, taking header and footer strings from given class.
     * 
     * @param parent parent component of the print dialog
     * @param table table to print
     * @param stringResourceClass class to use for header and footer strings from resources
     * @param notes lines to add as notes
     */
    public static void print(Component parent, JTable table, Class stringResourceClass, String [] notes) {
        ResourceMap resourceMap = BDApp.getResourceMap(stringResourceClass);
        Printable printable = new TabularReportPrintable(table, JTable.PrintMode.FIT_WIDTH, 
                new MessageFormat(resourceMap.getString("printHeader.message")), 
                notes,
                new MessageFormat(resourceMap.getString("printFooter.message")));
        print(parent, printable, resourceMap, true);        
    }
    
    /**
     * Print the given bridge as a blueprint.
     * 
     * @param parent parent component of the print dialog
     * @param fileName filename of bridge or null if none
     * @param bridge bridge model to print
     * @param stringResourceClass class to use for labels and notes from resources
     * @param dialog print dialog to use
     */
    public static void print(Component parent, String fileName, BridgeModel bridge, Class stringResourceClass, boolean dialog) {
        ResourceMap resourceMap = BDApp.getResourceMap(stringResourceClass);
        Printable printable = new BridgeBlueprintPrintable(fileName, bridge);
        print(parent, printable, resourceMap, dialog);
    }
    
    private static PrinterJob job = null;
    
    /**
     * Parameterized general printing routine that shares the print dialog including current printer,
     * paper orientation, margins, etc.
     * 
     * @param parent parent component of the print dialog
     * @param printable printable to print
     * @param resourceMap resource map to use for halted text and title
     * @param dialog print dialog to use
     */
    private static void print(Component parent, Printable printable, ResourceMap resourceMap, boolean dialog) {
        
        boolean firstPrint = false;
        
        // fetch a PrinterJob
        if (job == null) {
            job = PrinterJob.getPrinterJob();
            firstPrint = true;
        }

        // set the Printable on the PrinterJob
        job.setPrintable(printable);

        // create an attribute set to store attributes from the print dialog
        if (attr == null) {
            attr = new HashPrintRequestAttributeSet();
            attr.add(new JobName(BDApp.getResourceMap(PrinterUI.class).getString("jobName.text"), null));
            // Ask for a huge printable area and let driver trim it to actual maximum.
            attr.add(new MediaPrintableArea(0f, 0f, 100f, 100f, MediaPrintableArea.INCH));
        }

        // display a print dialog and record whether or not the user cancels it
        
        boolean printAccepted = true;
        
        if (firstPrint || dialog) {
            printAccepted = job.printDialog(attr);
        }

        // if the user didn't cancel the dialog
        if (printAccepted) {
            try {
                job.print(attr);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent, 
                        resourceMap.getString("printingHalted.text", ex.getMessage()),
                        resourceMap.getString("printingHaltedDialogTitle.text"), 
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}
