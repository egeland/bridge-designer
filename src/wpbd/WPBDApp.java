/*
 * The West Point Bridge Designer (2nd Edition) application class.
 */

package wpbd;

import ch.randelshofer.quaqua.QuaquaManager;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GLProfile;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;
import jogamp.common.Debug;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SessionStorage;

/**
 * The main class of the West Point Bridge Designer (2nd Edition) application.
 */
public class WPBDApp extends SingleFrameApplication {

    public static final int WINDOWS_OS = 1;
    public static final int MAC_OS_X = 2;
    public static final int LINUX_OS = 3;
    public static final int UNKNOWN_OS = 4;
    
    private static WPBDView view = null;
    private static int os = 0;
    private static Level loggerLevel = Debug.isPropertyDefined("wpbd.develop", false, null) ? Level.ALL : Level.OFF;
    private static String fileName = null;
    private static boolean legacyGraphics = false;
    // By default we'll try for an enhanced Mac interface as long as we're running on a Mac.
    private static boolean enhancedMacUI = (getOS() == MAC_OS_X);
    private static GLProfile glProfile = null;

    // Attempt to get a glProfile for OpenGL rendering.
    public static GLProfile getGLProfile()
    {
        if (glProfile == null) {
            glProfile = GLProfile.get(GLProfile.GL2)            ;
        }
        return glProfile;
    }

    public static int getOS() {
        if (os == 0) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.indexOf("windows") >= 0) {
                os = WINDOWS_OS;
            }
            else if (osName.indexOf("mac os x") >= 0) {
                os = MAC_OS_X;
            }
            else {
                os = UNKNOWN_OS;
            }
        }
        return os;
    }
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        // Our drawing is too slow to keep up with dyanmic window resizing, so
        // turn it off.  (This does nothing on some platforms.)
        // System.setProperty("sun.awt.noerasebackground", "true");
        Toolkit.getDefaultToolkit().setDynamicLayout(false);

        // Set up the Quaqua interface if main() decided we want it or if developer is forcing it.
        boolean developMac = Debug.isPropertyDefined("wpbd.develop.mac", false, null);
        if (enhancedMacUI || developMac) {
            try {
                if (developMac && WPBDApp.getOS() != WPBDApp.MAC_OS_X) {
                    System.setProperty("Quaqua.Debug.crossPlatform", "true");
                }
                UIManager.setLookAndFeel(QuaquaManager.getLookAndFeel());
            } catch (Exception e) {
                System.err.println("Could not load L&F (" + QuaquaManager.getLookAndFeelClassName() + ").");
                enhancedMacUI = false;
            }
        }

        // Workaround for bogus session storage logger messages.
        Logger logger = Logger.getLogger(SessionStorage.class.getName());
        logger.setLevel(loggerLevel);

        // Make the main view window with all UI widgets.
        view = new WPBDView(this);
        JFrame frame = view.getFrame();
        // This doesn't work with original SAF.  But with BSAF it does.
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

        // Install an exit listener for this app that does cleanup.
        addExitListener(new ExitListener() {
            public boolean canExit(EventObject e) { return quit(); }
            public void willExit(EventObject e) { }
        });
        if (getOS() == MAC_OS_X) {
            try {
                Method quitMethod = getClass().getDeclaredMethod("quit", (Class[])null);
                OSXAdapter.setQuitHandler(this, quitMethod);
                Method aboutMethod = getClass().getDeclaredMethod("about", (Class[])null);
                OSXAdapter.setAboutHandler(this, aboutMethod);
                // OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
                // OSXAdapter.setFileHandler(this, view.getClass().getDeclaredMethod("open", new Class[] { String.class }));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
            }
        }
        show(view);
        // Resize application to full screen.  Can't find a way to do this in advance.
        // getMainFrame().setExtendedState(getMainFrame().getExtendedState() | Frame.MAXIMIZED_BOTH);
        view.initComponentsPostShow();
    }

    /**
     * Initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) { }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of WPBDApp
     */
    public static WPBDApp getApplication() {
        return Application.getInstance(WPBDApp.class);
    }
    
    /**
     * Return the resource map for the given class within this application.
     * 
     * @param c the class
     * @return resource map for the class
     */
    public static ResourceMap getResourceMap(Class c) {
        return getApplication().getContext().getResourceMap(c);
    }
    
    /**
     * Return the main frame for this application.
     * 
     * @return frame
     */
    public static JFrame getFrame() {
        return view.getFrame();
    }

    /**
     * Return the file name given on the command line or null if none.
     * 
     * @return file name
     */
    public static String getFileName() {
        return fileName;
    }

    public static boolean isLegacyGraphics() {
        return legacyGraphics;
    }
    
    public static boolean isEnhancedMacUI() {
        return enhancedMacUI;
    }
    
    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-legacygraphics")) {
                legacyGraphics = true;
            }
            else if (args[i].equals("-noenhancedmacui")) {
                enhancedMacUI = false;
            }
            else if (fileName == null) {
                fileName = args[i];
            }
            else {
                System.err.printf("Invalid arguments in command.");
                return;
            }
        }
        launch(WPBDApp.class, args);
    }
    
    /**
     * Return an image retrieved from the application resource pool.
     * 
     * We fetch an icon because the image is embedded and it takes care of blocking until
     * the image read is complete.
     * 
     * @param name name of image to fetch
     * @return an image from the application resource pool
     */
    public Image getImageResource(String name) {
        return getIconResource(name).getImage();
    }

    /**
     * Return a buffered image taken from a graphics file in the application resource pool.
     * Argh... How many ways can the Java guys think of to represent a simple image file?
     *
     * @param name name of image to fetch
     * @return a buffered image from the application resource pool
     */
    public BufferedImage getBufferedImageResource(String name) {
        ImageIcon icon = getIconResource(name);
        Image image = icon.getImage();
        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics g = bufferedImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bufferedImage;
    }

    /**
     * Return an icon retrieved from the application resource pool.
     * 
     * @param name name of icon to retrieve
     * @return an icon from the application resource pool
     */
    public ImageIcon getIconResource(String name) {
        // Null URLs can result from CASE mismatches in resource file name.
        URL url = getClass().getResource("/wpbd/resources/" + name);
        // System.out.println("icon: " + name + " (" + url + ")");
        return new ImageIcon(url);
    }
    
    /**
     * Return a texture with data retrieved from the application resource pool.
     * 
     * @param name name of the texture to retrieve
     * @param mipmap whether to automatically generate mipmaps
     * @param suffix one of the Texture.XXX constants denoting the image type of the texture
     * @return the texture created from retrieved texture image data
     */
    public Texture getTextureResource(String name, boolean mipmap, String suffix) {
        try {
            return TextureIO.newTexture(getClass().getResource("/wpbd/resources/" + name), mipmap, suffix);
        } catch (IOException ex) { 
            return null;
        }
    }
    
    public TextureData getTextureDataResource(String name, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) {
        try {
            return TextureIO.newTextureData(glProfile, getClass().getResource("/wpbd/resources/" + name), internalFormat, pixelFormat, mipmap, fileSuffix);
        } catch (IOException ex) {
            return null;
        }
    }
    
    public TextureData getTextureDataResource(String name, boolean mipmap, String suffix) {
        try {
            return TextureIO.newTextureData(glProfile, getClass().getResource("/wpbd/resources/" + name), mipmap, suffix);
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static boolean saveToLocalStorage(Object item, String name) {
        try {
            getApplication().getContext().getLocalStorage().save(item, name);
            return true;
        } catch (IOException ex) { 
            return false;
        }
    }
    
    public static Object loadFromLocalStorage(String name) {
        try {
            return WPBDApp.getApplication().getContext().getLocalStorage().load(name);
        } catch (IOException ex) { 
            return null;
        }
    }

    public boolean quit() {
        // Take shutdown actions on the view.
        view.shutdown();
        // Ask the user to save a dirty design, if any.
        return view.querySaveIfDirty();
    }

    public void about() {
        view.about();
    }
 }
