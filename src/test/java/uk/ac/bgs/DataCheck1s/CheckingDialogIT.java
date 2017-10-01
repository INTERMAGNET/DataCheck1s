/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
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
public class CheckingDialogIT {

    private static TimeZone gmt_timezone;
    private static String obs = "tst";
    private static int year = 2014;
    
    public CheckingDialogIT() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        gmt_timezone = TimeZone.getTimeZone("GMT-0:00");
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test a run of the checking dialog using outputs that have previously been calculated
     */
    @Test
    public void testCheckingDialog() throws IOException {
        System.out.println("* CheckingDialog Integration Test: running CheckingDialog");

        // do a checking operation and write the output into a string
        System.out.println("   Launching dialog...");
        CheckingDialog checking_dialog = new CheckingDialog (null, false, null, null);
        checking_dialog.startNewCheckingThread (obs, year, this.getClass(), "files", 5.0, 1.0, 2.0, 0.5, 10, CLOptions.FilterType.GAUSSIAN);
        while (checking_dialog.isCheckingRunning()) 
        { 
            try { Thread.sleep(100); }
            catch (InterruptedException e) { }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        checking_dialog.saveErrors (ps, new Date ());
        checking_dialog.setVisible(false);
        checking_dialog.dispose();
        List<String> calculated_lines = readLines (new StringReader (baos.toString()));
        
        // get what should be the same data from class structure
        System.out.println("   Comparing saved results...");
        InputStream in = this.getClass().getResourceAsStream("files/CheckingExampleResults.txt");
        List<String> checked_lines = readLines (new InputStreamReader (in));
        
        // test that the two outputs are the same
        assertEquals (checked_lines.size(), calculated_lines.size());
        int n_lines = checked_lines.size();
        if (n_lines > calculated_lines.size()) n_lines = calculated_lines.size();
        for (int count=0; count<n_lines; count++)
        {
            switch (count)
            {
                case 2:     // date/time file created
                case 5:     // folder for input data
                    // don't check these lines
                    break;
                default:
                    assertEquals ("Data file line " + Integer.toString (count+1), checked_lines.get(count), calculated_lines.get(count));
                    break;
            }
        }
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
