/*
 * UnstableModelDialog.java
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
 * 
 * Implements the tutorial dialog on model instability.  This is designed so that the entire
 * tutorial lives in resources:
 *  - Animated gifs for the illustrations
 *  - HTML text for the example points
 *  - Meta-data on how many points there are per example.
 * 
 * Created on December 21, 2009, 10:37 PM
 */

package bridgedesigner;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.jdesktop.application.ResourceMap;

/**
 * Dialog denoting analyzed bridge is unstable and giving tutorial information on how to fix it.
 * 
 * @author Eugene K. Ressler
 */
public class UnstableModelDialog extends JDialog {

    private ResourceMap resourceMap;
    /**
     * Array of counts of the number of points for each example.
     */
    private int [] pointCounts;
    /**
     * Index of point currently displayed.
     */
    private int pointIndex = 0;
            
    /** 
     * Create a new unstable model dialog. 
     * 
     * @param parent parent frame of the dialog.
     */
    public UnstableModelDialog(Frame parent) {
        super(parent, true);
        // This will call initialize(resourceMap);
        initComponents();
        getRootPane().setDefaultButton(closeButton);
    }

    /**
     * Initialize the dialog after all components have been set up. This is called
     * by code added in the GUI builder.  We just add tabs and then update the 
     * graphic and text to match.
     * 
     * @param resourceMap resource map passed from initComponents()
     */
    private void initialize(ResourceMap resourceMap) {
        this.resourceMap = resourceMap;
        pointCounts = Utility.mapToInt(resourceMap.getString("exampleCounts.intArray").split(" +"));
        for (int i = 1; i < pointCounts.length; i++) {
            exampleTabs.addTab(resourceMap.getString("examplePanel.TabConstraints.tabTitleMore", i + 1), null, null);
        }
        update();
    }

    /**
     * Return the index of the example currently displayed.  This is just the selected tab index.
     * Note: example number is example index plus one
     * 
     * @return selected example index
     */
    private int getExampleIndex() {
        return exampleTabs.getSelectedIndex();
    }
    
    /**
     * Return a string key used to name animations and example text.
     * 
     * @return key string
     */
    private String getResourceKey() {
        int exampleNumber = getExampleIndex() + 1;
        int pointNumber = pointIndex + 1;
        return "ex" + exampleNumber + "pt"+ pointNumber;
    }
    
    /**
     * Update the animated cartoon and text to match current selected example and point.
     */
    private void update() {
        final String key = getResourceKey();
        // load the correct animation
        ImageIcon icon = BDApp.getApplication().getIconResource(key + ".gif");
        cartoonLabel.setIcon(icon);
        icon.setImageObserver(cartoonLabel);
        // load the correct text
        if (resourceMap != null) {
            exampleTextPane.setText(resourceMap.getString(key + ".text"));
        }
        // disable the back button if we're looking at the first point
        backButton.setEnabled(pointIndex > 0);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        explanationTextPane = new TipTextPane();
        exampleTabs = new javax.swing.JTabbedPane();
        examplePanel = new javax.swing.JPanel();
        cartoonLabel = new javax.swing.JLabel();
        exampleTextPane = new TipTextPane();
        backButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        tipTextPane = new TipTextPane();
        closeButton = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(UnstableModelDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | java.awt.Font.BOLD, titleLabel.getFont().getSize()+10));
        titleLabel.setText(resourceMap.getString("titleLabel.text")); // NOI18N
        titleLabel.setName("titleLabel"); // NOI18N

        explanationTextPane.setContentType(resourceMap.getString("explanationTextPane.contentType")); // NOI18N
        explanationTextPane.setEditable(false);
        explanationTextPane.setText(resourceMap.getString("explanationTextPane.text")); // NOI18N
        explanationTextPane.setName("explanationTextPane"); // NOI18N
        explanationTextPane.setOpaque(false);

        exampleTabs.setName("exampleTabs"); // NOI18N
        exampleTabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                exampleTabsStateChanged(evt);
            }
        });

        examplePanel.setName("examplePanel"); // NOI18N

        cartoonLabel.setIcon(resourceMap.getIcon("cartoonLabel.icon")); // NOI18N
        cartoonLabel.setText(null);
        cartoonLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        cartoonLabel.setDoubleBuffered(true);
        cartoonLabel.setName("cartoonLabel"); // NOI18N

        exampleTextPane.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        exampleTextPane.setContentType(resourceMap.getString("exampleTextPane.contentType")); // NOI18N
        exampleTextPane.setEditable(false);
        exampleTextPane.setName("exampleTextPane"); // NOI18N

        backButton.setText(resourceMap.getString("backButton.text")); // NOI18N
        backButton.setName("backButton"); // NOI18N
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        nextButton.setText(resourceMap.getString("nextButton.text")); // NOI18N
        nextButton.setName("nextButton"); // NOI18N
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout examplePanelLayout = new javax.swing.GroupLayout(examplePanel);
        examplePanel.setLayout(examplePanelLayout);
        examplePanelLayout.setHorizontalGroup(
            examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(examplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(examplePanelLayout.createSequentialGroup()
                        .addComponent(cartoonLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(exampleTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 394, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, examplePanelLayout.createSequentialGroup()
                        .addComponent(backButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nextButton)
                        .addGap(8, 8, 8))))
        );
        examplePanelLayout.setVerticalGroup(
            examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(examplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cartoonLabel)
                    .addComponent(exampleTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addGroup(examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nextButton)
                    .addComponent(backButton))
                .addGap(8, 8, 8))
        );

        exampleTabs.addTab(resourceMap.getString("examplePanel.TabConstraints.tabTitle"), examplePanel); // NOI18N

        initialize(resourceMap);

        tipTextPane.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("tipTextPane.border.title"))); // NOI18N
        tipTextPane.setContentType(resourceMap.getString("tipTextPane.contentType")); // NOI18N
        tipTextPane.setText(resourceMap.getString("tipTextPane.text")); // NOI18N
        tipTextPane.setName("tipTextPane"); // NOI18N
        tipTextPane.setOpaque(false);

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(explanationTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 550, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(titleLabel)
                    .addComponent(exampleTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tipTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 472, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(closeButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(explanationTextPane, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exampleTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(closeButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(tipTextPane, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_closeButtonActionPerformed

private void exampleTabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_exampleTabsStateChanged
    pointIndex = 0;
    update();
}//GEN-LAST:event_exampleTabsStateChanged

private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
    int n = pointCounts[getExampleIndex()];
    pointIndex = (pointIndex + n - 1) % n;
    update();
}//GEN-LAST:event_backButtonActionPerformed

private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
    int n = pointCounts[getExampleIndex()];
    pointIndex = (pointIndex + 1) % n;
    update();
}//GEN-LAST:event_nextButtonActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                UnstableModelDialog dialog = new UnstableModelDialog(new JFrame());
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JLabel cartoonLabel;
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel examplePanel;
    private javax.swing.JTabbedPane exampleTabs;
    private javax.swing.JTextPane exampleTextPane;
    private javax.swing.JTextPane explanationTextPane;
    private javax.swing.JButton nextButton;
    private javax.swing.JTextPane tipTextPane;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables
}
