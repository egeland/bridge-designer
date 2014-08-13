/*
 * StrengthCurve.java  
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.JLabel;
import org.jdesktop.application.ResourceMap;

/**
 * Drawing of stength curves for either one set of members all made of the same stock or
 * of all such sets at the same time.  Includes lots of fussy heuristics to make things look nice.
 * 
 * @author Eugene K. Ressler
 */
public class StrengthCurve extends JLabel {

    private Material material = null;
    private Shape shape = null;
    private Member[] members = null;
    private int selectedMemberIndex = 0;
    private Member[][] memberLists = null;
    private Analysis analysis = null;
    private boolean showSlenderness = true;
    private boolean showAll = false;
    private static final int widthPad = 20;
    private static final int heightPad = 8;
    private static final int[] multipliers = {2, 4, 5, 10};
    private final ResourceMap resourceMap = BDApp.getResourceMap(StrengthCurve.class);
    private static final String longestYLabel = "888888";
    private static int titleLabelSep = 4;
    private static int labelTickSep = 0;
    private static int tickSize = 4;
    private static final int dotRadius = 2;
    private final NumberFormat labelFormatter = new DecimalFormat("0");
    private final Stroke plotStroke = new BasicStroke(3.0f);
    private float[] gridDash = {1.0f, 5.0f};
    private final Stroke gridStroke = new BasicStroke(0.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, gridDash, 0.0f);
    private float[] lengthLeaderDash = {4.0f, 8.0f};
    private final Stroke lengthTicksStroke = new BasicStroke(0.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, lengthLeaderDash, 0.0f);
    private final Stroke bracketOkStroke = new BasicStroke();
    private final Rectangle bounds = new Rectangle();
    private static final Color subduedBlue = new Color(192, 192, 255);
    private static final Color subduedRed = new Color(255, 192, 192);
    private final Color bracketOkColor = new Color(0, 144, 0); 
    private final Color subduedBracketOkColor = new Color(112, 255, 112);
    private float widthYLabel = -1;
    private float heightText = -1;
    private int yPlotAreaTop = 0;
    private int yPlotAreaBottom = 1;
    private int xPlotAreaLeft = 0;
    private int xPlotAreaRight = 1;
    private int heightPlotArea = 1;
    private int widthPlotArea = 1;
    private double xMax = 1.0;
    private double yMax = 1.0;

    /**
     * Either show stengths of all selected members or just one.
     * 
     * @param showAll true iff inventory stocks of all selected members should be shown
     */
    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
        repaint();
    }

    /**
     * Set the index of the member whose strength should be shown.
     * 
     * @param index index of member strength to show
     */
    public void setSelectedMemberIndex(int index) {
        selectedMemberIndex = (members != null && 0 <= index && index < members.length) ? index : -1;
        repaint();
    }
    
    /**
     * Initialize this strength curve by attaching current information to draw.
     * 
     * @param material stock material of member
     * @param shape stock shape of member
     * @param members all members having the material and shape above
     * @param memberLists list of lists of all selected members as returned by
     *        <code>BridgeModel.getSelectedStockLists</code>, used for <code>showAll</code> mode.
     * @param analysis current analsis of the bridge used to graph force
     * @param showSlenderness whether slenderness test limit should be drawn
     */
    public void initialize(
            Material material, Shape shape,
            Member[] members,
            Member[][] memberLists,
            Analysis analysis,
            boolean showSlenderness) {
        this.material = material;
        this.shape = shape;
        this.members = members;
        this.memberLists = memberLists;
        this.analysis = analysis;
        this.showSlenderness = showSlenderness;
        repaint();
    }

    private double getDivisionSize(double max, int nDivisions) {
        double size = Math.pow(10.0, Math.ceil(Math.log10(max)));
        double newSize = 1.0;
        for (;;) {
            for (int i = 0; i < multipliers.length; i++) {
                newSize = size / multipliers[i];
                if (newSize * nDivisions <= max) {
                    return newSize;
                }
            }
            size = newSize;
        }
    }

    /**
     * Paint the strength curve(s).
     * 
     * @param g0 java graphics context
     */
    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        final Stroke savedStroke = g.getStroke();
        // Calculate font metrics one time.
        if (widthYLabel < 0) {
            FontRenderContext frc = g.getFontRenderContext();
            Font font = Labeler.getFont();
            LineMetrics metrics = font.getLineMetrics(longestYLabel, frc);
            widthYLabel = (float) font.getStringBounds(longestYLabel, frc).getWidth();
            heightText = metrics.getAscent() + metrics.getDescent();
        }
        // Clear background.
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        // Draw the title.
        g.setColor(Color.BLACK);
        /*
        Labeler.drawJustified(g0, resourceMap.getString("title.text"), w/2, heightPad, 
        Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null);
        */

        // Compute the plot area boundaries and sizes.
        yPlotAreaTop = Math.round(heightPad + heightText + labelTickSep + tickSize);
        yPlotAreaBottom = Math.round(h - 1 - heightPad - heightText - titleLabelSep - heightText - labelTickSep - tickSize);
        xPlotAreaLeft = Math.round(widthPad + heightText + titleLabelSep + widthYLabel + labelTickSep + tickSize);
        xPlotAreaRight = w - 1 - widthPad;
        heightPlotArea = yPlotAreaBottom - yPlotAreaTop;
        widthPlotArea = xPlotAreaRight - xPlotAreaLeft;

        // Draw the axes.
        g.drawLine(xPlotAreaLeft, yPlotAreaBottom + tickSize, xPlotAreaLeft, yPlotAreaTop - tickSize);
        g.drawLine(xPlotAreaLeft - tickSize, yPlotAreaBottom, xPlotAreaRight + tickSize, yPlotAreaBottom);

        // Decide what we are plotting and able to plot based on caller-provided data.
        boolean doShowAll = showAll && memberLists != null && memberLists.length > 0;
        boolean doShowOne = material != null && shape != null;

        // If we can't plot anything at all, we're done.
        if (!doShowAll && !doShowOne) {
            return;
        }

        // Find the maximum coordinate of the y-axis.
        yMax = 0;
        if (doShowAll) {
            for (int i = 0; i < memberLists.length; i++) {
                Member member = memberLists[i][0];
                yMax = Math.max(yMax, Inventory.tensileStrength(member.getMaterial(), member.getShape()));
            }
        }
        if (doShowOne) {
            yMax = Math.max(yMax, Inventory.tensileStrength(material, shape));
        }
        if (analysis != null && members != null) {
            for (int i = 0; i < members.length; i++) {
                yMax = Math.max(yMax, analysis.getMemberCompressiveForce(members[i].getIndex()));
                yMax = Math.max(yMax, analysis.getMemberTensileForce(members[i].getIndex()));
            }
        }

        // Determine the geometry of ticks and grid lines on the y-axis.  Adjust y max upward to top of scale.
        final double dyTick = getDivisionSize(yMax, (yPlotAreaBottom - yPlotAreaTop) / 50);
        final int nYTicks = (int) Math.ceil(yMax / dyTick);
        yMax = nYTicks * dyTick;

        // Draw ticks and labels on the y-axis.  Keep track of actual label width so we can set the title.
        int actualLabelWidth = 0;
        int ix = xPlotAreaLeft - tickSize - titleLabelSep;
        for (int i = 0; i <= nYTicks; i++) {
            double t = (double) i / nYTicks;
            double y = t * yMax;
            int iy = yPlotAreaBottom - (int) Math.round(t * heightPlotArea);
            if (i > 0) {
                g.setColor(Color.GRAY);
                g.setStroke(gridStroke);
                g.drawLine(xPlotAreaLeft, iy, xPlotAreaRight, iy);
                g.setStroke(savedStroke);
            }
            g.setColor(Color.BLACK);
            g.drawLine(xPlotAreaLeft, iy, xPlotAreaLeft - tickSize, iy);
            Labeler.drawJustified(g0, labelFormatter.format(y), ix, iy,
                    Labeler.JUSTIFY_RIGHT, Labeler.JUSTIFY_CENTER, null, bounds);
            actualLabelWidth = Math.max(actualLabelWidth, bounds.width);
        }

        // Determine x-coordinate of the rotated y-axis title and draw it.
        ix -= (actualLabelWidth + titleLabelSep);
        Labeler.drawRotatedAndJustified(g0, resourceMap.getString("yAxisTitle.text"), 90,
                ix, (yPlotAreaBottom + yPlotAreaTop) / 2,
                Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null, null, null);

        // Determine a good width and scale for the x-axis.  For now just fix at 12.
        xMax = 12.0;
        // Not currently needed: final double dxTick = 1.0;
        final double nXTicks = 12;

        // Determine top of label coordinate and draw x-axis ticks and vertical grid lines.
        int iy = yPlotAreaBottom + tickSize + titleLabelSep;
        for (int i = 0; i <= nXTicks; i++) {
            double t = (double) i / nXTicks;
            double x = t * xMax;
            ix = xPlotAreaLeft + (int) Math.round(t * widthPlotArea);
            if (i > 0) {
                g.setColor(Color.GRAY);
                g.setStroke(gridStroke);
                g.drawLine(ix, yPlotAreaBottom, ix, yPlotAreaTop);
                g.setStroke(savedStroke);
            }
            g.setColor(Color.BLACK);
            g.drawLine(ix, yPlotAreaBottom, ix, yPlotAreaBottom + tickSize);
            Labeler.drawJustified(g0, labelFormatter.format(x), ix, iy, Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null);
        }

        // Draw the x-axis title.
        iy += heightText + titleLabelSep;
        Labeler.drawJustified(g0, resourceMap.getString("xAxisTitle.text"),
                (xPlotAreaLeft + xPlotAreaRight) / 2, iy, Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null);

        if (doShowAll) {
            for (int i = 0; i < memberLists.length; i++) {
                Member member = memberLists[i][0];
                if (member.getMaterial() != material || member.getShape() != shape) {
                    plot(g, member.getMaterial(), member.getShape(), true);
                }
            }
        }
        if (doShowOne) {
            if (members != null) {
                plotMemberLengths(g, members);
            }
            plot(g, material, shape, false);
        }
    }

    private void plot(Graphics2D g, Material material, Shape shape, boolean subdued) {
        final Stroke savedStroke = g.getStroke();
        g.setStroke(plotStroke);
        double yTensile = Inventory.tensileStrength(material, shape);
        final int iy = yPlotAreaBottom - (int) Math.round((yTensile / yMax) * heightPlotArea);
        g.setColor(subdued ? subduedBlue : Color.BLUE);
        g.drawLine(xPlotAreaLeft, iy, xPlotAreaRight, iy);

        final int nPlotPoints = 32;
        double yCompressive = Inventory.compressiveStrength(material, shape, 0.0);
        int iy0 = yPlotAreaBottom - (int) Math.round((yCompressive / yMax) * heightPlotArea);
        int ix0 = xPlotAreaLeft;
        g.setColor(subdued ? subduedRed : Color.RED);
        for (int i = 1; i <= nPlotPoints; i++) {
            double t = (double) i / nPlotPoints;
            double x = t * xMax;
            int ix1 = xPlotAreaLeft + (int) Math.round(t * widthPlotArea);
            yCompressive = Inventory.compressiveStrength(material, shape, x);
            int iy1 = yPlotAreaBottom - (int) Math.round((yCompressive / yMax) * heightPlotArea);
            g.drawLine(ix0, iy0, ix1, iy1);
            ix0 = ix1;
            iy0 = iy1;
        }
        g.setStroke(savedStroke);
        g.setColor(Color.BLACK);
    }

    private void plotBracket(Graphics2D g, int number, double force, int ix, int iy, boolean subdued) {
        Color savedColor = g.getColor();
        if (force > 0) {
            // Find the y coordinate of the force hash (strength hash is at iy).
            int iyForce = yPlotAreaBottom - (int) Math.round((force / yMax) * heightPlotArea);
            // Draw the vertical.
            if (iyForce < iy) {
                g.setColor(savedColor);
                g.setStroke(plotStroke);
                g.drawLine(ix, iyForce, ix, iy);
            } else {
                g.setColor(subdued ? subduedBracketOkColor : bracketOkColor);
                g.setStroke(bracketOkStroke);
                g.drawLine(ix - tickSize, iyForce, ix + tickSize, iyForce);
                g.drawLine(ix, iyForce, ix, iy);
            }
            // Draw the force tick.
            g.setColor(savedColor);
            g.setStroke(plotStroke);
            g.drawLine(ix - tickSize, iyForce, ix + tickSize, iyForce);
        }
    }

    private void plotMemberLengths(Graphics2D g, Member[] members) {
        if (members.length == 1) {
            if (showSlenderness) {
                plotSlendernessLimit(g, members[0]);
            }
            plot1MemberLength(g, members[0], false);            
            return;
        }
        for (int i = 0; i < members.length; i++) {
            if (selectedMemberIndex != i) {
                plot1MemberLength(g, members[i], true);
            }
        }
        if (selectedMemberIndex >= 0) {
            plot1MemberLength(g, members[selectedMemberIndex], false);
        }
    }
    
    private void plotSlendernessLimit(Graphics2D g, Member member) {
        double length = member.getShape().getMaxSlendernessLength();
        double t = length / xMax;
        if (t <= 1) {
            // Find x-coordinate for the member getLength and plot a vertical line there.
            int ix = xPlotAreaLeft + (int) Math.round(t * widthPlotArea);
            g.setColor(Color.MAGENTA);
            g.drawLine(ix, yPlotAreaBottom, ix, yPlotAreaTop - tickSize);
            Labeler.drawRotatedAndJustified(g, resourceMap.getString("maxSlendernessLength.text"), 90,
                    ix, (yPlotAreaBottom + yPlotAreaTop) / 2,
                    Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_TOP, null, null, null);
            g.setColor(Color.BLACK);
        }
    }
    
    private void plot1MemberLength(Graphics2D g, Member member, boolean subdued) {
        double length = member.getLength();
        double t = length / xMax;
        if (t <= 1) {
            Stroke savedStroke = g.getStroke();
            double tensileStrength = Inventory.tensileStrength(member.getMaterial(), member.getShape());
            int iyTensileStrength = yPlotAreaBottom - (int) Math.round((tensileStrength / yMax) * heightPlotArea);
            double compressiveStrength = Inventory.compressiveStrength(member.getMaterial(), member.getShape(), length);
            int iyCompressiveStrength = yPlotAreaBottom - (int) Math.round((compressiveStrength / yMax) * heightPlotArea);

            // Find x-coordinate for the member getLength and plot a vertical line there.
            int ix = xPlotAreaLeft + (int) Math.round(t * widthPlotArea);
            g.setColor(subdued ? Color.LIGHT_GRAY : Color.BLACK);
            g.setStroke(lengthTicksStroke);
            g.drawLine(ix, yPlotAreaBottom, ix, yPlotAreaTop - tickSize);

            // Label with member number.
            g.setStroke(savedStroke);
            if (!subdued) {
                Labeler.drawJustified(g, Integer.toString(member.getNumber()), ix, yPlotAreaTop - tickSize, 
                        Labeler.JUSTIFY_CENTER, Labeler.JUSTIFY_BOTTOM, Member.labelBackground);
            }

            if (analysis != null) {
                g.setColor(subdued ? subduedRed : Color.RED);
                if (!subdued) {
                    g.fillRect(ix - dotRadius, iyCompressiveStrength - dotRadius, 2 * dotRadius + 1, 2 * dotRadius + 1);
                }
                if (analysis != null) {
                    plotBracket(g, member.getNumber(), analysis.getMemberCompressiveForce(member.getIndex()), ix, iyCompressiveStrength, subdued);
                }
                g.setColor(subdued ? subduedBlue : Color.BLUE);
                if (!subdued) {
                    g.fillRect(ix - dotRadius, iyTensileStrength - dotRadius, 2 * dotRadius + 1, 2 * dotRadius + 1);
                }
                if (analysis != null) {
                    plotBracket(g, member.getNumber(), analysis.getMemberTensileForce(member.getIndex()), ix, iyTensileStrength, subdued);
                }
            }
            g.setStroke(savedStroke);
        }
    }
}
