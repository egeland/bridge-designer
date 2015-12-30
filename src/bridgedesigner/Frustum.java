package bridgedesigner;

import com.jogamp.opengl.GL2;

/**
 * @author Gene Ressler
 * 
 * A frustum class that knows about trapezoidal shadow maps.
 *
 * The TSM algorithm is patented and used by permission of the authors.
 *
 * ATTRIBUTION NOTICE
 * Anti-aliasing and Continuity with Trapezoidal Shadow Maps.
 *
 * This is a new shadow map technique termed trapezoidal shadow maps that
 * calculates high quality shadows in real-time applications.
 *
 * Copyright (C) 2004 Martin Tobias, Tiow-Seng Tan, School of Computing,
 * National University of Singapore.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Author details:
 * 
 * Tiow Seng Tan (tants@comp.nus.edu.sg)
 * Department of Computer Science 13 Computing Drive Singapore 117417
 *
 * Tobias Martin (tobiasmartin@t-online.de)
 * 1985 Heritage Center Salt Lake City, UT 84112 USA
 */
public class Frustum {

    private static Homogeneous.Point [] arrayOf3dPoints(int size) {
        Homogeneous.Point [] a = new Homogeneous.Point[size];
        for (int i = 0; i < size; i++) {
            a[i] = new Homogeneous.Point();
        }
        return a;
    }
    private static Affine.Point [] arrayOf2dPoints(int size) {
        Affine.Point [] a = new Affine.Point[size];
        for (int i = 0; i < size; i++) {
            a[i] = new Affine.Point();
        }
        return a;
    }

    private class Pyramid {

        private final Homogeneous.Point vCanon [] = arrayOf3dPoints(10);
        private final Affine.Point vActual [] = arrayOf2dPoints(10);
        private final ConvexHullFactory hullFactory = new ConvexHullFactory();

        public void set(float left, float right, float bottom, float top, float near, float far) {
            final float zn = -near;
            vCanon[0].set(right, bottom, zn);
            vCanon[1].set(right, top, zn);
            vCanon[2].set(left, top, zn);
            vCanon[3].set(left, bottom, zn);
            float zf = -far;
            float r = zf / zn;
            float farLeft = r * left;
            float farRight = r * right;
            float farBottom = r * bottom;
            float farTop = r * top;
            vCanon[4].set(farRight, farBottom, zf);
            vCanon[5].set(farRight, farTop, zf);
            vCanon[6].set(farLeft, farTop, zf);
            vCanon[7].set(farLeft, farBottom, zf);
            // Points 8 and 9 are at the middle of the near and far planes.
            vCanon[8].set(.5f * (right + left), .5f * (top + bottom), zn);
            vCanon[9].set(.5f * (farRight + farLeft), .5f * (farTop + farBottom), zf);
        }

        public Affine.Point[] getHull(Affine.Point[] hull, Homogeneous.Matrix xForm) {
            // Transform the frustum to 2d normalized device coordinates.
            hullFactory.clear();
            for (int i = 0; i < vCanon.length; i++) {
                // Hand code 2d parallel mm for a bit of speed.
                vActual[i].x = xForm.a[ 0] * vCanon[i].a[0] +
                               xForm.a[ 4] * vCanon[i].a[1] +
                               xForm.a[ 8] * vCanon[i].a[2] +
                               xForm.a[12] * vCanon[i].a[3];
                vActual[i].y = xForm.a[ 1] * vCanon[i].a[0] +
                               xForm.a[ 5] * vCanon[i].a[1] +
                               xForm.a[ 9] * vCanon[i].a[2] +
                               xForm.a[13] * vCanon[i].a[3];
                // Perspective division for point light source would go here.
                hullFactory.add(vActual[i]);
            }
            return hullFactory.getHull(hull);
        }

        public Affine.Point getNearCenter() {
            return vActual[8];
        }

        public Affine.Point getFarCenter() {
            return vActual[9];
        }
        
        public void getAxis(Affine.Vector axis) {
            axis.diffInto(vActual[9], vActual[8]);
            double r = 1 / axis.length();
            if (Double.isInfinite(r)) {
                // Use an edge of the far plane as the axis direction.
                axis.diffInto(vActual[5], vActual[4]);
                r = 1 / axis.length();
            }
            axis.scaleInPlace(r);
        }

        public void draw(GL2 gl) {
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i < 4; i++) {
                gl.glVertex3fv(vCanon[i].a, 0);
            }
            gl.glEnd();

            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 4; i < 8; i++) {
                gl.glVertex3fv(vCanon[i].a, 0);
            }
            gl.glEnd();

            gl.glBegin(GL2.GL_LINES);
            for (int i = 0; i < 4; i++) {
                gl.glVertex3fv(vCanon[i].a, 0);
                gl.glVertex3fv(vCanon[i + 4].a, 0);
            }
            gl.glEnd();
        }
    }
    // Frustum parameters.
    private float left, right, bottom, top, near, far;

    // Allocate all this stuff once so we aren't doing it 60 x per second.
    private final Pyramid frustum = new Pyramid();
    private final Pyramid focusArea = new Pyramid();

    private final Homogeneous.Matrix tmpMatrix = new Homogeneous.Matrix();
    private final Affine.Vector tmpVector = new Affine.Vector();
    private final Homogeneous.Matrix invModelView = new Homogeneous.Matrix();
    private final Affine.Point [] trapezoid = arrayOf2dPoints(4);
    private final Affine.Vector unitL = new Affine.Vector();
    private final Affine.Vector unitR = new Affine.Vector();
    // Extra element below is required null sentinel.
    private final Affine.Point [] frustumHull = new Affine.Point[7];
    private final Affine.Point [] focusHull = new Affine.Point[7];
    private final Affine.Point [] focusProjected = arrayOf2dPoints(6);
    private final Affine.Vector axisDirection = new Affine.Vector();
    private final Affine.Point pointQ = new Affine.Point();

    // Parameters of trapazoid base and top lines wrt unit ray from
    // near plane center point toward far plane center point.  Also
    // parameter of top of focus area frustum.
    private double tBase, tTop, tFocus;
    
    void set(float fovy, float aspect, float near, float far, float focusRatio) {
        // Remember the parameters for glFrustum
        this.near = near;
        this.far = far;
        this.top = near * (float) Math.tan(fovy * Math.PI / 360.0);
        this.bottom = -this.top;
        this.left = aspect * this.bottom;
        this.right = aspect * this.top;

        // Set up the pyramids for the trapazoid algorithm. DEBUG: far *= 0.5; ?
        far *= 0.35; // fudge factor good for this scenario only.
        frustum.set(left, right, bottom, top, near, far);
        focusArea.set(left, right, bottom, top, near, focusRatio * far + (1f - focusRatio) * near);
    }

    /**
     * Apply the current view frustum transformation in the given OpenGL context.
     *
     * @param gl GL2 context to use
     */
    void apply(GL2 gl) {
        gl.glFrustum(left, right, bottom, top, near, far);
    }

    /**
     * Draw the frustum, hull, and trapezoid for debugging.
     *
     * @param gl GL2 context for drawing
     * @param lightView transformation to light space
     * @param lightProjection projection for drawing (not trapezoidal)
     */
    public void draw(GL2 gl, Homogeneous.Matrix lightView,
                            Homogeneous.Matrix lightProjection) {

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadMatrixf(lightProjection.a, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        // Convex hull.
        gl.glDepthFunc(GL2.GL_ALWAYS);
        gl.glColor3f(1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; frustumHull[i] != null; i++) {
            gl.glVertex3d(frustumHull[i].x, frustumHull[i].y, -100.0);
        }
        gl.glEnd();

        // Focus area.
        gl.glColor3f(1f, 0f, 1f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; focusHull[i] != null; i++) {
            gl.glVertex3d(focusHull[i].x, focusHull[i].y, -100.0);
        }
        gl.glEnd();

        // Trapezoid.
        gl.glColor3f(0f, 1f, 0f);
        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; i < 4; i++) {
            gl.glVertex3d(trapezoid[i].x, trapezoid[i].y, -100.0);
        }
        gl.glEnd();

        // Frustum transformed to world coordinates.
        tmpMatrix.multiplyInto(lightView, invModelView);
        gl.glLoadMatrixf(tmpMatrix.a, 0);
        gl.glColor3f(.2f, .2f, 1f);
        frustum.draw(gl);
        gl.glColor3f(0f, 1f, 1f);
        focusArea.draw(gl);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        // Restore state.
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void getTrapezoidalProjection(Homogeneous.Matrix m,
                                         Homogeneous.Matrix modelView,
                                         Homogeneous.Matrix lightView,
                                         float near, float far) {
        // Form the matrix that takes a canonical view volume to PPSL.
        invModelView.invertInto(modelView, 1f-6);
        tmpMatrix.multiplyInto(lightView, invModelView);

        // Get the convex hull and axis direction of the main frustum.
        frustum.getHull(frustumHull, tmpMatrix);
        frustum.getAxis(axisDirection);

        // Get the convex hull of the truncated frustum that defines the focus area.
        focusArea.getHull(focusHull, tmpMatrix);

        // Project vertices of the frustum hull onto the axis to get
        // top and base distances.
        tmpVector.diffInto(frustumHull[0], frustum.getNearCenter());
        tBase = tTop = axisDirection.dot(tmpVector);
        for (int i = 1; frustumHull[i] != null; i++) {
            tmpVector.diffInto(frustumHull[i], frustum.getNearCenter());
            final double t = axisDirection.dot(tmpVector);
            if (t < tTop) {
                tTop = t;
            }
            else if(t > tBase) {
                tBase = t;
            }
        }

        // Focus distance is just the length of the focus area frustum.
        tmpVector.diffInto(focusArea.getFarCenter(), focusArea.getNearCenter());
        tFocus = axisDirection.dot(tmpVector);

        // Get lambda and delta prime as in the paper.
        final double lambda = tBase - tTop;
        final double deltaPrime = tFocus - tTop;

        // Rename trapazoid points for convenience.
        final Affine.Point p0 = trapezoid[0];
        final Affine.Point p1 = trapezoid[1];
        final Affine.Point p2 = trapezoid[2];
        final Affine.Point p3 = trapezoid[3];
        final Affine.Vector a = axisDirection;

        double xi = -0.6; // the 80% line location
        // Just assume 1024 lines in depth buffer; close enough.
        double xiInc = 2.0 / 1024.0;  
        double lastArea = 0.0;
        // Iterate looking for a maximum in area of projected focus hull.
        double m00, m01, m02, m10, m11, m12, m20, m21, m22;
        do {
            // Compute eta as in the paper.
            final double eta =
                    (lambda * deltaPrime * (1 + xi))
                    / (lambda - 2 * deltaPrime - lambda * xi);

            if (eta > lambda * 100) {
                // This happens when the focus area is near the middle of
                // the far plane because the camera is pointing nearly
                // parallel to light. Find the bounding box aligned with the axis.
                double orthoL = 0.0;
                double orthoR = 0.0;
                pointQ.offsetInto(frustum.getNearCenter(), a, tTop);
                for (int i = 0; frustumHull[i] != null; i++) {
                    tmpVector.diffInto(frustumHull[i], pointQ);
                    double ortho = a.cross(tmpVector);
                    if (ortho > orthoL) {
                        orthoL = ortho;
                    }
                    else if (ortho < orthoR) {
                        orthoR = ortho;
                    }
                }
                p0.orthoOffsetInto(pointQ, a, orthoL);
                p1.orthoOffsetInto(pointQ, a, orthoR);
                pointQ.offsetInto(frustum.getNearCenter(), a, tBase);
                p2.orthoOffsetInto(pointQ, a, orthoR);
                p3.orthoOffsetInto(pointQ, a, orthoL);
            }
            else {
                // Otherwise we have a normal frustum calculation.
                // Find Q by offset from the near plane center.
                pointQ.offsetInto(frustum.getNearCenter(), axisDirection, tTop - eta);

                // Walk around the convex hull to find extreme left and right
                // rays from Q that include all points.  Save unit vectors for these.
                double crossL = 0.0;
                double crossR = 0.0;
                for (int i = 0; frustumHull[i] != null; i++) {
                    tmpVector.diffInto(frustumHull[i], pointQ);
                    tmpVector.scaleInPlace(1 / tmpVector.length());
                    double cross = axisDirection.cross(tmpVector);
                    if (cross > crossL) {
                        crossL = cross;
                        unitL.setLocation(tmpVector);
                    }
                    else if (cross < crossR) {
                        crossR = cross;
                        unitR.setLocation(tmpVector);
                    }
                }
                // Now we can compute the trapazoid boundary.
                final double dotLi = 1 / axisDirection.dot(unitL);
                final double dotRi = 1 / axisDirection.dot(unitR);
                final double tt = eta;
                final double tb = eta + lambda;
                p0.offsetInto(pointQ, unitL, tt * dotLi);
                p1.offsetInto(pointQ, unitR, tt * dotRi);
                p2.offsetInto(pointQ, unitR, tb * dotRi);
                p3.offsetInto(pointQ, unitL, tb * dotLi);
            }

            // Build the 3x3 matrix for 2d mapping.
            m00 = a.y;
            m01 = -a.x;
            m02 = a.x * p0.y - a.y * p0.x;                   // (23)
            m10 = a.x;
            m11 = a.y;
            m12 = -(a.x * p0.x + a.y * p0.y);
            double xc3 = m00 * p3.x + m01 * p3.y + m02;      // (24)
            double yc3 = m10 * p3.x + m11 * p3.y + m12;
            double s = -xc3 / yc3;                           // (25)
            m00 += s * m10;
            m01 += s * m11;
            m02 += s * m12;                                  // (27)
            double xd1 = m00 * p1.x + m01 * p1.y + m02;      // (28)
            double xd2 = m00 * p2.x + m01 * p2.y + m02;
            double d = yc3 / (xd2 - xd1);       // yd2 = yc3 in (29)
            if (0 <= d && d < 1e4) {
                d *= xd1;                             // finish (29)
                m12 += d;
                double sx = 2 / xd2;
                double sy = 1 / (yc3 + d);    // ye2=yd2=yc3 in (31)
                double u = (2 * (sy * d)) / (1 - (sy * d));  // (34)
                m20 = m10 * sy;
                m21 = m11 * sy;
                m22 = m12 * sy;                              // (38)
                m10 = (u + 1) * m20;
                m11 = (u + 1) * m21;
                m12 = (u + 1) * m22 - u;
                m00 = sx * m00 - m20;
                m01 = sx * m01 - m21;
                m02 = sx * m02 - m22;
            } else {
                double sx = 2 / xd2;
                double sy = 2 / yc3;            // yd2 = yc3 in (41)
                m00 *= sx;
                m01 *= sx;
                m02 = m02 * sx - 1;
                m10 *= sy;
                m11 *= sy;
                m12 = m12 * sy - 1;
                m20 = 0;
                m21 = 0;
                m22 = 1;
            }
            // Project the focus hull using our new transformation.
            int n = 0;
            while (focusHull[n] != null) {
                double w = m20 * focusHull[n].x + m21 * focusHull[n].y + m22;
                focusProjected[n].x = (m00 * focusHull[n].x + m01 * focusHull[n].y + m02) / w;
                focusProjected[n].y = (m10 * focusHull[n].x + m11 * focusHull[n].y + m12) / w;
                n++;
            }
            // Done when this matrix produces a smaller area than the last,
            // which means we're one past peak.
            double area = Affine.getPolygonArea(focusProjected, n);
            if (area < lastArea) {
                // System.err.println("eta=" + eta);
                break;
            }
            lastArea = area;
            xi += xiInc;
        } while (xi < 1);
        // Matrix m is now a mapping to ndc. Fill in rows of the OpenGL format 
        // matrix.  Third row determines front-back clip in depth buffer.
        m.a[ 0] = (float)m00;
        m.a[ 4] = (float)m01;
        m.a[ 8] = 0f;
        m.a[12] = (float)m02;

        m.a[ 1] = (float)m10;
        m.a[ 5] = (float)m11;
        m.a[ 9] = 0f;
        m.a[13] = (float)m12;
        
        m.a[ 2] = 0f;
        m.a[ 6] = 0f;
        m.a[10] = 2f / (near - far);
        m.a[14] = (near + far) / (near - far);

        m.a[ 3] = (float)m20;
        m.a[ 7] = (float)m21;
        m.a[11] = 0f;
        m.a[15] = (float)m22;
    }
}
