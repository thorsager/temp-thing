/*
 * TtyGuiInputJPanel.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.avaya.common.logging.Logger;

import javax.swing.JButton;
import javax.swing.SpringLayout;

import com.avaya.api.media.channels.TtySink;
import com.avaya.mvcs.media.filter.BaudotEncoding;

/**
 * This class implements the Input panel used in the GUI.  By implementing the
 * <code>TtySink</code> an instance of this object can then be attached to a
 * <code>MediaSession</code> to receive TTY data.  The received TTY will be
 * put into the textfield contained in this class
 *  
 * @author Avaya Inc.
 * @since Mar 15, 2007
 */
public class TtyGuiInputJPanel extends AbstractTtyGuiJPanel implements TtySink {
    
    private static final long serialVersionUID = -7449851876154458998L;
    
    private static final Logger log = Logger.getLogger(TtyGuiInputJPanel.class.getName());

    private final JButton flipButton;
    
    public TtyGuiInputJPanel() {
        super();
        
        flipButton = new JButton(resource.getString("tty.flip.button"));
        flipButton.setToolTipText(resource.getString("tty.flip.button.toolTip"));
        flipButton.addActionListener(new FlipButtonActionListener());
        flipButton.setFocusable(false);
        flipButton.setPreferredSize(PREFERRED_BUTTON_SIZE);
        
        layout.putConstraint(SpringLayout.WEST, flipButton, 5,
                SpringLayout.EAST, clearButton);
        layout.putConstraint(SpringLayout.NORTH, textField, 5,
                SpringLayout.NORTH, flipButton);
        
        layout.putConstraint(SpringLayout.EAST, this, 5,
                SpringLayout.EAST, flipButton);
        layout.putConstraint(SpringLayout.SOUTH, this, 5,
                SpringLayout.SOUTH, flipButton);

        add(flipButton);
    }

    /*
     * (non-Javadoc)
     * @see sampleapps.clientmediastack.tty.AbstractTtyGuiJPanel#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        flipButton.setEnabled(enabled);
    }

    /*
     * (non-Javadoc)
     * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
     */
    public int write(ByteBuffer src) throws IOException {
        int bytesWritten = 0;
        // read from the bytebuffer and add to the textbox
        while(src.position()+2 <= src.limit()) {
            char c = src.getChar();
            if (BaudotEncoding.isValidTtyChar(c)) {
                // then write it to the text field
                addCharToTextbox(c);
            } else {
                log.fine("invalid TTY char received: " + c);
            }
            bytesWritten += 2;
        }
        return bytesWritten;
    }

    /*
     * (non-Javadoc)
     * @see java.nio.channels.Channel#close()
     */
    public void close() throws IOException {
        log.fine("closing");
    }

    /*
     * (non-Javadoc)
     * @see java.nio.channels.Channel#isOpen()
     */
    public boolean isOpen() {
        return true;
    }

    /* (non-Javadoc)
     * @see sampleapps.clientmediastack.tty.AbstractTtyGuiJPanel#getForegroundColor()
     */
    @Override
    protected Color getForegroundColor() {
        return Color.RED;
    }

    /* (non-Javadoc)
     * @see sampleapps.clientmediastack.tty.AbstractTtyGuiJPanel#getLabel()
     */
    @Override
    protected String getLabel() {
        return resource.getString("tty.input.label");
    }

    /**
     * This will flip all of the text in the buffer
     *
     */
    private void flipText() {
        StringBuilder sb = new StringBuilder();
        for(char c : textField.getText().toCharArray()) {
            sb.append(BaudotEncoding.flipChar(c));
        }
        textField.setText(sb.toString());
    }
    
    /**
     *  Used to listen for when the flip button is pressed
     */
    private final class FlipButtonActionListener implements ActionListener {
        /*
         * (non-Javadoc)
         * 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(flipButton)) {
                flipText();
            }
        }
    }
}
