/*
 * Utility.java  
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handy utility functions and constants for the Bridge Designer.
 * 
 * @author Eugene K. Ressler
 */
public class Utility {
    /**
     * A small number in the bridge coordinate space.
     */
    public static final double small = 1e-6;
    /**
     * The square of a small number in the bridge coordinate space.
     */
    public static final double smallSq = small * small;
    /**
     * Default length of arrowheads.
     */
    public static final int arrowHeadLength = 8;
    /**
     * Default half-width of arrowheads.
     */
    public static final int arrowHalfHeadWidth = 3;

    /**
     * Return the square of the paramater.
     * 
     * @param x value to square
     * @return the square
     */
    public static float sqr(float x) {
        return x * x;
    }

    /**
     * Return the square of the paramater.
     * 
     * @param x value to square
     * @return the square
     */
    public static double sqr(double x) {
        return x * x;
    }

    /**
     * Return the fourth power of the paramater.
     * 
     * @param x value to raise to the 4th power
     * @return the 4th power
     */
    public static double p4(double x) {
        return sqr(sqr(x));
    }

    /**
     * Return true iff a and b are equal, allowing either or both to be null.
     * 
     * @param a first object
     * @param b second object
     * @return true iff a and b are equal
     */
    public static boolean areEqual(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    /**
     * Slurp the bytes of an entire file into a byte array.
     * 
     * @param file file to read bytes from
     * @return byte array containing all file contents
     * @throws java.io.IOException
     */
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte [] buf = new byte[16];
        int offset = 0;
        while (true) {
            int nRead = is.read(buf, offset, buf.length - offset);
            if (nRead < 0) {
                byte [] rtn = new byte[offset];
                System.arraycopy(buf, 0, rtn, 0, offset);
                is.close();
                return rtn;
            }
            offset += nRead;
            if (offset >= buf.length) {
                byte [] newBuf = new byte[2 * buf.length];
                System.arraycopy(buf, 0, newBuf, 0, buf.length);
                buf = newBuf;
            }
        }
    }
    
    /**
     * Draw and arrowhead using java graphics.
     * 
     * @param g java graphics context
     * @param x0 x-coordinate of arrow tip
     * @param y0 y-coordinate of arrow tip
     * @param x1 x-coordinate of arrow tail establishing arrow direction, but not drawn
     * @param y1 y-coordinate of arrow tail establishing arrow direction, but not drawn
     * @param halfWidth half-width of arrowhead
     * @param length length of arrowhead
     */
    public static void drawArrowhead(Graphics g, int x0, int y0, int x1, int y1, int halfWidth, int length) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        if (dx == 0 && dy == 0)
            return;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ux = dx / len;
        float uy = dy / len;
        float bx = x1 - ux * length;
        float by = y1 - uy * length;
        float dhx = -uy * halfWidth;
        float dhy = ux * halfWidth;
        g.drawLine(x1, y1, Math.round(bx + dhx), Math.round(by + dhy));
        g.drawLine(x1, y1, Math.round(bx - dhx), Math.round(by - dhy));
    }
    
    private static void drawArrowhead(Graphics g, int x0, int y0, int x1, int y1) {
        drawArrowhead(g, x0, y0, x1, y1, arrowHalfHeadWidth, arrowHeadLength);
    }
    
    /**
     * Draw a line secement from (x0,y0) to (x1,y1) and put an arrowhead at (x0,y0).
     * 
     * @param g java graphics context
     * @param x0 x-coordinate of arrow tip
     * @param y0 y-coordinate of arrow tip
     * @param x1 x-coordinate of arrow tail
     * @param y1 y-coordinate of arrow tail
     */
    public static void drawArrow(Graphics g, int x0, int y0, int x1, int y1) {
        g.drawLine(x0, y0, x1, y1);
        drawArrowhead(g, x0, y0, x1, y1);
    }
    
    /**
     * Draw a line secement from (x0,y0) to (x1,y1) and put an arrowheads at both points.
     * 
     * @param g java graphics context
     * @param x0 x-coordinate of arrow tip
     * @param y0 y-coordinate of arrow tip
     * @param x1 x-coordinate of arrow tail
     * @param y1 y-coordinate of arrow tail
     */
    public static void drawDoubleArrow(Graphics g, int x0, int y0, int x1, int y1) {
        g.drawLine(x0, y0, x1, y1);
        drawArrowhead(g, x0, y0, x1, y1);
        drawArrowhead(g, x1, y1, x0, y0);
    }

    /**
     * Draw a line secement from (x0,y0) to (x1,y1) and put an arrowheads at both points.
     * 
     * @param g java graphics context
     * @param x0 x-coordinate of arrow tip
     * @param y0 y-coordinate of arrow tip
     * @param x1 x-coordinate of arrow tail
     * @param y1 y-coordinate of arrow tail
     * @param halfWidth half-width of arrowhead
     * @param length length of arrowhead
     */
    public static void drawDoubleArrow(Graphics g, int x0, int y0, int x1, int y1, int halfWidth, int length) {
        g.drawLine(x0, y0, x1, y1);
        drawArrowhead(g, x0, y0, x1, y1, halfWidth, length);
        drawArrowhead(g, x1, y1, x0, y0, halfWidth, length);
    }

    /**
     * Return the intersection of two line segments p1-p2 and p3-p4 or null if there is none.
     * 
     * @param p1 one end of first line segment
     * @param p2 other end of first line segment
     * @param p3 one end of second line segment
     * @param p4 other end of second line segment
     * @return intersection point or null if none
     */
    public static Affine.Point intersection(Affine.Point p1, Affine.Point p2, Affine.Point p3, Affine.Point p4) {
        return intersection(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
    }
    
    /**
     * Reverse the contents of an array starting at given low index and continuing for given length.
     * 
     * @param aObj array to reverse
     * @param iLo starting index
     * @param len length of array segment to reverse
     */
    public static void reverse(Object aObj, int iLo, int len) {
        Object [] a = (Object [])aObj;
        int iHi = len - 1;
        while (iLo < iHi) {
            Object tmp = a[iLo];
            a[iLo] = a[iHi];
            a[iHi] = tmp;
            iLo++;
            iHi--;
        }
    }

    /**
     * Reverse the contents of an array.
     * 
     * @param aObj array to reverse
     */
    public static void reverse(Object aObj) {
        reverse(aObj, 0, ((Object [])aObj).length);
    }

    /**
     * Return the intersection of two line segments p1-p2 and p3-p4 or null if there is none.
     * 
     * @param x1 x-coordinate of p1
     * @param y1 y-coordinate of p1
     * @param x2 x-coordinate of p2
     * @param y2 y-coordinate of p2
     * @param x3 x-coordinate of p3
     * @param y3 y-coordinate of p3
     * @param x4 x-coordinate of p4
     * @param y4 y-coordinate of p4
     * @return intersection or null if none
     */
    public static Affine.Point intersection(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4) {
        
        final double dx21 = x2 - x1;
        final double dy21 = y2 - y1;
        final double dx43 = x4 - x3;
        final double dy43 = y4 - y3;
        final double d = dy43 * dx21 - dx43 * dy21;
        if (d == 0) {
            return null;
        }
        final double dx13 = x1 - x3;
        final double dy13 = y1 - y3;
        final double uan = dx43 * dy13 - dy43 * dx13;
        final double ubn = dx21 * dy13 - dy21 * dx13;
        final double ua = uan / d;
        final double ub = ubn / d;
        return (0 <= ua && ua <= 1 && 0 <= ub && ub <= 1) ? new Affine.Point(x1 + ua * dx21, y1 + ua * dy21) : null;
    }
    
    /**
     * Look up a float in an array and return the index of the largest value that's no greater than given x.
     * @param x value to search for
     * @param a array to search
     * @return index of greatest value not greater than x
     */
    public static int getIndexOfGreatestNotGreaterThan(float x, float [] a) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (a[mid] < x) {
                if (mid == a.length - 1 || x < a[mid + 1]) {
                    return mid;
                }
                lo = mid + 1;
            }
            else if (x < a[mid]) {
                hi = mid - 1;
            }
            else {
                return mid;
            }
        }
        return -1;
    }
    
    /**
     * Return an array of integers obtained by parsing each of the given array of strings.
     * 
     * @param s array of strings to parse
     * @return corresponding array of integers
     */
    public static int [] mapToInt(String [] s) {
        int [] rtn = new int [s.length];
        for (int i = 0; i < s.length; i++) {
            rtn[i] = Integer.parseInt(s[i]);
        }
        return rtn;
    }

    private static float [] rotationMatrix = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    };
            
    /**
     * Return an OpenGL-compatible rotation matrix (column-major 4x4) given the sine and cosine of 
     * the rotation angle.
     * 
     * @param cos cosine of rotation angle
     * @param sin sine of rotation angle
     * @return rotation matrix
     */
    public static float [] rotateAboutZ(double cos, double sin) {
        rotationMatrix[0] = rotationMatrix[5] = (float)cos;
        rotationMatrix[1] = (float)sin;
        rotationMatrix[4] = -rotationMatrix[1];
        return rotationMatrix;
    }
    
    /**
     * Clamp x to the range [min..max].  
     * That is, return x adjusted to be no smaller than min and no larger than max.
     * If min > max, the result is undefined.
     * 
     * @param x value to clamp
     * @param min minimum value
     * @param max maximum value
     * @return clamped value
     */
    public static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }
    public static float clamp(float x, float min, float max) {
        return Math.max(min, Math.min(max, x));
    }

    private static Dimension maxScreenSize;

    /**
     * Poll all screens in the system and return a dimension that is as high
     * and wide as the highest and widest screen(s).  This is to support
     * allocating a big enough backing store.
     *
     * @return dimension of largest screen in pixels.
     */
    public static Dimension getMaxScreenSize() {
        if (maxScreenSize == null) {
            maxScreenSize = new Dimension(0, 0);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice [] devices = ge.getScreenDevices();
            for (int id = 0; id < devices.length; ++id) {
                GraphicsConfiguration [] gcs = devices[id].getConfigurations();
                for (int ic = 0; ic < gcs.length; ++ic) {
                    Rectangle bounds = gcs[ic].getBounds();
                    if (bounds.width > maxScreenSize.width) {
                        maxScreenSize.width = bounds.width;
                    }
                    if (bounds.height > maxScreenSize.height) {
                        maxScreenSize.height = bounds.height;
                    }
                }
            }
        }
        return maxScreenSize;
    }

    /* Output:
     * 
     * (2.5, 2.5)
     * null
     * null
     * (1.8275862068965516, 3.896551724137931)
     * null
     * (3.5, 1.0)
     * null
     */
    public static void main(String args[]) {
        System.out.println("Max screen size=" + getMaxScreenSize());
        System.out.println(intersection(0.0, 0.0, 5.0, 5.0, 5.0, 0.0, 0.0, 5.0));
        System.out.println(intersection(1.0, 3.0, 9.0, 3.0, 0.0, 1.0, 2.0, 1.0));
        System.out.println(intersection(1.0, 5.0, 6.0, 8.0, 0.5, 3.0, 6.0, 4.0));
        System.out.println(intersection(1.0, 1.0, 3.0, 8.0, 0.5, 2.0, 4.0, 7.0));
        System.out.println(intersection(1.0, 2.0, 3.0, 6.0, 2.0, 4.0, 4.0, 8.0));
        System.out.println(intersection(3.5, 9.0, 3.5, 0.5, 3.0, 1.0, 9.0, 1.0));
        System.out.println(intersection(2.0, 3.0, 7.0, 9.0, 1.0, 2.0, 5.0, 7.0));
  }
}
