/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Misc.CheckVersion;
import bgs.geophys.library.Swing.FaultReport;
import bgs.geophys.library.Swing.SwingUtils;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Locale;
import joptsimple.OptionException;

/**
 *
 * @author smf
 */
public class DataCheck1s extends java.lang.ThreadGroup
{

    public static final String PROGRAM_TITLE = "INTERMAGNET One Second Data Checker";
    public static final String PROGRAM_NAME = "DataCheck1s";
    public static final String PROGRAM_VERSION = "1.2_02";
    
    private CLOptions options;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        DataCheck1s data_check_1s;

        // check for minimum java run time
        CheckVersion.CheckDefaultVersion().msgSwing(true);
    
        // set the default locale (prevents other locales from breaking date
        // formatting, file chooser language, etc...
        Locale.setDefault(Locale.UK);

        // create the data check object and check if the user asked for help
        data_check_1s = new DataCheck1s (args);
        if (data_check_1s.getOptions().isHelpRequested())
        {
            data_check_1s.showHelp(System.out);
            System.exit (0);
        }

        // create a thread group to run the main thread in
        // start the application inside this thread group - that way
        // unhandled exceptions from all threads (including SWING/AWT)
        // will be properly handled
        new Thread (data_check_1s, "MainThread") 
        {
            @Override
            public void run ()
            {
                MainFrame main_frame;
                
                // set the default look and feel
                SwingUtils.setPlatformLookAndFeel ();

                // show the main window
                main_frame = new MainFrame (data_check_1s.getOptions(), PROGRAM_TITLE, PROGRAM_VERSION);
                main_frame.setVisible(true);
            }
        }.start ();
    }

    /** constructor for thread group */
    public DataCheck1s (String args [])
    {
        super ("DataCheck1sThreadGroup");
        
        // load the options from the command line
        try
        {
            options = new CLOptions();
            options.parseCL(args);
        }
        catch (OptionException | ParseException e)
        {
            System.err.println ("Unreocgnised command line option");
            if (e.getMessage() != null) System.err.println (e.getMessage());
            System.err.println ();
            showHelp (System.out);
            System.exit (1);
        }
    }
    
    public CLOptions getOptions () { return options; }
    
    public void showHelp (PrintStream stream)
    {
        stream.println (PROGRAM_NAME + ": Check INTERMAGNET 1-second data against 1-minute data");
        stream.println ("             and write out 1-second data in INTERMAGNET CDF format");
        stream.println ();
        try
        {
             options.printHelp(stream);
        }
        catch (IOException e) { }        
        stream.println ();
        stream.println ("Version " + PROGRAM_VERSION);
    }
     
    /** catch unhandled exceptions in all threads */
    public void uncaughtException(Thread t, Throwable e)  
    {
        String report_fields [];
        FaultReport fault_report;
        
        report_fields = new String [3];
        report_fields [0] = "What part of the software were you using: ";
        report_fields [1] = "What data were you accessing: ";
        report_fields [2] = "Any other information: ";
        fault_report = new FaultReport (null, true, 
                                        PROGRAM_NAME + " Version " + PROGRAM_VERSION,
                                        "smf@bgs.ac.uk", report_fields, e);
        fault_report.setVisible (true);
      
        System.exit (1);
    }
    
}
