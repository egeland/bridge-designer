/*
 * ConvexHullFactory.java  
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Compute the 2d convex hull of any given set of points.  Use the Graham Scan, 
 * Andrews variation with the Ressler touch of using a deque to store the
 * monotonic subsequences.
 * 
 * @author Eugene K. Ressler
 */
public class ConvexHullFactory {

    private final List<Affine.Point> pts = new ArrayList<Affine.Point>(64);
    private final Deque<Affine.Point> orderedPts = new ArrayDeque<Affine.Point>(64);
    private final List<Affine.Point> hull = new ArrayList<Affine.Point>(64);
    
    /**
     * Add an input point. Creates a new Affine.Point to hold the coordinates.
     * 
     * @param x point x-coordinate
     * @param y point y-coordinate
     */
    public void add(double x, double y) {
        pts.add(new Affine.Point(x, y));
    }
    
    /**
     * Add an input point.
     * 
     * @param pt the point to add
     */
    public void add(Affine.Point pt) {
        pts.add(pt);
    }

    /**
     * Clear all input points.
     */
    public void clear() {
        orderedPts.clear();
        hull.clear();
        pts.clear();
    }

    static private final Comparator<Affine.Point> xThenYComparator =
            new Comparator<Affine.Point>() {
        public int compare(Affine.Point a, Affine.Point b) {
            return (a.x < b.x) ? -1 : (a.x > b.x) ? 1 :
                   (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : 0;
        }
    };

    /**
     * Return the convex hull of the input points as an array of them (not copies of them).
     *
     * @param rtn an array to hold the result if non-null large enough;
     *   else a new array is allocated and returned; if a provided array
     *   is too long, a <code>null</code> follows the last hull point
     * @return vertices of the convex hull in CCW order
     */
    public Affine.Point [] getHull(Affine.Point rtn[]) {
        orderedPts.clear();
        hull.clear();

        // Deal with trivial cases: 0, 1, or 2 points.
        if (pts.size() <= 2) {
            return pts.toArray(new Affine.Point[pts.size()]);
        }

        // Sort the points in x then y.
        // The secondary y-order helps with certain degenerate cases.
        Collections.sort(pts, xThenYComparator);

        // Split about the line from leftmost to rightmost point and add respective semi-hulls to
        // deque on opposite ends.  Could be a bit faster with implicit line equation rather than vector math.
        final Affine.Point leftmost = pts.get(0);
        final Affine.Point rightmost = pts.get(pts.size() - 1);
        final Affine.Vector vLeftRight = rightmost.minus(leftmost);
        Iterator<Affine.Point> ipts = pts.listIterator();
        while (ipts.hasNext()) {
            Affine.Point p = ipts.next();
            if (p == rightmost || vLeftRight.cross(p.minus(leftmost)) > 0) {
                orderedPts.addFirst(p);
            }
            else {
                orderedPts.addLast(p);
            }
        }

        // Copy the deque to form the hull, re-establishing convexity just before each point is copied.
        Iterator<Affine.Point> iOrderedPts = orderedPts.iterator();
        while (iOrderedPts.hasNext()) {
            Affine.Point p0 = iOrderedPts.next();
            makeHullConvex(p0);
            hull.add(p0);
        }
        // Re-establish convexity one more time for final wrapping segment of hull.
        makeHullConvex(orderedPts.peekFirst());
        // Return either the provided array or a new one if the provided one
        // is null or too short.  If a provided array is too long, there is a
        // null after the last point.
        return hull.toArray(rtn == null ? new Affine.Point[hull.size()] : rtn);
    }

    /**
     * Repeatedly remove the final point p1 in the hull under construction
     * if the the directed polyline p2->p1->p0 takes a "right turn".  
     * 
     * @param p0 prospective new head of the hull
     */
    private void makeHullConvex(Affine.Point p0) {
        while (hull.size() >= 2) {
            final Affine.Point p1 = hull.get(hull.size() - 1);
            final Affine.Point p2 = hull.get(hull.size() - 2);
            if ((p1.x - p2.x) * (p0.y - p1.y) - (p1.y - p2.y) * (p0.x - p1.x) > 0) {
                break;
            }
            hull.remove(hull.size() - 1);
        }
    }
    
    public static void main(String args[]) {
        ConvexHullFactory ch = new ConvexHullFactory();
        ch.add(0.0, 0.0);
        ch.add(1.0, 1.0);
        ch.add(0.0, 1.0);
        ch.add(0.25, 0.25);
        ch.add(1.0, 0.0);
        ch.add(0.25, 0.55);
        ch.add(0.5, 1.25);
        ch.add(0.55, 0.25);
        ch.add(0.5, -.25);
        ch.add(1.25, 0.5);
        ch.add(0.5, 0.5);
        ch.add(-.25, 0.5);
        Affine.Point [] ans = null;
        long start = System.nanoTime();
        int n = 1000000;
        for (int i = 0; i < n; i++) {
            ans = ch.getHull(null);
        }
        long stop = System.nanoTime();
        double usec = (stop - start) / 1e3 / n;
        System.out.println("uSec per iteration for " + n + " iterations: " + usec);
        for (int i = 0; i < ans.length; i++) {
            System.out.println(i + ": " + ans[i]);
        }
        System.out.println("Area=" + Affine.getPolygonArea(ans, ans.length));
    }
}
