/*
 * TerrainModel.java
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

import java.util.Random;

/**
 * <p>3d terrain model for the Bridge Designer.</p>
 * <p>Uses the diamond algorithm to establish basic terrain
 * shape, then carves a river valley and inserts abutments and pier.</p>
 *
 * @author Eugene K. Ressler
 */
public class TerrainModel {

    protected final int halfGridCount;
    protected final float metersPerGrid;
    protected final int gridCount;
    protected final int postCount;
    protected final TerrainPost posts [] [];
    protected final CenterlinePost [] roadCenterline;
    protected final Affine.Point [] naturalizedRiverAxis;

    protected static final float halfTerrainSize = 192f;
    protected static final float halfGapWidth = 24.0f;
    protected static final float bankSlope = 2.0f;
    protected static final float waterLevel = -26.0f;
    protected static final float blufSetback = halfGapWidth * .2f;
    protected static final float accessSlope = (float)BridgeView.accessSlope;
    protected static final float tangentOffset = (float)BridgeView.tangentOffset;
    protected static final float wearSurfaceHeight = (float)BridgeView.wearSurfaceHeight;
    protected static final float abutmentStepInset = (float)BridgeView.abutmentStepInset;
    protected static final float abutmentStepHeight = (float)BridgeView.abutmentStepHeight;
    protected static final float abutmentStepWidth = (float)BridgeView.abutmentStepWidth;
    protected static final float anchorOffset = (float)DesignConditions.anchorOffset;
    protected static final float deckHalfWidth = (float)Animation.deckHalfWidth;
    protected static final float stoneTextureSize = .3f;
    protected static final float tBlufAtBridge = halfGapWidth + blufSetback;
    protected static final float tInflection = halfGapWidth - blufSetback;
    protected static final float blufCoeff = -0.5f * bankSlope / (tInflection -  (blufSetback + halfGapWidth));
    protected static final float yGorgeBottom = -halfGapWidth * bankSlope;
    protected static final float tWaterEdge = (waterLevel - yGorgeBottom) / bankSlope;
    protected static final float roadCutSlope = 1f;
    protected static final float epsPaint = 0.05f;

    protected static final float terrainBrightness = 1.5f;
    protected static final float [] flatTerrainMaterial = { 
        0.03f * terrainBrightness,
        0.1f * terrainBrightness,
        0.009f * terrainBrightness,
        1.0f };
    protected static final float [] verticalTerrainMaterial = { 
        0.2f * terrainBrightness,
        0.2f * terrainBrightness,
        0.08f * terrainBrightness,
        1.0f };
    protected static final float yNormalMaterialThreshhold = 0.8f;
    protected static final float [] white = { 1f, 1f, 1f, 1f };
    protected static final float [] roadMaterial = { 0.2f, 0.2f, 0.2f, 1.0f };
    protected static final float [] abutmentMaterial = { .8f, .8f, .8f, 1f };
    protected static final float [] pillowMaterial = { .4f, .4f, .4f, 1f };
    protected float halfSpanLength = 0f;
    protected float yWater = 0f;
    protected float yGrade = 0f;
    protected float abutmentHalfWidth = 0f;
    protected float trussCenterOffset = 0f;
    protected boolean leftCable = false;
    protected boolean rightCable = false;
    protected long tLast = -1;
    protected float dWater = 0f;
    protected boolean drawingShadows = false;
    protected Affine.Point pierLocation = null;
    protected int roadEdgeIndexOffset = 1;

    // First coordinate must be directly south of (0,0).
    protected static final Affine.Point riverAxisSouth [] = {
       new Affine.Point(   0,    16),
       new Affine.Point(  15,   40),
       new Affine.Point( -20,  120),
       new Affine.Point(  80,  180)
    };

    // First coordinate must be directly north of (0,0).
    protected static final Affine.Point riverAxisNorth [] = {
       new Affine.Point(   0,    -14),
       new Affine.Point(  -4,    -50),
       new Affine.Point(  22,   -130),
       new Affine.Point(  80,  -190),
    };

    protected static final double majorPeriod = 183.0;
    protected static final double majorMagnitude = 18.0;
    protected static final double minorPeriod = 17.0;
    protected static final double minorMagnitude = 7;

    public TerrainModel() {
        this(32);
    }
    
    public TerrainModel(int halfGridCount) {
        this.halfGridCount = halfGridCount;
        metersPerGrid = halfTerrainSize / halfGridCount;
        gridCount = 2 * halfGridCount;
        postCount = gridCount + 1;
        roadEdgeIndexOffset = (int)(0.9999f + deckHalfWidth / metersPerGrid);
        posts = new TerrainPost [postCount] [postCount];
        roadCenterline = new CenterlinePost [postCount];
        yFractal = getFractalTerrain(postCount, 15.0f, 1.8f, 0);
        naturalizedRiverAxis = initialNaturalizedRiverAxis();
    }
    
    protected static class TerrainPost {
        public TerrainPost(float elevation) {
            this.elevation = elevation;
            this.xNormal = this.zNormal = 0f;
            this.yNormal = 1f;
        }
        public TerrainPost() {
        }
        float elevation;
        float xNormal;
        float yNormal;
        float zNormal;
    }

    protected static class CenterlinePost {
        public CenterlinePost(float elevation) {
            this.elevation = elevation;
            this.xNormal = 0;
            this.yNormal = 1;
        }
        public CenterlinePost() {
        }
        float elevation;
        float xNormal;
        float yNormal;
    }

    /**
     * Return one elevation along the road centerline.
     *
     * @param x x-coordinate in bridge coordinates (left side of deck is origin)
     * @return elevation
     */
    public float getRoadCenterlineElevation(float x) {
        x += halfTerrainSize - halfSpanLength;  // From bridge x-coords to terrain x-coord.
        float x0 = x / metersPerGrid;
        int i0 = (int)x0;
        int i1 = i0 + 1;
        // - yGrade converts terrain y-coord to bridge y-coord.
        if (i0 < 0) {
            return roadCenterline[0].elevation;
        }
        if (i1 >= postCount) {
            return roadCenterline[postCount - 1].elevation;
        }
        float t = x0 - i0;
        return (1 - t) * roadCenterline[i0].elevation + t * roadCenterline[i1].elevation;
    }

    /**
     * Return a perturbed version of the axis of the river. Uses a sum of sines as offset perpendicular
     * to the unperturbed axis.
     *
     * @param axis axis polyline points
     * @param nPoints number of points to produce in the result
     * @return perturbed axis points
     */
    protected static Affine.Point [] getPerturbedAxis(Affine.Point [] axis, int nPoints) {
        Affine.Point rtn [] = new Affine.Point [nPoints];
        double len = 0;
        for (int i = 1; i < axis.length; i++) {
            len += axis[i-1].distance(axis[i]);
        }
        double dt = len / (nPoints - 1);
        double t = 0;
        int iSeg = -1;
        double tSegStart = 0;
        double tSegEnd = 0;
        Affine.Vector v = null;
        Affine.Vector vPerp = null;
        for (int i = 0; i < nPoints; i++) {
            if (t >= tSegEnd && iSeg + 2 < axis.length) {
                iSeg++;
                tSegStart = tSegEnd;
                tSegEnd += axis[iSeg].distance(axis[iSeg + 1]);
                v = axis[iSeg + 1].minus(axis[iSeg]).unit(1);
                vPerp = v.perp();
            }
            Affine.Point p = axis[iSeg].plus(v.times(t - tSegStart));
            double ofs = majorMagnitude * Math.sin(t / majorPeriod * 2 * Math.PI) +
                     minorMagnitude * Math.sin(t / minorPeriod * 2 *Math.PI);
            rtn[i] = p.plus(vPerp.times(ofs));
            t += dt;
        }
        return rtn;
    }

    protected static Affine.Point [] initialNaturalizedRiverAxis() {
        Affine.Point north [] = getPerturbedAxis(riverAxisNorth, 32);
        Affine.Point south [] = getPerturbedAxis(riverAxisSouth, 32);
        Affine.Point rtn [] = new Affine.Point [north.length + south.length];
        Utility.reverse(north);
        System.arraycopy(north, 0, rtn, 0, north.length);
        System.arraycopy(south, 0, rtn, north.length, south.length);
        return rtn;
    }

    protected float distToRiver(float x, float z) {
        float dist = 1e6f;
        for (int i = 0; i < naturalizedRiverAxis.length - 1; i++) {
            dist = Math.min(dist, (float)new Affine.Point(x, z).distanceToSegment(naturalizedRiverAxis[i], naturalizedRiverAxis[i + 1]));
        }
        return dist;
    }

    protected final Random generator = new Random();

    protected float random() {
        return generator.nextFloat() * 2 - 1f;
    }

    /**
     *  Set up a static random fractal surface.
     */
    protected final float [] [] yFractal;

    /**
     * Return a height map for a square fractal terrain surface with given size, initial offset, and decay factor.
     * Fractal offsets are probabilistic zero mean.
     *
     * @param size number of posts on each edge of the height map
     * @param initDy offset of the first perturbation
     * @param decay decay factor (you'll usually want this to be > 1).
     * @return height map
     */
    private float [] [] getFractalTerrain(int size, float initDy, float decay, long seed) {

        // Set the random number stream.  Each seed causes a different terrain .
        generator.setSeed(seed);

        // Allocate the return array and initialize the corners.
        float y [] [] = new float [size] [size];
        final int iMax = size - 1;
        y[0][0] = y[iMax][0] = y[iMax][iMax] = y[0][iMax] = 0f;

        // Range of random perturbations, which is reduced by decay after each iteration.
        float dy = initDy;

        // Perturb successively halved subgrids.  Example: Initially we have a 1x1 and we perturb
        // the center (in the square phase) and edge midpoints (in the diamond phase).
        int halfStride = iMax / 2;
        for (int stride = iMax; stride > 1; stride = halfStride, halfStride /= 2, dy /= decay) {
            // Square phase.
            for (int i = 0; i < iMax; i += stride) {
                for (int j = 0; j < iMax; j += stride) {
                    float avg = 0.25f * (y[i][j] + y[i + stride][j] + y[i][j + stride] + y[i + stride][j + stride]);
                    y[i + halfStride][j + halfStride] = avg + random() * dy;
                }
            }
            // Diamond phase. More cases here because diamonds are partial at terrain edges.
            for (int i = 0; i < size; i += stride) {
                for (int j = halfStride; j < size; j += stride) {
                    float e = y[i][j - halfStride] + y[i][j + halfStride];
                    int in = i - halfStride;
                    int is = i + halfStride;
                    int n = 2;
                    if (in >= 0) {
                        e += y[in][j];
                        n++;
                    }
                    if (is < size) {
                        e += y[is][j];
                        n++;
                    }
                    y[i][j] = e / n + random() * dy;
                }
            }
            for (int i = halfStride; i < size; i += stride) {
                for (int j = 0; j < size; j += stride) {
                    float e = y[i - halfStride][j] + y[i + halfStride][j];
                    int jw = j - halfStride;
                    int je = j + halfStride;
                    int n = 2;
                    if (jw >= 0) {
                        e += y[i][jw];
                        n++;
                    }
                    if (je < size) {
                        e += y[i][je];
                        n++;
                    }
                    y[i][j] = e / n + random() * dy;
                }
            }
        }
        return y;
    }

    /**
     * Compute the y-component of a depression around an anchorage.
     *
     * @param x x-component in terrain coordinates of point to find y-component for
     * @param z z-component in terrain coordinates of point to find y-component for
     * @param xAnchor x-component of anchorage
     * @param zAnchor z-component of anchorage
     * @return y-component of depression
     */
    protected float yAnchor(float x, float z, float xAnchor, float zAnchor) {
        return abutmentStepHeight + Math.max(0, Math.max(Math.abs(x - xAnchor), Math.abs(z - zAnchor)) - metersPerGrid);
    }

    /**
     * Compute one elevation, combining all the models.
     *
     * @param i post row grid coordinate
     * @param j post column grid coordinate
     * @param grade grade level
     * @return elevation of this post
     */
    protected float syntheticElevation(int i, int j, float grade) {
        float x = (j - halfGridCount) * metersPerGrid;
        float z = (i - halfGridCount) * metersPerGrid;
        // Distance to river center line.
        float tWater = distToRiver(x, z);
        // Distance to center of road.
        float tRoadway = Math.abs(z);
        // Coefficient varies from 0 at water edge to 1 at the corner of the bluf above the water.
        float tFractalA = Math.min(1f, Math.max(0f, 0.1f * (tWater - tWaterEdge)));
        // Coefficient varies from 0 at a point 4 meters outside roadway edge to 1 at cut/fill edge.
        float tFractalB = Math.min(1f, Math.max(0f, 0.2f * (tRoadway - deckHalfWidth - 4f)));
        // Taking min determines how much fractal randomness should affect final elevation.
        float tFractal = Math.min(tFractalA, tFractalB);
        // Basic elevation is a portion of randomness from the fractal.
        float y = yFractal[i][j] * tFractal;
        // If we're close to the water, we roll off in a parabolic section.
        // Below is a hack to push back the river banks to the south for
        // the fixed point view only.
        float tBluf = tBlufAtBridge + 0.3f * tFractalB;
        /*
        if (halfGridCount > 32 && z > 0) {
            tBluf *= Math.min(1.3f, Math.max(1f, 1f + 0.05f * (z - 1.5 * deckHalfWidth)));
        }
        */
        if (tWater <= tBluf) {
            y -= blufCoeff * Utility.sqr(tWater - tBluf);
        }
        // Clamp underwater portion.
        y = Math.max(waterLevel - 5f, y);
        // Raise to grade.
        y += grade;

        // Cut or fill for the roadway.
        float tCut = Math.abs(z);
        float yRoad = roadCenterline[j].elevation - epsPaint;
        float yRise = (tCut - abutmentHalfWidth - metersPerGrid) * roadCutSlope;

        // Try cut first.
        float yCut = (yRise >= 0) ? yRoad + yRise : yRoad;
        if (yCut <= y) {
            y = yCut;
        }
        // Try fill only if we're not close to bridge.
        else if ( !(-halfGapWidth <= x && x <= halfGapWidth) ) {
            float yFill = (yRise >= 0) ? yRoad - yRise : yRoad;
            if (yFill >= y) {
                y = yFill;
            }
        }

        // Make depressions around the anchorages.
        if (leftCable) {
            if (z < -trussCenterOffset) {
                float yAnchorNW = yAnchor(x, z, -anchorOffset - halfSpanLength, -trussCenterOffset);
                if (yAnchorNW < y) {
                    y = yAnchorNW;
                }
            }
            if (z > trussCenterOffset) {
                float yAnchorSW = yAnchor(x, z, -anchorOffset - halfSpanLength, trussCenterOffset);
                if (yAnchorSW < y) {
                    y = yAnchorSW;
                }
            }
        }
        if (rightCable) {
            if (z < -trussCenterOffset) {
                float yAnchorNE = yAnchor(x, z, +anchorOffset + halfSpanLength, -trussCenterOffset);
                if (yAnchorNE < y) {
                    y = yAnchorNE;
                }
            }
            if (z > trussCenterOffset) {
                float yAnchorSE = yAnchor(x, z, +anchorOffset + halfSpanLength, trussCenterOffset);
                if (yAnchorSE < y) {
                    y = yAnchorSE;
                }
            }
        }

        return y;
    }

    protected float getElevationAt(int i, int j) {
        if (i < 0) {
            i = 0;
        }
        if (i >= postCount) {
            i = postCount - 1;
        }
        if (j < 0) {
            j = 0;
        }
        if (j >= postCount) {
            j = postCount - 1;
        }
        return posts[i][j].elevation;
    }

    /**
     * Return elevation for a point on the terrain model given by x- and z-coordinates.
     * Does not consider abutments and pier.
     *
     * @param x x in bridge coordinates
     * @param z z in bridge coordinates
     * @return elevation
     */
    public float getElevationAt(float x, float z) {
        float i0f = (z + halfTerrainSize) / metersPerGrid;
        int i0 = (int)i0f;
        float ti = i0f - i0;
        float j0f = (x - halfSpanLength + halfTerrainSize) / metersPerGrid;
        int j0 = (int)j0f;
        float tj = j0f - j0;
        float e00 = getElevationAt(i0, j0);
        float e01 = getElevationAt(i0, j0 + 1);
        float et0 = e00 * (1f - tj) + e01 * tj;
        float e10 = getElevationAt(i0 + 1, j0);
        float e11 = getElevationAt(i0 + 1, j0 + 1);
        float et1 = e10 * (1f - tj) + e11 * tj;
        return Math.max(yWater, et0 * (1f - ti) + et1 * ti);
    }

    protected float xGridToWorld(int j) {
        return (j - halfGridCount) * metersPerGrid + halfSpanLength;
    }

    protected float zGridToWorld(int i) {
        return (i - halfGridCount) * metersPerGrid;
    }

    /**
     * Initialize the terrain model with given parameters.
     *
     * @param conditions design conditions for the bridge siet
     * @param trussCenterOffset truss center offset used to position anchorages with respect to roadway axis
     * @param abutmentHalfWidth half width of abutment
     */
    public void initializeTerrain(DesignConditions conditions, float trussCenterOffset, float abutmentHalfWidth) {

        this.abutmentHalfWidth = abutmentHalfWidth;
        halfSpanLength = (float)(0.5 * conditions.getSpanLength());

        // Set the height of the terrain grade line.
        yGrade = (float) (DesignConditions.gapDepth - conditions.getDeckElevation() + wearSurfaceHeight);

        // Set water polygon height.
        yWater = waterLevel + yGrade;
        this.trussCenterOffset = trussCenterOffset;

        // Establish road centerline.
        final int iMax = postCount - 1;
        float x = 0;
        float dy = (yGrade - wearSurfaceHeight);
        if (dy == 0f) {
            for (int i = 0; i < postCount; i++) {
                roadCenterline[i] = new CenterlinePost(yGrade);
            }
        }
        else {
            float A = accessSlope / (2 * tangentOffset);
            // x0 is edge of bridge deck
            float x0 = halfSpanLength;
            // x1 is end of lower parabolic transition, start of linear ramp.
            float x1 = halfSpanLength + tangentOffset;
            // y1 is elevation over edge of deck at end of parabolic transition.
            float y1 = A * tangentOffset * tangentOffset;
            // x2 is end of linear ramp, start of upper parabolic transition.
            float x2 = halfSpanLength + dy / accessSlope;
            // x3 is end of uppper parabolic transition, start of level roadway.
            float x3 = x2 + tangentOffset;
            int j = halfGridCount;
            for (int i = halfGridCount; i < postCount; i++, j--) {
                float y = 0;
                if (x <= halfSpanLength) {
                    y = wearSurfaceHeight;
                }
                else if (x <= x1) {
                    float xp = x - x0;
                    y = A * xp * xp + wearSurfaceHeight;
                }
                else if (x <= x2) {
                    float xp = x - x1;
                    y = y1 + xp * accessSlope + wearSurfaceHeight;
                }
                else if (x <= x3) {
                    float xp = x - x3;
                    y = yGrade - A * xp * xp;
                }
                else {
                    y = yGrade;
                }
                roadCenterline[i] = new CenterlinePost(y);
                roadCenterline[j] = new CenterlinePost(y);
                x += metersPerGrid;
            }
        }

        // Set the roadway segment normals, which are 2d.  We average normals of segments to
        // west and east of each way point.
        for (int i = 1; i < iMax - 1; i++) {
            float e0 = roadCenterline[i].elevation;
            float xw = -metersPerGrid;
            float yw = roadCenterline[i - 1].elevation - e0;
            float xe = metersPerGrid;
            float ye = roadCenterline[i + 1].elevation - e0;
            double rw = 1.0 / Math.sqrt(xw * xw + yw * yw);
            xw *= rw;
            yw *= rw;
            double re = 1.0 / Math.sqrt(xe * xe + ye * ye);
            xe *= re;
            ye *= re;
            float xnw = yw;
            float ynw = -xw;
            float xne = -ye;
            float yne = xe;
            float nx = 0.5f * (xnw + xne);
            float ny = 0.5f * (ynw + yne);
            double rn = 1.0 / Math.sqrt(nx * nx + ny * ny);
            nx *= rn;
            ny *= rn;
            roadCenterline[i].xNormal = nx;
            roadCenterline[i].yNormal = ny;
        }
        roadCenterline[0] = roadCenterline[1];
        roadCenterline[iMax] = roadCenterline[iMax - 1];

        // Remember whether there are anchorages.
        leftCable = conditions.isLeftAnchorage();
        rightCable = conditions.isRightAnchorage();

        // Fill in the terrain surface with elevation of random terrain
        // adjusted for road cut and abutments.
        for (int i = 0; i < postCount; i++) {
            for (int j = 0; j < postCount; j++) {
                posts[i][j] = new TerrainPost(syntheticElevation(i, j, yGrade));
            }
        }

        // Compute unit normals at each intersection of 4 quads.
        for (int i = 0; i < postCount; i++) {
            for (int j = 0; j < postCount; j++) {
                initializeTerrainNormal(i, j);
            }
        }

        initializeAbutment(conditions.isArch() ? (float)conditions.getUnderClearance() : 0f, abutmentHalfWidth);
        if (conditions.isPier()) {
            pierLocation =  conditions.getPrescribedJointLocation(conditions.getPierJointIndex());
        }
        else {
            pierLocation = null;
        }

        initializePowerLines();
    }
  
    private void initializeTerrainNormal(int i, int j) {
        float xL = Animation.lightPosition.x();
        float yL = Animation.lightPosition.y();
        float zL = Animation.lightPosition.z();
        final int iMax = postCount - 1;
        float xNormal = 0;
        float yNormal = 0;
        float zNormal = 0;
        float e0 = posts[i][j].elevation;
        final float xe = metersPerGrid;
        final float ye = (j < iMax) ? posts[i][j + 1].elevation - e0: 0;
        final float ze = 0;

        final float xn = 0;
        final float yn = (i > 0) ? posts[i - 1][j].elevation - e0 : 0;
        final float zn = -metersPerGrid;

        final float xl = -metersPerGrid;
        final float yl = (j > 0) ? posts[i][j - 1].elevation - e0 : 0;
        final float zl = 0;

        final float xd = 0;
        final float yd = (i < iMax) ? posts[i + 1][j].elevation  - e0 : 0;
        final float zd = metersPerGrid;

        final float xne = ye * zn - ze * yn;
        final float yne = ze * xn - xe * zn;
        final float zne = xe * yn - ye * xn;
        final float rne = (float)(1.0 / Math.sqrt(xne * xne + yne * yne + zne * zne));
        // This is a hack to reduce shadow acne around light space horizons. If
        // any of the normals faces away from the light, we use that normal
        // for the vertex.  This isn't perfect. We'd have to use the normals
        // of the true rendered triangles, and this is using a pyramid of triangles
        // centered at the post.  The effect is that horizonss
        // appear darker than they would otherwise, but this isn't bad at all.
        if (xne * xL + yne * yL + zne * zL < 0) {
            posts[i][j].xNormal = xne * rne;
            posts[i][j].yNormal = yne * rne;
            posts[i][j].zNormal = zne * rne;
            return;
        }
        xNormal += xne * rne;
        yNormal += yne * rne;
        zNormal += zne * rne;

        final float xnw = yn * zl - zn * yl;
        final float ynw = zn * xl - xn * zl;
        final float znw = xn * yl - yn * xl;
        final float rnw = (float)(1.0 / Math.sqrt(xnw * xnw + ynw * ynw + znw * znw));
        if (xnw * xL + ynw * yL + znw * zL < 0) {
            posts[i][j].xNormal = xnw * rnw;
            posts[i][j].yNormal = ynw * rnw;
            posts[i][j].zNormal = znw * rnw;
            return;
        }
        xNormal += xnw * rnw;
        yNormal += ynw * rnw;
        zNormal += znw * rnw;

        final float xsw = yl * zd - zl * yd;
        final float ysw = zl * xd - xl * zd;
        final float zsw = xl * yd - yl * xd;
        final float rsw = (float)(1.0 / Math.sqrt(xsw * xsw + ysw * ysw + zsw * zsw));
        if (xsw * xL + ysw * yL + zsw * zL < 0) {
            posts[i][j].xNormal = xsw * rsw;
            posts[i][j].yNormal = ysw * rsw;
            posts[i][j].zNormal = zsw * rsw;
            return;
        }
        xNormal += xsw * rsw;
        yNormal += ysw * rsw;
        zNormal += zsw * rsw;
        final float xse = yd * ze - zd * ye;
        final float yse = zd * xe - ze * xd;
        final float zse = xd * ye - yd * xe;
        final float rse = (float)(1.0 / Math.sqrt(xse * xse + yse * yse + zse * zse));
        if (xse * xL + yse * yL + zse * zL < 0) {
            posts[i][j].xNormal = xse * rse;
            posts[i][j].yNormal = yse * rse;
            posts[i][j].zNormal = zse * rse;
            return;
        }
        xNormal += xse * rse;
        yNormal += yse * rse;
        zNormal += zse * rse;

        final float r = (float)(1.0 / Math.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal));
        posts[i][j].xNormal = xNormal * r;
        posts[i][j].yNormal = yNormal * r;
        posts[i][j].zNormal = zNormal * r;
    }

    protected float abutmentFrontFlank [] = null;
    protected float abutmentFrontFlankTexture [] = null;
    protected float abutmentRearFlank [] = null;
    protected float abutmentRearFlankTexture [] = null;
    protected float abutmentFrontFace [] = null;
    protected float abutmentRearFace [] = null;
    protected float abutmentFrontTop [] = null;
    protected float abutmentRearTop [] = null;
    protected float abutmentFaceNormals [] = null;
    protected float abutmentFrontFaceTexture [] = null;
    protected float abutmentRearFaceTexture [] = null;
    protected float pillowFrontFace [] = new float[9];
    protected float pillowRearFace [] = new float[9];

    /**
     * Initialize the polygons for the west abutment. Must be called after terrain surface and centerline
     * are already initialized for this site condition.
     *
     * @param archHeight
     * @param halfWidth
     */
    private void initializeAbutment(float archHeight, float halfWidth) {

        abutmentFaceNormals = new float [] {
            1f, 0f, 0f,
            0f, 1f, 0f,
            1f, 0f, 0f,
        };
        abutmentRearFace = new float[abutmentFaceNormals.length + 3];
        abutmentFrontFace = new float[abutmentFaceNormals.length + 3];

        int iFace = 0;
        int iFlank = 0;
        int iPillow = 0;

        abutmentRearFace[iFace + 0] = abutmentFrontFace[iFace + 0] = abutmentStepInset;
        abutmentRearFace[iFace + 1] = abutmentFrontFace[iFace + 1] = wearSurfaceHeight;
        abutmentRearFace[iFace + 2] = -halfWidth;
        abutmentFrontFace[iFace + 2] = halfWidth;
        iFace += 3;

        // Allocate oversize buffer and copy to final ones later.
        float flank [] = new float [3 * (postCount + 4)];

        // Search for j grid coordinate of left edge of abutment.
        int jAbutmentLeft;
        for (jAbutmentLeft = halfGridCount; jAbutmentLeft > 1; jAbutmentLeft--) {
            if (roadCenterline[jAbutmentLeft].elevation - posts[halfGridCount][jAbutmentLeft].elevation <= 2 * epsPaint) {
                break;
            }
        }
        --jAbutmentLeft;
        float xAbutmentLeft = xGridToWorld(jAbutmentLeft);

        // Inner corner of abutment step.
        abutmentRearFace[iFace + 0] = abutmentFrontFace[iFace + 0] = flank[iFlank + 0] =
            pillowRearFace[iPillow + 0] = pillowFrontFace[iPillow + 0] = abutmentStepInset;
        abutmentRearFace[iFace + 1] = abutmentFrontFace[iFace + 1] = flank[iFlank + 1] =
            pillowRearFace[iPillow + 1] = pillowFrontFace[iPillow + 1] = abutmentStepHeight - archHeight;
        flank[iFlank + 2] = -halfWidth;
        abutmentRearFace[iFace + 2] = pillowRearFace[iPillow + 2] = -halfWidth;
        abutmentFrontFace[iFace + 2] = pillowFrontFace[iPillow + 2] = halfWidth;
        iFace += 3;
        iFlank += 3;
        iPillow += 3;

        // Peak of pillow.
        pillowRearFace[iPillow + 0] = pillowFrontFace[iPillow + 0] = 0.5f * (abutmentStepInset + abutmentStepWidth);
        pillowRearFace[iPillow + 1] = pillowFrontFace[iPillow + 1] = -archHeight;
        pillowRearFace[iPillow + 2] = -halfWidth;
        pillowFrontFace[iPillow + 2] = halfWidth;
        iPillow += 3;

        // Step.
        abutmentRearFace[iFace + 0] = abutmentFrontFace[iFace + 0] = flank[iFlank + 0] =
            pillowRearFace[iPillow + 0] = pillowFrontFace[iPillow + 0] = abutmentStepWidth;
        abutmentRearFace[iFace + 1] = abutmentFrontFace[iFace + 1] = flank[iFlank + 1] =
            pillowRearFace[iPillow + 1] = pillowFrontFace[iPillow + 1] = abutmentStepHeight - archHeight;
        flank[iFlank + 2] = -halfWidth;
        abutmentRearFace[iFace + 2] = pillowRearFace[iPillow + 2] = -halfWidth;
        abutmentFrontFace[iFace + 2] = pillowFrontFace[iPillow + 2] = halfWidth;
        iFlank += 3;
        iFace += 3;

        // Base of face.
        abutmentRearFace[iFace + 0] = abutmentFrontFace[iFace + 0] = flank[iFlank + 0] = abutmentStepWidth;
        abutmentRearFace[iFace + 1] = abutmentFrontFace[iFace + 1] = flank[iFlank + 1] = yWater;
        flank[iFlank + 2] = -halfWidth;
        abutmentRearFace[iFace + 2] = -halfWidth;
        abutmentFrontFace[iFace + 2] = halfWidth;
        iFlank += 3;
        iFace += 3;

        // Base rear.
        flank[iFlank + 0] = xAbutmentLeft;
        flank[iFlank + 1] = yWater;
        flank[iFlank + 2] = -halfWidth;
        iFlank += 3;

        // Add points for wear surface left to right.
        int iTop = iFlank;
        float x = xAbutmentLeft;
        for (int j = jAbutmentLeft; j < postCount; j++) {
            if (x >= abutmentStepInset) {
                // Rightmost wear surface point.
                flank[iFlank + 0] = abutmentStepInset;
                flank[iFlank + 1] = wearSurfaceHeight;
                flank[iFlank + 2] = -halfWidth;
                iFlank += 3;
                break;
            }
            else {
                flank[iFlank + 0] = x;
                flank[iFlank + 1] = roadCenterline[j].elevation - 0.03f;
                flank[iFlank + 2] = -halfWidth;
                iFlank += 3;
            }
            x += metersPerGrid;
        }

        abutmentFrontTop = new float [iFlank - iTop];
        abutmentRearTop = new float [iFlank - iTop];
        for (int i = 0; i < abutmentFrontTop.length; i += 3) {
            abutmentRearTop[i + 0] = abutmentFrontTop[i + 0] = flank[iTop + i + 0];
            abutmentRearTop[i + 1] = abutmentFrontTop[i + 1] = flank[iTop + i + 1];
            abutmentRearTop[i + 2] = flank[iTop + i + 2];
            abutmentFrontTop[i + 2] = -flank[iTop + i + 2];
        }
        abutmentFrontFlank = new float[iFlank];
        abutmentRearFlank = new float[iFlank];

        // Copy points in forward order for rear flank polygon and in reverse order for front.
        System.arraycopy(flank, 0, abutmentRearFlank, 0, iFlank);

        abutmentFrontFlank[0] =  abutmentRearFlank[0];
        abutmentFrontFlank[1] =  abutmentRearFlank[1];
        abutmentFrontFlank[2] = -abutmentRearFlank[2];
        int j = abutmentRearFlank.length - 3;
        for (int i = 1 * 3; i < iFlank; i += 3, j -= 3) {
            abutmentFrontFlank[i + 0] =  abutmentRearFlank[j + 0];
            abutmentFrontFlank[i + 1] =  abutmentRearFlank[j + 1];
            abutmentFrontFlank[i + 2] = -abutmentRearFlank[j + 2];
        }

        abutmentFrontFaceTexture = new float [abutmentFrontFace.length * 2 / 3];
        abutmentRearFaceTexture = new float [abutmentFrontFace.length * 2 / 3];
        int i3 = 0;
        for (int i2 = 0; i2 < abutmentFrontFaceTexture.length; i2 += 2, i3 += 3) {
            abutmentFrontFaceTexture[i2] = stoneTextureSize * abutmentFrontFace[i3 + 2];
            abutmentRearFaceTexture[i2] = stoneTextureSize * abutmentRearFace[i3 + 2];
            abutmentFrontFaceTexture[i2 + 1] = abutmentRearFaceTexture[i2 + 1] = stoneTextureSize * abutmentFrontFace[i3 + 1];
        }
        abutmentFrontFlankTexture = new float [abutmentFrontFlank.length * 2 / 3];
        abutmentRearFlankTexture = new float [abutmentFrontFlank.length * 2 / 3];
        j = 0;
        for (int i = 0; i < abutmentFrontFlankTexture.length; i += 2, j += 3) {
            abutmentFrontFlankTexture[i] = stoneTextureSize * abutmentFrontFlank[j];
            abutmentFrontFlankTexture[i + 1] = stoneTextureSize * abutmentFrontFlank[j + 1];
            abutmentRearFlankTexture[i] = stoneTextureSize * abutmentRearFlank[j];
            abutmentRearFlankTexture[i + 1] = stoneTextureSize * abutmentRearFlank[j + 1];
        }
    }

    protected void setToTerrainY(float [] v, int i, float dy) {
        v[i+1] = getElevationAt(v[i+0], v[i+2]) + dy;
    }

//    protected static final float xWestTower = -158f;
    protected static final float xWestTower = -116f;
//    protected static final float zWestTower = -72f;
    protected static final float zWestTower = -102f;
    protected static final float dxTower = 90f;
    protected static final float dzTower = 70f;
    protected static final float dTower = (float)Math.sqrt(dxTower * dxTower + dzTower * dzTower);
    protected static final float xUnitPerpTower = -dzTower / dTower;
    protected static final float zUnitPerpTower = dxTower / dTower;
    protected static final float thetaTower = -(float)Math.toDegrees(Math.atan2(dzTower, dxTower));
    protected static final int towerCount = 4;
    protected static final int wirePostCountPerTower = 20;
    protected static final float dxWire = dxTower / wirePostCountPerTower;
    protected static final float dzWire = dzTower / wirePostCountPerTower;
    protected static final float droopSlope = -1f / 10f;
    protected static final float [] wireColor = { 0.4f, 0.4f, 0.4f, 1.0f };

    // This must match FlyThruTowerModel.java, which was produced from a Sketchup model.
    // For fixed eye tower, rear wires (negative z) must be first.
    protected static final Homogeneous.Point [] wireOffsets = {
        new Homogeneous.Point(-2.48f, 10.9f,            0f, 0f),
        new Homogeneous.Point(-2.48f, 10.9f + 1.5f,     0f, 0f),
        new Homogeneous.Point(-2.48f, 10.9f + 1.5f * 2, 0f, 0f),
        new Homogeneous.Point( 2.48f, 10.9f,            0f, 0f),
        new Homogeneous.Point( 2.48f, 10.9f + 1.5f,     0f, 0f),
        new Homogeneous.Point( 2.48f, 10.9f + 1.5f * 2, 0f, 0f),
    };

    protected final Homogeneous.Point towerPt [] = new Homogeneous.Point [towerCount];
    protected final Homogeneous.Point wirePt [] [] = new Homogeneous.Point [towerCount - 1] [wirePostCountPerTower + 1];

    /**
     * Initialize the power lines data structures.  Completely re-implemented 29 November 2011 for
     * legacy graphics.
     */
    private void initializePowerLines() {
        for (int iTower = 0; iTower < towerCount; iTower++) {
            float xTower = xWestTower + iTower * dxTower;
            float zTower = zWestTower + iTower * dzTower;
            // Place towers slightly below ground so they don't float over irregular terrain.
            towerPt[iTower] = new Homogeneous.Point(xTower, getElevationAt(xTower, zTower) - 0.5f, zTower);
            if (iTower > 0) {
                Homogeneous.Point p0 = towerPt[iTower - 1], p1 = towerPt[iTower];
                float dx = p1.x() - p0.x();
                float dy = p1.y() - p0.y();
                float dz = p1.z() - p0.z();
                float du = (float)Math.sqrt(dx * dx + dz * dz);
                float m = dy / du + droopSlope;
                float a = (dy - m * du) / (du * du);
                for (int iWire = 0; iWire <= wirePostCountPerTower; iWire++) {
                    float t = (float)iWire / wirePostCountPerTower;
                    float u = du * t;
                    wirePt[iTower - 1][iWire] = new Homogeneous.Point(
                            p0.x() + dx * t,
                            p0.y() + (a * u + m) * u,
                            p0.z() + dz * t);
                }
            }
        }
    }
}
