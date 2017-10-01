/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author smf
 */
public class DataCheck1sIT {
    
    public DataCheck1sIT() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of showHelp method, of class DataCheck1s.
     */
    @Test
    public void testShowHelp() {
        System.out.println("* DataCheck1s Integration Test: showHelp");
        DataCheck1s instance = new DataCheck1s (new String [0]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        instance.showHelp (ps);
        String content = baos.toString ();
        assertTrue (content.length() > 0);
        assertTrue(content.indexOf(DataCheck1s.PROGRAM_VERSION) > 0);
    }

    
}
