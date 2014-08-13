/*
 * WelcomeDialog.java
 *   
 * Copyright (C) 2008 Eugene K. Ressler
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

import java.awt.Frame;
import javax.swing.JDialog;

/**
 * Welcome dialog for the Bridge Designer.
 * 
 * @author Eugene K. Ressler
 */
public class WelcomeDialog extends JDialog {

    /**
     * User pressed cancel button.
     */
    public static final int CANCEL = 0;
    /**
     * User pressed Ok after selecting create new design.
     */
    public static final int CREATE_NEW  = 1;
    /**
     * User pressed Ok after selecting load sample.
     */
    public static final int LOAD_SAMPLE = 2;
    /**
     * User pressed Ok after selecting load design from file.
     */
    public static final int LOAD = 3;
    
    private int result;

    public interface AboutProvider {
        void showAbout();
    }

    private AboutProvider aboutProvider;

    /**
     * Construct a new welcome dialog with given parent.
     * 
     * @param parent parent frame of dialog
     */
    public WelcomeDialog(Frame parent, AboutProvider aboutProvider) {
        super(parent, true);
        this.aboutProvider = aboutProvider;
        initComponents();
        getRootPane().setDefaultButton(okButton);
    }

    /**
     * Get the result of the last dialog session.
     * 
     * @return one of CANCEL, CREATE_NEW, LOAD_SAMPLE, or LOAD depending on radio button selection
     * of user
     */
    public int getResult() {
        return result;
    }

    /**
     * Set whether the dialog should be visible or invisible.
     * 
     * @param b if true, set visible, else invisible
     */
    @Override
    public void setVisible(boolean b) {
        if (b) {
            result = CANCEL;
        }
        super.setVisible(b);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        createLoadSampleGroup = new javax.swing.ButtonGroup();
        motifLabel = new javax.swing.JLabel();
        aboutButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        bannerLabel = new javax.swing.JLabel();
        createLoadSamplePanel = new javax.swing.JPanel();
        createButton = new javax.swing.JRadioButton();
        loadSampleButton = new javax.swing.JRadioButton();
        createIcon = new javax.swing.JLabel();
        loadSampleIcon = new javax.swing.JLabel();
        loadPanel = new javax.swing.JPanel();
        loadIcon = new javax.swing.JLabel();
        loadButton = new javax.swing.JRadioButton();
        tipPanel = new javax.swing.JPanel();
        tipLabel = new javax.swing.JLabel();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(WelcomeDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        motifLabel.setIcon(resourceMap.getIcon("motifLabel.icon")); // NOI18N
        motifLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        motifLabel.setName("motifLabel"); // NOI18N

        aboutButton.setText(resourceMap.getString("aboutButton.text")); // NOI18N
        aboutButton.setName("aboutButton"); // NOI18N
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        bannerLabel.setIcon(resourceMap.getIcon("bannerLabel.icon")); // NOI18N
        bannerLabel.setFocusable(false);
        bannerLabel.setName("bannerLabel"); // NOI18N

        createLoadSamplePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        createLoadSamplePanel.setName("createLoadSamplePanel"); // NOI18N

        createLoadSampleGroup.add(createButton);
        createButton.setSelected(true);
        createButton.setText(resourceMap.getString("createButton.text")); // NOI18N
        createButton.setName("createButton"); // NOI18N
        createButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                createButtonMouseClicked(evt);
            }
        });

        createLoadSampleGroup.add(loadSampleButton);
        loadSampleButton.setText(resourceMap.getString("loadSampleButton.text")); // NOI18N
        loadSampleButton.setName("loadSampleButton"); // NOI18N
        loadSampleButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                loadSampleButtonMouseClicked(evt);
            }
        });

        createIcon.setIcon(resourceMap.getIcon("createIcon.icon")); // NOI18N
        createIcon.setName("createIcon"); // NOI18N

        loadSampleIcon.setIcon(resourceMap.getIcon("loadSampleIcon.icon")); // NOI18N
        loadSampleIcon.setName("loadSampleIcon"); // NOI18N

        javax.swing.GroupLayout createLoadSamplePanelLayout = new javax.swing.GroupLayout(createLoadSamplePanel);
        createLoadSamplePanel.setLayout(createLoadSamplePanelLayout);
        createLoadSamplePanelLayout.setHorizontalGroup(
            createLoadSamplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(createLoadSamplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(createLoadSamplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(createIcon)
                    .addComponent(loadSampleIcon))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(createLoadSamplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(createButton)
                    .addComponent(loadSampleButton))
                .addGap(53, 53, 53))
        );
        createLoadSamplePanelLayout.setVerticalGroup(
            createLoadSamplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, createLoadSamplePanelLayout.createSequentialGroup()
                .addContainerGap(14, Short.MAX_VALUE)
                .addGroup(createLoadSamplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(createIcon)
                    .addComponent(createButton))
                .addGap(5, 5, 5)
                .addGroup(createLoadSamplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(loadSampleIcon)
                    .addComponent(loadSampleButton))
                .addGap(13, 13, 13))
        );

        loadPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        loadPanel.setName("loadPanel"); // NOI18N

        loadIcon.setIcon(resourceMap.getIcon("loadIcon.icon")); // NOI18N
        loadIcon.setName("loadIcon"); // NOI18N

        createLoadSampleGroup.add(loadButton);
        loadButton.setText(resourceMap.getString("loadButton.text")); // NOI18N
        loadButton.setName("loadButton"); // NOI18N
        loadButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                loadButtonMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout loadPanelLayout = new javax.swing.GroupLayout(loadPanel);
        loadPanel.setLayout(loadPanelLayout);
        loadPanelLayout.setHorizontalGroup(
            loadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loadIcon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadButton)
                .addContainerGap(65, Short.MAX_VALUE))
        );
        loadPanelLayout.setVerticalGroup(
            loadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(loadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(loadButton)
                    .addComponent(loadIcon))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tipPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("tipPanel.border.title"))); // NOI18N
        tipPanel.setName("tipPanel"); // NOI18N
        tipPanel.setOpaque(false);

        tipLabel.setIcon(resourceMap.getIcon("tipLabel.icon")); // NOI18N
        tipLabel.setText(resourceMap.getString("tipLabel.text")); // NOI18N
        tipLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        tipLabel.setName("tipLabel"); // NOI18N
        tipLabel.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout tipPanelLayout = new javax.swing.GroupLayout(tipPanel);
        tipPanel.setLayout(tipPanelLayout);
        tipPanelLayout.setHorizontalGroup(
            tipPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tipPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                .addContainerGap())
        );
        tipPanelLayout.setVerticalGroup(
            tipPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tipLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(createLoadSamplePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                            .addComponent(loadPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tipPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(bannerLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(motifLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                    .addComponent(aboutButton, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(bannerLabel)
                        .addGap(13, 13, 13)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(createLoadSamplePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loadPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(tipPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(motifLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aboutButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okButton)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
    aboutProvider.showAbout();
}//GEN-LAST:event_aboutButtonActionPerformed

private void handleOk() {
    if (createButton.isSelected())
        result = CREATE_NEW;
    else if (loadSampleButton.isSelected())
        result = LOAD_SAMPLE;
    else if (loadButton.isSelected())
        result = LOAD;
    else 
        result = CANCEL;
    setVisible(false);    
}

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    handleOk();
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

private void loadSampleButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_loadSampleButtonMouseClicked
    if (evt.getClickCount() == 2) {
        handleOk();
    }
}//GEN-LAST:event_loadSampleButtonMouseClicked

private void createButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_createButtonMouseClicked
    if (evt.getClickCount() == 2) {
        handleOk();
    }
}//GEN-LAST:event_createButtonMouseClicked

private void loadButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_loadButtonMouseClicked
    if (evt.getClickCount() == 2) {
        handleOk();
    }
}//GEN-LAST:event_loadButtonMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JLabel bannerLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton createButton;
    private javax.swing.JLabel createIcon;
    private javax.swing.ButtonGroup createLoadSampleGroup;
    private javax.swing.JPanel createLoadSamplePanel;
    private javax.swing.JRadioButton loadButton;
    private javax.swing.JLabel loadIcon;
    private javax.swing.JPanel loadPanel;
    private javax.swing.JRadioButton loadSampleButton;
    private javax.swing.JLabel loadSampleIcon;
    private javax.swing.JLabel motifLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel tipLabel;
    private javax.swing.JPanel tipPanel;
    // End of variables declaration//GEN-END:variables

}
