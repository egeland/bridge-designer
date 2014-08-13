/*
 * BridgeSample.java  
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

import java.util.ArrayList;
import java.util.Iterator;
import org.jdesktop.application.ResourceMap;

/**
 * Sample bridge representation.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeSample {
    /**
     * Name of the sample.
     */
    private String name;
    /**
     * Bridge represented as a string.
     */
    private String bridgeAsString;
    /**
     * List of bridge samples accessible in resources for this class.
     */
    private static Object [] list;

    /**
     * Construct a new bridge sample with given name and bridge represented as a string.
     * 
     * @param name name of the sample
     * @param bridgeAsString bridge represented as a string
     */
    public BridgeSample(String name, String bridgeAsString) {
        this.name = name;
        this.bridgeAsString = bridgeAsString;
    }
    
    /**
     * Return the sample bridge represented as a string.
     * 
     * @return bridge string
     */
    public String getBridgeAsString() {
        return bridgeAsString;
    }

    /**
     * Return the name of this sample.
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Return a string representation of this sample: just its name.
     * 
     * @return name of sample
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Return an array of all samples accessible in resources for this class.  Because the string representation
     * of the sample is its name, this array is suitable for use in a standard Swing list model.
     * 
     * @return array of samples
     */
    public static Object[] getList() {
        if (list == null) {
            final ArrayList<Object> v = new ArrayList<Object>();
            ResourceMap resourceMap = BDApp.getResourceMap(BridgeSample.class);
            Iterator<String> i = resourceMap.keySet().iterator();
            while (i.hasNext()) {
                String nameKey = i.next();
                if (nameKey.endsWith(".bridgeSampleName")) {
                    String sampleKey = nameKey.substring(0, nameKey.lastIndexOf('.')).concat(".bridgeSample");
                    v.add(new BridgeSample(resourceMap.getString(nameKey), resourceMap.getString(sampleKey)));
                }
            }
            list = v.toArray();        
        }
        return list;
    }
}

