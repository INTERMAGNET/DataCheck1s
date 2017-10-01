/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Autoplot.AutoplotConnector;
import bgs.geophys.library.Data.GeomagAbsoluteValue;
import bgs.geophys.library.Data.ImagCDF.ImagCDFLowLevel;
import bgs.geophys.library.Net.BrowserControl;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent.EventType;

/**
 *
 * @author smf
 */
public class MainFrame extends javax.swing.JFrame 
implements WindowListener
{

    private JFileChooser folder_chooser;
    private CheckingDialog checking_dialog;
    private ConversionDialog conversion_dialog;
    private AutoplotConnector autoplot_connector;
    private BrowserControl browser_control;
    private CLOptions.FilterType filter_type;
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame (CLOptions options, String program_title, String program_version) 
    {
        initComponents();

        this.setTitle(program_title + " v" + program_version);
        this.filter_type = options.getFilterType();
        
        ObsTextField.setText(options.getIagaCode());
        YearTextField.setText(Integer.toString (options.getYear()));
        FolderTextField.setText(options.getFolder().getAbsolutePath());
        MinDiffFieldTextField.setText (String.format ("%.1f", options.getMinDiffNt()));
        MinDiffAngleTextField.setText (String.format ("%.1f", options.getMinDiffMinArc()));
        SecDiffFieldTextField.setText (String.format ("%.1f", options.getSecDiffNt()));
        SecDiffAngleTextField.setText (String.format ("%.1f", options.getSecDiffMinArc()));
        switch (options.getOrientation())
        {
            case GeomagAbsoluteValue.ORIENTATION_XYZ: XYZOrientationRadioButton.setSelected(true); break;
            case GeomagAbsoluteValue.ORIENTATION_HDZ: HDZOrientationRadioButton.setSelected(true); break;
            default: NativeOrientationRadioButton.setSelected(true); break;
        }
        folder_chooser = new JFileChooser (options.getFolder());
        folder_chooser.setDialogTitle("Select the folder holding IAF and IAGA-2002 data files");
        folder_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folder_chooser.setApproveButtonText("Select folder");
        if (options.isCompress()) CompressYesRadioButton.setSelected(true);
        else CompressNoRadioButton.setSelected(true);

        ImageIcon icon = new ImageIcon(this.getClass().getResource("resources/Imag.png"));
        if (icon != null) setIconImage(icon.getImage());
        
        addWindowListener(this);
        
        try
        {
            autoplot_connector = new AutoplotConnector();
        }
        catch (IOException e)
        {
            autoplot_connector = null;
        }

        checking_dialog = new CheckingDialog (this, false, options.getLogFile(), autoplot_connector);
        conversion_dialog = new ConversionDialog (this, false, autoplot_connector);
        
        browser_control = new BrowserControl();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        OrientationButtonGroup = new javax.swing.ButtonGroup();
        CompressButtonGroup = new javax.swing.ButtonGroup();
        ChoicesPanel = new javax.swing.JPanel();
        DataChoicePanel = new javax.swing.JPanel();
        ObsLabel = new javax.swing.JLabel();
        ObsTextField = new javax.swing.JTextField();
        YearLabel = new javax.swing.JLabel();
        YearTextField = new javax.swing.JTextField();
        FolderPanel = new javax.swing.JPanel();
        FolderLabel = new javax.swing.JLabel();
        FolderTextField = new javax.swing.JTextField();
        FolderButton = new javax.swing.JButton();
        DiffPanel = new javax.swing.JPanel();
        MinDiffLabel = new javax.swing.JLabel();
        MinDiffFieldTextField = new javax.swing.JTextField();
        MinDiffFieldUnitsLabel = new javax.swing.JLabel();
        MinDiffAngleTextField = new javax.swing.JTextField();
        MinDiffAngleUnitsLabel = new javax.swing.JLabel();
        SecDiffLabel = new javax.swing.JLabel();
        SecDiffFieldTextField = new javax.swing.JTextField();
        SecDiffFieldUnitsLabel = new javax.swing.JLabel();
        SecDiffAngleTextField = new javax.swing.JTextField();
        SecDiffAngleUnitsLabel = new javax.swing.JLabel();
        OrientationLabel = new javax.swing.JLabel();
        OrientationPanel = new javax.swing.JPanel();
        NativeOrientationRadioButton = new javax.swing.JRadioButton();
        XYZOrientationRadioButton = new javax.swing.JRadioButton();
        HDZOrientationRadioButton = new javax.swing.JRadioButton();
        ComrpessLabel = new javax.swing.JLabel();
        CompressYesRadioButton = new javax.swing.JRadioButton();
        CompressNoRadioButton = new javax.swing.JRadioButton();
        HelpScrollPane = new javax.swing.JScrollPane();
        HelpTextPane = new javax.swing.JTextPane();
        ActionPanel = new javax.swing.JPanel();
        ComparisonButton = new javax.swing.JButton();
        ConversionButton = new javax.swing.JButton();
        ExitButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("INTERMAGNET One Second Data Checker");

        ChoicesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Data Choices"));
        ChoicesPanel.setLayout(new java.awt.GridBagLayout());

        ObsLabel.setText("Observatory code:");
        DataChoicePanel.add(ObsLabel);

        ObsTextField.setColumns(4);
        DataChoicePanel.add(ObsTextField);

        YearLabel.setText("Year:");
        DataChoicePanel.add(YearLabel);

        YearTextField.setColumns(4);
        DataChoicePanel.add(YearTextField);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        ChoicesPanel.add(DataChoicePanel, gridBagConstraints);

        FolderLabel.setText("Folder for data:");
        FolderPanel.add(FolderLabel);

        FolderTextField.setEditable(false);
        FolderTextField.setColumns(30);
        FolderTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                FolderTextFieldMouseClicked(evt);
            }
        });
        FolderPanel.add(FolderTextField);

        FolderButton.setText("Change folder...");
        FolderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FolderButtonActionPerformed(evt);
            }
        });
        FolderPanel.add(FolderButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        ChoicesPanel.add(FolderPanel, gridBagConstraints);

        DiffPanel.setLayout(new java.awt.GridBagLayout());

        MinDiffLabel.setText("Minute/second difference threshold:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        DiffPanel.add(MinDiffLabel, gridBagConstraints);

        MinDiffFieldTextField.setColumns(5);
        MinDiffFieldTextField.setText("5.0");
        DiffPanel.add(MinDiffFieldTextField, new java.awt.GridBagConstraints());

        MinDiffFieldUnitsLabel.setText(" nT or ");
        DiffPanel.add(MinDiffFieldUnitsLabel, new java.awt.GridBagConstraints());

        MinDiffAngleTextField.setColumns(5);
        MinDiffAngleTextField.setText("1.0");
        DiffPanel.add(MinDiffAngleTextField, new java.awt.GridBagConstraints());

        MinDiffAngleUnitsLabel.setText(" minutes of arc");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        DiffPanel.add(MinDiffAngleUnitsLabel, gridBagConstraints);

        SecDiffLabel.setText("Second self difference threshold:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        DiffPanel.add(SecDiffLabel, gridBagConstraints);

        SecDiffFieldTextField.setColumns(5);
        SecDiffFieldTextField.setText("2.0");
        DiffPanel.add(SecDiffFieldTextField, new java.awt.GridBagConstraints());

        SecDiffFieldUnitsLabel.setText(" nT or ");
        DiffPanel.add(SecDiffFieldUnitsLabel, new java.awt.GridBagConstraints());

        SecDiffAngleTextField.setColumns(5);
        SecDiffAngleTextField.setText("0.5");
        DiffPanel.add(SecDiffAngleTextField, new java.awt.GridBagConstraints());

        SecDiffAngleUnitsLabel.setText(" minutes of arc");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        DiffPanel.add(SecDiffAngleUnitsLabel, gridBagConstraints);

        OrientationLabel.setText("Orientation of data in CDF output file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        DiffPanel.add(OrientationLabel, gridBagConstraints);

        OrientationPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        OrientationButtonGroup.add(NativeOrientationRadioButton);
        NativeOrientationRadioButton.setSelected(true);
        NativeOrientationRadioButton.setText("As found in minute data file");
        OrientationPanel.add(NativeOrientationRadioButton);

        OrientationButtonGroup.add(XYZOrientationRadioButton);
        XYZOrientationRadioButton.setText("XYZ");
        OrientationPanel.add(XYZOrientationRadioButton);

        OrientationButtonGroup.add(HDZOrientationRadioButton);
        HDZOrientationRadioButton.setText("HDZ");
        OrientationPanel.add(HDZOrientationRadioButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        DiffPanel.add(OrientationPanel, gridBagConstraints);

        ComrpessLabel.setText("CompressCDF files:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        DiffPanel.add(ComrpessLabel, gridBagConstraints);

        CompressButtonGroup.add(CompressYesRadioButton);
        CompressYesRadioButton.setSelected(true);
        CompressYesRadioButton.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        DiffPanel.add(CompressYesRadioButton, gridBagConstraints);

        CompressButtonGroup.add(CompressNoRadioButton);
        CompressNoRadioButton.setText("No");
        DiffPanel.add(CompressNoRadioButton, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        ChoicesPanel.add(DiffPanel, gridBagConstraints);

        getContentPane().add(ChoicesPanel, java.awt.BorderLayout.NORTH);

        HelpScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Help"));

        HelpTextPane.setEditable(false);
        HelpTextPane.setContentType("text/html"); // NOI18N
        HelpTextPane.setText("<html>\r\n<head>\r\n\r\n</head>\r\n<body>\r\n<h3>Instructions</h3>\n<p>\nBefore using this tool you need to install NASA's CDF software from<br>\n<a href=\"http://cdf.gsfc.nasa.gov/\">http://cdf.gsfc.nasa.gov/</a></p>\n<p>\nThis tool requires 1 year of 1-minute data in INTERMAGNET archive format and 1 year<br>\nof 1-second data in IAGA-2002 format from the same observatory. To calculate the<br>\nfirst and last minute values of the year you will also need the 1-second data files<br>\nfor the day before and after the year you are processing). All data files must be in<br>\none folder.</p>\n<p>\nWhen you use the \"Compare data\" button, the tool will calculate 1-minute values<br>\nfrom the 1-second data using the INTERMAGNET 91-point Gaussian filter, then<br>\ncompare the calculated 1-minute values against the values from the INTERMAGNET<br>\narchive format. Only the first 3 elements will be compared (the scalar value is not<br>\nchecked). The tool will tell you about any pair of values that differ by more than the<br>\n'1-minute difference threshold' that you specify. The tool will also calculate the<br>\ndifference between consecutive 1-second values and tell you about any that exceed<br>\nthe '1-second difference threshold' that you specify. You will have the option to view<br>\nthe 1-second data where it exceeds the given threshold.</p>\n<p>\nWhen you use the \"Convert to ImagCDF\" button the tool will convert the 1-second<br>\nIAGA-2002 data files to the INTERMAGNET CDF format. CDF data will be written into<br>\nthe folder where the IAGA-2002 data was found. Once conversion is complete you'll be<br>\nable to view the CDF data.</p>\n<h3>Acknowledgments</h3>\n<p>\nThe CDF software is copyright 1996-2013 United States Government as represented by<br>\nthe Administrator of the National Aeronautics and Space Administration. All Rights<br>\nReserved.</p>\n<p>\nData plotting is performed using the Autoplot software:\n<a href=\"http://autoplot.org/\">http://autoplot.org/</a>.</p>\n</body>\r\n</html>\r\n");
        HelpTextPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                HelpTextPaneHyperlinkUpdate(evt);
            }
        });
        HelpScrollPane.setViewportView(HelpTextPane);

        getContentPane().add(HelpScrollPane, java.awt.BorderLayout.LINE_END);

        ComparisonButton.setText("Compare data...");
        ComparisonButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComparisonButtonActionPerformed(evt);
            }
        });
        ActionPanel.add(ComparisonButton);

        ConversionButton.setText("Convert to ImagCDF...");
        ConversionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ConversionButtonActionPerformed(evt);
            }
        });
        ActionPanel.add(ConversionButton);

        ExitButton.setText("Exit");
        ExitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitButtonActionPerformed(evt);
            }
        });
        ActionPanel.add(ExitButton);

        getContentPane().add(ActionPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void FolderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FolderButtonActionPerformed
        int ret_val = folder_chooser.showOpenDialog(this);
        if (ret_val == JFileChooser.APPROVE_OPTION)
            FolderTextField.setText(folder_chooser.getSelectedFile().getAbsolutePath());
    }//GEN-LAST:event_FolderButtonActionPerformed

    private void ComparisonButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ComparisonButtonActionPerformed
        // don't do anything if the checking dialog is already displayed
        if (checking_dialog.isVisible())
        {
            checking_dialog.requestFocus();
        }
        else        
        {
            // validate form contents
            List<String> errmsgs = validateFormContents();
            
            // run the check and conversion??
            if (errmsgs.size() > 0)
            {
                // no - display errors
                String msg_list = "";
                for (String msg : errmsgs)
                    msg_list += msg + "\n";
                JOptionPane.showMessageDialog (this, msg_list, "Invalid choices", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                // configure the dialog and start up the conversion thread
                String obs = ObsTextField.getText ();
                int year = Integer.parseInt (YearTextField.getText().trim());
                File folder = new File (FolderTextField.getText());
                double min_diff_field_threshold = Double.parseDouble (MinDiffFieldTextField.getText().trim());
                double min_diff_angle_threshold = Double.parseDouble (MinDiffAngleTextField.getText().trim());
                double sec_diff_field_threshold = Double.parseDouble (SecDiffFieldTextField.getText().trim());
                double sec_diff_angle_threshold = Double.parseDouble (SecDiffAngleTextField.getText().trim());
                checking_dialog.startNewCheckingThread (obs, year, folder, min_diff_field_threshold, min_diff_angle_threshold,
                                                        sec_diff_field_threshold, sec_diff_angle_threshold, 10, filter_type);
            }
        }
    }//GEN-LAST:event_ComparisonButtonActionPerformed

    
    private void ExitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitButtonActionPerformed
        exit ();
    }//GEN-LAST:event_ExitButtonActionPerformed

    private void ConversionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ConversionButtonActionPerformed
        // check CDF is available
        String cdf_errmsg = ImagCDFLowLevel.checkNativeLib(null);
        if (cdf_errmsg != null)
        {
            JOptionPane.showMessageDialog(this, cdf_errmsg, "Error - no CDF", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // don't do anything if the conversion dialog is already displayed
        if (conversion_dialog.isVisible())
        {
            conversion_dialog.requestFocus();
        }
        else        
        {
            // validate form contents
            List<String> errmsgs = validateFormContents();

            // run the check and conversion??
            if (errmsgs.size() > 0)
            {
                // no - display errors
                String msg_list = "";
                for (String msg : errmsgs)
                    msg_list += msg + "\n";
                JOptionPane.showMessageDialog (this, msg_list, "Invalid choices", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                // configure the dialog and start up the conversion thread
                String obs = ObsTextField.getText ();
                int year = Integer.parseInt (YearTextField.getText().trim());
                File folder = new File (FolderTextField.getText());
                int cdf_orientation = getCDFOrientation();
                boolean compress = CompressYesRadioButton.isSelected();
                conversion_dialog.startNewConversionThread (obs, year, folder, cdf_orientation, compress);
            }
        }
    }//GEN-LAST:event_ConversionButtonActionPerformed

    private void HelpTextPaneHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_HelpTextPaneHyperlinkUpdate
        if (evt.getEventType() == EventType.ACTIVATED)
        {
            String msg = null;
            if (! browser_control.isBrowserAvailable())
                msg = "Unable to launch web broswer";
            else
            {
                try
                {
                    browser_control.displayURL(evt.getURL().toExternalForm());
                }
                catch (IOException | InterruptedException e)
                {
                    msg = "Error launching web browser";
                    if (e.getMessage() != null)
                        msg += ": " + e.getMessage();
                }
            }
            if (msg != null)
                JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_HelpTextPaneHyperlinkUpdate

    private void FolderTextFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_FolderTextFieldMouseClicked
        FolderButtonActionPerformed(null);
    }//GEN-LAST:event_FolderTextFieldMouseClicked
    private int getCDFOrientation ()
    {
        int cdf_orientation;
        if (XYZOrientationRadioButton.isSelected())
            cdf_orientation = GeomagAbsoluteValue.ORIENTATION_XYZ;
        else if (HDZOrientationRadioButton.isSelected())
            cdf_orientation = GeomagAbsoluteValue.ORIENTATION_HDZ;
        else
            cdf_orientation = GeomagAbsoluteValue.ORIENTATION_UNKNOWN;
        return cdf_orientation;
    }
    
    
    private List<String> validateFormContents ()
    {
        ArrayList<String> errmsgs = new ArrayList<> ();
        String obs = ObsTextField.getText ();
        if (obs.length() != 3) errmsgs.add ("Observatory must be a 3 character string: " + obs);
        int year = 0;
        try
        {
            year = Integer.parseInt (YearTextField.getText().trim());
            if (year < 1900 || year > 2100)
                errmsgs.add ("Year must be an integer between 1900 and 2100");
        }
        catch (NumberFormatException e)
        {
            errmsgs.add ("Year must be an integer between 1900 and 2100");
        }
        File folder = new File (FolderTextField.getText());
        if (! folder.isDirectory())
            errmsgs.add ("Folder must be a directory: " + folder.getAbsoluteFile());
        else if (folder.list().length <= 0)
            errmsgs.add ("Folder does not contain any files: " + folder.getAbsoluteFile());
        double min_diff_field_threhold = 0.0;
        try
        {
            min_diff_field_threhold = Double.parseDouble(MinDiffFieldTextField.getText().trim());
            if (min_diff_field_threhold < 0.5 || min_diff_field_threhold > 1000.0)
                errmsgs.add ("Minute difference field threshold must be a number between 0.5 and 1000.0");
        }
        catch (NumberFormatException e)
        {
            errmsgs.add ("Minute difference threshold field must be a number between 0.5 and 1000.0");
        }
        double min_diff_angle_threhold = 0.0;
        try
        {
            min_diff_angle_threhold = Double.parseDouble(MinDiffAngleTextField.getText().trim());
            if (min_diff_angle_threhold < 0.1 || min_diff_angle_threhold > 1000.0)
                errmsgs.add ("Minute difference angle threshold must be a number between 0.1 and 1000.0");
        }
        catch (NumberFormatException e)
        {
            errmsgs.add ("Minute difference threshold angle must be a number between 0.1 and 1000.0");
        }
        double sec_diff_field_threshold = 0.0;
        try
        {
            sec_diff_field_threshold = Double.parseDouble(SecDiffFieldTextField.getText().trim());
            if (sec_diff_field_threshold < 0.5 || sec_diff_field_threshold > 1000.0)
                errmsgs.add ("Second difference field threshold must be a number between 0.5 and 1000.0");
        }
        catch (NumberFormatException e)
        {
            errmsgs.add ("Second difference field threshold must be a number between 0.5 and 1000.0");
        }
        double sec_diff_angle_threshold = 0.0;
        try
        {
            sec_diff_angle_threshold = Double.parseDouble(SecDiffAngleTextField.getText().trim());
            if (sec_diff_angle_threshold < 0.1 || sec_diff_angle_threshold > 1000.0)
                errmsgs.add ("Second difference angle threshold must be a number between 0.1 and 1000.0");
        }
        catch (NumberFormatException e)
        {
            errmsgs.add ("Second difference angle threshold must be a number between 0.1 and 1000.0");
        }
        int cdf_orientation;
        if (XYZOrientationRadioButton.isSelected())
            cdf_orientation = GeomagAbsoluteValue.ORIENTATION_XYZ;
        else if (HDZOrientationRadioButton.isSelected())
            cdf_orientation = GeomagAbsoluteValue.ORIENTATION_HDZ;
        else
            cdf_orientation = GeomagAbsoluteValue.ORIENTATION_UNKNOWN;
        
        return errmsgs;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ActionPanel;
    private javax.swing.JPanel ChoicesPanel;
    private javax.swing.JButton ComparisonButton;
    private javax.swing.ButtonGroup CompressButtonGroup;
    private javax.swing.JRadioButton CompressNoRadioButton;
    private javax.swing.JRadioButton CompressYesRadioButton;
    private javax.swing.JLabel ComrpessLabel;
    private javax.swing.JButton ConversionButton;
    private javax.swing.JPanel DataChoicePanel;
    private javax.swing.JPanel DiffPanel;
    private javax.swing.JButton ExitButton;
    private javax.swing.JButton FolderButton;
    private javax.swing.JLabel FolderLabel;
    private javax.swing.JPanel FolderPanel;
    private javax.swing.JTextField FolderTextField;
    private javax.swing.JRadioButton HDZOrientationRadioButton;
    private javax.swing.JScrollPane HelpScrollPane;
    private javax.swing.JTextPane HelpTextPane;
    private javax.swing.JTextField MinDiffAngleTextField;
    private javax.swing.JLabel MinDiffAngleUnitsLabel;
    private javax.swing.JTextField MinDiffFieldTextField;
    private javax.swing.JLabel MinDiffFieldUnitsLabel;
    private javax.swing.JLabel MinDiffLabel;
    private javax.swing.JRadioButton NativeOrientationRadioButton;
    private javax.swing.JLabel ObsLabel;
    private javax.swing.JTextField ObsTextField;
    private javax.swing.ButtonGroup OrientationButtonGroup;
    private javax.swing.JLabel OrientationLabel;
    private javax.swing.JPanel OrientationPanel;
    private javax.swing.JTextField SecDiffAngleTextField;
    private javax.swing.JLabel SecDiffAngleUnitsLabel;
    private javax.swing.JTextField SecDiffFieldTextField;
    private javax.swing.JLabel SecDiffFieldUnitsLabel;
    private javax.swing.JLabel SecDiffLabel;
    private javax.swing.JRadioButton XYZOrientationRadioButton;
    private javax.swing.JLabel YearLabel;
    private javax.swing.JTextField YearTextField;
    // End of variables declaration//GEN-END:variables

    // window listener methods
    @Override
    public void windowOpened(WindowEvent e) { }
    @Override
    public void windowClosing(WindowEvent e) {
        exit ();
    }
    @Override
    public void windowClosed(WindowEvent e) { }
    @Override
    public void windowIconified(WindowEvent e) { }
    @Override
    public void windowDeiconified(WindowEvent e) { }
    @Override
    public void windowActivated(WindowEvent e) { }
    @Override
    public void windowDeactivated(WindowEvent e) { }
    
    /** method to handle tidy up and exit of the program */
    private void exit ()
    {
        boolean do_exit = true;
        if (checking_dialog.isCheckingRunning() || conversion_dialog.isConversionRunning())
        {
            if (JOptionPane.showConfirmDialog(this, "Data checking and conversion is taking place. Are you sure you want to exit?", "Do you want to exit?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                do_exit = false;
        }
        if (do_exit)
        {
            System.exit (0);
        }
    }
    
}

