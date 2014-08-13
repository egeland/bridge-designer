/*
 * IconFactory.java
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

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * A factory class to return icons to represent other values.
 * Truly useful only in the case where the same factory functions are
 * needed by more than one other class.
 */
public class IconFactory {

    static IconFactory iconFactory;

    private final ImageIcon goodIcon;
    private final ImageIcon badIcon;
    private final ImageIcon workingIcon;

    private IconFactory() {
        BDApp app = BDApp.getApplication();
        goodIcon = app.getIconResource("goodsmall.png");
        badIcon = app.getIconResource("badsmall.png");
        workingIcon = app.getIconResource("workingsmall.png");
    }

    private static void initialize() {
        if (iconFactory == null) {
            iconFactory =  new IconFactory();
        }
    }

    public static Icon bridgeStatus(int status) {
        initialize();
        switch (status) {
            case EditableBridgeModel.STATUS_PASSES:
                return iconFactory.goodIcon;
            case EditableBridgeModel.STATUS_FAILS:
                return iconFactory.badIcon;
            default:
                return iconFactory.workingIcon;
        }
    }
}
