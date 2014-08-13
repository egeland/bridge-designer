/*
 * Shape.java  
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

/**
 * Shape of inventory stock.
 * 
 * @author Eugene K. Ressler
 */
public class Shape {

    private final int sizeIndex;
    private final CrossSection section;
    private final String name;
    private final double width;
    private final double area;
    private final double moment;
    private final double inverseRadiusOfGyration;
    private final double thickness;

    /**
     * Construct a new solid bar shape with given parameters.
     * 
     * @param section cross-section
     * @param sizeIndex size index
     * @param name name
     * @param width width in millimeters
     * @param area cross-sectional area
     * @param moment moment
     */
    public Shape(CrossSection section, int sizeIndex, String name, double width, double area, double moment) {
        this(section, sizeIndex, name, width, area, moment, width);
    }
    
    /**
     * Construct a new hollow tube shape with given parameters.
     * 
     * @param section cross-section
     * @param sizeIndex size index
     * @param name name
     * @param width width in millimeters
     * @param area cross-sectional area
     * @param moment moment
     * @param thickness wall thickness
     */
    public Shape(CrossSection section, int sizeIndex, String name, double width, double area, double moment, double thickness) {
        this.section = section;
        this.sizeIndex = sizeIndex;
        this.name = name;
        this.width = width;
        this.area = area;
        this.moment = moment;
        this.inverseRadiusOfGyration = Math.sqrt(area / moment);
        this.thickness = thickness;
    }

    /**
     * Return the cross-sectional area of the shape.
     * 
     * @return cross-sectional area
     */
    public double getArea() {
        return area;
    }

    /**
     * Return the moment of the shape.
     * 
     * @return moment
     */
    public double getMoment() {
        return moment;
    }
    
    /**
     * Return the reciprocal of the radius of gyration of the shape.
     * 
     * @return reciprocal of the radius of gyration
     */
    public double getInverseRadiusOfGyration() {
        return inverseRadiusOfGyration;
    }
    
    /**
     * Return the maximum length that a member can be without failing the slenderness ratio test.
     * 
     * @return maximum length that a member can be without failing the slenderness ratio test
     */
    public double getMaxSlendernessLength() {
        return DesignConditions.maxSlenderness / inverseRadiusOfGyration;
    }

    /**
     * Return the name of the shape.
     * 
     * @return name of the shape
     */
    public String getName() {
        return name;
    }

    /**
     * Return the cross-section of the shape.
     * 
     * @return cross-section
     */
    public CrossSection getSection() {
        return section;
    }

    /**
     * Return the index of the size of the shape.
     * 
     * @return index of the size of the shape
     */
    public int getSizeIndex() {
        return sizeIndex;
    }

    /**
     * Return the width of the section millimeters.
     * 
     * @return width of the section in millimeters
     */
    public double getWidth() {
        return width;
    }

    /**
     * Return a nominal width for the shape for text display (not calculation) purposes.
     * 
     * @return nominal width
     */
    int getNominalWidth() {
        return (int) Math.round(width);
    }

    /**
     * Return the wall thickness if this is a tube or width if a solid bar.
     * 
     * @return wall thickness if this is a tube or width if a solid bar.
     */
    double getThickness() {
        return thickness;
    }
    
    /**
     * Return a string representation of this shape.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return name + " mm " + section;
    }
}
