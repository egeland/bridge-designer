/*
 * DesignIterationDialog.java
 *
 * Created on March 24, 2009, 9:32 PM
 */

package bridgedesigner;

import java.awt.Component;
import java.awt.Frame;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Dialog for selecting a design iteration.
 * 
 * @author Eugene K. Ressler
 */
public class DesignIterationDialog extends JDialog {

    private final DesignIterationTableModel designIterationTableModel;
    private final DefaultTreeModel designIterationTreeModel;
    private boolean ok = false;

    /**
     * Construct a new design iteration dialog with the given parent. Capture
     * the given bridge.
     * 
     * @param parent parent frame
     * @param bridge bridge to capture in the iteration
     */
    public DesignIterationDialog(Frame parent, final EditableBridgeModel bridge) {
        super(parent, true);
        designIterationTableModel = new DesignIterationTableModel(bridge);
        designIterationTreeModel = new DefaultTreeModel(bridge.getDesignIterationTreeRoot());
        initComponents();
        getRootPane().setDefaultButton(okButton);
        
        // Center table headers.
        ((DefaultTableCellRenderer)selectList.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        // Control content column by column.
        TableCellRenderer renderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel cellLabel = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // Set this null because the JTable uses one label for all cells,
                // and a previously set icon will persist unless we remove it
                // explicitly.  On the other hand, the default cell renderers
                // set the text to the toString() value, which is what we need
                // for all but column 1.
                cellLabel.setIcon(null);
                switch (column) {
                    case 0:
                        setHorizontalAlignment(JLabel.CENTER);
                        cellLabel.setIcon((Icon)value);
                        cellLabel.setText(null);
                        break;
                    case 1:
                        setHorizontalAlignment(JLabel.CENTER);
                        break;
                    case 2:
                        setHorizontalAlignment(JLabel.RIGHT);
                        break;
                    case 3:
                        setHorizontalAlignment(JLabel.LEADING);
                        break;
                }
                return this;
            }
        };
        
        for (int i = 0; i < selectList.getColumnCount(); i++) {
            selectList.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        selectList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int index = selectList.getSelectedRow();
                    if (index >= 0) {
                        designIterationTableModel.loadCartoon(index);
                        previewCartoon.repaint();
                        DesignIteration iteration = bridge.getDesignIteration(index);
                        selectTree.setSelectionPath(new TreePath(iteration.getPath()));
                    }
                }
            }
        });
        
        // Replace icons in tree rows with indicators of bridge status.
        TreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                DesignIteration designIteration = (DesignIteration)value;
                setIcon(IconFactory.bridgeStatus(designIteration.getBridgeStatus()));
                return this;
            }
        };
        selectTree.setCellRenderer(treeRenderer);

        selectTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                int index = bridge.getDesignIterationIndex(e.getPath().getLastPathComponent());
                selectList.getSelectionModel().setSelectionInterval(index, index);
            }
        });
    }

    /**
     * Return true iff the user selected an iteration as the dialog closed.
     * 
     * @return true iff an iteration was selected
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * Load the selected iteration into the attached bridge.
     */
    public void loadSelectedIteration() {
        designIterationTableModel.getBridge().loadIteration(selectList.getSelectedRow());
    }

    /**
     * Set the visibility of the dialog.  When being set visible, the dialog is initialized using the 
     * attached bridge's iteration list.
     * 
     * @param b whether to make the dialog visible or invisible.
     */
    @Override
    public void setVisible(boolean b) {
        if (b) {
            ok = false;
            designIterationTableModel.getBridge().saveSnapshot();       
            ((DefaultTreeModel)selectTree.getModel()).reload();
            int index = designIterationTableModel.getBridge().getCurrentIterationIndex();
            // If there is a change, setting the row will also set the tree.
            if (index != selectList.getSelectedRow()) {
                selectList.setRowSelectionInterval(index, index);
            }
            else {
                // Else set the tree explicitly.
                DesignIteration iteration = designIterationTableModel.getBridge().getDesignIteration(index);
                selectTree.setSelectionPath(new TreePath(iteration.getPath()));                
            }
            designIterationTableModel.loadCartoon(index);
            AutofitTableColumns.autoResizeTable(selectList, true, 4);
            // Pull focus to the visible tab item and scroll to show the current iteration.
            if (viewTabs.getSelectedIndex() == 0) {
                selectList.scrollRectToVisible(selectList.getCellRect(selectList.getSelectedRow(), 0, true));
                selectList.requestFocusInWindow();
            }
            else {
                selectTree.scrollPathToVisible(selectTree.getSelectionPath());
                selectTree.requestFocusInWindow();
            }
        }
        super.setVisible(b);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectLabel = new javax.swing.JLabel();
        tipPanel = new javax.swing.JPanel();
        tipPane = new bridgedesigner.TipTextPane();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        previewLabel = new javax.swing.JLabel();
        previewCartoon = designIterationTableModel.getBridgeView().getDrawing(2)
        ;
        viewTabs = new javax.swing.JTabbedPane();
        selectListScroll = new javax.swing.JScrollPane();
        selectList = new javax.swing.JTable();
        selectTreeScroll = new javax.swing.JScrollPane();
        selectTree = new JTree(designIterationTreeModel);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(DesignIterationDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        selectLabel.setText(resourceMap.getString("selectLabel.text")); // NOI18N
        selectLabel.setName("selectLabel"); // NOI18N

        tipPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("tipPanel.border.title"))); // NOI18N
        tipPanel.setName("tipPanel"); // NOI18N

        tipPane.setBorder(null);
        tipPane.setText(resourceMap.getString("tipPane.text")); // NOI18N
        tipPane.setName("tipPane"); // NOI18N

        javax.swing.GroupLayout tipPanelLayout = new javax.swing.GroupLayout(tipPanel);
        tipPanel.setLayout(tipPanelLayout);
        tipPanelLayout.setHorizontalGroup(
            tipPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tipPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipPane, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                .addContainerGap())
        );
        tipPanelLayout.setVerticalGroup(
            tipPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tipPanelLayout.createSequentialGroup()
                .addComponent(tipPane)
                .addContainerGap())
        );

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        previewLabel.setText(resourceMap.getString("previewLabel.text")); // NOI18N
        previewLabel.setName("previewLabel"); // NOI18N

        previewCartoon.setText(null);
        previewCartoon.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        previewCartoon.setName("previewCartoon"); // NOI18N

        viewTabs.setName("viewTabs"); // NOI18N

        selectListScroll.setName("selectListScroll"); // NOI18N

        selectList.setModel(designIterationTableModel);
        selectList.setFillsViewportHeight(true);
        selectList.setIntercellSpacing(new java.awt.Dimension(0, 2));
        selectList.setName("selectList"); // NOI18N
        selectList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        selectList.setShowHorizontalLines(false);
        selectList.setShowVerticalLines(false);
        selectList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectListMouseClicked(evt);
            }
        });
        selectListScroll.setViewportView(selectList);

        viewTabs.addTab(resourceMap.getString("selectListScroll.TabConstraints.tabTitle"), null, selectListScroll, resourceMap.getString("selectListScroll.TabConstraints.tabToolTip")); // NOI18N

        selectTreeScroll.setName("selectTreeScroll"); // NOI18N

        selectTree.setName("selectTree"); // NOI18N
        selectTree.setRootVisible(false);
        selectTree.setShowsRootHandles(true);
        selectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        selectTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectTreeMouseClicked(evt);
            }
        });
        selectTreeScroll.setViewportView(selectTree);

        viewTabs.addTab(resourceMap.getString("selectTreeScroll.TabConstraints.tabTitle"), null, selectTreeScroll, resourceMap.getString("selectTreeScroll.TabConstraints.tabToolTip")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectLabel)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(viewTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                            .addComponent(previewCartoon, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                            .addComponent(previewLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(tipPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tipPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(okButton)
                            .addComponent(cancelButton)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(viewTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(previewLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(previewCartoon, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    ok = true;
    setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

private void selectListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectListMouseClicked
    if (evt.getClickCount() == 2) {
        ok = true;
        setVisible(false);
    }
}//GEN-LAST:event_selectListMouseClicked

private void selectTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectTreeMouseClicked
    selectListMouseClicked(evt);
}//GEN-LAST:event_selectTreeMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel previewCartoon;
    private javax.swing.JLabel previewLabel;
    private javax.swing.JLabel selectLabel;
    private javax.swing.JTable selectList;
    private javax.swing.JScrollPane selectListScroll;
    private javax.swing.JTree selectTree;
    private javax.swing.JScrollPane selectTreeScroll;
    private javax.swing.JTextPane tipPane;
    private javax.swing.JPanel tipPanel;
    private javax.swing.JTabbedPane viewTabs;
    // End of variables declaration//GEN-END:variables

}
