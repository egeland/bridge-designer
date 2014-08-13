/*
 * DraftingGrid.java  
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

import java.awt.Point;

/**
 * Drafting grid of discretely variable density.
 * 
 * @author Eugene K. Ressler
 */
public class DraftingGrid {
    /**
     * Allowable snap grid densities expressed in grid coordinate units.  Indexed by <code>xx_GRID</code> flags.
     */
    private static final int snapMultiples[] = { 4, 2, 1 };
    /**
     * Constant corresponding to the coarse drawing grid.
     */
    public static final int COARSE_GRID = 0;
    /**
     * Constant corresponding to the medium density drawing grid.
     */
    public static final int MEDIUM_GRID = 1;
    /**
     * Constant corresponding to the fine drawing grid.
     */
    public static final int FINE_GRID = 2;
    /**
     * Maximum valid snap multiple, corresponding to coarsest grid.
     */
    protected static final int maxSnapMultiple = snapMultiples[0];
    /**
     * Current snap multiple, which, multiplied by <code>fineGridSize</code>, is the grid density in world coordinates.
     */
    protected int snapMultiple = maxSnapMultiple;
    /**
     * Size of grid elements in world coordinates, which are meters.
     */
    protected static final double fineGridSize = 0.25;

    /**
     * Construct a new drafting grid with the given grid density.
     * 
     * @param density
     */
    public DraftingGrid(int density) {
        snapMultiple = snapMultiples[density];
    }
    
    /**
     * Set the density in the grid coordinate system of the snap grid to one of the predetermined increments.
     * 
     * @param density one of <code>COARSE_GRID</code>, <code>MEDIUM_GRID</code>, or <code>FINE_GRID</code>.
     */
    public void setDensity(int density) {
        snapMultiple = snapMultiples[density];
    }

    /**
     * Convert a snap multiple to the corresponding density flag or return -1 if the given integer is
     * not a valid snap multiple.
     * 
     * @param snapMultiple snap multiple
     * @return density flag
     */
    public static int toDensity(int snapMultiple) {
        for (int i = 0; i < snapMultiples.length; i++) {
            if (snapMultiple == snapMultiples[i]){
                return i;
            }
        }
        return -1;        
    }
    
    /**
     * Return true iff the given density flag represents a finer grid than the current one.
     * 
     * @param density grid density to test
     * @return true iff density represents a finer grid than the current one
     */
    public boolean isFiner(int density) {
        return snapMultiples[density] < snapMultiple;
    }

    /**
     * Return the density flag olf the current grid.
     * 
     * @return density flag
     */
    public int getDensity() {
        return toDensity(snapMultiple);
    }
    
    /**
     * Return the size of the current grid in world coordinates.
     * 
     * @return distance between grid points in world coordinates
     */
    public double getGridSize() {
        return fineGridSize * snapMultiple;
    }
    
    /**
     * Set the snap multiple for the current grid. 
     * 
     * @param snapMultiple snap multiple
     */
    public void setSnapMultiple(int snapMultiple) {
        this.snapMultiple = snapMultiple;
    }
    
    /**
     * Return the snap multiple of the current grid.
     * 
     * @return snap multiple
     */
    public int getSnapMultiple() {
        return snapMultiple;
    }
    
    /**
     * Snap the given coordinate to the fine grid and then return the snap multiple
     * of the coarsest grid that includes the resulting coordinate.
     * 
     * @param c coordinate
     * @return snap multiple
     */
    public static int snapMultipleOf(double c) {
        final int grid = (int)Math.round(c / fineGridSize);
        final int lsb = grid & ~(grid - 1);
        return (lsb == 0) ? maxSnapMultiple : (lsb > maxSnapMultiple) ? maxSnapMultiple : lsb;
    }

    /**
     * Return the current grid x-coordinate that's closest to the given world x-coordinate.
     * 
     * @param xWorld world x-coordinate
     * @return grid x-coordinate
     */
    public int worldToGridX(double xWorld) {
        return snapMultiple * (int)Math.round(xWorld / (fineGridSize * snapMultiple));
    }
    
    /**
     * Return the current grid x-coordinate that's closest to the given world x-coordinate.
     * 
     * @param yWorld world y-coordinate
     * @return grid y-coordinate
     */
    public int worldToGridY(double yWorld) {
        return snapMultiple * (int)Math.round(yWorld / (fineGridSize * snapMultiple));
    }

    /**
     * Set the destination point to the grid coordinate point closest to the given world point.
     * 
     * @param dst grid coordinate point
     * @param src world coordinate point
     */
    public void worldToGrid(Point dst, Affine.Point src) {
        dst.x = worldToGridX(src.x);
        dst.y = worldToGridY(src.y);
    }
    
    /**
     * Return the graduation level of an arbitrary grid coordinate. The graduation level is 0 for points 
     * that lie only on the finest grid, 1 for those only on the next coarser in addition to the finest, etc.
     * Used to choose ruler tick heights.
     * 
     * @param grid grid coordinate
     * @return graduation level
     */
    static public int graduationLevel(int grid) {
        int mask = 0x3;
        int level = 2;
        while ((mask & grid) != 0) {
            mask >>= 1;
            --level;
        }
        return level;
    }
    
    /**
     * Return the world x-coordinate corresponding to the given grid x-coordinate.
     * 
     * @param xGrid grid x-coordinate
     * @return world x-coordinate
     */
    public double gridToWorldX(int xGrid) {
        return xGrid * fineGridSize;
    }
    
    /**
     * Return the world y-coordinate corresponding to the given grid y-coordinate.
     * 
     * @param yGrid grid y-coordinate
     * @return world y-coordinate
     */
    public double gridToWorldY(int yGrid) {
        return yGrid * fineGridSize;
    }
    
    /**
     * Set the destination point to the world coordinate point closest to the given grid point.
     * 
     * @param dst world coordinate point
     * @param src grid coordinate point
     */
    public void gridToWorld(Affine.Point dst, Point src) {
        dst.x = gridToWorldX(src.x);
        dst.y = gridToWorldY(src.y);
    }
}
