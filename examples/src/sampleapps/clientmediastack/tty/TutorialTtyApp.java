/*
 * TutorialTtyApp.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.avaya.common.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * This class is the center of the application. It delegates all telephony
 * related tasks to {@link TutorialTtyPhone}. TutorialTtyPhone simply calls
 * methods on this object to update the state.
 * <p>
 * The GUI contains two primary panels, the imput panel and output panel. The
 * input panel implments the <code>TtySink</code> interface and will display
 * all TTY data that is received. The output panel implements the
 * <code>TtySource</code> interface and will intercept key events as TTY chars
 * to be sent out. The captured chars will be displayed in the output textfield
 * and buffered to be read out one char at a time by the Client Media Stack.
 * <p>
 * After running this application the options will need to be configured before
 * you can successfuly register. The options can be edited in the GUI by
 * clicking File->Options in the menu. Once configuration has been completed
 * simply click System->Register in the menu. This will register a DMCC station
 * with client media coming to the appication. The status field at the bottom of
 * the application will inform you once you have successfully registered,
 * otherwise an error message will popup with information about why registration
 * failed.
 * <p>
 * When the application is registered the DMCC station can then receive calls or
 * place calls. To make a call click on "Make a Call" in the System menu. A
 * popup window will be displayed that will allow you to enter the phone number
 * to dial.  Clicking ok will initiate the call.  When the far end picks up
 * then the application will go to the connected state.
 * <p>
 * In the connected state there should be two way audio and if the call is 
 * connected to a TTY capable device the application will be able to send
 * and receive TTY characters.
 *  
 * @author Avaya Inc.
 * @since Apr 2, 2007
 */
public final class TutorialTtyApp implements ActionListener {
    
    private static final Logger log = Logger.getLogger(TutorialTtyApp.class.getName());

    private final JPanel mainPanel;

    private static final ResourceBundle resource =
            ResourceBundle.getBundle("sampleapps.clientmediastack.tty.ttygui");

    protected static final Color AVAYA_RED = new Color(0xcc0001);
    
    private static final String ACTION_EXIT = "exit";

    private static final String ACTION_OPTIONS = "options";
    

    /** Used to store a reference to the containing frame */
    private final JFrame mainFrame;

    private final TutorialTtyProperties tutorialTtyProps;

    private final TtyGuiInputJPanel ttyGuiInputJPanel;

    private final TtyGuiOutputJPanel ttyGuiOutputJPanel;

    private final JMenuBar menuBar;
    
    private JMenuItem optionsItem, registerItem, unregisterItem, connectItem,
            disconnectItem;
    
    private JLabel status;
    
    private JCheckBoxMenuItem ttyEnabledItem;
    
    private enum State {UNREGISTERED, REGISTERING, REGISTERED, CONNECTED, DISCONNECTED};
    
    private TutorialTtyPhone phone;
    
    /**
     * This SingleThreadExecutor is used to share a single thread that will
     * be used to offload work to be done from the GUI event thread
     */
    private Executor executor = Executors.newSingleThreadExecutor();
    
    /**
     * 
     * @param frame The parent frame that will contain this 
     * @throws IOException
     */
    protected TutorialTtyApp(JFrame frame) throws IOException {
        this.mainFrame = frame;
        
        tutorialTtyProps = new TutorialTtyProperties();
        tutorialTtyProps.load();
        
        mainPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        ttyGuiInputJPanel = new TtyGuiInputJPanel();
        ttyGuiOutputJPanel = new TtyGuiOutputJPanel(this);
        
        ttyGuiInputJPanel.setFocusable(false);

        mainPanel.add(ttyGuiInputJPanel);
        mainPanel.add(ttyGuiOutputJPanel);
        
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        status = new JLabel();
        status.setAlignmentY(Component.TOP_ALIGNMENT);
        statusPanel.add(status, BorderLayout.WEST);
        
        // add the Avaya logo
        URL avayaLogoURL = ClassLoader.getSystemResource("avaya_logo.gif");
        if (avayaLogoURL != null) {
            ImageIcon avayaLogo = new ImageIcon(avayaLogoURL);
            JLabel avayaLogoLabel = new JLabel();
            avayaLogoLabel.setIcon(avayaLogo);
            statusPanel.add(avayaLogoLabel, BorderLayout.EAST);
        }
        
        mainPanel.add(statusPanel);

        menuBar = new JMenuBar();
        buildMenuBar();
        
        setState(State.UNREGISTERED);
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event-dispatching thread.
     * 
     * @throws IOException
     */
    private static void createAndShowGUI() throws IOException {

        // Create and set up the window.
        JFrame frame = new JFrame(resource.getString("ttyapp.title"));

        // Create an instance
        TutorialTtyApp instance = new TutorialTtyApp(frame);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setJMenuBar(instance.menuBar);
        frame.setContentPane(instance.mainPanel);
        frame.addKeyListener(instance.ttyGuiOutputJPanel);

        
        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Performs a clean exit of the application
     */
    private void exit() {
        if (phone!=null)
            phone.cleanup();
        System.exit(0);
    }

    /**
     * This method will attempt to register the station with the AES.  If there
     * is a failure attempting to register than a dialog box will be displayed
     * showing the error and the state of the application will not change.
     * Assumes that the state is UNREGISTERED
     */
    private void registerAction() {
        setState(State.REGISTERING);
        phone = new TutorialTtyPhone(this, tutorialTtyProps);
        // run the register in a thread other than the gui event thread
        Runnable run = new Runnable() {
            public void run() {
                try {
                    // do the registration
                    phone.bootstrap();
                    phone.start(ttyGuiInputJPanel, ttyGuiOutputJPanel);

                } catch (Exception e) {
                    setState(State.UNREGISTERED);
                    // if there are any failures then display a dialog box
                    JOptionPane.showMessageDialog(mainPanel, resource
                            .getString("error.register.exception.msg")
                            + "\n\n" + e, resource
                            .getString("error.register.exception.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        executor.execute(run);
    }

    /**
     * Unregisters the app from AES.  Assumes the state is REGISTERED
     */
    private void unregisterAction() {
        // run the unregister in a thread besides the gui event thread
        Runnable run = new Runnable() {
            public void run() {
                mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (phone!=null) {
                    phone.cleanup();
                    phone=null;
                }
                setState(State.UNREGISTERED);
            }
        };
        executor.execute(run);
    }
    
    /**
     * This method will attempt to register the device with the AES and CM.
     * Assumes that the state before calling is REGISTERED
     */
    private void connectAction() {
        // run the connect in a thread besides the gui event thread
        Runnable run = new Runnable() {
            public void run() {
                try {
                    // attempt to call the given number
                    String number =
                            JOptionPane.showInputDialog(mainPanel,
                                    resource.getString("connectDialog.msg"),
                                    resource.getString("connectDialog.title"),
                                    JOptionPane.QUESTION_MESSAGE);
                    
                    if (number!=null) {
                        phone.makeCall(number);
                    }
                } catch (Exception e) {
                    // if there are any failures then display a dialog box
                    JOptionPane.showMessageDialog(mainPanel,
                            resource.getString("error.connect.exception.msg") + "\n\n"
                                    + e,
                            resource.getString("error.connect.exception.title"),
                            JOptionPane.ERROR_MESSAGE);
                }  
            }
        };
        executor.execute(run);
    }

    /**
     * Will disconnect the active call.  It is assuming that the current state
     * is State.CONNECTED
     */
    private void disconnectAction() {
        phone.disconnect();
        setState(State.DISCONNECTED);
    }

    /**
     * This method is responsible for maintaining disabled and enabled states
     * for each of the GUI components when the application state changes
     */
    private void setState(State applicationState) {
        // set everything to false by default, only setting to true if in the
        // correct state
        optionsItem.setEnabled(false);
        registerItem.setEnabled(false);
        unregisterItem.setEnabled(false);
        connectItem.setEnabled(false);
        disconnectItem.setEnabled(false);
        ttyGuiInputJPanel.setEnabled(false);
        ttyGuiOutputJPanel.setEnabled(false);
        ttyEnabledItem.setEnabled(false);
        mainFrame.setCursor(Cursor.getDefaultCursor());

        switch (applicationState) {
        case UNREGISTERED:
            status.setText(resource.getString("status.unregistered"));
            optionsItem.setEnabled(true);
            registerItem.setEnabled(true);
            break;
        case REGISTERING:
            mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            status.setText(resource.getString("status.register.wait"));
            break;
        case REGISTERED:
        case DISCONNECTED:
            status.setText(resource.getString("status.registered") + " : "
                    + tutorialTtyProps.getExtension());
            unregisterItem.setEnabled(true);
            connectItem.setEnabled(true);
            ttyEnabledItem.setEnabled(true);
            break;
        case CONNECTED:
            status.setText(resource.getString("status.connected"));
            disconnectItem.setEnabled(true);
            ttyGuiInputJPanel.setEnabled(true);
            ttyGuiOutputJPanel.setEnabled(true);
            ttyEnabledItem.setEnabled(true);
            // clear the input and output textfields
            ttyGuiInputJPanel.clearText();
            ttyGuiOutputJPanel.clearText();
            break;
        }
    }

    private void buildMenuBar() {
        // Where the GUI is created:
        JMenu fileMenu, systemMenu;
        JMenuItem exitItem;

        // Build the File menu
        fileMenu = new JMenu(resource.getString("menu.file"));
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.file.desc"));
        menuBar.add(fileMenu);

        // options
        optionsItem =
                new JMenuItem(resource.getString("menu.file.options"),
                        KeyEvent.VK_O);
        optionsItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.file.options.desc"));
        optionsItem.setActionCommand(ACTION_OPTIONS);
        optionsItem.addActionListener(this);
        fileMenu.add(optionsItem);

        fileMenu.addSeparator();

        // options
        exitItem =
                new JMenuItem(resource.getString("menu.file.exit"),
                        KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,
                KeyEvent.ALT_DOWN_MASK));
        exitItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.file.exit.desc"));
        exitItem.setActionCommand(ACTION_EXIT);
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);

        // Build the system menu
        systemMenu = new JMenu(resource.getString("menu.system"));
        systemMenu.setMnemonic(KeyEvent.VK_S);
        systemMenu.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.system.desc"));
        menuBar.add(systemMenu);

        // register
        registerItem =
                new JMenuItem(resource.getString("menu.system.register"),
                        KeyEvent.VK_R);
        registerItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.system.register.desc"));
        registerItem.addActionListener(this);
        systemMenu.add(registerItem);
        
        // unregister
        unregisterItem =
                new JMenuItem(resource.getString("menu.system.unregister"),
                        KeyEvent.VK_U);
        unregisterItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.system.unregister.desc"));
        unregisterItem.addActionListener(this);
        unregisterItem.setEnabled(false);
        systemMenu.add(unregisterItem);
        
        systemMenu.addSeparator();
       
        // connect
        connectItem =
                new JMenuItem(resource.getString("menu.system.connect"),
                        KeyEvent.VK_M);
        connectItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.system.connect.desc"));
        connectItem.addActionListener(this);
        connectItem.setEnabled(false);
        systemMenu.add(connectItem);

        // disconnect
        disconnectItem =
                new JMenuItem(resource.getString("menu.system.disconnect"),
                        KeyEvent.VK_H);
        disconnectItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.system.disconnect.desc"));
        disconnectItem.setEnabled(false);
        disconnectItem.addActionListener(this);
        systemMenu.add(disconnectItem);
        
        systemMenu.addSeparator();
        
        ttyEnabledItem = new JCheckBoxMenuItem(
                        resource.getString("menu.system.ttyEnabled"), true);
        ttyEnabledItem.getAccessibleContext().setAccessibleDescription(
                resource.getString("menu.system.ttyEnabled.desc"));
        ttyEnabledItem.addActionListener(this);
        ttyEnabledItem.setMnemonic(KeyEvent.VK_T);
        systemMenu.add(ttyEnabledItem);
    }
    
    /**
     * This method is called by the {@link TutorialTtyPhone} when it receives
     * the mediaStarted event.  We need it here to do GUI updates
     */
    public void mediaStarted() {
        log.info("media has started");
    }
    
    /**
     * This method is called by the {@link TutorialTtyPhone} when it successfully
     * registers
     */
    public void registered() {
        log.fine("successfully registered");
        
        setState(State.REGISTERED);
    }
    
    public void registerFailed(String reason) {
        log.fine("registration failed: " + reason);
        
        setState(State.UNREGISTERED);
        mainFrame.setCursor(Cursor.getDefaultCursor());
        
        JOptionPane.showMessageDialog(mainPanel,
                resource.getString("error.register.exception.msg") + "\n\n"
                        + reason,
                resource.getString("error.register.exception.title"),
                JOptionPane.ERROR_MESSAGE);
    }
    
    public void unregistered(String reason) {
        log.fine("station unregistered: " + reason);
        
        setState(State.UNREGISTERED);
        JOptionPane.showMessageDialog(mainPanel,
                resource.getString("error.unregistered.msg") + "\n\n"
                        + reason,
                resource.getString("error.unregistered.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * This method is called when a call has been successfully established
     */
    public void connected() {
        log.fine("call connected");
        setState(State.CONNECTED);
    }
    
    public void disconnected() {
        log.fine("call disconnected");
        setState(State.DISCONNECTED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ACTION_OPTIONS)) {
            new OptionsJDialog(mainFrame, resource.getString("config.title"),
                    true, tutorialTtyProps).setVisible(true);
        } else if (e.getActionCommand().equals(ACTION_EXIT)) {
            exit();
        } else if (e.getSource().equals(connectItem)) {
            connectAction();
        } else if (e.getSource().equals(disconnectItem)) {
            disconnectAction();
        } else if (e.getSource().equals(registerItem)) {
            registerAction();
        } else if (e.getSource().equals(unregisterItem)) {
            unregisterAction();
        } else if (e.getSource().equals(ttyEnabledItem)) {
            if (phone!=null) {
                phone.setTtyEnabled(((JCheckBoxMenuItem) e.getSource()).isSelected(), 0);
            } else {
                // we can't actually switch tty if phone hasn't been enabled yet
                // so we need to switch it back
                ttyEnabledItem.setSelected(!ttyEnabledItem.isSelected());
            }
        } else if (e.getActionCommand().equals(
                TtyGuiOutputJPanel.RESET_TO_LETTER_MODE_ACTION_COMMAND)) {
            if (phone!=null) {
                phone.resetTTYBackToLetterMode();
            }
        }
    }

    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        });
    }
}
