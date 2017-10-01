/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Autoplot.AutoplotConnector;
import bgs.geophys.library.Autoplot.AutoplotInstance;
import bgs.geophys.library.cdf.dump.CDFDumpDialog;
import bgs.geophys.library.cdf.edit.CDFE_MYEditor;
import gsfc.nssdc.cdf.CDFException;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

/**
 *
 * @author smf
 */
public class ConversionDialog extends javax.swing.JDialog 
implements ActionListener, WindowListener, ConversionDisplayInterface
{
    
    private enum ButtonState {INITIAL, CONVERTING, FINISHED}
    
    private ConversionThread conversion_thread;
    private Timer thread_check_timer;
    
    private Style title_style;
    private Style plain_style;
    
    private JFrame parent;
    private CDFE_MYEditor CDFE_Editors [];
    private CDFDumpDialog CDF_Dumpers  [];
    private AutoplotConnector autoplot_connector;
    
    private JButton month_buttons [];
   
    /**
     * Creates new form CheckProgressDialog
     */
    public ConversionDialog (JFrame parent, boolean modal, AutoplotConnector autoplot_connector)
    {
        super(parent, modal);
        this.parent = parent;
        CDFE_Editors = new CDFE_MYEditor [12];
        CDF_Dumpers = new CDFDumpDialog [12];
        for (int count=0; count<CDFE_Editors.length; count++) CDFE_Editors[count] = null;
        for (int count=0; count<CDF_Dumpers.length; count++) CDF_Dumpers[count] = null;
        initComponents();
        
        DefaultCaret caret = (DefaultCaret) MessageTextPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
                
        this.autoplot_connector = autoplot_connector;
        if (autoplot_connector == null)
        {
            ViewTypeTextRadioButton.setSelected(true);
            ViewTypePlotRadioButton.setEnabled(false);
        }
        
        title_style = createStyle ("Title Style", true, true, false);
        plain_style = createStyle ("Plain Style", false, false, false);
        
        month_buttons = new JButton [12];
        month_buttons [0] = ViewCDFJanButton;
        month_buttons [1] = ViewCDFFebButton;
        month_buttons [2] = ViewCDFMarButton;
        month_buttons [3] = ViewCDFAprButton;
        month_buttons [4] = ViewCDFMayButton;
        month_buttons [5] = ViewCDFJunButton;
        month_buttons [6] = ViewCDFJulButton;
        month_buttons [7] = ViewCDFAugButton;
        month_buttons [8] = ViewCDFSepButton;
        month_buttons [9] = ViewCDFOctButton;
        month_buttons [10] = ViewCDFNovButton;
        month_buttons [11] = ViewCDFDecButton;
        
        conversion_thread = null;
        SetButtonState(ButtonState.INITIAL);
        for (int count=0; count<month_buttons.length; count ++)
            month_buttons[count].setEnabled (false);
        addWindowListener(this);
    }
        
    public void startNewConversionThread (String obs, int year, File folder, int cdf_orientation, boolean compress)
    {
        conversion_thread = new ConversionThread (obs, year, cdf_orientation, folder, this, compress);
        startNewConversionThread(obs, year, folder.getAbsolutePath());
    }
    
    public void startNewConversionThread (String obs, int year, Class reference_class, String relative_reference, File out_folder, int cdf_orientation, boolean compress)
    {
        conversion_thread = new ConversionThread (obs, year, cdf_orientation, reference_class, relative_reference, out_folder, this, compress);
        startNewConversionThread (obs, year, reference_class.getName() + "/" + relative_reference);
    }
    
    private void startNewConversionThread (String obs, int year, String path)
    {
        ObsLabel.setText("Observatory code: " + obs);
        YearLabel.setText("Year: " + Integer.toString (year));
        FolderLabel.setText("Folder for data: " + path);

        Document document = MessageTextPane.getDocument();
        try
        {
            document.remove(0, document.getLength());
            document.insertString(0, "Conversion error messages\n\n", title_style);
        }
        catch (BadLocationException e) { }
        
        setProgress (0);
        
        conversion_thread.start();
        
        thread_check_timer = new Timer (100, this);
        thread_check_timer.start();

        SetButtonState(ButtonState.CONVERTING);
        for (int count=0; count<month_buttons.length; count ++)
            month_buttons[count].setEnabled (false);
        
        setVisible (true);
        
    }
    
    public boolean isConversionRunning () 
    { 
        if (conversion_thread == null) return false;
        return conversion_thread.isAlive();
    }
    
    // listener for the timer that checks for the end of threads running
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        if (conversion_thread != null)
        {
            if (! conversion_thread.isAlive())
            {
                // allow dialog exit and kill this timer
                SetButtonState(ButtonState.FINISHED);
                thread_check_timer.stop();
            }
        }
        File month_files [] = conversion_thread.getMonthFilesCreated();
        for (int count=0; count<12; count++)
        {
            if (month_files[count] != null) month_buttons[count].setEnabled (true);
        }
    }
    
    @Override
    public void setProgress (int percent) { ProgressBar.getModel().setValue (percent); }
    @Override
    public void addConversionErrorMsg (String msg) 
    { 
        Document document = MessageTextPane.getDocument();
        try { document.insertString(document.getLength(), msg + "\n", plain_style); }
        catch (BadLocationException e) { }
    }
    
    private void SetButtonState (ButtonState state)
    {
        switch (state)
        {
        case INITIAL:
            ExitButton.setText("Done");
            setTitle("INTERMAGNET 1-second data convertor");
            break;
        case CONVERTING:
            ExitButton.setText("Cancel");
            setTitle("INTERMAGNET 1-second data conversion in progress...");
            break;
        case FINISHED:
            ExitButton.setText("Done");
            setTitle("INTERMAGNET 1-second data convertor");
            break;
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ViewTypeButtonGroup = new javax.swing.ButtonGroup();
        InfoPanel = new javax.swing.JPanel();
        ObsLabel = new javax.swing.JLabel();
        YearLabel = new javax.swing.JLabel();
        FolderLabel = new javax.swing.JLabel();
        MessageScrollPane = new javax.swing.JScrollPane();
        MessageTextPane = new javax.swing.JTextPane();
        ActionPanel = new javax.swing.JPanel();
        ViewCDFPanel1l = new javax.swing.JPanel();
        ViewTypeLabel = new javax.swing.JLabel();
        ViewTypeTextRadioButton = new javax.swing.JRadioButton();
        ViewTypeEditRadioButton = new javax.swing.JRadioButton();
        ViewTypePlotRadioButton = new javax.swing.JRadioButton();
        ViewCDFPanel2 = new javax.swing.JPanel();
        ViewCDFJanButton = new javax.swing.JButton();
        ViewCDFFebButton = new javax.swing.JButton();
        ViewCDFMarButton = new javax.swing.JButton();
        ViewCDFAprButton = new javax.swing.JButton();
        ViewCDFMayButton = new javax.swing.JButton();
        ViewCDFJunButton = new javax.swing.JButton();
        ViewCDFJulButton = new javax.swing.JButton();
        ViewCDFAugButton = new javax.swing.JButton();
        ViewCDFSepButton = new javax.swing.JButton();
        ViewCDFOctButton = new javax.swing.JButton();
        ViewCDFNovButton = new javax.swing.JButton();
        ViewCDFDecButton = new javax.swing.JButton();
        ProgressAndButtonPanel = new javax.swing.JPanel();
        ProgressLabel = new javax.swing.JLabel();
        ProgressBar = new javax.swing.JProgressBar();
        ExitButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("INTERMAGNET 1-second data checker");

        InfoPanel.setLayout(new java.awt.GridLayout(0, 1));

        ObsLabel.setText("Observatory code:");
        InfoPanel.add(ObsLabel);

        YearLabel.setText("Year:");
        InfoPanel.add(YearLabel);

        FolderLabel.setText("Folder for data: ");
        InfoPanel.add(FolderLabel);

        getContentPane().add(InfoPanel, java.awt.BorderLayout.NORTH);

        MessageScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        MessageTextPane.setEditable(false);
        MessageTextPane.setPreferredSize(new java.awt.Dimension(400, 400));
        MessageScrollPane.setViewportView(MessageTextPane);

        getContentPane().add(MessageScrollPane, java.awt.BorderLayout.CENTER);

        ActionPanel.setLayout(new java.awt.BorderLayout());

        ViewCDFPanel1l.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        ViewTypeLabel.setText("Click on a button below to view CDF data as:");
        ViewCDFPanel1l.add(ViewTypeLabel);

        ViewTypeButtonGroup.add(ViewTypeTextRadioButton);
        ViewTypeTextRadioButton.setSelected(true);
        ViewTypeTextRadioButton.setText("Text");
        ViewCDFPanel1l.add(ViewTypeTextRadioButton);

        ViewTypeButtonGroup.add(ViewTypeEditRadioButton);
        ViewTypeEditRadioButton.setText("In an editor");
        ViewCDFPanel1l.add(ViewTypeEditRadioButton);

        ViewTypeButtonGroup.add(ViewTypePlotRadioButton);
        ViewTypePlotRadioButton.setText("Plot");
        ViewCDFPanel1l.add(ViewTypePlotRadioButton);

        ActionPanel.add(ViewCDFPanel1l, java.awt.BorderLayout.NORTH);

        ViewCDFPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 5));

        ViewCDFJanButton.setText("Jan");
        ViewCDFJanButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFJanButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFJanButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFJanButton);

        ViewCDFFebButton.setText("Feb");
        ViewCDFFebButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFFebButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFFebButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFFebButton);

        ViewCDFMarButton.setText("Mar");
        ViewCDFMarButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFMarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFMarButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFMarButton);

        ViewCDFAprButton.setText("Apr");
        ViewCDFAprButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFAprButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFAprButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFAprButton);

        ViewCDFMayButton.setText("May");
        ViewCDFMayButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFMayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFMayButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFMayButton);

        ViewCDFJunButton.setText("Jun");
        ViewCDFJunButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFJunButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFJunButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFJunButton);

        ViewCDFJulButton.setText("Jul");
        ViewCDFJulButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFJulButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFJulButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFJulButton);

        ViewCDFAugButton.setText("Aug");
        ViewCDFAugButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFAugButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFAugButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFAugButton);

        ViewCDFSepButton.setText("Sep");
        ViewCDFSepButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFSepButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFSepButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFSepButton);

        ViewCDFOctButton.setText("Oct");
        ViewCDFOctButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFOctButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFOctButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFOctButton);

        ViewCDFNovButton.setText("Nov");
        ViewCDFNovButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFNovButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFNovButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFNovButton);

        ViewCDFDecButton.setText("Dec");
        ViewCDFDecButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        ViewCDFDecButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewCDFDecButtonActionPerformed(evt);
            }
        });
        ViewCDFPanel2.add(ViewCDFDecButton);

        ActionPanel.add(ViewCDFPanel2, java.awt.BorderLayout.CENTER);

        ProgressAndButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        ProgressLabel.setText("Conversion progress:");
        ProgressAndButtonPanel.add(ProgressLabel);

        ProgressBar.setStringPainted(true);
        ProgressAndButtonPanel.add(ProgressBar);

        ExitButton.setText("Stop conversion");
        ExitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitButtonActionPerformed(evt);
            }
        });
        ProgressAndButtonPanel.add(ExitButton);

        ActionPanel.add(ProgressAndButtonPanel, java.awt.BorderLayout.SOUTH);

        getContentPane().add(ActionPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ExitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitButtonActionPerformed
        dialog_exit (false);
    }//GEN-LAST:event_ExitButtonActionPerformed

    private void ViewCDFJanButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFJanButtonActionPerformed
        viewCDF (0);
    }//GEN-LAST:event_ViewCDFJanButtonActionPerformed

    private void ViewCDFFebButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFFebButtonActionPerformed
        viewCDF (1);
    }//GEN-LAST:event_ViewCDFFebButtonActionPerformed

    private void ViewCDFMarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFMarButtonActionPerformed
        viewCDF (2);
    }//GEN-LAST:event_ViewCDFMarButtonActionPerformed

    private void ViewCDFAprButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFAprButtonActionPerformed
        viewCDF (3);
    }//GEN-LAST:event_ViewCDFAprButtonActionPerformed

    private void ViewCDFMayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFMayButtonActionPerformed
        viewCDF (4);
    }//GEN-LAST:event_ViewCDFMayButtonActionPerformed

    private void ViewCDFJunButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFJunButtonActionPerformed
        viewCDF (5);
    }//GEN-LAST:event_ViewCDFJunButtonActionPerformed

    private void ViewCDFJulButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFJulButtonActionPerformed
        viewCDF (6);
    }//GEN-LAST:event_ViewCDFJulButtonActionPerformed

    private void ViewCDFAugButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFAugButtonActionPerformed
        viewCDF (7);
    }//GEN-LAST:event_ViewCDFAugButtonActionPerformed

    private void ViewCDFSepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFSepButtonActionPerformed
        viewCDF (8);
    }//GEN-LAST:event_ViewCDFSepButtonActionPerformed

    private void ViewCDFOctButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFOctButtonActionPerformed
        viewCDF (9);
    }//GEN-LAST:event_ViewCDFOctButtonActionPerformed

    private void ViewCDFNovButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFNovButtonActionPerformed
        viewCDF (10);
    }//GEN-LAST:event_ViewCDFNovButtonActionPerformed

    private void ViewCDFDecButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewCDFDecButtonActionPerformed
        viewCDF (11);
    }//GEN-LAST:event_ViewCDFDecButtonActionPerformed

    private void viewCDF (int month)
    {
        File cdf_file = conversion_thread.getMonthFilesCreated()[month];
        String errmsg = "";
        try
        {
            if (cdf_file == null) 
            {
                errmsg = "File for month " + (month +1) + " not available yet";
                throw new IOException ();
            }
            if (ViewTypeTextRadioButton.isSelected())
            {
                errmsg = "Unable to view CDF file";
                if (CDF_Dumpers [month] != null)
                {
                    if (! CDF_Dumpers [month].isDisplayable()) CDF_Dumpers [month] = null;
                }
                if (CDF_Dumpers [month] == null)
                    CDF_Dumpers [month] = new CDFDumpDialog (parent, false, cdf_file, 1, 1000);
                CDF_Dumpers [month].setVisible(true);
            }
            else if (ViewTypeEditRadioButton.isSelected())
            {
                errmsg = "Unable to edit CDF file";
                if (CDFE_Editors [month] != null)
                {
                    if (CDFE_Editors [month].isDisplayable()) CDFE_Editors [month].setVisible(true);
                    else CDFE_Editors [month] = null;
                }
                if (CDFE_Editors [month] == null)
                    CDFE_Editors [month] = new CDFE_MYEditor (parent, cdf_file.getAbsolutePath(), false);
            }
            else if (ViewTypePlotRadioButton.isSelected())
            {
                errmsg = "Error running Autoplot";
                if (autoplot_connector != null)
                {
                    Cursor old_cursor = this.getCursor();
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    AutoplotInstance ap_instance = autoplot_connector.cullAutoplotInstances ();
                    if (ap_instance == null) ap_instance = autoplot_connector.runAutoplot ( new String [0], false, true);
                    ap_instance.loadImagCDF (cdf_file, true);
                    this.setCursor (old_cursor);
                }
            }
        }
        catch (CDFException | IOException | ClassNotFoundException | NoSuchMethodException e)
        {
            errmsg += ": " + cdf_file.getAbsolutePath();
            if (e.getMessage() != null) errmsg += ", " + e.getMessage();
            JOptionPane.showMessageDialog(this, errmsg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ActionPanel;
    private javax.swing.JButton ExitButton;
    private javax.swing.JLabel FolderLabel;
    private javax.swing.JPanel InfoPanel;
    private javax.swing.JScrollPane MessageScrollPane;
    private javax.swing.JTextPane MessageTextPane;
    private javax.swing.JLabel ObsLabel;
    private javax.swing.JPanel ProgressAndButtonPanel;
    private javax.swing.JProgressBar ProgressBar;
    private javax.swing.JLabel ProgressLabel;
    private javax.swing.JButton ViewCDFAprButton;
    private javax.swing.JButton ViewCDFAugButton;
    private javax.swing.JButton ViewCDFDecButton;
    private javax.swing.JButton ViewCDFFebButton;
    private javax.swing.JButton ViewCDFJanButton;
    private javax.swing.JButton ViewCDFJulButton;
    private javax.swing.JButton ViewCDFJunButton;
    private javax.swing.JButton ViewCDFMarButton;
    private javax.swing.JButton ViewCDFMayButton;
    private javax.swing.JButton ViewCDFNovButton;
    private javax.swing.JButton ViewCDFOctButton;
    private javax.swing.JPanel ViewCDFPanel1l;
    private javax.swing.JPanel ViewCDFPanel2;
    private javax.swing.JButton ViewCDFSepButton;
    private javax.swing.ButtonGroup ViewTypeButtonGroup;
    private javax.swing.JRadioButton ViewTypeEditRadioButton;
    private javax.swing.JLabel ViewTypeLabel;
    private javax.swing.JRadioButton ViewTypePlotRadioButton;
    private javax.swing.JRadioButton ViewTypeTextRadioButton;
    private javax.swing.JLabel YearLabel;
    // End of variables declaration//GEN-END:variables

    // window listener methods
    @Override
    public void windowOpened(WindowEvent e) { }
    @Override
    public void windowClosing(WindowEvent e) {
        dialog_exit (true);
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
    private void dialog_exit (boolean always_hide)
    {
        boolean hide = true;

        // is conversion running ??
        if (conversion_thread != null)
        {
            if (conversion_thread.isAlive())
            {
                if (JOptionPane.showConfirmDialog(this, "Data conversion is taking place. Are you sure you want to cancel?", "Do you want to cancel?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    conversion_thread.cancel();
                if (! always_hide) hide = false;
            }
        }
        
        if (hide)
            setVisible(false);
    }

    private Style createStyle (String name, boolean bold, boolean underline, boolean italic)
    {
        Style style = MessageTextPane.addStyle (name, null);
        StyleConstants.setBold (style, bold);
        StyleConstants.setUnderline (style, underline);
        StyleConstants.setItalic (style, italic);
        return style;
    }
}
