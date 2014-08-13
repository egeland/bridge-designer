/*
 * CostReport.java
 *
 * Created on March 20, 2009, 12:31 AM
 */

package bridgedesigner;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.Timer;

/**
 * Cost report dialog.
 * 
 * @author  de8827
 */
public class CostReport extends JDialog {

    /**
     * Cost summary from the bridge model.
     */
    private BridgeModel.Costs costs;

    /**
     * Timer to make the comment text go away after a few seconds.
     */
    private Timer commentHider = null;
    
    /**
     * Construct a new cost report with the given parent frame.
     * 
     * @param parent parent frame
     */
    public CostReport(Frame parent) {
        super(parent, true);
        initComponents();
        Help.getBroker().enableHelpOnButton(helpButton, "hlp_cost", Help.getSet());
        getRootPane().setDefaultButton(closeButton);
    }

    /**
     * Initialize the report with the given cost summary.
     * 
     * @param costs cost summary
     */
    public void initialize(EditableBridgeModel.Costs costs) {
        this.costs = costs;
        ((CostReportTableModel)costTable.getModel()).initialize(costs);
        ((CostReportTable) costTable).initalize();
        // Deal with case where table is smaller than default space alloted by packing the dialog.
        pack();
        int heightDiff = costTableScroll.getViewport().getHeight() - costTable.getHeight();
        if (heightDiff > 0) {
            Dimension size = this.getSize();
            size.height -= heightDiff;
            this.setSize(size);
            this.setResizable(false);
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        costTableScroll = new javax.swing.JScrollPane();
        costTable = new CostReportTable();
        helpButton = new javax.swing.JButton();
        copyButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        commentLabel = new DisappearingLabel(2000);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(CostReport.class);
        setTitle(resourceMap.getString("costReport.title")); // NOI18N
        setName("costReport"); // NOI18N

        costTableScroll.setName("costTableScroll"); // NOI18N

        costTable.setModel(new bridgedesigner.CostReportTableModel());
        costTable.setFocusable(false);
        costTable.setIntercellSpacing(new java.awt.Dimension(4, 1));
        costTable.setName("costTable"); // NOI18N
        costTable.setRowSelectionAllowed(false);
        costTable.setShowVerticalLines(false);
        costTableScroll.setViewportView(costTable);

        helpButton.setText(resourceMap.getString("helpButton.text")); // NOI18N
        helpButton.setName("helpButton"); // NOI18N

        copyButton.setText(resourceMap.getString("copyButton.text")); // NOI18N
        copyButton.setName("copyButton"); // NOI18N
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });

        printButton.setText(resourceMap.getString("printButton.text")); // NOI18N
        printButton.setName("printButton"); // NOI18N
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        commentLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        commentLabel.setText(null);
        commentLabel.setName("commentLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(commentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 388, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(helpButton)
                .addGap(12, 12, 12)
                .addComponent(copyButton, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(printButton, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(costTableScroll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 937, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(costTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeButton)
                    .addComponent(printButton)
                    .addComponent(helpButton)
                    .addComponent(copyButton)
                    .addComponent(commentLabel))
                .addContainerGap())
        );

        commentLabel.getAccessibleContext().setAccessibleName(null);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_closeButtonActionPerformed

private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(costs.toTabDelimitedText());
    clipboard.setContents(selection, selection);
    // This label disappears after 2 seconds.  See the custom code in the form.
    commentLabel.setText(BDApp.getResourceMap(CostReport.class).getString("copied.text"));
}//GEN-LAST:event_copyButtonActionPerformed

private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed
    PrinterUI.print(this, costTable, CostReport.class, costs.notes);    
}//GEN-LAST:event_printButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel commentLabel;
    private javax.swing.JButton copyButton;
    private javax.swing.JTable costTable;
    private javax.swing.JScrollPane costTableScroll;
    private javax.swing.JButton helpButton;
    private javax.swing.JButton printButton;
    // End of variables declaration//GEN-END:variables

}
