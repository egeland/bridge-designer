package wpbd;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

public class GouraudPaint implements Paint {

    private final int [] xPts = new int[3];
    private final int [] yPts = new int[3];
    private final float [] zFog = new float [3];
    private final int [] colorPts = new int[3];
    private int fogColor = 0xffffffff;
    private float cFog = 0f;
    private float zFogBase = 0f;

    public GouraudPaint(Color fogColor, float cFog, float zFogBase) {
        this.fogColor = fogColor.getRGB();
        this.cFog = cFog;
        this.zFogBase = zFogBase;
    }

    public GouraudPaint() {
        this(Color.WHITE, 0f, 0f);
    }

    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return new GouraudPaintContext(cm);
    }

    public int getTransparency() {
        final int alpha = 0xff000000;
        if (cFog > 0f && (fogColor & alpha) != alpha) {
            return Color.TRANSLUCENT;
        }
        for (int i = 0; i < 3; i++) {
            if ((colorPts[i] & alpha) != alpha) {
                return Color.TRANSLUCENT;
            }
        }
        return OPAQUE;
    }
}

class GouraudPaintContext implements PaintContext {

    GouraudPaintContext(ColorModel cm) {
    }

    public void dispose() {
    }

    public ColorModel getColorModel() {
        return null;
    }

    public Raster getRaster(int x, int y, int w, int h) {
        return null;
    }
}
