/*
 * OptionsJDialog.java
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * The popup window used in {@link TutorialTtyApp} to modify the application
 * parameters
 * 
 * @author Avaya Inc.
 * @since Apr 2, 2007
 */
public final class OptionsJDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 3095223882793825192L;

    private final TutorialTtyProperties defaultProps;

    private static final ResourceBundle resource =
            ResourceBundle.getBundle("sampleapps.clientmediastack.tty.ttygui");

    private static final String ACTION_SAVE = "save";

    private static final String ACTION_CANCEL = "cancel";

    private static final String ACTION_SECURE = "secure";

    /** Only G711U and G711A are shown in the gui */
    private static final String[] SUPPORTED_CODECS = { "G711U", "G711A" };

    private static final String[] SUPPORTED_MEDIA_ENCRYPTION = { "aes", "none" };

    private static final String[] BAUDOT_BAUD_RATES = { "US", "UK" };

    private static final String[] SUPPORTED_RTP_DIRECTION = { "XMIT_RCV", "XMIT_ONLY", "RCV_ONLY" };

    // AES Config Properties
    private JTextField aesIPTextField, aesUsernameTextField, aesPortTextField,
            aesTrustStoreLocField, convLogFilenameTextField, ttyRedundancyTextField;

    private JPasswordField aesPasswordTextField;

    private JLabel aesTrustStoreLocLabel, convLogFilenameLabel;

    private JCheckBox aesSecureCheckBox, convLogEnabledCheckBox;

    // CM Config Properties
    private JTextField cmIPTextField, cmExtTextField;

    private JPasswordField cmExtPwdTextField;

    private JComboBox aesCodecComboBox, mediaEncryptComboBox,
            baudotBaudRateComboBox, rtpDirectionComboBox;

    /**
     * Loads fresh properties from the file
     * 
     * @param props
     */
    protected OptionsJDialog(JFrame owner, String title, boolean modal,
            TutorialTtyProperties props) {
        super(owner, title, modal);

        
        defaultProps = props;
        defaultProps.load();

        JPanel innerPanel = new JPanel();
        add(innerPanel);

        setResizable(false);
        innerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));

        innerPanel.add(buildCMConfigPanel());
        innerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        innerPanel.add(buildAESConfigPanel());
        innerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        innerPanel.add(buildAppConfigPanel());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton saveButton = new JButton(resource.getString("config.save"));
        saveButton.setActionCommand(ACTION_SAVE);
        saveButton.addActionListener(this);
        buttonPanel.add(saveButton);

        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton cancelButton = new JButton(resource.getString("config.cancel"));
        cancelButton.setActionCommand(ACTION_CANCEL);
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton);

        innerPanel.add(buttonPanel);

        pack();
    }

    /**
     * This method will populate the AES configuration panel
     */
    private JPanel buildAESConfigPanel() {
        JPanel aesConfigPanel = new JPanel();
        aesConfigPanel.setLayout(new GridLayout(6, 2, 5, 5));
        TitledBorder border =BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(TutorialTtyApp.AVAYA_RED),
                resource.getString("config.aes.title"),
                TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION);
        border.setTitleColor(TutorialTtyApp.AVAYA_RED);
        aesConfigPanel.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // input for IP
        JLabel aesIPLabel = new JLabel(resource.getString("config.aes.ip"));
        aesIPTextField = new JTextField(defaultProps.getAesServerIP());
        aesConfigPanel.add(aesIPLabel);
        aesConfigPanel.add(aesIPTextField);

        // input for username
        JLabel aesUsernameLabel =
                new JLabel(resource.getString("config.aes.username"));
        aesUsernameTextField = new JTextField(defaultProps.getAesUsername());
        aesConfigPanel.add(aesUsernameLabel);
        aesConfigPanel.add(aesUsernameTextField);

        // input for password
        JLabel aesPasswordLabel =
                new JLabel(resource.getString("config.aes.password"));
        aesPasswordTextField =
                new JPasswordField(defaultProps.getAesPassword());
        aesConfigPanel.add(aesPasswordLabel);
        aesConfigPanel.add(aesPasswordTextField);

        // input for port
        JLabel aesPortLabel =
                new JLabel(resource.getString("config.aes.serverPort"));
        aesPortTextField = new JTextField(defaultProps.getAesServerPort());
        aesConfigPanel.add(aesPortLabel);
        aesConfigPanel.add(aesPortTextField);

        // input for secure
        aesSecureCheckBox =
                new JCheckBox(resource.getString("config.aes.secure"));
        aesSecureCheckBox.setActionCommand(ACTION_SECURE);
        aesSecureCheckBox.addActionListener(this);
        aesConfigPanel.add(aesSecureCheckBox);
        // add a blank label for spacing
        aesConfigPanel.add(new JLabel());

        // input for trust store location
        aesTrustStoreLocLabel =
                new JLabel(resource.getString("config.aes.trustStoreLoc"));
        aesTrustStoreLocField =
                new JTextField(defaultProps.getTrustStoreLocation());
        aesConfigPanel.add(aesTrustStoreLocLabel);
        aesConfigPanel.add(aesTrustStoreLocField);

        aesSecureCheckBox.setSelected(defaultProps.isAesEncryptionEnabled());
        aesTrustStoreLocLabel.setEnabled(defaultProps.isAesEncryptionEnabled());
        aesTrustStoreLocField.setEnabled(defaultProps.isAesEncryptionEnabled());

        return aesConfigPanel;
    }

    /**
     * Creates and returns the JPanel containing the CM related configuration
     * fields
     */
    private JPanel buildCMConfigPanel() {
        JPanel cmConfigPanel = new JPanel();
        cmConfigPanel.setLayout(new GridLayout(5, 2, 5, 5));
        TitledBorder border =BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(TutorialTtyApp.AVAYA_RED),
                resource.getString("config.cm.title"),
                TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION);
        border.setTitleColor(TutorialTtyApp.AVAYA_RED);
        cmConfigPanel.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // input for CM IP
        JLabel cmIPLabel = new JLabel(resource.getString("config.cm.ip"));
        cmIPTextField = new JTextField(defaultProps.getCallServerIP());
        cmConfigPanel.add(cmIPLabel);
        cmConfigPanel.add(cmIPTextField);

        // input for ext
        JLabel cmExtLabel = new JLabel(resource.getString("config.cm.ext"));
        cmExtTextField = new JTextField(defaultProps.getExtension());
        cmConfigPanel.add(cmExtLabel);
        cmConfigPanel.add(cmExtTextField);

        // input for ext password
        JLabel cmExtPwdLabel = new JLabel(resource.getString("config.cm.pwd"));
        cmExtPwdTextField =
                new JPasswordField(defaultProps.getExtensionPassword());
        cmConfigPanel.add(cmExtPwdLabel);
        cmConfigPanel.add(cmExtPwdTextField);

        // input for codec
        JLabel aesCodecLabel = new JLabel(resource.getString("config.cm.codec"));
        aesCodecComboBox = new JComboBox(SUPPORTED_CODECS);
        aesCodecComboBox.setToolTipText(resource.getString("config.cm.codec.toolTip"));
        cmConfigPanel.add(aesCodecLabel);
        cmConfigPanel.add(aesCodecComboBox);

        // input for media encryption (boolean checkbox)
        JLabel mediaEncryptLabel = new JLabel(resource.getString("config.cm.mediaEncrypt"));
        mediaEncryptComboBox = new JComboBox(SUPPORTED_MEDIA_ENCRYPTION);
        mediaEncryptComboBox.setToolTipText(resource.getString("config.cm.mediaEncrypt.toolTip"));
        cmConfigPanel.add(mediaEncryptLabel);
        cmConfigPanel.add(mediaEncryptComboBox);

        aesCodecComboBox.setSelectedItem(defaultProps.getCodec());
        mediaEncryptComboBox.setSelectedItem(defaultProps.getMediaEncryption().toLowerCase());

        return cmConfigPanel;
    }

    /**
     * Creates and returns the JPanel containing the CM related configuration
     * fields
     */
    private JPanel buildAppConfigPanel() {
        JPanel appConfigPanel = new JPanel();
        appConfigPanel.setLayout(new GridLayout(5, 2, 5, 5));

        TitledBorder border =BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(TutorialTtyApp.AVAYA_RED),
                resource.getString("config.app.title"),
                TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION);
        border.setTitleColor(TutorialTtyApp.AVAYA_RED);
        appConfigPanel.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // RTP direction
        JLabel rtpDirectionLabel = new JLabel(resource.getString("config.app.rtpDirection"));
        rtpDirectionComboBox = new JComboBox(SUPPORTED_RTP_DIRECTION);
        rtpDirectionComboBox.setToolTipText(resource.getString("config.app.rtpDirection.toolTip"));
        appConfigPanel.add(rtpDirectionLabel);
        appConfigPanel.add(rtpDirectionComboBox);
        rtpDirectionComboBox.setSelectedItem(defaultProps.getRtpDirection());

        // Baudot Baud Rate
        JLabel baudotBaudRateLabel = new JLabel(resource.getString("config.app.baudotBaudRate"));
        baudotBaudRateComboBox = new JComboBox(BAUDOT_BAUD_RATES);
        baudotBaudRateComboBox.setToolTipText(resource.getString("config.app.baudotBaudRate.toolTip"));
        appConfigPanel.add(baudotBaudRateLabel);
        appConfigPanel.add(baudotBaudRateComboBox);
        baudotBaudRateComboBox.setSelectedItem(defaultProps.getBaudotBaudRate());

        // input for TTY redundancy
        JLabel ttyRedundancyLabel =
                new JLabel(resource.getString("config.app.ttyRedundancy"));
        ttyRedundancyTextField = new JTextField(defaultProps.getTtyRedundancy());
        appConfigPanel.add(ttyRedundancyLabel);
        appConfigPanel.add(ttyRedundancyTextField);

        // enable TTY conversation logging checkbox
        convLogEnabledCheckBox = new JCheckBox(resource.getString("config.app.convLog.enabled"));
        convLogEnabledCheckBox.addActionListener(this);
        convLogEnabledCheckBox.setToolTipText(resource.getString("config.app.convLog.enabled.toolTip"));
        appConfigPanel.add(convLogEnabledCheckBox);
        boolean enabled = defaultProps.isConvLogEnabled();
        convLogEnabledCheckBox.setSelected(enabled);
        
        // add a blank label for spacing
        appConfigPanel.add(new JLabel());

        // filename for TTY conversation logging
        convLogFilenameLabel = new JLabel(resource.getString("config.app.convLog.filename"));
        convLogFilenameTextField = new JTextField(defaultProps.getConvLogFilename());
        convLogFilenameTextField.setToolTipText(resource.getString("config.app.convLog.filename.toolTip"));
        appConfigPanel.add(convLogFilenameLabel);
        appConfigPanel.add(convLogFilenameTextField);
        convLogFilenameLabel.setEnabled(enabled);
        convLogFilenameTextField.setEnabled(enabled);

        return appConfigPanel;
    }

    /**
     * Saves all the changes made in the form and calls store on the props
     * 
     */
    private void saveChanges() {
        try {
            // Save the CM properties
            defaultProps.setCallServerIP(cmIPTextField.getText());
            defaultProps.setExtension(cmExtTextField.getText());
            defaultProps.setExtensionPassword(new String(
                    cmExtPwdTextField.getPassword()));

            defaultProps.setCodec((String) aesCodecComboBox.getSelectedItem());
            defaultProps.setMediaEncryption((String) mediaEncryptComboBox.getSelectedItem());

            // Save the AES properties
            defaultProps.setAesServerIP(aesIPTextField.getText());
            defaultProps.setAesUsername(aesUsernameTextField.getText());
            defaultProps.setAesPassword(new String(
                    aesPasswordTextField.getPassword()));
            defaultProps.setAesServerPort(aesPortTextField.getText());
            defaultProps.setAesEncryptionEnabled(aesSecureCheckBox.isSelected());
            defaultProps.setTrustStoreLocation(aesTrustStoreLocField.getText());

            // Save the application properties
            defaultProps.setRtpDirection((String) rtpDirectionComboBox.getSelectedItem());
            defaultProps.setBaudotBaudRate((String) baudotBaudRateComboBox.getSelectedItem());
            defaultProps.setTtyRedundancy(ttyRedundancyTextField.getText());
            defaultProps.setConvLogEnabled(convLogEnabledCheckBox.isSelected());
            defaultProps.setConvLogFilename(convLogFilenameTextField.getText());

            defaultProps.store();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    resource.getString("error.options.store.exception.msg")
                            + "\n\n" + e,
                    resource.getString("error.options.store.exception.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(ACTION_SAVE)) {
            saveChanges();
            this.dispose();
        } else if (event.getActionCommand().equals(ACTION_CANCEL)) {
            this.dispose();
        } else if (event.getActionCommand().equals(ACTION_SECURE)) {
            aesTrustStoreLocLabel.setEnabled(aesSecureCheckBox.isSelected());
            aesTrustStoreLocField.setEnabled(aesSecureCheckBox.isSelected());
        } else if (event.getSource().equals(convLogEnabledCheckBox)) {
            convLogFilenameLabel.setEnabled(convLogEnabledCheckBox.isSelected());
            convLogFilenameTextField.setEnabled(convLogEnabledCheckBox.isSelected());
        }
    }
}
