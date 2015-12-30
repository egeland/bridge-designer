/*
 * AboutBox.java
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
import org.jdesktop.application.Action;

/**
 * "About" information dialog for the Bridge Designer.
 * 
 * @author Eugene K. Ressler
 */
public class AboutBox extends javax.swing.JDialog {

    /**
     * Show the about dialog.
     * 
     * @param parent parent frame.
     */
    public AboutBox(Frame parent) {
        super(parent);
        initComponents();
        sysInfoLabel.setText("JRE " + System.getProperty("java.version"));
        getRootPane().setDefaultButton(closeButton);
        Help.getBroker().enableHelpOnButton(purposesButton, "hlp_purposes", Help.getSet());
        Help.getBroker().enableHelpOnButton(howItWorksButton, "hlp_how_wpbd_works", Help.getSet());
    }

    /**
     * Close thie about box action for the close button
     */
    @Action 
    public void closeAboutBox() {
        setVisible(false);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contributorsTextPane = new TipTextPane();
        developerTextPane = new TipTextPane();
        restrictionsTextPane = new TipTextPane();
        headerLabel = new javax.swing.JLabel();
        rightStackPanel = new javax.swing.JPanel();
        sysInfoLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        javax.swing.JLabel flashImageLabel = new javax.swing.JLabel();
        howItWorksButton = new javax.swing.JButton();
        purposesButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(AboutBox.class);
        setTitle(resourceMap.getString("title")); // NOI18N
        setModal(true);
        setName("aboutBox"); // NOI18N
        setResizable(false);

        contributorsTextPane.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("contributorsTextPane.border.title"))); // NOI18N
        contributorsTextPane.setText(resourceMap.getString("contributorsTextPane.text")); // NOI18N
        contributorsTextPane.setName("contributorsTextPane"); // NOI18N
        contributorsTextPane.setOpaque(false);

        developerTextPane.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("developerTextPane.border.title"))); // NOI18N
        developerTextPane.setText(resourceMap.getString("developerTextPane.text")); // NOI18N
        developerTextPane.setName("developerTextPane"); // NOI18N
        developerTextPane.setOpaque(false);

        restrictionsTextPane.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("restrictionsTextPane.border.title"))); // NOI18N
        restrictionsTextPane.setText(resourceMap.getString("restrictionsTextPane.text")); // NOI18N
        restrictionsTextPane.setName("restrictionsTextPane"); // NOI18N
        restrictionsTextPane.setOpaque(false);

        headerLabel.setIcon(resourceMap.getIcon("headerLabel.icon")); // NOI18N
        headerLabel.setText(resourceMap.getString("headerLabel.text")); // NOI18N
        headerLabel.setName("headerLabel"); // NOI18N

        rightStackPanel.setName("rightStackPanel"); // NOI18N

        sysInfoLabel.setFont(resourceMap.getFont("sysInfoLabel.font")); // NOI18N
        sysInfoLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        sysInfoLabel.setText(null);
        sysInfoLabel.setName("sysInfoLabel"); // NOI18N

        versionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        versionLabel.setText(resourceMap.getString("versionLabel.text")); // NOI18N
        versionLabel.setName("versionLabel"); // NOI18N

        flashImageLabel.setIcon(resourceMap.getIcon("flashImageLabel.icon")); // NOI18N
        flashImageLabel.setAutoscrolls(true);
        flashImageLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        flashImageLabel.setName("flashImageLabel"); // NOI18N

        howItWorksButton.setText(resourceMap.getString("howItWorksButton.text")); // NOI18N
        howItWorksButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        howItWorksButton.setName("howItWorksButton"); // NOI18N

        purposesButton.setText(resourceMap.getString("purposesButton.text")); // NOI18N
        purposesButton.setName("purposesButton"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getActionMap(AboutBox.class, this);
        closeButton.setAction(actionMap.get("closeAboutBox")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N

        javax.swing.GroupLayout rightStackPanelLayout = new javax.swing.GroupLayout(rightStackPanel);
        rightStackPanel.setLayout(rightStackPanelLayout);
        rightStackPanelLayout.setHorizontalGroup(
            rightStackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightStackPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rightStackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rightStackPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(rightStackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(versionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(flashImageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sysInfoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(purposesButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(howItWorksButton, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(closeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        rightStackPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {flashImageLabel, versionLabel});

        rightStackPanelLayout.setVerticalGroup(
            rightStackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightStackPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flashImageLabel)
                .addGap(7, 7, 7)
                .addComponent(versionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sysInfoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(purposesButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(howItWorksButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(closeButton))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contributorsTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 593, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(restrictionsTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 593, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(developerTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 593, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(headerLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rightStackPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(headerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(developerTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(contributorsTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(restrictionsTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(rightStackPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JTextPane contributorsTextPane;
    private javax.swing.JTextPane developerTextPane;
    private javax.swing.JLabel headerLabel;
    private javax.swing.JButton howItWorksButton;
    private javax.swing.JButton purposesButton;
    private javax.swing.JTextPane restrictionsTextPane;
    private javax.swing.JPanel rightStackPanel;
    private javax.swing.JLabel sysInfoLabel;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
}
