
/*
 * SetupWizard.java  
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

import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import org.jdesktop.application.ResourceMap;

/**
 * Setup wizard for setup wizard dialog.  A complex dialog.
 * 
 * @author  Eugene K. Ressler
 */
public class SetupWizard extends JDialog {

    private final Page[] pages = new Page[8];
    private int currentPage;
    private DesignConditions conditions = DesignConditions.conditions[0];
    private final BridgeCartoonView bridgeCartoonView = new BridgeCartoonView();
    private int updateDepth = 0;
    private int pagesLoadedBits = 0;
    private boolean ok = false;
    private Icon deckCartoonIcon;
    private final Icon noDeckNoLoad;
    private final Icon medDeckStdLoad;
    private final Icon hghDeckStdLoad;
    private final Icon medDeckPmtLoad;
    private final Icon hghDeckPmtLoad;
    
    /**
     * Create a new setup wizard.
     * 
     * @param parent parent frame of the wizard
     */
    public SetupWizard(Frame parent) {
        super(parent, true);

        // Force the cartoon view to initialize its window and viewport so the dimensioned view is correct.
        bridgeCartoonView.initialize(DesignConditions.conditions[0]);        

        initComponents();
        localContest4charOKMsgLabel.setVisible(false);
        getRootPane().setDefaultButton(finish);
        Help.getBroker().enableHelpOnButton(help, "hlp_design_specifications", Help.getSet());        
        
        // Get icons for the deck/load cartoon
        BDApp app = BDApp.getApplication();
        noDeckNoLoad   = app.getIconResource("nodecknoload.png");
        medDeckStdLoad = app.getIconResource("meddeckstdload.png");
        hghDeckStdLoad = app.getIconResource("hghdeckstdload.png");
        medDeckPmtLoad = app.getIconResource("meddeckpmtload.png");
        hghDeckPmtLoad = app.getIconResource("hghdeckpmtload.png");
        
        pages[1] = new Page1();
        pages[2] = new Page2();
        pages[3] = new Page3();
        pages[4] = new Page4();
        pages[5] = new Page5();
        pages[6] = new Page6();
        pages[7] = new Page7();
        
        pages[1].load();
        showDetailPane(false);
    }

    /**
     * Return the template sketch selected in the wizard, if any.
     * 
     * @return template sketch
     */
    public BridgeSketchModel getSketchModel() {
       return bridgeCartoonView.getBridgeSketchView().getModel(); 
    }
    
    private class LocalContestCodeFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            string = string.toUpperCase(Locale.ENGLISH);
            Document doc = fb.getDocument();
            // Construct the string that the insertion would produce in the code text box.
            String s = doc.getText(0, offset) + string + doc.getText(offset, doc.getLength() - offset);
            // Scan for errors.
            int lccScan = localContestCodeScan(s);
            // Insert only if the insertion would not cause an error.
            if (lccScan != LCC_ERROR) {
                super.insertString(fb, offset, string, attr);
            }
            // Finish taking care of the scan result.
            handleScan(lccScan, s);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            Document doc = fb.getDocument();
            // Construct the string that the removal would produce in the code text box.
            String s = doc.getText(0, offset) + doc.getText(offset + length, doc.getLength() - offset - length);
            // Scan for errors.
            int lccScan = localContestCodeScan(s);
            // Remove only if the insertion would not cause an error.
            if (lccScan != LCC_ERROR) {
                super.remove(fb, offset, length);
            }
            // Finish taking care of the scan result.
            handleScan(lccScan, s);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            text = text.toUpperCase(Locale.ENGLISH);
            Document doc = fb.getDocument();
            // Construct the string that the replacement would produce in the code text box.
            String s = doc.getText(0, offset) + text + doc.getText(offset, doc.getLength() - offset);
            // Scan for errors.
            int lccScan = localContestCodeScan(s);
            // Replace only if the insertion would not cause an error.
            if (lccScan != LCC_ERROR) {
                super.replace(fb, offset, length, text, attrs);
            }
            // Finish taking care of the scan result.
            handleScan(lccScan, s);
        }
        private ResourceMap resourceMap = BDApp.getResourceMap(SetupWizard.class);

        /**
         * Deal with the results of a local contest code scan. This includes
         * updating the message label and setting the enabled state of the
         * "Next" button.
         *
         * @param lccScan code returned by localContestCodeScan
         * @param s the string that was scanned
         */
        private void handleScan(int lccScan, String s) {
            switch (lccScan) {
                case LCC_ERROR:
                    Toolkit.getDefaultToolkit().beep();
                    localContestMsgLabel.setText(resourceMap.getString("localContestCodeError.text"));
                    next.setEnabled(false);
                    break;
                case LCC_PREFIX:
                    localContestMsgLabel.setText("");
                    next.setEnabled(false);
                    break;
                case LCC_COMPLETE:
                    initialize(DesignConditions.getDesignConditions(s.substring(3)));
                    localContestMsgLabel.setText(resourceMap.getString("localContestCodeComplete.text"));
                    next.setEnabled(true);
                    setSiteCostEnable(true);
                    break;
            }
        }
    }

    /**
     * Return the design conditions selected in the dialog.
     * 
     * @return design conditions
     */
    public DesignConditions getDesignConditions() {
        return conditions;
    }
    
    /**
     * Return true iff the Ok button was used to close the wizard last time it was visible.
     * 
     * @return true iff the Ok button was used to close the wizard
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * Return the project ID text typed in the wizard by the user.
     * 
     * @return project ID text
     */
    public String getProjectId() {
        return projectIdPrefixLabel.getText() + projectIdEdit.getText();
    }

    /**
     * Return the designer's name typed in the wizard by the user.
     * 
     * @return designer's name
     */
    public String getDesignedBy() {
        return designedByEdit.getText();
    }

    /**
     * Set the visibility of the setup wizard.
     * 
     * @param b whether the wizard should be made visible or invisible
     */
    @Override
    public void setVisible(boolean b) {
        if (b) {
            ok = false;
            pages[1].load();
            //update();
        }
        super.setVisible(b);
    }

    private static final int LCC_ERROR = -1;
    private static final int LCC_PREFIX = 1;
    private static final int LCC_COMPLETE = 0;
    private final Pattern localContestPattern = Pattern.compile("\\A[0-9A-Z]{3}\\d\\d[A-D]\\Z");

    private int localContestCodeScan(String s) {
        if (localContestPattern.matcher(s).matches()) {
            return DesignConditions.getDesignConditions(s.substring(3)) == null ? LCC_ERROR : LCC_COMPLETE;
        }
        if (localContestPattern.matcher(s + "00001A".substring(s.length())).matches()) {
            return s.length() < 3 || DesignConditions.isTagPrefix(s.substring(3)) ? LCC_PREFIX : LCC_ERROR;
        }
        return LCC_ERROR;
    }

    /**
     * Initialize the dialog by asking the scenario to fill the SetupDesc with current information and
     * then using it to pre-set all the dialog controls to suitable positions and values.
     * 
     * @param conditions initial design conditions to select
     * @param projectId project id to preload or null if none
     * @param designedBy designer's name to preload or null if none
     */
    public void initialize(DesignConditions conditions, String projectId, String designedBy, BridgeSketchModel model) {
        if (projectId == null) {
            projectIdPrefixLabel.setText("000" + conditions.getTag() + "-");
            projectIdEdit.setText("");
        } else {
            String[] idParts = projectId.split("-");
            if (idParts.length > 0) {
                projectIdPrefixLabel.setText(idParts[0]);
                if (idParts.length > 1) {
                    projectIdEdit.setText(idParts[1]);
                }
            }
        }
        designedByEdit.setText(designedBy == null ? "" : designedBy);
        bridgeCartoonView.getBridgeSketchView().setModel(model);
        initialize(conditions);
    }

    public void initialize(DesignConditions conditions) {
        // If the conditions didn't come from the table, set to default instead.
        if (conditions == null || DesignConditions.getDesignConditions(conditions.getCodeLong()) == null) {
            conditions = DesignConditions.conditions[0];
        }
        // Set the model.
        this.conditions = conditions;
        deckElevationBox.setSelectedIndex((24 - (int) conditions.getDeckElevation()) / 4);
        if (conditions.isArch()) {
            archAbutmentsButton.setSelected(true);
            ((ExtendedComboBox)archHeightBox).setRawSelectedIndex((24 - (int) conditions.getArchHeight()) / 4);
        } else {
            standardAbutmentsButton.setSelected(true);
        }
        if (conditions.isPier()) {
            pierButton.setSelected(true);
            ((ExtendedComboBox)pierHeightBox).setRawSelectedIndex((24 - (int) conditions.getPierHeight()) / 4);
        } else {
            noPierButton.setSelected(true);
        }
        switch (conditions.getNAnchorages()) {
            default:
            case 0:
                noAnchorageButton.setSelected(true);
                break;
            case 1:
                oneAnchorageButton.setSelected(true);
                break;
            case 2:
                twoAnchoragesButton.setSelected(true);
                break;
        }
        if (conditions.getDeckType() == DesignConditions.HI_STRENGTH_DECK) {
            highConcreteButton.setSelected(true);
        } else {
            mediumConcreteButton.setSelected(true);
        }
        if (conditions.getLoadType() == DesignConditions.HEAVY_TRUCK) {
            permitLoadButton.setSelected(true);
        } else {
            standardTruckButton.setSelected(true);
        }
        // Set the detail pane values.
        updateDependencies();
    }

    private void beginUpdate() {
        updateDepth++;
    }

    private void endUpdate() {
        --updateDepth;
        update();
    }

    private void update() {
        if (updateDepth == 0) {
            conditions = DesignConditions.getDesignConditions(
                    24 - 4 * deckElevationBox.getSelectedIndex(),
                    archAbutmentsButton.isSelected() ? 24 - 4 * ((ExtendedComboBox) archHeightBox).getRawSelectedIndex() : -1,
                    pierButton.isSelected() ? 24 - 4 * ((ExtendedComboBox) pierHeightBox).getRawSelectedIndex() : -1,
                    noAnchorageButton.isSelected() ? 0 : oneAnchorageButton.isSelected() ? 1 : twoAnchoragesButton.isSelected() ? 2 : 0,
                    permitLoadButton.isSelected() ? DesignConditions.HEAVY_TRUCK : DesignConditions.STANDARD_TRUCK,
                    highConcreteButton.isSelected() ? DesignConditions.HI_STRENGTH_DECK : DesignConditions.MEDIUM_STRENGTH_DECK);
            updateDependencies();
        }
    }
    
    // Cost calculations always in US dollars.
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);        

    private void updateDependencies() {
        String val = projectIdPrefixLabel.getText();
        if (val.length() != 7) {
            val = "00001A-";
        }
        projectIdPrefixLabel.setText(val.substring(0, 3) + conditions.getTag() + val.substring(6));
        // Put new values in the detail table, whether it's visible or not.
        ((SiteCostTableModel) siteCostDetailTable.getModel()).initialize(conditions);
        
        siteCostLabel.setText(currencyFormat.format(conditions.getTotalFixedCost()));
        
        // Show site condition label in cost box.
        if (isSiteCostEnabled()) {
            ResourceMap resourceMap = BDApp.getResourceMap(SetupWizard.class);
            siteConditionsLabel.setText(resourceMap.getString("siteCondition.text", conditions.getTag()));        
        }
        // Set the deck cartoon.
        switch (conditions.getDeckType()) {
            case DesignConditions.MEDIUM_STRENGTH_DECK:
                switch (conditions.getLoadType()) {
                    case DesignConditions.STANDARD_TRUCK:
                        deckCartoonIcon = medDeckStdLoad;
                        break;
                    case DesignConditions.HEAVY_TRUCK:
                        deckCartoonIcon = medDeckPmtLoad;
                        break;
                }
                break;
            case DesignConditions.HI_STRENGTH_DECK:
                switch (conditions.getLoadType()) {
                    case DesignConditions.STANDARD_TRUCK:
                        deckCartoonIcon = hghDeckStdLoad;
                        break;
                    case DesignConditions.HEAVY_TRUCK:
                        deckCartoonIcon = hghDeckPmtLoad;
                        break;
                }
                break;
        }
        bridgeCartoonView.initialize(conditions);
        pages[currentPage].showDeckCartoon();
        pages[currentPage].showElevationCartoon();
    }

    private boolean isSiteCostEnabled() {
        return dropRaiseButton.isEnabled();
    }
    
    private void setSiteCostEnable(boolean val) {
        dropRaiseButton.setEnabled(val);
        costNoteLabel.setEnabled(val);
        siteCostLabel.setEnabled(val);
        if (val) {
            update();
        } else {
            showDetailPane(false);
            siteCostLabel.setText("$0.0");
            siteConditionsLabel.setText("");
        }
    }

    private void showDetailPane(boolean visible) {
        if (visible != siteCostDetailTable.isVisible()) {
            siteCostDetailTable.setVisible(visible);
            dropRaiseButton.setIcon(BDApp.getApplication().getIconResource(visible ? "undrop.png" : "drop.png"));
            pack();
        }
    }

    private boolean hasPageBeenLoaded(int n) {
        return (pagesLoadedBits & (1 << n)) != 0;
    }
    
    abstract private class Page {

        public void setDefaults() {
            // Remember we're on this page.
            currentPage = getPageNumber();
            pagesLoadedBits |= (1 << currentPage);

            ResourceMap resourceMap = BDApp.getResourceMap(SetupWizard.class);
            String pageNoString = Integer.toString(currentPage);

            // Set up header: page number and title
            pageNumber.setText(pageNoString);
            pageTitle.setText(resourceMap.getString("title" + pageNoString + ".text"));

            // Show the correct widget card in the left column.
            CardLayout cl = (CardLayout) widgetPanel.getLayout();
            cl.show(widgetPanel, pageNoString);

            // Show the correct cartoons.
            showDeckCartoon();
            showElevationCartoon();
            updateLegend();

            // Set elements enabled.
            boolean[] elementsEnabled = getElementsEnabled();
            back.setEnabled(elementsEnabled[0]);
            next.setEnabled(elementsEnabled[1]);
            finish.setEnabled(elementsEnabled[2]);

            // Load the tip box.
            loadTip(resourceMap, pageNoString);
        }

        public void updateLegend() {
            // Set legend visibility.
            boolean[] legendsVisible = getLegendsVisible();
            legendRiverBankLabel.setVisible(legendsVisible[0]);
            legendExcavationLabel.setVisible(legendsVisible[1]);
            legendRiverLabel.setVisible(legendsVisible[2]);
            legendDeckLabel.setVisible(legendsVisible[3]);
            legendAbutmentLabel.setVisible(legendsVisible[4]);
            legendPierLabel.setVisible(legendsVisible[5] && pierButton.isSelected());
        }

        public void load() {
            setDefaults();
            setSiteCostEnable(true);
        }

        abstract int getPageNumber();

        protected int getNextPageNumber() {
            return getPageNumber() + 1;
        }

        protected int getBackPageNumber() {
            return getPageNumber() - 1;
        }
        
        protected void showDeckCartoon() {
            deckCartoonLabel.setIcon(
                    (hasPageBeenLoaded(4) || localContestCodeScan(localContestCodeField.getText()) == LCC_COMPLETE) ? 
                        deckCartoonIcon : noDeckNoLoad);
            deckCartoonLabel.repaint();
        }

        protected void showElevationCartoon() {
            if (hasPageBeenLoaded(3) || localContestCodeScan(localContestCodeField.getText()) == LCC_COMPLETE) {
                bridgeCartoonView.setMode(BridgeCartoonView.MODE_STANDARD_ITEMS);
            }
            else {
                bridgeCartoonView.setMode(BridgeCartoonView.MODE_TERRAIN_ONLY);                
            }
            elevationViewLabel.repaint();
        }

        protected boolean[] getLegendsVisible() {
            return new boolean[]{true, true, true, true, true, true};
        }

        protected boolean[] getElementsEnabled() {
            return new boolean[]{true, true, true};
        }

        protected void loadTip(ResourceMap resourceMap, String pageNoString) {
            tipPane.setText(resourceMap.getString("tipPane" + pageNoString + ".text"));
        }
    }
    
    private class Page1 extends Page {

        @Override
        int getPageNumber() {
            return 1;
        }

        @Override
        protected void showElevationCartoon() {
            bridgeCartoonView.setMode(BridgeCartoonView.MODE_MEASUREMENTS);
            deckCartoonLabel.repaint();
        }

        @Override
        protected void showDeckCartoon() {
            deckCartoonLabel.setIcon(noDeckNoLoad);
        }
        
        @Override
        protected boolean[] getLegendsVisible() {
            return new boolean[]{true, false, true, false, false, false};
        }

        @Override
        protected boolean[] getElementsEnabled() {
            return new boolean[]{false, true, false};
        }

        @Override
        protected int getBackPageNumber() {
            return 1;
        }

        @Override
        public void load() {
            setDefaults();
            setSiteCostEnable(false);
        }
    }

    private class Page2 extends Page {

        @Override
        int getPageNumber() {
            return 2;
        }

        @Override
        protected boolean[] getElementsEnabled() {
            return new boolean[]{true, true, false};
        }

        @Override
        protected int getNextPageNumber() {
            return (localContestCodeScan(localContestCodeField.getText()) == LCC_COMPLETE) ? 5 : 3;
        }

        @Override
        public void load() {
            setDefaults();
            setSiteCostEnable(hasPageBeenLoaded(3));
        }
    }

    private class Page3 extends Page {

        @Override
        int getPageNumber() {
            return 3;
        }

        @Override
        protected boolean[] getElementsEnabled() {
            return new boolean[]{true, true, false};
        }
    }

    private class Page4 extends Page {

        @Override
        int getPageNumber() {
            return 4;
        }

    }

    private class Page5 extends Page {

        @Override
        int getPageNumber() {
            return 5;
        }

        @Override
        protected int getBackPageNumber() {
            return (localContestCodeScan(localContestCodeField.getText()) == LCC_COMPLETE) ? 2 : 4;
        }
        
        @Override
        public void load() {
            super.load();
            final BridgeSketchModel model = bridgeCartoonView.getBridgeSketchView().getModel();
            // This calls handler with new (null) selection, setting model null, so keep above line first!
            templateList.setListData(BridgeSketchModel.getList(conditions));
            if (model == null) {
                templateList.setSelectedIndex(0);
            }
            else {
                templateList.setSelectedValue(model, true);
            }
        }
    }

    private class Page6 extends Page {

        @Override
        int getPageNumber() {
            return 6;
        }
        
        @Override
        protected void showElevationCartoon() {
            bridgeCartoonView.setMode(BridgeCartoonView.MODE_STANDARD_ITEMS | BridgeCartoonView.MODE_TITLE_BLOCK);
            deckCartoonLabel.repaint();
        }
    }

    private class Page7 extends Page {

        @Override
        int getPageNumber() {
            return 7;
        }

        @Override
        protected boolean[] getElementsEnabled() {
            return new boolean[]{true, false, true};
        }

        @Override
        protected int getNextPageNumber() {
            return 7;
        }
        
        @Override
        protected void showElevationCartoon() {
            bridgeCartoonView.setMode(BridgeCartoonView.MODE_STANDARD_ITEMS | BridgeCartoonView.MODE_JOINTS | BridgeCartoonView.MODE_TITLE_BLOCK);
            deckCartoonLabel.repaint();
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        localContestGroup = new javax.swing.ButtonGroup();
        abutmentGroup = new javax.swing.ButtonGroup();
        pierGroup = new javax.swing.ButtonGroup();
        anchorageGroup = new javax.swing.ButtonGroup();
        deckMaterialGroup = new javax.swing.ButtonGroup();
        loadingGroup = new javax.swing.ButtonGroup();
        pageNumber = new javax.swing.JLabel();
        pageTitle = new javax.swing.JLabel();
        widgetPanel = new javax.swing.JPanel();
        card1 = new javax.swing.JPanel();
        requirementPanel = new javax.swing.JPanel();
        requirementPane = new TipTextPane();
        card2 = new javax.swing.JPanel();
        localContestCodePanel = new javax.swing.JPanel();
        localContestLabel = new javax.swing.JLabel();
        localContestNoButton = new javax.swing.JRadioButton();
        localContest4YesButton = new javax.swing.JRadioButton();
        localContest6YesButton = new javax.swing.JRadioButton();
        localContestCodeLabel = new javax.swing.JTextPane();
        localContestCodeField = new javax.swing.JTextField();
        localContestMsgLabel = new javax.swing.JLabel();
        localContest4charOKMsgLabel = new javax.swing.JLabel();
        card3 = new javax.swing.JPanel();
        deckElevationPanel = new javax.swing.JPanel();
        deckElevationButtonPanel = new javax.swing.JPanel();
        deckElevationUpButton = new javax.swing.JButton();
        deckElevationDownButton = new javax.swing.JButton();
        deckElevationBox = new ExtendedComboBox(deckElevationUpButton, deckElevationDownButton);
        supportConfigPanel = new javax.swing.JPanel();
        abutmentPanel = new javax.swing.JPanel();
        standardAbutmentsButton = new javax.swing.JRadioButton();
        archAbutmentsButton = new javax.swing.JRadioButton();
        archHeightLabel = new javax.swing.JLabel();
        archHeightButtonPanel = new javax.swing.JPanel();
        archHeightUpButton = new javax.swing.JButton();
        archHeightDownButton = new javax.swing.JButton();
        archHeightBox = new ExtendedComboBox(archHeightUpButton, archHeightDownButton);
        pierPanel = new javax.swing.JPanel();
        noPierButton = new javax.swing.JRadioButton();
        pierButton = new javax.swing.JRadioButton();
        pierHeightLabel = new javax.swing.JLabel();
        pierHeightButtonPanel = new javax.swing.JPanel();
        pierHeightUpButton = new javax.swing.JButton();
        pierHeightDownButton = new javax.swing.JButton();
        pierHeightBox = new ExtendedComboBox(pierHeightUpButton, pierHeightDownButton);
        anchoragePanel = new javax.swing.JPanel();
        noAnchorageButton = new javax.swing.JRadioButton();
        oneAnchorageButton = new javax.swing.JRadioButton();
        twoAnchoragesButton = new javax.swing.JRadioButton();
        card4 = new javax.swing.JPanel();
        deckMaterialPanel = new javax.swing.JPanel();
        mediumConcreteButton = new javax.swing.JRadioButton();
        highConcreteButton = new javax.swing.JRadioButton();
        loadingPanel = new javax.swing.JPanel();
        standardTruckButton = new javax.swing.JRadioButton();
        permitLoadButton = new javax.swing.JRadioButton();
        card5 = new javax.swing.JPanel();
        selectTemplatePanel = new javax.swing.JPanel();
        templateList = new javax.swing.JList();
        card6 = new javax.swing.JPanel();
        titleBlockInfoPanel = new javax.swing.JPanel();
        projectNameLabel = new javax.swing.JLabel();
        projectNameEdit = new javax.swing.JTextField();
        designedByLabel = new javax.swing.JLabel();
        designedByEdit = new javax.swing.JTextField();
        projectIdLabel = new javax.swing.JLabel();
        projectIdEdit = new javax.swing.JTextField();
        projectIdPrefixLabel = new javax.swing.JLabel();
        card7 = new javax.swing.JPanel();
        designPanel = new javax.swing.JPanel();
        instructionsPane = new TipTextPane();
        deckCrossSectionPanel = new javax.swing.JPanel();
        deckCartoonLabel = new javax.swing.JLabel();
        elevationViewPanel = new javax.swing.JPanel();
        elevationViewLabel = bridgeCartoonView.getDrawing(1);
        legendPanel = new javax.swing.JPanel();
        legendRiverBankLabel = new javax.swing.JLabel();
        legendExcavationLabel = new javax.swing.JLabel();
        legendRiverLabel = new javax.swing.JLabel();
        legendTopSpacerLabel = new javax.swing.JLabel();
        legendDeckLabel = new javax.swing.JLabel();
        legendAbutmentLabel = new javax.swing.JLabel();
        legendPierLabel = new javax.swing.JLabel();
        legendBottomSpacerLabel = new javax.swing.JLabel();
        tipPanel = new javax.swing.JPanel();
        tipPane = new TipTextPane();
        siteCostPanel = new javax.swing.JPanel();
        siteCostDetailTable = new SiteCostTable();
        siteCostLabel = new javax.swing.JLabel();
        dropRaiseButton = new javax.swing.JButton();
        costNoteLabel = new javax.swing.JLabel();
        siteConditionsLabel = new javax.swing.JLabel();
        help = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        back = new javax.swing.JButton();
        next = new javax.swing.JButton();
        finish = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(SetupWizard.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        pageNumber.setBackground(resourceMap.getColor("pageNumber.background")); // NOI18N
        pageNumber.setFont(resourceMap.getFont("pageNumber.font")); // NOI18N
        pageNumber.setForeground(resourceMap.getColor("pageNumber.foreground")); // NOI18N
        pageNumber.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pageNumber.setText(resourceMap.getString("pageNumber.text")); // NOI18N
        pageNumber.setMaximumSize(new java.awt.Dimension(35, 35));
        pageNumber.setMinimumSize(new java.awt.Dimension(35, 35));
        pageNumber.setName("pageNumber"); // NOI18N
        pageNumber.setOpaque(true);
        pageNumber.setPreferredSize(new java.awt.Dimension(35, 35));
        pageNumber.setRequestFocusEnabled(false);

        pageTitle.setFont(resourceMap.getFont("pageTitle.font")); // NOI18N
        pageTitle.setForeground(resourceMap.getColor("pageTitle.foreground")); // NOI18N
        pageTitle.setText(resourceMap.getString("pageTitle.text")); // NOI18N
        pageTitle.setName("pageTitle"); // NOI18N

        widgetPanel.setName("widgetPanel"); // NOI18N
        widgetPanel.setLayout(new java.awt.CardLayout());

        card1.setName("card1"); // NOI18N

        requirementPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Design Requirement:", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14), new java.awt.Color(0, 0, 128))); // NOI18N
        requirementPanel.setName("requirementPanel"); // NOI18N

        requirementPane.setText(resourceMap.getString("requirementPane.text")); // NOI18N
        requirementPane.setName("requirementPane"); // NOI18N

        javax.swing.GroupLayout requirementPanelLayout = new javax.swing.GroupLayout(requirementPanel);
        requirementPanel.setLayout(requirementPanelLayout);
        requirementPanelLayout.setHorizontalGroup(
            requirementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, requirementPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(requirementPane, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                .addContainerGap())
        );
        requirementPanelLayout.setVerticalGroup(
            requirementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(requirementPanelLayout.createSequentialGroup()
                .addComponent(requirementPane)
                .addContainerGap())
        );

        javax.swing.GroupLayout card1Layout = new javax.swing.GroupLayout(card1);
        card1.setLayout(card1Layout);
        card1Layout.setHorizontalGroup(
            card1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(requirementPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card1Layout.setVerticalGroup(
            card1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(requirementPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        widgetPanel.add(card1, "1");

        card2.setName("card2"); // NOI18N

        localContestCodePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Local Contest Code", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14), new java.awt.Color(0, 0, 128))); // NOI18N
        localContestCodePanel.setName("localContestCodePanel"); // NOI18N

        localContestLabel.setText(resourceMap.getString("localContestLabel.text")); // NOI18N
        localContestLabel.setName("localContestLabel"); // NOI18N

        localContestGroup.add(localContestNoButton);
        localContestNoButton.setSelected(true);
        localContestNoButton.setText(resourceMap.getString("localContestNoButton.text")); // NOI18N
        localContestNoButton.setName("localContestNoButton"); // NOI18N

        localContestGroup.add(localContest4YesButton);
        localContest4YesButton.setText(resourceMap.getString("localContest4YesButton.text")); // NOI18N
        localContest4YesButton.setName("localContest4YesButton"); // NOI18N
        localContest4YesButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                localContest4YesButtonItemStateChanged(evt);
            }
        });

        localContestGroup.add(localContest6YesButton);
        localContest6YesButton.setText(resourceMap.getString("localContest6YesButton.text")); // NOI18N
        localContest6YesButton.setName("localContest6YesButton"); // NOI18N
        localContest6YesButton.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localContest6YesButtonStateChanged(evt);
            }
        });

        localContestCodeLabel.setEditable(false);
        localContestCodeLabel.setText(resourceMap.getString("localContestCodeLabel.text")); // NOI18N
        localContestCodeLabel.setEnabled(false);
        localContestCodeLabel.setFocusable(false);
        localContestCodeLabel.setName("localContestCodeLabel"); // NOI18N
        localContestCodeLabel.setOpaque(false);

        ((AbstractDocument)localContestCodeField.getDocument()).setDocumentFilter(new LocalContestCodeFilter());
        localContestCodeField.setEnabled(false);
        localContestCodeField.setName("localContestCodeField"); // NOI18N

        localContestMsgLabel.setFont(resourceMap.getFont("localContestMsgLabel.font")); // NOI18N
        localContestMsgLabel.setForeground(resourceMap.getColor("localContestMsgLabel.foreground")); // NOI18N
        localContestMsgLabel.setText(resourceMap.getString("localContestMsgLabel.text")); // NOI18N
        localContestMsgLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        localContestMsgLabel.setName("localContestMsgLabel"); // NOI18N

        localContest4charOKMsgLabel.setText(resourceMap.getString("localContest4charOKMsgLabel.text")); // NOI18N
        localContest4charOKMsgLabel.setName("localContest4charOKMsgLabel"); // NOI18N

        javax.swing.GroupLayout localContestCodePanelLayout = new javax.swing.GroupLayout(localContestCodePanel);
        localContestCodePanel.setLayout(localContestCodePanelLayout);
        localContestCodePanelLayout.setHorizontalGroup(
            localContestCodePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(localContestCodePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(localContestCodePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(localContest6YesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localContest4YesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localContestLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localContestNoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(localContestCodePanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(localContestCodePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(localContestCodeField)
                            .addComponent(localContestCodeLabel)
                            .addComponent(localContest4charOKMsgLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)))
                    .addComponent(localContestMsgLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        localContestCodePanelLayout.setVerticalGroup(
            localContestCodePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(localContestCodePanelLayout.createSequentialGroup()
                .addComponent(localContestLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContestNoButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContest4YesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContest6YesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContestCodeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContestCodeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContest4charOKMsgLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localContestMsgLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout card2Layout = new javax.swing.GroupLayout(card2);
        card2.setLayout(card2Layout);
        card2Layout.setHorizontalGroup(
            card2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(localContestCodePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card2Layout.setVerticalGroup(
            card2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(localContestCodePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        widgetPanel.add(card2, "2");

        card3.setName("card3"); // NOI18N

        deckElevationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Deck Elevation", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14), new java.awt.Color(0, 0, 128))); // NOI18N
        deckElevationPanel.setName("deckElevationPanel"); // NOI18N

        deckElevationButtonPanel.setName("deckElevationButtonPanel"); // NOI18N
        deckElevationButtonPanel.setLayout(new javax.swing.BoxLayout(deckElevationButtonPanel, javax.swing.BoxLayout.Y_AXIS));

        deckElevationUpButton.setIcon(resourceMap.getIcon("deckElevationUpButton.icon")); // NOI18N
        deckElevationUpButton.setEnabled(false);
        deckElevationUpButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        deckElevationUpButton.setMaximumSize(new java.awt.Dimension(19, 12));
        deckElevationUpButton.setMinimumSize(new java.awt.Dimension(19, 12));
        deckElevationUpButton.setName("deckElevationUpButton"); // NOI18N
        deckElevationUpButton.setPreferredSize(new java.awt.Dimension(19, 12));
        deckElevationButtonPanel.add(deckElevationUpButton);

        deckElevationDownButton.setIcon(resourceMap.getIcon("deckElevationDownButton.icon")); // NOI18N
        deckElevationDownButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        deckElevationDownButton.setMaximumSize(new java.awt.Dimension(19, 12));
        deckElevationDownButton.setMinimumSize(new java.awt.Dimension(19, 12));
        deckElevationDownButton.setName("deckElevationDownButton"); // NOI18N
        deckElevationDownButton.setPreferredSize(new java.awt.Dimension(19, 12));
        deckElevationButtonPanel.add(deckElevationDownButton);

        ((ExtendedComboBox)deckElevationBox).fillUsingResources(new String [] {"24meters", "20meters", "16meters", "12meters", "8meters", "4meters", "0meters"}, SetupWizard.class);
        deckElevationBox.setName("deckElevationBox"); // NOI18N
        deckElevationBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                deckElevationBoxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout deckElevationPanelLayout = new javax.swing.GroupLayout(deckElevationPanel);
        deckElevationPanel.setLayout(deckElevationPanelLayout);
        deckElevationPanelLayout.setHorizontalGroup(
            deckElevationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, deckElevationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deckElevationBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deckElevationButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        deckElevationPanelLayout.setVerticalGroup(
            deckElevationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckElevationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deckElevationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(deckElevationBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deckElevationButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        supportConfigPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Support Configuration", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14), new java.awt.Color(0, 0, 128))); // NOI18N
        supportConfigPanel.setName("supportConfigPanel"); // NOI18N

        abutmentPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        abutmentPanel.setName("abutmentPanel"); // NOI18N

        abutmentGroup.add(standardAbutmentsButton);
        standardAbutmentsButton.setSelected(true);
        standardAbutmentsButton.setText(resourceMap.getString("standardAbutmentsButton.text")); // NOI18N
        standardAbutmentsButton.setName("standardAbutmentsButton"); // NOI18N

        abutmentGroup.add(archAbutmentsButton);
        archAbutmentsButton.setText(resourceMap.getString("archAbutmentsButton.text")); // NOI18N
        archAbutmentsButton.setName("archAbutmentsButton"); // NOI18N
        archAbutmentsButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                archAbutmentsButtonItemStateChanged(evt);
            }
        });

        archHeightLabel.setText(resourceMap.getString("archHeightLabel.text")); // NOI18N
        archHeightLabel.setEnabled(false);
        archHeightLabel.setName("archHeightLabel"); // NOI18N

        archHeightButtonPanel.setName("archHeightButtonPanel"); // NOI18N
        archHeightButtonPanel.setLayout(new javax.swing.BoxLayout(archHeightButtonPanel, javax.swing.BoxLayout.Y_AXIS));

        archHeightUpButton.setIcon(resourceMap.getIcon("archHeightUpButton.icon")); // NOI18N
        archHeightUpButton.setEnabled(false);
        archHeightUpButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        archHeightUpButton.setMaximumSize(new java.awt.Dimension(19, 12));
        archHeightUpButton.setMinimumSize(new java.awt.Dimension(19, 12));
        archHeightUpButton.setName("archHeightUpButton"); // NOI18N
        archHeightUpButton.setPreferredSize(new java.awt.Dimension(19, 12));
        archHeightButtonPanel.add(archHeightUpButton);

        archHeightDownButton.setIcon(resourceMap.getIcon("archHeightDownButton.icon")); // NOI18N
        archHeightDownButton.setEnabled(false);
        archHeightDownButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        archHeightDownButton.setMaximumSize(new java.awt.Dimension(19, 12));
        archHeightDownButton.setMinimumSize(new java.awt.Dimension(19, 12));
        archHeightDownButton.setName("archHeightDownButton"); // NOI18N
        archHeightDownButton.setPreferredSize(new java.awt.Dimension(19, 12));
        archHeightButtonPanel.add(archHeightDownButton);

        ((ExtendedComboBox)archHeightBox).fillUsingResources(new String [] {"24meters", "20meters", "16meters", "12meters", "8meters", "4meters"}, SetupWizard.class);
        archHeightBox.setSelectedIndex(archHeightBox.getItemCount() - 1);
        archHeightBox.setEnabled(false);
        archHeightBox.setName("archHeightBox"); // NOI18N
        archHeightBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                archHeightBoxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout abutmentPanelLayout = new javax.swing.GroupLayout(abutmentPanel);
        abutmentPanel.setLayout(abutmentPanelLayout);
        abutmentPanelLayout.setHorizontalGroup(
            abutmentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(abutmentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(abutmentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(standardAbutmentsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(archAbutmentsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(abutmentPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(abutmentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(archHeightLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(abutmentPanelLayout.createSequentialGroup()
                                .addComponent(archHeightBox, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(archHeightButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        abutmentPanelLayout.setVerticalGroup(
            abutmentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(abutmentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(standardAbutmentsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(archAbutmentsButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(archHeightLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(abutmentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(archHeightBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(archHeightButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pierPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pierPanel.setName("pierPanel"); // NOI18N

        pierGroup.add(noPierButton);
        noPierButton.setSelected(true);
        noPierButton.setText(resourceMap.getString("noPierButton.text")); // NOI18N
        noPierButton.setName("noPierButton"); // NOI18N

        pierGroup.add(pierButton);
        pierButton.setText(resourceMap.getString("pierButton.text")); // NOI18N
        pierButton.setName("pierButton"); // NOI18N
        pierButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                pierButtonItemStateChanged(evt);
            }
        });

        pierHeightLabel.setText(resourceMap.getString("pierHeightLabel.text")); // NOI18N
        pierHeightLabel.setEnabled(false);
        pierHeightLabel.setName("pierHeightLabel"); // NOI18N

        pierHeightButtonPanel.setName("pierHeightButtonPanel"); // NOI18N
        pierHeightButtonPanel.setLayout(new javax.swing.BoxLayout(pierHeightButtonPanel, javax.swing.BoxLayout.Y_AXIS));

        pierHeightUpButton.setIcon(resourceMap.getIcon("pierHeightUpButton.icon")); // NOI18N
        pierHeightUpButton.setEnabled(false);
        pierHeightUpButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        pierHeightUpButton.setMaximumSize(new java.awt.Dimension(19, 12));
        pierHeightUpButton.setMinimumSize(new java.awt.Dimension(19, 12));
        pierHeightUpButton.setName("pierHeightUpButton"); // NOI18N
        pierHeightUpButton.setPreferredSize(new java.awt.Dimension(19, 12));
        pierHeightButtonPanel.add(pierHeightUpButton);

        pierHeightDownButton.setIcon(resourceMap.getIcon("pierHeightDownButton.icon")); // NOI18N
        pierHeightDownButton.setEnabled(false);
        pierHeightDownButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        pierHeightDownButton.setMaximumSize(new java.awt.Dimension(19, 12));
        pierHeightDownButton.setMinimumSize(new java.awt.Dimension(19, 12));
        pierHeightDownButton.setName("pierHeightDownButton"); // NOI18N
        pierHeightDownButton.setPreferredSize(new java.awt.Dimension(19, 12));
        pierHeightButtonPanel.add(pierHeightDownButton);

        ((ExtendedComboBox)pierHeightBox).fillUsingResources(new String [] {"24meters", "20meters", "16meters", "12meters", "8meters", "4meters", "0meters"}, SetupWizard.class);
        pierHeightBox.setSelectedIndex(pierHeightBox.getItemCount() - 1);
        pierHeightBox.setEnabled(false);
        pierHeightBox.setName("pierHeightBox"); // NOI18N
        pierHeightBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                pierHeightBoxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout pierPanelLayout = new javax.swing.GroupLayout(pierPanel);
        pierPanel.setLayout(pierPanelLayout);
        pierPanelLayout.setHorizontalGroup(
            pierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pierPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(noPierButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pierPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(pierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pierHeightLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(pierPanelLayout.createSequentialGroup()
                                .addComponent(pierHeightBox, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pierHeightButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(pierButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pierPanelLayout.setVerticalGroup(
            pierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pierPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(noPierButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pierButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pierHeightLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pierHeightBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pierHeightButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        anchoragePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        anchoragePanel.setName("anchoragePanel"); // NOI18N

        anchorageGroup.add(noAnchorageButton);
        noAnchorageButton.setSelected(true);
        noAnchorageButton.setText(resourceMap.getString("noAnchorageButton.text")); // NOI18N
        noAnchorageButton.setName("noAnchorageButton"); // NOI18N
        noAnchorageButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                noAnchorageButtonItemStateChanged(evt);
            }
        });

        anchorageGroup.add(oneAnchorageButton);
        oneAnchorageButton.setText(resourceMap.getString("oneAnchorageButton.text")); // NOI18N
        oneAnchorageButton.setName("oneAnchorageButton"); // NOI18N
        oneAnchorageButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                oneAnchorageButtonItemStateChanged(evt);
            }
        });

        anchorageGroup.add(twoAnchoragesButton);
        twoAnchoragesButton.setText(resourceMap.getString("twoAnchoragesButton.text")); // NOI18N
        twoAnchoragesButton.setName("twoAnchoragesButton"); // NOI18N
        twoAnchoragesButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                twoAnchoragesButtonItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout anchoragePanelLayout = new javax.swing.GroupLayout(anchoragePanel);
        anchoragePanel.setLayout(anchoragePanelLayout);
        anchoragePanelLayout.setHorizontalGroup(
            anchoragePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(anchoragePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(anchoragePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(noAnchorageButton, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                    .addComponent(twoAnchoragesButton)
                    .addComponent(oneAnchorageButton, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE))
                .addContainerGap())
        );
        anchoragePanelLayout.setVerticalGroup(
            anchoragePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(anchoragePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(noAnchorageButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(oneAnchorageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(twoAnchoragesButton)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout supportConfigPanelLayout = new javax.swing.GroupLayout(supportConfigPanel);
        supportConfigPanel.setLayout(supportConfigPanelLayout);
        supportConfigPanelLayout.setHorizontalGroup(
            supportConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, supportConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(supportConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(abutmentPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pierPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(anchoragePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        supportConfigPanelLayout.setVerticalGroup(
            supportConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(supportConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(abutmentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(pierPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(anchoragePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout card3Layout = new javax.swing.GroupLayout(card3);
        card3.setLayout(card3Layout);
        card3Layout.setHorizontalGroup(
            card3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(supportConfigPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(deckElevationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card3Layout.setVerticalGroup(
            card3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card3Layout.createSequentialGroup()
                .addComponent(deckElevationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addComponent(supportConfigPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        widgetPanel.add(card3, "3");

        card4.setName("card4"); // NOI18N

        deckMaterialPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("deckMaterialPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("deckMaterialPanel.border.titleFont"), resourceMap.getColor("deckMaterialPanel.border.titleColor"))); // NOI18N
        deckMaterialPanel.setName("deckMaterialPanel"); // NOI18N

        deckMaterialGroup.add(mediumConcreteButton);
        mediumConcreteButton.setSelected(true);
        mediumConcreteButton.setText(resourceMap.getString("mediumConcreteButton.text")); // NOI18N
        mediumConcreteButton.setName("mediumConcreteButton"); // NOI18N
        mediumConcreteButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                mediumConcreteButtonItemStateChanged(evt);
            }
        });

        deckMaterialGroup.add(highConcreteButton);
        highConcreteButton.setText(resourceMap.getString("highConcreteButton.text")); // NOI18N
        highConcreteButton.setName("highConcreteButton"); // NOI18N
        highConcreteButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                highConcreteButtonItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout deckMaterialPanelLayout = new javax.swing.GroupLayout(deckMaterialPanel);
        deckMaterialPanel.setLayout(deckMaterialPanelLayout);
        deckMaterialPanelLayout.setHorizontalGroup(
            deckMaterialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckMaterialPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deckMaterialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mediumConcreteButton, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                    .addComponent(highConcreteButton, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE))
                .addContainerGap())
        );
        deckMaterialPanelLayout.setVerticalGroup(
            deckMaterialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckMaterialPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mediumConcreteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(highConcreteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(42, Short.MAX_VALUE))
        );

        loadingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("loadingPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("loadingPanel.border.titleFont"), resourceMap.getColor("loadingPanel.border.titleColor"))); // NOI18N
        loadingPanel.setName("loadingPanel"); // NOI18N

        loadingGroup.add(standardTruckButton);
        standardTruckButton.setSelected(true);
        standardTruckButton.setText(resourceMap.getString("standardTruckButton.text")); // NOI18N
        standardTruckButton.setName("standardTruckButton"); // NOI18N
        standardTruckButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                standardTruckButtonItemStateChanged(evt);
            }
        });

        loadingGroup.add(permitLoadButton);
        permitLoadButton.setText(resourceMap.getString("permitLoadButton.text")); // NOI18N
        permitLoadButton.setName("permitLoadButton"); // NOI18N
        permitLoadButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                permitLoadButtonItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout loadingPanelLayout = new javax.swing.GroupLayout(loadingPanel);
        loadingPanel.setLayout(loadingPanelLayout);
        loadingPanelLayout.setHorizontalGroup(
            loadingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(loadingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(standardTruckButton)
                    .addComponent(permitLoadButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        loadingPanelLayout.setVerticalGroup(
            loadingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(standardTruckButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(permitLoadButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(240, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout card4Layout = new javax.swing.GroupLayout(card4);
        card4.setLayout(card4Layout);
        card4Layout.setHorizontalGroup(
            card4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(loadingPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(deckMaterialPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card4Layout.setVerticalGroup(
            card4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, card4Layout.createSequentialGroup()
                .addComponent(deckMaterialPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        widgetPanel.add(card4, "4");

        card5.setName("card5"); // NOI18N

        selectTemplatePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Select A Template", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial 14", 1, 12), new java.awt.Color(0, 0, 128))); // NOI18N
        selectTemplatePanel.setName("selectTemplatePanel"); // NOI18N

        templateList.setBackground(resourceMap.getColor("templateList.background")); // NOI18N
        templateList.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        templateList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "<none>" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        templateList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        templateList.setName("templateList"); // NOI18N
        templateList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                templateListValueChanged(evt);
            }
        });

        javax.swing.GroupLayout selectTemplatePanelLayout = new javax.swing.GroupLayout(selectTemplatePanel);
        selectTemplatePanel.setLayout(selectTemplatePanelLayout);
        selectTemplatePanelLayout.setHorizontalGroup(
            selectTemplatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectTemplatePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(templateList, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                .addContainerGap())
        );
        selectTemplatePanelLayout.setVerticalGroup(
            selectTemplatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectTemplatePanelLayout.createSequentialGroup()
                .addComponent(templateList, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout card5Layout = new javax.swing.GroupLayout(card5);
        card5.setLayout(card5Layout);
        card5Layout.setHorizontalGroup(
            card5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(selectTemplatePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card5Layout.setVerticalGroup(
            card5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(selectTemplatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        widgetPanel.add(card5, "5");

        card6.setName("card6"); // NOI18N

        titleBlockInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("titleBlockInfoPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("titleBlockInfoPanel.border.titleFont"), resourceMap.getColor("titleBlockInfoPanel.border.titleColor"))); // NOI18N
        titleBlockInfoPanel.setName("titleBlockInfoPanel"); // NOI18N

        projectNameLabel.setText(resourceMap.getString("projectNameLabel.text")); // NOI18N
        projectNameLabel.setName("projectNameLabel"); // NOI18N

        projectNameEdit.setText(resourceMap.getString("projectNameEdit.text")); // NOI18N
        projectNameEdit.setEnabled(false);
        projectNameEdit.setName("projectNameEdit"); // NOI18N

        designedByLabel.setText(resourceMap.getString("designedByLabel.text")); // NOI18N
        designedByLabel.setName("designedByLabel"); // NOI18N

        designedByEdit.setText(resourceMap.getString("designedByEdit.text")); // NOI18N
        designedByEdit.setName("designedByEdit"); // NOI18N

        projectIdLabel.setText(resourceMap.getString("projectIdLabel.text")); // NOI18N
        projectIdLabel.setName("projectIdLabel"); // NOI18N

        projectIdEdit.setText(resourceMap.getString("projectIdEdit.text")); // NOI18N
        projectIdEdit.setName("projectIdEdit"); // NOI18N

        projectIdPrefixLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        projectIdPrefixLabel.setText(resourceMap.getString("projectIdPrefixLabel.text")); // NOI18N
        projectIdPrefixLabel.setEnabled(false);
        projectIdPrefixLabel.setName("projectIdPrefixLabel"); // NOI18N

        javax.swing.GroupLayout titleBlockInfoPanelLayout = new javax.swing.GroupLayout(titleBlockInfoPanel);
        titleBlockInfoPanel.setLayout(titleBlockInfoPanelLayout);
        titleBlockInfoPanelLayout.setHorizontalGroup(
            titleBlockInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titleBlockInfoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(titleBlockInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, titleBlockInfoPanelLayout.createSequentialGroup()
                        .addComponent(projectIdPrefixLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(projectIdEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(designedByEdit)
                    .addComponent(projectNameEdit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(projectIdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(designedByLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        titleBlockInfoPanelLayout.setVerticalGroup(
            titleBlockInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titleBlockInfoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(projectNameEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(designedByLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(designedByEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(projectIdLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(titleBlockInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectIdEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectIdPrefixLabel))
                .addContainerGap(322, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout card6Layout = new javax.swing.GroupLayout(card6);
        card6.setLayout(card6Layout);
        card6Layout.setHorizontalGroup(
            card6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(titleBlockInfoPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card6Layout.setVerticalGroup(
            card6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(titleBlockInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        widgetPanel.add(card6, "6");

        card7.setName("card7"); // NOI18N

        designPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("designPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("designPanel.border.titleFont"), resourceMap.getColor("designPanel.border.titleColor"))); // NOI18N
        designPanel.setName("designPanel"); // NOI18N

        instructionsPane.setText(resourceMap.getString("instructionsPane.text")); // NOI18N
        instructionsPane.setName("instructionsPane"); // NOI18N

        javax.swing.GroupLayout designPanelLayout = new javax.swing.GroupLayout(designPanel);
        designPanel.setLayout(designPanelLayout);
        designPanelLayout.setHorizontalGroup(
            designPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(designPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(instructionsPane, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                .addContainerGap())
        );
        designPanelLayout.setVerticalGroup(
            designPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(designPanelLayout.createSequentialGroup()
                .addComponent(instructionsPane, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout card7Layout = new javax.swing.GroupLayout(card7);
        card7.setLayout(card7Layout);
        card7Layout.setHorizontalGroup(
            card7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(designPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        card7Layout.setVerticalGroup(
            card7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(designPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        widgetPanel.add(card7, "7");

        deckCrossSectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Deck Cross-Section", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14), new java.awt.Color(0, 0, 128))); // NOI18N
        deckCrossSectionPanel.setName("deckCrossSectionPanel"); // NOI18N

        deckCartoonLabel.setIcon(resourceMap.getIcon("deckCartoonLabel.icon")); // NOI18N
        deckCartoonLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        deckCartoonLabel.setName("deckCartoonLabel"); // NOI18N

        javax.swing.GroupLayout deckCrossSectionPanelLayout = new javax.swing.GroupLayout(deckCrossSectionPanel);
        deckCrossSectionPanel.setLayout(deckCrossSectionPanelLayout);
        deckCrossSectionPanelLayout.setHorizontalGroup(
            deckCrossSectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckCrossSectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deckCartoonLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        deckCrossSectionPanelLayout.setVerticalGroup(
            deckCrossSectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckCrossSectionPanelLayout.createSequentialGroup()
                .addComponent(deckCartoonLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        elevationViewPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Elevation View", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 1, 14), new java.awt.Color(0, 0, 128))); // NOI18N
        elevationViewPanel.setName("elevationViewPanel"); // NOI18N

        elevationViewLabel.setIcon(resourceMap.getIcon("elevationViewLabel.icon")); // NOI18N
        elevationViewLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        elevationViewLabel.setName("elevationViewLabel"); // NOI18N

        legendPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        legendPanel.setName("legendPanel"); // NOI18N

        legendRiverBankLabel.setIcon(resourceMap.getIcon("legendRiverBankLabel.icon")); // NOI18N
        legendRiverBankLabel.setText(resourceMap.getString("legendRiverBankLabel.text")); // NOI18N
        legendRiverBankLabel.setName("legendRiverBankLabel"); // NOI18N

        legendExcavationLabel.setIcon(resourceMap.getIcon("legendExcavationLabel.icon")); // NOI18N
        legendExcavationLabel.setText(resourceMap.getString("legendExcavationLabel.text")); // NOI18N
        legendExcavationLabel.setName("legendExcavationLabel"); // NOI18N

        legendRiverLabel.setIcon(resourceMap.getIcon("legendRiverLabel.icon")); // NOI18N
        legendRiverLabel.setText(resourceMap.getString("legendRiverLabel.text")); // NOI18N
        legendRiverLabel.setName("legendRiverLabel"); // NOI18N

        legendTopSpacerLabel.setText(" "); // NOI18N
        legendTopSpacerLabel.setName("legendTopSpacerLabel"); // NOI18N

        legendDeckLabel.setIcon(resourceMap.getIcon("legendDeckLabel.icon")); // NOI18N
        legendDeckLabel.setText(resourceMap.getString("legendDeckLabel.text")); // NOI18N
        legendDeckLabel.setName("legendDeckLabel"); // NOI18N

        legendAbutmentLabel.setIcon(resourceMap.getIcon("legendAbutmentLabel.icon")); // NOI18N
        legendAbutmentLabel.setText(resourceMap.getString("legendAbutmentLabel.text")); // NOI18N
        legendAbutmentLabel.setName("legendAbutmentLabel"); // NOI18N

        legendPierLabel.setIcon(resourceMap.getIcon("legendPierLabel.icon")); // NOI18N
        legendPierLabel.setText(resourceMap.getString("legendPierLabel.text")); // NOI18N
        legendPierLabel.setName("legendPierLabel"); // NOI18N
        legendPierLabel.setOpaque(true);

        legendBottomSpacerLabel.setText(" "); // NOI18N
        legendBottomSpacerLabel.setMaximumSize(new java.awt.Dimension(3, 19));
        legendBottomSpacerLabel.setMinimumSize(new java.awt.Dimension(3, 19));
        legendBottomSpacerLabel.setName("legendBottomSpacerLabel"); // NOI18N
        legendBottomSpacerLabel.setPreferredSize(new java.awt.Dimension(3, 19));

        javax.swing.GroupLayout legendPanelLayout = new javax.swing.GroupLayout(legendPanel);
        legendPanel.setLayout(legendPanelLayout);
        legendPanelLayout.setHorizontalGroup(
            legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(legendPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(legendDeckLabel)
                    .addComponent(legendRiverBankLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(legendPanelLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(legendExcavationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(legendRiverLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(legendPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(legendAbutmentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(legendPierLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(legendTopSpacerLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(legendBottomSpacerLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        legendPanelLayout.setVerticalGroup(
            legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(legendPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(legendRiverBankLabel)
                    .addComponent(legendRiverLabel)
                    .addComponent(legendExcavationLabel)
                    .addComponent(legendTopSpacerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(legendDeckLabel)
                    .addComponent(legendAbutmentLabel)
                    .addComponent(legendPierLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(legendBottomSpacerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        legendPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {legendExcavationLabel, legendRiverBankLabel, legendRiverLabel, legendTopSpacerLabel});

        legendPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {legendAbutmentLabel, legendBottomSpacerLabel, legendDeckLabel, legendPierLabel});

        javax.swing.GroupLayout elevationViewPanelLayout = new javax.swing.GroupLayout(elevationViewPanel);
        elevationViewPanel.setLayout(elevationViewPanelLayout);
        elevationViewPanelLayout.setHorizontalGroup(
            elevationViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(elevationViewPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(elevationViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(elevationViewLabel)
                    .addComponent(legendPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        elevationViewPanelLayout.setVerticalGroup(
            elevationViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, elevationViewPanelLayout.createSequentialGroup()
                .addComponent(elevationViewLabel)
                .addGap(18, 18, 18)
                .addComponent(legendPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tipPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Design Tip:"));
        tipPanel.setName("tipPanel"); // NOI18N

        tipPane.setName("tipPane"); // NOI18N

        javax.swing.GroupLayout tipPanelLayout = new javax.swing.GroupLayout(tipPanel);
        tipPanel.setLayout(tipPanelLayout);
        tipPanelLayout.setHorizontalGroup(
            tipPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tipPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipPane, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                .addContainerGap())
        );
        tipPanelLayout.setVerticalGroup(
            tipPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tipPanelLayout.createSequentialGroup()
                .addComponent(tipPane)
                .addContainerGap())
        );

        siteCostPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Site Cost:"));
        siteCostPanel.setName("siteCostPanel"); // NOI18N

        siteCostDetailTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        siteCostDetailTable.setModel(new SiteCostTableModel() );
        ((SiteCostTable)siteCostDetailTable).initalize();
        siteCostDetailTable.setFocusable(false);
        siteCostDetailTable.setIntercellSpacing(new java.awt.Dimension(6, 1));
        siteCostDetailTable.setName("siteCostDetailTable"); // NOI18N
        siteCostDetailTable.setRowSelectionAllowed(false);
        siteCostDetailTable.getTableHeader().setReorderingAllowed(false);

        siteCostLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        siteCostLabel.setText(resourceMap.getString("siteCostLabel.text")); // NOI18N
        siteCostLabel.setName("siteCostLabel"); // NOI18N

        dropRaiseButton.setIcon(resourceMap.getIcon("dropRaiseButton.icon")); // NOI18N
        dropRaiseButton.setMaximumSize(new java.awt.Dimension(27, 27));
        dropRaiseButton.setMinimumSize(new java.awt.Dimension(27, 27));
        dropRaiseButton.setName("dropRaiseButton"); // NOI18N
        dropRaiseButton.setPreferredSize(new java.awt.Dimension(27, 27));
        dropRaiseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropRaiseButtonActionPerformed(evt);
            }
        });

        costNoteLabel.setText(resourceMap.getString("costNoteLabel.text")); // NOI18N
        costNoteLabel.setName("costNoteLabel"); // NOI18N

        siteConditionsLabel.setText(resourceMap.getString("siteConditionsLabel.text")); // NOI18N
        siteConditionsLabel.setName("siteConditionsLabel"); // NOI18N

        javax.swing.GroupLayout siteCostPanelLayout = new javax.swing.GroupLayout(siteCostPanel);
        siteCostPanel.setLayout(siteCostPanelLayout);
        siteCostPanelLayout.setHorizontalGroup(
            siteCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, siteCostPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(siteCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(siteCostDetailTable, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(siteCostPanelLayout.createSequentialGroup()
                        .addComponent(siteCostLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(costNoteLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 539, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(siteConditionsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dropRaiseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        siteCostPanelLayout.setVerticalGroup(
            siteCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(siteCostPanelLayout.createSequentialGroup()
                .addGroup(siteCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(dropRaiseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(siteCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(siteCostLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(costNoteLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(siteConditionsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(siteCostDetailTable, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                .addContainerGap())
        );

        siteCostDetailTable.getColumnModel().getColumn(0).setResizable(false);
        siteCostDetailTable.getColumnModel().getColumn(1).setResizable(false);
        siteCostDetailTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        siteCostDetailTable.getColumnModel().getColumn(2).setResizable(false);

        help.setText(resourceMap.getString("help.text")); // NOI18N
        help.setName("help"); // NOI18N

        cancel.setText(resourceMap.getString("cancel.text")); // NOI18N
        cancel.setName("cancel"); // NOI18N
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        back.setText(resourceMap.getString("back.text")); // NOI18N
        back.setName("back"); // NOI18N
        back.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backActionPerformed(evt);
            }
        });

        next.setText(resourceMap.getString("next.text")); // NOI18N
        next.setName("next"); // NOI18N
        next.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextActionPerformed(evt);
            }
        });

        finish.setText(resourceMap.getString("finish.text")); // NOI18N
        finish.setName("finish"); // NOI18N
        finish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                finishActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(siteCostPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(help)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(back)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(next)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(finish))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pageNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pageTitle)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(widgetPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(deckCrossSectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(elevationViewPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tipPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pageNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pageTitle))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tipPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(deckCrossSectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(elevationViewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(widgetPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(siteCostPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(finish)
                    .addComponent(next)
                    .addComponent(back)
                    .addComponent(cancel)
                    .addComponent(help))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
    setVisible(false);
}//GEN-LAST:event_cancelActionPerformed

private void backActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backActionPerformed
    currentPage = pages[currentPage].getBackPageNumber();
    pages[currentPage].load();
}//GEN-LAST:event_backActionPerformed

private void nextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextActionPerformed
    currentPage = pages[currentPage].getNextPageNumber();
    pages[currentPage].load();
}//GEN-LAST:event_nextActionPerformed

private void finishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_finishActionPerformed
    ok = true;//GEN-LAST:event_finishActionPerformed
    setVisible(false);
}

private void localContest6YesButtonStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localContest6YesButtonStateChanged
    boolean localContest = localContest6YesButton.isSelected();
    next.setEnabled(!localContest);
    localContestCodeLabel.setEnabled(localContest);
    localContestCodeField.setEnabled(localContest);
    if (localContest) {
        localContestCodeField.requestFocusInWindow();
    }
    else {
        localContestCodeField.setText("");
        localContestMsgLabel.setText("");
    }
}//GEN-LAST:event_localContest6YesButtonStateChanged

private void deckElevationBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_deckElevationBoxItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        beginUpdate();
        int nItems = deckElevationBox.getItemCount();
        int selectedIndex = deckElevationBox.getSelectedIndex();
        ((ExtendedComboBoxModel) archHeightBox.getModel()).setBase(selectedIndex);
        ((ExtendedComboBoxModel) pierHeightBox.getModel()).setBase(selectedIndex);
        boolean zeroMeters = (selectedIndex == nItems - 1);
        standardAbutmentsButton.setSelected(zeroMeters);
        archAbutmentsButton.setEnabled(!zeroMeters);
        if (!zeroMeters) {
            if (archHeightBox.getSelectedIndex() == -1) {
                archHeightBox.setSelectedIndex(archHeightBox.getItemCount() - 1);
            }
            if (pierHeightBox.getSelectedIndex() == -1) {
                pierHeightBox.setSelectedIndex(pierHeightBox.getItemCount() - 1);
            }
        }
        endUpdate();
    }
}//GEN-LAST:event_deckElevationBoxItemStateChanged

private void archHeightBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_archHeightBoxItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_archHeightBoxItemStateChanged

private void pierHeightBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_pierHeightBoxItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_pierHeightBoxItemStateChanged

private void archAbutmentsButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_archAbutmentsButtonItemStateChanged
    beginUpdate();
    boolean selected = (evt.getStateChange() == ItemEvent.SELECTED);
    if (selected) {
        noPierButton.setSelected(true);
        noAnchorageButton.setSelected(true);
    }
    archHeightLabel.setEnabled(selected);
    archHeightBox.setEnabled(selected);
    pierButton.setEnabled(!selected);
    oneAnchorageButton.setEnabled(!selected);
    twoAnchoragesButton.setEnabled(!selected);
    endUpdate();
}//GEN-LAST:event_archAbutmentsButtonItemStateChanged

private void pierButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_pierButtonItemStateChanged
    beginUpdate();
    boolean isSelected = (evt.getStateChange() == ItemEvent.SELECTED);
    pierHeightLabel.setEnabled(isSelected);
    pierHeightBox.setEnabled(isSelected);
    oneAnchorageButton.setEnabled(!isSelected);
    if (isSelected) {
        if (oneAnchorageButton.isSelected()) {
            noAnchorageButton.setSelected(true);
        }
    }
    // Must update legend because pier icon may show or disappear.
    pages[currentPage].updateLegend();
    endUpdate();
}//GEN-LAST:event_pierButtonItemStateChanged

private void noAnchorageButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_noAnchorageButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_noAnchorageButtonItemStateChanged

private void oneAnchorageButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_oneAnchorageButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_oneAnchorageButtonItemStateChanged

private void twoAnchoragesButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_twoAnchoragesButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_twoAnchoragesButtonItemStateChanged

private void mediumConcreteButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_mediumConcreteButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_mediumConcreteButtonItemStateChanged

private void highConcreteButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_highConcreteButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_highConcreteButtonItemStateChanged

private void standardTruckButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_standardTruckButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_standardTruckButtonItemStateChanged

private void permitLoadButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_permitLoadButtonItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        update();
    }
}//GEN-LAST:event_permitLoadButtonItemStateChanged

private void templateListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_templateListValueChanged
    bridgeCartoonView.getBridgeSketchView().setModel(templateList.getSelectedIndex() == 0 ? null : (BridgeSketchModel) templateList.getSelectedValue());
    elevationViewLabel.repaint();
}//GEN-LAST:event_templateListValueChanged

private void localContest4YesButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_localContest4YesButtonItemStateChanged
    localContest4charOKMsgLabel.setVisible(localContest4YesButton.isSelected());
}//GEN-LAST:event_localContest4YesButtonItemStateChanged

    private void dropRaiseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropRaiseButtonActionPerformed
        showDetailPane(!siteCostDetailTable.isVisible());
    }//GEN-LAST:event_dropRaiseButtonActionPerformed
// <editor-fold defaultstate="collapsed" desc="Global defs">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup abutmentGroup;
    private javax.swing.JPanel abutmentPanel;
    private javax.swing.ButtonGroup anchorageGroup;
    private javax.swing.JPanel anchoragePanel;
    private javax.swing.JRadioButton archAbutmentsButton;
    private javax.swing.JComboBox archHeightBox;
    private javax.swing.JPanel archHeightButtonPanel;
    private javax.swing.JButton archHeightDownButton;
    private javax.swing.JLabel archHeightLabel;
    private javax.swing.JButton archHeightUpButton;
    private javax.swing.JButton back;
    private javax.swing.JButton cancel;
    private javax.swing.JPanel card1;
    private javax.swing.JPanel card2;
    private javax.swing.JPanel card3;
    private javax.swing.JPanel card4;
    private javax.swing.JPanel card5;
    private javax.swing.JPanel card6;
    private javax.swing.JPanel card7;
    private javax.swing.JLabel costNoteLabel;
    private javax.swing.JLabel deckCartoonLabel;
    private javax.swing.JPanel deckCrossSectionPanel;
    private javax.swing.JComboBox deckElevationBox;
    private javax.swing.JPanel deckElevationButtonPanel;
    private javax.swing.JButton deckElevationDownButton;
    private javax.swing.JPanel deckElevationPanel;
    private javax.swing.JButton deckElevationUpButton;
    private javax.swing.ButtonGroup deckMaterialGroup;
    private javax.swing.JPanel deckMaterialPanel;
    private javax.swing.JPanel designPanel;
    private javax.swing.JTextField designedByEdit;
    private javax.swing.JLabel designedByLabel;
    private javax.swing.JButton dropRaiseButton;
    private javax.swing.JLabel elevationViewLabel;
    private javax.swing.JPanel elevationViewPanel;
    private javax.swing.JButton finish;
    private javax.swing.JButton help;
    private javax.swing.JRadioButton highConcreteButton;
    private javax.swing.JTextPane instructionsPane;
    private javax.swing.JLabel legendAbutmentLabel;
    private javax.swing.JLabel legendBottomSpacerLabel;
    private javax.swing.JLabel legendDeckLabel;
    private javax.swing.JLabel legendExcavationLabel;
    private javax.swing.JPanel legendPanel;
    private javax.swing.JLabel legendPierLabel;
    private javax.swing.JLabel legendRiverBankLabel;
    private javax.swing.JLabel legendRiverLabel;
    private javax.swing.JLabel legendTopSpacerLabel;
    private javax.swing.ButtonGroup loadingGroup;
    private javax.swing.JPanel loadingPanel;
    private javax.swing.JRadioButton localContest4YesButton;
    private javax.swing.JLabel localContest4charOKMsgLabel;
    private javax.swing.JRadioButton localContest6YesButton;
    private javax.swing.JTextField localContestCodeField;
    private javax.swing.JTextPane localContestCodeLabel;
    private javax.swing.JPanel localContestCodePanel;
    private javax.swing.ButtonGroup localContestGroup;
    private javax.swing.JLabel localContestLabel;
    private javax.swing.JLabel localContestMsgLabel;
    private javax.swing.JRadioButton localContestNoButton;
    private javax.swing.JRadioButton mediumConcreteButton;
    private javax.swing.JButton next;
    private javax.swing.JRadioButton noAnchorageButton;
    private javax.swing.JRadioButton noPierButton;
    private javax.swing.JRadioButton oneAnchorageButton;
    private javax.swing.JLabel pageNumber;
    private javax.swing.JLabel pageTitle;
    private javax.swing.JRadioButton permitLoadButton;
    private javax.swing.JRadioButton pierButton;
    private javax.swing.ButtonGroup pierGroup;
    private javax.swing.JComboBox pierHeightBox;
    private javax.swing.JPanel pierHeightButtonPanel;
    private javax.swing.JButton pierHeightDownButton;
    private javax.swing.JLabel pierHeightLabel;
    private javax.swing.JButton pierHeightUpButton;
    private javax.swing.JPanel pierPanel;
    private javax.swing.JTextField projectIdEdit;
    private javax.swing.JLabel projectIdLabel;
    private javax.swing.JLabel projectIdPrefixLabel;
    private javax.swing.JTextField projectNameEdit;
    private javax.swing.JLabel projectNameLabel;
    private javax.swing.JTextPane requirementPane;
    private javax.swing.JPanel requirementPanel;
    private javax.swing.JPanel selectTemplatePanel;
    private javax.swing.JLabel siteConditionsLabel;
    private javax.swing.JTable siteCostDetailTable;
    private javax.swing.JLabel siteCostLabel;
    private javax.swing.JPanel siteCostPanel;
    private javax.swing.JRadioButton standardAbutmentsButton;
    private javax.swing.JRadioButton standardTruckButton;
    private javax.swing.JPanel supportConfigPanel;
    private javax.swing.JList templateList;
    private javax.swing.JTextPane tipPane;
    private javax.swing.JPanel tipPanel;
    private javax.swing.JPanel titleBlockInfoPanel;
    private javax.swing.JRadioButton twoAnchoragesButton;
    private javax.swing.JPanel widgetPanel;
    // End of variables declaration//GEN-END:variables
// </editor-fold>
}
