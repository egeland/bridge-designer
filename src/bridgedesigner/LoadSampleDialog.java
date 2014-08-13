/*
 * LoadSampleDialog.java
 *
 * Created on April 15, 2009, 10:54 PM
 */

package bridgedesigner;

import java.awt.Frame;
import javax.swing.JDialog;

/**
 * Load sample dialog for the Bridge Designer.
 * 
 * @author  de8827
 */
public class LoadSampleDialog extends JDialog {

    private boolean ok;
    final private BridgeModel cartoonBridge;
    final private BridgeView cartoonView;
    
    /** 
     * Construct a new load sample dialog.
     * 
     * @param parent parent frame of the dialog
     */
    public LoadSampleDialog(Frame parent) {
        super(parent, true);
        cartoonBridge = new BridgeModel();
        cartoonView = new BridgeDraftingView(cartoonBridge);
        initComponents();
        getRootPane().setDefaultButton(okButton);
    }

    /**
     * Set the visibility of the dialog.
     * 
     * @param visible whether the dialog should be made visible or invisible
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            ok = false;
            sampleList.requestFocusInWindow();
        }
        super.setVisible(visible);
    }

    /**
     * Return true iff the Ok button was pressed to the close the dialog last time it was visible.
     * 
     * @return true iff the Ok button was pressed
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * Initialize the dialog by getting the list of available samples from resources.
     */
    public void initialize() {
        sampleList.setListData(BridgeSample.getList());
        sampleList.setSelectedIndex(0);
    }
    
    /**
     * Load the selected sample into the given bridge.
     * 
     * @param bridge bridge model to load sample in
     */
    public void loadUsingSelectedSample(BridgeModel bridge) {
        Object val = sampleList.getSelectedValue();
        if (val instanceof BridgeSample) {
            bridge.read(((BridgeSample)val).getBridgeAsString());
        }
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectSampleLabel = new javax.swing.JLabel();
        templateScroll = new javax.swing.JScrollPane();
        sampleList = new javax.swing.JList();
        previewLabel = new javax.swing.JLabel();
        cartoonLabel = cartoonView.getDrawing(2, null);
        tipTextPanel = new javax.swing.JPanel();
        tipTextPane = new TipTextPane();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(LoadSampleDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        selectSampleLabel.setText(resourceMap.getString("selectSampleLabel.text")); // NOI18N
        selectSampleLabel.setName("selectSampleLabel"); // NOI18N

        templateScroll.setName("templateScroll"); // NOI18N

        sampleList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sampleList.setName("sampleList"); // NOI18N
        sampleList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                sampleListMouseClicked(evt);
            }
        });
        sampleList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                sampleListValueChanged(evt);
            }
        });
        templateScroll.setViewportView(sampleList);

        previewLabel.setText(resourceMap.getString("previewLabel.text")); // NOI18N
        previewLabel.setName("previewLabel"); // NOI18N

        cartoonLabel.setBackground(resourceMap.getColor("cartoonLabel.background")); // NOI18N
        cartoonLabel.setForeground(resourceMap.getColor("cartoonLabel.foreground")); // NOI18N
        cartoonLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cartoonLabel.setText(resourceMap.getString("cartoonLabel.text")); // NOI18N
        cartoonLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        cartoonLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        cartoonLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cartoonLabel.setName("cartoonLabel"); // NOI18N
        cartoonLabel.setOpaque(true);
        cartoonLabel.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        tipTextPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Design TIp:"));
        tipTextPanel.setName("tipTextPanel"); // NOI18N

        tipTextPane.setBorder(null);
        tipTextPane.setText(resourceMap.getString("tipTextPane.text")); // NOI18N
        tipTextPane.setName("tipTextPane"); // NOI18N

        javax.swing.GroupLayout tipTextPanelLayout = new javax.swing.GroupLayout(tipTextPanel);
        tipTextPanel.setLayout(tipTextPanelLayout);
        tipTextPanelLayout.setHorizontalGroup(
            tipTextPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tipTextPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipTextPane, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                .addContainerGap())
        );
        tipTextPanelLayout.setVerticalGroup(
            tipTextPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tipTextPanelLayout.createSequentialGroup()
                .addComponent(tipTextPane, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectSampleLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(previewLabel)
                            .addComponent(templateScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 431, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cartoonLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 430, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(7, 7, 7)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(tipTextPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(okButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectSampleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tipTextPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(templateScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(previewLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cartoonLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void sampleListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sampleListMouseClicked
    if (evt.getClickCount() == 2) {
        ok = true;
        setVisible(false);
    }
}//GEN-LAST:event_sampleListMouseClicked

private void sampleListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_sampleListValueChanged
    loadUsingSelectedSample(cartoonBridge);
    cartoonView.initialize(cartoonBridge.getDesignConditions());
    cartoonLabel.repaint();
}//GEN-LAST:event_sampleListValueChanged

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    ok = true;
    setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel cartoonLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel previewLabel;
    private javax.swing.JList sampleList;
    private javax.swing.JLabel selectSampleLabel;
    private javax.swing.JScrollPane templateScroll;
    private javax.swing.JTextPane tipTextPane;
    private javax.swing.JPanel tipTextPanel;
    // End of variables declaration//GEN-END:variables
}
