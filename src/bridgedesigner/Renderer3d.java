package bridgedesigner;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;

/**
 *
 * @author Gene Ressler
 */
public class Renderer3d {

    public static final int TRIANGLE_STRIP = 1;
    public static final int TRIANGLE_FAN = 2;
    public static final int LINES = 3;
    public static final int QUAD_STRIP = 4;
    public static final int POLYGON = 5;
    public static final int LINE_STRIP = 6;
    public static final int TRIANGLES = 7;
    public static final int RULED_QUAD_STRIP = 8;
    public static final int RULED_POLYGON = 9;
    public static final int RULE_U = 1;
    public static final int RULE_V = 2;
    public static final int RULE_UV = 4;
    public static final int RULE_VU = 8;
    public static final int GOURAUD_TRIANGLE_STRIP = 10;
    public static final int GOURAUD_TRIANGLE_FAN = 11;
    private final double[] xViewportPoints = new double[32];
    private final double[] yViewportPoints = new double[32];
    private final float [] zViewportPoints = new float[32];
    private final float [] sViewportPoints = new float[32];
    private final int[] xBuf = new int[32];
    private final int[] yBuf = new int[32];
    private final Homogeneous.Matrix M = new Homogeneous.Matrix();
    private final Homogeneous.Matrix T = new Homogeneous.Matrix();
    private final Homogeneous.Matrix[] stack = {
        new Homogeneous.Matrix(),
        new Homogeneous.Matrix(),
        new Homogeneous.Matrix(),};
    private int stackPointer = 0;
    private Homogeneous.Matrix modelTransform = null;
    private int nPoints = 0;
    private int kind = 0;
    private Paint paint = Color.WHITE;
    private Paint rulePaint = Color.BLACK;
    private int parity = 0;
    private boolean cull = false;
    private boolean approximateGouraud = false;
    private int ruleFlags = RULE_U | RULE_V;

    public Renderer3d() {
    }

    private void convert() {
        for (int i = 0; i < nPoints; i++) {
            xBuf[i] = (int) (0.5 + xViewportPoints[i]);
            yBuf[i] = (int) (0.5 + yViewportPoints[i]);
        }
    }

    public void begin(int kind) {
        begin(kind, 0);
    }

    public void begin(int kind, int salvage) {
        nPoints = salvage;
        this.kind = kind;
        parity = 0;
    }

    public void setCulling(boolean cull) {
        this.cull = cull;
    }

    public void setApproximateGouraud(boolean approximateGouraud)
    {
        this.approximateGouraud = approximateGouraud;
    }
    private boolean cull() {
        if (!cull || nPoints <= 2) {
            return false;
        }
        // Z-component part of Newell's algorihm.
        double nz = 0;
        int j = nPoints - 1;
        for (int i = 0; i < nPoints; j = i++) {
            nz += (xViewportPoints[j] - xViewportPoints[i]) * (yViewportPoints[j] + yViewportPoints[i]);
        }
        return nz >= 0;
    }

    public void pushModelTransform() {
        stack[stackPointer++].set(M);
    }

    public void popModelTransform() {
        M.set(stack[--stackPointer]);
    }

    public void restoreModelTransform() {
        M.set(stack[stackPointer - 1]);
    }

    public void enableModelTransform() {
        modelTransform = M;
        M.setIdentity();
    }

    public void enableModelTransform(Homogeneous.Matrix initial) {
        modelTransform = M;
        M.set(initial);
    }

    public void disableModelTransform() {
        modelTransform = null;
    }

    public void prependTransform(Homogeneous.Matrix t) {
        T.set(M);
        M.multiplyInto(t, T);
    }

    public void appendTransform(Homogeneous.Matrix t) {
        T.set(M);
        M.multiplyInto(T, t);
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public void setRuleFlags(int ruleFlags) {
        this.ruleFlags = ruleFlags;
    }

    public void setRulePaint(Paint rulePaint) {
        this.rulePaint = rulePaint;
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform, Homogeneous.Point p) {
        addVertex(g, viewportTransform, p.a[0], p.a[1], p.a[2]);
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform, float[] v, int i) {
        addVertex(g, viewportTransform, v[i + 0], v[i + 1], v[i + 2]);
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform, float[] v, int i, int n) {
        while (n > 0) {
            addVertex(g, viewportTransform, v[i + 0], v[i + 1], v[i + 2]);
            i += 3;
            --n;
        }
        while (n < 0) {
            addVertex(g, viewportTransform, v[i + 0], v[i + 1], v[i + 2]);
            i -= 3;
            ++n;
        }
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform,
            float[] u, int iu,
            float[] v, int iv,
            Paint[] paints, int ip,
            int n) {
        while (n > 0) {
            if (paints != null) {
                paint = paints[ip];
                ++ip;
            }
            addVertex(g, viewportTransform, u[iu + 0], u[iu + 1], u[iu + 2]);
            addVertex(g, viewportTransform, v[iv + 0], v[iv + 1], v[iv + 2]);
            iu += 3;
            iv += 3;
            --n;
        }
        while (n < 0) {
            if (paints != null) {
                paint = paints[ip];
                --ip;
            }
            addVertex(g, viewportTransform, u[iu + 0], u[iu + 1], u[iu + 2]);
            addVertex(g, viewportTransform, v[iv + 0], v[iv + 1], v[iv + 2]);
            iu -= 3;
            iv -= 3;
            ++n;
        }
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform, float x, float y, float z, float s) {
        sViewportPoints[nPoints] = s;
        addVertex(g, viewportTransform, x, y, z);
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform, Homogeneous.Point p, float s) {
        addVertex(g, viewportTransform, p.a[0], p.a[1], p.a[2], s);
    }

    public void addVertex(Graphics2D g, ViewportTransform viewportTransform, float x, float y, float z) {
        if (modelTransform != null) {
            float[] a = modelTransform.a;
            float xp = x * a[0] + y * a[4] + z * a[8] + a[12];
            float yp = x * a[1] + y * a[5] + z * a[9] + a[13];
            float zp = x * a[2] + y * a[6] + z * a[10] + a[14];
            x = xp;
            y = yp;
            z = zp;
        }
        if (!viewportTransform.worldToViewport(xViewportPoints, yViewportPoints, nPoints, x, y, z)) {
            nPoints = 0;
            return;
        }
        zViewportPoints[nPoints] = z; // for fog
        nPoints = nPoints + 1;
        switch (kind) {
            case TRIANGLE_STRIP:
                if (nPoints == 3) {
                    if (!cull()) {
                        g.setPaint(paint);
                        convert();
                        g.fillPolygon(xBuf, yBuf, 3);
                    }
                    xViewportPoints[parity] = xViewportPoints[2];
                    yViewportPoints[parity] = yViewportPoints[2];
                    parity = 1 - parity;
                    nPoints = 2;
                }
                break;
            case GOURAUD_TRIANGLE_STRIP:
                if (nPoints == 3) {
                    if (!cull()) {
                        convert();
                        drawGouraudTriangle(null, g, xBuf, yBuf, sViewportPoints, zViewportPoints);
                    }
                    xViewportPoints[parity] = xViewportPoints[2];
                    yViewportPoints[parity] = yViewportPoints[2];
                    zViewportPoints[parity] = zViewportPoints[2];
                    sViewportPoints[parity] = sViewportPoints[2];
                    parity = 1 - parity;
                    nPoints = 2;
                }
               break;
            case TRIANGLE_FAN:
                if (nPoints == 3) {
                    if (!cull()) {
                        g.setPaint(paint);
                        convert();
                        g.fillPolygon(xBuf, yBuf, 3);
                    }
                    xViewportPoints[1] = xViewportPoints[2];
                    yViewportPoints[1] = yViewportPoints[2];
                    nPoints = 2;
                }
                break;
            case GOURAUD_TRIANGLE_FAN:
                if (nPoints == 3) {
                    if (!cull()) {
                        convert();
                        drawGouraudTriangle(null, g, xBuf, yBuf, sViewportPoints, zViewportPoints);
                    }
                    xViewportPoints[1] = xViewportPoints[2];
                    yViewportPoints[1] = yViewportPoints[2];
                    zViewportPoints[1] = zViewportPoints[2];
                    sViewportPoints[1] = sViewportPoints[2];
                    nPoints = 2;
                }
                break;
            case LINES:
                if (nPoints == 2) {
                    g.setPaint(paint);
                    convert();
                    g.drawLine(xBuf[0], yBuf[0], xBuf[1], yBuf[1]);
                    nPoints = 0;
                }
                break;
            case QUAD_STRIP:
                if (nPoints == 4) {
                    double tx = xViewportPoints[0];
                    double ty = yViewportPoints[0];
                    xViewportPoints[0] = xViewportPoints[2];
                    yViewportPoints[0] = yViewportPoints[2];
                    xViewportPoints[2] = xViewportPoints[1];
                    yViewportPoints[2] = yViewportPoints[1];
                    xViewportPoints[1] = xViewportPoints[3];
                    yViewportPoints[1] = yViewportPoints[3];
                    xViewportPoints[3] = tx;
                    yViewportPoints[3] = ty;
                    if (!cull()) {
                        g.setPaint(paint);
                        convert();
                        g.fillPolygon(xBuf, yBuf, 4);
                    }
                    nPoints = 2;
                }
                break;
            case RULED_QUAD_STRIP:
                if (nPoints == 4) {
                    double tx = xViewportPoints[0];
                    double ty = yViewportPoints[0];
                    xViewportPoints[0] = xViewportPoints[2];
                    yViewportPoints[0] = yViewportPoints[2];
                    xViewportPoints[2] = xViewportPoints[1];
                    yViewportPoints[2] = yViewportPoints[1];
                    xViewportPoints[1] = xViewportPoints[3];
                    yViewportPoints[1] = yViewportPoints[3];
                    xViewportPoints[3] = tx;
                    yViewportPoints[3] = ty;
                    if (!cull()) {
                        g.setPaint(paint);
                        convert();
                        g.fillPolygon(xBuf, yBuf, 4);
                        g.setPaint(rulePaint);
                        if ((ruleFlags & RULE_U) != 0) {
                            g.drawLine(xBuf[3], yBuf[3], xBuf[0], yBuf[0]);
                        }
                        if ((ruleFlags & RULE_V) != 0) {
                            g.drawLine(xBuf[1], yBuf[1], xBuf[2], yBuf[2]);
                        }
                    }
                    nPoints = 2;
                }
                break;
            case TRIANGLES:
                if (nPoints == 3) {
                    if (!cull()) {
                        g.setPaint(paint);
                        convert();
                        g.fillPolygon(xBuf, yBuf, 3);
                    }
                    nPoints = 0;
                }
                break;
        }
    }

    public void end(Graphics2D g) {
        switch (kind) {
            case POLYGON:
            case RULED_POLYGON:
                if (!cull()) {
                    g.setPaint(paint);
                    convert();
                    g.fillPolygon(xBuf, yBuf, nPoints);
                    if (kind == RULED_POLYGON) {
                        g.setPaint(rulePaint);
                        g.drawPolygon(xBuf, yBuf, nPoints);
                    }
                }
                nPoints = 0;
                break;
            case LINE_STRIP:
                if (!cull()) {
                    g.setPaint(paint);
                    convert();
                    g.drawPolyline(xBuf, yBuf, nPoints);
                }
                nPoints = 0;
        }
    }
    
    private int [] gouraudColor;

    public void setGouraudColor(int [] color) {
        gouraudColor = color;
    }

    // Larger values cause fog to close at less negative z values.
    private static final float cFog = 0.0025f;

    public void drawGouraudTriangle(Component c, Graphics2D g, int [] x, int [] y, float [] s, float [] z) {
        
        if (approximateGouraud) {
            float sAvg = (s[0] + s[1] + s[2]) * 0.333333f;
            float zAvg = (z[0] + z[1] + z[2]) * 0.333333f;
            float f = zAvg >= 0 ? 0 : (cFog * zAvg) / (cFog * zAvg - 1);
            Color color = new Color(
                (int)((1f - f) * sAvg * gouraudColor[0] + f * 192f),
                (int)((1f - f) * sAvg * gouraudColor[1] + f * 255f),
                (int)((1f - f) * sAvg * gouraudColor[2] + f * 255f));
            g.setPaint(color);
            g.fillPolygon(x, y, 3);
            return;
        }
        
        // sort by intensity
        int i0 = 0;
        int i1 = 1;
        int i2 = 2;
        if (s[i1] < s[i0]) {
            int t = i0; i0 = i1; i1 = t;
        }
        if (s[i2] < s[i0]) {
            int t = i0; i0 = i2; i2 = t;
        }
        if (s[i2] < s[i1]) {
            int t = i1; i1 = i2; i2 = t;
        }
        final float x0 = (float)x[i0];
        final float y0 = (float)y[i0];
        final float s0 = s[i0];
        final float s2 = s[i2];

        // Solve for gradient direction vector: The inverse of
        // out how much the triangle must rotate to cause
        // x1' / (i1 - i0) = x2' / (i2 - i0)
        // where xi' = [ R(cos,sin) T(-x0, -y0) xi ]_x
        // This is 1 eqn in 2 unknowns: cos,sin.  The other is cos^2+sin^2 = 1.
        // When done, flipping the sign of sin gives the desired vector: rotate
        // the coordinate space rather than the triangle.
        final float dx1 = (float)x[i1] - x0;
        final float dy1 = (float)y[i1] - y0;
        final float ds1 = s[i1] - s0;
        
        final float dx2 = (float)x[i2] - x0;
        final float dy2 = (float)y[i2] - y0;
        final float ds2 = s2 - s0;

        // C * cos = S * sin
        // is the equation that must be solved for cos,sin.
        float C = dx1 * ds2 - dx2 * ds1;
        float S = dy1 * ds2 - dy2 * ds1;

        float cos = 0;
        float sin = 1;
        if (Math.abs(S) > 1f-6) {
            float p = C / S;
            cos = (float)Math.sqrt(1f / (p * p + 1f));
            sin = (float)Math.sqrt(1 - cos * cos);
        }
        // Rotation flip is incorporated here.  See above.
        if ((S < 0 && C < 0) || (S > 0 && C > 0)) {
            sin = -sin;
        }
        // [cos, sin] is now a unit vector in the direction the
        // gradient has to go.  Find gradient length.
        float len = cos * dx2 + sin * dy2;
        // Interpolate the colors with fog.
        float z0 = z[0];
        float z2 = z[2];
        float f0 = z0 >= 0 ? 0 : (cFog * z0) / (cFog * z0 - 1);
        float f2 = z2 >= 0 ? 0 : (cFog * z2) / (cFog * z2 - 1);
        Color c0 = new Color(
                (int)((1f - f0) * s0 * gouraudColor[0] + f0 * 192f),
                (int)((1f - f0) * s0 * gouraudColor[1] + f0 * 255f),
                (int)((1f - f0) * s0 * gouraudColor[2] + f0 * 255f));
        Color c2 = new Color(
                (int)((1f - f2) * s2 * gouraudColor[0] + f2 * 192f),
                (int)((1f - f2) * s2 * gouraudColor[1] + f2 * 255f),
                (int)((1f - f2) * s2 * gouraudColor[2] + f2 * 255f));
        g.setPaint(new GradientPaint(x0, y0, c0, x0 + len * cos, y0 + len * sin, c2));
        g.fillPolygon(x, y, 3);
    }

    /*
    private BufferedImage gradient;

    private final Color [] gradientColors = new Color[256];

    public void setGouraudColor(int [] color) {
        int r = color[0];
        int g = color[1];
        int b = color[2];
        int size = 1 + Math.max(r, Math.max(g, b));
        gradient = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = gradient.createGraphics();
        for (int i = 0; i < size; i++) {
            float t = i / (float)(size - 1);
            gradientColors[i] = new Color((int)(.5f + t * r), (int)(.5f + t * g),(int)(.5f + t * b));
            graphics.setColor(gradientColors[i]);
            graphics.fillRect(0, i, size, 1);
        }
        graphics.dispose();
    }

    Homogeneous.Matrix src = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);
    Homogeneous.Matrix srcInv = new Homogeneous.Matrix();
    Homogeneous.Matrix dst = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);
    Homogeneous.Matrix xformMatrix = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);
    AffineTransform xform = new AffineTransform();

    public void drawGouraudTriangle(Component c, Graphics2D g, int [] x, int [] y, float [] s, float [] z) {
        int size = gradient.getHeight();
        int s0 = (int)(0.499f + (size - 1) * s[0]);
        int s1 = (int)(0.499f + (size - 1) * s[1]);
        int s2 = (int)(0.499f + (size - 1) * s[2]);
        if (s0 == s1 && s1 == s2) {
            g.setColor(gradientColors[s0]);
            g.fillPolygon(x, y, nPoints);
            return;
        }
        int x0 = 0, x1 = 0, x2 = 0;
        if (s0 == s1 || s1 == s2) {
            x1 = size - 1;
        }
        else {
            x0 = size - 1;
        }

        src.a[0] = x0;
        src.a[1] = s0;
        src.a[2] = 1;

        src.a[4] = x1;
        src.a[5] = s1;
        src.a[6] = 1;

        src.a[8] = x2;
        src.a[9] = s2;
        src.a[10]= 1;
        srcInv.invertInto(src, .0001f);

        dst.a[0] = x[0];
        dst.a[1] = y[0];
        dst.a[2] = 1;

        dst.a[4] = x[1];
        dst.a[5] = y[1];
        dst.a[6] = 1;

        dst.a[8] = x[2];
        dst.a[9] = y[2];
        dst.a[10]= 1;

        xformMatrix.multiplyInto(dst, srcInv);
        xform.setTransform(xformMatrix.a[0], xformMatrix.a[1], xformMatrix.a[4], xformMatrix.a[5], xformMatrix.a[8], xformMatrix.a[9]);

        Polygon p = new Polygon(x, y, 3);
        g.setClip(p);
        g.drawImage(gradient, xform, c);
        g.setClip(null);
    }/*
    */
}
