/*
 * FixedEyeTerrainModel.java
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 *
 * @author Eugene K. Ressler
 */
public class FixedEyeTerrainModel extends TerrainModel {

    private final FixedEyeAnimation.Config config;
    private final FixedEyeTowerModel tower = new FixedEyeTowerModel();

    /**
     * Construct a new terrain model using the given animation configuration, which specifies
     * whether certain terrain features should be drawn or not.
     *
     * @param config animation configuration
     */
    public FixedEyeTerrainModel(FixedEyeAnimation.Config config) {
        //super(64);
        super(32);
        this.config = config;
        renderer.setGouraudColor(terrainColor);
    }

    public static final Homogeneous.Point unitLight = new Homogeneous.Point();
    static {
        Homogeneous.Point l = Animation.lightPosition;
        float r = 1f / (float)Math.sqrt(l.a[0] * l.a[0] + l.a[1] * l.a[1] + l.a[2] * l.a[2]);
        unitLight.a[0] = r * l.a[0];
        unitLight.a[1] = r * l.a[1];
        unitLight.a[2] = r * l.a[2];
        unitLight.a[3] = 0f;
    };

    public static final float ambientIntensity = 0.08f;
    private static final float lambertIntensity = 1f - ambientIntensity;

    private float lightIntensityAtPost(TerrainPost p) {
        float s = (p.xNormal * unitLight.x() +
                   p.yNormal * unitLight.y() +
                   p.zNormal * unitLight.z());
        return (s <= 0) ? ambientIntensity : ambientIntensity + s * lambertIntensity;
    }

    public static final int [] terrainColor = { 110, 160, 35 };
    public static final int [] waterColor = { 75, 150, 150 };

    private static final float cFog = 0.0025f;

    private Paint interpolateWaterColor(float z) {
        float tFog = z >= 0 ? 0 : (cFog * z) / (cFog * z - 1);
        int r = (int)((1f - tFog) * waterColor[0] + tFog * 192f);
        int g = (int)((1f - tFog) * waterColor[1] + tFog * 255f);
        int b = (int)((1f - tFog) * waterColor[2] + tFog * 255f);
        return new Color(r, g, b);
    }

    private final Renderer3d renderer = new Renderer3d();
    // Computed vertices of water polygons.
    private final Homogeneous.Point waterNorthWest = new Homogeneous.Point();
    private final Homogeneous.Point waterSouthWest = new Homogeneous.Point();
    private final Homogeneous.Point waterNorthEast = new Homogeneous.Point();
    private final Homogeneous.Point waterSouthEast = new Homogeneous.Point();

    private void fillToWater(Graphics2D g, ViewportTransform viewportTransform,
                             float dx, float x1, int dj, int j1,
                             int iUnder, float zUnder, Homogeneous.Point pUnder,
                             int iOver,  float zOver,  Homogeneous.Point pOver) {
        renderer.end(g);
        /* Causes jaggies at edge of water. Maintain color of last full quad.
        renderer.setPaint(interpolateColor(lightIntensityAtPost(posts[iUnder][j1 - dj]),
                                        lightIntensityAtPost(posts[iUnder][j1]),
                                        lightIntensityAtPost(posts[iOver][j1])));
         */
        renderer.begin(Renderer3d.GOURAUD_TRIANGLE_FAN);
        // Find point where under touches the water.
        float y0 = posts[iUnder][j1 - dj].elevation;  // Above water.
        float y1 = posts[iUnder][j1].elevation;  // Under water
        pUnder.set(x1 - dx * (yWater - y1) / (y0 - y1), yWater, zUnder);
        // This is center of fan.
        TerrainPost tpUnder = posts[iUnder][j1 - dj];
        float sUnder = lightIntensityAtPost(tpUnder);
        renderer.addVertex(g, viewportTransform, pUnder.a[0], pUnder.a[1], pUnder.a[2], sUnder);
        // Last visible addVertex on the under side is first fan addVertex.
        renderer.addVertex(g, viewportTransform, x1 - dx, tpUnder.elevation, zUnder, sUnder);
        // Last vertext on the over side is the second fan vertex.
        TerrainPost tpOver = posts[iOver][j1 - dj];
        float sOver = lightIntensityAtPost(tpOver);
        renderer.addVertex(g, viewportTransform, x1 - dx, tpOver.elevation, zOver, sOver);
        // Add fan vertices until over goes underwater.  Should always add at least onee.
        while (0 <= j1 && j1 < postCount) {
            TerrainPost tp = posts[iOver][j1];
            y1 = tp.elevation;
            if (y1 < yWater) {
                break;
            }
            float s = lightIntensityAtPost(tp);
            renderer.addVertex(g, viewportTransform, x1, y1, zOver, s);
            j1 += dj;
            x1 += dx;
        }
        // This test would fail only if the terrain model doesn't have the
        // river running fully north to south.
        if (0 <= j1 && j1 < postCount) {
            // Now add point where over touches water.
            TerrainPost tpAbove = posts[iOver][j1 - dj];
            float sAbove = lightIntensityAtPost(tpAbove);
            y0 = tpAbove.elevation;              // Above water.
            y1 = posts[iOver][j1].elevation;     // Under water
            pOver.set(x1 - dx * (yWater - y1) / (y0 - y1), yWater, zOver);
            renderer.addVertex(g, viewportTransform, pOver, sAbove);
        }
    }

    /**
     * Draw terrain strips for grid rows i0 to i1-1.  The south of i1-1 is
     * post row i1.
     * 
     * @param g
     * @param viewportTransform
     * @param i0
     * @param i1
     */
    private void drawTerrainStrip(Graphics2D g, ViewportTransform viewportTransform, int i0, int i1) {
        renderer.setApproximateGouraud(!config.showSmoothTerrain);

        // FixedTerrainModel surface as triangle strips.
        float zNorth = zGridToWorld(i0);
        float zSouth = zNorth + metersPerGrid;

        // This many grid squares are not rendered on east and west edges.
        int j0 = postCount / 5;
       // Upper bound of loop determines southmost extent of rendering.
        for (int iNorth = i0; iNorth < i1; iNorth++) {
            int iSouth = iNorth + 1;
            float x = xGridToWorld(j0);
            // The idea here is to set the water polygon to the east
            // border and then go hunting for the actual intersection with
            // the land.
            waterNorthWest.set(x, yWater, zNorth);
            waterSouthWest.set(x, yWater, zSouth);
            renderer.begin(Renderer3d.GOURAUD_TRIANGLE_STRIP);
            for (int j = j0; j < postCount; j++) {
                TerrainPost n = posts[iNorth][j];
                TerrainPost s = posts[iSouth][j];
                if (j > j0) {
                    if (n.elevation <= yWater) {
                        fillToWater(g, viewportTransform,
                                    metersPerGrid, x,
                                    1, j,
                                    iNorth, zNorth, waterNorthWest,
                                    iSouth, zSouth, waterSouthWest);
                        break;
                    }
                    if (s.elevation <= yWater) {
                        fillToWater(g, viewportTransform,
                                    metersPerGrid, x,
                                    1, j,
                                    iSouth, zSouth, waterSouthWest,
                                    iNorth, zNorth, waterNorthWest);
                        break;
                    }
                }
                float sn = lightIntensityAtPost(n);
                renderer.addVertex(g, viewportTransform, x, n.elevation, zNorth, sn);
                float ss = lightIntensityAtPost(s);
                renderer.addVertex(g, viewportTransform, x, s.elevation, zSouth, ss);
                x += metersPerGrid;
            }
            renderer.end(g);
            x = xGridToWorld(gridCount - j0);
            waterNorthEast.set(x, yWater, zNorth);
            waterSouthEast.set(x, yWater, zSouth);
            renderer.begin(Renderer3d.GOURAUD_TRIANGLE_STRIP);
            for (int j = gridCount - j0; j >= 0; j--) {
                TerrainPost n = posts[iNorth][j];
                TerrainPost s = posts[iSouth][j];
                if (j < gridCount - j0) {
                    if (n.elevation <= yWater) {
                        fillToWater(g, viewportTransform,
                                    -metersPerGrid, x,
                                    -1, j,
                                    iNorth, zNorth, waterNorthEast,
                                    iSouth, zSouth, waterSouthEast);
                        break;
                    }
                    if (s.elevation <= yWater) {
                        fillToWater(g, viewportTransform,
                                    -metersPerGrid, x,
                                    -1, j,
                                    iSouth, zSouth, waterSouthEast,
                                    iNorth, zNorth, waterNorthEast);
                        break;
                    }
                }
                float sn = lightIntensityAtPost(n);
                renderer.addVertex(g, viewportTransform, x, n.elevation, zNorth, sn);
                float ss = lightIntensityAtPost(s);
                renderer.addVertex(g, viewportTransform, x, s.elevation, zSouth, ss);
                x -= metersPerGrid;
            }
            renderer.end(g);
            // Draw the water polygon.
            // Blue polygon.
            renderer.setPaint(interpolateWaterColor(zSouth));
            renderer.begin(Renderer3d.TRIANGLE_STRIP);
            renderer.addVertex(g, viewportTransform, waterNorthWest);
            renderer.addVertex(g, viewportTransform, waterSouthWest);
            renderer.addVertex(g, viewportTransform, waterNorthEast);
            renderer.addVertex(g, viewportTransform, waterSouthEast);
            renderer.end(g);
            // Shoreline with light colored rule
            renderer.setPaint(Color.LIGHT_GRAY);
            renderer.begin(Renderer3d.LINES);
            renderer.addVertex(g, viewportTransform, waterNorthWest);
            renderer.addVertex(g, viewportTransform, waterSouthWest);
            renderer.addVertex(g, viewportTransform, waterNorthEast);
            renderer.addVertex(g, viewportTransform, waterSouthEast);
            renderer.end(g);
            zNorth = zSouth;
            zSouth += metersPerGrid;
        }
    }

    private boolean isRoadwayHidden(ViewportTransform viewportTransform) {
        return viewportTransform.isAboveVanishingPoint(westAbutmentFrontFace[1]);
    }

    private void drawRoadway(Graphics2D g, ViewportTransform viewportTransform, int j0) {
        if (isRoadwayHidden(viewportTransform)) {
            return;
        }
        float x = xGridToWorld(j0);
        renderer.setPaint(Bridge3dView.gray00);
        renderer.begin(Renderer3d.QUAD_STRIP);
        for (int j = j0; j < postCount - j0; j++) {
            if (x >= abutmentStepInset) {
                renderer.addVertex(g, viewportTransform, abutmentStepInset, wearSurfaceHeight, -deckHalfWidth);
                renderer.addVertex(g, viewportTransform, abutmentStepInset, wearSurfaceHeight, deckHalfWidth);
                break;
            }
            renderer.addVertex(g, viewportTransform, x, roadCenterline[j].elevation, -deckHalfWidth);
            renderer.addVertex(g, viewportTransform, x, roadCenterline[j].elevation, deckHalfWidth);
            x += metersPerGrid;
        }
        renderer.end(g);
        renderer.begin(Renderer3d.QUAD_STRIP);
        x = xGridToWorld(gridCount - j0);
        float xDeckEnd = 2f * halfSpanLength - abutmentStepInset;
        for (int j = gridCount - j0; j >= 0; j--) {
            if (x <= xDeckEnd) {
                renderer.addVertex(g, viewportTransform, xDeckEnd, wearSurfaceHeight, deckHalfWidth);
                renderer.addVertex(g, viewportTransform, xDeckEnd, wearSurfaceHeight, -deckHalfWidth);
                break;
            }
            renderer.addVertex(g, viewportTransform, x, roadCenterline[j].elevation, deckHalfWidth);
            renderer.addVertex(g, viewportTransform, x, roadCenterline[j].elevation, -deckHalfWidth);
            x -= metersPerGrid;
        }
        renderer.end(g);
    }

    private void drawWires(Graphics2D g, ViewportTransform viewportTransform, int i0, int i1) {
        g.setPaint(Color.GRAY);
        final int [] xWire = new int [wirePostCountPerTower + 1];
        final int [] yWire = new int [wirePostCountPerTower + 1];
        for (int iOffset = i0; iOffset < i1; ++iOffset) {
            float xOfs = xUnitPerpTower * wireOffsets[iOffset].x();
            float yOfs = wireOffsets[iOffset].y();
            float zOfs = zUnitPerpTower * wireOffsets[iOffset].x();
            for (int iTower = 0; iTower < 2; ++iTower) {
                int nGoodPts = 0;
                for (int iWire = 0; iWire < wirePt[iTower].length; ++iWire) {
                    Homogeneous.Point p = wirePt[iTower][iWire];
                    if (!viewportTransform.worldToViewport(xWire, yWire, iWire,
                            p.x() + xOfs, p.y() + yOfs, p.z() + zOfs)) {
                        break;
                    }
                    nGoodPts++;
                }
                g.drawPolyline(xWire, yWire, nGoodPts);
            }
        }
    }

    private void drawPowerLines(Graphics2D g, ViewportTransform viewportTransform) {
        drawWires(g, viewportTransform, 0, 3);
        for (int i = 0; i < towers.length; i++){
            tower.paint(g, viewportTransform, towers[i]);
        }
        drawWires(g, viewportTransform, 3, 6);
    }

    private float [] westAbutmentFlank, eastAbutmentFlank;
    private final float [] westAbutmentFrontFace = new float [9];
    private final float [] westAbutmentRearFace = new float [9];
    private final float [] westSkirt = new float[15];
    private final float [] eastAbutmentFrontFace = new float [9];
    private final float [] eastAbutmentRearFace = new float [9];
    private final float [] eastSkirt = new float[15];

    private void trimFlankBase(float [] flank) {
        int i = flank.length - 6;
        float xFoot = getElevationAt(flank[i], flank[i+2]);
        if (xFoot > yWater) {
            flank[i+1] = xFoot;
            setToTerrainY(flank, i - 3, -1f);
        }
    }

    // We only need 2 towers for this view.  The
    FixedEyeTowerModel.Triangle [] [] towers = new FixedEyeTowerModel.Triangle [2] [];

    @Override
    public void initializeTerrain(DesignConditions conditions, float trussCenterOffset, float abutmentHalfWidth) {
        super.initializeTerrain(conditions, trussCenterOffset, abutmentHalfWidth);
        for (int i = 0; i < towers.length; i++) {
            towers[i] = tower.getTriangles(dxTower, dzTower, towerPt[i]);
        }
        westAbutmentFlank = Arrays.copyOf(abutmentFrontFlank, abutmentFrontFlank.length);
        eastAbutmentFlank = Arrays.copyOf(abutmentFrontFlank, abutmentFrontFlank.length);
        System.arraycopy(abutmentFrontFace, 0, westAbutmentFrontFace, 0, westAbutmentFrontFace.length);
        System.arraycopy(abutmentRearFace, 0, westAbutmentRearFace, 0, westAbutmentFrontFace.length);
        System.arraycopy(abutmentFrontFace, 0, eastAbutmentFrontFace, 0, eastAbutmentFrontFace.length);
        System.arraycopy(abutmentRearFace, 0, eastAbutmentRearFace, 0, eastAbutmentFrontFace.length);

        // Mirror and translate east sides.
        for (int i3 = 0; i3 < eastAbutmentFrontFace.length; i3 += 3) {
            eastAbutmentFrontFace[i3+0] = 2 * halfSpanLength - eastAbutmentFrontFace[i3+0];
            eastAbutmentRearFace[i3+0] = 2 * halfSpanLength - eastAbutmentRearFace[i3+0];
        }
        for (int i3 = 0; i3 < eastAbutmentFlank.length; i3 += 3) {
            eastAbutmentFlank[i3+0] = 2 * halfSpanLength - eastAbutmentFlank[i3+0];
        }
        
        // Trim the abutment flanks to just below ground.
        trimFlankBase(westAbutmentFlank);
        trimFlankBase(eastAbutmentFlank);

        // Make the skirt polygons fit the terrain level.
        for (int i3 = 0; i3 < eastSkirt.length; i3 += 3) {
            float dz = i3 * (eastAbutmentFrontFace[2*3+2] - eastAbutmentRearFace[2*3+2]) / (eastSkirt.length - 3);
            westSkirt[i3+0] = westAbutmentFrontFace[2*3+0];       // copy x
            westSkirt[i3+2] = westAbutmentFrontFace[2*3+2] - dz;  // vary z in depth
            setToTerrainY(westSkirt, i3, 0f);                     // set y to terrain level
            eastSkirt[i3+0] = eastAbutmentFrontFace[2*3+0];       // copy x
            eastSkirt[i3+2] = eastAbutmentFrontFace[2*3+2] - dz;  // vary z in depth
            setToTerrainY(eastSkirt, i3, 0f);                     // set y to terrain level
        }
    }

    private static final Color [] abutmentFaceColors = { Bridge3dView.gray50, Bridge3dView.gray40, Bridge3dView.gray25, Bridge3dView.gray50 };
    private static BufferedImage abutmentTextureImage = BDApp.getApplication().getBufferedImageResource("bricktile.png");
    private static final Stroke waterlineStroke = new BasicStroke(3f);
    private Paint abutmentPaint = null;
    private int viewportUpdateKey = -1;

    public void drawAbutmentFlanks(Graphics2D g, ViewportTransform viewportTransform) {
        if (!config.showAbutments) {
            return;
        }
        // Try to reduce garbage by re-creating the paint only when the user resizes.
        if (abutmentPaint == null || viewportUpdateKey != viewportTransform.getUpdateKey()) {
            int textureSize = viewportTransform.worldToViewportDistance(3.0);
            abutmentPaint = new TexturePaint(abutmentTextureImage, new Rectangle(textureSize, textureSize));
            viewportUpdateKey = viewportTransform.getUpdateKey();
        }
        renderer.setPaint(abutmentPaint);
        renderer.begin(Renderer3d.POLYGON);
        renderer.addVertex(g, viewportTransform, westAbutmentFlank, 0, westAbutmentFlank.length / 3);
        renderer.end(g);
        renderer.begin(Renderer3d.POLYGON);
        renderer.addVertex(g, viewportTransform, eastAbutmentFlank, 0, eastAbutmentFlank.length / 3);
        renderer.end(g);
        // Kludge uses 3 pixel wide line to leave 1 pixel standing after water is patched.
        if (westAbutmentFlank[westAbutmentFlank.length - 6 + 1] == yWater) {
            Stroke savedStroke = g.getStroke();
            g.setStroke(waterlineStroke);
            renderer.setPaint(Color.LIGHT_GRAY);
            renderer.begin(Renderer3d.LINE_STRIP);
            renderer.addVertex(g, viewportTransform, westAbutmentFlank, westAbutmentFlank.length - 9, 2);
            renderer.end(g);
            renderer.begin(Renderer3d.LINE_STRIP);
            renderer.addVertex(g, viewportTransform, eastAbutmentFlank, eastAbutmentFlank.length - 9, 2);
            renderer.end(g);
            g.setStroke(savedStroke);
        }
    }

    public void drawAbutmentTops(Graphics2D g, ViewportTransform viewportTransform) {
        if (!config.showAbutments) {
            return;
        }
        // Abutment tops.
        renderer.setPaint(Bridge3dView.gray30);
        renderer.begin(Renderer3d.QUAD_STRIP);
        renderer.addVertex(g, viewportTransform,
                abutmentRearTop, 0,
                abutmentFrontTop, 0,
                null, 0,
                abutmentFrontTop.length / 3);
        renderer.begin(Renderer3d.QUAD_STRIP);
        for (int i3 = 0; i3 < abutmentRearTop.length; i3 += 3) {
            renderer.addVertex(g, viewportTransform,
                    2 * halfSpanLength - abutmentRearTop[i3],
                    abutmentRearTop[i3+1],
                    abutmentRearTop[i3+2]);
            renderer.addVertex(g, viewportTransform,
                    2 * halfSpanLength - abutmentFrontTop[i3],
                    abutmentFrontTop[i3+1],
                    abutmentFrontTop[i3+2]);
        }
        renderer.end(g);
    }

    public void drawAbutmentFaces(Graphics2D g, ViewportTransform viewportTransform) {
        if (!config.showAbutments) {
            return;
        }
        renderer.begin(Renderer3d.QUAD_STRIP);
        renderer.addVertex(g, viewportTransform, westAbutmentRearFace, 0, westAbutmentFrontFace, 0, abutmentFaceColors, 0, westAbutmentRearFace.length / 3);
        renderer.end(g);

        renderer.setPaint(abutmentFaceColors[abutmentFaceColors.length - 1]);
        renderer.begin(Renderer3d.POLYGON, 2);
        renderer.addVertex(g, viewportTransform, westSkirt, 0, eastSkirt.length / 3);
        renderer.end(g);

        renderer.begin(Renderer3d.QUAD_STRIP);
        renderer.addVertex(g, viewportTransform, eastAbutmentRearFace, 0, eastAbutmentFrontFace, 0, abutmentFaceColors, 0, eastAbutmentRearFace.length / 3);
        renderer.end(g);

        renderer.setPaint(abutmentFaceColors[abutmentFaceColors.length - 1]);
        renderer.begin(Renderer3d.POLYGON, 2);
        renderer.addVertex(g, viewportTransform, eastSkirt, 0, eastSkirt.length / 3);
        renderer.end(g);

        if (westSkirt[3*0 + 1] == yWater) {
            renderer.setPaint(Color.LIGHT_GRAY);
            renderer.begin(Renderer3d.LINES);
            renderer.addVertex(g, viewportTransform, westSkirt, 0);
            renderer.addVertex(g, viewportTransform, westSkirt, westSkirt.length - 3);
            renderer.addVertex(g, viewportTransform, eastSkirt, 0);
            renderer.addVertex(g, viewportTransform, eastSkirt, westSkirt.length - 3);
            renderer.end(g);
        }
    }

    public void patchRoadway(Graphics2D g, ViewportTransform viewportTransform) {
        if (config.showBackground) {
            drawRoadway(g, viewportTransform, 3 * gridCount / 8);
        }
    }

    public void patchTerrain(Graphics2D g, ViewportTransform viewportTransform) {
        drawAbutmentFlanks(g, viewportTransform);
        if (config.showBackground) {
            final int iRoadCenterline = gridCount / 2;
            drawTerrainStrip(g, viewportTransform, iRoadCenterline + roadEdgeIndexOffset, 
                    iRoadCenterline + 5 * roadEdgeIndexOffset);
        }
    }

    public void paint(Graphics2D g, ViewportTransform viewportTransform) {
        final int iRoadCenterline = gridCount / 2;
        if (config.showBackground) {
            drawPowerLines(g, viewportTransform);
            drawTerrainStrip(g, viewportTransform,
                    0,
                    iRoadCenterline + roadEdgeIndexOffset);
            drawRoadway(g, viewportTransform, gridCount / 4);
        }
        drawAbutmentFaces(g, viewportTransform);
        if (config.showBackground) {
            drawTerrainStrip(g, viewportTransform, 
                    iRoadCenterline + roadEdgeIndexOffset,
                    iRoadCenterline + 8 * roadEdgeIndexOffset);
        }
    }
}
