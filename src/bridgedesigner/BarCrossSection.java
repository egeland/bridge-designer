/*
 * BarCrossSection.java  
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
 * Representation of member bar stock.
 * 
 * @author Eugene K. Ressler
 */
class BarCrossSection extends CrossSection {

    /**
     * Construct a bar cross section object.
     */
    public BarCrossSection() {
        super(0, "Solid Bar", "Bar");
    }

    /**
     * Get an array of all bar cross sections indexed by size
     * 
     * @return shapes of all bar cross sections
     */
    @Override public Shape[] getShapes() {
        int nSizes = widths.length;
        Shape[] s = new Shape[nSizes];
        for (int sizeIndex = 0; sizeIndex < nSizes; sizeIndex++) {
            int width = widths[sizeIndex];
            double area = Utility.sqr(width) * 1e-6;
            double moment = Utility.p4(width) / 12 * 1e-12;
            s[sizeIndex] = new Shape(this, sizeIndex, String.format("%dx%d", width, width), width, area, moment);
        }
        return s;
    }
}
