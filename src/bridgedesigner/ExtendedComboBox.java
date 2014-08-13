/*
 * ExtendedComboBox.java  
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.jdesktop.application.ResourceMap;

/**
 * Combo box with provisions for spin buttons that change the selection and also for
 * viewing only a suffix of the items in the model lying after a specific base index.
 * Extensions support the Bridge Designer setup wizard.
 *  
 * @author Eugene K. Ressler
 */
public class ExtendedComboBox extends JComboBox {

    private JButton up;
    private JButton down;
    private final ListDataListener listener = new ListDataListener() {

        public void intervalAdded(ListDataEvent e) {
            synchSpinButtons();
        }

        public void intervalRemoved(ListDataEvent e) {
            synchSpinButtons();
        }

        public void contentsChanged(ListDataEvent e) {
            synchSpinButtons();
        }
    };

    /**
     * Construct an extended combo box with no spin buttons.
     */
    public ExtendedComboBox() {
        this(null, null);
    }

    /**
     * Construct an extended combo box with the given spin buttons.
     * 
     * @param up up button
     * @param down down button
     */
    public ExtendedComboBox(JButton up, JButton down) {
        this.up = up;
        this.down = down;
        if (up != null) {
            up.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    incrementIndex(-1);
                }
            });
        }
        if (down != null) {
            down.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    incrementIndex(+1);
                }
            });
        }
        if (up != null || down != null) {
            addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent evt) {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        synchSpinButtons();
                    }
                }
            });
        }
    }

    /**
     * Enable or disable the extended combo box.  If spin buttons are attached, they are also enabled or disabled.
     * 
     * @param b true iff the combo box and spin buttons are to be enabled; else disabled
     */
    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        synchSpinButtons();
    }

    private void incrementIndex(int increment) {
        setSelectedIndex(getSelectedIndex() + increment);
        synchSpinButtons();
    }

    /**
     * Set the state of the spin buttons based on current index by disabling one or both if there
     * is no element in the respective direction.
     */
    private void synchSpinButtons() {
        if (isEnabled()) {
            int selectedIndex = getSelectedIndex();
            if (down != null) {
                down.setEnabled(selectedIndex < getItemCount() - 1);
            }
            if (up != null) {
                up.setEnabled(selectedIndex > 0);
            }
        } else {
            if (down != null) {
                down.setEnabled(false);
            }
            if (up != null) {
                up.setEnabled(false);
            }
        }
    }

    /**
     * Set the selected item using a raw index, ignoring the base index supported by the model.
     * 
     * @param index raw index of item to set
     */
    public void setRawSelectedIndex(int index) {
        setSelectedIndex(index - ((ExtendedComboBoxModel) getModel()).getBase());
    }

    /**
     * Return the raw index of the selected item, ignoring the base index supported by the model.
     * 
     * @return raw index of selected item
     */
    public int getRawSelectedIndex() {
        return getSelectedIndex() + ((ExtendedComboBoxModel) getModel()).getBase();
    }

    /**
     * Set the model of this combobox. Overriden to add a listener that 
     * updates the spin buttons whenever the model changes.
     * 
     * @param model combobox model
     */
    @Override
    public void setModel(ComboBoxModel model) {
        model.addListDataListener(listener);
        super.setModel(model);
    }

    /**
     * Fill this list using strings drawn from resources.
     * 
     * @param keys resources key prefixes to use (".text" is appended by this method)
     * @param mapSrc class containing the resources.
     */
    public void fillUsingResources(String[] keys, Class mapSrc) {
        String[] items = new String[keys.length];
        ResourceMap resourceMap = BDApp.getResourceMap(mapSrc);
        for (int i = 0; i < items.length; i++) {
            items[i] = resourceMap.getString(keys[i] + ".text");
        }
        ExtendedComboBoxModel model = new ExtendedComboBoxModel(items);
        setModel(model);
    }
}
