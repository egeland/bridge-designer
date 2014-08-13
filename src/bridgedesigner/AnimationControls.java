/*
 * AnimationControls.java
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

import java.awt.Dialog;

/**
 *
 * @author Eugene K. Ressler
 */
public interface AnimationControls {
    public void saveState();
    public void restoreState();
    public Dialog getDialog();
    public void startAnimation();
    public void saveVisibilityAndHide();
    public boolean getVisibleState();
}
