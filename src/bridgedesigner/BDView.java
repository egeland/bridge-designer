/*
 * BDView.java
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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.event.ChangeEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import java.awt.event.ComponentAdapter;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import jogamp.common.Debug;

/**
 * The application's main frame.
 */
public final class BDView extends FrameView
        implements 
            ContextComponentProvider,
            RecentFileManager.Listener,
            KeyEventDispatcher,
            WelcomeDialog.AboutProvider {

    /**
     * The bridge structure being filled by loading, edited in the drafting panel, analyzed, animated, and saved.
     */
    private EditableBridgeModel bridge;
    /**
     * The view of the bridge in the drafting panel.
     */
    private BridgeDraftingView bridgeDraftingView;
    /**
     * Fine-grained control over event flow among interface components because
     * Swing's event architecture does not provide this.
     */
    private Dispatcher dispatcher;
    /**
     * Manager for applying aggregate application state to all the components of the GUI.  E.g. when there is
     * no bridge yet loaded, all the bridge editing controls should be disabled.  Also
     * state values that it's managing.
     */
    private EnabledStateManager enabledStateManager;
    private static final int NO_BRIDGE_STATE = 0;
    private static final int DRAFTING_STATE = 1;
    private static final int ANIMATING_STATE = 2;
    /**
     * Manager for tracking recently used files.  State is auto-saved in local storage.
     */
    private RecentFileManager recentFileManager;
    /**
     * Sub-components of the view.  Some not defined in the GUI builder.  Others are aliases.
     * 
     * File chooser dialog used for opening and saving bridges.
     */
    private JFileChooser fileChooser;
    /**
     * About dialog selected from the Help menu.
     */
    private JDialog aboutBox;
    /**
     * Dialog that presents optional tips to the user before being Welcomed.
     */
    private TipDialog tipDialog;
    /**
     * A mini-wizard that lets the user select among new bridge, sample bridge, or previously saved
     * bridge at startup time.
     */
    private WelcomeDialog welcomeDialog;
    /**
     * Complex 7-page dialog that lets the user choose the design conditions/scenario for a bridge,
     * also fill in title block information, and select a template.
     */
    private SetupWizard setupWizard;
    /**
     * Report dialog that is a printable table full of load test information from most recent analysis.
     */
    private LoadTestReport loadTestReport;
    /**
     * Report dialog that is a printable table of cost data.
     */
    private CostReport costReport;
    /**
     * Dialog offering user choice of design templates to load.  These are subdued sketches underlaid
     * on the draftng panel that show joint and member positions for some straightforward truss designs.
     */
    private LoadTemplateDialog loadTemplateDialog;    
    /**
     * Dialog offering user choice of sample designs, which are loaded just like previously saved bridge files.
     */
    private LoadSampleDialog loadSampleDialog;
    /**
     * Dialog showing views of the tree of design iterations since the user's session began.  User can
     * recover any previous iteration.  Essentially a structured auto-save mechanism.
     */
    private DesignIterationDialog designIterationDialog;
    /**
     * Dialog presenting a tiny tutorial for fixing common forms of instability.  Pops up when analysis
     * failes because the user's truss is inadequate in stability.
     */
    private UnstableModelDialog unstableModelDialog;
    /**
     * Dialog presenting a tiny tutorial for fixing members that fail the slenderness test, i.e. they 
     * are too "skinny."
     */
    private SlendernessTestFailDialog slendernessTestFailDialog;
    /**
     * Dialog shown as user is exiting program, reminding to check out
     * the bridge design contest.
     */
    private ContestReminderDialog contestReminderDialog;
    /**
     * Member detail window, which is a tab on the right hand side of the drafting panel, coinciding
     * with the member detail.  Serves as an explorer for properties of members and their material properties.
     */
    private MemberDetail memberDetail;
    /**
     * Not a Swing component but an aggregate object connected to three drop boxes
     * used for selecting from material inventory stock.  These are in the toolbar.
     */
    private StockSelector stockSelector;
    /**
     * Not a Swing component but an aggregate object connected to three drop boxes
     * used for selecting from material inventory stock.  These are in a right-click 
     * context dialog.
     */
    private StockSelector popupStockSelector;
    /**
     * The animation of the load test. Can be OpenGL or normal canvas.
     */
    private Animation animation;
    private Animation fixedEyeAnimation, flyThruAnimation;
    /**
     * State variables for the design tools floating toolbar dialog.
     */
    private boolean toolsDialogInitialized = false;
    private boolean toolsDialogVisibleState = true;
    /**
     * State variable and values for the card panels that can be selected in the
     * design area: the drafting board, animation, or nothing.
     */
    private static final String nullPanelCard = "nullPanel";
    private static final String designPanelCardName = "designPanel";
    private static final String flyThruAnimationPanelCardName = "flyThruAnimationPanel";
    private static final String fixedEyeAnimationPanelCardName = "fixedEyeAnimationPanel";
    private String animationPanelCardName = flyThruAnimationPanelCardName;
    private String selectedCard;
    /**
     * Local storage names for various bits of state. 
     */
    private static final String screenDimensionStorage = "screenDimension.xml";
    private static final String fileChooserPathStorage = "fileChooserPath.xml";
    private static final String keyCodeStorage = "keyCode.xml";
    private static final String graphicsCapabilityStorage = "graphicsCapability.xml";
    /**
     * Currency format to use for cost information. Always USD.
     */
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    /**
     * One component - the button to close the member table - that can't be
     * created in the GUI builder due to the Layout object required.
     */
    private JButton closeMemberTableButton;

    /**
     * Construct the Bridge Designer main window view.
     * 
     * @param app Bridge Designer application object.
     */
    public BDView(SingleFrameApplication app) {
        super(app);

        preinitComponents();
        initComponents();
        postInitComponents();
    }

    /**
     * ---------------
     * Nested classes
     * ---------------
     */
    
    /**
     * Title block overlay for drafting panel.  We manage this as a modeless dialog.
     */
    private class TitleBlockPanel extends JPanel {

        static final int margin = 4;
        static final int spacing = 2;

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, w - 1, h - 1);
            g.drawRect(margin, margin, w - 2 * margin - 1, h - 2 * margin - 1);
            int y = margin + spacing + titleLabel.getHeight() + spacing;
            g.drawLine(margin, y, w - margin - 1, y);
            y += 1 + spacing + designedByField.getHeight() + spacing;
            g.drawLine(margin, y, w - margin - 1, y);
        }
    }

    /**
     * -------------------------
     * Initialization sequences
     * -------------------------
     */
    void preinitComponents() {
        Help.initialize();
        dispatcher = new Dispatcher();
        bridge = new EditableBridgeModel();
        bridgeDraftingView = new BridgeDraftingView(bridge);
        // For OpenGL canvas compatibility, ensure heavyweight menus are used.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        
        // Allocate the animations depending on legacy graphics flag.
        fixedEyeAnimation = FixedEyeAnimation.create(getFrame(), bridge);
        animation = flyThruAnimation =
                BDApp.isLegacyGraphics() ?
                    fixedEyeAnimation :
                    FlyThruAnimation.create(getFrame(), bridge);
     }
    
    private void postInitComponents() {
        // Set the close button up "by hand" because Netbeans does not know about OverlayLayout.
        closeMemberTableButton = new javax.swing.JButton();
        
        ActionMap actionMap = BDApp.getApplication().getContext().getActionMap(BDView.class, this);
        closeMemberTableButton.setAction(actionMap.get("closeMemberTable")); // NOI18N
        closeMemberTableButton.setAlignmentX(1.0F);
        closeMemberTableButton.setAlignmentY(0.0F);
        closeMemberTableButton.setHideActionText(true);
        closeMemberTableButton.setFocusable(false);
        closeMemberTableButton.setMargin(new Insets(0, 0, 0, 0));
        closeMemberTableButton.setName("closeMemberTableButton"); // NOI18N
        memberPanel.setLayout(new OverlayLayout(memberPanel));
        memberPanel.add(closeMemberTableButton);
        memberPanel.add(memberTabs);
        
        if (!Debug.isPropertyDefined("wpbd.develop", false)) {
            saveAsSample.setVisible(false);
            saveAsTemplate.setVisible(false);
            printLoadedClassesMenuItem.setVisible(false);
        }

        // Build our file chooser with preview box.  Fiddle with size to account for preview window.
        fileChooser = new JFileChooser();
        Dimension fileChooserSize = fileChooser.getPreferredSize();
        PreviewAccessory preview = new PreviewAccessory();
        if (BDApp.isEnhancedMacUI()) {
            fileChooser.putClientProperty("Quaqua.FileChooser.preview", preview);
        }
        else {
            fileChooser.setAccessory(preview);
        }
        fileChooser.addPropertyChangeListener(preview);
        Dimension accessorySize = preview.getPreferredSize();
        Dimension maxSize = Toolkit.getDefaultToolkit().getScreenSize();
        fileChooserSize.width = Math.min(maxSize.width, fileChooserSize.width + accessorySize.width);
        fileChooser.setPreferredSize(fileChooserSize);
        fileChooser.addChoosableFileFilter(new BDCFileFilter());
        restoreFileChooserState();
        
        // Build stock selector and connect it to the drafting panel.  
        stockSelector = new StockSelector(materialBox, sectionBox, sizeBox);
        draftingPanel.setMemberStockSelector(stockSelector);
        stockSelector.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!bridge.isSelectedMember()) {
                    // If there are no members selected, the size buttons are stepping the selector size.
                    // This ensures the correct size button is disabled when we reach max or min size.
                    setSizeButtonsEnabled();
                }
            }
        });

        // Add listener that unselects the animation controls toggle if the user closes manually.
        animation.getControls().getDialog().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                setSelected(toggleAnimationControlsMenuItem, false);
            }
        });
        
        // Add a listener that unselects the tools dialog toggle if the user closes manually.
        toolsDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                setSelected(toggleToolsMenuItem, false);
            }
        });
        
        // Add a listener that works around the well-known Canvas resize bug.
        cardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                animation.applyCanvasResizeBugWorkaround();
            }
        });

        popupStockSelector = new StockSelector(memberPopupMaterialBox, memberPopupSectionBox, memberPopupSizeBox);
        // Build the member detail object and attach its delegates.  Allow it to listen for various changes.
        memberDetail = new MemberDetail(
                bridge,
                stockSelector,
                memberDetailTabs, 
                memberDetailPanel,
                materialPropertiesTable, 
                dimensionsTable, 
                memberCostTable, 
                crossSectionSketchLabel, 
                memberSelectBox,
                graphAllCheck,
                strengthCurveLabel);
        bridge.addIterationChangeListener(memberDetail);
        graphAllCheck.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                memberDetail.handleShowAllStateChange(e);
            }
        });
        memberSelectBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                memberDetail.handleMemberSelectChange(e);
            }
        });
        dispatcher.initialize(bridge, memberTable, memberDetail, stockSelector, popupStockSelector, draftingPanel);
        // Set the selected key in each action involving a toggle button or menu item.  This causes
        // the action mechanism to update them automatically.  The NetBeans IDE builder ought to do
        // this, but it doesn't.
        // Simple toggle buttons.
        setSelected(toggleTitleBlockMenuItem, true);
        setSelected(toggleMemberNumbersMenuItem, false);
        setSelected(toggleRulerMenuItem, true);
        setSelected(toggleToolsMenuItem, true);
        setSelected(toggleAnimationControlsMenuItem, true);
        setSelected(toggleLegacyGraphicsMenuItem, setDefaultGraphics());
        setSelected(toggleGuidesMenuItem, false);
        setSelected(toggleTemplateMenuItem, true);
        // Set grid button group.
        setSelected(setCoarseGridButton, true);
        setSelected(setMediumGridButton, false);
        setSelected(setFineGridButton, false);
        // Design/test button group.
        setSelected(drawingBoardButton, true);
        setSelected(loadTestButton, false);
        // Edit button group.
        setSelected(editJointsMenuItem, true);
        setSelected(editMembersMenuItem, false);
        setSelected(editSelectMenuItem, false);
        setSelected(editEraseMenuItem, false);
        // Show animation flag
        setSelected(toggleAnimationMenuItem, true);
        setSelected(toggleAutoCorrectMenuItem, true);
        
        undoButton.getAction().setEnabled(false);
        redoButton.getAction().setEnabled(false);
        saveButton.getAction().setEnabled(false);
        deleteButton.getAction().setEnabled(false);
        loadTestButton.getAction().setEnabled(false);
        costReportButton.getAction().setEnabled(false);
        showGoToIterationButton.getAction().setEnabled(false);
        loadTestReportButton.getAction().setEnabled(false);
        back1iterationButton.getAction().setEnabled(false);
        forward1iterationButton.getAction().setEnabled(false);
        toggleTemplateButton.getAction().setEnabled(false);

        // Set up setEnabled/disable management.
        enabledStateManager = new EnabledStateManager(3);
        // These are indexed by ..._STATE values above.
        boolean [] draftingOnly = { false, true, false };
        boolean [] animationOnly = { false, false, true };
        boolean [] draftingOrAnimation = { false, true, true };
        enabledStateManager.add(toggleMemberListButton,          draftingOnly);
        enabledStateManager.add(loadTemplateMenuItem,            draftingOnly);
        enabledStateManager.add(saveAsMenuItem,                  draftingOrAnimation);
        enabledStateManager.add(printButton,                     draftingOnly);
        enabledStateManager.add(printMenuItem,                   draftingOnly);
        enabledStateManager.add(selectAllButton,                 draftingOnly);
        enabledStateManager.add(deleteButton,                    draftingOnly);
        enabledStateManager.add(undoButton,                      draftingOnly);
        enabledStateManager.add(undoDropButton,                  draftingOnly);
        enabledStateManager.add(redoButton,                      draftingOnly);
        enabledStateManager.add(redoDropButton,                  draftingOnly);
        enabledStateManager.add(back1iterationButton,            draftingOnly);
        enabledStateManager.add(forward1iterationButton,         draftingOnly);
        enabledStateManager.add(showGoToIterationButton,         draftingOnly);
        enabledStateManager.add(toggleToolsMenuItem,             draftingOnly);
        enabledStateManager.add(toggleRulerMenuItem,             draftingOnly);
        enabledStateManager.add(toggleTitleBlockMenuItem,        draftingOnly);
        enabledStateManager.add(toggleMemberNumbersButton,       draftingOnly);
        enabledStateManager.add(toggleTemplateButton,            draftingOnly);
        enabledStateManager.add(toggleGuidesButton,              draftingOnly);        
        enabledStateManager.add(coarseGridMenuItem,              draftingOnly);
        enabledStateManager.add(mediumGridMenuItem,              draftingOnly);
        enabledStateManager.add(fineGridMenuItem,                draftingOnly);
        enabledStateManager.add(toolsMenu,                       draftingOnly);
        enabledStateManager.add(drawingBoardButton,              draftingOrAnimation);
        enabledStateManager.add(loadTestButton,                  draftingOrAnimation);
        enabledStateManager.add(toggleAnimationControlsMenuItem, animationOnly);
        enabledStateManager.add(toggleAnimationMenuItem,         draftingOnly);
        enabledStateManager.add(toggleAutoCorrectMenuItem,       draftingOnly);
        enabledStateManager.add(costReportButton,                draftingOrAnimation);
        enabledStateManager.add(loadTestReportButton,            draftingOrAnimation);
        enabledStateManager.add(increaseMemberSizeButton,        draftingOnly);
        enabledStateManager.add(decreaseMemberSizeButton,        draftingOnly);
        enabledStateManager.add(materialBox,                     draftingOnly);        
        enabledStateManager.add(sectionBox,                      draftingOnly);        
        enabledStateManager.add(sizeBox,                         draftingOnly);

        // If we're in legacy graphics mode, disable the toggle in the on position.
        if (BDApp.isLegacyGraphics()) {
            setSelected(toggleLegacyGraphicsMenuItem, true);
            toggleLegacyGraphicsMenuItem.getAction().setEnabled(false);
        }
        else {
            // Else it's subject to drafting mode enabling.
            enabledStateManager.add(toggleLegacyGraphicsMenuItem, draftingOnly);
        }

        // Connect the undo/redo button enabled status to the undo manager state.
        bridge.getUndoManager().addUndoableAfterEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent e) {
                ExtendedUndoManager undoManager = bridge.getUndoManager();
                enabledStateManager.setEnabled(undoButton, undoManager.canUndo());
                enabledStateManager.setEnabled(undoDropButton, undoManager.canUndo());
                enabledStateManager.setEnabled(redoButton, undoManager.canRedo());
                enabledStateManager.setEnabled(redoDropButton, undoManager.canRedo());
                undoButton.setToolTipText(undoManager.getUndoPresentationName());
                redoButton.setToolTipText(undoManager.getRedoPresentationName());
                enabledStateManager.setEnabled(saveButton, undoManager.isDirty() || !undoManager.isStored());        
            }
        });
        
        bridge.addAnalysisChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                enabledStateManager.setEnabled(loadTestReportButton, bridge.isAnalysisValid());
                setLoadTestButtonEnabled();
                setStatusIcon();
                // Do this here for the initial enable status.
                setSizeButtonsEnabled();
            }
        });
 
        bridge.addIterationChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                enabledStateManager.setEnabled(forward1iterationButton, bridge.canLoadNextIteration(+1));
                enabledStateManager.setEnabled(back1iterationButton, bridge.canLoadNextIteration(-1));
                enabledStateManager.setEnabled(showGoToIterationButton, bridge.canGotoIteration());
                setIterationLabel();
            }
        });
        
        bridge.addStructureChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                enabledStateManager.setEnabled(loadTestReportButton, bridge.isAnalysisValid());
                costLabel.setText(currencyFormatter.format(bridge.getTotalCost()));
                setLoadTestButtonEnabled();
                setStatusIcon();
                setSizeButtonsEnabled();
            }
        });
        
        bridge.addSelectionChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (bridge.getSelectedJoint() == null) {
                     draftingPanel.eraseCrosshairs();
                }
                enabledStateManager.setEnabled(deleteButton, bridge.isSelection());
                setSizeButtonsEnabled();
            }
        });

        // Listen for key code sequence no matter where focus is.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        
        // Make exit menu item invisible on Mac.
        if (BDApp.getOS() == BDApp.MAC_OS_X) {
            exitMenuItem.setVisible(false);
        }
        
        recentFileManager = new RecentFileManager(5, this);
        addRecentFilesToFileMenu();
        
        checkHardware();
    }
    
    private void addRecentFilesToFileMenu() {
        recentFileManager.addRecentFileMenuItemsAtSep(fileMenu, 4, fileChooser.getCurrentDirectory());        
    }
    
    private void print(boolean dialog) {
        final String fileName = bridge.getUndoManager().isStored() ? fileChooser.getSelectedFile().getAbsolutePath() : null;
        PrinterUI.print(getFrame(), fileName, bridge, BDView.class, dialog);        
    }
    
    private class BDCFileFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String name = f.getPath();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex < 0) {
                return false;
            }
            return name.substring(dotIndex + 1).equalsIgnoreCase("bdc");
        }

        @Override
        public String getDescription() {
            return getResourceMap().getString("fileDescription.text"); 
        }
    }

    /**
     * ---------------------------------
     * Helper and convenience functions
     * ---------------------------------
     */
    
    /**
     * Return true iff there is a modal dialog visible.
     * 
     * @return true iff there is a modal dialog visible.
     */
    public static boolean isModalDialogVisible() {
	Frame [] frames = JFrame.getFrames();
	for (int i = 0; i < frames.length; i++) {
	   Window [] windows = frames[i].getOwnedWindows();
	   for (int j = 0; j < windows.length; j++) {
		if (windows[j].isVisible() && windows[j] instanceof JDialog && ((JDialog)windows[j]).isModal()) {
                    return true;
		}
	   }
	}
        return false;
    }

    /**
     * Convenience function to return the selected attribute of the action of a given button.
     * 
     * @param button button to check
     * @return true iff selected
     */
    public static boolean isSelected(AbstractButton button) {
        return button.getAction().getValue(javax.swing.Action.SELECTED_KEY) == Boolean.TRUE;
    }
    
    /**
     * Convenience function to set the selected attribute of the action of a given button.
     * 
     * @param button button whose action selected attribute should be set
     * @param val value to give the attribute
     */
    public static void setSelected(AbstractButton button, boolean val) {
        button.getAction().putValue(javax.swing.Action.SELECTED_KEY, Boolean.valueOf(val));
    }

    /**
     * Intercept certain keystrokes with special meanings throughout the application.
     * 
     * @param e key event
     * @return true iff we've handled the keystroke here, and it needs no further action
     */
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            final int code = e.getKeyCode();
            // Handle escape key combination that allows entering key code design conditions.
            if (e.isControlDown() && e.isShiftDown() && (code == KeyEvent.VK_HOME || code == KeyEvent.VK_UP) && !isModalDialogVisible()) {
                // Added UP to key sequence for Macintosh, which doesn't have a Home key.
                keyCodeDialog.setLocationRelativeTo(BDApp.getFrame());
                final String keyCode = (String)BDApp.loadFromLocalStorage(keyCodeStorage);
                keyCodeTextField.setText(keyCode);
                keyCodeErrorLabel.setVisible(false);
                keyCodeDialog.getRootPane().setDefaultButton(keyCodeOkButton);
                bridge.clearSelectedJoint(true);
                keyCodeDialog.setVisible(true);
                keyCodeTextField.requestFocusInWindow();
                return true;
            }
            // If a joint is selected in the bridge and an arrow key is typed, redispatch to the drafting panel.
            // If the user types Escape, clear the selection so the user can get the arrows back.
            if (bridge.getSelectedJoint() != null) {
                switch (code) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_RIGHT:
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(draftingPanel, e);
                        return true;
                    case KeyEvent.VK_ESCAPE:
                        bridge.clearSelectedJoint(true);
                        draftingPanel.eraseCrosshairs();
                        return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Load the file chooser state from local storage.
     */
    private void restoreFileChooserState() {
        final String path = (String) BDApp.loadFromLocalStorage(fileChooserPathStorage);
        if (path != null) {
            fileChooser.setCurrentDirectory(new File(path));
        }        
    }
    
    /**
     * Enforce predicates that enable or disable the material stock size buttons.
     */
    private void setSizeButtonsEnabled() {
        int mask = bridge.isSelectedMember() ? bridge.getAllowedShapeChanges() : stockSelector.getAllowedShapeChanges();
        increaseMemberSizeButton.setEnabled((mask & Inventory.SHAPE_INCREASE_SIZE) != 0);
        decreaseMemberSizeButton.setEnabled((mask & Inventory.SHAPE_DECREASE_SIZE) != 0);
    }
 
    /**
     * Enforce predicates that enable or disable the load test button.
     */
    private void setLoadTestButtonEnabled() {
        enabledStateManager.setEnabled(
                loadTestButton, 
                autofixEnabled() || (bridge.isAnalyzable() && (animationEnabled() || !bridge.isAnalysisValid())));
    }
    
    /**
     * Set the status icon based on the analysis status.
     */
    private void setStatusIcon() {
        String iconName, tipKey;

        if (!bridge.isAnalysisValid()) {
            iconName = "working.png";
            tipKey = "workingTip.text";
        }
        else if(!bridge.isPassing()) {
            iconName = "bad.png";
            tipKey = "badTip.text";
        }
        else {
            iconName = "good.png";
            tipKey = "goodTip.text";
        }
        statusLabel.setIcon(BDApp.getApplication().getIconResource(iconName));
        statusLabel.setToolTipText(getResourceMap().getString(tipKey));
    }
    
    /**
     * Update the iteration count in the toolbar.
     */
    private void setIterationLabel() {
        iterationNumberLabel.setText(" " + Integer.toString(bridge.getIteration()));
    }
    
    /**
     * Do initializations that only work after the main frame is shown.
     */
    public void initComponentsPostShow() {
        BDApp app = BDApp.getApplication();
        ArrayList<Image> icons = new ArrayList<Image>();
        icons.add(app.getImageResource("appicon.png"));
        icons.add(app.getImageResource("appicon32.png"));
        JFrame mainFrame = app.getMainFrame();
        mainFrame.setIconImages(icons);

        // Build this dialog in advance because it takes a while.
        setupWizard = new SetupWizard(mainFrame);
        setupWizard.setLocationRelativeTo(mainFrame);
        tipDialog = new TipDialog(mainFrame);
        tipDialog.setLocationRelativeTo(mainFrame);
        if (BDApp.getFileName() == null) {
            tipDialog.showTip(true, 1);
            showWelcomeDialog();
        }
        else {
            try {
                File file = new File(BDApp.getFileName());
                bridge.read(file);
                // Do this after read in case it fails.
                fileChooser.setSelectedFile(file);
                initializePostBridgeLoad();
            } catch (IOException e) {
                selectCard(nullPanelCard);
                showReadFailedMessage(e);
            }            
        }
        animation.applyCanvasResizeBugWorkaround();
    }

    /**
     * Copy title block information from bridge to the view and do other updating of the veiw.
     */
    private void uploadBridgeToDraftingPanel() {
        designedByField.setText(bridge.getDesignedBy());
        String pid = bridge.getProjectId();
        int dashIndex = pid.indexOf('-');
        scenarioIDLabel.setText(pid.substring(0, dashIndex + 1));
        projectIDField.setText(pid.substring(dashIndex + 1));
        memberTable.fireTableDataChanged();
        // Reset the viewport transform, rulers, symmetry guides, 
        // paint backing store, and repaint the drafting panel.
        draftingPanel.setViewport(true);
        // Repaint rulers with new transform.
        horizontalRuler.repaint();
        verticalRuler.repaint();
    }

    /**
     * Copy title block information from the drafting panel to the bridge.  Must be done before saving.
     */
    private void downloadBridgeFromDraftingPanel() {
        bridge.setDesignedBy(designedByField.getText());
        bridge.setProjectId(scenarioIDLabel.getText() + projectIDField.getText());
    }

    /**
     * Update the window title using the file chooser's selected file.
     */
    private void setTitleFileName() {
        String title = getFrame().getTitle();
        int dashIndex = title.indexOf('-');
        if (dashIndex >= 0) {
            getFrame().setTitle(title.substring(0, dashIndex + 2) + fileChooser.getSelectedFile());
        } else {
            getFrame().setTitle(title + " - " + fileChooser.getSelectedFile());
        }
    }
    
    /**
     * Set the file chooser's selecte file to a default name and then update the window title.
     */
    private void setDefaultFile() {
        fileChooser.setSelectedFile(getDefaultFile());
        setTitleFileName();
    }

    /**
     * Ask if the current bridge should be saved if it's been edited since
     * last save.  Return true if it should be saved, including user agreement
     * that it should.
     * 
     * @return true iff the bridge should be saved
     */
    public boolean querySaveIfDirty() {
        // Prevent the user from losing changes.
        if (bridge.getUndoManager().isDirty()) {
            int yesNoCancel = JOptionPane.showConfirmDialog(getFrame(), 
                    getResourceMap().getString("saveDialog.text"),
                    getResourceMap().getString("saveDialog.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            switch (yesNoCancel) {
                case JOptionPane.YES_OPTION:
                    save();
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return false;
            }
        }
        return true;
    }

    /**
     * See if we have enough hardware to run.  Show warnings if not.  Only do it once.
     */
    private void checkHardware() {
        final Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();

        // Get any previously stored dimension and then save the new one.
        final Dimension storedScreenDim = (Dimension) BDApp.loadFromLocalStorage(screenDimensionStorage);
        BDApp.saveToLocalStorage(screenDim, screenDimensionStorage);
        
        // Only show a warning dialog if it hasn't been shown before at this screen dimension.
        if ((storedScreenDim == null || !storedScreenDim.equals(screenDim)) && 
                (screenDim.width < 1200 || screenDim.height < 900)) {
            int yesNo = JOptionPane.showConfirmDialog(getFrame(), 
                    getResourceMap().getString("hardwareWarning.text", screenDim.width, screenDim.height), 
                    getResourceMap().getString("hardwareWarning.title"), 
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (yesNo == JOptionPane.NO_OPTION) {
                BDApp.getApplication().exit();
            }
        }
    }
    
    /**
     * Do all the GUI bookkeeping needed after a new bridge is loaded (from file, iteration, or setup wizard).
     */
    private void initializePostBridgeLoad() {
        // Detach any template and disable the toggle template button.
        setSketchModel(null);
        // Set the grid finer if needed to edit this bridge.
        setGridFiner(DraftingGrid.toDensity(bridge.getSnapMultiple()));
        // Load the most common stock of the bridge into the seletor or a default if no members.
        stockSelector.setMostCommonStockOf(bridge);
        // Initialize the drafting view.
        bridgeDraftingView.initialize(bridge.getDesignConditions());
        // Copy title block and other data to the drafting panel.
        uploadBridgeToDraftingPanel();
        // Show the drafting board panel.
        showDrawingBoard();
        // Set the window title to match the file chooser selection.
        setTitleFileName();
        // Virtually press the edit joints button.
        setSelected(editJointsMenuItem, true);
        editJoints();
    }

    private void recordRecentFileUse(File file) {
        recentFileManager.addNew(file);
        recentFileManager.save();
        addRecentFilesToFileMenu();
    }
    
    private void recordRecentFileUse() {
        recordRecentFileUse(fileChooser.getSelectedFile());
    }
    
    /** 
     * Verify that it's okay to write a file, verifying with the user that overwriting, if it's about
     * to occur, is permissible.
     * 
     * @param f file to check
     * @param title title for verification dialog
     * @return true iff it's okay to write the file
     */
    private boolean overwriteOk(File f, String title) {
        if (f.exists()) {
            int yesNo = JOptionPane.showConfirmDialog(getFrame(),
                    getResourceMap().getString("pathExistsMessage.text", f.getPath()),
                    title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return (yesNo == JOptionPane.YES_OPTION);
        }
        return true;
    }

    /**
     * If the given file chooser has the bdc file filter selected, append .bdc to the selected
     * file name if there isn't already a suffix.
     * 
     * @param fileChooser
     */
    private static void appendDefaultSuffix(JFileChooser fileChooser) {
        if (fileChooser.getFileFilter() instanceof BDCFileFilter) {
            try {
                String path = fileChooser.getSelectedFile().getCanonicalPath();
                if (path.indexOf('.') < 0) {
                    fileChooser.setSelectedFile(new File(path + ".bdc"));
                }
            } catch (IOException ex) {
                return;
            }
        }
    }

    private void setDesignMenuItemEnabled(boolean b) {
        enabledStateManager.setEnabled(toggleToolsMenuItem, b);
        enabledStateManager.setEnabled(toggleAnimationControlsMenuItem, !b);
    }

    // Shut down the view in the case where the user is closing the
    // program through the host system.
    public void shutdown() {
        // Record recently used files.
        recordRecentFileUse();
        animation.stop();
        animation.getControls().saveState();
    }

    /**
     * Select a specified card for the drafting panel space.
     * 
     * @param name one of designPanelCardName, animationPanelCardName, or nullPanelCardName
     */
    private void selectCard(String name) {
        // Don't do anything if we're already showing the right card.
        if (selectedCard != null && selectedCard.equals(name)) {
            return;
        }
        // Hide floating tools if necessary.'
        if (animationPanelCardName.equals(selectedCard)) {
            animation.getControls().saveVisibilityAndHide();
        }
        if (designPanelCardName.equals(selectedCard) && toolsDialogInitialized) {
            toolsDialogVisibleState = toolsDialog.isVisible();
            bridge.clearSelectedJoint(true);
            toolsDialog.setVisible(false);            
        }
        // Show the new card.
        CardLayout cl = (CardLayout) cardPanel.getLayout();
        cl.show(cardPanel, name);
        selectedCard = name;
        // Initialize the card that's now showing.
        if (designPanelCardName.equals(name)) {
            // Make sure the animation is off.
            animation.stop();
            // Set enabled state to match card.
            enabledStateManager.setGUIState(DRAFTING_STATE);
            // Initalize the tools dialog if not aready done.
            if (!toolsDialogInitialized) {
                toolsDialog.pack();
                toolsDialog.setLocation(draftingPanel.getReasonableDrawingToolsLocation());
                toolsDialogVisibleState = true;
                toolsDialogInitialized = true;
            }
            // Flip the selector buttons to match this card (for programmatic selections).
            setDesignMenuItemEnabled(true);
            enabledStateManager.setEnabled(costReportButton, true);
            setSelected(drawingBoardButton, true);
            draftingPanel.requestFocusInWindow();
            
            // Make visible later so that drafting panel is fully initialized first.            
            if (toolsDialogVisibleState) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        bridge.clearSelectedJoint(true);
                        toolsDialog.setVisible(true);
                        setSelected(toggleToolsMenuItem, true);
                    }
                });
            }
        } else if (animationPanelCardName.equals(name)) {
            // Set GUI enabled state to match card.
            enabledStateManager.setGUIState(ANIMATING_STATE);
            // Flip the selector buttons to match this card (for programmatic selections).
            setDesignMenuItemEnabled(false);
            setSelected(loadTestButton, true);
            animation.getControls().startAnimation();
            setSelected(toggleAnimationControlsMenuItem, animation.getControls().getVisibleState());
        }
        else {
            // Switch to empty card.
            enabledStateManager.setGUIState(NO_BRIDGE_STATE);
            setDefaultFile();
            bridge.getUndoManager().newSession();
        }
    }

    /**
     * Return true iff the autofix menu check is selected.
     * 
     * @return true iff the autofix menu check is selected
     */
    private boolean autofixEnabled() {
        return isSelected(toggleAutoCorrectMenuItem);
    }
    
    /**
     * Return true iff the animation toggle button / menu item is pressed.
     * 
     * @return true iff the animation toggle button / menu item is pressed
     */
    private boolean animationEnabled() {
        return isSelected(toggleAnimationMenuItem);
    }
    
    /**
     * Make the member list/member detail panel visible.
     * 
     * @param val whether to make the panel visible
     */
    private void selectMemberList(boolean val) {
        setSelected(toggleMemberListButton, val);
        openMemberTableButton.setVisible(!val);
        memberPanel.setVisible(val);
    }

    /**
     * Set the template attached to the drafting view.
     * 
     * @param designTemplate template to attach to the bridge's drafting view
     */
    private void setSketchModel(BridgeSketchModel designTemplate) {
        bridgeDraftingView.getBridgeSketchView().setModel(designTemplate);
        if (designTemplate == null) {
            enabledStateManager.setEnabled(toggleTemplateButton, false);
        }
        else {
            enabledStateManager.setEnabled(toggleTemplateButton, true);
            setSelected(toggleTemplateButton , true);
            toggleTemplate(); // setting action selected doesn't call action handler!
            setGridFiner(DraftingGrid.toDensity(designTemplate.getSnapMultiple()));
        }
    }
    
    private File getDefaultFile() {
        return new File("MyDesign.bdc");
    }
    
    private void showSetupWizard() {
        if (bridge.isInitialized()) {
            setupWizard.initialize(bridge.getDesignConditions(),
                    bridge.getProjectId(), bridge.getDesignedBy(), 
                    bridgeDraftingView.getBridgeSketchView().getModel());
        }
        if (animationPanelCardName.equals(selectedCard)) {
            selectCard(designPanelCardName);
        }
        bridge.clearSelectedJoint(true);
        setupWizard.setVisible(true);
        if (setupWizard.isOk()) {
            recordRecentFileUse();
            bridge.initialize(setupWizard.getDesignConditions(), setupWizard.getProjectId(), setupWizard.getDesignedBy());
            bridgeDraftingView.initialize(setupWizard.getDesignConditions());
            setSketchModel(setupWizard.getSketchModel());
            showDrawingBoard();
            uploadBridgeToDraftingPanel();
            setDefaultFile();
            setLoadTestButtonEnabled();
            // Virtually press the edit joints button.
            setSelected(editJointsMenuItem, true);
            editJoints();
        }
    }

    private void restartWithCurrentConditions() {
        recordRecentFileUse();
        bridge.initialize(bridge.getDesignConditions(), null, null);
        setSketchModel(null);
        showDrawingBoard();
        uploadBridgeToDraftingPanel();
        setDefaultFile();
        setLoadTestButtonEnabled();
        setSelected(editJointsMenuItem, true);
        editJoints();
    }

    private void setGrid(int density) {
        draftingPanel.getDraftingCoordinates().setDensity(density);
        horizontalRuler.repaint();
        verticalRuler.repaint();
    }

    /**
     * Adjust the drafting grid density finer if necessary.  This is used
     * to match the density necessary to hit all the points in a template or sample.
     * 
     * @param density target grid density from DraftingGrid.xxx_GRID
     */
    private void setGridFiner(int density) {
        if (draftingPanel.getDraftingCoordinates().isFiner(density)) {
            setGrid(density);
            switch (density) {
                case DraftingGrid.COARSE_GRID:
                    setSelected(setCoarseGridButton, true);
                    break;
                case DraftingGrid.MEDIUM_GRID:
                    setSelected(setMediumGridButton, true);
                    break;
                case DraftingGrid.FINE_GRID:
                    setSelected(setFineGridButton, true);
                    break;
            }
        }
    }

    /**
     * Helper routine to turn rulers on and off.
     * 
     * @param val whether rulers should be turned on
     */
    private void selectRulers(boolean val) {
        setSelected(toggleRulerMenuItem, val);
        verticalRuler.setVisible(val);
        corner.setVisible(val);
        horizontalRuler.setVisible(val);
    }
    
    /**
     * Show the welcome dialog and handle the result.
     */
    private void showWelcomeDialog() {
        if (welcomeDialog == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            welcomeDialog = new WelcomeDialog(mainFrame, this);
            welcomeDialog.setLocationRelativeTo(mainFrame);
        }
        welcomeDialog.setVisible(true);
        switch (welcomeDialog.getResult()) {
            case WelcomeDialog.CREATE_NEW:
                newDesign();
                break;
            case WelcomeDialog.LOAD_SAMPLE:
                loadSampleDesign();
                break;
            case WelcomeDialog.LOAD:
                open();
                break;
            case WelcomeDialog.CANCEL:
                selectCard(nullPanelCard);
                break;
        }
    }

    /**
     * Show the right-click context menu for the member table panel at the given location.
     * 
     * @param x pixel x-coorinate for upper left corner of popup
     * @param y pixel y-coorinate for upper left corner of popup
     */
    public void showMemberEditPopup(int x, int y) {
        memberEditPopup.getRootPane().setDefaultButton(memberPopupDoneButton);
        memberEditPopup.pack();
        memberEditPopup.setLocation(x, y);
        popupStockSelector.matchSelection(bridge);
        bridge.clearSelectedJoint(true);
        memberEditPopup.setVisible(true);
    }

    /**
     * Show the right-click context menu for the drafting panel at the given location.
     * 
     * @param x pixel x-coorinate for upper left corner of popup
     * @param y pixel y-coorinate for upper left corner of popup
     */
    public void showDraftingPopup(int x, int y) {
        draftingPopup.show(draftingJPanel, x, y);
    }

    /**
     * Show a message that says something went wrong during a bridge file read operation.
     * 
     * @param e exception describing what went wrong
     */
    private void showReadFailedMessage(Exception e) {
        showMessageDialog(getResourceMap().getString("readFailedMessage.text", e.getMessage()));
    }

    /**
     * Show an information-type message dialog with a standard title.
     *  
     * @param msg message
     */
    private void showMessageDialog(String msg) {
        JOptionPane.showMessageDialog(getFrame(),
                msg,
                getResourceMap().getString("messageDialog.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Listener function for the recent file manager.
     * 
     * @param file recenly used file to open
     */
    public void openRecentFile(File file) {
        if (querySaveIfDirty()) {
            animation.stop();
            try {
                bridge.read(file);
                fileChooser.setSelectedFile(file);
                initializePostBridgeLoad();
            } catch (IOException e) {
                selectCard(nullPanelCard);
                showReadFailedMessage(e);
            } finally {
                // reload menu to reflect new order
                addRecentFilesToFileMenu();
            }
        }
    }
    
    /**
     * Provide a way for the welcome dialog to show the about box.
     */
    public void showAbout() {
        about();
    }

    /**
     * Show the contest reminder dialog.
     */
    public void showContestReminderDialog() {
        if (contestReminderDialog == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            contestReminderDialog = new ContestReminderDialog(mainFrame);
            contestReminderDialog.setLocationRelativeTo(mainFrame);
        }
        contestReminderDialog.setVisible(true);
    }
    
    /**
     * ------------------------------------
     * Event handlers generated by the GUI
     * ------------------------------------
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newDesignMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator1 = new javax.swing.JSeparator();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator2 = new javax.swing.JSeparator();
        openSampleDesignMenuItem = new javax.swing.JMenuItem();
        loadTemplateMenuItem = new javax.swing.JMenuItem();
        saveAsSample = new javax.swing.JMenuItem();
        saveAsTemplate = new javax.swing.JMenuItem();
        printLoadedClassesMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator3 = new javax.swing.JSeparator();
        printMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator4 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        selectallItem = new javax.swing.JMenuItem();
        deleteItem = new javax.swing.JMenuItem();
        editMenuSeparator1 = new javax.swing.JSeparator();
        undoItem = new javax.swing.JMenuItem();
        redoItem = new javax.swing.JMenuItem();
        editMenuSeparator2 = new javax.swing.JSeparator();
        back1iterationItem = new javax.swing.JMenuItem();
        forward1iterationItem = new javax.swing.JMenuItem();
        gotoIterationItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        toggleToolsMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleAnimationControlsMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleMemberListMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleRulerMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleTitleBlockMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleMemberNumbersMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleGuidesMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleTemplateMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewSeparator1 = new javax.swing.JSeparator();
        coarseGridMenuItem = new javax.swing.JRadioButtonMenuItem();
        mediumGridMenuItem = new javax.swing.JRadioButtonMenuItem();
        fineGridMenuItem = new javax.swing.JRadioButtonMenuItem();
        toolsMenu = new javax.swing.JMenu();
        editJointsMenuItem = new javax.swing.JRadioButtonMenuItem();
        editMembersMenuItem = new javax.swing.JRadioButtonMenuItem();
        editSelectMenuItem = new javax.swing.JRadioButtonMenuItem();
        editEraseMenuItem = new javax.swing.JRadioButtonMenuItem();
        testMenu = new javax.swing.JMenu();
        drawingBoardMenuItem = new javax.swing.JRadioButtonMenuItem();
        loadTestMenuItem = new javax.swing.JRadioButtonMenuItem();
        testMenuSep01 = new javax.swing.JSeparator();
        toggleAnimationMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleLegacyGraphicsMenuItem = new javax.swing.JCheckBoxMenuItem();
        testMenuSep02 = new javax.swing.JPopupMenu.Separator();
        toggleAutoCorrectMenuItem = new javax.swing.JCheckBoxMenuItem();
        reportMenu = new javax.swing.JMenu();
        costReportMenuItem = new javax.swing.JMenuItem();
        loadTestReportMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        howToDesignMenuItem = new javax.swing.JMenuItem();
        bridgeDesignWindowMenuItem = new javax.swing.JMenuItem();
        helpSeparator01 = new javax.swing.JSeparator();
        helpTopicsMenuItem = new javax.swing.JMenuItem();
        searchForHelpMenuItem = new javax.swing.JMenuItem();
        helpSeparator02 = new javax.swing.JSeparator();
        tipOfTheDayMenuItem = new javax.swing.JMenuItem();
        browseOurWebSiteMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        mainPanel = new javax.swing.JPanel();
        topToolBar = new javax.swing.JToolBar();
        spacer3 = new javax.swing.JLabel();
        newButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        separator6 = new javax.swing.JToolBar.Separator();
        drawingBoardButton = new javax.swing.JToggleButton();
        loadTestButton = new javax.swing.JToggleButton();
        separator3 = new javax.swing.JToolBar.Separator();
        selectAllButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        separator1 = new javax.swing.JToolBar.Separator();
        undoButton = new javax.swing.JButton();
        undoDropButton = UndoRedoDropButton.getUndoDropButton(bridge.getUndoManager());
        spacer6 = new javax.swing.JLabel();
        redoButton = new javax.swing.JButton();
        redoDropButton = UndoRedoDropButton.getRedoDropButton(bridge.getUndoManager());
        separator2 = new javax.swing.JToolBar.Separator();
        iterationLabel = new javax.swing.JLabel();
        iterationNumberLabel = new javax.swing.JLabel();
        back1iterationButton = new javax.swing.JButton();
        forward1iterationButton = new javax.swing.JButton();
        showGoToIterationButton = new javax.swing.JButton();
        separator8 = new javax.swing.JToolBar.Separator();
        costLabel = new javax.swing.JLabel();
        spacer7 = new javax.swing.JLabel();
        costReportButton = new javax.swing.JButton();
        separator4 = new javax.swing.JToolBar.Separator();
        statusLabel = new javax.swing.JLabel();
        separator5 = new javax.swing.JToolBar.Separator();
        loadTestReportButton = new javax.swing.JButton();
        bottomToolBar = new javax.swing.JToolBar();
        spacer0 = new javax.swing.JLabel();
        materialBox = new javax.swing.JComboBox();
        spacer1 = new javax.swing.JLabel();
        sectionBox = new javax.swing.JComboBox();
        spacer2 = new javax.swing.JLabel();
        sizeBox = new javax.swing.JComboBox();
        spacer4 = new javax.swing.JLabel();
        increaseMemberSizeButton = new javax.swing.JButton();
        decreaseMemberSizeButton = new javax.swing.JButton();
        spacer5 = new javax.swing.JLabel();
        toggleMemberListButton = new javax.swing.JToggleButton();
        toggleMemberNumbersButton = new javax.swing.JToggleButton();
        toggleGuidesButton = new javax.swing.JToggleButton();
        toggleTemplateButton = new javax.swing.JToggleButton();
        separator7 = new javax.swing.JToolBar.Separator();
        setCoarseGridButton = new javax.swing.JToggleButton();
        setMediumGridButton = new javax.swing.JToggleButton();
        setFineGridButton = new javax.swing.JToggleButton();
        cardPanel = new javax.swing.JPanel();
        nullPanel = new javax.swing.JPanel();
        designPanel = new javax.swing.JPanel();
        drawingPanel = new javax.swing.JPanel();
        draftingJPanel = new DraftingPanel(bridge, bridgeDraftingView, this);
        openMemberTableButton = new javax.swing.JButton();
        titleBlockPanel = new TitleBlockPanel();
        titleLabel = new javax.swing.JLabel();
        designedByLabel = new javax.swing.JLabel();
        designedByField = new javax.swing.JTextField();
        projectIDLabel = new javax.swing.JLabel();
        scenarioIDLabel = new javax.swing.JLabel();
        projectIDField = new javax.swing.JTextField();
        verticalRuler = new Ruler((RulerHost)draftingJPanel, Ruler.WEST);
        corner = new javax.swing.JLabel();
        horizontalRuler = new Ruler((RulerHost)draftingJPanel, Ruler.SOUTH);
        memberPanel = new javax.swing.JPanel();
        memberTabs = new javax.swing.JTabbedPane();
        memberListPanel = new javax.swing.JPanel();
        loadTestResultsLabel = new javax.swing.JLabel();
        memberScroll = new javax.swing.JScrollPane();
        memberJTable = new MemberTable();
        memberDetailTabs = new javax.swing.JTabbedPane();
        memberDetailPanel = new javax.swing.JPanel();
        materialPropertiesLabel = new javax.swing.JLabel();
        materialPropertiesTable = new javax.swing.JTable();
        dimensionsLabel = new javax.swing.JLabel();
        dimensionsTable = new javax.swing.JTable();
        sketchLabel = new javax.swing.JLabel();
        crossSectionSketchLabel = new CrossSectionSketch();
        memberCostLabel = new javax.swing.JLabel();
        memberCostTable = new javax.swing.JTable();
        strengthCurveLabel = new StrengthCurve();
        graphAllCheck = new javax.swing.JCheckBox();
        curveLabel = new javax.swing.JLabel();
        memberSelectLabel = new javax.swing.JLabel();
        memberSelecButtonPanel = new javax.swing.JPanel();
        memberSelectLeftButton = new javax.swing.JButton();
        memberSelectRightButton = new javax.swing.JButton();
        memberSelectBox = new ExtendedComboBox(memberSelectLeftButton, memberSelectRightButton);
        flyThruAnimationCanvas = flyThruAnimation.getCanvas();
        fixedEyeAnimationCanvas = fixedEyeAnimation.getCanvas();
        gridSizeButtonGroup = new javax.swing.ButtonGroup();
        toolMenuGroup = new javax.swing.ButtonGroup();
        designTestGroup = new javax.swing.ButtonGroup();
        toolsDialog = new javax.swing.JDialog(getFrame());
        toolsToolbar = new javax.swing.JToolBar();
        editJointsButton = new javax.swing.JToggleButton();
        editMembersButton = new javax.swing.JToggleButton();
        editSelectButton = new javax.swing.JToggleButton();
        editEraseButton = new javax.swing.JToggleButton();
        memberEditPopup = new javax.swing.JDialog();
        memberPopupPanel = new javax.swing.JPanel();
        memberPopupMaterialBox = new javax.swing.JComboBox();
        memberPopupMaterialLabel = new javax.swing.JLabel();
        memberPopupSectionBox = new javax.swing.JComboBox();
        memberPopupSectionLabel = new javax.swing.JLabel();
        memberPopupSizeLabel = new javax.swing.JLabel();
        memberPopupSizeBox = new javax.swing.JComboBox();
        memberPopupIncreaseSizeButton = new javax.swing.JButton();
        memberPopupDecreaseSizeButton = new javax.swing.JButton();
        memberPopupDeleteButton = new javax.swing.JButton();
        memberPopupDoneButton = new javax.swing.JButton();
        memberPopupMemberListButton = new javax.swing.JToggleButton();
        draftingPopup = new javax.swing.JPopupMenu();
        draftingPopupJoints = new javax.swing.JRadioButtonMenuItem();
        draftingPopupMembers = new javax.swing.JRadioButtonMenuItem();
        draftingPopupSelect = new javax.swing.JRadioButtonMenuItem();
        draftingPopupErase = new javax.swing.JRadioButtonMenuItem();
        draftingPopupSep01 = new javax.swing.JSeparator();
        draftingPopupSelectAll = new javax.swing.JMenuItem();
        draftingPopupSep02 = new javax.swing.JSeparator();
        draftingPopupMemberList = new javax.swing.JCheckBoxMenuItem();
        draftingPopupSep03 = new javax.swing.JSeparator();
        draftingPopupCoarseGrid = new javax.swing.JRadioButtonMenuItem();
        draftingPopupMediumGrid = new javax.swing.JRadioButtonMenuItem();
        draftingPopupFineGrid = new javax.swing.JRadioButtonMenuItem();
        animationButtonGroup = new javax.swing.ButtonGroup();
        toolsButtonGroup = new javax.swing.ButtonGroup();
        keyCodeDialog = new javax.swing.JDialog();
        keyCodeLabel = new javax.swing.JLabel();
        keyCodeTextField = new javax.swing.JTextField();
        keyCodeOkButton = new javax.swing.JButton();
        keyCodeCancelButton = new javax.swing.JButton();
        keyCodeErrorLabel = new javax.swing.JLabel();
        drawingBoardLabel = new javax.swing.JLabel();

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setMnemonic('F');
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getResourceMap(BDView.class);
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(bridgedesigner.BDApp.class).getContext().getActionMap(BDView.class, this);
        newDesignMenuItem.setAction(actionMap.get("newDesign")); // NOI18N
        newDesignMenuItem.setText(resourceMap.getString("newDesignMenuItem.text")); // NOI18N
        newDesignMenuItem.setName("newDesignMenuItem"); // NOI18N
        fileMenu.add(newDesignMenuItem);

        fileMenuSeparator1.setName("fileMenuSeparator1"); // NOI18N
        fileMenu.add(fileMenuSeparator1);

        openMenuItem.setAction(actionMap.get("open")); // NOI18N
        openMenuItem.setName("openMenuItem"); // NOI18N
        fileMenu.add(openMenuItem);

        saveMenuItem.setAction(actionMap.get("save")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setAction(actionMap.get("saveas")); // NOI18N
        saveAsMenuItem.setText(resourceMap.getString("saveAsMenuItem.text")); // NOI18N
        saveAsMenuItem.setName("saveAsMenuItem"); // NOI18N
        fileMenu.add(saveAsMenuItem);

        fileMenuSeparator2.setName("fileMenuSeparator2"); // NOI18N
        fileMenu.add(fileMenuSeparator2);

        openSampleDesignMenuItem.setAction(actionMap.get("loadSampleDesign")); // NOI18N
        openSampleDesignMenuItem.setText(resourceMap.getString("openSampleDesignMenuItem.text")); // NOI18N
        openSampleDesignMenuItem.setName("openSampleDesignMenuItem"); // NOI18N
        fileMenu.add(openSampleDesignMenuItem);

        loadTemplateMenuItem.setAction(actionMap.get("loadTemplate")); // NOI18N
        loadTemplateMenuItem.setText(resourceMap.getString("loadTemplateMenuItem.text")); // NOI18N
        loadTemplateMenuItem.setName("loadTemplateMenuItem"); // NOI18N
        fileMenu.add(loadTemplateMenuItem);

        saveAsSample.setAction(actionMap.get("saveAsSample")); // NOI18N
        saveAsSample.setName("saveAsSample"); // NOI18N
        fileMenu.add(saveAsSample);

        saveAsTemplate.setAction(actionMap.get("saveAsTemplate")); // NOI18N
        saveAsTemplate.setName("saveAsTemplate"); // NOI18N
        fileMenu.add(saveAsTemplate);

        printLoadedClassesMenuItem.setAction(actionMap.get("printClasses")); // NOI18N
        printLoadedClassesMenuItem.setName("printLoadedClassesMenuItem"); // NOI18N
        fileMenu.add(printLoadedClassesMenuItem);

        fileMenuSeparator3.setName("fileMenuSeparator3"); // NOI18N
        fileMenu.add(fileMenuSeparator3);

        printMenuItem.setAction(actionMap.get("print")); // NOI18N
        printMenuItem.setName("printMenuItem"); // NOI18N
        fileMenu.add(printMenuItem);

        fileMenuSeparator4.setName("fileMenuSeparator4"); // NOI18N
        fileMenu.add(fileMenuSeparator4);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setIcon(resourceMap.getIcon("exitMenuItem.icon")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);
        exitMenuItem.getAccessibleContext().setAccessibleDescription(resourceMap.getString("exitMenuItem.AccessibleContext.accessibleDescription")); // NOI18N

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        selectallItem.setAction(actionMap.get("selectAll")); // NOI18N
        selectallItem.setName("selectallItem"); // NOI18N
        editMenu.add(selectallItem);

        deleteItem.setAction(actionMap.get("delete")); // NOI18N
        deleteItem.setName("deleteItem"); // NOI18N
        editMenu.add(deleteItem);

        editMenuSeparator1.setName("editMenuSeparator1"); // NOI18N
        editMenu.add(editMenuSeparator1);

        undoItem.setAction(actionMap.get("undo")); // NOI18N
        undoItem.setName("undoItem"); // NOI18N
        editMenu.add(undoItem);

        redoItem.setAction(actionMap.get("redo")); // NOI18N
        redoItem.setName("redoItem"); // NOI18N
        editMenu.add(redoItem);

        editMenuSeparator2.setName("editMenuSeparator2"); // NOI18N
        editMenu.add(editMenuSeparator2);

        back1iterationItem.setAction(actionMap.get("back1iteration")); // NOI18N
        back1iterationItem.setName("back1iterationItem"); // NOI18N
        editMenu.add(back1iterationItem);

        forward1iterationItem.setAction(actionMap.get("forward1iteration")); // NOI18N
        forward1iterationItem.setName("forward1iterationItem"); // NOI18N
        editMenu.add(forward1iterationItem);

        gotoIterationItem.setAction(actionMap.get("gotoIteration")); // NOI18N
        gotoIterationItem.setName("gotoIterationItem"); // NOI18N
        editMenu.add(gotoIterationItem);

        menuBar.add(editMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N

        toggleToolsMenuItem.setAction(actionMap.get("toggleTools")); // NOI18N
        toggleToolsMenuItem.setSelected(true);
        toggleToolsMenuItem.setName("toggleToolsMenuItem"); // NOI18N
        viewMenu.add(toggleToolsMenuItem);

        toggleAnimationControlsMenuItem.setAction(actionMap.get("toggleAnimationControls")); // NOI18N
        toggleAnimationControlsMenuItem.setSelected(true);
        toggleAnimationControlsMenuItem.setName("toggleAnimationControlsMenuItem"); // NOI18N
        viewMenu.add(toggleAnimationControlsMenuItem);

        toggleMemberListMenuItem.setAction(actionMap.get("toggleMemberList")); // NOI18N
        toggleMemberListMenuItem.setSelected(true);
        toggleMemberListMenuItem.setName("toggleMemberListMenuItem"); // NOI18N
        viewMenu.add(toggleMemberListMenuItem);

        toggleRulerMenuItem.setAction(actionMap.get("toggleRulers")); // NOI18N
        toggleRulerMenuItem.setSelected(true);
        toggleRulerMenuItem.setName("toggleRulerMenuItem"); // NOI18N
        viewMenu.add(toggleRulerMenuItem);

        toggleTitleBlockMenuItem.setAction(actionMap.get("toggleTitleBlock")); // NOI18N
        toggleTitleBlockMenuItem.setSelected(true);
        toggleTitleBlockMenuItem.setName("toggleTitleBlockMenuItem"); // NOI18N
        viewMenu.add(toggleTitleBlockMenuItem);

        toggleMemberNumbersMenuItem.setAction(actionMap.get("toggleMemberNumbers")); // NOI18N
        toggleMemberNumbersMenuItem.setSelected(true);
        toggleMemberNumbersMenuItem.setName("toggleMemberNumbersMenuItem"); // NOI18N
        viewMenu.add(toggleMemberNumbersMenuItem);

        toggleGuidesMenuItem.setAction(actionMap.get("toggleGuides")); // NOI18N
        toggleGuidesMenuItem.setSelected(true);
        toggleGuidesMenuItem.setName("toggleGuidesMenuItem"); // NOI18N
        viewMenu.add(toggleGuidesMenuItem);

        toggleTemplateMenuItem.setAction(actionMap.get("toggleTemplate")); // NOI18N
        toggleTemplateMenuItem.setSelected(true);
        toggleTemplateMenuItem.setName("toggleTemplateMenuItem"); // NOI18N
        viewMenu.add(toggleTemplateMenuItem);

        viewSeparator1.setName("viewSeparator1"); // NOI18N
        viewMenu.add(viewSeparator1);

        coarseGridMenuItem.setAction(actionMap.get("setCoarseGrid")); // NOI18N
        coarseGridMenuItem.setSelected(true);
        coarseGridMenuItem.setName("coarseGridMenuItem"); // NOI18N
        viewMenu.add(coarseGridMenuItem);

        mediumGridMenuItem.setAction(actionMap.get("setMediumGrid")); // NOI18N
        mediumGridMenuItem.setName("mediumGridMenuItem"); // NOI18N
        viewMenu.add(mediumGridMenuItem);

        fineGridMenuItem.setAction(actionMap.get("setFineGrid")); // NOI18N
        fineGridMenuItem.setName("fineGridMenuItem"); // NOI18N
        viewMenu.add(fineGridMenuItem);

        menuBar.add(viewMenu);

        toolsMenu.setMnemonic('T');
        toolsMenu.setText(resourceMap.getString("toolsMenu.text")); // NOI18N
        toolsMenu.setName("toolsMenu"); // NOI18N

        editJointsMenuItem.setAction(actionMap.get("editJoints")); // NOI18N
        toolMenuGroup.add(editJointsMenuItem);
        editJointsMenuItem.setSelected(true);
        editJointsMenuItem.setName("editJointsMenuItem"); // NOI18N
        toolsMenu.add(editJointsMenuItem);

        editMembersMenuItem.setAction(actionMap.get("editMembers")); // NOI18N
        toolMenuGroup.add(editMembersMenuItem);
        editMembersMenuItem.setName("editMembersMenuItem"); // NOI18N
        toolsMenu.add(editMembersMenuItem);

        editSelectMenuItem.setAction(actionMap.get("editSelect")); // NOI18N
        toolMenuGroup.add(editSelectMenuItem);
        editSelectMenuItem.setName("editSelectMenuItem"); // NOI18N
        toolsMenu.add(editSelectMenuItem);

        editEraseMenuItem.setAction(actionMap.get("editErase")); // NOI18N
        toolMenuGroup.add(editEraseMenuItem);
        editEraseMenuItem.setName("editEraseMenuItem"); // NOI18N
        toolsMenu.add(editEraseMenuItem);

        menuBar.add(toolsMenu);

        testMenu.setMnemonic('s');
        testMenu.setText(resourceMap.getString("testMenu.text")); // NOI18N
        testMenu.setName("testMenu"); // NOI18N

        drawingBoardMenuItem.setAction(actionMap.get("showDrawingBoard")); // NOI18N
        drawingBoardMenuItem.setSelected(true);
        drawingBoardMenuItem.setName("drawingBoardMenuItem"); // NOI18N
        testMenu.add(drawingBoardMenuItem);

        loadTestMenuItem.setAction(actionMap.get("runLoadTest")); // NOI18N
        loadTestMenuItem.setName("loadTestMenuItem"); // NOI18N
        testMenu.add(loadTestMenuItem);

        testMenuSep01.setName("testMenuSep01"); // NOI18N
        testMenu.add(testMenuSep01);

        toggleAnimationMenuItem.setAction(actionMap.get("toggleShowAnimation")); // NOI18N
        toggleAnimationMenuItem.setSelected(true);
        toggleAnimationMenuItem.setName("toggleAnimationMenuItem"); // NOI18N
        testMenu.add(toggleAnimationMenuItem);

        toggleLegacyGraphicsMenuItem.setAction(actionMap.get("toggleLegacyGraphics")); // NOI18N
        toggleLegacyGraphicsMenuItem.setSelected(true);
        toggleLegacyGraphicsMenuItem.setName("toggleLegacyGraphicsMenuItem"); // NOI18N
        testMenu.add(toggleLegacyGraphicsMenuItem);

        testMenuSep02.setName("testMenuSep02"); // NOI18N
        testMenu.add(testMenuSep02);

        toggleAutoCorrectMenuItem.setAction(actionMap.get("toggleAutoCorrect")); // NOI18N
        toggleAutoCorrectMenuItem.setSelected(true);
        toggleAutoCorrectMenuItem.setName("toggleAutoCorrectMenuItem"); // NOI18N
        testMenu.add(toggleAutoCorrectMenuItem);

        menuBar.add(testMenu);

        reportMenu.setMnemonic('R');
        reportMenu.setText(resourceMap.getString("reportMenu.text")); // NOI18N
        reportMenu.setName("reportMenu"); // NOI18N

        costReportMenuItem.setAction(actionMap.get("showCostDialog")); // NOI18N
        costReportMenuItem.setName("costReportMenuItem"); // NOI18N
        reportMenu.add(costReportMenuItem);

        loadTestReportMenuItem.setAction(actionMap.get("showLoadTestReport")); // NOI18N
        loadTestReportMenuItem.setName("loadTestReportMenuItem"); // NOI18N
        reportMenu.add(loadTestReportMenuItem);

        menuBar.add(reportMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        howToDesignMenuItem.setAction(actionMap.get("howToDesignABridge")); // NOI18N
        howToDesignMenuItem.setName("howToDesignMenuItem"); // NOI18N
        helpMenu.add(howToDesignMenuItem);

        bridgeDesignWindowMenuItem.setAction(actionMap.get("theBridgeDesignWindow")); // NOI18N
        bridgeDesignWindowMenuItem.setName("bridgeDesignWindowMenuItem"); // NOI18N
        helpMenu.add(bridgeDesignWindowMenuItem);

        helpSeparator01.setName("helpSeparator01"); // NOI18N
        helpMenu.add(helpSeparator01);

        helpTopicsMenuItem.setText(resourceMap.getString("helpTopicsMenuItem.text")); // NOI18N
        helpTopicsMenuItem.setName("helpTopicsMenuItem"); // NOI18N
        helpTopicsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpTopicsMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpTopicsMenuItem);

        searchForHelpMenuItem.setText(resourceMap.getString("searchForHelpMenuItem.text")); // NOI18N
        searchForHelpMenuItem.setName("searchForHelpMenuItem"); // NOI18N
        searchForHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchForHelpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(searchForHelpMenuItem);

        helpSeparator02.setName("helpSeparator02"); // NOI18N
        helpMenu.add(helpSeparator02);

        tipOfTheDayMenuItem.setAction(actionMap.get("showTipOfTheDay")); // NOI18N
        tipOfTheDayMenuItem.setName("tipOfTheDayMenuItem"); // NOI18N
        helpMenu.add(tipOfTheDayMenuItem);

        browseOurWebSiteMenuItem.setAction(actionMap.get("browseOurWebSite")); // NOI18N
        browseOurWebSiteMenuItem.setName("browseOurWebSiteMenuItem"); // NOI18N
        helpMenu.add(browseOurWebSiteMenuItem);

        aboutMenuItem.setAction(actionMap.get("about")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        jMenuItem1.setAction(actionMap.get("whatsNew")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        helpMenu.add(jMenuItem1);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1154, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 979, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        topToolBar.setFloatable(false);
        topToolBar.setRollover(true);
        topToolBar.setMaximumSize(new java.awt.Dimension(32767, 32));
        topToolBar.setMinimumSize(new java.awt.Dimension(1, 32));
        topToolBar.setName("topToolBar"); // NOI18N
        topToolBar.setPreferredSize(new java.awt.Dimension(100, 32));

        spacer3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer3.setText(resourceMap.getString("spacer3.text")); // NOI18N
        spacer3.setEnabled(false);
        spacer3.setFocusable(false);
        spacer3.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer3.setInheritsPopupMenu(false);
        spacer3.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer3.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer3.setName("spacer3"); // NOI18N
        spacer3.setOpaque(true);
        spacer3.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer3.setRequestFocusEnabled(false);
        spacer3.setVerifyInputWhenFocusTarget(false);
        topToolBar.add(spacer3);

        newButton.setAction(actionMap.get("newDesign")); // NOI18N
        newButton.setHideActionText(true);
        newButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newButton.setMaximumSize(new java.awt.Dimension(27, 27));
        newButton.setMinimumSize(new java.awt.Dimension(27, 27));
        newButton.setName("newButton"); // NOI18N
        newButton.setPreferredSize(new java.awt.Dimension(27, 27));
        newButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(newButton);

        openButton.setAction(actionMap.get("open")); // NOI18N
        openButton.setHideActionText(true);
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setMaximumSize(new java.awt.Dimension(27, 27));
        openButton.setMinimumSize(new java.awt.Dimension(27, 27));
        openButton.setName("openButton"); // NOI18N
        openButton.setPreferredSize(new java.awt.Dimension(27, 27));
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(openButton);

        saveButton.setAction(actionMap.get("save")); // NOI18N
        saveButton.setHideActionText(true);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setMaximumSize(new java.awt.Dimension(27, 27));
        saveButton.setMinimumSize(new java.awt.Dimension(27, 27));
        saveButton.setName("saveButton"); // NOI18N
        saveButton.setPreferredSize(new java.awt.Dimension(27, 27));
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(saveButton);

        printButton.setAction(actionMap.get("printToDefaultPrinter")); // NOI18N
        printButton.setHideActionText(true);
        printButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        printButton.setMaximumSize(new java.awt.Dimension(29, 29));
        printButton.setMinimumSize(new java.awt.Dimension(29, 29));
        printButton.setName("printButton"); // NOI18N
        printButton.setPreferredSize(new java.awt.Dimension(29, 29));
        printButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(printButton);

        separator6.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator6.setName("separator6"); // NOI18N
        topToolBar.add(separator6);

        drawingBoardButton.setAction(actionMap.get("showDrawingBoard")); // NOI18N
        designTestGroup.add(drawingBoardButton);
        drawingBoardButton.setSelected(true);
        drawingBoardButton.setHideActionText(true);
        drawingBoardButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        drawingBoardButton.setMaximumSize(new java.awt.Dimension(29, 29));
        drawingBoardButton.setMinimumSize(new java.awt.Dimension(29, 29));
        drawingBoardButton.setName("drawingBoardButton"); // NOI18N
        drawingBoardButton.setPreferredSize(new java.awt.Dimension(29, 29));
        drawingBoardButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(drawingBoardButton);

        loadTestButton.setAction(actionMap.get("runLoadTest")); // NOI18N
        designTestGroup.add(loadTestButton);
        loadTestButton.setHideActionText(true);
        loadTestButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadTestButton.setMaximumSize(new java.awt.Dimension(29, 29));
        loadTestButton.setMinimumSize(new java.awt.Dimension(29, 29));
        loadTestButton.setName("loadTestButton"); // NOI18N
        loadTestButton.setPreferredSize(new java.awt.Dimension(29, 29));
        loadTestButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(loadTestButton);

        separator3.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator3.setName("separator3"); // NOI18N
        topToolBar.add(separator3);

        selectAllButton.setAction(actionMap.get("selectAll")); // NOI18N
        selectAllButton.setHideActionText(true);
        selectAllButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        selectAllButton.setMaximumSize(new java.awt.Dimension(27, 27));
        selectAllButton.setMinimumSize(new java.awt.Dimension(27, 27));
        selectAllButton.setName("selectAllButton"); // NOI18N
        selectAllButton.setPreferredSize(new java.awt.Dimension(27, 27));
        selectAllButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(selectAllButton);

        deleteButton.setAction(actionMap.get("delete")); // NOI18N
        deleteButton.setHideActionText(true);
        deleteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteButton.setMaximumSize(new java.awt.Dimension(27, 27));
        deleteButton.setMinimumSize(new java.awt.Dimension(27, 27));
        deleteButton.setName("deleteButton"); // NOI18N
        deleteButton.setPreferredSize(new java.awt.Dimension(27, 27));
        deleteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(deleteButton);

        separator1.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator1.setName("separator1"); // NOI18N
        topToolBar.add(separator1);

        undoButton.setAction(actionMap.get("undo")); // NOI18N
        undoButton.setHideActionText(true);
        undoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        undoButton.setMaximumSize(new java.awt.Dimension(27, 27));
        undoButton.setMinimumSize(new java.awt.Dimension(27, 27));
        undoButton.setName("undoButton"); // NOI18N
        undoButton.setPreferredSize(new java.awt.Dimension(27, 27));
        undoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(undoButton);

        undoDropButton.setIcon(resourceMap.getIcon("undoDropButton.icon")); // NOI18N
        undoDropButton.setText(resourceMap.getString("undoDropButton.text")); // NOI18N
        undoDropButton.setToolTipText(resourceMap.getString("undoDropButton.toolTipText")); // NOI18N
        undoDropButton.setFocusable(false);
        undoDropButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        undoDropButton.setName("undoDropButton"); // NOI18N
        topToolBar.add(undoDropButton);

        spacer6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer6.setEnabled(false);
        spacer6.setFocusable(false);
        spacer6.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer6.setInheritsPopupMenu(false);
        spacer6.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer6.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer6.setName("spacer6"); // NOI18N
        spacer6.setOpaque(true);
        spacer6.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer6.setRequestFocusEnabled(false);
        spacer6.setVerifyInputWhenFocusTarget(false);
        topToolBar.add(spacer6);

        redoButton.setAction(actionMap.get("redo")); // NOI18N
        redoButton.setHideActionText(true);
        redoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        redoButton.setMaximumSize(new java.awt.Dimension(27, 27));
        redoButton.setMinimumSize(new java.awt.Dimension(27, 27));
        redoButton.setName("redoButton"); // NOI18N
        redoButton.setPreferredSize(new java.awt.Dimension(27, 27));
        redoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(redoButton);

        redoDropButton.setIcon(resourceMap.getIcon("redoDropButton.icon")); // NOI18N
        redoDropButton.setToolTipText(resourceMap.getString("redoDropButton.toolTipText")); // NOI18N
        redoDropButton.setFocusable(false);
        redoDropButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        redoDropButton.setName("redoDropButton"); // NOI18N
        topToolBar.add(redoDropButton);

        separator2.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator2.setName("separator2"); // NOI18N
        topToolBar.add(separator2);

        iterationLabel.setText(resourceMap.getString("iterationLabel.text")); // NOI18N
        iterationLabel.setName("iterationLabel"); // NOI18N
        topToolBar.add(iterationLabel);

        iterationNumberLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        iterationNumberLabel.setText(resourceMap.getString("iterationNumberLabel.text")); // NOI18N
        iterationNumberLabel.setMaximumSize(new java.awt.Dimension(32, 16));
        iterationNumberLabel.setMinimumSize(new java.awt.Dimension(32, 16));
        iterationNumberLabel.setName("iterationNumberLabel"); // NOI18N
        iterationNumberLabel.setPreferredSize(new java.awt.Dimension(32, 16));
        topToolBar.add(iterationNumberLabel);

        back1iterationButton.setAction(actionMap.get("back1iteration")); // NOI18N
        back1iterationButton.setHideActionText(true);
        back1iterationButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        back1iterationButton.setMaximumSize(new java.awt.Dimension(27, 27));
        back1iterationButton.setMinimumSize(new java.awt.Dimension(27, 27));
        back1iterationButton.setName("back1iterationButton"); // NOI18N
        back1iterationButton.setPreferredSize(new java.awt.Dimension(27, 27));
        back1iterationButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(back1iterationButton);

        forward1iterationButton.setAction(actionMap.get("forward1iteration")); // NOI18N
        forward1iterationButton.setHideActionText(true);
        forward1iterationButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        forward1iterationButton.setMaximumSize(new java.awt.Dimension(27, 27));
        forward1iterationButton.setMinimumSize(new java.awt.Dimension(27, 27));
        forward1iterationButton.setName("forward1iterationButton"); // NOI18N
        forward1iterationButton.setPreferredSize(new java.awt.Dimension(27, 27));
        forward1iterationButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(forward1iterationButton);

        showGoToIterationButton.setAction(actionMap.get("gotoIteration")); // NOI18N
        showGoToIterationButton.setHideActionText(true);
        showGoToIterationButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showGoToIterationButton.setMaximumSize(new java.awt.Dimension(27, 27));
        showGoToIterationButton.setMinimumSize(new java.awt.Dimension(27, 27));
        showGoToIterationButton.setName("showGoToIterationButton"); // NOI18N
        showGoToIterationButton.setPreferredSize(new java.awt.Dimension(27, 27));
        showGoToIterationButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(showGoToIterationButton);

        separator8.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator8.setName("separator8"); // NOI18N
        topToolBar.add(separator8);

        costLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        costLabel.setText(resourceMap.getString("costLabel.text")); // NOI18N
        costLabel.setToolTipText(resourceMap.getString("costLabel.toolTipText")); // NOI18N
        costLabel.setMaximumSize(new java.awt.Dimension(100, 16));
        costLabel.setMinimumSize(new java.awt.Dimension(100, 16));
        costLabel.setName("costLabel"); // NOI18N
        costLabel.setPreferredSize(new java.awt.Dimension(100, 16));
        topToolBar.add(costLabel);

        spacer7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer7.setEnabled(false);
        spacer7.setFocusable(false);
        spacer7.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer7.setInheritsPopupMenu(false);
        spacer7.setMaximumSize(new java.awt.Dimension(4, 16));
        spacer7.setMinimumSize(new java.awt.Dimension(4, 16));
        spacer7.setName("spacer7"); // NOI18N
        spacer7.setOpaque(true);
        spacer7.setPreferredSize(new java.awt.Dimension(4, 16));
        spacer7.setRequestFocusEnabled(false);
        spacer7.setVerifyInputWhenFocusTarget(false);
        topToolBar.add(spacer7);

        costReportButton.setAction(actionMap.get("showCostDialog")); // NOI18N
        costReportButton.setHideActionText(true);
        costReportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        costReportButton.setMaximumSize(new java.awt.Dimension(27, 27));
        costReportButton.setMinimumSize(new java.awt.Dimension(27, 27));
        costReportButton.setName("costReportButton"); // NOI18N
        costReportButton.setPreferredSize(new java.awt.Dimension(27, 27));
        costReportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(costReportButton);

        separator4.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator4.setName("separator4"); // NOI18N
        topToolBar.add(separator4);

        statusLabel.setIcon(resourceMap.getIcon("statusLabel.icon")); // NOI18N
        statusLabel.setText(resourceMap.getString("statusLabel.text")); // NOI18N
        statusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        statusLabel.setName("statusLabel"); // NOI18N
        topToolBar.add(statusLabel);

        separator5.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator5.setName("separator5"); // NOI18N
        topToolBar.add(separator5);

        loadTestReportButton.setAction(actionMap.get("showLoadTestReport")); // NOI18N
        loadTestReportButton.setFocusable(false);
        loadTestReportButton.setHideActionText(true);
        loadTestReportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadTestReportButton.setMaximumSize(new java.awt.Dimension(27, 27));
        loadTestReportButton.setMinimumSize(new java.awt.Dimension(27, 27));
        loadTestReportButton.setName("loadTestReportButton"); // NOI18N
        loadTestReportButton.setPreferredSize(new java.awt.Dimension(27, 27));
        loadTestReportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        topToolBar.add(loadTestReportButton);

        bottomToolBar.setFloatable(false);
        bottomToolBar.setRollover(true);
        bottomToolBar.setMaximumSize(new java.awt.Dimension(559, 32));
        bottomToolBar.setMinimumSize(new java.awt.Dimension(272, 32));
        bottomToolBar.setName("bottomToolBar"); // NOI18N

        spacer0.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer0.setText(resourceMap.getString("spacer0.text")); // NOI18N
        spacer0.setEnabled(false);
        spacer0.setFocusable(false);
        spacer0.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer0.setInheritsPopupMenu(false);
        spacer0.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer0.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer0.setName("spacer0"); // NOI18N
        spacer0.setOpaque(true);
        spacer0.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer0.setRequestFocusEnabled(false);
        spacer0.setVerifyInputWhenFocusTarget(false);
        bottomToolBar.add(spacer0);

        materialBox.setModel(bridge.getInventory().getMaterialBoxModel());
        materialBox.setMaximumSize(new java.awt.Dimension(250, 24));
        materialBox.setName("materialBox"); // NOI18N
        bottomToolBar.add(materialBox);

        spacer1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer1.setText(resourceMap.getString("spacer1.text")); // NOI18N
        spacer1.setEnabled(false);
        spacer1.setFocusable(false);
        spacer1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer1.setInheritsPopupMenu(false);
        spacer1.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer1.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer1.setName("spacer1"); // NOI18N
        spacer1.setOpaque(true);
        spacer1.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer1.setRequestFocusEnabled(false);
        spacer1.setVerifyInputWhenFocusTarget(false);
        bottomToolBar.add(spacer1);

        sectionBox.setModel(bridge.getInventory().getSectionBoxModel());
        sectionBox.setMaximumSize(new java.awt.Dimension(180, 24));
        sectionBox.setName("sectionBox"); // NOI18N
        bottomToolBar.add(sectionBox);

        spacer2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer2.setText(resourceMap.getString("spacer2.text")); // NOI18N
        spacer2.setEnabled(false);
        spacer2.setFocusable(false);
        spacer2.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer2.setInheritsPopupMenu(false);
        spacer2.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer2.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer2.setName("spacer2"); // NOI18N
        spacer2.setOpaque(true);
        spacer2.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer2.setRequestFocusEnabled(false);
        spacer2.setVerifyInputWhenFocusTarget(false);
        bottomToolBar.add(spacer2);

        sizeBox.setModel(bridge.getInventory().getSizeBoxModel());
        sizeBox.setMaximumSize(new java.awt.Dimension(150, 24));
        sizeBox.setName("sizeBox"); // NOI18N
        bottomToolBar.add(sizeBox);

        spacer4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer4.setEnabled(false);
        spacer4.setFocusable(false);
        spacer4.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer4.setInheritsPopupMenu(false);
        spacer4.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer4.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer4.setName("spacer4"); // NOI18N
        spacer4.setOpaque(true);
        spacer4.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer4.setRequestFocusEnabled(false);
        spacer4.setVerifyInputWhenFocusTarget(false);
        bottomToolBar.add(spacer4);

        increaseMemberSizeButton.setAction(actionMap.get("increaseMemberSize")); // NOI18N
        increaseMemberSizeButton.setHideActionText(true);
        increaseMemberSizeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        increaseMemberSizeButton.setMaximumSize(new java.awt.Dimension(29, 29));
        increaseMemberSizeButton.setMinimumSize(new java.awt.Dimension(29, 29));
        increaseMemberSizeButton.setName("increaseMemberSizeButton"); // NOI18N
        increaseMemberSizeButton.setPreferredSize(new java.awt.Dimension(29, 29));
        increaseMemberSizeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(increaseMemberSizeButton);

        decreaseMemberSizeButton.setAction(actionMap.get("decreaseMemberSize")); // NOI18N
        decreaseMemberSizeButton.setHideActionText(true);
        decreaseMemberSizeButton.setMaximumSize(new java.awt.Dimension(29, 29));
        decreaseMemberSizeButton.setMinimumSize(new java.awt.Dimension(29, 29));
        decreaseMemberSizeButton.setName("decreaseMemberSizeButton"); // NOI18N
        decreaseMemberSizeButton.setPreferredSize(new java.awt.Dimension(29, 29));
        bottomToolBar.add(decreaseMemberSizeButton);

        spacer5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        spacer5.setEnabled(false);
        spacer5.setFocusable(false);
        spacer5.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        spacer5.setInheritsPopupMenu(false);
        spacer5.setMaximumSize(new java.awt.Dimension(8, 16));
        spacer5.setMinimumSize(new java.awt.Dimension(8, 16));
        spacer5.setName("spacer5"); // NOI18N
        spacer5.setOpaque(true);
        spacer5.setPreferredSize(new java.awt.Dimension(8, 16));
        spacer5.setRequestFocusEnabled(false);
        spacer5.setVerifyInputWhenFocusTarget(false);
        bottomToolBar.add(spacer5);

        toggleMemberListButton.setAction(actionMap.get("toggleMemberList")); // NOI18N
        toggleMemberListButton.setSelected(true);
        toggleMemberListButton.setHideActionText(true);
        toggleMemberListButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleMemberListButton.setMaximumSize(new java.awt.Dimension(29, 29));
        toggleMemberListButton.setMinimumSize(new java.awt.Dimension(29, 29));
        toggleMemberListButton.setName("toggleMemberListButton"); // NOI18N
        toggleMemberListButton.setPreferredSize(new java.awt.Dimension(29, 29));
        toggleMemberListButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(toggleMemberListButton);

        toggleMemberNumbersButton.setAction(actionMap.get("toggleMemberNumbers")); // NOI18N
        toggleMemberNumbersButton.setHideActionText(true);
        toggleMemberNumbersButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleMemberNumbersButton.setMaximumSize(new java.awt.Dimension(29, 29));
        toggleMemberNumbersButton.setMinimumSize(new java.awt.Dimension(29, 29));
        toggleMemberNumbersButton.setName("toggleMemberNumbersButton"); // NOI18N
        toggleMemberNumbersButton.setPreferredSize(new java.awt.Dimension(29, 29));
        toggleMemberNumbersButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(toggleMemberNumbersButton);

        toggleGuidesButton.setAction(actionMap.get("toggleGuides")); // NOI18N
        toggleGuidesButton.setHideActionText(true);
        toggleGuidesButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleGuidesButton.setMaximumSize(new java.awt.Dimension(29, 29));
        toggleGuidesButton.setMinimumSize(new java.awt.Dimension(29, 29));
        toggleGuidesButton.setName("toggleGuidesButton"); // NOI18N
        toggleGuidesButton.setPreferredSize(new java.awt.Dimension(29, 29));
        toggleGuidesButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(toggleGuidesButton);

        toggleTemplateButton.setAction(actionMap.get("toggleTemplate")); // NOI18N
        toggleTemplateButton.setFocusable(false);
        toggleTemplateButton.setHideActionText(true);
        toggleTemplateButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleTemplateButton.setMaximumSize(new java.awt.Dimension(29, 29));
        toggleTemplateButton.setMinimumSize(new java.awt.Dimension(29, 29));
        toggleTemplateButton.setName("toggleTemplateButton"); // NOI18N
        toggleTemplateButton.setPreferredSize(new java.awt.Dimension(29, 29));
        toggleTemplateButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(toggleTemplateButton);

        separator7.setMaximumSize(new java.awt.Dimension(12, 32767));
        separator7.setName("separator7"); // NOI18N
        bottomToolBar.add(separator7);

        setCoarseGridButton.setAction(actionMap.get("setCoarseGrid")); // NOI18N
        gridSizeButtonGroup.add(setCoarseGridButton);
        setCoarseGridButton.setSelected(true);
        setCoarseGridButton.setHideActionText(true);
        setCoarseGridButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        setCoarseGridButton.setMaximumSize(new java.awt.Dimension(29, 29));
        setCoarseGridButton.setMinimumSize(new java.awt.Dimension(29, 29));
        setCoarseGridButton.setName("setCoarseGridButton"); // NOI18N
        setCoarseGridButton.setPreferredSize(new java.awt.Dimension(29, 29));
        setCoarseGridButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(setCoarseGridButton);

        setMediumGridButton.setAction(actionMap.get("setMediumGrid")); // NOI18N
        gridSizeButtonGroup.add(setMediumGridButton);
        setMediumGridButton.setHideActionText(true);
        setMediumGridButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        setMediumGridButton.setMaximumSize(new java.awt.Dimension(29, 29));
        setMediumGridButton.setMinimumSize(new java.awt.Dimension(29, 29));
        setMediumGridButton.setName("setMediumGridButton"); // NOI18N
        setMediumGridButton.setPreferredSize(new java.awt.Dimension(29, 29));
        setMediumGridButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(setMediumGridButton);

        setFineGridButton.setAction(actionMap.get("setFineGrid")); // NOI18N
        gridSizeButtonGroup.add(setFineGridButton);
        setFineGridButton.setHideActionText(true);
        setFineGridButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        setFineGridButton.setMaximumSize(new java.awt.Dimension(29, 29));
        setFineGridButton.setMinimumSize(new java.awt.Dimension(29, 29));
        setFineGridButton.setName("setFineGridButton"); // NOI18N
        setFineGridButton.setPreferredSize(new java.awt.Dimension(29, 29));
        setFineGridButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bottomToolBar.add(setFineGridButton);

        cardPanel.setName("cardPanel"); // NOI18N
        cardPanel.setPreferredSize(new java.awt.Dimension(640, 480));
        cardPanel.setLayout(new java.awt.CardLayout());

        nullPanel.setBackground(resourceMap.getColor("nullPanel.background")); // NOI18N
        nullPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        nullPanel.setName("nullPanel"); // NOI18N

        javax.swing.GroupLayout nullPanelLayout = new javax.swing.GroupLayout(nullPanel);
        nullPanel.setLayout(nullPanelLayout);
        nullPanelLayout.setHorizontalGroup(
            nullPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1150, Short.MAX_VALUE)
        );
        nullPanelLayout.setVerticalGroup(
            nullPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 607, Short.MAX_VALUE)
        );

        cardPanel.add(nullPanel, "nullPanel");

        designPanel.setName("designPanel"); // NOI18N

        drawingPanel.setName("drawingPanel"); // NOI18N
        drawingPanel.setLayout(new java.awt.GridBagLayout());

        draftingPanel = (DraftingPanel)draftingJPanel;
        draftingJPanel.setBackground(resourceMap.getColor("draftingJPanel.background")); // NOI18N
        draftingJPanel.setName("draftingJPanel"); // NOI18N

        openMemberTableButton.setAction(actionMap.get("openMemberTable")); // NOI18N
        openMemberTableButton.setFocusable(false);
        openMemberTableButton.setHideActionText(true);
        openMemberTableButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        openMemberTableButton.setName("openMemberTableButton"); // NOI18N
        openMemberTableButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        openMemberTableButton.setVisible(false);

        titleBlockPanel.setBackground(resourceMap.getColor("titleBlockPanel.background")); // NOI18N
        titleBlockPanel.setName("titleBlockPanel"); // NOI18N
        titleBlockPanel.setPreferredSize(new java.awt.Dimension(260, 71));
        titleBlockPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        titleLabel.setBackground(resourceMap.getColor("titleLabel.background")); // NOI18N
        titleLabel.setFont(resourceMap.getFont("titleLabel.font")); // NOI18N
        titleLabel.setForeground(resourceMap.getColor("titleLabel.foreground")); // NOI18N
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText(resourceMap.getString("titleLabel.text")); // NOI18N
        titleLabel.setName("titleLabel"); // NOI18N
        titleLabel.setPreferredSize(new java.awt.Dimension(258, 17));
        titleBlockPanel.add(titleLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 6, 248, -1));

        designedByLabel.setFont(resourceMap.getFont("designedByLabel.font")); // NOI18N
        designedByLabel.setText(resourceMap.getString("designedByLabel.text")); // NOI18N
        designedByLabel.setName("designedByLabel"); // NOI18N
        titleBlockPanel.add(designedByLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 28, -1, -1));

        designedByField.setForeground(resourceMap.getColor("designedByField.foreground")); // NOI18N
        designedByField.setText(resourceMap.getString("designedByField.text")); // NOI18N
        designedByField.setBorder(null);
        designedByField.setName("designedByField"); // NOI18N
        titleBlockPanel.add(designedByField, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 28, 180, -1));

        projectIDLabel.setFont(resourceMap.getFont("projectIDLabel.font")); // NOI18N
        projectIDLabel.setText(resourceMap.getString("projectIDLabel.text")); // NOI18N
        projectIDLabel.setName("projectIDLabel"); // NOI18N
        titleBlockPanel.add(projectIDLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 49, -1, -1));

        scenarioIDLabel.setForeground(resourceMap.getColor("scenarioIDLabel.foreground")); // NOI18N
        scenarioIDLabel.setText(resourceMap.getString("scenarioIDLabel.text")); // NOI18N
        scenarioIDLabel.setName("scenarioIDLabel"); // NOI18N
        titleBlockPanel.add(scenarioIDLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 49, -1, -1));

        projectIDField.setForeground(resourceMap.getColor("projectIDField.foreground")); // NOI18N
        projectIDField.setText(resourceMap.getString("projectIDField.text")); // NOI18N
        projectIDField.setBorder(null);
        projectIDField.setName("projectIDField"); // NOI18N
        titleBlockPanel.add(projectIDField, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 49, 140, -1));

        javax.swing.GroupLayout draftingJPanelLayout = new javax.swing.GroupLayout(draftingJPanel);
        draftingJPanel.setLayout(draftingJPanelLayout);
        draftingJPanelLayout.setHorizontalGroup(
            draftingJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(draftingJPanelLayout.createSequentialGroup()
                .addContainerGap(390, Short.MAX_VALUE)
                .addGroup(draftingJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(openMemberTableButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(titleBlockPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        draftingJPanelLayout.setVerticalGroup(
            draftingJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(draftingJPanelLayout.createSequentialGroup()
                .addComponent(openMemberTableButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 493, Short.MAX_VALUE)
                .addComponent(titleBlockPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        drawingPanel.add(draftingJPanel, gridBagConstraints);

        verticalRuler.setFocusable(false);
        verticalRuler.setMinimumSize(new java.awt.Dimension(32, 1));
        verticalRuler.setName("verticalRuler"); // NOI18N
        verticalRuler.setPreferredSize(new java.awt.Dimension(32, 1));
        verticalRuler.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        drawingPanel.add(verticalRuler, gridBagConstraints);

        corner.setFocusable(false);
        corner.setMinimumSize(new java.awt.Dimension(32, 32));
        corner.setName("corner"); // NOI18N
        corner.setPreferredSize(new java.awt.Dimension(32, 32));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        drawingPanel.add(corner, gridBagConstraints);

        horizontalRuler.setFocusable(false);
        horizontalRuler.setMinimumSize(new java.awt.Dimension(1, 32));
        horizontalRuler.setName("horizontalRuler"); // NOI18N
        horizontalRuler.setPreferredSize(new java.awt.Dimension(1, 32));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drawingPanel.add(horizontalRuler, gridBagConstraints);

        memberPanel.setName("memberPanel"); // NOI18N

        memberTabs.setAlignmentX(1.0F);
        memberTabs.setAlignmentY(0.0F);
        memberTabs.setName(null); // ");

        memberListPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        memberListPanel.setMaximumSize(new java.awt.Dimension(540, 32767));
        memberListPanel.setMinimumSize(new java.awt.Dimension(540, 100));
        memberListPanel.setName("memberListPanel"); // NOI18N
        memberListPanel.setPreferredSize(new java.awt.Dimension(540, 100));
        memberListPanel.setRequestFocusEnabled(false);

        loadTestResultsLabel.setFont(loadTestResultsLabel.getFont().deriveFont(loadTestResultsLabel.getFont().getStyle() | java.awt.Font.BOLD));
        loadTestResultsLabel.setText(resourceMap.getString("loadTestResultsLabel.text")); // NOI18N
        loadTestResultsLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        loadTestResultsLabel.setAlignmentY(0.0F);
        loadTestResultsLabel.setName("loadTestResultsLabel"); // NOI18N

        memberScroll.setName("memberScroll"); // NOI18N
        memberScroll.setPreferredSize(new java.awt.Dimension(454, 32));

        memberTable = (MemberTable)memberJTable;
        memberJTable.setModel(new MemberTableModel(bridge));
        memberJTable.setFillsViewportHeight(true);
        memberJTable.setName("memberJTable"); // NOI18N
        memberTable.initialize();
        memberScroll.setViewportView(memberJTable);

        javax.swing.GroupLayout memberListPanelLayout = new javax.swing.GroupLayout(memberListPanel);
        memberListPanel.setLayout(memberListPanelLayout);
        memberListPanelLayout.setHorizontalGroup(
            memberListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, memberListPanelLayout.createSequentialGroup()
                .addContainerGap(264, Short.MAX_VALUE)
                .addComponent(loadTestResultsLabel)
                .addGap(80, 80, 80))
            .addComponent(memberScroll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)
        );
        memberListPanelLayout.setVerticalGroup(
            memberListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(memberListPanelLayout.createSequentialGroup()
                .addComponent(loadTestResultsLabel)
                .addGap(8, 8, 8)
                .addComponent(memberScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 559, Short.MAX_VALUE))
        );

        memberTabs.addTab(resourceMap.getString("memberListPanel.TabConstraints.tabTitle"), memberListPanel); // NOI18N

        memberDetailTabs.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        memberDetailTabs.setName("memberDetailTabs"); // NOI18N

        memberDetailPanel.setName("memberDetailPanel"); // NOI18N

        materialPropertiesLabel.setText(resourceMap.getString("materialPropertiesLabel.text")); // NOI18N
        materialPropertiesLabel.setName("materialPropertiesLabel"); // NOI18N

        materialPropertiesTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        materialPropertiesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Material", null},
                {"Yield Stress (Fy)", null},
                {"Modulus of Elasticity (E)", null},
                {"Mass Density", null}
            },
            new String [] {
                "Title 1", "Title 2"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        materialPropertiesTable.setFocusable(false);
        materialPropertiesTable.setIntercellSpacing(new java.awt.Dimension(6, 4));
        materialPropertiesTable.setName("materialPropertiesTable"); // NOI18N
        materialPropertiesTable.setRowSelectionAllowed(false);

        dimensionsLabel.setText(resourceMap.getString("dimensionsLabel.text")); // NOI18N
        dimensionsLabel.setName("dimensionsLabel"); // NOI18N

        dimensionsTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        dimensionsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Cross-Section Type", null},
                {"Cross-Section Size", null},
                {"Area", null},
                {"Moment of Inertia", null},
                {"Member Length", null}
            },
            new String [] {
                "Title 1", "Title 2"
            }
        ));
        dimensionsTable.setFocusable(false);
        dimensionsTable.setIntercellSpacing(new java.awt.Dimension(6, 4));
        dimensionsTable.setName("dimensionsTable"); // NOI18N
        dimensionsTable.setRowSelectionAllowed(false);

        sketchLabel.setText(resourceMap.getString("sketchLabel.text")); // NOI18N
        sketchLabel.setName("sketchLabel"); // NOI18N

        crossSectionSketchLabel.setText(null);
        crossSectionSketchLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        crossSectionSketchLabel.setName("crossSectionSketchLabel"); // NOI18N

        memberCostLabel.setText(resourceMap.getString("memberCostLabel.text")); // NOI18N
        memberCostLabel.setName("memberCostLabel"); // NOI18N

        memberCostTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        memberCostTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Unit Cost", null},
                {"Member Cost", null}
            },
            new String [] {
                "Title 1", "Title 2"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        memberCostTable.setFocusable(false);
        memberCostTable.setIntercellSpacing(new java.awt.Dimension(6, 4));
        memberCostTable.setName("memberCostTable"); // NOI18N
        memberCostTable.setRowSelectionAllowed(false);

        strengthCurveLabel.setText(null);
        strengthCurveLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        strengthCurveLabel.setName("strengthCurveLabel"); // NOI18N

        graphAllCheck.setText(resourceMap.getString("graphAllCheck.text")); // NOI18N
        graphAllCheck.setName("graphAllCheck"); // NOI18N

        curveLabel.setText(resourceMap.getString("curveLabel.text")); // NOI18N
        curveLabel.setName("curveLabel"); // NOI18N

        memberSelectLabel.setText(resourceMap.getString("memberSelectLabel.text")); // NOI18N
        memberSelectLabel.setName("memberSelectLabel"); // NOI18N

        memberSelecButtonPanel.setName("memberSelecButtonPanel"); // NOI18N
        memberSelecButtonPanel.setLayout(new javax.swing.BoxLayout(memberSelecButtonPanel, javax.swing.BoxLayout.X_AXIS));

        memberSelectLeftButton.setIcon(resourceMap.getIcon("memberSelectLeftButton.icon")); // NOI18N
        memberSelectLeftButton.setEnabled(false);
        memberSelectLeftButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        memberSelectLeftButton.setMaximumSize(new java.awt.Dimension(16, 23));
        memberSelectLeftButton.setMinimumSize(new java.awt.Dimension(16, 23));
        memberSelectLeftButton.setName("memberSelectLeftButton"); // NOI18N
        memberSelectLeftButton.setPreferredSize(new java.awt.Dimension(16, 23));
        memberSelecButtonPanel.add(memberSelectLeftButton);

        memberSelectRightButton.setIcon(resourceMap.getIcon("memberSelectRightButton.icon")); // NOI18N
        memberSelectRightButton.setEnabled(false);
        memberSelectRightButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        memberSelectRightButton.setMaximumSize(new java.awt.Dimension(16, 23));
        memberSelectRightButton.setMinimumSize(new java.awt.Dimension(16, 23));
        memberSelectRightButton.setName("memberSelectRightButton"); // NOI18N
        memberSelectRightButton.setPreferredSize(new java.awt.Dimension(16, 23));
        memberSelecButtonPanel.add(memberSelectRightButton);

        memberSelectBox.setModel(new ExtendedComboBoxModel());
        memberSelectBox.setName("memberSelectBox"); // NOI18N

        javax.swing.GroupLayout memberDetailPanelLayout = new javax.swing.GroupLayout(memberDetailPanel);
        memberDetailPanel.setLayout(memberDetailPanelLayout);
        memberDetailPanelLayout.setHorizontalGroup(
            memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, memberDetailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(strengthCurveLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                    .addComponent(materialPropertiesTable, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                    .addComponent(materialPropertiesLabel, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(memberCostLabel, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, memberDetailPanelLayout.createSequentialGroup()
                        .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dimensionsLabel)
                            .addComponent(dimensionsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sketchLabel)
                            .addComponent(crossSectionSketchLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(memberCostTable, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, memberDetailPanelLayout.createSequentialGroup()
                        .addComponent(curveLabel)
                        .addGap(18, 18, 18)
                        .addComponent(graphAllCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 90, Short.MAX_VALUE)
                        .addComponent(memberSelectLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(memberSelectBox, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(memberSelecButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        memberDetailPanelLayout.setVerticalGroup(
            memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(memberDetailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(materialPropertiesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(materialPropertiesTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dimensionsLabel)
                    .addComponent(sketchLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(memberDetailPanelLayout.createSequentialGroup()
                        .addComponent(dimensionsTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(memberCostLabel))
                    .addComponent(crossSectionSketchLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(memberCostTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(memberDetailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(curveLabel)
                        .addComponent(graphAllCheck)
                        .addComponent(memberSelectLabel)
                        .addComponent(memberSelectBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(memberSelecButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(strengthCurveLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addContainerGap())
        );

        materialPropertiesTable.getColumnModel().getColumn(0).setResizable(false);
        materialPropertiesTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("materialPropertiesTable.columnModel.title0")); // NOI18N
        materialPropertiesTable.getColumnModel().getColumn(1).setResizable(false);
        materialPropertiesTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("materialPropertiesTable.columnModel.title1")); // NOI18N
        dimensionsTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("dimensionsTable.columnModel.title0")); // NOI18N
        dimensionsTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("dimensionsTable.columnModel.title1")); // NOI18N
        memberCostTable.getColumnModel().getColumn(0).setResizable(false);
        memberCostTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("materialPropertiesTable.columnModel.title0")); // NOI18N
        memberCostTable.getColumnModel().getColumn(1).setResizable(false);
        memberCostTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("materialPropertiesTable.columnModel.title1")); // NOI18N

        memberDetailTabs.addTab(resourceMap.getString("memberDetailPanel.TabConstraints.tabTitle"), memberDetailPanel); // NOI18N

        memberTabs.addTab(resourceMap.getString("memberDetailTabs.TabConstraints.tabTitle"), memberDetailTabs); // NOI18N

        javax.swing.GroupLayout memberPanelLayout = new javax.swing.GroupLayout(memberPanel);
        memberPanel.setLayout(memberPanelLayout);
        memberPanelLayout.setHorizontalGroup(
            memberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(memberTabs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 466, Short.MAX_VALUE)
        );
        memberPanelLayout.setVerticalGroup(
            memberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(memberTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 603, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout designPanelLayout = new javax.swing.GroupLayout(designPanel);
        designPanel.setLayout(designPanelLayout);
        designPanelLayout.setHorizontalGroup(
            designPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, designPanelLayout.createSequentialGroup()
                .addComponent(drawingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(memberPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        designPanelLayout.setVerticalGroup(
            designPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(drawingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
            .addComponent(memberPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        cardPanel.add(designPanel, "designPanel");

        flyThruAnimationCanvas.setName("flyThruAnimationCanvas"); // NOI18N
        cardPanel.add(flyThruAnimationCanvas, "flyThruAnimationPanel");

        fixedEyeAnimationCanvas.setName("fixedEyeAnimationCanvas"); // NOI18N
        cardPanel.add(fixedEyeAnimationCanvas, "fixedEyeAnimationPanel");

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cardPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1154, Short.MAX_VALUE)
            .addComponent(bottomToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 1154, Short.MAX_VALUE)
            .addComponent(topToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 1154, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(topToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bottomToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE))
        );

        toolsDialog.setTitle(resourceMap.getString("toolsDialog.title")); // NOI18N
        toolsDialog.setFocusable(false);
        toolsDialog.setFocusableWindowState(false);
        toolsDialog.setIconImage(bridgedesigner.BDApp.getApplication().getImageResource("tools.png"));
        toolsDialog.setMinimumSize(new java.awt.Dimension(80, 33));
        toolsDialog.setName("toolsDialog"); // NOI18N
        toolsDialog.setResizable(false);
        toolsDialog.getContentPane().setLayout(new javax.swing.BoxLayout(toolsDialog.getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        toolsToolbar.setFloatable(false);
        toolsToolbar.setName("toolsToolbar"); // NOI18N

        editJointsButton.setAction(actionMap.get("editJoints")); // NOI18N
        toolsButtonGroup.add(editJointsButton);
        editJointsButton.setFocusable(false);
        editJointsButton.setHideActionText(true);
        editJointsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editJointsButton.setName("editJointsButton"); // NOI18N
        editJointsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolsToolbar.add(editJointsButton);

        editMembersButton.setAction(actionMap.get("editMembers")); // NOI18N
        toolsButtonGroup.add(editMembersButton);
        editMembersButton.setFocusable(false);
        editMembersButton.setHideActionText(true);
        editMembersButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editMembersButton.setName("editMembersButton"); // NOI18N
        editMembersButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolsToolbar.add(editMembersButton);

        editSelectButton.setAction(actionMap.get("editSelect")); // NOI18N
        toolsButtonGroup.add(editSelectButton);
        editSelectButton.setFocusable(false);
        editSelectButton.setHideActionText(true);
        editSelectButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editSelectButton.setName("editSelectButton"); // NOI18N
        editSelectButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolsToolbar.add(editSelectButton);

        editEraseButton.setAction(actionMap.get("editErase")); // NOI18N
        toolsButtonGroup.add(editEraseButton);
        editEraseButton.setFocusable(false);
        editEraseButton.setHideActionText(true);
        editEraseButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editEraseButton.setName("editEraseButton"); // NOI18N
        editEraseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolsToolbar.add(editEraseButton);

        toolsDialog.getContentPane().add(toolsToolbar);

        memberEditPopup.setModal(true);
        memberEditPopup.setName("memberEditPopup"); // NOI18N
        memberEditPopup.setResizable(false);
        memberEditPopup.setUndecorated(true);

        memberPopupPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        memberPopupPanel.setName("memberPopupPanel"); // NOI18N

        memberPopupMaterialBox.setModel(bridge.getInventory().getMaterialBoxModel());
        memberPopupMaterialBox.setName("memberPopupMaterialBox"); // NOI18N

        memberPopupMaterialLabel.setLabelFor(memberPopupMaterialBox);
        memberPopupMaterialLabel.setText(resourceMap.getString("memberPopupMaterialLabel.text")); // NOI18N
        memberPopupMaterialLabel.setName("memberPopupMaterialLabel"); // NOI18N

        memberPopupSectionBox.setModel(bridge.getInventory().getSectionBoxModel());
        memberPopupSectionBox.setName("memberPopupSectionBox"); // NOI18N

        memberPopupSectionLabel.setLabelFor(memberPopupSectionBox);
        memberPopupSectionLabel.setText(resourceMap.getString("memberPopupSectionLabel.text")); // NOI18N
        memberPopupSectionLabel.setName("memberPopupSectionLabel"); // NOI18N

        memberPopupSizeLabel.setLabelFor(memberPopupSizeBox);
        memberPopupSizeLabel.setText(resourceMap.getString("memberPopupSizeLabel.text")); // NOI18N
        memberPopupSizeLabel.setName("memberPopupSizeLabel"); // NOI18N

        memberPopupSizeBox.setModel(bridge.getInventory().getSizeBoxModel());
        memberPopupSizeBox.setName("memberPopupSizeBox"); // NOI18N

        memberPopupIncreaseSizeButton.setAction(actionMap.get("increaseMemberSize")); // NOI18N
        memberPopupIncreaseSizeButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        memberPopupIncreaseSizeButton.setMargin(new java.awt.Insets(0, 0, 0, 2));
        memberPopupIncreaseSizeButton.setName("memberPopupIncreaseSizeButton"); // NOI18N

        memberPopupDecreaseSizeButton.setAction(actionMap.get("decreaseMemberSize")); // NOI18N
        memberPopupDecreaseSizeButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        memberPopupDecreaseSizeButton.setMargin(new java.awt.Insets(0, 0, 0, 2));
        memberPopupDecreaseSizeButton.setName("memberPopupDecreaseSizeButton"); // NOI18N

        memberPopupDeleteButton.setAction(actionMap.get("delete")); // NOI18N
        memberPopupDeleteButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        memberPopupDeleteButton.setMargin(new java.awt.Insets(0, 0, 0, 2));
        memberPopupDeleteButton.setName("memberPopupDeleteButton"); // NOI18N
        memberPopupDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberPopupDeleteButtonActionPerformed(evt);
            }
        });

        memberPopupDoneButton.setText(resourceMap.getString("memberPopupDoneButton.text")); // NOI18N
        memberPopupDoneButton.setName("memberPopupDoneButton"); // NOI18N
        memberPopupDoneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberPopupDoneButtonActionPerformed(evt);
            }
        });

        memberPopupMemberListButton.setAction(actionMap.get("toggleMemberList")); // NOI18N
        memberPopupMemberListButton.setMargin(new java.awt.Insets(0, 0, 0, 2));
        memberPopupMemberListButton.setName("memberPopupMemberListButton"); // NOI18N

        javax.swing.GroupLayout memberPopupPanelLayout = new javax.swing.GroupLayout(memberPopupPanel);
        memberPopupPanel.setLayout(memberPopupPanelLayout);
        memberPopupPanelLayout.setHorizontalGroup(
            memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(memberPopupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, memberPopupPanelLayout.createSequentialGroup()
                        .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(memberPopupDeleteButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                            .addComponent(memberPopupDecreaseSizeButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(memberPopupIncreaseSizeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(memberPopupDoneButton)
                            .addComponent(memberPopupMemberListButton)))
                    .addGroup(memberPopupPanelLayout.createSequentialGroup()
                        .addComponent(memberPopupSizeLabel)
                        .addGap(27, 27, 27)
                        .addComponent(memberPopupSizeBox, 0, 225, Short.MAX_VALUE))
                    .addGroup(memberPopupPanelLayout.createSequentialGroup()
                        .addComponent(memberPopupSectionLabel)
                        .addGap(9, 9, 9)
                        .addComponent(memberPopupSectionBox, 0, 225, Short.MAX_VALUE))
                    .addGroup(memberPopupPanelLayout.createSequentialGroup()
                        .addComponent(memberPopupMaterialLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(memberPopupMaterialBox, 0, 225, Short.MAX_VALUE)))
                .addContainerGap())
        );
        memberPopupPanelLayout.setVerticalGroup(
            memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(memberPopupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memberPopupMaterialLabel)
                    .addComponent(memberPopupMaterialBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memberPopupSectionLabel)
                    .addComponent(memberPopupSectionBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memberPopupSizeLabel)
                    .addComponent(memberPopupSizeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memberPopupIncreaseSizeButton)
                    .addComponent(memberPopupMemberListButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(memberPopupDecreaseSizeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memberPopupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memberPopupDeleteButton)
                    .addComponent(memberPopupDoneButton, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout memberEditPopupLayout = new javax.swing.GroupLayout(memberEditPopup.getContentPane());
        memberEditPopup.getContentPane().setLayout(memberEditPopupLayout);
        memberEditPopupLayout.setHorizontalGroup(
            memberEditPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(memberPopupPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        memberEditPopupLayout.setVerticalGroup(
            memberEditPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(memberPopupPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        draftingPopup.setName("draftingPopup"); // NOI18N

        draftingPopupJoints.setAction(actionMap.get("editJoints")); // NOI18N
        draftingPopupJoints.setSelected(true);
        draftingPopupJoints.setName("draftingPopupJoints"); // NOI18N
        draftingPopup.add(draftingPopupJoints);

        draftingPopupMembers.setAction(actionMap.get("editMembers")); // NOI18N
        draftingPopupMembers.setSelected(true);
        draftingPopupMembers.setName("draftingPopupMembers"); // NOI18N
        draftingPopup.add(draftingPopupMembers);

        draftingPopupSelect.setAction(actionMap.get("editSelect")); // NOI18N
        draftingPopupSelect.setSelected(true);
        draftingPopupSelect.setName("draftingPopupSelect"); // NOI18N
        draftingPopup.add(draftingPopupSelect);

        draftingPopupErase.setAction(actionMap.get("editErase")); // NOI18N
        draftingPopupErase.setSelected(true);
        draftingPopupErase.setName("draftingPopupErase"); // NOI18N
        draftingPopup.add(draftingPopupErase);

        draftingPopupSep01.setName("draftingPopupSep01"); // NOI18N
        draftingPopup.add(draftingPopupSep01);

        draftingPopupSelectAll.setAction(actionMap.get("selectAll")); // NOI18N
        draftingPopupSelectAll.setName("draftingPopupSelectAll"); // NOI18N
        draftingPopup.add(draftingPopupSelectAll);

        draftingPopupSep02.setName("draftingPopupSep02"); // NOI18N
        draftingPopup.add(draftingPopupSep02);

        draftingPopupMemberList.setAction(actionMap.get("toggleMemberList")); // NOI18N
        draftingPopupMemberList.setSelected(true);
        draftingPopupMemberList.setName("draftingPopupMemberList"); // NOI18N
        draftingPopup.add(draftingPopupMemberList);

        draftingPopupSep03.setName("draftingPopupSep03"); // NOI18N
        draftingPopup.add(draftingPopupSep03);

        draftingPopupCoarseGrid.setAction(actionMap.get("setCoarseGrid")); // NOI18N
        draftingPopupCoarseGrid.setSelected(true);
        draftingPopupCoarseGrid.setName("draftingPopupCoarseGrid"); // NOI18N
        draftingPopup.add(draftingPopupCoarseGrid);

        draftingPopupMediumGrid.setAction(actionMap.get("setMediumGrid")); // NOI18N
        draftingPopupMediumGrid.setSelected(true);
        draftingPopupMediumGrid.setName("draftingPopupMediumGrid"); // NOI18N
        draftingPopup.add(draftingPopupMediumGrid);

        draftingPopupFineGrid.setAction(actionMap.get("setFineGrid")); // NOI18N
        draftingPopupFineGrid.setSelected(true);
        draftingPopupFineGrid.setName("draftingPopupFineGrid"); // NOI18N
        draftingPopup.add(draftingPopupFineGrid);

        keyCodeDialog.setTitle(resourceMap.getString("keyCodeDialog.title")); // NOI18N
        keyCodeDialog.setIconImage(bridgedesigner.BDApp.getApplication().getImageResource("appicon.png"));
        keyCodeDialog.setName("keyCodeDialog"); // NOI18N
        keyCodeDialog.setResizable(false);

        keyCodeLabel.setText(resourceMap.getString("keyCodeLabel.text")); // NOI18N
        keyCodeLabel.setName("keyCodeLabel"); // NOI18N

        keyCodeTextField.setColumns(10);
        keyCodeTextField.setName("keyCodeTextField"); // NOI18N
        keyCodeTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                keyCodeTextFieldKeyTyped(evt);
            }
        });

        keyCodeOkButton.setText(resourceMap.getString("keyCodeOkButton.text")); // NOI18N
        keyCodeOkButton.setName("keyCodeOkButton"); // NOI18N
        keyCodeOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyCodeOkButtonActionPerformed(evt);
            }
        });

        keyCodeCancelButton.setText(resourceMap.getString("keyCodeCancelButton.text")); // NOI18N
        keyCodeCancelButton.setName("keyCodeCancelButton"); // NOI18N
        keyCodeCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyCodeCancelButtonActionPerformed(evt);
            }
        });

        keyCodeErrorLabel.setFont(keyCodeErrorLabel.getFont().deriveFont(keyCodeErrorLabel.getFont().getStyle() | java.awt.Font.BOLD));
        keyCodeErrorLabel.setForeground(resourceMap.getColor("keyCodeErrorLabel.foreground")); // NOI18N
        keyCodeErrorLabel.setText(resourceMap.getString("keyCodeErrorLabel.text")); // NOI18N
        keyCodeErrorLabel.setName("keyCodeErrorLabel"); // NOI18N

        javax.swing.GroupLayout keyCodeDialogLayout = new javax.swing.GroupLayout(keyCodeDialog.getContentPane());
        keyCodeDialog.getContentPane().setLayout(keyCodeDialogLayout);
        keyCodeDialogLayout.setHorizontalGroup(
            keyCodeDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, keyCodeDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(keyCodeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(keyCodeDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(keyCodeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(keyCodeErrorLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(keyCodeDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(keyCodeOkButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(keyCodeCancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        keyCodeDialogLayout.setVerticalGroup(
            keyCodeDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(keyCodeDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(keyCodeDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keyCodeCancelButton)
                    .addComponent(keyCodeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(keyCodeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(keyCodeDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keyCodeErrorLabel)
                    .addComponent(keyCodeOkButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        keyCodeDialog.pack();
        keyCodeErrorLabel.setVisible(false);

        drawingBoardLabel.setFont(resourceMap.getFont("drawingBoardLabel.font")); // NOI18N
        drawingBoardLabel.setForeground(resourceMap.getColor("drawingBoardLabel.foreground")); // NOI18N
        drawingBoardLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        drawingBoardLabel.setText(resourceMap.getString("drawingBoardLabel.text")); // NOI18N
        drawingBoardLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        drawingBoardLabel.setName("drawingBoardLabel"); // NOI18N

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

private void memberPopupDoneButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_memberPopupDoneButtonActionPerformed
    memberEditPopup.setVisible(false);
}//GEN-LAST:event_memberPopupDoneButtonActionPerformed

private void memberPopupDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_memberPopupDeleteButtonActionPerformed
    memberEditPopup.setVisible(false);
}//GEN-LAST:event_memberPopupDeleteButtonActionPerformed

private void searchForHelpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchForHelpMenuItemActionPerformed
    Help.getBroker().setCurrentView("Search");
    Help.getBroker().setDisplayed(true);
}//GEN-LAST:event_searchForHelpMenuItemActionPerformed

private void helpTopicsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpTopicsMenuItemActionPerformed
    Help.getBroker().setCurrentView("TOC");
    Help.getBroker().setCurrentID("hlp_purposes");
    Help.getBroker().setDisplayed(true);    
}//GEN-LAST:event_helpTopicsMenuItemActionPerformed

private void keyCodeOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyCodeOkButtonActionPerformed
    if (querySaveIfDirty()) {
        DesignConditions conditions = DesignConditions.fromKeyCode(keyCodeTextField.getText());
        if (conditions == null) {
            // Condition creation failed. Show the error message and keep dialoging.
            keyCodeErrorLabel.setVisible(true);
            return;
        }
        // If we have an open file, add it to the recent file list.
        recordRecentFileUse();
        // Close the dialog.
        keyCodeDialog.setVisible(false);
        // Save the key code in local storage for next use.
        BDApp.saveToLocalStorage(keyCodeTextField.getText(), keyCodeStorage);
        // Initialize the bridge and drafting view.
        bridge.initialize(conditions, "00000A-", null);
        bridgeDraftingView.initialize(conditions);
        // Erase template.
        setSketchModel(null);
        // Make sure drafting panel is visible.
        showDrawingBoard();
        // Copy bridge auxiliary data to drafting panel.
        uploadBridgeToDraftingPanel();
        // Reset file chooser to default save name.
        setDefaultFile();
        // Update load test button -- it will be disabled.
        setLoadTestButtonEnabled();
        // Virtually press the edit joints button.
        setSelected(editJointsMenuItem, true);
        editJoints();
    }
}//GEN-LAST:event_keyCodeOkButtonActionPerformed

private void keyCodeTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyCodeTextFieldKeyTyped
    keyCodeErrorLabel.setVisible(false);
}//GEN-LAST:event_keyCodeTextFieldKeyTyped

private void keyCodeCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyCodeCancelButtonActionPerformed
    keyCodeDialog.setVisible(false);
}//GEN-LAST:event_keyCodeCancelButtonActionPerformed

    @Action
    public void open() {
        // If we're animating, flip back to drafting board.
        if (animationPanelCardName.equals(selectedCard)) {
            selectCard(designPanelCardName);
        }
        if (querySaveIfDirty()) {
            File oldFile = fileChooser.getSelectedFile();
            int okCancel = fileChooser.showOpenDialog(mainPanel);
            if (okCancel == JFileChooser.APPROVE_OPTION) {
                BDApp.saveToLocalStorage(fileChooser.getCurrentDirectory().getPath(), fileChooserPathStorage);
                try {
                    animation.stop();
                    recordRecentFileUse(oldFile);
                    bridge.read(fileChooser.getSelectedFile());
                    if (bridge.getDesignConditions().isFromKeyCode()) {
                        BDApp.saveToLocalStorage(bridge.getDesignConditions().getCodeString(), keyCodeStorage);
                    }
                    initializePostBridgeLoad();
                } catch (IOException e) {
                    selectCard(nullPanelCard);
                    showReadFailedMessage(e);
                }
            }
        }
    }

    @Action
    public void save() {
        if (!bridge.getUndoManager().isStored()) {
            int okCancel = fileChooser.showSaveDialog(mainPanel);
            if (okCancel != JFileChooser.APPROVE_OPTION) {
                return;
            }
            appendDefaultSuffix(fileChooser);
            if (!overwriteOk(fileChooser.getSelectedFile(), getResourceMap().getString("saveDialog.title"))) {
                return;
            }
            BDApp.saveToLocalStorage(fileChooser.getCurrentDirectory().getPath(), fileChooserPathStorage);
        }
        downloadBridgeFromDraftingPanel();
        try {
            bridge.write(fileChooser.getSelectedFile());
            setTitleFileName();
        } catch (IOException e) {
            showMessageDialog(getResourceMap().getString("saveDialog.error") + e.getMessage());
        }
    }

    @Action
    public void saveas() {
        final int ok = fileChooser.showDialog(mainPanel, getResourceMap().getString("saveAsDialog.title"));
        if (ok != JFileChooser.APPROVE_OPTION) {
            return;
        }
        appendDefaultSuffix(fileChooser);
        if (!overwriteOk(fileChooser.getSelectedFile(), getResourceMap().getString("saveAsDialog.title"))) {
            return;
        }
        BDApp.saveToLocalStorage(fileChooser.getCurrentDirectory().getPath(), fileChooserPathStorage);
        downloadBridgeFromDraftingPanel();
        try {
            bridge.write(fileChooser.getSelectedFile());
            setTitleFileName();
        } catch (IOException e) {
            showMessageDialog(getResourceMap().getString("saveDialog.error") + e.getMessage());
        }
    }

    @Action
    public void print() {
        print(true);
    }

    @Action
    public void showDrawingBoard() {
        selectCard(designPanelCardName);
        // Normal usage pattern is to edit after testing, so help the user.
        setSelected(editSelectButton, true);
        editSelect();
    }

    @Action
    public void runLoadTest() {
        // Fixup might be needed due certain joint move edge cases.
        FixupCommand ruleEnforcer = new FixupCommand(bridge);
        int revisedMemberCount = ruleEnforcer.revisedMemberCount();
        if (revisedMemberCount > 0) {
            ruleEnforcer.execute(bridge.getUndoManager());
            showMessageDialog(revisedMemberCount == 1 ?
                getResourceMap().getString("autoCorrectMessageSingle.text") :
                getResourceMap().getString("autoCorrectMessageMany.text", revisedMemberCount));
        }
        // Analyze the bridge the first time.
        bridge.analyze();
        // If bridge is indeterminate and user has asked for it, try heuristic automatic fixes.
        if (bridge.getAnalysis().getStatus() == Analysis.UNSTABLE && autofixEnabled()) {
            bridge.autofix();
            bridge.analyze();
        }
        // Enable or disable the load test report button based on availabilty of valid analysis.
        enabledStateManager.setEnabled(loadTestReportButton, bridge.isAnalysisValid());
        // Update the design status icon.
        setStatusIcon();
        if (bridge.getAnalysis().getStatus() == Analysis.UNSTABLE) {
            // Show the unstable truss tutorial if analysis failed entirely.
            setSelected(drawingBoardButton, true);
            if (unstableModelDialog == null) {
                JFrame mainFrame = BDApp.getApplication().getMainFrame();
                unstableModelDialog = new UnstableModelDialog(mainFrame);
                unstableModelDialog.pack();
                unstableModelDialog.setLocationRelativeTo(mainFrame);
            }
            unstableModelDialog.setVisible(true);
        } else if (bridge.getAnalysis().getStatus() == Analysis.FAILS_SLENDERNESS) {
            // Show the slenderness tutorial if the slenderness test failed.
            setSelected(drawingBoardButton, true);
            if (slendernessTestFailDialog == null) {
                JFrame mainFrame = BDApp.getApplication().getMainFrame();
                slendernessTestFailDialog = new SlendernessTestFailDialog(mainFrame);
                slendernessTestFailDialog.pack();
                slendernessTestFailDialog.setLocationRelativeTo(mainFrame);
            }
            slendernessTestFailDialog.setVisible(true);            
        } else if (animationEnabled()) {
            // Show the animation if it's enabled.
            selectCard(animationPanelCardName);
        }
        else {
            // Otherwise back to the drwing board.
            setSelected(drawingBoardButton, true);
        }
        if (!bridge.isPassing()) {
            // If there are failed members, show the member table tab so user can see them.
            memberTabs.setSelectedIndex(0);                
        }
    }

    @Action
    public void openMemberTable() {
        selectMemberList(true);
    }

    @Action
    public void closeMemberTable() {
        selectMemberList(false);
    }

    @Action
    public void toggleMemberList() {
        selectMemberList(isSelected(toggleMemberListButton));
    }

    @Action
    public void increaseMemberSize() {
        if (bridge.isSelectedMember()) {
            dispatcher.incrementMemberSize(1);
        }
        else {
            stockSelector.incrementSize(1);
        }
    }

    @Action
    public void decreaseMemberSize() {
        if (bridge.isSelectedMember()) {
            dispatcher.incrementMemberSize(-1);
        }
        else {
            stockSelector.incrementSize(-1);            
        }
    }    

    @Action
    public void newDesign() {
        if(querySaveIfDirty()) {
            DesignConditions conditions = bridge.getDesignConditions();
            if (conditions != null && conditions.isFromKeyCode()) {
                showMessageDialog(getResourceMap().getString("keyCodeRestart.text", conditions.getCodeString()));
                restartWithCurrentConditions();
            }
            else {
                showSetupWizard();
            }
        }
    }

    @Action
    public void loadSampleDesign() {
        if (loadSampleDialog == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            loadSampleDialog = new LoadSampleDialog(mainFrame);
            loadSampleDialog.setLocationRelativeTo(mainFrame);
            loadSampleDialog.initialize();
        }
        loadSampleDialog.setVisible(true);
        if (loadSampleDialog.isOk()) {
            animation.stop();
            recordRecentFileUse();
            loadSampleDialog.loadUsingSelectedSample(bridge);
            fileChooser.setSelectedFile(getDefaultFile());
            initializePostBridgeLoad();
        }
    }

    @Action
    public void loadTemplate() {
        if (loadTemplateDialog == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            loadTemplateDialog = new LoadTemplateDialog(mainFrame);
            loadTemplateDialog.setLocationRelativeTo(mainFrame);
        }
        loadTemplateDialog.initialize(bridge.getDesignConditions(), bridgeDraftingView.getBridgeSketchView().getModel());
        loadTemplateDialog.setVisible(true);
        if (loadTemplateDialog.isOk()) {
            setSketchModel(loadTemplateDialog.getSketchModel());
            draftingPanel.paintBackingStore();
            draftingPanel.repaint();
        }
    }

    @Action
    public void editJoints() {
        draftingPanel.editJoints();
    }

    @Action
    public void editMembers() {
        draftingPanel.editMembers();
    }

    @Action
    public void editSelect() {
        draftingPanel.editSelect();
    }

    @Action
    public void editErase() {
        draftingPanel.editErase();
    }

    @Action
    public void setCoarseGrid() {
        setGrid(DraftingGrid.COARSE_GRID);
    }

    @Action
    public void setMediumGrid() {
        setGrid(DraftingGrid.MEDIUM_GRID);
    }

    @Action
    public void setFineGrid() {
        setGrid(DraftingGrid.FINE_GRID);
    }

    @Action
    public void toggleRulers() {
        selectRulers(isSelected(toggleRulerMenuItem));
    }

    @Action
    public void undo() {
        draftingPanel.eraseCrosshairs();
        bridge.getUndoManager().undo();
    }

    @Action
    public void redo() {
        draftingPanel.eraseCrosshairs();
        bridge.getUndoManager().redo();
    }

    @Action
    public void toggleTitleBlock() {
        titleBlockPanel.setVisible(isSelected(toggleTitleBlockMenuItem));
    }

    @Action
    public void toggleMemberNumbers() {
        boolean label = isSelected(toggleMemberNumbersMenuItem);
        draftingPanel.setLabel(label);
        memberTable.setLabel(label);
    }

    @Action
    public void selectAll() {
        bridge.selectAllMembers();
    }

    @Action
    public void delete() {
        bridge.deleteSelection();
    }

    @Action
    public void toggleTools() {
        toolsDialog.setVisible(isSelected(toggleToolsMenuItem));
    }

    @Action
    public void toggleGuides() {
        draftingPanel.setGuidesVisible(isSelected(toggleGuidesButton));
    }

    @Action
    public void saveAsTemplate() {
        downloadBridgeFromDraftingPanel();
        String id = JOptionPane.showInputDialog(getFrame(), "Enter unique id", "Save As Template", JOptionPane.QUESTION_MESSAGE);
        if (id != null) {
            try {
                bridge.writeTemplate(id);
            } catch (IOException e) {
                showMessageDialog("Could not write template: " + e.getMessage());
            }
        }
    }

    @Action
    public void saveAsSample() {
        downloadBridgeFromDraftingPanel();
        String id = JOptionPane.showInputDialog(getFrame(), "Enter unique id", "Save As Template", JOptionPane.QUESTION_MESSAGE);
        if (id != null) {
            try {
                bridge.writeSample(id);
            } catch (IOException e) {
                showMessageDialog("Could not write sample: " + e.getMessage());
            }
        }
    }
    
    @Action
    public void showLoadTestReport() {
        if (loadTestReport == null) {
            loadTestReport = new LoadTestReport(getFrame(), bridge);
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            loadTestReport.setLocationRelativeTo(mainFrame);
        }
        downloadBridgeFromDraftingPanel();
        loadTestReport.setVisible(true);
    }

    @Action
    public void showTipOfTheDay() {
        tipDialog.showTip(false, 0);
    }

    @Action
    public void howToDesignABridge() {
        Help.getBroker().setCurrentView("TOC");
        Help.getBroker().setCurrentID("hlp_how_to_design_a_bridge");
        Help.getBroker().setDisplayed(true);    
    }

    @Action
    public void theBridgeDesignWindow() {
        Help.getBroker().setCurrentView("TOC");
        Help.getBroker().setCurrentID("hlp_bridge_design_window");
        Help.getBroker().setDisplayed(true);    
    }

    @Action
    public void browseOurWebSite() {
        try {
            Browser.openUrl("http://bridgecontest.org");
        } catch (IOException ex) {
            showMessageDialog(getResourceMap().getString("browseOurWebSite.error"));
        }
    }

    @Action
    public void about() {
        if (aboutBox == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            aboutBox = new AboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        aboutBox.setVisible(true);
    }

    @Action
    public void showCostDialog() {
        if (costReport == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            costReport = new CostReport(mainFrame);
            costReport.setLocationRelativeTo(mainFrame);
        }
        downloadBridgeFromDraftingPanel();
        costReport.initialize(bridge.getCostsWithNotes());
        costReport.setVisible(true);
    }

    @Action
    public void back1iteration() {
        bridge.saveSnapshot();
        int nextIterationIndex = bridge.getNextIterationIndex(-1);
        if (nextIterationIndex >= 0) {
            bridge.loadIteration(nextIterationIndex);
            bridgeDraftingView.initialize(bridge.getDesignConditions());
            uploadBridgeToDraftingPanel();        
        }
    }

    @Action
    public void forward1iteration() {
        int nextIterationIndex = bridge.getNextIterationIndex(+1);
        if (nextIterationIndex >= 0) {
            bridge.loadIteration(nextIterationIndex);
            bridgeDraftingView.initialize(bridge.getDesignConditions());
            uploadBridgeToDraftingPanel();        
        }
    }

    @Action
    public void gotoIteration() {
        if (designIterationDialog == null) {
            JFrame mainFrame = BDApp.getApplication().getMainFrame();
            designIterationDialog = new DesignIterationDialog(mainFrame, bridge);
            designIterationDialog.setLocationRelativeTo(mainFrame);
        }
        bridge.clearSelectedJoint(true);
        designIterationDialog.setVisible(true);
        if (designIterationDialog.isOk()) {
            designIterationDialog.loadSelectedIteration();
            bridgeDraftingView.initialize(bridge.getDesignConditions());
            uploadBridgeToDraftingPanel();
        }
    }

    @Action
    public void toggleAnimationControls() {
        animation.getControls().getDialog().setVisible(isSelected(toggleAnimationControlsMenuItem));
    }

    @Action
    public void toggleTemplate() {
        draftingPanel.setTemplateVisible(isSelected(toggleTemplateMenuItem));
    }

    @Action
    public void toggleShowAnimation() {
        setLoadTestButtonEnabled();
    }

    private void setLegacyGraphics()
    {
        animationPanelCardName = fixedEyeAnimationPanelCardName;
        animation = fixedEyeAnimation;
        BDApp.saveToLocalStorage(true, graphicsCapabilityStorage);
    }

    private void setStandardGraphics()
    {
        animationPanelCardName = flyThruAnimationPanelCardName;
        animation = flyThruAnimation;
        BDApp.saveToLocalStorage(false, graphicsCapabilityStorage);
    }

    /**
     * Retrieve last animation convention from local storage if there and set
     * appropriately.  Default to legacy graphics.
     *
     * @return whether legacy graphics are the default
     */
    private boolean setDefaultGraphics()
    {
        Boolean useLegacyStorage = (Boolean)BDApp.loadFromLocalStorage(graphicsCapabilityStorage);
        boolean useLegacy = BDApp.isLegacyGraphics() || useLegacyStorage == null || useLegacyStorage.booleanValue();
        if (useLegacy) {
            setLegacyGraphics();
        }
        else {
            setStandardGraphics();
        }
        return useLegacy;
    }

    @Action
    public void toggleLegacyGraphics() {
        boolean selected = isSelected(toggleLegacyGraphicsMenuItem);
        if (selected) {
            setLegacyGraphics();
        }
        else {
            setStandardGraphics();
        }
    }

    @Action
    public void toggleAutoCorrect() {
        setLoadTestButtonEnabled();
    }

    @Action
    public void printToDefaultPrinter() {
        print(false);
    }

    @Action
    public void whatsNew() {
        Help.getBroker().setCurrentID("hlp_whats_new_2ed");
        Help.getBroker().setDisplayed(true);
    }

    @Action
    public void printClasses() {
        ClassLister.printPreloader(null);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Declarations">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup animationButtonGroup;
    private javax.swing.JButton back1iterationButton;
    private javax.swing.JMenuItem back1iterationItem;
    private javax.swing.JToolBar bottomToolBar;
    private javax.swing.JMenuItem bridgeDesignWindowMenuItem;
    private javax.swing.JMenuItem browseOurWebSiteMenuItem;
    private javax.swing.JPanel cardPanel;
    private javax.swing.JRadioButtonMenuItem coarseGridMenuItem;
    private javax.swing.JLabel corner;
    private javax.swing.JLabel costLabel;
    private javax.swing.JButton costReportButton;
    private javax.swing.JMenuItem costReportMenuItem;
    private javax.swing.JLabel crossSectionSketchLabel;
    private javax.swing.JLabel curveLabel;
    private javax.swing.JButton decreaseMemberSizeButton;
    private javax.swing.JButton deleteButton;
    private javax.swing.JMenuItem deleteItem;
    private javax.swing.JPanel designPanel;
    private javax.swing.ButtonGroup designTestGroup;
    private javax.swing.JTextField designedByField;
    private javax.swing.JLabel designedByLabel;
    private javax.swing.JLabel dimensionsLabel;
    private javax.swing.JTable dimensionsTable;
    private javax.swing.JPanel draftingJPanel;
    private DraftingPanel draftingPanel;
    private javax.swing.JPopupMenu draftingPopup;
    private javax.swing.JRadioButtonMenuItem draftingPopupCoarseGrid;
    private javax.swing.JRadioButtonMenuItem draftingPopupErase;
    private javax.swing.JRadioButtonMenuItem draftingPopupFineGrid;
    private javax.swing.JRadioButtonMenuItem draftingPopupJoints;
    private javax.swing.JRadioButtonMenuItem draftingPopupMediumGrid;
    private javax.swing.JCheckBoxMenuItem draftingPopupMemberList;
    private javax.swing.JRadioButtonMenuItem draftingPopupMembers;
    private javax.swing.JRadioButtonMenuItem draftingPopupSelect;
    private javax.swing.JMenuItem draftingPopupSelectAll;
    private javax.swing.JSeparator draftingPopupSep01;
    private javax.swing.JSeparator draftingPopupSep02;
    private javax.swing.JSeparator draftingPopupSep03;
    private javax.swing.JToggleButton drawingBoardButton;
    private javax.swing.JLabel drawingBoardLabel;
    private javax.swing.JRadioButtonMenuItem drawingBoardMenuItem;
    private javax.swing.JPanel drawingPanel;
    private javax.swing.JToggleButton editEraseButton;
    private javax.swing.JRadioButtonMenuItem editEraseMenuItem;
    private javax.swing.JToggleButton editJointsButton;
    private javax.swing.JRadioButtonMenuItem editJointsMenuItem;
    private javax.swing.JToggleButton editMembersButton;
    private javax.swing.JRadioButtonMenuItem editMembersMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JSeparator editMenuSeparator1;
    private javax.swing.JSeparator editMenuSeparator2;
    private javax.swing.JToggleButton editSelectButton;
    private javax.swing.JRadioButtonMenuItem editSelectMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JSeparator fileMenuSeparator1;
    private javax.swing.JSeparator fileMenuSeparator2;
    private javax.swing.JSeparator fileMenuSeparator3;
    private javax.swing.JSeparator fileMenuSeparator4;
    private javax.swing.JRadioButtonMenuItem fineGridMenuItem;
    private java.awt.Canvas fixedEyeAnimationCanvas;
    private java.awt.Canvas flyThruAnimationCanvas;
    private javax.swing.JButton forward1iterationButton;
    private javax.swing.JMenuItem forward1iterationItem;
    private javax.swing.JMenuItem gotoIterationItem;
    private javax.swing.JCheckBox graphAllCheck;
    private javax.swing.ButtonGroup gridSizeButtonGroup;
    private javax.swing.JSeparator helpSeparator01;
    private javax.swing.JSeparator helpSeparator02;
    private javax.swing.JMenuItem helpTopicsMenuItem;
    private javax.swing.JLabel horizontalRuler;
    private javax.swing.JMenuItem howToDesignMenuItem;
    private javax.swing.JButton increaseMemberSizeButton;
    private javax.swing.JLabel iterationLabel;
    private javax.swing.JLabel iterationNumberLabel;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JButton keyCodeCancelButton;
    private javax.swing.JDialog keyCodeDialog;
    private javax.swing.JLabel keyCodeErrorLabel;
    private javax.swing.JLabel keyCodeLabel;
    private javax.swing.JButton keyCodeOkButton;
    private javax.swing.JTextField keyCodeTextField;
    private javax.swing.JMenuItem loadTemplateMenuItem;
    private javax.swing.JToggleButton loadTestButton;
    private javax.swing.JRadioButtonMenuItem loadTestMenuItem;
    private javax.swing.JButton loadTestReportButton;
    private javax.swing.JMenuItem loadTestReportMenuItem;
    private javax.swing.JLabel loadTestResultsLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JComboBox materialBox;
    private javax.swing.JLabel materialPropertiesLabel;
    private javax.swing.JTable materialPropertiesTable;
    private javax.swing.JRadioButtonMenuItem mediumGridMenuItem;
    private javax.swing.JLabel memberCostLabel;
    private javax.swing.JTable memberCostTable;
    private javax.swing.JPanel memberDetailPanel;
    private javax.swing.JTabbedPane memberDetailTabs;
    private javax.swing.JDialog memberEditPopup;
    private javax.swing.JTable memberJTable;
    private MemberTable memberTable;
    private javax.swing.JPanel memberListPanel;
    private javax.swing.JPanel memberPanel;
    private javax.swing.JButton memberPopupDecreaseSizeButton;
    private javax.swing.JButton memberPopupDeleteButton;
    private javax.swing.JButton memberPopupDoneButton;
    private javax.swing.JButton memberPopupIncreaseSizeButton;
    private javax.swing.JComboBox memberPopupMaterialBox;
    private javax.swing.JLabel memberPopupMaterialLabel;
    private javax.swing.JToggleButton memberPopupMemberListButton;
    private javax.swing.JPanel memberPopupPanel;
    private javax.swing.JComboBox memberPopupSectionBox;
    private javax.swing.JLabel memberPopupSectionLabel;
    private javax.swing.JComboBox memberPopupSizeBox;
    private javax.swing.JLabel memberPopupSizeLabel;
    private javax.swing.JScrollPane memberScroll;
    private javax.swing.JPanel memberSelecButtonPanel;
    private javax.swing.JComboBox memberSelectBox;
    private javax.swing.JLabel memberSelectLabel;
    private javax.swing.JButton memberSelectLeftButton;
    private javax.swing.JButton memberSelectRightButton;
    private javax.swing.JTabbedPane memberTabs;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newButton;
    private javax.swing.JMenuItem newDesignMenuItem;
    private javax.swing.JPanel nullPanel;
    private javax.swing.JButton openButton;
    private javax.swing.JButton openMemberTableButton;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem openSampleDesignMenuItem;
    private javax.swing.JButton printButton;
    private javax.swing.JMenuItem printLoadedClassesMenuItem;
    private javax.swing.JMenuItem printMenuItem;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTextField projectIDField;
    private javax.swing.JLabel projectIDLabel;
    private javax.swing.JButton redoButton;
    private javax.swing.JToggleButton redoDropButton;
    private javax.swing.JMenuItem redoItem;
    private javax.swing.JMenu reportMenu;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveAsSample;
    private javax.swing.JMenuItem saveAsTemplate;
    private javax.swing.JButton saveButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JLabel scenarioIDLabel;
    private javax.swing.JMenuItem searchForHelpMenuItem;
    private javax.swing.JComboBox sectionBox;
    private javax.swing.JButton selectAllButton;
    private javax.swing.JMenuItem selectallItem;
    private javax.swing.JToolBar.Separator separator1;
    private javax.swing.JToolBar.Separator separator2;
    private javax.swing.JToolBar.Separator separator3;
    private javax.swing.JToolBar.Separator separator4;
    private javax.swing.JToolBar.Separator separator5;
    private javax.swing.JToolBar.Separator separator6;
    private javax.swing.JToolBar.Separator separator7;
    private javax.swing.JToolBar.Separator separator8;
    private javax.swing.JToggleButton setCoarseGridButton;
    private javax.swing.JToggleButton setFineGridButton;
    private javax.swing.JToggleButton setMediumGridButton;
    private javax.swing.JButton showGoToIterationButton;
    private javax.swing.JComboBox sizeBox;
    private javax.swing.JLabel sketchLabel;
    private javax.swing.JLabel spacer0;
    private javax.swing.JLabel spacer1;
    private javax.swing.JLabel spacer2;
    private javax.swing.JLabel spacer3;
    private javax.swing.JLabel spacer4;
    private javax.swing.JLabel spacer5;
    private javax.swing.JLabel spacer6;
    private javax.swing.JLabel spacer7;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JLabel strengthCurveLabel;
    private javax.swing.JMenu testMenu;
    private javax.swing.JSeparator testMenuSep01;
    private javax.swing.JPopupMenu.Separator testMenuSep02;
    private javax.swing.JMenuItem tipOfTheDayMenuItem;
    private javax.swing.JPanel titleBlockPanel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JCheckBoxMenuItem toggleAnimationControlsMenuItem;
    private javax.swing.JCheckBoxMenuItem toggleAnimationMenuItem;
    private javax.swing.JCheckBoxMenuItem toggleAutoCorrectMenuItem;
    private javax.swing.JToggleButton toggleGuidesButton;
    private javax.swing.JCheckBoxMenuItem toggleGuidesMenuItem;
    private javax.swing.JCheckBoxMenuItem toggleLegacyGraphicsMenuItem;
    private javax.swing.JToggleButton toggleMemberListButton;
    private javax.swing.JCheckBoxMenuItem toggleMemberListMenuItem;
    private javax.swing.JToggleButton toggleMemberNumbersButton;
    private javax.swing.JCheckBoxMenuItem toggleMemberNumbersMenuItem;
    private javax.swing.JCheckBoxMenuItem toggleRulerMenuItem;
    private javax.swing.JToggleButton toggleTemplateButton;
    private javax.swing.JCheckBoxMenuItem toggleTemplateMenuItem;
    private javax.swing.JCheckBoxMenuItem toggleTitleBlockMenuItem;
    private javax.swing.JCheckBoxMenuItem toggleToolsMenuItem;
    private javax.swing.ButtonGroup toolMenuGroup;
    private javax.swing.ButtonGroup toolsButtonGroup;
    private javax.swing.JDialog toolsDialog;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JToolBar toolsToolbar;
    private javax.swing.JToolBar topToolBar;
    private javax.swing.JButton undoButton;
    private javax.swing.JToggleButton undoDropButton;
    private javax.swing.JMenuItem undoItem;
    private javax.swing.JLabel verticalRuler;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JSeparator viewSeparator1;
    // End of variables declaration//GEN-END:variables

    // </editor-fold>
}
