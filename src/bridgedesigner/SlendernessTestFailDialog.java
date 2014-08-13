/*
 * SlendernessTestFailDialog.java
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

import java.awt.Frame;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import org.jdesktop.application.ResourceMap;

/**
 * Dialog to show slenderness test has failed in a bridge and
 * tutorial information on how to correct it.
 * 
 * @author Eugene K. Ressler
 */
public class SlendernessTestFailDialog extends JDialog {

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
     * Create a new slenderness test failure dialog.
     * 
     * @param parent parent frame of the dialog
     */
    public SlendernessTestFailDialog(Frame parent) {
        super(parent, true);
        initComponents();
        getRootPane().setDefaultButton(closeButton);
        Help.getBroker().enableHelpOnButton(helpButton, "hlp_slenderness", Help.getSet());
        update();
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
        update();
    }

    /**
     * Update the animated cartoon and text to match current selected example and point.
     */
    private void update() {
        final String key = getResourceKey();
        // load the correct animation
        ImageIcon icon = BDApp.getApplication().getIconResource(key + ".gif");
        icon.setImageObserver(cartoonLabel);
        cartoonLabel.setIcon(icon);
        // load the correct text
        if (resourceMap != null) {
            exampleTextPane.setText(resourceMap.getString(key + ".text"));
        }
        // disable the back button if we're looking at the first point
        backButton.setEnabled(pointIndex > 0);
    }
    
    /**
     * Return a string key used to name animations and example text.
     * 
     * @return key string
     */
    private String getResourceKey() {
        int pointNumber = pointIndex + 1;
        return "ex5pt"+ pointNumber;
    }

    private int getExampleIndex() {
        return 0;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        examplePanel = new javax.swing.JPanel();
        exampleTextPane = new TipTextPane();
        cartoonLabel = new javax.swing.JLabel();
        nextButton = new javax.swing.JButton();
        backButton = new javax.swing.JButton();
        tipTextPane = new TipTextPane();
        helpButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(SlendernessTestFailDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | java.awt.Font.BOLD, titleLabel.getFont().getSize()+10));
        titleLabel.setText(resourceMap.getString("titleLabel.text")); // NOI18N
        initialize(resourceMap);
        titleLabel.setName("titleLabel"); // NOI18N

        examplePanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        examplePanel.setName("examplePanel"); // NOI18N

        exampleTextPane.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        exampleTextPane.setContentType(resourceMap.getString("exampleTextPane.contentType")); // NOI18N
        exampleTextPane.setName("exampleTextPane"); // NOI18N
        exampleTextPane.setOpaque(false);

        cartoonLabel.setIcon(resourceMap.getIcon("cartoonLabel.icon")); // NOI18N
        cartoonLabel.setText(null);
        cartoonLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        cartoonLabel.setName("cartoonLabel"); // NOI18N

        nextButton.setText(resourceMap.getString("nextButton.text")); // NOI18N
        nextButton.setName("nextButton"); // NOI18N
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        backButton.setText(resourceMap.getString("backButton.text")); // NOI18N
        backButton.setName("backButton"); // NOI18N
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout examplePanelLayout = new javax.swing.GroupLayout(examplePanel);
        examplePanel.setLayout(examplePanelLayout);
        examplePanelLayout.setHorizontalGroup(
            examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(examplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, examplePanelLayout.createSequentialGroup()
                        .addComponent(backButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nextButton))
                    .addGroup(examplePanelLayout.createSequentialGroup()
                        .addComponent(cartoonLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(exampleTextPane, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)))
                .addContainerGap())
        );
        examplePanelLayout.setVerticalGroup(
            examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(examplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exampleTextPane, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                    .addComponent(cartoonLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(examplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nextButton)
                    .addComponent(backButton))
                .addContainerGap())
        );

        tipTextPane.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("tipTextPane.border.title"))); // NOI18N
        tipTextPane.setContentType(resourceMap.getString("tipTextPane.contentType")); // NOI18N
        tipTextPane.setText(resourceMap.getString("tipTextPane.text")); // NOI18N
        tipTextPane.setName("tipTextPane"); // NOI18N
        tipTextPane.setOpaque(false);

        helpButton.setText(resourceMap.getString("helpButton.text")); // NOI18N
        helpButton.setName("helpButton"); // NOI18N

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
                    .addComponent(titleLabel)
                    .addComponent(examplePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tipTextPane, javax.swing.GroupLayout.DEFAULT_SIZE, 381, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(closeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(helpButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(examplePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(helpButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeButton))
                    .addComponent(tipTextPane))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_closeButtonActionPerformed

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JLabel cartoonLabel;
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel examplePanel;
    private javax.swing.JTextPane exampleTextPane;
    private javax.swing.JButton helpButton;
    private javax.swing.JButton nextButton;
    private javax.swing.JTextPane tipTextPane;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables

}
