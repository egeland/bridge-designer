/*
 * BridgeBlueprintPrintable.java  
 *   
 * Copyright (C) 2010 Eugene K. Ressler
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import javax.swing.UIManager;
import org.jdesktop.application.ResourceMap;

/**
 * Swing Printable to render a blueprint of a bridge, which consists of a nice drawing and tables
 * containing numbered member geometric data.
 * 
 * @author Eugene K. Ressler
 */
public class BridgeBlueprintPrintable implements Printable {

    private final String fileName;
    private final BridgeModel bridge;
    private final ViewportTransform viewportTransform;
    private final BridgeBlueprintView bridgeBlueprintView;
    private final Font standardFont = UIManager.getFont("Label.font").deriveFont(6.5f);
    private final Stroke standardStroke = new BasicStroke(.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    
    /**
     * Tables contained in the drawing.
     */
    private final Table memberTable = new Table();
    private final Table titleBlock = new Table();
    
    /**
     * Extent of the bridge blueprint.
     */
    private Rectangle blueprintExtent;
    
    /**
     * Conversion constants for twips.
     */
    private static final double PTS_PER_INCH = 72;
    private static final double TWIPS_PER_PT = 20;
    private static final double TWIPS_PER_INCH = PTS_PER_INCH * TWIPS_PER_PT;
    
    /**
     * Margin dimensions.
     */
    private static final double leftViewportMargin = 0.65 * TWIPS_PER_INCH;
    private static final double rightViewportMargin = 0.65 * TWIPS_PER_INCH;
    private static final double topViewportMargin = rightViewportMargin;
    private static final double bottomViewportMargin = leftViewportMargin;

    /**
     * Construct a printable to print a bridge as a blueprint.
     * 
     * @param fileName file name printed in title block
     * @param bridge bridge to draw
     */
    public BridgeBlueprintPrintable(String fileName, BridgeModel bridge) {
        this.fileName = fileName;
        this.bridge = bridge;
        viewportTransform = new ViewportTransform();
        viewportTransform.setVAlign(ViewportTransform.TOP);
        bridgeBlueprintView = new BridgeBlueprintView(bridge);
        memberTable.setContents(getMemberTableCells(), 3);  
        titleBlock.setContents(getTitleBlockCells(), 0);
    }

    /**
     * Build cells of member table.
     * 
     * @return 2d array of cells for the member table
     */
    private Cell [] [] getMemberTableCells() {
        ResourceMap resourceMap = BDApp.getResourceMap(BridgeBlueprintPrintable.class);
        Cell [] [] cells = new Cell [3 + bridge.getMembers().size()][5];
        cells[0][0] = new Cell(resourceMap.getString("memberTableTitle.text")).setColSpan(5);
        cells[1][0] = new Cell(resourceMap.getString("memberTableNumber.text")).setRowSpan(2);
        cells[1][1] = new Cell(resourceMap.getString("memberTableMaterial.text")).setRowSpan(2);
        
        cells[1][2] = new Cell(resourceMap.getString("memberTableCross.text")).setBorder(1, 1, 0, 1);
        cells[2][2] = new Cell(resourceMap.getString("memberTableSection.text")).setBorder(0, 1, 1, 1);
        
        cells[1][3] = new Cell(resourceMap.getString("memberTableSize.text")).setBorder(1, 1, 0, 1);
        cells[2][3] = new Cell(resourceMap.getString("memberTableSizeUnits.text")).setBorder(0, 1, 1, 1);
        
        cells[1][4] = new Cell(resourceMap.getString("memberTableLength.text")).setBorder(1, 1, 0, 1);
        cells[2][4] = new Cell(resourceMap.getString("memberTableLengthUnits.text")).setBorder(0, 1, 1, 1);
        
        int i = 2;
        Iterator<Member> e = bridge.getMembers().iterator();
        while (e.hasNext()) {
            Member m = e.next();
            ++i;
            cells[i][0] = new Cell(Integer.toString(m.getNumber())).setHAlign(Cell.HALIGN_RIGHT);
            cells[i][1] = new Cell(m.getMaterial().getShortName());
            cells[i][2] = new Cell(m.getShape().getSection().getShortName());
            cells[i][3] = new Cell(m.getShape().getName());
            cells[i][4] = new Cell(String.format(Locale.US, "%.2f", m.getLength())).setHAlign(Cell.HALIGN_RIGHT);
        }
        return cells;
    }
    
    /**
     * Build cells of title block.
     * 
     * @return 2d array of cells
     */
    private Cell [] [] getTitleBlockCells() {
        ResourceMap resourceMap = BDApp.getResourceMap(BridgeBlueprintPrintable.class);
        Cell [] [] cells = new Cell[6][3];
        cells[0][0] = new Cell(bridge.getProjectName()).setColSpan(3);
        cells[1][0] = new Cell(resourceMap.getString("titleBlockElevation.text")).setColSpan(2);
        cells[1][2] = new Cell("<pageNo>").setHAlign(Cell.HALIGN_LEFT);
        cells[2][0] = new Cell(resourceMap.getString("titleBlockCost.text", currencyFormat.format(bridge.getTotalCost()))).setHAlign(Cell.HALIGN_LEFT);
        String fmt = resourceMap.getString("titleBlockDate.text");
        SimpleDateFormat dateFormat = new SimpleDateFormat(fmt == null ? "d MMM yyyy" : fmt);
        cells[2][1] = new Cell(dateFormat.format(Calendar.getInstance().getTime())).setHAlign(Cell.HALIGN_LEFT);
        cells[2][2] = new Cell(resourceMap.getString("titleBlockIteration.text", bridge.getIteration())).setHAlign(Cell.HALIGN_LEFT);
        cells[3][0] = new Cell(resourceMap.getString("titleBlockDesignedBy.text")).setHAlign(Cell.HALIGN_LEFT).setBorder(1, 0, 1, 1);
        cells[3][1] = new Cell(bridge.getDesignedBy()).setHAlign(Cell.HALIGN_LEFT).setColSpan(2).setBorder(1, 1, 1, 0);
        cells[4][0] = new Cell(resourceMap.getString("titleBlockProjectId.text")).setHAlign(Cell.HALIGN_LEFT).setBorder(1, 0, 1, 1);
        cells[4][1] = new Cell(bridge.getProjectId()).setHAlign(Cell.HALIGN_LEFT).setColSpan(2).setBorder(1, 1, 1, 0);
        cells[5][0] = new Cell(resourceMap.getString("titleBlockPath.text", fileName)).setHAlign(Cell.HALIGN_LEFT).setColSpan(3);
        return cells;
    }
    
    /**
     * Print a standard sheet border and title block.
     * 
     * @param g graphics object
     * @param pageFormat page format for drawing
     * @return available height of drawing area
     */
    private int printStandardSheet(Graphics2D g, PageFormat pageFormat, int pageNo) {
        int pageWidth = (int)pageFormat.getImageableWidth();
        int pageHeight = (int)pageFormat.getImageableHeight();
        g.drawRect(0, 0, pageWidth - 1, pageHeight - 1);
        titleBlock.getCell(1, 2).setText(BDApp.getResourceMap(BridgeBlueprintPrintable.class).getString("titleBlockPage.text", pageNo));
        titleBlock.setLocation(pageWidth - 1, pageHeight - 1, Cell.HALIGN_RIGHT, Cell.VALIGN_BOTTOM);
        titleBlock.paint(g);
        return pageHeight - titleBlock.height;
    }
    
    /**
     * Set the viewport transform to take world coordinates to printable device coordinates.
     * 
     * @param pageWidth width of page
     * @param pageHeight height of page
     */
    private void setUpBlueprintViewportTransform(int pageWidth, int pageHeight) {
        bridgeBlueprintView.initialize(bridge.getDesignConditions());
        Rectangle2D.Double window = bridgeBlueprintView.getPreferredDrawingWindow();
        viewportTransform.setWindow(window);
        final double viewportWidth  = pageWidth * TWIPS_PER_PT - leftViewportMargin - rightViewportMargin;
        final double viewportHeight = pageHeight * TWIPS_PER_PT - topViewportMargin - bottomViewportMargin;
        viewportTransform.setViewport(leftViewportMargin, viewportHeight + topViewportMargin, viewportWidth, -viewportHeight);        
    }
    
    /**
     * Return a rectangle describing the extent of the bridge blueprint in printable device coordinates.
     * 
     * @param pageWidth width of page
     * @param pageHeight height of page
     * @return extent rectangle
     */
    private Rectangle getBlueprintExtent(int pageWidth, int pageHeight) {
        setUpBlueprintViewportTransform(pageWidth, pageHeight);
        return bridgeBlueprintView.getPaintedExtent(null, viewportTransform);
    }
    
    /**
     * Paint the bridge blueprint at the current origin.
     * 
     * @param g graphics context
     * @param pageWidth width of page
     * @param pageHeight height of page
     */
    private void printBlueprint(Graphics2D g, int pageWidth, int pageHeight) {
        setUpBlueprintViewportTransform(pageWidth, pageHeight);
        AffineTransform savedXform = g.getTransform();
        g.scale(1 / TWIPS_PER_PT, 1 / TWIPS_PER_PT);
        bridgeBlueprintView.paint(g, viewportTransform);
        g.setTransform(savedXform);
    }

    private class Cell extends Rectangle {

        public static final int HALIGN_LEFT = -1;
        public static final int HALIGN_CENTER = 0;
        public static final int HALIGN_RIGHT = 1;
        public static final int VALIGN_TOP = -1;
        public static final int VALIGN_CENTER = 0;
        public static final int VALIGN_BOTTOM = 1;

        public String text = null;            
        public byte hAlign = HALIGN_CENTER;
        public byte vAlign = VALIGN_CENTER;
        public byte rowSpan = 1;
        public byte colSpan = 1;
        public byte leftBorder = 1;
        public byte rightBorder = 1;
        public byte topBorder = 1;
        public byte bottomBorder = 1;
        public byte leftPad = 2;
        public byte rightPad = 2;
        public byte topPad = 0;
        public byte bottomPad = 0;
        public Font font = null;

        public Cell(String text) {
            this.text = text;
        }

        public Cell setBorder(int top, int right, int bottom, int left) {
            this.topBorder = (byte)top;
            this.rightBorder = (byte)right;
            this.bottomBorder = (byte)bottom;
            this.leftBorder = (byte)left;
            return this;
        }
        
        public Cell setHAlign(int hAlign) {
            this.hAlign = (byte)hAlign;
            return this;
        }
        
        public Cell setVAlign(int vAlign) {
            this.vAlign = (byte)vAlign;
            return this;
        }
        
        public Cell setRowSpan(int rowSpan) {
            this.rowSpan = (byte)rowSpan;
            return this;
        }
        
        public Cell setColSpan(int colSpan) {
            this.colSpan = (byte)colSpan;
            return this;
        }
        
        public Cell setFont(Font font) {
            this.font = font;
            return this;
        }

        public void setText(String text) {
            this.text = text;
        }
        
        public Dimension getNativeDimension(Dimension dim, Graphics2D g) {
            if (dim == null) {
                dim = new Dimension();
            }
            FontMetrics fm = g.getFontMetrics(font == null ? g.getFont() : font);
            dim.width = leftBorder + leftPad + fm.stringWidth(text) + rightPad + rightBorder;
            dim.height = topBorder + topPad + fm.getAscent() + fm.getDescent() + bottomPad + bottomBorder;
            return dim;
        }

        public void paint(Graphics2D g) {
            Font savedFont = null;
            if (font != null) {
                savedFont = g.getFont();
                g.setFont(font);
            }
            if (leftBorder > 0) {
                g.setStroke(new BasicStroke(0.25f * leftBorder));
                g.drawLine(x, y, x, y + height);
            }
            if (rightBorder > 0) {
                g.setStroke(new BasicStroke(0.25f * rightBorder));
                final int xRight = x + width - rightBorder + 1;
                g.drawLine(xRight, y, xRight, y + height);
            }
            if (topBorder > 0) {
                g.setStroke(new BasicStroke(0.25f * topBorder));
                g.drawLine(x, y, x + width, y);
            }
            if (bottomBorder > 0) {
                g.setStroke(new BasicStroke(0.25f * bottomBorder));
                final int yBottom = y + height - bottomBorder + 1;
                g.drawLine(x, yBottom, x + width, yBottom);
            }
            // bounding box of open area for text
            final int xTextBox = x + leftBorder + leftPad;
            final int yTextBox = y + topBorder + topPad;
            final int widthTextBox = width - leftBorder - leftPad - rightBorder - rightPad + 1;
            final int heightTextBox = height - topBorder - topPad - bottomBorder - bottomPad + 1;
            FontMetrics fm = g.getFontMetrics();
            int xText, yText;
            switch (hAlign) {
                default:
                    xText = xTextBox;
                    break;
                case HALIGN_CENTER:
                    xText = xTextBox + (widthTextBox - fm.stringWidth(text)) / 2;
                    break;
                case HALIGN_RIGHT:
                    xText = xTextBox + widthTextBox - fm.stringWidth(text);
                    break;
            }
            switch (vAlign) {
                default:
                    yText = yTextBox + fm.getAscent();
                    break;
                case VALIGN_CENTER:
                    yText = yTextBox + (heightTextBox + fm.getAscent() - fm.getDescent()) / 2;
                    break;
                case VALIGN_BOTTOM:
                    yText = yTextBox + heightTextBox - fm.getAscent() - fm.getDescent();
                    break;
            }
            g.drawString(text, xText, yText);
            if (savedFont != null) {
                g.setFont(savedFont);
            }
        }
    }
        
    /**
     * A rudimentary table representation something like HTML tables
     * with collapsed cell borders.
     */
    private class Table extends Rectangle {
        
        private byte hAlign = Cell.HALIGN_LEFT;
        private byte vAlign = Cell.VALIGN_TOP;
        private boolean dirty = true;
        private Cell [] [] cells;
        private int nHeaderRows;
        private int [] colWidths;
        private int [] rowHeights;

        public boolean isDirty() {
            return dirty;
        }
        
        public Cell getCell(int i, int j) {
            return cells[i][j];
        }
        
        private void initialize(Graphics2D g) {
            if (!dirty || cells == null) {
                return;
            }
            dirty = false;
            if (rowHeights == null || rowHeights.length != cells.length) {
                rowHeights = new int [cells.length];
            }
            final int nCols = cells[0].length;
            if (colWidths == null || colWidths.length != nCols) {
                colWidths = new int [nCols];                
            }
            for (int i = 0; i < colWidths.length; i++) {
                colWidths[i] = 0;
            }
            for (int j = 0; j < rowHeights.length; j++) {
                rowHeights[j] = 0;
            }
            // Use native sizes to get widths and heights based on non-spanning cells.
            final Dimension dim = new Dimension();
            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < colWidths.length; j++) {
                    if (cells[i][j] != null) {
                        cells[i][j].getNativeDimension(dim, g);
                        cells[i][j].setSize(dim);
                        if (dim.width > colWidths[j] && cells[i][j].colSpan == 1) {
                            colWidths[j] = dim.width;
                        }
                        if (dim.height > rowHeights[i] && cells[i][j].rowSpan == 1) {
                             rowHeights[i] = dim.height;
                        }
                    }
                }
            }
            // Make a second pass and update widths and heights to accomodate spanning cells.
            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < colWidths.length; j++) {
                    Cell cell = cells[i][j];
                    if (cell != null) {
                        int colSpan = cells[i][j].colSpan;
                        if (cell.width > colWidths[j] && colSpan > 1) {
                            int totalMaxWidth = 0;
                            for (int jj = j; jj < j + colSpan; jj++) {
                                totalMaxWidth += colWidths[jj];
                            }
                            int dearth = cells[i][j].width - totalMaxWidth;
                            if (dearth > 0) {
                                // Distribute the dearth evenly over the widths of relevant columns.
                                int fractionalMaxWidth = 0;
                                int last = 0;
                                for (int jj = j; jj < j + colSpan; jj++) {
                                    fractionalMaxWidth += colWidths[jj];
                                    int current = dearth * fractionalMaxWidth / totalMaxWidth;
                                    colWidths[jj] += current - last;
                                    last = current;
                                }
                            }
                        }
                        int rowSpan = cell.rowSpan;
                        if (cell.height > rowHeights[i] && rowSpan > 1) {
                            int totalMaxHeight = 0;
                            for (int ii = i; ii < i + rowSpan; ii++) {
                                totalMaxHeight += rowHeights[ii];
                            }
                            int dearth = cells[i][j].height - totalMaxHeight;
                            if (dearth > 0) {
                                // Distribute the dearth evenly over the heights of relevant rows.
                                int fractionalMaxHeight = 0;
                                int last = 0;
                                for (int ii = i; ii < i + rowSpan; ii++) {
                                    fractionalMaxHeight += rowHeights[ii];
                                    int current = dearth * fractionalMaxHeight / totalMaxHeight;
                                    rowHeights[ii] += current - last;
                                    last = current;
                                }
                            }
                        }
                    }
                }
            }
            // Insert additional space if width and/or height are specified greater than natural values.
            int totalHeight = 0;
            for (int i = 0; i < rowHeights.length; i++) {
                totalHeight += rowHeights[i];
            }
            int dearth = height - totalHeight;
            if (dearth > 0) {
                int fractionalHeight = 0;
                int last = 0;
                for (int i = 0; i < rowHeights.length; i++) {
                    fractionalHeight += rowHeights[i];
                    int current = dearth * fractionalHeight / totalHeight;
                    rowHeights[i] += current - last;
                    last = current;
                }
            }
            int totalWidth = 0;
            for (int j = 0; j < colWidths.length; j++) {
                totalWidth += colWidths[j];
            }
            dearth = width - totalWidth;
            if (dearth > 0) {
                int fractionalWidth = 0;
                int last = 0;
                for (int j = 0; j < colWidths.length; j++) {
                    fractionalWidth += colWidths[j];
                    int current = dearth * fractionalWidth / totalWidth;
                    colWidths[j] += current - last;
                    last = current;
                }
            }
            int xBase = 0;
            int yBase = 0;
            switch (hAlign) {
                case Cell.HALIGN_LEFT:
                    xBase = x;
                    break;
                case Cell.HALIGN_CENTER:
                    xBase = x - totalWidth / 2;
                    break;
                case Cell.HALIGN_RIGHT:
                    xBase = x - totalWidth;
                    break;
            }
            switch (vAlign) {
               case Cell.VALIGN_TOP:
                   yBase = y;
                   break;
               case Cell.VALIGN_CENTER:
                   yBase = y - totalHeight / 2;
                   break;
               case Cell.VALIGN_BOTTOM:
                   yBase = y - totalHeight;
                   break;
            }
            // Use row widths and column heights to fill in cell rectangles.
            int yCell = 0;
            for (int i = 0; i < cells.length; i++) {
                int xCell = 0;
                for (int j = 0; j < colWidths.length; j++) {
                    Cell cell = cells[i][j];
                    if (cell != null) {
                        int cellWidth = 0;
                        for (int jj = j; jj < j + cell.colSpan; jj++) {
                            cellWidth += colWidths[jj];
                        }
                        int cellHeight = 0;
                        for (int ii = i; ii < i + cell.rowSpan; ii++) {
                            cellHeight += rowHeights[ii];
                        }
                        cell.setLocation(xBase + xCell, yBase + yCell);
                        cell.setSize(cellWidth, cellHeight);
                    }
                    xCell += colWidths[j];
                }
                width = xCell;
                yCell += rowHeights[i];
            }
            height = yCell;
        }

        private void paintRaw(Graphics2D g, int i0, int j0, int nRows, int nCols) {
            for (int i = i0; i < i0 + nRows; i++) {
                for (int j = j0; j < j0 + nCols; j++) {
                    Cell cell = cells[i][j];
                    if (cell != null) {
                        cell.paint(g);
                    }
                }
            }
        }
        
        public void paint(Graphics2D g, int i0, int j0, int nRows, int nCols) {
            if (cells == null) {
                return;
            }
            initialize(g);
            if (nRows < 0 || nRows > cells.length - nHeaderRows - i0) {
                nRows = cells.length - nHeaderRows - i0;
            }
            if (nCols < 0 || nCols > colWidths.length - j0) {
                nCols = colWidths.length - j0;
            }
            if (nRows < 0 || nCols < 0) {
                return;
            }
            paintRaw(g, 0, j0, nHeaderRows, nCols);
            AffineTransform savedTransform = g.getTransform();
            g.translate(0, cells[nHeaderRows][0].y - cells[nHeaderRows + i0][0].y);
            paintRaw(g, nHeaderRows + i0, j0, nRows, nCols);
            g.setTransform(savedTransform);
        }
        
        public void paintSideBySide(Graphics2D g, int i0, int nRows, int nSlices, int hSep) {
            AffineTransform savedTransform = g.getTransform();
            if (nRows <= 0) {
                return;
            }
            int i = i0;
            for (int s = 0; s < nSlices; s++) {
                if (i >= cells.length) {
                    break;
                }
                paint(g, i, 0, nRows, -1);
                g.translate(width + hSep, 0);
                i += nRows;
            }
            g.setTransform(savedTransform);
        }
                
        public int getNRowsThatFit(int i0, int height) {
            if (i0 >= rowHeights.length) {
                return -1;
            }
            int nRows = 0;
            int currentHeight = 0;
            for (int i = 0; i < nHeaderRows; i++) {
                int newHeight = currentHeight + rowHeights[i];
                if (newHeight > height) {
                    return 0;
                }
                currentHeight = newHeight;
            }
            for (int i = i0 + nHeaderRows; i < rowHeights.length; i++) {
                int newHeight = currentHeight + rowHeights[i];
                if (newHeight >= height) {
                    return nRows;
                }
                currentHeight = newHeight;
                ++nRows;
            }
            return nRows;
        }
    
        public void paint(Graphics2D g) {
            if (cells == null) {
                return;
            }
            paint(g, 0, 0, -1, -1);
        }
        
        public void setLocation(int x, int y, int hAlign, int vAlign) {
            this.x = x;
            this.y = y;
            this.hAlign = (byte)hAlign;
            this.vAlign = (byte)vAlign;
            dirty = true;
        }
        
        @Override
        public void setSize(int width, int height) {
            this.width = width;
            this.height = height;
            dirty = true;
        }
        
        public void setContents(Cell [] [] cells, int headerCount) {
            // Ensure the cells array is consistent with our assumptions.
            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < cells[0].length; j++) {
                    Cell cell = cells[i][j];
                    if (cell != null) {
                        byte correctColSpan = 1;
                        for (int jj = j + 1; jj < j + cell.colSpan && jj < cells[0].length; jj++) {
                            correctColSpan++;
                        }
                        cell.colSpan = correctColSpan;
                        byte correctRowSpan = 1;
                        for (int ii = i + 1; ii < i + cell.rowSpan && ii < cells.length; ii++) {
                            correctRowSpan++;
                        }
                        cell.rowSpan = correctRowSpan;
                        for (int ii = i; ii < i + cell.rowSpan; ii++) {
                            for (int jj = j; jj < j + cell.colSpan; jj++) {
                                if (ii != i || jj != j) {
                                    cells[ii][jj] = null;
                                }
                            }
                        }
                    }
                }
            }
            this.cells = cells;
            this.nHeaderRows = headerCount;
            dirty = true;
        }
    }

    /**
     * Print this printable as a blueprint.
     * 
     * @param graphics Swing graphics object
     * @param pageFormat page format for the printing
     * @param pageIndex 0-based index of page to print
     * @return NO_SUCH_PAGE if pageIndex doesn't exist, or PAGE_EXISTS if we've completed printing
     * @throws java.awt.print.PrinterException
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        final Graphics2D g = (Graphics2D)graphics;
        
        g.setFont(standardFont);
        g.setStroke(standardStroke);
        
        final int pageWidth = (int)pageFormat.getImageableWidth();
        final int pageHeight = (int)pageFormat.getImageableHeight();

        // Initializations
        if (blueprintExtent == null) {
            blueprintExtent = getBlueprintExtent(pageWidth, pageHeight);
        }
        memberTable.initialize(g); // Set dimensions of cells and table.
        // End initializations
        
        final int sliceSep = 36;
        final int maxNSlices = Math.max(1, (pageWidth - sliceSep) / (memberTable.width + sliceSep));
        
        g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        final int standardSheetHeight = printStandardSheet(g, pageFormat, pageIndex + 1);
        
        int nMembersRemaining = bridge.getMembers().size();
        int i0 = -1;
        int i1 = 0;
        int nRows = 0;
        int nSlices = 0;
        int yTable = 0;
        for (int i = 0; ; i++) {
            int availableHeight = 0;
            yTable = (i == 0) ? (blueprintExtent.y + 19) / 20 + sliceSep : sliceSep;
            availableHeight = standardSheetHeight - yTable;
            availableHeight -= 8; // separation from title block
            nRows = memberTable.getNRowsThatFit(i1, availableHeight);
            if (nRows == -1) {
                // The first row is not in the table.  Should never happen.
                return NO_SUCH_PAGE;        
            }
            // Don't orphan a few rows under the bridge sketch.
            if (i == 0 && nRows <= 4) {
                nRows = 0;
            }
            if (i > 0 && nRows == 0) {
                // Force at least one row to ensure we don't loop forever.
                nRows = 1;
            }
            if (nRows == 0) {
                nSlices = 1;
            }
            else {
                nSlices = Math.min((nMembersRemaining + nRows - 1) / nRows, maxNSlices);
                // Decrease the number of rows on the last page so columns are approximately same length.
                nRows -= Math.max(0, (nSlices * nRows - nMembersRemaining) / nSlices);
            }
            i0 = i1;
            i1 += nRows * nSlices;
            if (i == pageIndex) {
                break;
            }
            // Simulate printing.
            nMembersRemaining -= nSlices * nRows;
            
            // If we simulated printing everything, and we're still 
            // not at pageIndex, there's nothing to print for real.
            if (nMembersRemaining <= 0) {
                return NO_SUCH_PAGE;
            }
        }
        // Print the blueprint drawing on the first page.
        if (pageIndex == 0) {
            printBlueprint(g, pageWidth, standardSheetHeight);
        }
        AffineTransform savedTransform = g.getTransform();
        int margin = pageWidth - (nSlices * memberTable.width + (nSlices + 1) * sliceSep);
        g.translate(sliceSep + margin / 2, yTable);
        memberTable.paintSideBySide(g, i0, nRows, nSlices, sliceSep);
        g.setTransform(savedTransform);
        return PAGE_EXISTS;
    }
}
