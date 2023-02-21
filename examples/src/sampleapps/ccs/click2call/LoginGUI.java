/*
 * LoginGUI.java
 * 
 * Copyright (c) 2002-2008 Avaya Inc. All rights reserved.
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

package sampleapps.ccs.click2call;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Displays the login screen
 * 
 * @author Avaya Inc.
 */
public final class LoginGUI extends JFrame {
    private static final long serialVersionUID = 4130203790351673371L;

    private final Click2Call c2c;

    private JTextField user;

    private JPasswordField passwd;

    /**
     * Constructor used to perform JFrame setup and invoke the displaying of the
     * Login GUI.
     * 
     * @param instance - Click2Call class instance.
     */
    protected LoginGUI(final Click2Call instance) {
        super("Click2Call Login");
        c2c = instance;
        displayGUI();
    }

    /**
     * Setup and display the Login GUI
     */
    private void displayGUI() {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        // Load image
        URL url = LoginGUI.class.getClassLoader().getResource("avaya_logo.gif");
        Icon image = new ImageIcon(url);
        JLabel logo = new JLabel(image, JLabel.LEFT);

        // Set Title
        JLabel title = new JLabel("Click2Call Login");
        title.setFont(new Font("", Font.BOLD, 20));

        // Create Login Fields
        JLabel uLabel = new JLabel("Username: ");
        user = new JTextField(20);

        // Create Password Fields
        JLabel pLabel = new JLabel("Password: ");
        passwd = new JPasswordField(20);
        passwd.setEchoChar('*');
        passwd.enableInputMethods(true);

        // Create Buttons
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new LoginHandler());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new CancelHandler());

        // Create Layout Panels
        // User
        JPanel jp1 = new JPanel();
        jp1.setLayout(new FlowLayout());
        jp1.setBackground(Color.white);
        jp1.add(uLabel);
        jp1.add(user);

        // Password
        JPanel jp2 = new JPanel();
        jp2.setLayout(new FlowLayout());
        jp2.setBackground(Color.white);
        jp2.add(pLabel);
        jp2.add(passwd);

        // Buttons
        JPanel jp3 = new JPanel();
        jp3.setLayout(new FlowLayout());
        jp3.setBackground(Color.white);
        jp3.add(loginButton);
        jp3.add(cancelButton);

        // Title
        JPanel jp4 = new JPanel();
        jp4.setLayout(new FlowLayout());
        jp4.setBackground(Color.white);
        jp4.add(title);

        // Combine Panels
        JPanel jp5 = new JPanel();
        jp5.setLayout(new FlowLayout());
        jp5.setBackground(Color.white);
        jp5.add(jp4);
        jp5.add(jp1);
        jp5.add(jp2);
        jp5.add(jp3);

        // Image
        JPanel jp6 = new JPanel();
        jp6.setLayout(new GridLayout(1, 1));
        jp6.setBackground(Color.white);
        jp6.add(logo);

        // Add Panes to Frame
        pane.add(jp5, BorderLayout.CENTER);
        pane.add(jp6, BorderLayout.NORTH);

        // Allow user to use enter to login
        getRootPane().setDefaultButton(loginButton);

        setLocation(400, 400);
        setSize(400, 300);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JFrame.setDefaultLookAndFeelDecorated(true);
    }

    /**
     * Error Message GUI to display during a login error.
     */
    public void failed() {
        JOptionPane.showMessageDialog(null, "Authentication Failed: \n Incorrect Username or Password");
    }

    /**
     * LoginHandler class used to verify the entering of a user name and
     * password, and invokes the start of the Click2Call process.
     */
    private final class LoginHandler implements ActionListener {
        public void actionPerformed(final ActionEvent event) {
            StringBuffer inputUser = new StringBuffer(user.getText());
            StringBuffer inputPasswd = new StringBuffer();
            char[] tempPasswd = passwd.getPassword();
            inputPasswd.append(tempPasswd);

            // Clear out tempPasswd for security reasons
            for (int i = 0; i < tempPasswd.length; i++) {
                tempPasswd[i] = 0;
            }

            if ((inputUser.length() > 0) && (inputPasswd.length() > 0)) {
                setVisible(false);
                c2c.startup(inputUser, inputPasswd);
            } else {
                JOptionPane.showMessageDialog(null, "Username and Password Required");
            }
        }
    }

    /**
     * CancelHandler class used to exit the application when cancel is pressed
     * on the LoginGUI.
     * 
     */
    private static final class CancelHandler implements ActionListener {
        public void actionPerformed(final ActionEvent event) {
            System.exit(0);
        }
    }
}
