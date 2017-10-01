/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Autoplot.AutoplotConnector;
import bgs.geophys.library.Autoplot.AutoplotInstance;
import bgs.geophys.library.Data.GeomagDataFilename;
import bgs.geophys.library.Data.GeomagDataFormat;
import bgs.geophys.library.Data.Iaga2002Filename;
import bgs.geophys.library.Swing.TextViewerDialog;
import bgs.geophys.library.Swing.TextViewerException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

/**
 *
 * @author smf
 */
public class CheckingDialog extends javax.swing.JDialog 
implements ActionListener, WindowListener, CheckingDisplayInterface
{
    
    private enum ButtonState {INITIAL, CHECKING, FINISHED}
    
    private CheckingThread checking_thread;
    private Timer thread_check_timer;
    private JFileChooser msg_save_file_chooser;
    
    private String data_obs;
    private File data_folder;
    
    private DefaultStyledDocument message_text_document;
    private Style title_style;
    private Style highlight_style;
    private Style plain_style;
    private LinePosition highlighted_line;
    private SimpleDateFormat msg_date_format;
    private int n_checking_err_msgs;
    private int n_sec_min_diff_msgs;
    private int n_sec_self_diff_msgs;
    
    private AutoplotConnector autoplot_connector;
    
    /**
     * Creates new form CheckProgressDialog
     */
    public CheckingDialog (java.awt.Frame parent, boolean modal, File msg_save_folder, AutoplotConnector autoplot_connector)
    {
        super(parent, modal);
        initComponents();
        
        DefaultCaret caret = (DefaultCaret) MessageTextPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        if (msg_save_folder == null) msg_save_folder = new File (".");
        msg_save_file_chooser = new JFileChooser (msg_save_folder);
        msg_save_file_chooser.setDialogTitle ("Save Errors...");
        msg_save_file_chooser.setApproveButtonText("Save");
        
        initMessagePaneDocument();

        this.autoplot_connector = autoplot_connector;
        if (autoplot_connector == null)
        {
            ViewTypeTextRadioButton.setSelected(true);
            ViewTypePlotRadioButton.setEnabled(false);
        }
        
        checking_thread = null;
        SetButtonState(ButtonState.INITIAL);
        addWindowListener(this);
    }
    
    public void startNewCheckingThread (String obs, int year, File folder, double min_diff_field_threshold, double min_diff_angle_threshold, 
                                        double sec_diff_field_threshold, double sec_diff_angle_threshold, int max_missing_pc, CLOptions.FilterType filter_type) 
    {
        this.data_folder = folder;
        checking_thread = new CheckingThread (obs, year, folder, min_diff_field_threshold, min_diff_angle_threshold, 
                                              sec_diff_field_threshold, sec_diff_angle_threshold, max_missing_pc, 
                                              filter_type, this);
        startNewCheckingThread (obs, year, folder.getAbsolutePath(), min_diff_field_threshold, min_diff_angle_threshold, 
                                sec_diff_field_threshold, sec_diff_angle_threshold, max_missing_pc, filter_type);
    }
    
    public void startNewCheckingThread (String obs, int year, Class reference_class, String relative_reference, double min_diff_field_threshold, double min_diff_angle_threshold, 
                                        double sec_diff_field_threshold, double sec_diff_angle_threshold, int max_missing_pc, CLOptions.FilterType filter_type) 
    {
        this.data_folder = null;
        checking_thread = new CheckingThread (obs, year, reference_class, relative_reference, min_diff_field_threshold, min_diff_angle_threshold, 
                                              sec_diff_field_threshold, sec_diff_angle_threshold, max_missing_pc, 
                                              filter_type, this);
        startNewCheckingThread (obs, year, reference_class.getName(), min_diff_field_threshold, min_diff_angle_threshold,
                                sec_diff_field_threshold, sec_diff_angle_threshold, max_missing_pc, filter_type);
    }
            
            
    private void startNewCheckingThread (String obs, int year, String path, double min_diff_field_threshold, double min_diff_angle_threshold, 
                                        double sec_diff_field_threshold, double sec_diff_angle_threshold, int max_missing_pc, CLOptions.FilterType filter_type) 
    {
        this.data_obs = obs;
        
        ObsLabel.setText("Observatory code: " + obs);
        YearLabel.setText("Year: " + Integer.toString (year));
        FolderLabel.setText("Folder for data: " + path);
        switch (filter_type)
        {
            case COSINE: FilterTypeLabel.setText ("Filter: Non-standard 61-point Cosine"); break;
            case GAUSSIAN: FilterTypeLabel.setText ("Filter: INTERMAGNET standard 91-point Gaussian"); break;
            default: FilterTypeLabel.setText ("Filter: Unknown [" + filter_type.name() + "], using Gausian"); break;
        }
        String title = "Filtered second - minute differences over threshold of " + String.format ("%.1f", min_diff_field_threshold) + " nT or " + String.format ("%.1f", min_diff_angle_threshold) + " min. arc";
        replaceParagraph(calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_TITLE), title, title_style);
        title = "Second 1st differences over threshold of " + String.format ("%.1f", sec_diff_field_threshold) + " nT or " + String.format ("%.1f", sec_diff_angle_threshold) + " min. arc";
        replaceParagraph(calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_TITLE), title, title_style);
 
        setProgress (0);

        // clear all error messages
        if (n_sec_min_diff_msgs > 0)
        {
            while (n_sec_min_diff_msgs > 0)
            {
                removeParagraph(calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_LAST_MESSAGE));
                n_sec_min_diff_msgs --;
            }
            insertParagraph(calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_LAST_MESSAGE), "No difference values exceed the threshold", plain_style);
        }
        if (n_sec_self_diff_msgs > 0)
        {
            while (n_sec_self_diff_msgs > 0)
            {
                removeParagraph(calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_LAST_MESSAGE));
                n_sec_self_diff_msgs --;
            }
            insertParagraph(calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_LAST_MESSAGE), "No difference values exceed the threshold", plain_style);
        }
        if (n_checking_err_msgs > 0)
        {
            while (n_checking_err_msgs > 0)
            {
                removeParagraph(calcParagraphNumber(ParagraphPosition.CHECKING_ERROR_LAST_MESSAGE));
                n_checking_err_msgs --;
            }
            insertParagraph(calcParagraphNumber(ParagraphPosition.CHECKING_ERROR_FIRST_MESSAGE), "No checking errors", plain_style);
        }

        checking_thread.start();
        thread_check_timer = new Timer (100, this);
        thread_check_timer.start();
        
        SetButtonState(ButtonState.CHECKING);
        
        setVisible (true);
    }
    
    public boolean isCheckingRunning () { return checking_thread != null; }
    
    // listener for the timer that checks for the end of threads running
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        if (checking_thread != null)
        {
            if (! checking_thread.isAlive())
            {
                // allow dialog exit and kill this timer
                SetButtonState(ButtonState.FINISHED);
                checking_thread = null;
            }
        }
    }
    
    @Override
    public void setProgress (int percent) { ProgressBar.getModel().setValue (percent); }
    @Override
    public void addCheckingErrorMsg (String msg) 
    { 
        if (n_checking_err_msgs == 0)
            replaceParagraph(calcParagraphNumber(ParagraphPosition.CHECKING_ERROR_LAST_MESSAGE), msg, plain_style);
        else
            insertParagraph(calcParagraphNumber(ParagraphPosition.CHECKING_ERROR_LAST_MESSAGE) +1, msg, plain_style); 
        n_checking_err_msgs ++;
    }
    @Override
    public void addSecMinDiffMsg (String msg) 
    { 
        if (n_sec_min_diff_msgs == 0)
            replaceParagraph(calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_LAST_MESSAGE), msg, plain_style);
        else
            insertParagraph(calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_LAST_MESSAGE) +1, msg, plain_style); 
        n_sec_min_diff_msgs ++;
    }
    @Override
    public void addSecSelfDiffMsg (String msg) 
    { 
        if (n_sec_self_diff_msgs == 0)
            replaceParagraph(calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_LAST_MESSAGE), msg, plain_style); 
        else
            insertParagraph(calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_LAST_MESSAGE) +1, msg, plain_style); 
        n_sec_self_diff_msgs ++;
    }
    
    private void SetButtonState (ButtonState state)
    {
        switch (state)
        {
        case INITIAL:
            ExitButton.setText("Done");
            SaveErrorsButton.setEnabled(false);
            setTitle("INTERMAGNET 1-second data checker");
            break;
        case CHECKING:
            ExitButton.setText("Cancel");
            SaveErrorsButton.setEnabled(false);
            setTitle("INTERMAGNET 1-second data checking in progress...");
            break;
        case FINISHED:
            ExitButton.setText("Done");
            SaveErrorsButton.setEnabled(true);
            setTitle("INTERMAGNET 1-second data checker");
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
        FilterTypeLabel = new javax.swing.JLabel();
        MessageScrollPane = new javax.swing.JScrollPane();
        MessageTextPane = new javax.swing.JTextPane();
        ActionPanel = new javax.swing.JPanel();
        ChoicePanel = new javax.swing.JPanel();
        ViewTypeLabel = new javax.swing.JLabel();
        ViewTypeTextRadioButton = new javax.swing.JRadioButton();
        ViewTypePlotRadioButton = new javax.swing.JRadioButton();
        ProgressAndButtonPanel = new javax.swing.JPanel();
        ProgressLabel = new javax.swing.JLabel();
        ProgressBar = new javax.swing.JProgressBar();
        ExitButton = new javax.swing.JButton();
        SaveErrorsButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("INTERMAGNET 1-second data checker");

        InfoPanel.setLayout(new java.awt.GridLayout(0, 1));

        ObsLabel.setText("Observatory code:");
        InfoPanel.add(ObsLabel);

        YearLabel.setText("Year:");
        InfoPanel.add(YearLabel);

        FolderLabel.setText("Folder for data: ");
        InfoPanel.add(FolderLabel);

        FilterTypeLabel.setText("Filter: ");
        InfoPanel.add(FilterTypeLabel);

        getContentPane().add(InfoPanel, java.awt.BorderLayout.NORTH);

        MessageScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        MessageTextPane.setEditable(false);
        MessageTextPane.setPreferredSize(new java.awt.Dimension(400, 400));
        MessageTextPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                MessageTextPaneMouseMoved(evt);
            }
        });
        MessageTextPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                MessageTextPaneMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                MessageTextPaneMouseExited(evt);
            }
        });
        MessageScrollPane.setViewportView(MessageTextPane);

        getContentPane().add(MessageScrollPane, java.awt.BorderLayout.CENTER);

        ActionPanel.setLayout(new java.awt.BorderLayout());

        ChoicePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        ViewTypeLabel.setText("Click on a message in the window above to view the data as:");
        ChoicePanel.add(ViewTypeLabel);

        ViewTypeButtonGroup.add(ViewTypeTextRadioButton);
        ViewTypeTextRadioButton.setSelected(true);
        ViewTypeTextRadioButton.setText("Text");
        ChoicePanel.add(ViewTypeTextRadioButton);

        ViewTypeButtonGroup.add(ViewTypePlotRadioButton);
        ViewTypePlotRadioButton.setText("Plot");
        ChoicePanel.add(ViewTypePlotRadioButton);

        ActionPanel.add(ChoicePanel, java.awt.BorderLayout.NORTH);

        ProgressAndButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        ProgressLabel.setText("Checking progress:");
        ProgressAndButtonPanel.add(ProgressLabel);

        ProgressBar.setStringPainted(true);
        ProgressAndButtonPanel.add(ProgressBar);

        ExitButton.setText("Stop checking");
        ExitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitButtonActionPerformed(evt);
            }
        });
        ProgressAndButtonPanel.add(ExitButton);

        SaveErrorsButton.setText("Save Errors...");
        SaveErrorsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveErrorsButtonActionPerformed(evt);
            }
        });
        ProgressAndButtonPanel.add(SaveErrorsButton);

        ActionPanel.add(ProgressAndButtonPanel, java.awt.BorderLayout.SOUTH);

        getContentPane().add(ActionPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ExitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitButtonActionPerformed
        exit_dialog (false);
    }//GEN-LAST:event_ExitButtonActionPerformed

    private void SaveErrorsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveErrorsButtonActionPerformed
        int ret_val = msg_save_file_chooser.showSaveDialog(this);
        if (ret_val == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                boolean do_write = true;
                if (msg_save_file_chooser.getSelectedFile().exists())
                {
                    if (JOptionPane.showConfirmDialog(this, 
                                                      msg_save_file_chooser.getSelectedFile() + " exists, do you want to overwrite it?", 
                                                      "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
                        do_write = false;
                }
                if (do_write)
                {
                    PrintStream ps = new PrintStream (msg_save_file_chooser.getSelectedFile());
                    saveErrors(ps, new Date());
                    ps.close();
                }
            }
            catch (IOException e)
            {
                String msg = "Unable to write errors to file";
                if (e.getMessage() != null) msg += ": " + e.getMessage();
                JOptionPane.showMessageDialog (this, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_SaveErrorsButtonActionPerformed

    public void saveErrors (PrintStream ps, Date date)
    throws IOException
    {
        ps.println ("INTERMAGNET 1-second data checking programme");
        ps.println ("");
        ps.println (date.toString());
        ps.println (ObsLabel.getText());
        ps.println (YearLabel.getText());
        ps.println (FolderLabel.getText());
        ps.println (FilterTypeLabel.getText());
        ps.println ("");                    
        ps.print (MessageTextPane.getText());
        if (ps.checkError()) throw new IOException ();
    }
    
    private void MessageTextPaneMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MessageTextPaneMouseMoved
        Point pt = new Point(evt.getX(), evt.getY());
        int pos = MessageTextPane.viewToModel(pt);
        LinePosition line_pos = findLinePosition(pos);
        highlightText(line_pos);
    }//GEN-LAST:event_MessageTextPaneMouseMoved

    private void MessageTextPaneMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MessageTextPaneMouseExited
        unhighlightText();
    }//GEN-LAST:event_MessageTextPaneMouseExited

    private void MessageTextPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MessageTextPaneMouseClicked
        if (data_folder == null)
        {
            JOptionPane.showMessageDialog (this, "Data views not available when reading data from URL", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            String para_text = getParagraphText (LinePositionToElementNo(highlighted_line));
            try
            {
                Date file_date = msg_date_format.parse(para_text);
                Iaga2002Filename iaga_2002_filename = new Iaga2002Filename (data_obs, file_date, GeomagDataFilename.QualityType.DEFINITIVE, GeomagDataFilename.Interval.SECOND, false, GeomagDataFormat.DurationType.DAY, GeomagDataFilename.Case.LOWER);
                File iaga_2002_file = new File (data_folder, iaga_2002_filename.getFilename());
                if (ViewTypeTextRadioButton.isSelected())
                {
                    try
                    {
                        new TextViewerDialog (this, iaga_2002_file.getAbsoluteFile(), "Text viewer: " + iaga_2002_file.getName(), null);
                    }
                    catch (TextViewerException e)
                    {
                        JOptionPane.showMessageDialog(this, "Unable to view file: " + iaga_2002_file.getName(), "Unable to view file", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else if (ViewTypePlotRadioButton.isSelected())
                {
                    if (autoplot_connector != null)
                    {
                        AutoplotInstance ap_instance = autoplot_connector.cullAutoplotInstances ();
                        try
                        {
                            if (ap_instance == null) ap_instance = autoplot_connector.runAutoplot (new String [0], false, true);
                            ap_instance.loadIaga2002 (iaga_2002_file, true);
                        }
                        catch (IOException | ClassNotFoundException | NoSuchMethodException e)
                        {
                            String msg = "Error running Autoplot";
                            if (e.getMessage() != null) msg += ": " + e.getMessage();
                            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }   
            catch (ParseException e) { }
        }
    }//GEN-LAST:event_MessageTextPaneMouseClicked

    private void highlightText (LinePosition pos)
    {
        unhighlightText();
        setParagraphStyle(LinePositionToElementNo(pos), highlight_style);
        highlighted_line = pos;
    }
    private void unhighlightText ()
    {
        setParagraphStyle(LinePositionToElementNo(highlighted_line), plain_style);
        highlighted_line = new LinePosition ();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ActionPanel;
    private javax.swing.JPanel ChoicePanel;
    private javax.swing.JButton ExitButton;
    private javax.swing.JLabel FilterTypeLabel;
    private javax.swing.JLabel FolderLabel;
    private javax.swing.JPanel InfoPanel;
    private javax.swing.JScrollPane MessageScrollPane;
    private javax.swing.JTextPane MessageTextPane;
    private javax.swing.JLabel ObsLabel;
    private javax.swing.JPanel ProgressAndButtonPanel;
    private javax.swing.JProgressBar ProgressBar;
    private javax.swing.JLabel ProgressLabel;
    private javax.swing.JButton SaveErrorsButton;
    private javax.swing.ButtonGroup ViewTypeButtonGroup;
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
        exit_dialog (true);
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
    private void exit_dialog (boolean always_hide)
    {
        boolean hide = true;

        // is checking running ??
        if (checking_thread != null)
        {
            if (JOptionPane.showConfirmDialog(this, "Data checking is taking place. Are you sure you want to cancel?", "Do you want to cancel?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                checking_thread.cancel();
            if (! always_hide) hide = false;
        }
        
        if (hide)
            setVisible(false);
    }

    // methods to manage manipulation of blocks of text inside a JTextPane
    // info on doing this comes from these wbesites:
    // http://www.comp.nus.edu.sg/~cs3283/ftp/Java/swingConnect/text/element_interface/element_interface.html
    // http://www.java2s.com/Code/JavaAPI/javax.swing.text/StyleConstantsBold.htm
    // 1.) This code assumes that elements aren't nested (which I think would only happen where there are attribute
    // changes mid-paragraph)
    // 2.) Do not include newlines in paragraph strings - the code will add them for you
    // 3.) Text is inserted before the element number given. To insert after the last element, use a -ve element_no
    // The structure of the text in the text pane is (in this order):
    //      Number of lines Contents
    //      1               Checking error title
    //      1 to n          Checking error messages - there's always at least one line,
    //                      even when the number of messages is zero
    //      1               Blank line
    //      1               Second-minute different title
    //      1 to m          Second-minute difference threshold execption - there's always at least one line,
    //                      even when the number of messages is zero
    //      1               Blank line
    //      1               Second 1st different title
    //      1 to o          Second 1st difference threshold execption - there's always at least one line,
    //                      even when the number of messages is zero
    public enum ParagraphPosition { CHECKING_ERROR_TITLE, CHECKING_ERROR_FIRST_MESSAGE, CHECKING_ERROR_LAST_MESSAGE,
                                    SEC_MIN_DIFF_TITLE, SEC_MIN_DIFF_FIRST_MESSAGE, SEC_MIN_DIFF_LAST_MESSAGE,
                                    SEC_SELF_DIFF_TITLE, SEC_SELF_DIFF_FIRST_MESSAGE, SEC_SELF_DIFF_LAST_MESSAGE }
    public enum LineType { CHECKING_ERROR_MESSAGE, SEC_MIN_DIFF_MESSAGE, SEC_SELF_DIFF_MESSAGE, OTHER }
    private class LinePosition 
    {
        LineType line_type;
        int line_number;
        public LinePosition ()
        {
            this.line_type = LineType.OTHER;
            this.line_number = -1;
        }
        public LinePosition (LineType line_type, int line_number)
        {
            this.line_type = line_type;
            this.line_number = line_number;
        }
        public LineType getLineType () { return line_type; }
        public int getLineNumber () { return line_number; }
        @Override
        public String toString () { return line_type.name() + ": " + line_number; }
    }
    private void appendParagraph (String paragraph, Style style)
    {
        insertParagraph(-1, paragraph, style);
    }
    private void insertParagraph (int element_no, String paragraph, Style style)
    {
        int n_elements = message_text_document.getDefaultRootElement().getElementCount();
        if (element_no >= n_elements) element_no = -1;
        int start_pos;
        if (element_no >= 0)
            start_pos = message_text_document.getDefaultRootElement().getElement(element_no).getStartOffset();
        else if (n_elements <= 0)
            start_pos = message_text_document.getLength();
        else
            start_pos = message_text_document.getDefaultRootElement().getElement(n_elements -1).getEndOffset();
        try
        {
            message_text_document.insertString(start_pos, paragraph + "\n", style);
        }
        catch (BadLocationException e) { }
    }
    private void replaceParagraph (int element_no, String paragraph, Style style)
    {
        int n_elements = message_text_document.getDefaultRootElement().getElementCount();
        if (element_no >= n_elements) 
            throw new ArrayIndexOutOfBoundsException ();
        removeParagraph(element_no);
        insertParagraph(element_no, paragraph, style);
    }
    private void removeParagraph (int element_no)
    {
        int n_elements = message_text_document.getDefaultRootElement().getElementCount();
        if (element_no >= n_elements) 
            throw new ArrayIndexOutOfBoundsException ();
        message_text_document.removeElement(message_text_document.getDefaultRootElement().getElement(element_no));
    }
    private void setParagraphStyle (int element_no, Style style)
    {
        int n_elements = message_text_document.getDefaultRootElement().getElementCount();
        if (element_no >= 0 && element_no < n_elements)
        {
            Element element = message_text_document.getDefaultRootElement().getElement(element_no);
            message_text_document.setCharacterAttributes (element.getStartOffset(), 
                                                          element.getEndOffset() - element.getStartOffset(),
                                                          style, true);
        }
    }
    private String getParagraphText (int element_no)
    {
        int n_elements = message_text_document.getDefaultRootElement().getElementCount();
        String text = "";
        if (element_no >= 0 && element_no < n_elements)
        {
            Element element = message_text_document.getDefaultRootElement().getElement(element_no);
            try
            {
                text = message_text_document.getText (element.getStartOffset(), 
                                                      element.getEndOffset() - element.getStartOffset());
            }
            catch (BadLocationException e) { text = ""; }
        }
        return text;
    }
    private int LinePositionToElementNo (LinePosition pos)
    {
        int element_no;
        switch (pos.getLineType())
        {
            case CHECKING_ERROR_MESSAGE: element_no = calcParagraphNumber(ParagraphPosition.CHECKING_ERROR_FIRST_MESSAGE) + pos.getLineNumber(); break;
            case SEC_MIN_DIFF_MESSAGE: element_no = calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_FIRST_MESSAGE) + pos.getLineNumber(); break;
            case SEC_SELF_DIFF_MESSAGE: element_no = calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_FIRST_MESSAGE) + pos.getLineNumber(); break;
            default: element_no = -1; break;
        }
        return element_no;
    }
    private int calcParagraphNumber (ParagraphPosition para_pos)
    {
        int n_lines_checking_err_msgs = n_checking_err_msgs == 0 ? 1 : n_checking_err_msgs;
        int n_lines_sec_min_diff_msgs = n_sec_min_diff_msgs == 0 ? 1 : n_sec_min_diff_msgs;
        int n_lines_sec_self_diff_msgs = n_sec_self_diff_msgs == 0 ? 1 : n_sec_self_diff_msgs;
        switch (para_pos)
        {
            case CHECKING_ERROR_TITLE: return 0;
            case CHECKING_ERROR_FIRST_MESSAGE: return 1;
            case CHECKING_ERROR_LAST_MESSAGE: return n_lines_checking_err_msgs;
            case SEC_MIN_DIFF_TITLE: return 2 + n_lines_checking_err_msgs;
            case SEC_MIN_DIFF_FIRST_MESSAGE: return 3 + n_lines_checking_err_msgs;
            case SEC_MIN_DIFF_LAST_MESSAGE: return 2 + n_lines_checking_err_msgs + n_lines_sec_min_diff_msgs;
            case SEC_SELF_DIFF_TITLE: return 4 + n_lines_checking_err_msgs + n_lines_sec_min_diff_msgs;
            case SEC_SELF_DIFF_FIRST_MESSAGE: return 5 + n_lines_checking_err_msgs + n_lines_sec_min_diff_msgs;
            case SEC_SELF_DIFF_LAST_MESSAGE: return 4 + n_lines_checking_err_msgs + n_lines_sec_min_diff_msgs + n_lines_sec_self_diff_msgs;
        }
        return -1;
    }
    private LinePosition findLinePosition (int text_pos)
    {
        // find which line in the text pane this position is at by counting the newlines
        String text = "";
        try
        {
            text = message_text_document.getText (0, text_pos);
        }
        catch (BadLocationException e) { return new LinePosition(); }
        int n_newlines = 0;
        for (int i=0; i<text.length(); i++)
        {
            if (text.charAt(i) == '\n') n_newlines ++;
        }
        
        // find whether this text position is in any of the blocks of messages
        // (excluding the error messages - we can't follow these to a data file)
        if (n_sec_min_diff_msgs > 0)
        {
            int first_line = calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_FIRST_MESSAGE);
            if (n_newlines >= first_line &&
                n_newlines <= calcParagraphNumber(ParagraphPosition.SEC_MIN_DIFF_LAST_MESSAGE))
                return new LinePosition (LineType.SEC_MIN_DIFF_MESSAGE, n_newlines - first_line);
        }
        if (n_sec_self_diff_msgs > 0)
        {
            int first_line = calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_FIRST_MESSAGE);
            if (n_newlines >= first_line &&
                n_newlines <= calcParagraphNumber(ParagraphPosition.SEC_SELF_DIFF_LAST_MESSAGE))
                return new LinePosition (LineType.SEC_SELF_DIFF_MESSAGE, n_newlines - first_line);
        }
        return new LinePosition();
    }
    private Style createStyle (String name, boolean bold, boolean underline, boolean italic)
    {
        Style style = MessageTextPane.addStyle (name, null);
        StyleConstants.setBold (style, bold);
        StyleConstants.setUnderline (style, underline);
        StyleConstants.setItalic (style, italic);
        return style;
    }
    private void initMessagePaneDocument ()
    {
        // this sets up the element (paragraph) structure in the text pane
        // at the end of this there will be 8 paragraphs = 8 elements in the underlying document
        // this must correpsond with the calcParagraphNumber logic above
        message_text_document = (DefaultStyledDocument) MessageTextPane.getDocument();
        title_style = createStyle ("Title Style", true, true, false);
        plain_style = createStyle ("Plain Style", false, false, false);
        highlight_style = createStyle("Highlight Style", false, true, true);
        highlighted_line = new LinePosition();
        msg_date_format = CheckingThread.createDateFormatter();
        n_checking_err_msgs = 0;
        n_sec_min_diff_msgs = 0;
        n_sec_self_diff_msgs = 0;
        
        // the next call clears the document, which actually results in it's having a single element of length 1
        // next we append the first title - having done this we remove the original single element, otherwise
        // we will have an extra element at the start of the document that will confuse calcParagraphNumber ()
        MessageTextPane.setText("");
        appendParagraph("Checking error messages", title_style);
        removeParagraph(0);
        appendParagraph("No checking errors", plain_style);
        appendParagraph("", plain_style);
        appendParagraph("Filtered second - minute differences over threshold of xxx nT or xxx min. arc", title_style);
        appendParagraph("No difference values exceed the threshold", plain_style);
        appendParagraph("", plain_style);
        appendParagraph("Second 1st differences over threshold of xxx nT or xxx min. arc", title_style);
        appendParagraph("No difference values exceed the threshold", plain_style);
    }
    
    private void dumpElement (Element el, String spacing)
    {
        System.out.print (spacing);
        System.out.print (el.getName());
        System.out.print (": ");
        System.out.print (el.getStartOffset());
        System.out.print (" - ");
        System.out.print (el.getEndOffset());
        Document document = el.getDocument();
        String encoded_data = null;
        try
        {
            String data = document.getText(el.getStartOffset(), el.getEndOffset() - el.getStartOffset());
            encoded_data = URLEncoder.encode(data, "UTF-8");
        }
        catch (BadLocationException e) { encoded_data = "*** Bad location exception ***"; }
        catch (UnsupportedEncodingException e) { encoded_data = "*** Unsupported encoding exception ***"; }
        System.out.print (" [" + encoded_data + "]");
        System.out.println ();
        if (! el.isLeaf())
        {
            for (int count=0; count<el.getElementCount(); count ++)
                dumpElement (el.getElement(count), spacing + " ");
        }
    }
    
}
