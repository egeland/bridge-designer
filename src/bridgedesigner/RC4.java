/*
 * RC4.java  
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

/**
 * RC4 encoder/decoder of byte arrays.
 * 
 * @author Eugene K. Ressler
 */
public class RC4 {

    private int xState, yState;
    private byte[] buf = new byte[256];

    /**
     * Construct an RC4 encoder object.
     */
    public RC4() {
        for (int i = 0; i < 256; i++) {
            buf[i] = (byte) i;
        }
        xState = yState = 0;
    }

    /**
     * Set the key for the encoding.
     * 
     * @param key key as byte array.
     */
    public void setKey(byte[] key) {
        int tmp, x, y;
        int len = Math.min(256, key.length);
        for (x = y = 0; x < 256; x++) {
            y = (y + buf[x] + key[x % len]) & 255;
            tmp = buf[x];
            buf[x] = buf[y];
            buf[y] = (byte) tmp;
        }
        xState = x;
        yState = y;
    }
    
    /**
     * Set the key for the encoding
     * 
     * @param s key as string
     */
    public void setKey(String s) {
        setKey(s.getBytes());
    }

    /**
     * Encrypt or decrypt a buffer of bytes.
     * 
     * @param buf byte buffer
     */
    public void endecrypt(byte[] buf) {
        int x = xState;
        int y = yState;
        byte[] s = this.buf;
        for (int i = 0; i < buf.length; i++) {
            x = (x + 1) & 255;
            y = (y + s[x]) & 255;
            byte tmp = s[x];
            s[x] = s[y];
            s[y] = tmp;
            buf[i] ^= s[((int) s[x] + s[y]) & 255];
        }
        xState = x;
        yState = y;
    }
}
