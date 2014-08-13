/*
 * Material.java
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

/**
 * Materials for bridges.
 * 
 * @author Eugene K. Ressler
 */
public class Material {

    private final int index;
    private final String name;
    private final String shortName;
    private final double E;
    private final double Fy;
    private final double density;
    private final double[] cost;

    /**
     * Construct a given material.
     * 
     * @param index index of the material in the inventory
     * @param name name of the material
     * @param shortName short name for the material
     * @param E modulus of elasticity
     * @param Fy yield strength
     * @param density density
     * @param cost cost per unit volume
     */
    public Material(int index, String name, String shortName, double E, double Fy, double density, double[] cost) {
        super();
        this.index = index;
        this.name = name;
        this.shortName = shortName;
        this.E = E;
        this.Fy = Fy;
        this.density = density;
        this.cost = cost;
    }

    /**
     * Return the material's modulus of elasticity.
     * 
     * @return modulus of elasticity
     */
    public double getE() {
        return E;
    }

    /**
     * Return the material's yield strength.
     * 
     * @return yield strength
     */
    public double getFy() {
        return Fy;
    }

    /**
     * Return the material's cost per unit volume.
     * 
     * @param crossSection cross-section of the material.
     * @return cost per unit volume
     */
    public double getCost(CrossSection crossSection) {
        return cost[crossSection.getIndex()];
    }

    /**
     * Return the density of the material.
     * 
     * @return density
     */
    public double getDensity() {
        return density;
    }

    /**
     * Return the index of this material in the inventory table.
     * 
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Return the name of the material.
     * 
     * @return material name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the short form of the material's name.
     * 
     * @return short form of the material's name
     */
    public String getShortName() {
        return shortName;
    }
    
    /**
     * Return the name as the material's string representation.
     * 
     * @return name
     */
    @Override
    public String toString() {
        return name;
    }
}
