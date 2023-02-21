/*
 * Click2CallGUI.java
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
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import ch.ecma.csta.binding.ConnectionID;

/**
 * This class creates and manages call log GUI of Click2Call application.
 * 
 * @author Avaya Inc.
 */
public final class Click2CallGUI extends JFrame {

    private static final long serialVersionUID = 6679398171602774532L;

    /** Table displaying incoming call data. */
    private JTable table; 

    private MyTableModel myTableModel;

    private static final Color BUTTON_BKGD_COLOR = new Color(80, 90, 170);

    private static final Color MISSED_CALL_COLOR = new Color(240, 200, 200);

    private static final Color DIALED_CALL_COLOR = new Color(255, 200, 100);

    private static final Color CALLED_BACK_CALL_COLOR = new Color(220, 180, 140);
    
    /** Index of the column displaying phone number of caller. */
    public static final int NUMBER_COLUMN_INDEX = 0;

    /** Index of the column displaying time of received call. */
    public static final int TIME_COLUMN_INDEX = 1;

    /** Index of the column displaying status (answered/missed) of received call. */
    public static final int STATUS_COLUMN_INDEX = 2;

    /** Total number of columns in table displaying received calls. */
    public static final int TABLE_COLUMN_COUNT = 3;

    private CallStatus myCallStatus = null;

    private boolean returningCall = false;

    /**
     * This is the constructor to get action listener which will listen to
     * events generated by this GUI and the two dimensional String array
     * containing initialization data.
     */
    protected Click2CallGUI(final ActionListener actionListener, final String[][] log) {
        super("Click2Call");

        // obtain the ContentPane and set BorderLayout.
        Container contentpane = getContentPane();
        contentpane.setLayout(new BorderLayout());

        // Set table header and data.
        String[] tableHeader = { "Phone Number", "Call Time", "Call Status" };
        String[][] tableData;

        // if log is null, display empty table otherwise initialize the JTable
        // with data in log file.
        if (log == null) {
            tableData = new String[0][0];
        } else {
            tableData = log;
        }

        table = new JTable();

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set table model, foreground, background, font etc.
        myTableModel = new MyTableModel(tableData, tableHeader);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setFont(new Font(table.getTableHeader().getFont().getName(), Font.BOLD, 10));
        table.setGridColor(BUTTON_BKGD_COLOR);
        table.setModel(myTableModel);

        // set table cell renderer for using custom colors in table cells
        // depending upon cell data.
        table.setDefaultRenderer(table.getColumnClass(0), new ColoredTableCellRenderer());

        // create scroll pane, add table to it.
        int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;

        JScrollPane jsp = new JScrollPane(table, v, h);

        jsp.getViewport().setBackground(Color.WHITE);

        // Create a panel and add various buttons to it. Actions generated
        // by these buttons are sent to action_listener.
        JPanel jp1 = new JPanel();
        jp1.setLayout(new FlowLayout());
        jp1.setBackground(Color.white);

        JButton jb = new JButton("Call Back");
        jb.setActionCommand("callback");
        jb.addActionListener(actionListener);
        jb.setBackground(BUTTON_BKGD_COLOR);
        jb.setForeground(Color.WHITE);
        jp1.add(jb);

        jb = new JButton("Delete");
        jb.setActionCommand("delete");
        jb.addActionListener(actionListener);
        jb.setBackground(BUTTON_BKGD_COLOR);
        jb.setForeground(Color.WHITE);
        jp1.add(jb);

        jb = new JButton("Delete All");
        jb.setActionCommand("deleteall");
        jb.addActionListener(actionListener);
        jb.setBackground(BUTTON_BKGD_COLOR);
        jb.setForeground(Color.WHITE);
        jp1.add(jb);

        jb = new JButton("Single Step Conference");
        jb.setActionCommand("singleStepConf");
        jb.addActionListener(actionListener);
        jb.setBackground(BUTTON_BKGD_COLOR);
        jb.setForeground(Color.WHITE);
        jp1.add(jb);

        JPanel jp2 = new JPanel();
        JLabel jl = new JLabel("Please select a table row" + " and click \"Call Back\" to make the call.");
        jl.setForeground(BUTTON_BKGD_COLOR);
        jl.setFont(new Font(jl.getFont().getName(), Font.BOLD, 16));

        jp2.setLayout(new FlowLayout());
        jp2.setBackground(Color.white);
        jp2.add(jl);

        // add various panels and scroll pane to content pane.
        contentpane.add(jp2, BorderLayout.NORTH);
        contentpane.add(jsp, BorderLayout.CENTER);
        contentpane.add(jp1, BorderLayout.SOUTH);

        // set size of content pane and display it.
        setSize(800, 400);

        setVisible(true);

        // add custom window listener to exit the application if GUI is closed.
        addWindowListener(new MyWindowAdapter());

        JFrame.setDefaultLookAndFeelDecorated(true);
    }

    /**
     * Deletes a single row. Take different actions depending upon whether a row
     * is selected to be deleted or not.
     */
    protected void delete() {
        int selectedRowIndex = table.getSelectedRow();

        // if no row is selected, selectedRowIndex would be -1
        if (selectedRowIndex == -1) {
            JOptionPane.showMessageDialog(null, "Please select a table row to delete.", "Delete Error",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            myTableModel.removeRow(selectedRowIndex);
        }
    }

    /**
     * Deletes all rows of table, i.e., delete all calls logged.
     */
    protected void deleteAll() {
        int rowCount = table.getRowCount();

        for (int i = rowCount - 1; i >= 0; i--) {
            myTableModel.removeRow(i);
        }
    }
    
    /**
     * Prompts the user to enter an access code (if required) to make the call.
     * 
     * @return access code required to make the call
     */
    public String getAccessCode() {
        String accessCode;

        try {
            accessCode =
                    (String) JOptionPane.showInputDialog("If you have to dial an access code \n"
                            + "before dialing this number, please \n" + "enter it now.");

            return accessCode;
        } catch (HeadlessException hle) {
            System.out.println("An exception occured while obtaining" + "access code");
            hle.printStackTrace();
            return null;
        }
    }

    /**
     * Handles the functionality related to making a call, makes appropriate
     * changes, displays various messages to user.
     */
    public void callBack(final String number) {
        // If no row is selected, prompt the user to select a row first.
        if ((number == null) && (table.getSelectedRow() == -1)) {
            JOptionPane.showMessageDialog(null, "Please select a number (table row) to call.", "Call Error",
                    JOptionPane.ERROR_MESSAGE);
        } else if (number == null) {
            // User pressed cancel when prompted for access code.
            System.out.println("Call canceled");
        } else if (number.trim().length() == 0) {
            // Check if callee number is valid, if length of number is greater
            // than 0, other checks (to determine if it is a valid number
            // string) are done in Click2Call.java
            JOptionPane.showMessageDialog(null, "Invalid callee number.", "Call Error", JOptionPane.ERROR_MESSAGE);
        } else {
            int selectedRowIndex = table.getSelectedRow();

            // check that the calling party matches the selected row
            // row remains selected if a call was made in the ldap lookup window
            String calledBackNumber =
                    (String) table.getValueAt(selectedRowIndex, NUMBER_COLUMN_INDEX);
            if (number.trim().equalsIgnoreCase(calledBackNumber)) {

                // The call went through, change call status.
                if (((String) table.getValueAt(selectedRowIndex, STATUS_COLUMN_INDEX)).equals("Missed")) {
                    table.setValueAt("Called back", selectedRowIndex, STATUS_COLUMN_INDEX);
                    returningCall = true;

                } else {
                    table.setValueAt("Dialed", selectedRowIndex, STATUS_COLUMN_INDEX);

                }
            }
        }
    }

    /**
     * Method used to determine if the current call is a call back call.
     * 
     */
    public boolean isReturnCall() {

        int selectedRowIndex = table.getSelectedRow();
        if (selectedRowIndex < 0) {
            return false;
        }
        if ((((String) table.getValueAt(selectedRowIndex, STATUS_COLUMN_INDEX)).equals("Called back"))
                && (returningCall)) {
            return true;
        }

        return false;
    }

    public void clearReturnCall() {
        returningCall = false;
    }

    /**
     * General purpose method used to display message on GUI in appropriate
     * JOptionPane type.
     * 
     * @param msgType Error or message
     * @param msg message
     */
    public void displayMessage(final int msgType, final String msg) {
        if (msg != null) {
            switch (msgType) {
            case JOptionPane.ERROR_MESSAGE:
                JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
                break;
            default:
                JOptionPane.showMessageDialog(null, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Display a received call in table.
     * 
     * @param name name to display for this call
     * @param number number to display for this call
     * @param time time to display for this call
     * @param callType Answered/Missed
     */
    public void displayCall(final String number, final String time, final String callType) {
        Vector<String> temp = new Vector<String>(4, 0);

        temp.add(number);
        temp.add(time);
        temp.add(callType);

        myTableModel.insertRow(0, temp);
    }

    /**
     * Used to obtain the number to call back when user hits "CallBack" button.
     * 
     * @return String number to call, null if no row is selected.
     * @throws Exception if user enters an invalid access code, for example an
     *             alphabet.
     */
    public String getNumberToCall() throws Exception {
        int selectedRowIndex = table.getSelectedRow();

        // no row is selected, return null.
        if (selectedRowIndex == -1) {
            return null;
        } else {
            String accessCode = getAccessCode();

            // User pressed cancel when prompted for access code, so return
            // null
            // to cancel the call.
            if (accessCode == null) {
                return null;
            } else {
                accessCode = accessCode.trim();
            }

            if (accessCode.length() == 0) {
                return (String) table.getValueAt(selectedRowIndex, NUMBER_COLUMN_INDEX);
            } else {
                // Check if the access code is a number
                try {
                    Integer.parseInt(accessCode);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(null, "Invalid access code", "Call Error", JOptionPane.ERROR_MESSAGE);

                    throw new Exception("Invalid access code.");
                }
                return accessCode
                        + (String) table.getValueAt(selectedRowIndex, NUMBER_COLUMN_INDEX);
            }
        }
    }

    /**
     * Obtains and returns index of selected row, -1 if no row is selected.
     * 
     * @return int selected row index, null if no row is selected.
     */
    public int getSelectedRowIndex() {
        return table.getSelectedRow();
    }

    /**
     * Displays the status of a call if the call is successfully made. User can
     * disconnect the call by pressing HangUp button.
     * 
     * @param al action listener of this status window
     * @param calledNumber called number
     * @param calleeName name of the callee
     * @param connId connectionId of the call.
     */
    public void displayCallStatus(final ActionListener al, final String calledNumber, final ConnectionID connId) {
        myCallStatus = new CallStatus(al, calledNumber, connId);
    }

    public void setConnectedCallStatus() {
        if (myCallStatus != null) {
            myCallStatus.setConnectedStatus();
        }

    }

    /**
     * Obtains and returns call data displayed in the table.
     * 
     * @return ArrayList containing table data.
     */
    public ArrayList<Object> getTableData() {
        ArrayList<Object> tableData = new ArrayList<Object>();

        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < TABLE_COLUMN_COUNT; j++) {
                tableData.add(table.getValueAt(i, j));
            }
        }

        return tableData;
    }

    /**
     * Obtains and returns data related to call to be disconnected when user
     * presses HangUp button on status window.
     * 
     * @param actionSource HangUp button on call status window which generated
     *            event.
     * @return ArrayList containing data related to the call being disconnected.
     */
    public ArrayList<String> whichCallToDisconnect(final Object actionSource) {

        ArrayList<String> callData = new ArrayList<String>(2);

        // get called number and call appearance in use for this call.
        callData.add(myCallStatus.getCalledNumber());
        callData.add(myCallStatus.getCallAppearance());

        // release resources in use by this callStatus window.
        myCallStatus.dispose();
        myCallStatus = null;

        return callData;
    }

    public void closeWindow() {
        if (myCallStatus != null) {
            myCallStatus.dispose();
            myCallStatus = null;
        }
    }

    public ConnectionID getConnectionId() {
        if (myCallStatus!=null) {
            return myCallStatus.getActiveConnection();
        } else {
            return null;
        }
    }

    /**
     * Provides custom table model to initialize table and set the data in cells
     * uneditable.
     */
    private class MyTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;

        public MyTableModel(final Object[][] data, final Object[] header) {
            super(data, header);
        }

        public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    }

    /**
     * Provides custom table cell renderer which enables the application to
     * display Answered and Missed calls in different colors.
     */
    private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean selected,
                final boolean focused, final int row, final int column) {
            setEnabled(table == null || table.isEnabled());

            // display missed call in different color than answered calls.
            if (((String) table.getValueAt(row, 2)).trim().equals("Missed")) {
                setBackground(MISSED_CALL_COLOR);
            } else if (((String) table.getValueAt(row, 2)).trim().equals("Dialed")) {
                setBackground(DIALED_CALL_COLOR);
            } else if (((String) table.getValueAt(row, 2)).trim().equals("Called back")) {
                setBackground(CALLED_BACK_CALL_COLOR);
            } else {
                setBackground(Color.WHITE);
            }

            super.getTableCellRendererComponent(table, value, selected, focused, row, column);

            return this;
        }
    }

    /**
     * Custom window adapter to exit the application if gui is closed.
     */
    private class MyWindowAdapter extends WindowAdapter {
        public void windowClosing(final WindowEvent we) {
            setVisible(false);
            System.exit(0);
        }
    }

    /**
     * This class renders JFrame used to display status of a call after a call
     * is successfully made. It contains called number and corresponding call
     * appearance in use to facilitate disconnecting a call.
     */
    private final class CallStatus extends JFrame {
        private static final long serialVersionUID = 1L;

        private String calledNumber;

        private String callAppearance;

        private ConnectionID activeConnection;

        // window message name and number
        JPanel jp = null;

        // we can be connected up to 6 parties
        JLabel[] connectedParty = new JLabel[6];

        /**
         * Constructor stores called number, call appearance used in making this
         * call and callee name. Action listener to be registered for listening
         * events generated here is received as parameter.
         * 
         * @param actionListener
         * @param calledNumber
         * @param calleeName
         * @param connectionId
         */
        private CallStatus(final ActionListener actionListener, final String calledNumber, final ConnectionID connId) {
            super("Call Status");
            
            this.calledNumber = calledNumber;

            Container contentpane = getContentPane();
            contentpane.setLayout(new GridLayout(1, 0));

            jp = new JPanel();
            jp.setLayout(new GridLayout(3, 0));
            jp.setBackground(Color.WHITE);

            connectedParty[0] = new JLabel("Calling.... " + calledNumber);
            jp.add(connectedParty[0]);

            JButton jb = new JButton("Hang Up");
            jb.setActionCommand("hangup");
            jb.addActionListener(actionListener);
            jb.setBackground(BUTTON_BKGD_COLOR);
            jb.setForeground(Color.WHITE);
            jp.add(jb);

            contentpane.add(jp);

            JFrame.setDefaultLookAndFeelDecorated(true);

            this.activeConnection = connId;

            setSize(300, 100);
            setVisible(true);
        }

        private void setConnectedStatus() {
            if (connectedParty[0] != null) {
                connectedParty[0].setText("Connected to " + calledNumber);
                jp.updateUI();
            }
        }

        /**
         * This method returns called number displayed in this status frame.
         * 
         * @return String called number
         */
        private String getCalledNumber() {
            return calledNumber;
        }

        /**
         * This method returns call appearance in use for making this call.
         * 
         * @return String call appearance.
         */
        private String getCallAppearance() {
            return callAppearance;
        }

        private ConnectionID getActiveConnection() {
            return activeConnection;
        }
    }
}
