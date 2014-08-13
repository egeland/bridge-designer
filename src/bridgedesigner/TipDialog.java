/*
 * TipDialog.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.jdesktop.application.ResourceMap;

/**
 * Tip dialog for the Bridge Designer.  Saves state in local storage so
 * that a different tip will be shown each time this dialog is made visible, and 
 * the user can also elect not to see tips on each startup.  All text is in resources.
 * 
 * @author Eugene K. Ressler
 */
public class TipDialog extends JDialog {

    static private String [] index;
    private int n = -1;
    private boolean showOnStart = true;
    private static final String tipNumberStorageName = "tipNumber.xml";
    private static final String showOnStartStorageName = "showOnStartState.xml";
    
    /** 
     * Create a new tip dialog.
     */
    public TipDialog(Frame parent) {
        super(parent, true);
        initComponents(); 
        getRootPane().setDefaultButton(closeButton);
        tipPane.setOpaque(true);
    }

    /**
     * Show a new tip with index the given increment from the current one.
     * 
     * @param startup
     * @param inc increment to apply to current tip index
     */
    public void showTip(boolean startup, int inc) {
        ResourceMap resourceMap = BDApp.getResourceMap(TipDialog.class);
        Object obj = BDApp.loadFromLocalStorage(showOnStartStorageName);
        if (obj != null) {
            showOnStart = (Boolean)obj;
        }
        if (startup && !showOnStart) {
            return;
        }
        if (index == null) {
            ArrayList<String> tipKeys = new ArrayList<String>();
            Iterator<String> i = resourceMap.keySet().iterator();
            while (i.hasNext()) {
                String nameKey = i.next();
                if (nameKey.endsWith(".tipText")) {
                    tipKeys.add(nameKey);
                }
            }
            index = tipKeys.toArray(new String[tipKeys.size()]);
            Arrays.sort(index, new Comparator<String>() {
                public int compare(String a, String b) {
                    return a.compareTo(b);
                }
            });
        }
        obj = BDApp.loadFromLocalStorage(tipNumberStorageName);
        if (obj != null) {
            n = (Integer)obj;
        }
        n = (n + inc) % index.length;
        if (n < 0) {
            n += index.length;
        }
        BDApp.saveToLocalStorage(new Integer(n), tipNumberStorageName);
        tipPane.setText(resourceMap.getString("tipTemplate.text", resourceMap.getString(index[n])));
        if (!isVisible()) {
            showTipsCheck.setSelected(showOnStart);
            setVisible(true);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        showTipsCheck = new javax.swing.JCheckBox();
        closeButton = new javax.swing.JButton();
        nextTipButton = new javax.swing.JButton();
        backTipButton = new javax.swing.JButton();
        tipScroll = new javax.swing.JScrollPane();
        tipPane = new TipTextPane();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(TipDialog.class);
        setTitle(resourceMap.getString("TipForm.title")); // NOI18N
        setLocationByPlatform(true);
        setModal(true);
        setName("TipForm"); // NOI18N
        setResizable(false);

        showTipsCheck.setText(resourceMap.getString("showTipsCheck.text")); // NOI18N
        showTipsCheck.setToolTipText(resourceMap.getString("showTipsCheck.toolTipText")); // NOI18N
        showTipsCheck.setName("showTipsCheck"); // NOI18N
        showTipsCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showTipsCheckActionPerformed(evt);
            }
        });

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        nextTipButton.setText(resourceMap.getString("nextTipButton.text")); // NOI18N
        nextTipButton.setName("nextTipButton"); // NOI18N
        nextTipButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextTipButtonActionPerformed(evt);
            }
        });

        backTipButton.setText(resourceMap.getString("backTipButton.text")); // NOI18N
        backTipButton.setName("backTipButton"); // NOI18N
        backTipButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backTipButtonActionPerformed(evt);
            }
        });

        tipScroll.setName("tipScroll"); // NOI18N

        tipPane.setMargin(new java.awt.Insets(16, 16, 16, 16));
        tipPane.setName("tipPane"); // NOI18N
        tipScroll.setViewportView(tipPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(showTipsCheck)
                        .addGap(18, 18, 18)
                        .addComponent(backTipButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nextTipButton)
                        .addGap(12, 12, 12)
                        .addComponent(closeButton))
                    .addComponent(tipScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backTipButton)
                    .addComponent(nextTipButton)
                    .addComponent(closeButton)
                    .addComponent(showTipsCheck))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
    setVisible(false); 
}//GEN-LAST:event_closeButtonActionPerformed

private void nextTipButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextTipButtonActionPerformed
    showTip(false, 1);   
}//GEN-LAST:event_nextTipButtonActionPerformed

private void showTipsCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTipsCheckActionPerformed
    showOnStart = showTipsCheck.isSelected();
    if ( !BDApp.saveToLocalStorage(showOnStart, showOnStartStorageName) ) {
        JOptionPane.showMessageDialog(null, 
                "Can't save show on start checkbox state. It will remain the same next time!", 
                "Design Tip of the Day", JOptionPane.WARNING_MESSAGE);
    }
}//GEN-LAST:event_showTipsCheckActionPerformed

private void backTipButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backTipButtonActionPerformed
    showTip(false, -1);
}//GEN-LAST:event_backTipButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backTipButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton nextTipButton;
    private javax.swing.JCheckBox showTipsCheck;
    private javax.swing.JTextPane tipPane;
    private javax.swing.JScrollPane tipScroll;
    // End of variables declaration//GEN-END:variables
}
