package bridgedesigner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

/*
 * We have four cases:
 *   left-above
 *   left-below
 *   right-above
 *   right below
 * when truck is below, we need
 *
 */
public class FixedEyeTruckModel {

    private final Renderer3d renderer = new Renderer3d();
    private final Homogeneous.Matrix loadTransform = new Homogeneous.Matrix();
    private final Homogeneous.Matrix loadRotation = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);
    private final Stroke thickStroke = new BasicStroke(2f);
    private static final Color darkerOrange = new Color(180, 80, 0);
    private static final Color darkOrange = new Color(200, 90, 0);
    private static final Color orange = new Color(225, 108, 0);
    private static final Color lightOrange = new Color(255, 148, 0);
    private static final Color veryLightOrange = new Color(255, 214, 64);
    private static final Color darkerGray = new Color(40, 40, 40);
    private static final Color lighterGray = new Color(210, 210, 210);
    private static final Color veryLightGray = new Color(230, 230, 230);
    private static final int nTirePoints = 12;
    private static final int nSpokePoints = 4;
    private static final float outerRadius = 0.4f;
    private static final float innerRadius = 0.25f;
    private static final float spokeRadius = 0.14f;
    private static final float spokeHoleRadius = 0.04f;
    private static final float hubRadius = 0.07f;

    private static final float [] dualOuterRear = new float [3 * (1 + nTirePoints)];
    private static final float [] tireOuterRear = new float [3 * (1 + nTirePoints)];
    private static final float [] tireOuterFront = new float [3 * (1 + nTirePoints)];
    private static final float [] tireInnerFront = new float [3 * (1 + nTirePoints)];
    private static final float [] spokes = new float [3 * 4 * nSpokePoints];
    private static final float [] hub = new float[3 * nSpokePoints];

    static {
        int i = 0;
        for (int i3 = 0; i3 < tireOuterRear.length; i3 += 3, ++i) {
            final double theta = Math.PI * 2 * i / (double)nTirePoints;
            final float s = (float)Math.sin(theta);
            final float c = (float)Math.cos(theta);
            tireOuterRear[i3+0] = tireOuterFront[i3+0] = dualOuterRear[i3+0] = c * outerRadius;
            tireOuterRear[i3+1] = tireOuterFront[i3+1] = dualOuterRear[i3+1] = s * outerRadius;
            tireInnerFront[i3+0] = c * innerRadius;
            tireInnerFront[i3+1] = s * innerRadius;
            dualOuterRear[i3+2] = -0.50f;
            tireOuterRear[i3+2] = -0.25f;
            tireOuterFront[i3+2] = 0f;
        }
        for (i = 0; i < nSpokePoints; i++) {
            final double theta = Math.PI * 2 * i / (double)nSpokePoints;
            final float s = (float)Math.sin(theta);
            final float c = (float)Math.cos(theta);
            final float xc = c * spokeRadius;
            final float yc = s * spokeRadius;
            final float dx = c * spokeHoleRadius;
            final float dy = s * spokeHoleRadius;
            final int i12 = 12 * i;
            final int i0 = i12 + 0;
            final int i1 = i12 + 3;
            final int i2 = i12 + 6;
            final int i3 = i12 + 9;
            // Small rotated square by perp vector arithmetic.
            spokes[i0+0] = xc + dx + dy;
            spokes[i0+1] = yc + dy - dx;
            spokes[i1+0] = xc + dx - dy;
            spokes[i1+1] = yc + dy + dx;
            spokes[i2+0] = xc - dx - dy;
            spokes[i2+1] = yc - dy + dx;
            spokes[i3+0] = xc - dx + dy;
            spokes[i3+1] = yc - dy - dx;
            spokes[i3+2] = spokes[i2+2] = spokes[i1+2] = spokes[i0+2] = 0f;
            final int ih = 3 * i;
            hub[ih+0] = c * hubRadius;
            hub[ih+1] = s * hubRadius;
            hub[ih+2] = 0f;
        }
    }

    // private final Homogeneous.Matrix debugScale = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);

    private final Homogeneous.Matrix wheelTranslation = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);
    private final Homogeneous.Matrix wheelRotation = new Homogeneous.Matrix(Homogeneous.Matrix.Ia);
    private float thetaWheel = 0f;


    private abstract class Drawable {
        Color color;
        float [] pts;
        abstract public void paint(Graphics2D g, ViewportTransform viewportTransform);
    }

    private class Polygon extends Drawable {

        public Polygon(Color color, float [] pts) {
            this.color = color;
            this.pts = pts;
        }

        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.setPaint(color);
            renderer.begin(Renderer3d.POLYGON);
            renderer.addVertex(g, viewportTransform, pts, 0, pts.length / 3);
            renderer.end(g);
        }
    }

    private class RuledPolygon extends Drawable {

        Color ruleColor = Color.BLACK;
        
        public RuledPolygon(Color color, float [] pts) {
            this.color = color;
            this.pts = pts;
        }

        public RuledPolygon(Color color, Color ruleColor, float [] pts) {
            this.color = color;
            this.ruleColor = ruleColor;
            this.pts = pts;
        }

        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.setPaint(color);
            renderer.setRulePaint(ruleColor);
            renderer.begin(Renderer3d.RULED_POLYGON);
            renderer.addVertex(g, viewportTransform, pts, 0, pts.length / 3);
            renderer.end(g);
        }
    }

    private class Lines extends Drawable {
        final byte thickness;
        public Lines(Color color, float [] pts) {
            this.color = color;
            this.pts = pts;
            thickness = 0;
        }
        public Lines(Color color, int thickness, float [] pts) {
            this.color = color;
            this.pts = pts;
            this.thickness = (byte)thickness;
        }

        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.setPaint(color);
            if (thickness > 0) {
                Stroke savedStroke = g.getStroke();
                g.setStroke(thickStroke);
                renderer.begin(Renderer3d.LINES);
                renderer.addVertex(g, viewportTransform, pts, 0, pts.length / 3);
                renderer.end(g);
                g.setStroke(savedStroke);
            }
            else {
                renderer.begin(Renderer3d.LINES);
                renderer.addVertex(g, viewportTransform, pts, 0, pts.length / 3);
                renderer.end(g);
            }
        }
    }

    private class FrontRightWheel extends Drawable {
        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.pushModelTransform();
            wheelTranslation.setTranslation(0.0f, 0.4f, 1.15f);
            renderer.appendTransform(wheelTranslation);
            // Tread
            renderer.setPaint(Color.GRAY);
            renderer.setRulePaint(Color.BLACK);
            renderer.setRuleFlags(Renderer3d.RULE_U);
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterRear, 0, tireOuterFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Side wall
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterFront, 0, tireInnerFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Wheel
            renderer.begin(Renderer3d.RULED_POLYGON);
            renderer.setPaint(Color.LIGHT_GRAY);
            renderer.setRulePaint(Color.WHITE);
            renderer.addVertex(g, viewportTransform, tireInnerFront, 0, nTirePoints + 1);
            renderer.end(g);
            // Hub
            renderer.setPaint(darkerOrange);
            renderer.begin(Renderer3d.POLYGON);
            renderer.addVertex(g, viewportTransform, hub, 0, nSpokePoints);
            renderer.end(g);
            // Spokes: These are all that rotate.
            renderer.appendTransform(wheelRotation);
            renderer.setPaint(Color.BLACK);
            for (int i12 = 0; i12 < spokes.length; i12 += 12) {
                renderer.begin(Renderer3d.POLYGON);
                renderer.addVertex(g, viewportTransform, spokes, i12, 4);
                renderer.end(g);
            }
            renderer.popModelTransform();
        }
    }

    private class RearRightWheel extends Drawable {
        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.pushModelTransform();
            wheelTranslation.setTranslation(-(float)DesignConditions.panelSizeWorld, 0.4f, 1.15f);
            renderer.appendTransform(wheelTranslation);
            renderer.setPaint(Color.GRAY);
            renderer.setRulePaint(Color.BLACK);
            renderer.setRuleFlags(Renderer3d.RULE_U);
            // Inside dual tread
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, dualOuterRear, 0, tireOuterRear, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Outside dual tread
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterRear, 0, tireOuterFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Side wall
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterFront, 0, tireInnerFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Wheel
            renderer.begin(Renderer3d.RULED_POLYGON);
            renderer.setRulePaint(Color.WHITE);
            renderer.setPaint(darkerGray);
            renderer.addVertex(g, viewportTransform, tireInnerFront, 0, nTirePoints);
            renderer.end(g);
            // Hub
            renderer.setPaint(darkerOrange);
            renderer.begin(Renderer3d.POLYGON);
            renderer.addVertex(g, viewportTransform, hub, 0, nSpokePoints);
            renderer.end(g);

            renderer.popModelTransform();
        }
    }

    private class FrontLeftWheel extends Drawable {
        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.pushModelTransform();
            wheelTranslation.setTranslation(0.0f, 0.4f, -1.15f + 0.25f);
            renderer.appendTransform(wheelTranslation);
            // Tread
            renderer.setPaint(darkerGray);
            renderer.setRulePaint(Color.BLACK);
            renderer.setRuleFlags(Renderer3d.RULE_U);
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterRear, 0, tireOuterFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Side wall
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterFront, 0, tireInnerFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Wheel
            renderer.begin(Renderer3d.RULED_POLYGON);
            renderer.setPaint(Color.BLACK);
            renderer.setRulePaint(Color.GRAY);
            renderer.addVertex(g, viewportTransform, tireInnerFront, 0, nTirePoints);
            renderer.end(g);
            renderer.popModelTransform();
        }
    }

    private class RearLeftWheel extends Drawable {
        @Override
        public void paint(Graphics2D g, ViewportTransform viewportTransform) {
            renderer.pushModelTransform();
            wheelTranslation.setTranslation(-(float)DesignConditions.panelSizeWorld, 0.4f, -1.15f + 0.5f);
            renderer.appendTransform(wheelTranslation);
            renderer.setPaint(darkerGray);
            renderer.setRulePaint(Color.BLACK);
            renderer.setRuleFlags(Renderer3d.RULE_U);
            // Inside dual tread
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, dualOuterRear, 0, tireOuterRear, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Outside dual tread
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterRear, 0, tireOuterFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Side wall
            renderer.begin(Renderer3d.RULED_QUAD_STRIP);
            renderer.addVertex(g, viewportTransform, tireOuterFront, 0, tireInnerFront, 0, null, 0, nTirePoints + 1);
            renderer.end(g);
            // Wheel
            renderer.begin(Renderer3d.RULED_POLYGON);
            renderer.setPaint(Color.BLACK);
            renderer.setRulePaint(Color.GRAY);
            renderer.addVertex(g, viewportTransform, tireInnerFront, 0, nTirePoints);
            renderer.end(g);
            renderer.popModelTransform();
        }
    }

    private final Drawable [] rightWheels = {
        new FrontRightWheel(),
        new RearRightWheel(),
    };

    private final Drawable [] leftWheels = {
        new FrontLeftWheel(),
        new RearLeftWheel(),
    };

    private final Drawable [] cabLeftInside = {
        new Polygon(Color.DARK_GRAY, new float [] {
           -0.400f, 0.850f,-1.150f,
            1.200f, 0.850f,-1.150f,
            1.200f, 1.500f,-1.150f,
            1.000f, 1.500f,-1.150f,
            0.800f, 1.800f,-1.150f,
            0.000f, 1.900f,-1.150f,
            0.000f, 2.500f,-1.150f,
           -0.400f, 2.500f,-1.150f,
        }),
        // Corner above window
        new Polygon(Color.BLACK, new float [] {
            0.800f, 2.700f,-0.950f,
           -0.200f, 2.700f,-0.950f,
           -0.400f, 2.500f,-1.150f,
            1.000f, 2.500f,-1.150f,
        }),
        // Rear windshield struts
        new Lines(Color.DARK_GRAY, 2, new float [] {
            1.200f, 1.500f,-1.150f,
            1.000f, 2.500f,-1.150f,
            0.800f, 1.800f,-1.150f,
            0.800f, 2.500f,-1.150f,
        }),
        // Front (white) strut
        new Lines(Color.WHITE, 2, new float [] {
            1.400f, 1.500f,-0.950f,
            1.200f, 2.600f,-0.950f,
        }),
    };

    private final Drawable [] cabFrontAboveBumper = {
        new Polygon(veryLightOrange, new float [] {
            1.400f, 1.500f, 0.950f,
            1.400f, 0.950f, 0.950f,
            1.400f, 0.950f,-0.950f,
            1.400f, 1.500f,-0.950f,
        }),
    };

    private final Drawable [] dashboard = {
        new Polygon(Color.LIGHT_GRAY, new float [] {
            1.000f, 1.500f, 1.150f,
            1.200f, 1.500f, 1.150f,
            1.400f, 1.500f, 0.950f,
            1.400f, 1.500f,-0.950f,
            1.200f, 1.500f,-1.150f,
            1.000f, 1.500f,-1.150f,
        }),
    };

    private final Drawable [] cabFront = {
        new Polygon(Color.GRAY, new float [] {
            1.500f, 0.950f, 0.950f,
            1.500f, 0.650f, 0.950f,
            1.500f, 0.650f,-0.950f,
            1.500f, 0.950f,-0.950f,
        }),
        new Polygon(veryLightOrange, new float [] {
            1.500f, 0.650f, 0.950f,
            1.500f, 0.400f, 0.950f,
            1.500f, 0.400f,-0.950f,
            1.500f, 0.650f,-0.950f,
        }),
    };

    private final Drawable [] cabCorner = {
        // Corner below windshield.
        new Polygon(lightOrange, new float [] {
            1.400f, 1.500f, 0.950f,
            1.200f, 1.500f, 1.150f,
            1.200f, 0.950f, 1.150f,
            1.400f, 0.950f, 0.950f,
        }),
    };

    private final Drawable [] bumperTop = {
        new Polygon(darkerGray, new float [] {
           1.200f, 0.950f, 1.150f,
           1.300f, 0.950f, 1.150f,
           1.500f, 0.950f, 0.950f,
           1.500f, 0.950f,-0.950f,
           1.300f, 0.950f,-1.150f,
           1.200f, 0.950f,-1.150f,
           1.400f, 0.950f,-0.950f,
           1.400f, 0.950f, 0.950f,
        }),
    };

    private final Drawable [] cabTop = {
        new Polygon(Color.DARK_GRAY, new float [] {
           -0.400f, 2.500f,-1.150f,
           -0.200f, 2.700f,-0.950f,
           -0.200f, 2.700f, 0.950f,
           -0.400f, 2.500f, 1.150f,
        }),
        new Polygon(Color.DARK_GRAY, new float [] {
           -0.200f, 2.700f,-0.950f,
            0.800f, 2.700f,-0.950f,
            0.800f, 2.700f, 0.950f,
           -0.200f, 2.700f, 0.950f,
        }),
        new Polygon(Color.BLACK, new float [] {
            0.800f, 2.700f,-0.950f,
            1.200f, 2.600f,-0.950f,
            1.200f, 2.600f, 0.950f,
            0.800f, 2.700f, 0.950f,
        }),
        // Corner above windshield
        new Polygon(Color.BLACK, new float [] {
            1.200f, 2.600f,-0.950f,
            0.800f, 2.700f,-0.950f,
            1.000f, 2.500f,-1.150f,
        }),
        // Front outside
        new Polygon(lightOrange, new float [] {
            0.800f, 2.700f, 0.950f,
            1.200f, 2.600f, 0.950f,
            1.200f, 2.600f,-0.950f,
            0.800f, 2.700f,-0.950f,
        }),
        // Main outside
        new Polygon(orange, new float [] {
           -0.200f, 2.700f, 0.950f,
            0.800f, 2.700f, 0.950f,
            0.800f, 2.700f,-0.950f,
           -0.200f, 2.700f,-0.950f,
        }),
        // Rear outside
        new Polygon(darkOrange, new float [] {
           -0.400f, 2.500f, 1.150f,
           -0.200f, 2.700f, 0.950f,
           -0.200f, 2.700f,-0.950f,
           -0.400f, 2.500f,-1.150f,
        }),
    };

    private final Drawable [] cabRear = {
        new Polygon(darkerOrange, new float [] {
           -0.400f, 2.000f,-0.700f,
           -0.400f, 2.500f,-0.700f,
           -0.400f, 2.500f,-1.150f,
           -0.400f, 0.850f,-1.150f,
           -0.400f, 0.850f, 1.150f,
           -0.400f, 2.500f, 1.150f,
           -0.400f, 2.500f, 0.700f,
           -0.400f, 2.000f, 0.700f,
        }),
    };

    private final Drawable [] cabRearInside = {
        new Polygon(Color.GRAY, new float [] {
           -0.400f, 2.000f, 0.700f,
           -0.400f, 2.500f, 0.700f,
           -0.400f, 2.500f, 1.150f,
           -0.400f, 0.850f, 1.150f,
           -0.400f, 0.850f,-1.150f,
           -0.400f, 2.500f,-1.150f,
           -0.400f, 2.500f,-0.700f,
           -0.400f, 2.000f,-0.700f,
        }),
    };

    private final Drawable [] cabBottom = {
        // Front horizontal panel
        new Polygon(Color.BLACK, new float [] {
            0.450f, 0.400f,-1.150f,
            1.300f, 0.400f,-1.150f,
            1.500f, 0.400f,-0.950f,
            1.500f, 0.400f, 0.950f,
            1.300f, 0.400f, 1.150f,
            0.450f, 0.400f, 1.150f,
        }),
        // Vertical connector
        new Polygon(Color.BLACK, new float [] {
            0.450f, 0.400f, 1.150f,
            0.450f, 0.850f, 1.150f,
            0.450f, 0.850f,-1.150f,
            0.450f, 0.400f,-1.150f,
        }),
        // Rear horizontal panel
        new Polygon(Color.BLACK, new float [] {
            0.450f, 0.850f, 1.150f,
           -0.400f, 0.850f, 1.150f,
           -0.400f, 0.850f,-1.150f,
            0.450f, 0.850f,-1.150f,
        })
    };

    private final Drawable [] cabRight = {
        new Polygon(orange, new float [] {
            // Main side panel
            0.000f, 0.850f, 1.150f,
            0.116f, 0.835f, 1.150f,
            0.225f, 0.790f, 1.150f,
            0.318f, 0.718f, 1.150f,
            0.390f, 0.625f, 1.150f,
            0.435f, 0.516f, 1.150f,
            0.450f, 0.400f, 1.150f,
            1.300f, 0.400f, 1.150f,
            1.300f, 0.650f, 1.150f,
            1.200f, 0.650f, 1.150f,
            1.200f, 1.500f, 1.150f,
            1.000f, 1.500f, 1.150f,
            0.800f, 1.800f, 1.150f,
            0.000f, 1.900f, 1.150f,
            0.000f, 2.500f, 1.150f,
           -0.400f, 2.500f, 1.150f,
           -0.400f, 0.850f, 1.150f,
        }),
        // Side light
        new Polygon(Color.RED, new float [] {
            1.200f, 0.650f, 1.150f,
            1.300f, 0.650f, 1.150f,
            1.300f, 0.800f, 1.150f,
            1.200f, 0.800f, 1.150f,
        }),
        // Side of bumper
        new Polygon(Color.BLACK, new float [] {
            1.200f, 0.800f, 1.150f,
            1.300f, 0.800f, 1.150f,
            1.300f, 0.950f, 1.150f,
            1.200f, 0.950f, 1.150f,
        }),
        // Corner below bumper
        new Polygon(lightOrange, new float [] {
            1.300f, 0.400f, 1.150f,
            1.500f, 0.400f, 0.950f,
            1.500f, 0.650f, 0.950f,
            1.300f, 0.659f, 1.150f,
        }),
        // Headlight
        new Polygon(Color.WHITE, new float [] {
            1.300f, 0.650f, 1.150f,
            1.500f, 0.650f, 0.950f,
            1.500f, 0.800f, 0.950f,
            1.300f, 0.800f, 1.150f,
        }),
        // Corner of bumper
        new Polygon(Color.DARK_GRAY, new float [] {
            1.300f, 0.800f, 1.150f,
            1.500f, 0.800f, 0.950f,
            1.500f, 0.950f, 0.950f,
            1.300f, 0.950f, 1.150f,
        }),
        // Corner above windshield
        new Polygon(lightOrange, new float [] {
            1.200f, 2.600f, 0.950f,
            0.800f, 2.700f, 0.950f,
            1.000f, 2.500f, 1.150f,
        }),
        // Rear windshield struts
        new Lines(lightOrange, 2, new float [] {
            1.200f, 1.500f, 1.150f,
            1.000f, 2.500f, 1.150f,
            0.800f, 1.800f, 1.150f,
            0.800f, 2.500f, 1.150f,
        }),
        // Front (white) strut
        new Lines(Color.WHITE, 2, new float [] {
            1.400f, 1.500f, 0.950f,
            1.200f, 2.600f, 0.950f,
        }),
        // Ridge above window
        new Polygon(lightOrange, new float [] {
            0.800f, 2.700f, 0.950f,
           -0.200f, 2.700f, 0.950f,
           -0.400f, 2.500f, 1.150f,
            1.000f, 2.500f, 1.150f,
        }),
    };

    private final Drawable [] mirror = {
        // Brackets
        new Lines(Color.WHITE, 2, new float [] {
            0.800f, 2.500f, 1.150f,
            0.800f, 2.500f, 1.450f,
            0.800f, 2.500f, 1.450f,
            1.000f, 2.500f, 1.150f,
            0.800f, 1.800f, 1.150f,
            0.800f, 1.800f, 1.450f,
            0.800f, 1.800f, 1.450f,
            1.000f, 1.500f, 1.150f,
        }),
        // Reflective and chrome surfaces of mirror
        new Polygon(Color.WHITE, new float [] {
            0.800f, 1.800f, 1.450f,
            0.800f, 2.500f, 1.450f,
            0.720f, 2.500f, 1.650f,
            0.720f, 1.800f, 1.650f,
        }),
        new Polygon(Color.LIGHT_GRAY, new float [] {
            0.720f, 1.800f, 1.650f,
            0.720f, 2.500f, 1.650f,
            0.800f, 2.500f, 1.450f,
            0.800f, 1.800f, 1.450f,
        }),
    };

    private final Drawable [] chasis = {
        // Short top filler
        new Polygon(Color.GRAY, new float [] {
           -0.800f, 0.850f, 0.800f,
           -0.400f, 0.850f, 0.800f,
           -0.400f, 0.850f,-0.800f,
           -0.800f, 0.850f,-0.800f,
        }),
        // Long vertical web
        new Polygon(Color.DARK_GRAY, new float [] {
            0.450f, 0.650f, 0.800f,
            0.450f, 0.850f, 0.800f,
           -0.400f, 0.850f, 0.800f, // Match back of cab
           -0.800f, 0.850f, 0.800f, // March front of cargo box
           -6.000f, 0.850f, 0.800f,
           -6.000f, 0.650f, 0.800f,
        }),
        // Belly
        new Polygon(Color.BLACK, new float [] {
            0.450f, 0.650f, 0.800f,
           -6.000f, 0.650f, 0.800f,
           -6.000f, 0.650f,-0.800f,
            0.450f, 0.650f,-0.800f,
        }),
    };

    private final Drawable [] gasTank = {
        // Side
        new Polygon(Color.LIGHT_GRAY, new float [] {
           -0.800f, 0.800f, 1.300f,
           -2.000f, 0.800f, 1.300f,
           -2.000f, 0.350f, 1.300f,
           -0.800f, 0.350f, 1.300f,
        }),
        // Front
        new Polygon(lighterGray, new float [] {
           -0.800f, 0.800f, 1.300f,
           -0.800f, 0.350f, 1.300f,
           -0.800f, 0.350f, 0.800f,
           -0.800f, 0.800f, 0.800f,
        }),
        // Back
        new Polygon(darkerGray, new float [] {
           -2.000f, 0.800f, 0.800f,
           -2.000f, 0.350f, 0.800f,
           -2.000f, 0.350f, 1.300f,
           -2.000f, 0.800f, 1.300f,
        }),
        // Bottom
        new Polygon(Color.BLACK, new float [] {
           -0.800f, 0.350f, 1.300f,
           -2.000f, 0.350f, 1.300f,
           -2.000f, 0.350f, 0.800f,
           -0.800f, 0.350f, 0.800f,
        }),
    };

    private final Drawable rightRearBumper [] = {
        new Lines(Color.GRAY, 2, new float [] {
           -6.000f, 0.850f, 1.300f,
           -6.000f, 0.300f, 1.300f,
           -6.000f, 0.300f, 0.800f,
           -6.000f, 0.650f, 0.800f,
           -5.500f, 0.850f, 1.300f,
           -6.000f, 0.400f, 1.300f,
           -6.000f, 0.300f, 1.300f,
           -6.000f, 0.300f,-1.300f,
        }),
    };

    private final Drawable leftRearBumper [] = {
        new Lines(Color.GRAY, 2, new float [] {
           -6.000f, 0.850f,-1.300f,
           -6.000f, 0.300f,-1.300f,
           -6.000f, 0.300f,-0.800f,
           -6.000f, 0.650f,-0.800f,
           -5.500f, 0.850f,-1.300f,
           -6.000f, 0.400f,-1.300f,
        }),
    };

    private final Drawable [] cargoBoxTopRightRear = {
        // Right bottom frame
        new Polygon(lighterGray, new float [] {
           -0.800f, 0.850f, 1.300f,
           -0.800f, 0.950f, 1.300f,
           -6.000f, 0.950f, 1.300f,
           -6.000f, 0.850f, 1.300f,
        }),
        // Right main
        new Polygon(lightOrange, new float [] {
           -0.800f, 0.950f, 1.300f,
           -0.800f, 3.100f, 1.300f,
           -6.000f, 3.100f, 1.300f,
           -6.000f, 0.950f, 1.300f,
        }),
        // Right top frame
        new Polygon(lighterGray, new float [] {
           -0.800f, 3.100f, 1.300f,
           -0.800f, 3.300f, 1.300f,
           -6.000f, 3.300f, 1.300f,
           -6.000f, 3.100f, 1.300f,
        }),
        // Rear bottom frame
        new Polygon(Color.LIGHT_GRAY, new float [] {
           -6.000f, 0.950f,-1.300f,
           -6.000f, 0.850f,-1.300f,
           -6.000f, 0.850f, 1.300f,
           -6.000f, 0.950f, 1.300f,
        }),
        // Rear main panel
        new Polygon(darkOrange, new float [] {
           -6.000f, 3.100f,-1.300f,
           -6.000f, 0.950f,-1.300f,
           -6.000f, 0.9500f, 1.300f,
           -6.000f, 3.100f, 1.300f,
        }),
        // Rear top frame
        new Polygon(Color.LIGHT_GRAY, new float [] {
           -6.000f, 3.300f,-1.300f,
           -6.000f, 3.100f,-1.300f,
           -6.000f, 3.100f, 1.300f,
           -6.000f, 3.300f, 1.300f,
        }),
        // Top
        new Polygon(new Color(160,160,160), new float [] {
           -6.000f, 3.300f, 1.300f,
           -0.800f, 3.300f, 1.300f,
           -0.800f, 3.300f,-1.300f,
           -6.000f, 3.300f,-1.300f,
        }),
        // Cap on chasis
        new Polygon(darkerGray, new float [] {
           -6.000f, 0.650f, 0.800f,
           -6.000f, 0.850f, 0.800f,
           -6.000f, 0.850f,-0.800f,
           -6.000f, 0.650f,-0.800f,
        }),
        // Left tail light
        new Polygon(Color.RED, new float [] {
           -6.000f, 0.930f,-1.200f,
           -6.000f, 0.870f,-1.200f,
           -6.000f, 0.870f,-1.000f,
           -6.000f, 0.930f,-1.000f,
        }),
        // Right tail light
        new Polygon(Color.RED, new float [] {
           -6.000f, 0.930f, 1.000f,
           -6.000f, 0.870f, 1.000f,
           -6.000f, 0.870f, 1.200f,
           -6.000f, 0.930f, 1.200f,
        }),
        new RuledPolygon(Color.LIGHT_GRAY, Color.GRAY, new float [] {
           -6.000f, 2.900f,-1.100f,
           -6.000f, 0.950f,-1.100f,
           -6.000f, 0.950f, 1.100f,
           -6.000f, 2.900f, 1.100f,
        })
    };

    private final Drawable [] cargoBoxFrontBottom = {
        // Front bottom frame
        new Polygon(Color.WHITE, new float [] {
           -0.850f, 0.950f, 1.300f,
           -0.850f, 0.850f, 1.300f,
           -0.850f, 0.850f,-1.300f,
           -0.850f, 0.950f,-1.300f,
        }),
        // Front main panel
        new Polygon(veryLightOrange, new float [] {
           -0.850f, 3.100f, 1.300f,
           -0.850f, 0.9500f, 1.300f,
           -0.850f, 0.950f,-1.300f,
           -0.850f, 3.100f,-1.300f,
        }),
        // Front top frame
        new Polygon(Color.WHITE, new float [] {
           -0.850f, 3.300f, 1.300f,
           -0.850f, 3.100f, 1.300f,
           -0.850f, 3.100f,-1.300f,
           -0.850f, 3.300f,-1.300f,
        }),
        // Bottom, right side
        new Polygon(Color.BLACK, new float [] {
            -0.800f, 0.850f, 1.300f,
            -6.000f, 0.850f, 1.300f,
            -6.000f, 0.850f, 0.800f,
            -0.800f, 0.850f, 0.800f,
        }),
        // Bottom, left side
        new Polygon(Color.BLACK, new float [] {
            -0.800f, 0.850f,-0.800f,
            -6.000f, 0.850f,-0.800f,
            -6.000f, 0.850f,-1.300f,
            -0.800f, 0.850f,-1.300f,
        }),
    };

    private final Drawable [] [] lowRightView = {
        leftWheels,
        cargoBoxFrontBottom,
        cabLeftInside,
        cabTop,
        cabFrontAboveBumper,
        cabRearInside,
        cabBottom,
        leftRearBumper,
        chasis,
        rightWheels,
        gasTank,
        rightRearBumper,
        cabFront,
        cabCorner, cabRight,
        cargoBoxTopRightRear,
        mirror,
    };

    private final Drawable [] [] lowLeftView = {
        leftWheels,
        cabLeftInside,
        cabTop,
        cabBottom,
        cabRear,
        cargoBoxFrontBottom,
        leftRearBumper,
        chasis,
        gasTank,
        rightWheels,
        rightRearBumper,
        cabCorner, cabRight,
        cargoBoxTopRightRear,
        mirror,
    };

    private final Drawable [] [] highRightView = {
        leftWheels,
        leftRearBumper,
        chasis,
        gasTank,
        rightRearBumper,
        rightWheels,
        cargoBoxFrontBottom,
        cabLeftInside,
        cabFront,
        bumperTop,
        cabFrontAboveBumper,
        dashboard,
        cabRearInside,
        cabTop,
        cabRight, cabCorner,
        cargoBoxTopRightRear,
        mirror,
    };

    private final Drawable [] [] highLeftView = {
        leftWheels,
        leftRearBumper,
        chasis,
        gasTank,
        rightWheels,
        rightRearBumper,
        cabLeftInside,
        bumperTop,
        dashboard,
        cabTop,
        cabRear,
        cabRight, cabCorner,
        cargoBoxFrontBottom,
        cargoBoxTopRightRear,
    };

    public void paint(Graphics2D g, ViewportTransform viewportTransform,
            Affine.Point ptLoad, Affine.Vector rotLoad, double distanceMoved) {
        // Build the transformation that translates and rotates canonical truck to current location.
        // The 3.5f moves the truck to the front of the deck.  In the middle the low views block
        // the lower half from view.
        loadTransform.setTranslation((float)ptLoad.x, (float)ptLoad.y, 3.5f);
        renderer.enableModelTransform(loadTransform);
        renderer.setCulling(true);
        loadRotation.a[0] = loadRotation.a[5] = (float)rotLoad.x;
        loadRotation.a[1]= (float)rotLoad.y;
        loadRotation.a[4] = -(float)rotLoad.y;
        renderer.appendTransform(loadRotation);

        // Make truck big so we can see how we're doing.
        // debugScale.a[0] = debugScale.a[5] = debugScale.a[10] = 4;
        // renderer.appendTransform(debugScale);

        // Build the wheel rotation transform.
        thetaWheel += (float)distanceMoved * (1f / 0.4f);
        final float twoPi = 2f * 3.14159226f;
        while (thetaWheel > twoPi) {
            thetaWheel -= twoPi;
        }
        float cos = (float)Math.cos(thetaWheel);
        float sin = (float)Math.sin(thetaWheel);
        wheelRotation.setIdentity();
        wheelRotation.a[0] = wheelRotation.a[5] = cos;
        wheelRotation.a[1] = -sin;
        wheelRotation.a[4] = sin;

        // Truck's cab top is above the vanishing point.
        boolean lowView = viewportTransform.isAboveVanishingPoint(ptLoad.y + 2.5);
        // Truck's cab rear is right of vanishing point.
        boolean leftView = viewportTransform.isRightOfVanishingPoint(ptLoad.x - 0.85);
        Drawable sequence [] [] =
                lowView ? (leftView ? lowLeftView : lowRightView)
                        : (leftView ? highLeftView : highRightView);
        for (int iSeq = 0; iSeq < sequence.length; iSeq++) {
            Drawable [] drawables = sequence[iSeq];
            for (int iDrbl = 0; iDrbl < drawables.length; iDrbl++) {
                drawables[iDrbl].paint(g, viewportTransform);
            }
        }

        renderer.setCulling(false);
        renderer.disableModelTransform();
    }
}
