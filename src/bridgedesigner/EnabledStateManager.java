/*
 * EnabledStateManager.java  
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

import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.AbstractButton;

/**
 * Manage enabling and disabling of a set of components based on GUI state.
 * Each component is added with an array of booleans, one for each 
 * GUI state.  For a component to be actually enabled, it must be enabled as a
 * component AND the GUI must be in a state where the component is enablable
 * as determined by using the current GUI state to look up a boolean for the
 * component. 
 * 
 * @author Eugene K. Ressler
 */
public class EnabledStateManager {

    private int guiState;
    private int guiStateCount;
    private final HashMap<Component, State> componentToStateMap = new HashMap<Component, State>(42);
        
    private static void setEnabledImpl(Component component, boolean enable) {
        // If we have an abstract button with an Action attached, enable or disable the action
        // instead of the button so that any other attached abstract buttons are also enabled or disabled.
        if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton)component;
            if (button.getAction() != null) {
                button.getAction().setEnabled(enable);
                return;
            }
        }
        component.setEnabled(enable);
    }
    
    private static class State {
        boolean componentEnabled;
        boolean [] enablable;
        State(Component component, boolean[] enablable) {
            this.enablable = enablable;
            this.componentEnabled = component.isEnabled();
        }
    }

    public EnabledStateManager(int guiStateCount) {
        this.guiStateCount = guiStateCount;
    }
    
    public void add(Component component, boolean[] enabledVsGuiState) {
        if (enabledVsGuiState.length != guiStateCount) {
            throw new IllegalArgumentException();
        }
        componentToStateMap.put(component, new State(component, enabledVsGuiState));
    }
    
    public void remove(Component component) {
        componentToStateMap.remove(component);
    }
    
    public void setEnabled(Component component, boolean enable) {
        State state = componentToStateMap.get(component);
        if (state == null) {
            setEnabledImpl(component, enable);
        }
        else {
            setEnabledImpl(component, enable && state.enablable[guiState]);
            state.componentEnabled = enable;
        }
    }
    
    public void setGUIState(int guiState) {
        Iterator<Map.Entry<Component,State>> i = componentToStateMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<Component,State> entry = i.next();
            State state = entry.getValue();
            setEnabledImpl(entry.getKey(), state.componentEnabled && state.enablable[guiState]);
        }
        this.guiState = guiState;
    }
}
