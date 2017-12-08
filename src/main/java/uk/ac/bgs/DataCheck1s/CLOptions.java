/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Data.GeomagAbsoluteValue;
import bgs.geophys.library.Misc.DateUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Parser and holder for command line options
 * 
 * @author smf
 */
public class CLOptions 
{

    public enum FilterType {GAUSSIAN, COSINE}
    
    private OptionParser parser;
    
    private OptionSpec<String> iaga_code;
    private OptionSpec<Integer> year;
    private OptionSpec<File> folder;
    private OptionSpec<Double> min_diff_nt;
    private OptionSpec<Double> min_diff_min_arc;
    private OptionSpec<Double> sec_diff_nt;
    private OptionSpec<Double> sec_diff_min_arc;
    private OptionSpec<String> orientation;
    private int decoded_orientation;
    private OptionSpec<String> filter_type;
    private FilterType decoded_filter_type;
    private OptionSpec<File> log_file;
    
    private OptionSpec help;
    private OptionSpec perform_comparison;
    private OptionSpec write_cdf;
    
    private OptionSet options;
    
    public CLOptions ()
    throws OptionException
    {
        GregorianCalendar cal = new GregorianCalendar(DateUtils.gmtTimeZone);
        cal.setTime(new Date ());
        
        parser = new OptionParser();
        
        iaga_code = parser.accepts ("iaga-code", "The IAGA code for the data that you want to process.").withRequiredArg().defaultsTo("");
        year = parser.accepts ("year", "The year for the data that you want to process.").withRequiredArg().ofType(Integer.class).defaultsTo (cal.get (GregorianCalendar.YEAR));
        folder = parser.accepts ("folder", "The folder containing the data files to process.").withRequiredArg().ofType(File.class).defaultsTo(new File (".").getAbsoluteFile());
        min_diff_nt = parser.accepts ("min-diff-nt", "The maximum allowed difference between given and calculated minute values in nT.").withRequiredArg().ofType(Double.class).defaultsTo (5.0);
        min_diff_min_arc = parser.accepts ("min-diff-min-arc", "The maximum allowed difference between given and calculated minute values in minutes of arc.").withRequiredArg().ofType(Double.class).defaultsTo (1.0);
        sec_diff_nt = parser.accepts ("sec-diff-nt", "The maximum allowed first difference in second values in nT.").withRequiredArg().ofType(Double.class).defaultsTo (2.0);
        sec_diff_min_arc = parser.accepts ("sec-diff-min-arc", "The maximum allowed first difference in second values in minutes of arc.").withRequiredArg().ofType(Double.class).defaultsTo (0.5);
        orientation = parser.accepts ("orientation", "The orientation for the CDF output data files: 'native' (to use the orientation from the IAF data), 'XYZ' or 'HDZ'").withRequiredArg().defaultsTo("Native");
        filter_type = parser.accepts ("filter", "The type of filter to use to convert 1-second data to 1-minute values. Use 'Gaussian' for the standard 91-point INTERMAGNET filter, or 'Cosine' for a non-standard 61-point cosine filter").withRequiredArg().defaultsTo("Gaussian");
        log_file = parser.accepts ("log-file", "The path to the log file where errors and threshold exceptions will be written.").withRequiredArg().ofType(File.class).defaultsTo(new File ("DataCheck1s.log"));
        parser.accepts ("nocompress", "Turn off CDF compression.");
        parser.accepts ("debug-ap-comms", "Display communications between this program and autoplot on stdout.");
        
        help = parser.accepts("help", "Print this help and exit.");
        perform_comparison = parser.accepts ("perform-comparison", "Automatically perform the comparison, write the log and exit.");
        write_cdf = parser.accepts ("write-cdf", "Automatically writes CDF data after the comparison is performed");
    }
    
    public void parseCL (String args [])
    throws OptionException, ParseException
    {
        options = parser.parse (args);
        
        switch (orientation.value (options).toLowerCase())
        {
            case "xyz": decoded_orientation = GeomagAbsoluteValue.ORIENTATION_XYZ; break;
            case "hdz": decoded_orientation = GeomagAbsoluteValue.ORIENTATION_HDZ; break;
            case "native": decoded_orientation = GeomagAbsoluteValue.ORIENTATION_UNKNOWN; break;
            default: throw new ParseException ("Unrecognised orientation code: " + orientation.value (options), 0);
        }
        switch (filter_type.value (options).toLowerCase())
        {
            case "gaussian": decoded_filter_type = FilterType.GAUSSIAN; break;
            case "cosine": decoded_filter_type = FilterType.COSINE; break;
            default: throw new ParseException ("Unrecognised filter: " + filter_type.value (options), 0);
        }
    }
    
    public String getIagaCode () { return iaga_code.value(options); }
    public int getYear () { return year.value(options); }
    public File getFolder () { return folder.value(options); }
    public double getMinDiffNt() { return min_diff_nt.value(options); }
    public double getMinDiffMinArc() { return min_diff_min_arc.value(options); }
    public double getSecDiffNt() { return sec_diff_nt.value(options); }
    public double getSecDiffMinArc() { return sec_diff_min_arc.value(options); }
    public int getOrientation() { return decoded_orientation; }
    public FilterType getFilterType () { return decoded_filter_type; }
    public File getLogFile() { return log_file.value(options); }
    public boolean isCompress() { return ! options.has ("nocmompress"); }
    public boolean isDebugAPComms() { return options.has ("debug-ap-comms"); }

    public boolean isHelpRequested () { return options.has (help); }
    public boolean isPerformComparison () { return options.has (perform_comparison); }
    public boolean isWriteCDF () { return options.has (write_cdf); }
    
    public void printHelp (OutputStream stream) throws IOException { parser.printHelpOn (stream); }
    
}
