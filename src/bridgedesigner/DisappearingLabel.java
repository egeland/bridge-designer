package bridgedesigner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * A label that erases itself after a specified number of milliseconds.
 */
public class DisappearingLabel extends JLabel implements ActionListener {

    private final Timer hider;

    public DisappearingLabel(int delay) {
        hider = new Timer(delay, this);
        hider.setRepeats(false);
    }

    @Override
    public void setText(String s) {
        super.setText(s);
        if (s != null && !s.isEmpty()) {
            hider.start();
        }
    }

    public void actionPerformed(ActionEvent e) {
        super.setText(null);
    }
}