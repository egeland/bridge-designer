/*
 * Overlay.java  
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

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.GL2;

/**
 * Overlayed bitmaps for OpenGL scenes that are normally faded and become opaque upon demand.
 * 
 * @author Eugene K. Ressler
 */
public class Overlay {

    private int x = 0;
    private int y = 0;
    private float alpha = .2f;
    private Texture texture;
    private int size;
    
    private static final float [] texBox = {
        0f, 1f,
        1f, 1f,
        1f, 0f,
        0f, 0f,
    };
    private static final float [] boxTemplate = {
        -.5f, -.5f,
        .5f, -.5f,
        .5f, .5f, 
        -.5f, .5f, 
    };
    private final float [] box = new float[boxTemplate.length];

    /**
     * Return the x-coordinate of the overlay.
     * 
     * @return x-coordinate of overlay
     */
    public int getX() {
        return x;
    }

    /**
     * Return the y-coordinate of the overlay.
     * 
     * @return y-coordinate of overlay
     */
    public int getY() {
        return y;
    }
      
    public void initialize(GL2 gl, String textureResourceName) {
        texture = BDApp.getApplication().getTextureResource(textureResourceName, false, TextureIO.PNG);
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        size = texture.getImageWidth();
        setPosition(size / 2 + 1, size /2 + 1);
    }

    /**
     * Set the position of the overlay.
     * 
     * @param x x-coordinate of overlay
     * @param y y-coordinate of overlay
     */
    public void setPosition(int x, int y) {
        for (int i = 0; i < box.length; i += 2) {
            box[i + 0] = boxTemplate[i + 0] * size + x;
            box[i + 1] = boxTemplate[i + 1] * size + y;
        }
        this.x = x;
        this.y = y;
    }
    
    /**
     * Return true iff the given screen coordinate is inside the extend of the overlay.
     * 
     * @param x screen x-coordinate to test
     * @param y screen y-coordinate to test
     * @return true iff the given screen coordinate is inside the overlay
     */
    public boolean inside(int x, int y) {
        return box[0] <= x && x <= box[2] && box[1] <= y && y <= box[5];
    }
    
    /**
     * Delegate for the mouse moved event that makes the overlay opaque if the mouse has rolled over it.
     * 
     * @param x mouse x-coordinate
     * @param y mouse y-coordinate
     */
    public void mouseMoved(int x, int y) {
        alpha = inside(x, y) ? 1f : .15f;
    }
    
    /**
     * Display the overlay.
     * 
     * @param gl OpenGL graphics context
     */
    public void display(GL2 gl) {
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        texture.enable(gl);
        texture.bind(gl);
        //gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1f, 1f, 1f, alpha);
        for (int i = 0; i < box.length; i += 2) {
            gl.glTexCoord2fv(texBox, i);
            gl.glVertex2fv(box, i);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_BLEND);
        texture.disable(gl);
    }
}
