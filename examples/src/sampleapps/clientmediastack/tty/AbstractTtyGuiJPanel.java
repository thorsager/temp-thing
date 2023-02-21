/*
 * AbstractTtyGuiJPanel.java
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

/**
 * This abstract JPanel is intended to be extended by the
 * {@link TtyGuiInputJPanel} and {@link TtyGuiOutputJPanel} classes to combine
 * common functionality. This JPanel will create a single textbox that will not
 * be directly editable. The addCharToTextbox method is required to be called to
 * add text into the textbox. If a backspace char is sent to the method the
 * previously entered character will be deleted
 * 
 * @author Avaya Inc.
 * @since Mar 15, 2007
 */
public abstract class AbstractTtyGuiJPanel extends JPanel {

    protected static final ResourceBundle resource =
            ResourceBundle.getBundle("sampleapps.clientmediastack.tty.ttygui");

    protected static final Dimension PREFERRED_BUTTON_SIZE = new Dimension(70,27);

    private static final String ACTION_CLEAR = "clear";

    protected final JTextField textField;

    protected final JLabel textFieldLabel;
    
    protected final JButton clearButton;
    
    protected final SpringLayout layout;

    private final ActionListener clearButtonActionListener;

    /**
     * Creates the Panel as well as the JLabel, JTextField and JButton
     */
    public AbstractTtyGuiJPanel() {
        layout = new SpringLayout();
        setLayout(layout);

        textFieldLabel = new JLabel(getLabel() + ": ");
        textField = new JTextField(45);

        // Create some labels for the fields.
        textFieldLabel.setLabelFor(textField);
        Dimension dim = new Dimension(60, 20);
        textFieldLabel.setPreferredSize(dim);
        textFieldLabel.setMaximumSize(dim);
        textFieldLabel.setMinimumSize(dim);
        // set the layout for 1 row and 2 columns
        add(textFieldLabel);

        // Create the input textfield where the input and output is sent
        textField.setForeground(getForegroundColor());
        // this field is not editable, we will catch keyboard events and append
        // the TTY data ourselves in order to have more control of this field
        textField.setEditable(false);
        add(textField);

        clearButton = new JButton(resource.getString("tty.clear.button"));
        clearButton.setActionCommand(ACTION_CLEAR);
        clearButtonActionListener = new ClearButtonActionListener();
        clearButton.addActionListener(clearButtonActionListener);
        clearButton.setPreferredSize(PREFERRED_BUTTON_SIZE);
        add(clearButton);

        layout.putConstraint(SpringLayout.WEST, textFieldLabel, 5,
                SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, textFieldLabel, 5,
                SpringLayout.NORTH, this);

        layout.putConstraint(SpringLayout.WEST, textField, 5,
                SpringLayout.EAST, textFieldLabel);
        layout.putConstraint(SpringLayout.NORTH, textField, 5,
                SpringLayout.NORTH, textFieldLabel);

        layout.putConstraint(SpringLayout.WEST, clearButton, 5,
                SpringLayout.EAST, textField);
        layout.putConstraint(SpringLayout.NORTH, textField, 5,
                SpringLayout.NORTH, clearButton);

        // Adjust constraints for the content pane: Its right
        // edge should be 5 pixels beyond the button's right
        // edge, and its bottom edge should be 5 pixels beyond
        // the bottom edge of the tallest component (which we'll
        // assume is the clearButton).
        layout.putConstraint(SpringLayout.EAST, this, 5,
                SpringLayout.EAST, clearButton);
        layout.putConstraint(SpringLayout.SOUTH, this, 5,
                SpringLayout.SOUTH, clearButton);
        
        // we don't want focus by default
        setFocusable(false);
    }

    /**
     * This method is required to be implemented by any extending classes. The
     * returned String will be the Label used for the TextField
     */
    protected abstract String getLabel();

    /**
     * Provides the ability to give a different font color
     */
    protected abstract Color getForegroundColor();

    /**
     * Adds the given char to the JTextfield
     */
    protected final void addCharToTextbox(final char c) {
        String currVal = textField.getText();
        if (c == 8) {
            // delete button pressed, if there is a char to delete
            if (currVal.length() > 0) {
                textField.setText(currVal.substring(0, currVal.length() - 1));
            }
        } else {
            // This isn't the greatest scrolling, but it will work for now
            if (currVal.length() > (textField.getColumns())) {
                // then delete one char off the front
                currVal = currVal.substring(1);
            }
            textField.setText(currVal + c);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        textField.setEnabled(enabled);
        textFieldLabel.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        
        if (enabled)
            textField.setBackground(Color.WHITE);
        else
            textField.setBackground(Color.LIGHT_GRAY);
    }

    /*
     * (non-Javadoc)
     * @see java.awt.Component#setFocusable(boolean)
     */
    @Override
    public void setFocusable(boolean focusable) {
        super.setFocusable(focusable);
        // set the focus for the subelements in the panel
        textField.setFocusable(focusable);
        textFieldLabel.setFocusable(focusable);
        clearButton.setFocusable(focusable);
    }
    
    /**
     * This will clear the JTextfield of all text
     */
    public void clearText() {
        textField.setText("");
    }

    /**
     *  Used to listen for when the clear button is pressed so we can clear the
     *  text field 
     */
    private final class ClearButtonActionListener implements ActionListener {

        /*
         * (non-Javadoc)
         * 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(ACTION_CLEAR)) {
                clearText();
            }
        }
    }
}
