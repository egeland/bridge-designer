/*
 * LoadTestReport.java
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
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.JDialog;

/**
 * Load test report dialog for the Bridge Designer.
 * 
 * @author  Eugene K. Ressler
 */
public class LoadTestReport extends JDialog {

    private EditableBridgeModel bridge;
    
    /**
     * Construct a fresh load test report.
     * 
     * @param parent parent frame of the dialog
     * @param bridge bridge model containing the report information
     */
    public LoadTestReport(Frame parent, EditableBridgeModel bridge) {
        super(parent, true);
        this.bridge = bridge;
        initComponents();
        getRootPane().setDefaultButton(closeButton);
        ((LoadTestReportTable)memberTable).initialize();
        pack();
        int heightDiff = memberTableScroll.getViewport().getHeight() - memberTable.getHeight();
        Dimension size = this.getSize();
        if (heightDiff > 0) {
            size.height -= heightDiff;
            this.setSize(size);
            this.setResizable(false);
        }
    }

    /*
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            AutofitTableColumns.autoResizeTable(memberTable, true, 4);
        }
        super.setVisible(visible);
    }
    */
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        closeButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        copyButton = new javax.swing.JButton();
        memberTableScroll = new javax.swing.JScrollPane();
        memberTable = new LoadTestReportTable();
        commentLabel = new DisappearingLabel(2000);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(LoadTestReport.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        printButton.setText(resourceMap.getString("printButton.text")); // NOI18N
        printButton.setName("printButton"); // NOI18N
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });

        copyButton.setText(resourceMap.getString("copyButton.text")); // NOI18N
        copyButton.setName("copyButton"); // NOI18N
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });

        memberTableScroll.setName("memberTableScroll"); // NOI18N

        memberTable.setModel(new LoadTestReportTableModel(bridge));
        memberTable.setName("memberTable"); // NOI18N
        memberTable.setRowSelectionAllowed(false);
        memberTable.getTableHeader().setReorderingAllowed(false);
        memberTableScroll.setViewportView(memberTable);

        commentLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        commentLabel.setText(null);
        commentLabel.setName("commentLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(commentLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(copyButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(closeButton)
                .addContainerGap())
            .addComponent(memberTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 940, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(memberTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(printButton)
                    .addComponent(copyButton)
                    .addComponent(closeButton)
                    .addComponent(commentLabel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(bridge.toTabDelimitedText());
    clipboard.setContents(selection, selection);
    commentLabel.setText(BDApp.getResourceMap(LoadTestReport.class).getString("copied.text"));
}//GEN-LAST:event_copyButtonActionPerformed

private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_closeButtonActionPerformed

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
    PrinterUI.print(this, memberTable, LoadTestReport.class, bridge.getNotes());    
}//GEN-LAST:event_printButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel commentLabel;
    private javax.swing.JButton copyButton;
    private javax.swing.JTable memberTable;
    private javax.swing.JScrollPane memberTableScroll;
    private javax.swing.JButton printButton;
    // End of variables declaration//GEN-END:variables

}
