/*
 * ComponentStateLocalStorable.java  
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
 */

package bridgedesigner;

import java.io.Serializable;
import javax.swing.JCheckBox;
import javax.swing.JSlider;

/**
 * Java Bean for saving and restoring various kinds of component state to/from local storage.
 * It's a framework and limited implementation only: only doing sliders and checkboxes.  More easily added.
 * 
 * @author Eugene K. Ressler
 */
public class ComponentStateLocalStorable implements Serializable {
    boolean [] checkStates;
    int [] sliderStates;

    /**
     * Construct a storable object.
     */
    public ComponentStateLocalStorable() {
    }

    /**
     * Return the currently represented checkbox states.
     * 
     * @return checkbox states
     */
    public boolean[] getCheckStates() {
        return checkStates;
    }

    /**
     * Set the currently represented checkbox states.
     * 
     * @param checkStates checkbox states
     */
    public void setCheckStates(boolean[] checkStates) {
        this.checkStates = checkStates;
    }

    /**
     * Return the currently represented slider positions.
     * 
     * @return slider states
     */
    public int[] getSliderStates() {
        return sliderStates;
    }

    /**
     * Set the currently represented slider positions
     * 
     * @param sliderStates slider states
     */
    public void setSliderStates(int[] sliderStates) {
        this.sliderStates = sliderStates;
    }
    
    /**
     * Add the states of an array of checkboxes.
     * 
     * @param checkBoxes
     */
    public void add(JCheckBox [] checkBoxes) {
        checkStates = new boolean [checkBoxes.length];
        for (int i = 0; i < checkBoxes.length; i++) {
            checkStates[i] = checkBoxes[i].isSelected();
        }
    }
    
    /**
     * Add the positions of an array of sliders.
     * 
     * @param sliders sliders
     */
    public void add(JSlider [] sliders) {
        sliderStates = new int [sliders.length];
        for (int i = 0; i < sliders.length; i++) {
            sliderStates[i] = sliders[i].getValue();
        }
    }

    /**
     * Apply the currently stored checkbox states to checkboxes.
     * 
     * @param checkBoxes checkboxes
     */
    public void apply(JCheckBox [] checkBoxes) {
        if (checkStates != null && checkStates.length == checkBoxes.length) {
            for (int i = 0; i < checkBoxes.length; i++) {
                 checkBoxes[i].setSelected(checkStates[i]);
            }  
        }
    }
 
    /**
     * Apply the currently stored slider positions to sliders.
     * 
     * @param sliders sliders
     */
    public void apply(JSlider [] sliders) {
        if (sliderStates != null && sliderStates.length == sliders.length) {
            for (int i = 0; i < sliders.length; i++) {
                 sliders[i].setValue(sliderStates[i]);
            }        
        }
    }

    /**
     * Save this storable to local storage for persistence.
     * 
     * @param fileName local storage file
     */
    public void save(String fileName) {
        BDApp.saveToLocalStorage(this, fileName);
    }
    
    /**
     * Create a fresh one of these objects by reading from local storage.
     * 
     * @param fileName local storage file
     * @return new storable containing component states
     */
    public static ComponentStateLocalStorable load(String fileName) {
        return (ComponentStateLocalStorable)BDApp.loadFromLocalStorage(fileName);
    }
}
