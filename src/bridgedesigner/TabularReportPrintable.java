/*
 * TabularReportPrintable.java  
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Printable for tables in the Bridge Designer.  This is nearly an exact copy 
 * of the Swing table printable.  We needed to choose our own fonts and also fixed a 
 * potential problem: the Swing class assumes pages come in order, when the Printable spec
 * says they can come in any order, reverse for example.
 * 
 * @author Eugene K. Ressler
 */
public class TabularReportPrintable implements Printable {

    /** The table to print. */
    private JTable table;

    /** For quick reference to the table's header. */
    private JTableHeader header;

    /** For quick reference to the table's column model. */
    private TableColumnModel colModel;

    /** To save multiple calculations of total column width. */
    private int totalColWidth;

    /** The printing mode of this printable. */
    private JTable.PrintMode printMode;

    /** Provides the header text for the table. */
    private MessageFormat headerFormat;
    
    /** Lines of subheader text. */
    private String [] subheaderText;

    /** Provides the footer text for the table. */
    private MessageFormat footerFormat;

    /** The most recent page index asked to print. */
    private int last = -1;

    /** The next row to print. */
    private int row = 0;

    /** The next column to print. */
    private int col = 0;

    /** Used to store an area of the table to be printed. */
    private final Rectangle clip = new Rectangle(0, 0, 0, 0);

    /** Used to store an area of the table's header to be printed. */
    private final Rectangle hclip = new Rectangle(0, 0, 0, 0);
    
    /** Saves the creation of multiple rectangles. */
    private final Rectangle tempRect = new Rectangle(0, 0, 0, 0);

    /** Vertical space to leave between table and header/footer text. */
    private static final int H_F_SPACE = 8;

    /** Font size for the header text. */
    private static final float HEADER_FONT_SIZE = 10.0f;

    /** Font size for the sub-header text. */
    private static final float SUBHEADER_FONT_SIZE = 8.0f;

    /** Font size for the footer text. */
    private static final float FOOTER_FONT_SIZE = 8.0f;

    /** The font to use in rendering header text. */
    private Font headerFont;
    
    /** The font to use in rendering subheader text. */
    private Font subheaderFont;

    /** The font to use in rendering footer text. */
    private Font footerFont;

    /**
     * Create a new <code>TabularReportPrintable</code> for the given
     * <code>JTable</code>. Header and footer text can be specified using the
     * two <code>MessageFormat</code> parameters. When called upon to provide
     * a String, each format is given the current page number.
     *
     * @param  table         the table to print
     * @param  printMode     the printing mode for this printable
     * @param  headerFormat  a <code>MessageFormat</code> specifying the text to
     *                       be used in printing a header, or null for none
     * @param  footerFormat  a <code>MessageFormat</code> specifying the text to
     *                       be used in printing a footer, or null for none
     * @throws IllegalArgumentException if passed an invalid print mode
     */
    public TabularReportPrintable(JTable table,
                                  JTable.PrintMode printMode,
                                  MessageFormat headerFormat,
                                  String [] subheaderText,
                                  MessageFormat footerFormat) {

        this.table = table;

        header = table.getTableHeader();
        colModel = table.getColumnModel();
        totalColWidth = colModel.getTotalColumnWidth();
        
        if (header != null) {
            // the header clip height can be set once since it's unchanging
            hclip.height = header.getHeight();
        }

        this.printMode = printMode;

        this.headerFormat = headerFormat;
        this.subheaderText = subheaderText;
        this.footerFormat = footerFormat;

        // derive fonts from the table's font
        headerFont = table.getFont().deriveFont(Font.BOLD, HEADER_FONT_SIZE);
        subheaderFont = table.getFont().deriveFont(Font.PLAIN, SUBHEADER_FONT_SIZE);
        footerFont = table.getFont().deriveFont(Font.PLAIN, FOOTER_FONT_SIZE);
    }

    /**
     * Prints the specified page of the table into the given {@link Graphics}
     * context, in the specified format.
     *
     * @param   graphics    the context into which the page is drawn
     * @param   pageFormat  the size and orientation of the page being drawn
     * @param   pageIndex   the zero based index of the page to be drawn
     * @return  PAGE_EXISTS if the page is rendered successfully, or
     *          NO_SUCH_PAGE if a non-existent page index is specified
     * @throws  PrinterException if an error causes printing to be aborted
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                                                       throws PrinterException {

        // for easy access to these values
        final int imgWidth = (int)pageFormat.getImageableWidth();
        final int imgHeight = (int)pageFormat.getImageableHeight();

        if (imgWidth <= 0) {
            throw new PrinterException("Width of printable area is too small.");
        }

        // to pass the page number when formatting the header and footer text
        Object[] pageNumber = new Object[]{new Integer(pageIndex + 1)};

        // fetch the formatted header text, if any
        String headerText = null;
        if (headerFormat != null) {
            headerText = headerFormat.format(pageNumber);
        }

        FontMetrics subheaderFontMetrics = null;
        if (subheaderText != null) {
            subheaderFontMetrics = graphics.getFontMetrics(subheaderFont);
        }
                
        // fetch the formatted footer text, if any
        String footerText = null;
        if (footerFormat != null) {
            footerText = footerFormat.format(pageNumber);
        }

        // the amount of vertical space needed for the header and footer text
        Rectangle2D headerRect = null;
        int subheaderTextHeight = 0;
        Rectangle2D footerRect = null;

        // the amount of vertical space available for printing the table
        int availableSpace = imgHeight;

        // if there's header text, find out how much space is needed for it
        // and subtract that from the available space
        if (headerText != null) {
            headerRect = graphics.getFontMetrics(headerFont).getStringBounds(headerText, graphics);
            availableSpace -= round(headerRect.getHeight()) + H_F_SPACE;
        }

        // if there's subheader text, find out how much space is needed for it
        // and subtract that from the available space
        if (subheaderText != null) {
            subheaderTextHeight = subheaderFontMetrics.getHeight() * subheaderText.length;
            availableSpace -= subheaderTextHeight + H_F_SPACE;            
        }

        // if there's footer text, find out how much space is needed for it
        // and subtract that from the available space
        if (footerText != null) {
            footerRect = graphics.getFontMetrics(footerFont).getStringBounds(footerText, graphics);
            availableSpace -= round(footerRect.getHeight()) + H_F_SPACE;
        }
        
        if (availableSpace <= 0) {
            throw new PrinterException("Height of printable area is too small.");
        }

        // depending on the print mode, we may need a scale factor to
        // fit the table's entire width on the page
        double sf = 1.0D;
        if (printMode == JTable.PrintMode.FIT_WIDTH &&
                totalColWidth > imgWidth) {

            // if not, we would have thrown an acception previously
            assert imgWidth > 0;

            // it must be, according to the if-condition, since imgWidth > 0
            assert totalColWidth > 1;

            sf = (double)imgWidth / (double)totalColWidth;
        }

        // dictated by the previous two assertions
        assert sf > 0;

        // This takes care of cases where pageIndex restarts because
        // multiple collated copies have been requested.  Also for possible
        // undocumented case where pages are requested in reverse order
        // for some printers.
        if (pageIndex < last) {
            row = col = 0;
            clip.x = clip.y = clip.width = clip.height = 0;
            last = -1;
        }
        
        // This is in a loop for two reasons:
        // First, it allows us to catch up in case we're called starting
        // with a non-zero pageIndex. Second, we know that we can be called
        // for the same page multiple times. The condition of this while
        // loop acts as a check, ensuring that we don't attempt to do the
        // calculations again when we are called subsequent times for the
        // same page.
        while (last < pageIndex) {
            // if we are finished all columns in all rows
            if (row >= table.getRowCount() && col == 0) {
                return NO_SUCH_PAGE;
            }

            // rather than multiplying every row and column by the scale factor
            // in findNextClip, just pass a width and height that have already
            // been divided by it
            int scaledWidth = (int)(imgWidth / sf);
            int scaledHeight = (int)((availableSpace - hclip.height) / sf);

            // calculate the area of the table to be printed for this page
            findNextClip(scaledWidth, scaledHeight);

            last++;
        }

        // translate into the co-ordinate system of the pageFormat
        Graphics2D g2d = (Graphics2D)graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        
        // if there's footer text, print it at the bottom of the imageable area
        if (footerText != null) {
            AffineTransform oldTrans = g2d.getTransform();
            g2d.translate(0, imgHeight - ceil(footerRect.getHeight()));
            printText(g2d, footerText, round(footerRect.getWidth()), ceil(-footerRect.getY()), footerFont, imgWidth);
            g2d.setTransform(oldTrans);
        }

        // if there's header text, print it at the top of the imageable area
        // and then translate downwards
        if (headerText != null) {
            printText(g2d, headerText, round(headerRect.getWidth()), ceil(-headerRect.getY()), headerFont, imgWidth);
            g2d.translate(0, round(headerRect.getHeight()) + H_F_SPACE);
        }

        // if there's subheader text, print it at at the top, below the header
        // if there is one
        int yBaseline = 0;
        if (subheaderText != null) {
            for (int i = 0; i < subheaderText.length; ++i) {
                yBaseline += subheaderFontMetrics.getAscent();
                Rectangle2D r = subheaderFontMetrics.getStringBounds(subheaderText[i], graphics);
                printText(g2d, subheaderText[i], round(r.getWidth()), yBaseline, subheaderFont, imgWidth);
                yBaseline += subheaderFontMetrics.getDescent() + subheaderFontMetrics.getLeading();
            }
            g2d.translate(0, subheaderTextHeight + H_F_SPACE);
        }

        // constrain the table output to the available space
        tempRect.x = 0;
        tempRect.y = 0;
        tempRect.width = imgWidth;
        tempRect.height = availableSpace;
        g2d.clip(tempRect);

        // if we have a scale factor, scale the graphics object to fit
        // the entire width
        if (sf != 1.0D) {
            g2d.scale(sf, sf);

        // otherwise, ensure that the current portion of the table is
        // centered horizontally
        } else {
            int diff = (imgWidth - clip.width) / 2;
            g2d.translate(diff, 0);
        }

        // store the old transform and clip for later restoration
        AffineTransform oldTrans = g2d.getTransform();
        Shape oldClip = g2d.getClip();

        // if there's a table header, print the current section and
        // then translate downwards
        if (header != null) {
            hclip.x = clip.x;
            hclip.width = clip.width;

            g2d.translate(-hclip.x, 0);
            g2d.clip(hclip);
            header.print(g2d);

            // restore the original transform and clip
            g2d.setTransform(oldTrans);
            g2d.setClip(oldClip);

            // translate downwards
            g2d.translate(0, hclip.height);
        }

        // print the current section of the table
        g2d.translate(-clip.x, -clip.y);
        g2d.clip(clip);
        table.print(g2d);
        
        // restore the original transform and clip
        g2d.setTransform(oldTrans);
        g2d.setClip(oldClip);
        
        // draw a box around the table
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, clip.width, hclip.height + clip.height);

        return PAGE_EXISTS;
    }

    /**
     * A helper method that encapsulates common code for rendering the
     * header and footer text.
     *
     * @param  g2d       the graphics to draw into
     * @param  text      the text to draw, non null
     * @param  rect      the bounding rectangle for this text,
     *                   as calculated at the given font, non null
     * @param  font      the font to draw the text in, non null
     * @param  imgWidth  the width of the area to draw into
     */
    private void printText(Graphics2D g2d,
                           String text,
                           int width,
                           int yBaseline,
                           Font font,
                           int imgWidth) {

        int tx;

        // if the text is small enough to fit, center it
        if (width < imgWidth) {
            tx = (imgWidth - width) / 2;
        // otherwise, if the table is LTR, ensure the left side of
        // the text shows; the right can be clipped
        } else if (table.getComponentOrientation().isLeftToRight()) {
            tx = 0;
        // otherwise, ensure the right side of the text shows
        } else {
            tx = imgWidth - width;
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(font);
        g2d.drawString(text, tx, yBaseline);
    }

    /**
     * Calculate the area of the table to be printed for
     * the next page. This should only be called if there
     * are rows and columns left to print.
     *
     * To avoid an infinite loop in printing, this will
     * always put at least one cell on each page.
     *
     * @param  pw  the width of the area to print in
     * @param  ph  the height of the area to print in
     */
    private void findNextClip(int pw, int ph) {
        final boolean ltr = table.getComponentOrientation().isLeftToRight();

        // if we're ready to start a new set of rows
        if (col == 0) {
            if (ltr) {
                // adjust clip to the left of the first column
                clip.x = 0;
            } else {
                // adjust clip to the right of the first column
                clip.x = totalColWidth;
            }
            // adjust clip to the top of the next set of rows
            clip.y += clip.height;
            
            // adjust clip width and height to be zero
            clip.width = 0;
            clip.height = 0;

            // fit as many rows as possible, and at least one
            int rowCount = table.getRowCount();
            int rowHeight = table.getRowHeight(row);
            do {
                clip.height += rowHeight;
                if (++row >= rowCount) {
                    break;
                }
                rowHeight = table.getRowHeight(row);
            } while (clip.height + rowHeight <= ph);
        }

        // we can short-circuit for JTable.PrintMode.FIT_WIDTH since
        // we'll always fit all columns on the page
        if (printMode == JTable.PrintMode.FIT_WIDTH) {
            clip.x = 0;
            clip.width = totalColWidth;
            return;
        }

        if (ltr) {
            // adjust clip to the left of the next set of columns
            clip.x += clip.width;
        }

        // adjust clip width to be zero
        clip.width = 0;

        // fit as many columns as possible, and at least one
        int colCount = table.getColumnCount();
        int colWidth = colModel.getColumn(col).getWidth();
        do {
            clip.width += colWidth;
            if (!ltr) {
                clip.x -= colWidth;
            }

            if (++col >= colCount) {
                // reset col to 0 to indicate we're finished all columns
                col = 0;
                break;
            }

            colWidth = colModel.getColumn(col).getWidth();
        } while (clip.width + colWidth <= pw);

    }

    static private int round(double x) {
        return (int)(x + 0.5);
    }

    static private int ceil(double x) {
        return (int)Math.ceil(x);
    }
}
