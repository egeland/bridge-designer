package bridgedesigner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import com.jogamp.opengl.GL2;

/**
 * Implements members for bridge models.
 * 
 * @author Eugene K. Ressler
 */
public class Member implements HotEditableItem<BridgePaintContext> {

    private int index = -1;
    private Joint jointA;
    private Joint jointB;
    private Material material;
    private Shape shape;
    private boolean selected = false;
    private double compressionForceStrengthRatio = -1;
    private double tensionForceStrengthRatio = -1;

    /**
     * Normal unselected, unhot colors of member, indexed by material.
     */
    private static final Color[] normalColors = getMemberColors(0, 0);
    /**
     * Colors of unselected members during mouse-over, indexed by material.
     */
    private static final Color[] hotColors = getMemberColors(.3f, 0);
    /**
     * Colors of selected members during mouse-over, indexed by material.
     */
    private static final Color[] hotSelectedColors = getMemberColors(.3f, .9f);
    
    /**
     * Colors of unselected, unhot tube members' inner parts, indexed by material.
     */
    private static final Color[] innerColors = getInnerColors(0, 0);
    /**
     * Colors of selected tube members inner parts, indexed by material.
     */
    private static final Color[] selectedInnerColors = getInnerColors(0, .9f);
    /**
     * Colors of unselected tube members' inner parts during mouse-over, indexed by material .
     */
    private static final Color[] hotInnerColors = getInnerColors(.3f, 0);
    /**
     * Colors of selected tube members' inner parts during mouseover, indexed by material.
     */
    private static final Color[] hotSelectedInnerColors = getInnerColors(.3f, .9f);
    /**
     * Color of X-shaped wires between trusses in 3D animation.
     */
    private static final float wireColor [] = { 0.1f, 0.3f, 0.1f, 1.0f };
    /**
     * Dash pattern for centerline stroke used for move joint cursor.
     */
    private static final float centerLine[] = {10.0f, 4.0f, 4.0f, 4.0f};
    /**
     * Table of strokes where stroke width depends on member width.  Indexed by member size.
     */
    private static BasicStroke[] memberStrokes, innerStrokes;

    /**
     * Export selected member colors so that member table can match.
     */
    public static final Color[] selectedColors = getMemberColors(0, .9f);
    /**
     * Export member label background color so that member table can match.
     */
    public static final Color labelBackground = new Color(255, 255, 192);
    
    /** 
     * These tables relate height and width of a parabola with unit arc length. 
     * We interpolate linearly below to get height values for compression failures.
     */
    private static final float [] parabolaWidth =
        {0.000000f,0.062500f,0.125000f,0.156250f,
         0.187500f,0.218750f,0.250000f,0.281250f,
         0.312500f,0.343750f,0.375000f,0.406250f,
         0.437500f,0.468750f,0.500000f,0.531250f,
         0.562500f,0.593750f,0.625000f,0.656250f,
         0.687500f,0.718750f,0.750000f,0.781250f,
         0.812500f,0.843750f,0.875000f,0.906250f,
         0.937500f,0.968750f,0.984375f,0.992188f,
         1.000000f,};
    private static final float [] parabolaHeight =
        {0.500000f,0.497717f,0.492161f,0.488378f,
         0.483979f,0.478991f,0.473431f,0.467310f,
         0.460633f,0.453402f,0.445614f,0.437259f,
         0.428326f,0.418797f,0.408650f,0.397857f,
         0.386383f,0.374182f,0.361201f,0.347371f,
         0.332606f,0.316796f,0.299798f,0.281420f,
         0.261396f,0.239344f,0.214672f,0.186382f,
         0.152526f,0.108068f,0.076484f,0.054105f,
         0.000000f,};

    /**
     * This tells us how high a parabola should result if a a member with getLength <code>arcLen</code>
     * buckles to a parabola with getLength <code>width</code>.
     * 
     * @param buckledLength buckled getLength of 
     * @param baseLength
     * @return height
     */
    public static float getParabolaHeight(float buckledLength, float baseLength) {
        if (baseLength == 0) {
            return 0f;
        }
        float p = Math.min(buckledLength / baseLength, 1);
        // Binary search in pTable.
        int i = Utility.getIndexOfGreatestNotGreaterThan(p, parabolaWidth);
        if (i == parabolaWidth.length - 1) {
            return 0f;
        }
        // Interpolate.
        float t = (p - parabolaWidth[i]) / (parabolaWidth[i + 1] - parabolaWidth[i]);
        float h = (i == parabolaWidth.length - 1) ? 0f : parabolaHeight[i] * (1f - t) + parabolaHeight[i + 1] * t;
        return h * baseLength;
    }

    /**
     * Construct a new member with undefined index and materials taken from tiven member
     * 
     * @param member member to copy materials from
     * @param a first joint of member
     * @param b second joint of member
     */
    public Member(Member member, Joint a, Joint b) {
        this(a, b, member.material, member.shape);
    }
    
    /**
     * Construct a new member with undefined index and given material and shape.
     * 
     * @param a first joint of member
     * @param b second joint of member
     * @param material material of member
     * @param shape shape of member
     */
    public Member(Joint a, Joint b, Material material, Shape shape) {
        jointA = a;
        jointB = b;
        this.material = material;
        this.shape = shape;
    }

    /**
     * Construct a new member taking everything from a different member except its material and shape.
     * 
     * @param member member from which index and joints of new member are copied
     * @param material material of the member
     * @param shape shape of the member
     */
    public Member(Member member, Material material, Shape shape) {
        this(member.index, member.jointA, member.jointB, material, shape);
    }

    /**
     * Construct a new member with given index, material, and shape.
     * 
     * @param index index of the member
     * @param a first joint of member
     * @param b second joint of member
     * @param material material of member
     * @param shape shape of member
     */
    public Member(int index, Joint a, Joint b, Material material, Shape shape) {
        this(a, b, material, shape);
        this.index = index;
    }

    /**
     * Construct a new member by copying index and joints from a given member and taking material and shape
     * from a given inventory unless the respective index is unspecified (has value -1).
     * 
     * @param member member from which to take index and joints
     * @param inventory inventory of construction stocks
     * @param materialIndex index of stock material in the inventory
     * @param sectionIndex index of stock  section in the inventory
     * @param sizeIndex index of stock size in the inventory
     */
    public Member(Member member, Inventory inventory, int materialIndex, int sectionIndex, int sizeIndex) {
        index = member.index;
        jointA = member.jointA;
        jointB = member.jointB;
        if (materialIndex == -1) {
            materialIndex = member.getMaterial().getIndex();
        }
        if (sectionIndex == -1) {
            sectionIndex = member.getShape().getSection().getIndex();
        }
        if (sizeIndex == -1) {
            sizeIndex = member.getShape().getSizeIndex();
        }
        material = inventory.getMaterial(materialIndex);
        shape = inventory.getShape(sectionIndex, sizeIndex);
    }

    /**
     * Construct a new member with everything taken from an existing member except its shape.
     * 
     * @param member member from which to copy index, joints, and material
     * @param shape
     */
    public Member(Member member, Shape shape) {
        this(member, member.material, shape);
    }

    /**
     * Get last-calculated ratio of compression force on the member to strength due to analysis.
     * 
     * @return ratio of compression force to member strength
     */
    public double getCompressionForceStrengthRatio() {
        return compressionForceStrengthRatio;
    }

    /**
     * Used by Analysis to set the compression force to strength ratio.
     * 
     * @param compressionForceStrengthRatio ratio of compression force to member strength
     */
    public void setCompressionForceStrengthRatio(double compressionForceStrengthRatio) {
        this.compressionForceStrengthRatio = compressionForceStrengthRatio;
    }

    /**
     * Get last-calculated ratio of tension force on the member to strength due to analysis.
     * 
     * @return ratio of tension force to member strength
     */
    public double getTensionForceStrengthRatio() {
        return tensionForceStrengthRatio;
    }

    /**
     * Used by Analysis to set the tension force to strength ratio.
     * 
     * @param tensionForceStrengthRatio ratio of tension force to member strength
     */
    public void setTensionForceStrengthRatio(double tensionForceStrengthRatio) {
        this.tensionForceStrengthRatio = tensionForceStrengthRatio;
    }

    /**
     * Swap the joints, material, and shape of this member with another one.
     * Used for command execute/undo/redo processing.
     * 
     * @param otherSelectable the other member with which to swap contents
     */
    public void swapContents(Editable otherSelectable) {
        Member other = (Member) otherSelectable;

        Joint tmpJoint = jointA;
        jointA = other.jointA;
        other.jointA = tmpJoint;

        tmpJoint = jointB;
        jointB = other.jointB;
        other.jointB = tmpJoint;

        Material tmpMaterial = material;
        material = other.material;
        other.material = tmpMaterial;

        Shape tmpShape = shape;
        shape = other.shape;
        other.shape = tmpShape;
   }

    /**
     * Set the material of this member.
     * 
     * @param material material value
     */
    public void setMaterial(Material material) {
        this.material = material;
    }

    /**
     * Get the material of this member.
     * 
     * @return material value
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Set the shape of this member.
     * 
     * @param shape shape value
     */
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    /**
     * Get the shape of this member.
     * 
     * @return shape value
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * Get the first joint of this member.
     * 
     * @return first joint
     */
    public Joint getJointA() {
        return jointA;
    }

    /**
     * Get the second joint of this member.
     * 
     * @return second joint.
     */
    public Joint getJointB() {
        return jointB;
    }

    /**
     * Get the number of this member. By convention, this is just one more than the index.
     * 
     * @return member number
     */
    public int getNumber() {
        return index + 1;
    }

    /**
     * Set the 0-based index of this member.
     * 
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Get the 0-based index of this member.
     * @return index of member
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the slenderness ratio for this member.
     * 
     * @return slenderness ratio
     */
    public double getSlenderness() {
        return getLength() * shape.getInverseRadiusOfGyration();
    }
    
    /**
     * Return an indicator of whether this member is selected or not.
     * 
     * @return true iff the member is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set the selected indicator for this member.
     * 
     * @param selected new value of indicator
     * @return true iff the set operation resulted in a change
     */
    public boolean setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            return true;
        }
        return false;
    }

    /**
     * Get the geometric getLength of this member.
     * 
     * @return Euclidean getLength
     */
    public double getLength() {
        return jointA.getPointWorld().distance(jointB.getPointWorld());
    }
    
    /**
     * Compute the pick distance from a point to a member where the joints have given world pixelRadius.
     * 
     * @param pt pick point
     * @param jointRadius world joint pixelRadius
     * @return pick distance to member
     */
    public double pickDistanceTo(Affine.Point pt, double jointRadius) {
        Affine.Point a = jointA.getPointWorld();
        Affine.Point b = jointB.getPointWorld();
        Affine.Vector d = b.minus(a);
        double len = d.length();
        // Do point pick if line is less than 2 joint diameters long.
        if (4 * jointRadius >= len) {
            return pt.distance(a.plus(d.times(0.5)));
        }
        // Else do a line segment pick.
        Affine.Vector v = d.unit(2 * jointRadius);
        return pt.distanceToSegment(a.plus(v), b.minus(v));
    }

    /**
     * Check whether the given joint is one of the joints of this member.
     * 
     * @param joint other joint
     * @return truee iff the given joint is one of this member's joints
     */
    public boolean hasJoint(Joint joint) {
        return (joint == jointA || joint == jointB);
    }

    /**
     * Check whether the given joints are the joints of this member.
     * 
     * @param jointA first joint
     * @param jointB second joint
     * @return true iff the given joints match this member
     */
    public boolean hasJoints(Joint jointA, Joint jointB) {
        return hasJoint(jointA) && hasJoint(jointB);
    }

    /**
     * If the given joint is one of this member's joints, return the other one.  Return null
     * iff the given joint does not belong to this member.
     * 
     * @param joint joint to check for
     * @return other member of joint or null if given joint does not belong to this member
     */
    public Joint otherJoint(Joint joint) {
        return joint == jointA ? jointB : joint == jointB ? jointA : null;
    }

    /**
     * Initialize all the drawing resources of this object using the given stock inventory.
     * 
     * @param inventory stock inventory
     */
    public static void initializeDrawing(Inventory inventory) {
        // Assume all sections have same number of shapes.  Okay for current inventory.
        memberStrokes = new BasicStroke[1 + inventory.getNShapes(0)];
        innerStrokes = new BasicStroke[memberStrokes.length];
        memberStrokes[0] = new BasicStroke(0.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, centerLine, 0.0f);
        for (int i = 1; i < memberStrokes.length; i++) {
            float width = getStrokeWidth(inventory.getShape(0, i - 1));
            memberStrokes[i] = new BasicStroke(width,
                    (width <= 2 * Joint.pixelRadius) ? BasicStroke.CAP_BUTT : BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_MITER);
            float widthInner = .6f * width;
            if (width - widthInner < 2) {
                widthInner = Math.max(1, width - 2);
            }
            innerStrokes[i] = new BasicStroke(widthInner, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        }
    }

    /**
     * Return the stroke width that should be used to represent the given member shape.
     * 
     * @param shape shape to use as basis for stroke width
     * @return stroke width
     */
    private static float getStrokeWidth(Shape shape) {
        // Ensure tubes always have an inner area.
        return Math.max((float)shape.getWidth() * .05f, 3f);
    }

    /**
     * Return the stroke width for this member.
     * 
     * @return stroke width
     */
    public float getStrokeWidth() {
        return getStrokeWidth(shape);
    }

    /**
     * Heuristic algorithm to compute color variations for normal, selected, hot, and hot-selected states.
     * 
     * @param r base red component
     * @param g base green component
     * @param b base blue component
     * @param dBright amount to brighten colors from base
     * @param dBlue amount to blue-ify colors from base
     * @return computed color
     */
    private static Color getColor(int r, int g, int b, float dBright, float dBlue) {
        r += (int) (dBright * (255 - r));
        g += (int) (dBright * (255 - g));
        b += (int) (dBright * (255 - b));
        r -= (int) (.25f * dBlue * r);
        g -= (int) (.25f * dBlue * g);
        b += (int) (dBlue * (255 - b));
        return new Color(r, g, b);
    }

    /**
     * Compute the member color tables.
     * 
     * @param dBright amount to brighten colors from base
     * @param dBlue abount to blue-ify colors from base
     * @return member color table
     */
    private static Color[] getMemberColors(float dBright, float dBlue) {
        Color[] colors = new Color[3];
        colors[0] = getColor(128, 128, 128, dBright, dBlue);
        colors[1] = getColor(64, 64, 64, dBright, dBlue);
        colors[2] = getColor(192, 192, 192, dBright, dBlue);
        return colors;
    }
    
    /**
     * Compute the tube member inner color table.
     * 
     * @param dBright amount to brighten colors from base
     * @param dBlue abount to blue-ify colors from base
     * @return member color table
     */
    private static Color[] getInnerColors(float dBright, float dBlue) {
        Color[] colors = new Color[3];
        colors[0] = getColor(192, 192, 192, dBright, dBlue);
        colors[1] = getColor(128, 128, 128, dBright, dBlue);
        colors[2] = getColor(224, 224, 224, dBright, dBlue);
        return colors;
    }

    /**
     * Constants needed for the calculation below.
     */
    private static final float dashLength = 3f;
    private static final float spaceLength = 7f;
    private static final float [] markDashes = { dashLength, spaceLength };
    private static final float dashesLength = dashLength + spaceLength;
    private static final float halfDashesLength = 0.5f * dashesLength;
    private static final float lengthScale = 1f / dashesLength;
    private static final float phaseCoeff = dashLength * lengthScale;
    
    /**
     * Get a mark stroke with phase adjusted so ticks are centered between joints.
     * 
     * @param width base line width
     * @param getLength base line getLength
     * @return mark stroke
     */
    private static BasicStroke getMarkStroke(float width, float length) {
        final float l = length * lengthScale;
        final int n = (int)(l + phaseCoeff - .0001f);
        final float phase = (1 - l + n + phaseCoeff) * halfDashesLength;
        return new BasicStroke(width + 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, markDashes, phase);
    }

    /**
     * Raw, highly parameterized drawing of members in viewport space.
     * 
     * @param g Swing graphics object
     * @param a first end point of member
     * @param b second end point of member
     * @param number member number if member to be numbered, -1 if not
     * @param sizeIndex size index of section to be drawn
     * @param color member color
     * @param innerColor tube member inner color or null or null if not a tube
     * @param markColor color to use for member failure marks or null if none
     */
    public static void draw(Graphics2D g, Point a, Point b, int number, int sizeIndex, Color color, Color innerColor, Color markColor) {
        Stroke savedStroke = g.getStroke();
        g.setColor(color);
        final BasicStroke stroke = memberStrokes[sizeIndex];
        g.setStroke(stroke);
        g.drawLine(a.x, a.y, b.x, b.y);
        if (innerColor != null) {
            g.setColor(innerColor);
            g.setStroke(innerStrokes[sizeIndex]);
            g.drawLine(a.x, a.y, b.x, b.y);
        }
        if (markColor != null) {
            float dx = a.x - b.x;
            float dy = a.y - b.y;
            g.setStroke(getMarkStroke(stroke.getLineWidth(), (float)Math.sqrt(dx * dx + dy * dy)));
            g.setColor(markColor);
            g.drawLine(a.x, a.y, b.x, b.y);            
        }
        g.setStroke(savedStroke);
        g.setColor(Color.BLACK);
        if (number >= 0) {
            Labeler.drawJustified(g, Integer.toString(number),
                    (a.x + b.x) / 2, (a.y + b.y) / 2,
                    Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_CENTER, labelBackground);
        }
    }

    /**
     * Scratch space for displaced member end points.
     */
    private final Point ptA = new Point();
    private final Point ptB = new Point();

    /**
     * Raw, highly parameterized drawing of members in world coordinate space.
     * 
     * @param g Swing graphics object.
     * @param viewportTransform viewport transformation to go from world to viewport coords
     * @param label whether to label the member with its number
     * @param color base color of member
     * @param innerColor inner color for tube members, null if not a tube
     * @param markColor color to use for member failure marks or null if none
     */
    private void draw(Graphics2D g, ViewportTransform viewportTransform, boolean label, Color color, Color innerColor, Color markColor) {
        viewportTransform.worldToViewport(ptA, jointA.getPointWorld());
        viewportTransform.worldToViewport(ptB, jointB.getPointWorld());
        draw(g, ptA, ptB, label ? getNumber() : -1, 1 + shape.getSizeIndex(), color, innerColor, markColor);
    }

    /**
     * Return the mark color for this member or null if no marks.  The allowable slenderness is provided 
     * through a painting context. This supports scenarios where the slenderness test is not done.
     * 
     * @param ctx painting context
     * @return mark color
     */
    private Color getMarkColor(BridgePaintContext ctx) {
        return (getSlenderness() > ctx.allowableSlenderness) ? Color.MAGENTA : 
                    (compressionForceStrengthRatio > 1) ? Color.RED : 
                        (tensionForceStrengthRatio > 1) ? Color.BLUE : null;
    }

    /**
     * Paint this member as a blueprint rep.
     * 
     * @param g Swing printing graphics object
     * @param viewportTransform transform from world to viewport coordinates
     */
    private void paintBlueprint(Graphics2D g, ViewportTransform viewportTransform) {
        // We're printing... Draw member as parallel lines.
        Affine.Point ptAworld = jointA.getPointWorld();
        Affine.Point ptBworld = jointB.getPointWorld();
        Affine.Vector uPerp = ptBworld.minus(ptAworld).unit(0.5 * getWidthInMeters()).perp();
        viewportTransform.worldToViewport(ptA, ptAworld.plus(uPerp));
        viewportTransform.worldToViewport(ptB, ptBworld.plus(uPerp));
        g.drawLine(ptA.x, ptA.y, ptB.x, ptB.y);
        viewportTransform.worldToViewport(ptA, ptAworld.minus(uPerp));
        viewportTransform.worldToViewport(ptB, ptBworld.minus(uPerp));
        g.drawLine(ptA.x, ptA.y, ptB.x, ptB.y);
        // Handle label differently here than for screen. Because printing is in twips, we must
        // scale back to points temporarily for the font to match other text.
        AffineTransform savedTransform = g.getTransform();
        Stroke savedStroke = g.getStroke();
        Font savedFont = g.getFont();
        // Find member midpoint in viewport coords and shift origin there.
        viewportTransform.worldToViewport(ptA, ptAworld);
        viewportTransform.worldToViewport(ptB, ptBworld);
        g.translate((ptA.x + ptB.x)/2, (ptA.y + ptB.y)/2);
        // Back to points. 
        g.scale(20, 20);
        // Use slightly smaller than table font.
        g.setFont(savedFont.deriveFont(savedFont.getSize() - 1.5f));
        // Compute text and get bounding box.
        String numberString = Integer.toString(getNumber());
        FontMetrics fm = g.getFontMetrics();
        LineMetrics lm = fm.getLineMetrics(numberString, g);
        Rectangle2D.Float numberStringBounds = new Rectangle2D.Float();
        numberStringBounds.setRect(fm.getStringBounds(numberString, g));
        // Shift origin to center of bounding box.
        g.translate(-numberStringBounds.getWidth() / 2, lm.getAscent() - numberStringBounds.getHeight() / 2);
        // 1 pt of left and right padding
        numberStringBounds.x -= 1;
        numberStringBounds.width += 2;
        // If tall and skinny, widen to a square.
        if (numberStringBounds.width < numberStringBounds.height) {
            float diff = numberStringBounds.height - numberStringBounds.width;
            numberStringBounds.x -= 0.5f * diff;
            numberStringBounds.width += diff;
        }
        // Fill and draw enclosing box and then number as text.
        g.setColor(Color.white);                
        g.fill(numberStringBounds);
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(.25f));
        g.draw(numberStringBounds);
        g.drawString(numberString, 0, 0);
        // Restore.
        g.setTransform(savedTransform);
        g.setStroke(savedStroke);
        g.setFont(savedFont);
    }

    /**
     * Paint this member.
     * 
     * @param g Swing graphics object
     * @param viewportTransform transformation from world to viewport coordinates
     * @param ctx painting context
     */
    public void paint(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext ctx) {
        if (ctx.blueprint) {
            paintBlueprint(g, viewportTransform);
        }
        else {
            final Color markColor = getMarkColor(ctx);
            final Color mainColor = isSelected() ? selectedColors[material.getIndex()] : normalColors[material.getIndex()];
            Color innerColor = null;
            if (shape.getSection().getIndex() == 1) {
                // Tube cross section. Fill in with narrower line.
                innerColor = isSelected() ? selectedInnerColors[material.getIndex()] : innerColors[material.getIndex()];
            }
            draw(g, viewportTransform, ctx.label || isSelected(), mainColor, innerColor, markColor);
        }
    }

    /**
     * Paint this member with its mouse over appearance.
     * 
     * @param g Swing graphics object
     * @param viewportTransform transformation from world to viewport coordinates
     * @param ctx painting context
     */
    public void paintHot(Graphics2D g, ViewportTransform viewportTransform, BridgePaintContext ctx) {
        final Color markColor = getMarkColor(ctx);
        final Color hotColor = isSelected() ? hotSelectedColors[material.getIndex()] : hotColors[material.getIndex()];
        Color innerColor = null;
        if (shape.getSection().getIndex() == 1) {
            // Tube cross section. Fill in with narrower line.
            innerColor = isSelected() ? hotSelectedInnerColors[material.getIndex()] : hotInnerColors[material.getIndex()];
        }
        draw(g, viewportTransform, ctx.label || isSelected(), hotColor, innerColor, markColor);
        jointA.paint(g, viewportTransform, ctx);
        jointB.paint(g, viewportTransform, ctx);
    }

    /**
     * Get the rectangular extent of this member in viewport coordinates.
     * 
     * @param extent extent rectangle
     * @param viewportTransform transformation from world to viewport coordinates
     */
    public void getViewportExtent(Rectangle extent, ViewportTransform viewportTransform) {
        viewportTransform.worldToViewport(ptA, jointA.getPointWorld());
        viewportTransform.worldToViewport(ptB, jointB.getPointWorld());
        extent.setFrameFromDiagonal(ptA, ptB);
        int halfWidth = 1 + (int)(getStrokeWidth() / 2);
        extent.grow(halfWidth, halfWidth);
    }

    /**
     * Get the 3D drawing width of this member in meters. 
     * 
     * @return member width in meters
     */
    public float getWidthInMeters() {
        return (float)shape.getWidth() * 0.001f; // millimeter correction
    }
    
    /**
     * Prototypical cube to stretch and rotate to form 3D member.
     */
    private static final float [] cubeVerticesLeft = { 
        0.0f, +.5f, +.5f,
        0.0f, +.5f, -.5f,
        0.0f, -.5f, -.5f,
        0.0f, -.5f, +.5f,
        0.0f, +.5f, +.5f,
    };
    private static final float [] cubeVerticesRight = { 
        1.0f, +.5f, +.5f,
        1.0f, +.5f, -.5f,
        1.0f, -.5f, -.5f,
        1.0f, -.5f, +.5f,
        1.0f, +.5f, +.5f,
    };
    private static final float [] cubeNormals = {
        0.0f, 1.0f, 0.0f,
        0.0f, 0.0f,-1.0f,
        0.0f,-1.0f, 0.0f,
        0.0f, 0.0f, 1.0f,
    };
    
    /**
     * Paint 3D representation of this member.
     * 
     * @param gl OpenGL object.
     * @param xa x coordinate of first joint
     * @param ya y coordinate of first joint
     * @param xb x coordinate of second joint
     * @param yb y coordinate of second joint
     * @param z z coordinate of the member
     */
    private void paint(GL2 gl, float xa, float ya, float xb, float yb, float z) {
        final float width = getWidthInMeters();
        final float dx = xb - xa;
        final float dy = yb - ya;
        final float len = (float)Math.sqrt(dx * dx + dy * dy);
        gl.glPushMatrix();
        gl.glTranslatef(xa, ya, z);
        gl.glMultMatrixf(Utility.rotateAboutZ(dx / len, dy / len), 0);
        gl.glScaled(len, width, width);
        gl.glBegin(GL2.GL_QUADS);
        for (int i = 0; i < cubeNormals.length; i += 3) {
            gl.glNormal3fv(cubeNormals, i);
            gl.glVertex3fv(cubeVerticesLeft,  i + 0);
            gl.glVertex3fv(cubeVerticesRight, i + 0);
            gl.glVertex3fv(cubeVerticesRight, i + 3);
            gl.glVertex3fv(cubeVerticesLeft,  i + 3);
        }
        gl.glEnd();
        gl.glPopMatrix();        
    }

    /**
     * Get the points of a parabola with given width and height.
     * 
     * @param pts consecutive x,y coordinates of the parabola in order (length should be (2k + 1) * 2 for some k)
     * @param n consecutive x,y components of unit normals
     * @param width width of parabola
     * @param height height of parabola
     */
    public static void makeParabola (float [] pts, float [] n, double width, double height) {
        float fw = (float)width;
        float fh = (float)height;
        // Fill in x values depending on shape of parabola.
        if (fw > 2 * fh) {
            // low and flat
            final float dx = fw / (pts.length / 2 - 1);
            float x = 0;
            for (int i = 0; i < pts.length; i += 2, x += dx) {
                pts[i + 0] = x;
            }
        }
        else {
            // high arch
            final int nSlices = (pts.length / 2 - 1) / 2;
            final double dy = 1.0 / nSlices;
            double y = 0;
            int i;
            for (i = 0; i < 2 * nSlices; i += 2, y += dy) {
                float dx = 0.5f * (float)(width * Math.sqrt(y));
                pts[i + 0] = dx;
                pts[pts.length - 2 - i] = fw - dx;
            }
            pts[i + 0] = fw * 0.5f;
        }
        // Fill in y and normal values.
        for (int i = 0; i < pts.length; i += 2) {
            final float x = pts[i + 0];
            final float t = 2 * x / fw - 1;
            pts[i + 1] = fh * (1 - t * t);
            final float nx = 4 * fh * t / fw;
            final float len = (float)Math.sqrt(nx * nx + 1);
            n[i + 0] = nx / len;
            n[i + 1] = 1 / len;
        }
    }

    /**
     * Scratch space for member displayed as buckled parabola.
     */
    private float [] pts = new float [66];
    private float [] normals = new float[pts.length];
    private float [] topChord = new float[pts.length];
    private float [] bottomChord = new float[pts.length];

    /**
     * Paint a member failing by buckling.
     * 
     * @param gl OpenGL object.
     * @param xa x coordinate of first end point
     * @param ya y coordinate of first end point
     * @param xb x coordinate of second end point
     * @param yb y coordinate of second end point
     * @param z z coordinate of member
     * @param arcLen length of member without buckling
     */
    private void paintParabola(GL2 gl, float xa, float ya, float xb, float yb, float z, float arcLen) {
        final float wHalf = 0.5f * getWidthInMeters() + 0.005f;
        final float dx = xb - xa;
        final float dy = yb - ya;
        final float len = (float)Math.sqrt(dx * dx + dy * dy);
        makeParabola(pts, normals, len, getParabolaHeight(len, arcLen));
        for (int i = 0; i < pts.length; i += 2) {
            float px = pts[i + 0];
            float py = pts[i + 1];
            float nx = normals[i + 0] * wHalf;
            float ny = normals[i + 1] * wHalf;
            topChord[i + 0] = px + nx;
            topChord[i + 1] = py + ny;
            bottomChord[i + 0] = px - nx;
            bottomChord[i + 1] = py - ny;
        }
        gl.glPushMatrix();
        gl.glTranslatef(xa, ya, z);
        gl.glMultMatrixf(Utility.rotateAboutZ(dx / len, dy / len), 0);
        
        gl.glBegin(GL2.GL_QUAD_STRIP);
        gl.glNormal3f(0f, 0f, 1f);
        for (int i = 0; i < pts.length; i += 2) {
            gl.glVertex3f(topChord[i + 0],    topChord[i + 1],    wHalf);
            gl.glVertex3f(bottomChord[i + 0], bottomChord[i + 1], wHalf);
        }
        gl.glEnd();
        
        gl.glBegin(GL2.GL_QUAD_STRIP);
        gl.glNormal3f(0f, 0f, -1f);
        for (int i = 0; i < pts.length; i += 2) {
            gl.glVertex3f(bottomChord[i + 0], bottomChord[i + 1], -wHalf);
            gl.glVertex3f(topChord[i + 0],    topChord[i + 1],    -wHalf);
        }
        gl.glEnd();
        
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i < pts.length; i += 2) {
            gl.glNormal3f(normals[i + 0], normals[i + 1], 0f);
            gl.glVertex3f(topChord[i + 0], topChord[i + 1], -wHalf);
            gl.glVertex3f(topChord[i + 0], topChord[i + 1],  wHalf);
        }
        gl.glEnd();
        
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i < pts.length; i += 2) {
            gl.glNormal3f(-normals[i + 0], -normals[i + 1], 0f);
            gl.glVertex3f(bottomChord[i + 0], bottomChord[i + 1],  wHalf);
            gl.glVertex3f(bottomChord[i + 0], bottomChord[i + 1], -wHalf);
        }
        gl.glEnd();
 
        gl.glPopMatrix();
    }
    
    /**
     * Color used for cross-section of a tension-failed member.
     */
    private static final float [] brokenEndColor = { 0.3f, 0.3f, 0.3f, 1.0f };
    /**
     * Scratch space for painting members broken by tension.
     */
    private final float [] half = new float [8];
    
    /**
     * Helper routine to paint the quad at the broken end of a tension-failed member.
     * 
     * @param gl OpenGL object.
     * @param i index pointing to first of two line segments of the quad
     * @param dz half-depth of the member section
     */
    private void paintWebQuad(GL2 gl, int i, float dz) {
        gl.glVertex3f(half[i + 0], half[i + 1],  dz);
        gl.glVertex3f(half[i + 0], half[i + 1], -dz);
        i += 2;  
        gl.glVertex3f(half[i + 0], half[i + 1], -dz);
        gl.glVertex3f(half[i + 0], half[i + 1],  dz);
    }
    
    /**
     * Paint half of a tension broken member.
     * 
     * @param gl OpenGL object
     * @param dz half-depth of member
     */
    private void paintBrokenHalf(GL2 gl, float dz) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0f, 0f, 1f);
        for (int i = 0; i < half.length; i += 2) {
            gl.glVertex3f(half[i + 0], half[i + 1], dz);            
        }
        gl.glNormal3f(0f, 0f, -1f);
        for (int i = half.length - 2; i >= 0; i -= 2) {
            gl.glVertex3f(half[i + 0], half[i + 1], -dz);            
        }
        
        gl.glNormal3f(0f, -1f, 0f);
        paintWebQuad(gl, 0, dz);

        gl.glNormal3f(0f, 1f, 0f);
        paintWebQuad(gl, 4, dz);
        
        gl.glEnd();

        // Save the current color.
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glColor3fv(brokenEndColor, 0);

        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(1f, 0f, 0f);
        paintWebQuad(gl, 2, dz);
        gl.glEnd();

        // Restore color.
        gl.glPopAttrib();
    }
    
    /**
     * Paint a member as two broken halves due to tension failure.
     * 
     * @param gl OpenGL object
     * @param xa x coordinate of first point
     * @param ya y coordinate of first point
     * @param xb x coordinate of second point
     * @param yb y coordinate of second point
     * @param z z coordinate of member
     * @param baseLength member length with no load
     */
    private void paintBroken(GL2 gl, float xa, float ya, float xb, float yb, float z, float baseLength) {
        final float wHalf = 0.5f * getWidthInMeters();
        float dx = xb - xa;
        float dy = yb - ya;
        final float len = (float)Math.sqrt(dx * dx + dy * dy);
        final float mid = 0.5f * baseLength;
        final float ofs = 0.8f * Math.min(wHalf, mid);
        int i = -2;
        i += 2;
        half[i + 0] = 0f;
        half[i + 1] = -wHalf;
        i += 2;
        half[i + 0] = mid + ofs;
        half[i + 1] = -wHalf;
        i += 2;
        half[i + 0] = mid - ofs;
        half[i + 1] = wHalf;
        i += 2;
        half[i + 0] = 0f;
        half[i + 1] = wHalf;

        dx /= len;
        dy /= len;
        
        gl.glPushMatrix();
        gl.glTranslatef(xa, ya, z);
        gl.glMultMatrixf(Utility.rotateAboutZ(dx, dy), 0);
        paintBrokenHalf(gl, wHalf);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslatef(xb, yb, z);
        gl.glMultMatrixf(Utility.rotateAboutZ(-dx, -dy), 0);
        paintBrokenHalf(gl, wHalf);
        gl.glPopMatrix();
    }
    
    /**
     * Scratch color for painting 3d rep.
     */
    private final float [] color = { 0f, 0f, 0f, 1f };
            
    /**
     * Paint a 3d representation of this member.
     * 
     * @param gl OpenGL object
     * @param aDisp displacement vector for the first joint
     * @param bDisp displacement vector for the second joint
     * @param zOffset offset of member from z = 0 plane
     * @param forceRatio ratio of force to strength; negative is compression, positive tension
     * @param showColors whether to show colors depicting tension and compression ratios in members
     * @param status overloaded status information; base length if failed or a special value indicating not failed
     */
    public void paint(GL2 gl, Affine.Vector aDisp, Affine.Vector bDisp, double zOffset, double forceRatio, boolean showColors, double status) {
        float xa = (float) (jointA.getPointWorld().x + aDisp.x);
        float yaRaw = (float) jointA.getPointWorld().y;
        float ya = (float) (yaRaw + aDisp.y);
        float xb = (float) (jointB.getPointWorld().x + bDisp.x);
        float ybRaw = (float) jointB.getPointWorld().y;
        float yb = (float) (ybRaw + bDisp.y);
        float z = (float) zOffset;
        if (showColors) {
            if (forceRatio < 0.0) {
                final float f = -(float)forceRatio;
                color[0] = 0.5f * (1.0f + f);
                color[1] = color[2] = 0.5f * (1.0f - f);
            }
            else {
                final float f = (float)forceRatio;
                color[0] = color[1] = 0.5f * (1.0f - f);
                color[2] = 0.5f * (1.0f + f);
            }
        }
        else {
            color[0] = color[1] = color[2] = .5f;
        }
        gl.glColor3fv(color, 0);
        if (!Analysis.isStatusBaseLength(status)) {
            // member has not failed
            paint(gl, xa, ya, xb, yb, z);
            paint(gl, xa, ya, xb, yb, -z);
            if ((yaRaw >= FlyThruAnimation.deckClearance && ybRaw >= FlyThruAnimation.deckClearance) || (yaRaw <= 0 && ybRaw <= 0)){
                gl.glColor3fv(wireColor, 0);
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3f(xa, ya,  z);
                gl.glVertex3f(xb, yb, -z);
                gl.glVertex3f(xa, ya, -z);
                gl.glVertex3f(xb, yb,  z);
                gl.glEnd();
            }
        }
        // member failed
        else if (forceRatio < 0.0) {
            final float baseLength = (float) status;
            paintParabola(gl, xa, ya, xb, yb,  z, baseLength);
            paintParabola(gl, xa, ya, xb, yb, -z, baseLength);
        }
        else {
            final float baseLength = (float) status;
            paintBroken(gl, xa, ya, xb, yb,  z, baseLength);
            paintBroken(gl, xa, ya, xb, yb, -z, baseLength);
        }
    }

    /**
     * Get a string representation of this member.  Used for tip text.
     * 
     * @return string representation
     */
    @Override public String toString() {
        return BDApp.getResourceMap(Member.class).getString("rollover.text", 
                getNumber(), shape.getName(), material, shape.getSection());
    }

    /**
     * No cursor for hot selectable interface.
     * 
     * @return null cursor
     */
    public Cursor getCursor() {
        return null;
    }
}
