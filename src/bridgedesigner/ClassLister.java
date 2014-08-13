/*
 * ClassLister.java  
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Lister for classes in use, with code emitter for a preloader.
 * [ Not currently in use. ]
 * 
 * @author Eugene K. Ressler
 */
public class ClassLister {

    /**
     * Return an iterator over a vector of classes currently in use.
     * 
     * @param cl class loader to inspect
     * @return iterator over a vector of classes
     * @throws java.lang.NoSuchFieldException
     * @throws java.lang.IllegalAccessException
     */
    public static Iterator list(ClassLoader cl)
            throws NoSuchFieldException, IllegalAccessException {
        Class clClass = cl.getClass();
        while (clClass != java.lang.ClassLoader.class) {
            clClass = clClass.getSuperclass();
        }
        Field clClassesField = clClass.getDeclaredField("classes");
        clClassesField.setAccessible(true);
        Vector classes = (Vector) clClassesField.get(cl);
        return classes.iterator();
    }

    /**
     * Print java code for a preloader for all classes currently in use.
     * 
     * @param cl class loader to inspect; if null, application loader will be used
     */
    public static void printPreloader(ClassLoader cl) {
        if (cl == null) {
            cl = BDApp.class.getClassLoader();
        }
        File f = new File("src/wpbd/Preloader.java");
        FileOutputStream os;
        try {
            os = new FileOutputStream(f, false);
        } catch (FileNotFoundException ex) {
            return;
        }
        PrintWriter ps = new PrintWriter(os);
        ps.println("package wpbd;");
        ps.println();
        ps.println("public class Preloader {");
        ps.println();
        ps.println("    private static final String [] classes = {");
        while (cl != null) {
            Iterator iter;
            try {
                iter = list(cl);
            } catch (Exception ex) { 
                continue;
            }
            while (iter.hasNext()) {
                String name = iter.next().toString();
                if (name.indexOf('$') >= 0) {
                    continue;
                }
                int i = name.indexOf(' ');
                if (i >= 0) {
                    name = name.substring(i + 1);
                }
                ps.println("        \"" + name + "\",");
            }
            cl = cl.getParent();
        }        
        ps.println("    };");
        ps.println();
        ps.println("    public static void preload() {");
        ps.println("        for (int i = 0; i < classes.length; i++) {");
        ps.println("            try {");
        ps.println("                Class.forName(classes[i]);");
        ps.println("            } catch (ClassNotFoundException ex) { }");
        ps.println("        }");
        ps.println("    }");
        ps.println("}");
        ps.close();
    }
    
    /**
     * Print in textual form some information about classes currently in use.
     * 
     * @param cl class loader to use.
     */
    public static void print(ClassLoader cl) {
        while (cl != null) {
            System.out.println("ClassLoader: " + cl);
            Iterator iter;
            try {
                iter = list(cl);
            } catch (Exception ex) { 
                continue;
            }
            while (iter.hasNext()) {
                System.out.println("\"" + iter.next() + "\",");
            }
            cl = cl.getParent();
        }        
    }
}
