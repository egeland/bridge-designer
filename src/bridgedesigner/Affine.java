/*
 * Affine.java  
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

import java.awt.geom.Point2D;

/**
 * Simple affine arithmetic with points and vectors in 2d.
 * 
 * @author Eugene K. Ressler
 */
public abstract class Affine {

    /**
     * Affine points with double coordinates.
     */
    public static class Point extends Point2D.Double {

        /**
         * Construct default affine point (uninitialized).
         */
        public Point() {
            x = y = 0;
        }

        /**
         * Construct affine point (x, y).
         * 
         * @param x x-coordinate of new point
         * @param y y-coordinate of new point
         */
        public Point(double x, double y) {
            super(x, y);
        }

        /**
         * Construct an affine equivalent of integer point.
         * 
         * @param p integer point
         */
        public Point(Point p) {
            this(p.x, p.y);
        }

        /**
         * Point-vector addition.
         * 
         * @param v operand
         * @return point, this + v
         */
        public Point plus(Vector v) {
            return new Point(x + v.x, y + v.y);
        }

        /**
         * Point-offset addition.
         * 
         * @param dx x offset
         * @param dy y offset
         * @return new point
         */
        public Point plus(double dx, double dy) {
            return new Point(x + dx, y + dy);
        }
        
        /**
         * Point-point subtraction.
         * 
         * @param p operand
         * @return vector, this - p 
         */
        public Vector minus(Point p) {
            return new Vector(x - p.x, y - p.y);
        }

        /**
         * Point-vector subtraction.
         * 
         * @param v operand
         * @return point, this - v
         */
        public Point minus(Vector v) {
            return new Point(x - v.x, y - v.y);
        }

        /**
         * @return position vector corresponding to this point.
         */
        public Vector position() {
            return new Vector(x, y);
        }

        public Point offsetInto(Point a, Vector v, double t) {
            x = a.x + t * v.x;
            y = a.y + t * v.y;
            return this;
        }

        public Point orthoOffsetInto(Point a, Vector v, double t) {
            x = a.x - t * v.y;
            y = a.y + t * v.x;
            return this;
        }

        /**
         * Return the distance from this point to the line segment between points a and b.
         * 
         * @param a line segement end point
         * @param b the other line segement end point
         * @return distance from this point to the line segment AB
         */
        public double distanceToSegment(Point a, Point b) {
            Vector v = b.minus(a);
            double vDotV = v.dot(v);
            if (vDotV < Utility.smallSq) {
                return this.distance(a);
            }
            Vector u = this.minus(a);
            if (u.dot(v) <= 0) {
                return u.length();
            }
            Vector w = this.minus(b);
            if (w.dot(v) >= 0) {
                return w.length();
            }
            return Math.abs(u.dot(v.perp())) / Math.sqrt(vDotV);
        }

        public Affine.Point interpolateInto(Affine.Point a, Affine.Point b, double t) {
            x = (1 - t) * a.x + t * b.x;
            y = (1 - t) * a.y + t * b.y;
            return this;
        }

        /**
         * Return true iff the point lies on line segment AB.
         * @param a one segment endpoint
         * @param b the other segment endpoint
         * @return true iff the point lies on AB
         */
        public boolean onSegment(Point a, Point b) {
            return !this.equals(a) && !this.equals(b) &&
                    ((a.x <= x && x <= b.x) || (b.x <= x && x <= a.x)) &&
                    ((a.y <= y && y <= b.y) || (b.y <= y && y <= a.y)) &&
                    Math.abs((x - a.x) * (b.y - a.y) - (y - a.y) * (b.x - a.x)) < Utility.smallSq;
        }

        /**
         * Printable representation of an affine point.
         * 
         * @return string representation
         */
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * Affine vectors with double coordinates.
     */
    public static class Vector extends Point2D.Double {

        /**
         * Construct a default affine vector (uninitialized).
         */
        public Vector() {
            x = y = 0;
        }

        /**
         * Construct a vector [x,y].
         * 
         * @param x x-coordinate of new vector
         * @param y y-coordinate of new vector
         */
        public Vector(double x, double y) {
            super(x, y);
        }

        /**
         * Vector addition.
         * 
         * @param v operand
         * @return vector, this + v
         */
        public Vector plus(Vector v) {
            return new Vector(x + v.x, y + v.y);
        }

        /**
         * Return (0,0) + this.
         * 
         * @return point given by this as a position vector.
         */
        public Point toPoint() {
            return new Point(x, y);
        }
 
        /**
         * Vector subtraction.
         * 
         * @param v operand 
         * @return vector, this - v
         */
        public Vector minus(Vector v) {
            return new Vector(x - v.x, y - v.y);
        }

        /**
         * Vector scalar multiplication.
         * 
         * @param a scalar multiplicand
         * @return vector, a times this
         */
        public Vector times(double a) {
            return new Vector(a * x, a * y);
        }

        /**
         * Vector scalar division.  May throw division by zero exception.
         * 
         * @param a scalar dividend
         * @return vector 1/a times this
         */
        public Vector div(double a) {
            return new Vector(x / a, y / a);
        }

        /**
         * Return counter-clockwise perpendicular vector.
         * 
         * @return vector perp(this)
         */
        public Vector perp() {
            return new Vector(-y, x);
        }

        /**
         * Return unary negative of this vector.
         * 
         * @return negative this
         */
        public Vector minus() {
            return new Vector(-x, -y);            
        }
        
        /**
         * Vector dot product.
         * 
         * @param v operand
         * @return this dot v
         */
        public double dot(Vector v) {
            return x * v.x + y * v.y;
        }

        /**
         * Return cross product (this X v ).  Since this is always a parallel to the z-axis, need only 
         * return the signed magnitude.  The value will be postive if the acute angle between this vector and
         * v is counter-clockwise, else negative.
         * 
         * @param v vector to cross with this one
         * @return scalar value of the cross-product
         */
        public double cross(Vector v) {
            return x * v.y - y * v.x;
        }
        
        /**
         * Squared length
         * 
         * @return length(this) squared
         */
        public double lengthSq() {
            return this.dot(this);
        }

        /**
         * Vector length
         * 
         * @return length of this vector
         */
        public double length() {
            return Math.sqrt(lengthSq());
        }

        /**
         * Unit vector
         * 
         * @return unit vector with same direction as this one
         */
        public Vector unit(double c) {
            return this.times(c / this.length());
        }

        public Vector diffInto(Point a, Point b) {
            x = a.x - b.x;
            y = a.y - b.y;
            return this;
        }

        public Vector scaleInPlace(double r)
        {
            x *= r;
            y *= r;
            return this;
        }

        @Override
        public String toString() {
            return "[" + x + ", " + y + "]";
        }
    }

    /**
     * Return the area of a simple polygon with given array of vertex points.
     *
     * @param n number of points in the array
     * @param p array of vertex points
     * @return area of simple polygon
     */
    public static double getPolygonArea(Point [] p, int n) {
        double detSum = 0;
        int j = n - 1;
        for (int i = 0; i < n; j = i++) {
           detSum += p[j].x * p[i].y - p[j].y * p[i].x;
        }
        return 0.5 * detSum;
    }
}