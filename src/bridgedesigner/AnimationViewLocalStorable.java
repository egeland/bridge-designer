/*
 * AnimationViewLocalStorable.java  
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

/**
 * Java bean to store animation view independent variables for persistence.
 * 
 * @author Eugene K. Ressler
 */
public class AnimationViewLocalStorable implements Serializable {

    /**
     * View independent variables.
     */
    public double xEye, yEye, zEye, thetaEye, phiEye;
    /**
     * Scenario tag this view applies to.
     */
    private String scenarioTag;

    /**
     * Get eye elevation angle.
     * 
     * @return eye elevation angle
     */
    public double getPhiEye() {
        return phiEye;
    }

    /**
     * Set eye elevation angle.
     * 
     * @param phiEye eye elevation angle
     */
    public void setPhiEye(double phiEye) {
        this.phiEye = phiEye;
    }

    /**
     * Get eye azimuth angle.
     * 
     * @return eye azimuth angle
     */
    public double getThetaEye() {
        return thetaEye;
    }

    /**
     * Set eye azimuth angle.
     * 
     * @param thetaEye eye azimuth angle
     */
    public void setThetaEye(double thetaEye) {
        this.thetaEye = thetaEye;
    }

    /**
     * Get eye x coordinate.
     * 
     * @return eye x coordinate
     */
    public double getXEye() {
        return xEye;
    }

    /**
     * Set eye x coordinate.
     * 
     * @param xEye eye x coordinate
     */
    public void setXEye(double xEye) {
        this.xEye = xEye;
    }

    /**
     * Get eye y coordinate.
     * 
     * @return eye y coordinate
     */
    public double getYEye() {
        return yEye;
    }

    /**
     * Set eye y coordinate.
     * 
     * @param yEye eye y coordinate
     */
    public void setYEye(double yEye) {
        this.yEye = yEye;
    }

    /**
     * Get eye z coordinate.
     * 
     * @return eye z coordinate
     */
    public double getZEye() {
        return zEye;
    }

    /**
     * Set eye z coordinate.
     * 
     * @param zEye eye z coordinate
     */
    public void setZEye(double zEye) {
        this.zEye = zEye;
    }

    /**
     * Get scenario tag this view is for.
     * 
     * @return scenario tag
     */
    public String getScenarioTag() {
        return scenarioTag;
    }

    /**
     * Set scenario tag this view is for
     * 
     * @param scenarioTag scenario tag
     */
    public void setScenarioTag(String scenarioTag) {
        this.scenarioTag = scenarioTag;
    }

    /**
     * Save this bean to local storage.
     * 
     * @param fileName local storage file name
     */
    public void save(String fileName) {
        BDApp.saveToLocalStorage(this, fileName);
    }
    
    /**
     * Load an instance of this bean from local storage.
     * 
     * @param fileName local storage file name
     * @return instance of this bean
     */
    public static AnimationViewLocalStorable load(String fileName) {
        return (AnimationViewLocalStorable) BDApp.loadFromLocalStorage(fileName);
    }
    
    /**
     * Return a representation of this bean for debugging.
     * 
     * @return string representation
     */
    @Override public String toString() {
        return scenarioTag;
    }
}
