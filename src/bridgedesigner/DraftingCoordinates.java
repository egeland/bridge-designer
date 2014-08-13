/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bridgedesigner;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Drafting coordinate system that adds additional constraints to the drafting grid, which are
 * based on design conditions of an attached bridge view.
 * 
 * @author Eugene K. Ressler
 */
public class DraftingCoordinates extends DraftingGrid {
    
    private BridgeView bridgeView;
    private static final double abutmentClearance = 1.0;
    private static final double pierClearance = 1.0;
    private final Point grid = new Point();
        
    /**
     * When getting nearby point, check for all possibilities up to this fixed number 
     * of meters away.  Should be multiple of LCM of possible snap multiples.  Must 
     * be big enough to get past high pier.
     */
    private static final int searchRadiusInMeters = 8;

    /**
     * Construct new drafting coordinates connected to the given bridge view.
     * 
     * @param view bridge view
     */
    public DraftingCoordinates(BridgeView view) {
        super(DraftingGrid.COARSE_GRID);
        bridgeView = view;
    }
    
    /**
     * Get the extent of the bridge view connected to these drafting coordinates.
     * 
     * @return extent 
     */
    public Rectangle.Double getExtent() {
        return bridgeView.getDrawingExtent();
    }

    /**
     * Search the grid in the direction [dx,dy] for the valid point nearest the source that is not already 
     * occupied by a joint. Valid means it's inside the river banks and not on a high pier.  If the search fails, 
     * the destination is set equal to the source.
     * 
     * @param dst search result
     * @param src original point
     * @param dx x-component of search direction
     * @param dy y-component of source direction
     */
    public void getNearbyPointOnGrid(Affine.Point dst, Affine.Point src, int dx, int dy) {
        BridgeModel bridge = bridgeView.getBridgeModel();
        int tryDx = dx;
        int tryDy = dy;
        final int nSearchSteps = searchRadiusInMeters / snapMultiple;
        for (int i = 0; i < nSearchSteps; i++) {
            dst.x = src.x + tryDx * snapMultiple * fineGridSize;
            dst.y = src.y + tryDy * snapMultiple * fineGridSize;
            shiftToNearestValidWorldPoint(dst, grid, dst);
            //If the new point is not the same as the starting point and there is no joint there, we're done.
            if (!dst.equals(src) && (bridge == null || bridge.findJointAt(dst) == null)) {
                return;
            }
            tryDx += dx;
            tryDy += dy;
        }
        dst.setLocation(src);
    }

    /**
     * Set the destination point to be the grid point closest to a givden source point that is valid.
     * Valid means within the design space including river banks and not interfering with
     * abutments or the high pier, if any.
     * 
     * @param dst destination point in world coordinates
     * @param dstGrid destination point in grid coordinates
     * @param src source point in world coordinates
     */
    public void shiftToNearestValidWorldPoint(Affine.Point dst, Point dstGrid, Affine.Point src) {
        
        double x = src.x;
        double y = src.y;
        
        double yTop = getExtent().getMaxY();
        double yBottom = getExtent().getMinY();
        double xLeft = getExtent().getMinX();
        double xRight = getExtent().getMaxX();
        
        // Be safe about testing which world zone we're in.
        final double tol = 0.5 * fineGridSize;
        
        // Adjust for abutments and slope. No worries for arches.
        if (!bridgeView.getConditions().isArch() && y <= tol) {
            xLeft += abutmentClearance;
            xRight -= abutmentClearance;
            double dy = bridgeView.getYGradeLevel() - y;
            double xLeftSlope = bridgeView.getLeftBankX() + 0.5 * dy - 0.5;            
            if (xLeftSlope > xLeft) {
                xLeft = xLeftSlope;
            }
            double xRightSlope = bridgeView.getRightBankX() - 0.5 * dy + 0.5;
            if (xRightSlope < xRight) {
                xRight = xRightSlope;
            }
        }
        
        // Move off high pier.
        if (bridgeView.getConditions().isHiPier()) {
            Affine.Point pierLocation = bridgeView.getPierLocation();
            if (y <= pierLocation.y + tol) {
                if (pierLocation.x - pierClearance <= x && x <= pierLocation.x + pierClearance) {
                    x = (x < pierLocation.x) ? pierLocation.x - pierClearance : pierLocation.x + pierClearance;
                }
            }
        }
        dst.x = x < xLeft ? xLeft : x > xRight ? xRight : x;
        dst.y = y < yBottom ? yBottom : y > yTop ? yTop : y;
        
        // Snap
        worldToGrid(dstGrid, dst);
        gridToWorld(dst, dstGrid);
        
        // If snapping took us out of bounds, move one grid and reconvert.
        if (dst.x < xLeft) {
            dstGrid.x += snapMultiple;
            gridToWorld(dst, dstGrid);
        }
        else if (dst.x > xRight) {
            dstGrid.x -= snapMultiple;
            gridToWorld(dst, dstGrid);
        }
    }
}
