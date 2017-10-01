/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author smf
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({uk.ac.bgs.DataCheck1s.DataCheck1sIT.class, uk.ac.bgs.DataCheck1s.ConversionDialogIT.class, uk.ac.bgs.DataCheck1s.CheckingDialogIT.class})
public class TestSuiteDataCheck1s {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
}
