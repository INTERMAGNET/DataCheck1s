/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Data.GeomagAbsoluteValue;
import bgs.geophys.library.Threads.ProcessMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author smf
 */
public class ConversionDialogIT {
    
    private static TimeZone gmt_timezone;
    private static String obs = "tst";
    private static int year = 2014;
    
    public ConversionDialogIT() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        gmt_timezone = TimeZone.getTimeZone("GMT-0:00");
    }
    
    @AfterClass
    public static void tearDownClass() {
    }


    /**
     * Test a run of the conversion dialog using outputs that have previously been calculated
     */
    @Test
    public void testConversionDialog() throws IOException {
        System.out.println("* CoversionDialog Integration Test: running ConversionDialog");
        
        // set up a temporary folder for data files
        Path tmp_path = Files.createTempDirectory("TestConversionDialog");
        
        // do a conversion operation, then use CDFDump (from the CDF toolkit) to dump the 
        // CDF file to text and compare with a previously converted file
        System.out.println("   Launching dialog...");
        ConversionDialog conversion_dialog = new ConversionDialog (null, false, null);
        conversion_dialog.startNewConversionThread (obs, year, this.getClass(), "files", tmp_path.toFile(), GeomagAbsoluteValue.ORIENTATION_HDZ, true);
        while (conversion_dialog.isConversionRunning())
        { 
            try { Thread.sleep(100); }
            catch (InterruptedException e) { }
        }
        conversion_dialog.setVisible(false);
        conversion_dialog.dispose();
        
        // check the expected CDF file exists, dump it and capture the output
        System.out.println("   Dumping CDF to ASCII...");
        File cdf_file = new File (tmp_path.toFile(), "tst_20140101_000000_pt1s_4.cdf");
        assertTrue (cdf_file.exists());
        ProcessMonitor pm = new ProcessMonitor ("cdfdump -r 1,1000 " + cdf_file.getAbsolutePath());
        pm.startProcess();
        while (pm.isProcessAlive())
        { 
            try { Thread.sleep(100); }
            catch (InterruptedException e) { }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        List<String> calculated_lines = readLines (new StringReader (pm.getStdoutData()));
        
        // get what should be the same data from class structure
        System.out.println("   Comparing CDF dump files...");
        InputStream in = this.getClass().getResourceAsStream("files/tst_20140101_000000_4.cdf.dump");
        List<String> checked_lines = readLines (new InputStreamReader (in));
        
        // test that the two outputs are the same
        assertEquals (checked_lines.size(), calculated_lines.size());
        int n_lines = checked_lines.size();
        if (n_lines > calculated_lines.size()) n_lines = calculated_lines.size();
        for (int count=0; count<n_lines; count++)
        {
            switch (count)
            {
                case 0:     // name of CDF file
                case 4:     // CDF version
                case 7:     // Copyright years
                case 38:    // Publication date
                    // don't check these lines
                    break;
                default:
                    assertEquals ("Data file line " + Integer.toString (count+1), checked_lines.get(count), calculated_lines.get(count));
                    break;
            }
        }
        
        // tidy up the temporary folder
        File files [] = tmp_path.toFile().listFiles();
        for (File file : files) file.delete();
        tmp_path.toFile().delete();
        System.out.println("   Check completed...");
    }
    
    private List<String> readLines (Reader reader)
    throws IOException
    {
        List<String> lines = new ArrayList<> ();
        BufferedReader br =  new BufferedReader (reader);
        String line;
        while ((line = br.readLine()) != null) lines.add(line);
        br.close();
        return lines;
    }
    
}
