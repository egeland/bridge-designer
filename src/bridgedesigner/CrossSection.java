/*
 * CrossSection.java  
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
 * Representation of one cross-section of member stock.
 * 
 * @author Eugene K. Ressler
 */
public abstract class CrossSection {

    /**
     * Index of this cross-section in the inventory where it belongs.
     */
    protected final int index;
    /**
     * Full name of the cross-section.
     */
    protected final String name;
    /**
     * Short form name of the cross-section.
     */
    protected final String shortName;
    /**
     * Table of widths taking an index to corresponding width in millimeters.
     */
    protected int[] widths = new int[]{
        30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80,                /* 0 to 10 */
        90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, /* 11 to 22 */
        220, 240, 260, 280, 300,                                   /* 23 to 27 */
        320, 340, 360, 400, 500                                    /* 28 to 32 */
    };

    /**
     * Construct a cross section object.
     * 
     * @param index index of this cross section
     * @param name name of this cross section
     * @param shortName short name for this cross section
     */
    public CrossSection(int index, String name, String shortName) {
        super();
        this.index = index;
        this.name = name;
        this.shortName = shortName;
    }

    /**
     * Get a table of shapes with this cross section indexed by size.
     * 
     * @return table of shapes
     */
    public abstract Shape[] getShapes();

    /**
     * Get 0-based index of this section.
     * 
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Count of sizes of members with this cross section.
     * 
     * @return number of sizes
     */
    public int getNSizes() {
        return widths.length;
    }

    /**
     * Get the name of this cross section.
     * 
     * @return cross section name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the short name of this cross section.
     * 
     * @return short cross section name
     */
    public String getShortName() {
        return shortName;
    }
    
    /**
     * Return short name of this cross section as string rep.  Used for tip text.
     * 
     * @return short cross section name
     */
    @Override public String toString() {
        return shortName;
    }
}
