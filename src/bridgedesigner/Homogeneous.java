/*
 * Homogeneous.java  
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
 * Homogeneous points and transformations.  Implemented with type
 * float for efficient compatibility with JOGL.
 * 
 * @author Eugene K. Ressler
 */
public class Homogeneous {
    
    /**
     * Homogeneous point.
     */
    public static class Point {
        /**
         * Array of four point components in order x, y, z, w.
         */
        public final float a [] = new float[4];
        
        /**
         * Construct a new homogeneous point at the origin.
         */
        public Point() {
            this(0, 0, 0);
        }
        
        /**
         * Construct a new homogeneous point with the given Euclidean coordinates.
         * 
         * @param x x-coordinate
         * @param y y-coordinate
         * @param z z-coordinate
         */
        public Point(float x, float y, float z) {
            this(x, y, z, 1);
        }

        /**
         * Construct a new homogeneous point with the given homogeneous coordinates.
         * 
         * @param x x-coordinate
         * @param y y-coordinate
         * @param z z-coordinate
         * @param w w-coordinate
         */
        public Point(float x, float y, float z, float w) {
            a[0] = x;
            a[1] = y;
            a[2] = z;
            a[3] = w;
        }

        /**
         * Set new coordinates for this point.
         *
         * @param x x-coordinate
         * @param y y-coordinate
         * @param z z-coordinate
         */
        public void set(float x, float y, float z) {
            a[0] = x;
            a[1] = y;
            a[2] = z;
            a[3] = 1;
        }

        /**
         * Return a new point that is a scaled version of the current one.
         * 
         * @param r scale factor
         * @return scaled point
         */
        public Point scale(float r) {
            return new Point(r * x(), r * y(), r * z(), w());
        }
        /**
         * Return the x-coordinate of this homogeneous point.
         * @return x-coordinate
         */
        public float x() { return a[0]; }
        /**
         * Return the w-coordinate of this homogeneous point.
         * @return w-coordinate
         */
        public float y() { return a[1]; }
        /**
         * Return the z-coordinate of this homogeneous point.
         * @return z-coordinate
         */
        public float z() { return a[2]; }
        /**
         * Return the w-coordinate of this homogeneous point.
         * @return w-coordinate
         */
        public float w() { return a[3]; }
        
        /** 
         * Set the x-coordinate of this point.
         * 
         * @param val x-coordinate value
         */
        public void setX(float val) { a[0] = val; }
        /** 
         * Set the y-coordinate of this point.
         * 
         * @param val y-coordinate value
         */
        public void setY(float val) { a[1] = val; }
        /** 
         * Set the z-coordinate of this point.
         * 
         * @param val z-coordinate value
         */
        public void setZ(float val) { a[2] = val; }
        /** 
         * Set the w-coordinate of this point.
         * 
         * @param val w-coordinate value
         */
        public void setW(float val) { a[3] = val; }

        /** 
         * Set the x-coordinate of this point.
         * 
         * @param val x-coordinate value
         */
        public void setX(double val) { a[0] = (float)val; }
        /** 
         * Set the y-coordinate of this point.
         * 
         * @param val y-coordinate value
         */
        public void setY(double val) { a[1] = (float)val; }
        /** 
         * Set the z-coordinate of this point.
         * 
         * @param val z-coordinate value
         */
        public void setZ(double val) { a[2] = (float)val; }
        /** 
         * Set the w-coordinate of this point.
         * 
         * @param val w-coordinate value
         */
        public void setW(double val) { a[3] = (float)val; }

        /**
         * Use a to transform x into b. Points a and b can't be the same.
         * @param b
         * @param a
         * @param x
         */
        public void multiplyInto(Matrix a, Point x) {
            this.a[0] = a.a[0] * x.a[0] + a.a[4] * x.a[1] + a.a[ 8] * x.a[2] + a.a[12] * x.a[3];
            this.a[1] = a.a[1] * x.a[0] + a.a[5] * x.a[1] + a.a[ 9] * x.a[2] + a.a[13] * x.a[3];
            this.a[2] = a.a[2] * x.a[0] + a.a[6] * x.a[1] + a.a[10] * x.a[2] + a.a[14] * x.a[3];
            this.a[3] = a.a[3] * x.a[0] + a.a[7] * x.a[1] + a.a[11] * x.a[2] + a.a[15] * x.a[3];
        }
        
        public Point normalize() {
            try {
                double r = 1.0 / a[3];
                a[0] *= r;
                a[1] *= r;
                a[2] *= r;
                a[3] = 1.0f;
            }
            catch (Exception ex) {}
            return this;
        }
        
        public float distance(Point other) {
            float dx = this.a[0] - other.a[0];
            float dy = this.a[1] - other.a[1];
            float dz = this.a[2] - other.a[2];
            return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public float dot(Point other) {
            return this.a[0] * other.a[0] +
                   this.a[1] * other.a[1] +
                   this.a[2] * other.a[2];
        }

        @Override
        public String toString() {
            return "(" + a[0] + "," + a[1] + "," + a[2]+ "," + a[3] + ")";
        }
    }

    /**
     * Homogeneours transformation matrix.
     */
    public static class Matrix {
        /**
         * Array of 16 transformation components in column-major order.
         */
        public final float a [] = new float[16];
        
        /**
         * Construct a new matrix with the given components.
         * @param a00 row 0, column 0 component
         * @param a01 row 0, column 1 component
         * @param a02 row 0, column 2 component
         * @param a03 row 0, column 3 component
         * @param a10 row 1, column 0 component
         * @param a11 row 1, column 1 component
         * @param a12 row 1, column 2 component
         * @param a13 row 1, column 3 component
         * @param a20 row 2, column 0 component
         * @param a21 row 2, column 1 component
         * @param a22 row 2, column 2 component
         * @param a23 row 2, column 3 component
         * @param a30 row 3, column 0 component
         * @param a31 row 3, column 1 component
         * @param a32 row 3, column 2 component
         * @param a33 row 3, column 3 component
         */
        public Matrix(
                float a00, float a01, float a02, float a03,
                float a10, float a11, float a12, float a13,
                float a20, float a21, float a22, float a23,
                float a30, float a31, float a32, float a33) {
            // Note we are transposing from row major parameter order to column major internal order.
            a[ 0] = a00; a[ 4] = a01; a[ 8] = a02; a[12] = a03; 
            a[ 1] = a10; a[ 5] = a11; a[ 9] = a12; a[13] = a13; 
            a[ 2] = a20; a[ 6] = a21; a[10] = a22; a[14] = a23; 
            a[ 3] = a30; a[ 7] = a31; a[11] = a32; a[15] = a33;
        }
        
        /**
         * Construct a new matrix with all 16 elements set to zero.
         */
        public Matrix() {  }

        public Matrix(float [] a) {
            System.arraycopy(a, 0, this.a, 0, this.a.length);
        }

        public void set(Matrix m) {
            System.arraycopy(m.a, 0, this.a, 0, 16);
        }

        public float [] getRow(int i) {
            return new float [] { a[i + 0], a[i + 4], a[i + 8], a[i + 12] };
        }

        public void setZero() {
            for (int i = 0; i < a.length; i++) {
                a[i] = 0f;
            }
        }

        public static final float [] Ia = {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        };

        public static Matrix I = new Matrix(Ia);

        public void setIdentity() {
            System.arraycopy(Ia, 0, a, 0, Ia.length);
        }

        public void setTranslation(float dx, float dy, float dz) {
            setIdentity();
            a[12] = dx;
            a[13] = dy;
            a[14] = dz;
        }
        
        /**
         * Transpose the contents of this matrix in place.
         */
        public void transposeInPlace() {
            swap(1, 4);
            swap(2, 8);
            swap(3, 12);
            swap(6, 9);
            swap(7, 13);
            swap(11, 14);
        }

        private void swap(int i, int j) {
            float t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
        
        /**
         * Multiply two existing matrices and assign result into this one. 
         * Neither parameter may be this matrix.
         * 
         * @param x left-hand matrix to multiply
         * @param y right-hand matrix to multiply
         */
        public void multiplyInto(Matrix x, Matrix y) {
            for (int j = 0; j < 16; j += 4) {
                for (int i = 0; i < 4; i++) {
                    a[j + i] = x.a[i +  0] * y.a[j + 0] + 
                               x.a[i +  4] * y.a[j + 1] + 
                               x.a[i +  8] * y.a[j + 2] +
                               x.a[i + 12] * y.a[j + 3];
                }
            }
        }

        void invertInto(Matrix x, float epsilon) {
            float a0 = x.a[ 0] * x.a[ 5] - x.a[ 4] * x.a[ 1];
            float a1 = x.a[ 0] * x.a[ 9] - x.a[ 8] * x.a[ 1];
            float a2 = x.a[ 0] * x.a[13] - x.a[12] * x.a[ 1];
            float a3 = x.a[ 4] * x.a[ 9] - x.a[ 8] * x.a[ 5];
            float a4 = x.a[ 4] * x.a[13] - x.a[12] * x.a[ 5];
            float a5 = x.a[ 8] * x.a[13] - x.a[12] * x.a[ 9];
            float b0 = x.a[ 2] * x.a[ 7] - x.a[ 6] * x.a[ 3];
            float b1 = x.a[ 2] * x.a[11] - x.a[10] * x.a[ 3];
            float b2 = x.a[ 2] * x.a[15] - x.a[14] * x.a[ 3];
            float b3 = x.a[ 6] * x.a[11] - x.a[10] * x.a[ 7];
            float b4 = x.a[ 6] * x.a[15] - x.a[14] * x.a[ 7];
            float b5 = x.a[10] * x.a[15] - x.a[14] * x.a[11];

            float det = a0 * b5 - a1 * b4 + a2 * b3 + a3 * b2 - a4 * b1 + a5 * b0;
            if (Math.abs(det) > epsilon) {
                a[ 0] = +x.a[ 5] * b5 - x.a[ 9] * b4 + x.a[13] * b3;
                a[ 1] = -x.a[ 1] * b5 + x.a[ 9] * b2 - x.a[13] * b1;
                a[ 2] = +x.a[ 1] * b4 - x.a[ 5] * b2 + x.a[13] * b0;
                a[ 3] = -x.a[ 1] * b3 + x.a[ 5] * b1 - x.a[ 9] * b0;
                a[ 4] = -x.a[ 4] * b5 + x.a[ 8] * b4 - x.a[12] * b3;
                a[ 5] = +x.a[ 0] * b5 - x.a[ 8] * b2 + x.a[12] * b1;
                a[ 6] = -x.a[ 0] * b4 + x.a[ 4] * b2 - x.a[12] * b0;
                a[ 7] = +x.a[ 0] * b3 - x.a[ 4] * b1 + x.a[ 8] * b0;
                a[ 8] = +x.a[ 7] * a5 - x.a[11] * a4 + x.a[15] * a3;
                a[ 9] = -x.a[ 3] * a5 + x.a[11] * a2 - x.a[15] * a1;
                a[10] = +x.a[ 3] * a4 - x.a[ 7] * a2 + x.a[15] * a0;
                a[11] = -x.a[ 3] * a3 + x.a[ 7] * a1 - x.a[11] * a0;
                a[12] = -x.a[ 6] * a5 + x.a[10] * a4 - x.a[14] * a3;
                a[13] = +x.a[ 2] * a5 - x.a[10] * a2 + x.a[14] * a1;
                a[14] = -x.a[ 2] * a4 + x.a[ 6] * a2 - x.a[14] * a0;
                a[15] = +x.a[ 2] * a3 - x.a[ 6] * a1 + x.a[10] * a0;

                float invDet = 1f / det;
                for (int i = 0; i < a.length; i++) {
                    a[i] *= invDet;
                }
            }
            else {
                setZero();
            }
        }
        @Override
        public String toString() {
            return "[[" + a[0] + "," + a[4] + "," + a[ 8]+ "," + a[12] + ")]\n" +
                    "[" + a[1] + "," + a[5] + "," + a[ 9]+ "," + a[13] + ")]\n" +
                    "[" + a[2] + "," + a[6] + "," + a[10]+ "," + a[14] + ")]\n" +
                    "[" + a[3] + "," + a[7] + "," + a[11]+ "," + a[15] + ")]]" ;
        }
    }
}
