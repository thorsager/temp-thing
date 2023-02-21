/*
 * TtyGuiOutputJPanel.java
 * 
 * Copyright (c) 2002-2007 Avaya Inc. All rights reserved.
 * 
 * USE OR INSTALLATION OF THIS SAMPLE DEMONSTRATION SOFTWARE INDICATES THE END
 * USERS ACCEPTANCE OF THE GENERAL LICENSE TERMS AVAILABLE ON THE AVAYA WEBSITE
 * AT http://support.avaya.com/LicenseInfo/ (GENERAL LICENSE TERMS). DO NOT USE
 * THE SOFTWARE IF YOU DO NOT WISH TO BE BOUND BY THE GENERAL LICENSE TERMS. IN
 * ADDITION TO THE GENERAL LICENSE TERMS, THE FOLLOWING ADDITIONAL TERMS AND
 * RESTRICTIONS WILL TAKE PRECEDENCE AND APPLY TO THIS DEMONSTRATION SOFTWARE.
 * 
 * THIS DEMONSTRATION SOFTWARE IS PROVIDED FOR THE SOLE PURPOSE OF DEMONSTRATING
 * HOW TO USE THE SOFTWARE DEVELOPMENT KIT AND MAY NOT BE USED IN A LIVE OR
 * PRODUCTION ENVIRONMENT. THIS DEMONSTRATION SOFTWARE IS PROVIDED ON AN AS IS
 * BASIS, WITHOUT ANY WARRANTIES OR REPRESENTATIONS EXPRESS, IMPLIED, OR
 * STATUTORY, INCLUDING WITHOUT LIMITATION, WARRANTIES OF QUALITY, PERFORMANCE,
 * INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * EXCEPT FOR PERSONAL INJURY CLAIMS, WILLFUL MISCONDUCT AND END USERS VIOLATION
 * OF AVAYA OR ITS SUPPLIERS INTELLECTUAL PROPERTY RIGHTS, INCLUDING THROUGH A
 * BREACH OF THE SOFTWARE LICENSE, NEITHER AVAYA, ITS SUPPLIERS NOR END USER
 * SHALL BE LIABLE FOR (i) ANY INCIDENTAL, SPECIAL, STATUTORY, INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR FOR ANY LOSS OF PROFITS, REVENUE, OR DATA, TOLL
 * FRAUD, OR COST OF COVER AND (ii) DIRECT DAMAGES ARISING UNDER THIS AGREEMENT
 * IN EXCESS OF FIFTY DOLLARS (U.S. $50.00).
 * 
 * To the extent there is a conflict between the General License Terms, your
 * Customer Sales Agreement and the terms and restrictions set forth herein, the
 * terms and restrictions set forth herein shall prevail solely for this Utility
 * Demonstration Software.
 */

package sampleapps.clientmediastack.tty;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.avaya.common.logging.Logger;

import javax.swing.JButton;
import javax.swing.SpringLayout;

import com.avaya.api.media.channels.TtySource;
import com.avaya.mvcs.media.filter.BaudotEncoding;

/**
 * This class extends the {@link AbstractTtyGuiJPanel} to display a single text
 * field in the TTY gui. It implements the <code>TtySource</code> interface
 * and will listen for key events in the gui and then add them to the output
 * textfield and to the <code>ByteBuffer</code> when {@link #read(ByteBuffer)}
 * is called
 * 
 * @author Avaya Inc.
 * @since Mar 15, 2007
 */
public class TtyGuiOutputJPanel extends AbstractTtyGuiJPanel implements
        KeyListener, TtySource {

    private static final long serialVersionUID = -8479804729850384711L;

    private static final Logger log =
            Logger.getLogger(TtyGuiOutputJPanel.class.getName());

    protected static final String RESET_TO_LETTER_MODE_ACTION_COMMAND =
            "resetToLetters";

    private boolean ttyEnabled = false;

    private boolean open = true;

    /**
     * Used to store the characters captured by a {@link #keyTyped(KeyEvent)}
     * event. A call to {@link #read(ByteBuffer)} will read off of this one
     * character at a time
     */
    private ConcurrentLinkedQueue<Character> buffer =
            new ConcurrentLinkedQueue<Character>();

    private final JButton resetToLetterModeButton;

    public TtyGuiOutputJPanel(ActionListener resetToLettersButtonActionListener) {
        super();

        resetToLetterModeButton =
                new JButton(resource.getString("tty.resetToLetter.button"));
        resetToLetterModeButton.setToolTipText(resource.getString("tty.resetToLetter.button.toolTip"));
        resetToLetterModeButton.setActionCommand(RESET_TO_LETTER_MODE_ACTION_COMMAND);
        resetToLetterModeButton.addActionListener(resetToLettersButtonActionListener);
        resetToLetterModeButton.setFocusable(false);
        resetToLetterModeButton.setPreferredSize(PREFERRED_BUTTON_SIZE);

        layout.putConstraint(SpringLayout.WEST, resetToLetterModeButton, 5,
                SpringLayout.EAST, clearButton);
        layout.putConstraint(SpringLayout.NORTH, textField, 5,
                SpringLayout.NORTH, resetToLetterModeButton);

        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST,
                resetToLetterModeButton);
        layout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH,
                resetToLetterModeButton);

        add(resetToLetterModeButton);

        textField.addKeyListener(this);

        // super calls setFocusable(false) on the three components,
        // we want textField to be focusable and in focus
        textField.setFocusable(true);
        textField.requestFocus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) {
        // ignoring
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e) {
        // ignoring
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e) {
        char c = Character.toUpperCase(e.getKeyChar());
        if (ttyEnabled) {
            if (BaudotEncoding.isValidTtyChar(c)) {
                // then store it in buffer to be sent to the TtySource
                buffer.add(c);
                addCharToTextbox(c);
            } else {
                getToolkit().beep();
                log.info("invalid TTY char typed that cannot be sent: " + c);
                // would be nice to change the status bar here, but then
                // we'd want to change it back after a few seconds...
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
     */
    public int read(ByteBuffer dst) throws IOException {
        if (!open) {
            throw new IllegalStateException("Cannot read while closed");
        }

        Character obj = buffer.poll();

        if (obj == null) {
            return 0;
        }

        dst.putChar(obj);
        return 2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.channels.Channel#close()
     */
    public void close() throws IOException {
        open = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.channels.Channel#isOpen()
     */
    public boolean isOpen() {
        return open;
    }

    /*
     * (non-Javadoc)
     * 
     * @see sampleapps.clientmediastack.tty.AbstractTtyGuiJPanel#getForegroundColor()
     */
    @Override
    protected Color getForegroundColor() {
        return Color.BLUE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see sampleapps.clientmediastack.tty.AbstractTtyGuiJPanel#getLabel()
     */
    @Override
    protected String getLabel() {
        return resource.getString("tty.output.label");
    }

    /*
     * (non-Javadoc)
     * 
     * @see sampleapps.clientmediastack.tty.AbstractTtyGuiJPanel#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        resetToLetterModeButton.setEnabled(enabled);
        ttyEnabled = enabled;
    }
}
