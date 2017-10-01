/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

/**
 *
 * @author smf
 */
public interface CheckingDisplayInterface 
{
    public void setProgress (int percent);
    public void addSecMinDiffMsg (String msg);
    public void addSecSelfDiffMsg (String msg);
    public void addCheckingErrorMsg (String msg);
    
}
