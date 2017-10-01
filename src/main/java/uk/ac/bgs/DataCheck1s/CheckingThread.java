/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Data.GeomagAbsoluteValue;
import bgs.geophys.library.Data.GeomagDataFilename.Case;
import bgs.geophys.library.Data.GeomagDataFilename.Interval;
import bgs.geophys.library.Data.GeomagDataFilename.QualityType;
import bgs.geophys.library.Data.GeomagDataFormat;
import bgs.geophys.library.Data.GeomagDataFormat.DurationType;
import bgs.geophys.library.Data.Iaga2002;
import bgs.geophys.library.Data.Iaga2002Filename;
import bgs.geophys.library.Data.ImagCD.ImagCDDataDay;
import bgs.geophys.library.Data.ImagCD.ImagCDFile;
import bgs.geophys.library.Data.ImagCDFilename;
import bgs.geophys.library.File.ClasspathFileFactory;
import bgs.geophys.library.File.ClasspathFileInterface;
import bgs.geophys.library.Maths.FIRFilter;
import bgs.geophys.library.Misc.DateUtils;
import bgs.geophys.library.Misc.GeoString;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 *
 * @author smf
 */
public class CheckingThread extends Thread
{

    enum CheckingState { NOT_STARTED_YET, NORMAL, IAGA_AND_IAF_NOT_AVAIL, IAF_NOT_AVAIL, IAGA_NOT_AVAIL, THERSHOLD_EXCEEDED; }
    
    private String obs;
    private int year;
    private double min_diff_field_threshold;
    private double min_diff_angle_threshold;
    private double sec_diff_field_threshold;
    private double sec_diff_angle_threshold;
    private int max_missing_pc;
    private CLOptions.FilterType filter_type;
    private boolean cancelled;
    private CheckingDisplayInterface conversion_display;
    private int iaf_orientation;
    private ClasspathFileFactory classpath_file_factory;

    public static SimpleDateFormat createDateFormatter ()
    {
        SimpleDateFormat date_format = new SimpleDateFormat ("yyyy-MM-dd");
        date_format.setTimeZone(DateUtils.gmtTimeZone);        
        return date_format;
    }
    
    public CheckingThread (String obs, int year, File base_folder, double min_diff_field_threshold, double min_diff_angle_threshold,
                             double sec_diff_field_threshold, double sec_diff_angle_threshold,
                             int max_missing_pc, CLOptions.FilterType filter_type, CheckingDisplayInterface conversion_display)
    {
        this.obs = obs;
        this.year = year;
        iaf_orientation = GeomagAbsoluteValue.ORIENTATION_UNKNOWN;
        classpath_file_factory = new ClasspathFileFactory (base_folder);
        this.min_diff_field_threshold = min_diff_field_threshold;
        this.min_diff_angle_threshold = min_diff_angle_threshold;
        this.sec_diff_field_threshold = sec_diff_field_threshold;
        this.sec_diff_angle_threshold = sec_diff_angle_threshold;
        this.max_missing_pc = max_missing_pc;
        this.filter_type = filter_type;
        this.cancelled = false;
        this.conversion_display = conversion_display;
    }
    
    public CheckingThread (String obs, int year, Class base_reference, String relative_reference, double min_diff_field_threshold, double min_diff_angle_threshold,
                             double sec_diff_field_threshold, double sec_diff_angle_threshold,
                             int max_missing_pc, CLOptions.FilterType filter_type, CheckingDisplayInterface conversion_display)
    {
        this.obs = obs;
        this.year = year;
        iaf_orientation = GeomagAbsoluteValue.ORIENTATION_UNKNOWN;
        classpath_file_factory = new ClasspathFileFactory (base_reference, relative_reference);
        this.min_diff_field_threshold = min_diff_field_threshold;
        this.min_diff_angle_threshold = min_diff_angle_threshold;
        this.sec_diff_field_threshold = sec_diff_field_threshold;
        this.sec_diff_angle_threshold = sec_diff_angle_threshold;
        this.max_missing_pc = max_missing_pc;
        this.filter_type = filter_type;
        this.cancelled = false;
        this.conversion_display = conversion_display;
    }
       
    public void cancel ()
    {
        this.cancelled = true;
        conversion_display.addCheckingErrorMsg("Checking cancelled");
    }
    
    @Override
    public void run() 
    {
        
        conversion_display.setProgress (0);
        
        // check that the files we want are in the folder or base url
        GregorianCalendar cal = new GregorianCalendar (DateUtils.gmtTimeZone);
        cal.set (year, 0, 1, 0, 0, 0);
        cal.set (GregorianCalendar.MILLISECOND, 0);
        cal.add (GregorianCalendar.DAY_OF_MONTH, -1);
        int n_iaga_files_found = 0;
        int n_iaf_files_found = 0;
        ClasspathFileInterface iaga_files [], iaf_files [];
        int n_possible_iaga_files = DateUtils.daysInYear(year) +2;
        int n_possible_iaf_files = 12;
        iaga_files = new ClasspathFileInterface[n_possible_iaga_files];
        iaf_files = new ClasspathFileInterface [n_possible_iaf_files];
        for (int day_count=0; (day_count<n_possible_iaga_files) && (! cancelled); day_count++, cal.add (GregorianCalendar.DAY_OF_MONTH, 1))
        {
            // find IAGA-2002 files
            Iaga2002Filename iaga_2002_filename = new Iaga2002Filename (obs, cal.getTime(), QualityType.DEFINITIVE, Interval.SECOND, false, DurationType.DAY, Case.LOWER);
            iaga_files [day_count] = classpath_file_factory.getClasspathFile (iaga_2002_filename.getFilename());
            if (iaga_files[day_count].exists()) n_iaga_files_found ++;
            else
            {
                iaga_2002_filename = new Iaga2002Filename (obs, cal.getTime(), QualityType.DEFINITIVE, Interval.SECOND, false, DurationType.DAY, Case.UPPER);
                iaga_files [day_count] = classpath_file_factory.getClasspathFile (iaga_2002_filename.getFilename());
                if (iaga_files[day_count].exists()) n_iaga_files_found ++;
                else iaga_files[day_count] = null;
            }

            // find IAF files
            if (cal.get(GregorianCalendar.DAY_OF_MONTH) == 1 && cal.get(GregorianCalendar.YEAR) == year)
            {
                int month = cal.get (GregorianCalendar.MONTH);
                ImagCDFilename iaf_filename = new ImagCDFilename (obs, cal.getTime(), false, ImagCDFilename.Case.LOWER);
                iaf_files [month] = classpath_file_factory.getClasspathFile (iaf_filename.getFilename());
                if (iaf_files[month].exists()) n_iaf_files_found ++;
                else
                {
                    iaf_filename = new ImagCDFilename (obs, cal.getTime(), true, ImagCDFilename.Case.LOWER);
                    iaf_files [month] = classpath_file_factory.getClasspathFile (iaf_filename.getFilename());
                    if (iaf_files[month].exists()) n_iaf_files_found ++;
                    else
                    {
                        iaf_filename = new ImagCDFilename (obs, cal.getTime(), false, ImagCDFilename.Case.UPPER);
                        iaf_files [month] = classpath_file_factory.getClasspathFile (iaf_filename.getFilename());
                        if (iaf_files[month].exists()) n_iaf_files_found ++;
                        else
                        {
                            iaf_filename = new ImagCDFilename (obs, cal.getTime(), true, ImagCDFilename.Case.UPPER);
                            iaf_files [month] = classpath_file_factory.getClasspathFile (iaf_filename.getFilename());
                            if (iaf_files[month].exists()) n_iaf_files_found ++;
                            else iaf_files[month] = null;
                        }
                    }
                }
            }
        }
        if (n_iaga_files_found == 0 || n_iaf_files_found == 0)
        {
            if (n_iaga_files_found <= 0)
                conversion_display.addCheckingErrorMsg ("No 1-second IAGA 2002 data files found: can't do data checking");
            if (n_iaf_files_found <= 0)
                conversion_display.addCheckingErrorMsg ("No 1-minute IAF 2002 data files found: can't do data checking");
            conversion_display.setProgress (100);
            return;
        }
        if (n_iaga_files_found < n_possible_iaga_files)
            conversion_display.addCheckingErrorMsg ("Not all second data available: " + 
                                                     Integer.toString (n_iaga_files_found) + " of " +
                                                     Integer.toString (n_possible_iaga_files) +
                                                     " IAGA 2002 files found."); 
        if (n_iaf_files_found < n_possible_iaf_files)
            conversion_display.addCheckingErrorMsg ("Not all minute data available: " + 
                                                     Integer.toString (n_iaf_files_found) + " of " +
                                                     Integer.toString (n_possible_iaf_files) +
                                                     " IAF files found."); 
        
        // create INTERMAGNET Gaussian filter - see http://www.intermagnet.org/publication-software/technicalsoft-eng.php
        FIRFilter filter;
        switch (filter_type)
        {
            case COSINE:
                filter = new FIRFilter(FIRFilter.FILTER_COSINE, 61, 0.0, 0.0, Iaga2002.MISSING_DATA_SAMPLE, (61 * max_missing_pc) / 100);
                break;
            case GAUSSIAN:
                filter = new FIRFilter(FIRFilter.FILTER_GAUSS, 91, 1.0, 15.90062181, Iaga2002.MISSING_DATA_SAMPLE, (91 * max_missing_pc) / 100);
                break;
            default:
                conversion_display.addCheckingErrorMsg ("Unrecognised filter type [" + filter_type.name() + "], using Gaussian filter");
                filter = new FIRFilter(FIRFilter.FILTER_GAUSS, 91, 1.0, 15.90062181, Iaga2002.MISSING_DATA_SAMPLE, (91 * max_missing_pc) / 100);
                break;
        }
        
        // when you get here:
        //   iaga_files contains [NDaysInyear +2] entries, 1 per day file of second data (the first file
        //     is for 31st Dec the year before the given year, the last file for 12st Jan the following year
        //   iaf_files contains [12] entries, 1 per month for the year specified
        SimpleDateFormat date_format = createDateFormatter();
        cal.set (year, 0, 1, 0, 0, 0);
        Iaga2002 today_iaga_2002, tomorrow_iaga_2002;
        today_iaga_2002 = loadIaga2002(iaga_files[0]);
        tomorrow_iaga_2002 = loadIaga2002(iaga_files[1]);
        ImagCDFile iaf_data = null;
        for (int day_count=0; (day_count<DateUtils.daysInYear(year)) && (! cancelled); day_count ++, cal.add (GregorianCalendar.DAY_OF_MONTH, 1))
        {
            // load the second data for this day and the immediately surrounding days
            Iaga2002 yesterday_iaga_2002 = today_iaga_2002;
            today_iaga_2002 = tomorrow_iaga_2002;
            if (today_iaga_2002 == null) today_iaga_2002 = loadIaga2002 (iaga_files[day_count +1]);
            tomorrow_iaga_2002 = loadIaga2002(iaga_files[day_count +2]);

            // check orientation codes between IAGA days
            if (yesterday_iaga_2002 != null && today_iaga_2002 != null)
            {
                String codes1 = today_iaga_2002.getComponentCodes().trim().toUpperCase();
                if (codes1.length() > 3) codes1 = codes1.substring(0, 3);
                String codes2 = yesterday_iaga_2002.getComponentCodes().trim().toUpperCase();
                if (codes2.length() > 3) codes2 = codes2.substring(0, 3);
                if (! codes1.equalsIgnoreCase(codes2))
                {
                    conversion_display.addCheckingErrorMsg (date_format.format(cal.getTime()) + ": orientation code changes between this day and previous, first minute sample of day not checked");
                    tomorrow_iaga_2002 = null;
                }
            }
            if (tomorrow_iaga_2002 != null && today_iaga_2002 != null)
            {
                String codes1 = today_iaga_2002.getComponentCodes().trim().toUpperCase();
                if (codes1.length() > 3) codes1 = codes1.substring(0, 3);
                String codes2 = tomorrow_iaga_2002.getComponentCodes().trim().toUpperCase();
                if (codes2.length() > 3) codes2 = codes2.substring(0, 3);
                if (! codes1.equalsIgnoreCase(codes2))
                {
                    conversion_display.addCheckingErrorMsg (date_format.format(cal.getTime()) + ": orientation code changes between this day and next, last minute sample of day not checked");
                    tomorrow_iaga_2002 = null;
                }
            }
            
            // load the minute data for this month
            int month = cal.get(GregorianCalendar.MONTH);
            if (cal.get(GregorianCalendar.DAY_OF_MONTH) == 1 && cal.get(GregorianCalendar.YEAR) == year)
            {
                if (iaf_files[month] == null) iaf_data = null;
                else
                {
                    iaf_data = new ImagCDFile ();
                    String msg = iaf_data.loadFromFile(iaf_files[month]);
                    if (msg != null)
                    {
                        iaf_data = null;
                        conversion_display.addCheckingErrorMsg("IAF: Unable to load file [" + iaf_files[month].getName() + "]: " + msg);
                    }
                }
            }
            
            // when you get to this point:
            //   the date for the day we are working on is in 'cal'
            //   the IAGA 2002 data for this date is in 'today_iaga_2002' with yesterday's and
            //      tomorrow's data in yesterday_iaga_2002 and tomorrow_iaga_2002
            //   the IAF data for this month is in iaf_data 
            if (today_iaga_2002 != null && iaf_data != null)
            {
                // get a reference to this day's IAF data
                ImagCDDataDay iaf_day = iaf_data.getDataDay (cal.get (GregorianCalendar.DAY_OF_MONTH));
                
                // find the orientation for the IAF data
                String iaf_element_codes = iaf_day.getRecordedElements().trim().toUpperCase();
                if (iaf_element_codes.length() >= 3 && iaf_element_codes.length() <= 4)
                {
                    if (iaf_element_codes.substring(0, 3).equalsIgnoreCase("HDZ"))
                        iaf_orientation = GeomagAbsoluteValue.ORIENTATION_HDZ;
                    else if (iaf_element_codes.substring(0, 3).equalsIgnoreCase("XYZ"))
                        iaf_orientation = GeomagAbsoluteValue.ORIENTATION_XYZ;
                    else if (iaf_element_codes.substring(0, 3).equalsIgnoreCase("DIF"))
                        iaf_orientation = GeomagAbsoluteValue.ORIENTATION_DIF;
                }
                if (iaf_orientation == GeomagAbsoluteValue.ORIENTATION_UNKNOWN) 
                    conversion_display.addCheckingErrorMsg (date_format.format(cal.getTime()) + ": unrecognised geomagnetic orientation in IAF data: " + iaf_element_codes);
                else
                {
                    // copy the minute data for this day to an array of GeomagAbsoluteValue objects
                    GeomagAbsoluteValue iaf_minute_data [];
                    if (iaf_data == null) iaf_minute_data = null;
                    else
                    {
                        iaf_minute_data = new GeomagAbsoluteValue [1440];
                        for (int minute_count=0; minute_count < iaf_minute_data.length; minute_count ++)
                        {
                            iaf_minute_data [minute_count] = new GeomagAbsoluteValue (extractIAFSample (iaf_day, 0, minute_count),
                                                                                      extractIAFSample (iaf_day, 1, minute_count),
                                                                                      extractIAFSample (iaf_day, 2, minute_count),
                                                                                      GeomagDataFormat.MISSING_DATA_SAMPLE, iaf_orientation, GeomagAbsoluteValue.ANGLE_MINUTES);
                        }
                    }

                    // for each vector element in the IAGA file...
                    String iaga_element_codes = today_iaga_2002.getComponentCodes().trim().toUpperCase();
                    if (iaga_element_codes.length() < 3 || iaga_element_codes.length() > 4)
                        conversion_display.addCheckingErrorMsg (date_format.format(cal.getTime()) + ": unrecognised geomagnetic orientation in IAGA 2002 data: " + iaga_element_codes);
                    else
                    {
                        for (int iaga_element_count=0; iaga_element_count<3; iaga_element_count++)
                        {
                            // does this component exist in the GeomagAbsoluteValue that the IAF data has been copied to?
                            String iaga_element_code = iaga_element_codes.substring(iaga_element_count, iaga_element_count +1);
                            int iaf_element_code = GeomagAbsoluteValue.COMPONENT_UNKNOWN;
                            double min_threshold = 0;
                            double sec_threshold = 0;
                            String units = "Unknown";
                            switch (iaga_element_code)
                            {
                                case "h": case "H": iaf_element_code = GeomagAbsoluteValue.COMPONENT_H; min_threshold = min_diff_field_threshold; sec_threshold = sec_diff_field_threshold; units = "nT"; break;
                                case "d": case "D": iaf_element_code = GeomagAbsoluteValue.COMPONENT_D; min_threshold = min_diff_angle_threshold; sec_threshold = sec_diff_angle_threshold; units = "min"; break;
                                case "z": case "Z": iaf_element_code = GeomagAbsoluteValue.COMPONENT_Z; min_threshold = min_diff_field_threshold; sec_threshold = sec_diff_field_threshold; units = "nT"; break;
                                case "x": case "X": iaf_element_code = GeomagAbsoluteValue.COMPONENT_X; min_threshold = min_diff_field_threshold; sec_threshold = sec_diff_field_threshold; units = "nT"; break;
                                case "y": case "Y": iaf_element_code = GeomagAbsoluteValue.COMPONENT_Y; min_threshold = min_diff_field_threshold; sec_threshold = sec_diff_field_threshold; units = "nT"; break;
                                case "i": case "I": iaf_element_code = GeomagAbsoluteValue.COMPONENT_I; min_threshold = min_diff_angle_threshold; sec_threshold = sec_diff_angle_threshold; units = "min"; break;
                                case "f": case "F": iaf_element_code = GeomagAbsoluteValue.COMPONENT_F; min_threshold = min_diff_field_threshold; sec_threshold = sec_diff_field_threshold; units = "nT"; break;
                            }
                            if (iaf_element_code == GeomagAbsoluteValue.COMPONENT_UNKNOWN)
                                conversion_display.addCheckingErrorMsg (date_format.format(cal.getTime()) + ": unrecognised geomagnetic orientation code in IAGA 2002 data: " + iaga_element_code);
                            else
                            {
                                // extract the 1-second IAGA data for this element as an array
                                double today_data [] = new double [today_iaga_2002.getDataLength ()];
                                for (int sample_count=0; sample_count<today_data.length; sample_count++)
                                    today_data [sample_count] = today_iaga_2002.getData(iaga_element_count, sample_count, Iaga2002.MISSING_DATA_SAMPLE, Iaga2002.MISSING_DATA_SAMPLE);
                                double yesterday_data [];
                                if (yesterday_iaga_2002 == null) yesterday_data = null;
                                else if (yesterday_iaga_2002.getDataLength() < filter.getNCoeffs()) yesterday_data = null;
                                else
                                {
                                    yesterday_data = new double [filter.getNCoeffs()];
                                    for (int sample_count=0; sample_count<yesterday_data.length; sample_count++)
                                    {
                                        int index = yesterday_iaga_2002.getDataLength() - (yesterday_data.length - sample_count);
                                        yesterday_data [sample_count] = yesterday_iaga_2002.getData(iaga_element_count, index, Iaga2002.MISSING_DATA_SAMPLE, Iaga2002.MISSING_DATA_SAMPLE);
                                    }
                                }
                                double tomorrow_data [];
                                if (tomorrow_iaga_2002 == null) tomorrow_data = null;
                                else if (tomorrow_iaga_2002.getDataLength() < filter.getNCoeffs()) tomorrow_data = null;
                                else
                                {
                                    tomorrow_data = new double [filter.getNCoeffs()];
                                    for (int sample_count=0; sample_count<tomorrow_data.length; sample_count++)
                                        tomorrow_data [sample_count] = tomorrow_iaga_2002.getData(iaga_element_count, sample_count, Iaga2002.MISSING_DATA_SAMPLE, Iaga2002.MISSING_DATA_SAMPLE);
                                }

                                // filter the 1-second data to 1-minute data
                                int day_length = today_data.length / 60;
                                double iaga_minute_data [] = new double [day_length];
                                doFilter (filter, yesterday_data, today_data, tomorrow_data, iaga_minute_data); 

                                // compare against the IAF data - the 'state' variable is used to
                                // hold the state in which the comparison exists:
                                //   NOT_STARTED_YET           not started yet;
                                //   NORMAL                    IAGA and IAF data available, threshold not exceeded
                                //   IAGA_AND_IAF_NOT_AVAIL    IAGA and IAF data not available
                                //   IAF_NOT_AVAIL             IAGA data available, IAF data not available
                                //   IAGA_NOT_AVAIL            IAGA data not available, IAF data available
                                //   THERSHOLD_EXCEEDED        IAGA and IAF data available, threshold exceeded
                                // A transition from IAGA__AND_IAF_NOT_AVAIL to a state below in the table is the start of an exception condition
                                // A transition from IAF_NOT_AVAIL to a state above in the table is the end of an exception condition 
                                //   and the exception type will be in the 'last_state' variable
                                String exception_time_list = "";
                                CheckingState state = CheckingState.NOT_STARTED_YET;
                                CheckingState last_state = CheckingState.NOT_STARTED_YET;
                                int sample_count_at_exception_start = 0;
                                double max_diff = 0.0;
                                for (int sample_count=0; sample_count<day_length; sample_count ++)
                                {
                                    last_state = state;
                                    
                                    // work out the state for this minute
                                    double iaf_sample = iaf_minute_data[sample_count].getComponent(iaf_element_code, GeomagAbsoluteValue.ANGLE_MINUTES);
                                    if (iaf_sample == GeomagDataFormat.MISSING_DATA_SAMPLE)
                                    {
                                        if (iaga_minute_data [sample_count] == GeomagDataFormat.MISSING_DATA_SAMPLE)
                                            state = CheckingState.IAGA_AND_IAF_NOT_AVAIL;
                                        else
                                            state = CheckingState.IAF_NOT_AVAIL;
                                    }
                                    else
                                    {
                                        if (iaga_minute_data [sample_count] == Iaga2002.MISSING_DATA_SAMPLE)
                                            state = CheckingState.IAGA_NOT_AVAIL;
                                        else
                                            state = CheckingState.NORMAL;
                                    }
                                    
                                    // if we have data available from both sources we can do a comparison
                                    if (state == CheckingState.NORMAL)
                                    {
                                        double diff = Math.abs (iaf_sample - iaga_minute_data [sample_count]);
                                        if (diff > min_threshold)
                                        {
                                            state = CheckingState.THERSHOLD_EXCEEDED;
                                            if (last_state != CheckingState.THERSHOLD_EXCEEDED || diff > max_diff)
                                                max_diff = diff;
                                        }
                                    }
                                
                                    // record the exception - there are three types:
                                    //    1.) Difference threshold exceeded
                                    //    2.) Gap in IAGA 1-second data but not IAF minute data
                                    //    3.) Gap in IAF minute data but not IAGA 1-second data
                                    if (last_state != state)
                                    {
                                        switch (last_state)
                                        {
                                            case IAF_NOT_AVAIL:
                                                exception_time_list += formatGapException(exception_time_list.length() > 0, 
                                                                                       sample_count_at_exception_start * 60, 
                                                                                       (sample_count -1) * 60, false);
                                                break;
                                            case IAGA_NOT_AVAIL:
                                                exception_time_list += formatGapException(exception_time_list.length() > 0, 
                                                                                       sample_count_at_exception_start * 60, 
                                                                                       (sample_count -1) * 60, true);
                                                break;
                                            case THERSHOLD_EXCEEDED:
                                                exception_time_list += formatDiffException(exception_time_list.length() > 0, 
                                                                                       sample_count_at_exception_start * 60, 
                                                                                       (sample_count -1) * 60, 
                                                                                       max_diff, 
                                                                                       units);
                                                break;
                                        }
                                        sample_count_at_exception_start = sample_count;
                                    }
                                }
                            
                                // process the end of any exception and record the sec->min exceptions for this day/element
                                switch (state)
                                {
                                    case IAF_NOT_AVAIL:
                                        exception_time_list += formatGapException(exception_time_list.length() > 0, 
                                                                               sample_count_at_exception_start * 60, 
                                                                               86400 - 60, false);
                                        break;
                                    case IAGA_NOT_AVAIL:
                                        exception_time_list += formatGapException(exception_time_list.length() > 0, 
                                                                               sample_count_at_exception_start * 60, 
                                                                               86400 - 60, true);
                                        break;
                                    case THERSHOLD_EXCEEDED:
                                        exception_time_list += formatDiffException(exception_time_list.length() > 0, 
                                                                               sample_count_at_exception_start * 60, 
                                                                               86400 - 60, 
                                                                               max_diff, 
                                                                               units);
                                        break;
                                }
                                if (exception_time_list.length() > 0)
                                    conversion_display.addSecMinDiffMsg (date_format.format(cal.getTime()) + "," + iaga_element_code + ": " + exception_time_list);

                                // take the first difference of the 1-second data
                                double last_sample, this_sample = GeomagDataFormat.MISSING_DATA_SAMPLE;
                                exception_time_list = "";
                                int in_exception = -1;
                                max_diff = 0.0;
                                for (int sample_count=0; sample_count<=today_data.length; sample_count++)
                                {
                                    if (sample_count == 0)
                                        last_sample = yesterday_data == null ? Iaga2002.MISSING_DATA_SAMPLE : yesterday_data [yesterday_data.length -1];
                                    else
                                        last_sample = this_sample;
                                    if (sample_count == today_data.length)
                                        this_sample = tomorrow_data == null ? Iaga2002.MISSING_DATA_SAMPLE : tomorrow_data [0];
                                    else
                                    this_sample = today_data [sample_count];
                                    if (last_sample != Iaga2002.MISSING_DATA_SAMPLE && this_sample != Iaga2002.MISSING_DATA_SAMPLE)
                                    {
                                        double diff = Math.abs (last_sample - this_sample);
                                        if (diff > sec_threshold)
                                        {
                                            if (in_exception < 0) 
                                            {
                                                in_exception = sample_count;
                                                max_diff = diff;
                                            }
                                            if (diff > max_diff) max_diff = diff;
                                        }
                                        else if (in_exception >= 0)
                                        {
                                            exception_time_list += formatDiffException(exception_time_list.length() > 0, in_exception, sample_count -1, max_diff, units);
                                            in_exception = -1;
                                        }
                                    }
                                }
                                if (in_exception >= 0)
                                    exception_time_list += formatDiffException(exception_time_list.length() > 0, in_exception, 86400 -1, max_diff, units);
                                if (exception_time_list.length() > 0)
                                    conversion_display.addSecSelfDiffMsg (date_format.format(cal.getTime()) + "," + iaga_element_code + ": " + exception_time_list);
                            }
                        }
                    }
                }
            }    
            conversion_display.setProgress ((day_count * 100) / DateUtils.daysInYear(year));
        }
        
        conversion_display.setProgress (100);
    }

    private double extractIAFSample (ImagCDDataDay iaf_day, int component_count, int sample_count)
    {
        int int_val = iaf_day.getMinuteData (component_count, sample_count);
        if (int_val == ImagCDDataDay.MISSING_DATA) return GeomagDataFormat.MISSING_DATA_SAMPLE;
        if (int_val == ImagCDDataDay.MISSING_COMPONENT) return GeomagDataFormat.MISSING_DATA_SAMPLE;
        return (double) int_val / 10.0;                    
    }

    private Iaga2002 loadIaga2002 (ClasspathFileInterface file)
    {
        Iaga2002 iaga2002 = null;
        
        if (file != null)
        {
            try
            {
                iaga2002 = Iaga2002.read (file.openInputStream());
                if (iaga2002.getSamplePeriod() != 1000)
                {
                    conversion_display.addCheckingErrorMsg("Iaga2002 file must contain 1-second data: " + file.getName());
                    iaga2002 = null;
                }
                if (iaga2002.getDataLength() != 86400)
                {
                    conversion_display.addCheckingErrorMsg("Iaga2002 file must contain exactly one day of data: " + file.getName());
                    iaga2002 = null;
                }
            }
            catch (IOException ex) 
            { 
                String msg;
                msg = "Iaga 2002: Unable to load file [" + file.getName() + "]";
                if (ex.getMessage() != null) msg += ": " + ex.getMessage();
                conversion_display.addCheckingErrorMsg(msg);
                iaga2002 = null; 
            }
        }
        
        return iaga2002;
    }
    
    private void doFilter (FIRFilter filter, double src_before [], double src_data [], double src_after [], double dest_data [])
    {        
        // check that the chunks of data before and after the main source data are large enough
        int n_coeffs = filter.getNCoeffs();
        int half_n_coeffs = n_coeffs / 2;
        int points_per_mean = src_data.length / dest_data.length;
        
        // work through the body of the source data
        for (int min_count=0; min_count<dest_data.length; min_count++)
        {
            // calculate the array index of the first and last points of 1-sec data to pass to the filter
            int start_index = (min_count * points_per_mean) - half_n_coeffs;
            int end_index = start_index + n_coeffs -1;
            if (src_before == null) src_before = new double [0];
            if (src_after == null) src_after = new double [0];
            
            // do we need to include data from before the start of the main data array ??
            if (start_index < 0)
            {
                double array [] = new double [n_coeffs];
                int data_offset, before_offset;
                for (int sec_count=0; sec_count<array.length; sec_count++)
                {
                    data_offset =  start_index + sec_count;
                    before_offset = src_before.length + data_offset;
                    if (data_offset >= 0)
                        array [sec_count] = src_data [data_offset];
                    else if (before_offset >= src_before.length || before_offset < 0)
                        array [sec_count] = Iaga2002.MISSING_DATA_SAMPLE;
                    else
                        array [sec_count] = src_before [before_offset];
                }
                dest_data [min_count] = filter.applyFilter(array);
            }
            // do we need to include data from after the end of the main data array ??
            else if (end_index >= src_data.length)
            {
                double array [] = new double [n_coeffs];
                int data_offset, after_offset;
                for (int sec_count=0; sec_count<array.length; sec_count++)
                {
                    data_offset = start_index + sec_count;
                    after_offset = data_offset - src_data.length;
                    if (data_offset < src_data.length)
                        array [sec_count] = src_data [data_offset];
                    else if (after_offset >= src_after.length || after_offset < 0)
                        array [sec_count] = Iaga2002.MISSING_DATA_SAMPLE;
                    else
                        array [sec_count] = src_after [after_offset];
                }                
                dest_data [min_count] = filter.applyFilter(array);
            }
            else
                dest_data [min_count] = filter.applyFilter(src_data, start_index);
        }
    }
    
    private String formatTime (int sec_of_day)
    {
        int min_of_day = sec_of_day / 60;
        return GeoString.pad(Integer.toString (min_of_day / 60), 2, true, '0') + ":" +
               GeoString.pad(Integer.toString (min_of_day % 60), 2, true, '0') + ":" +
               GeoString.pad(Integer.toString (sec_of_day % 60), 2, true, '0');
    }
    
    private String formatGapException (boolean add_to_list, int start_sec_of_day, int end_sec_of_day, boolean gap_in_iaga)
    {
        String gap_info;
        if (gap_in_iaga)
            gap_info = "gap in 1-sec but not in 1-min data";
        else
            gap_info = "gap in 1-min but not in 1-sec data";
        String string = "";
        if (add_to_list) string += ", ";
        if (end_sec_of_day == start_sec_of_day)
            string += formatTime (start_sec_of_day) + " (" + gap_info + ")";
        else
            string += formatTime (start_sec_of_day) + " to " + formatTime (end_sec_of_day) +
                      " (" + gap_info + ")";
        return string;
    }
    
    private String formatDiffException (boolean add_to_list, int start_sec_of_day, int end_sec_of_day, double max_diff, String units)
    {
        String string = "";
        if (add_to_list) string += ", ";
        if (end_sec_of_day == start_sec_of_day)
            string += formatTime (start_sec_of_day) + " (diff " + String.format ("%.1f", max_diff) + units + ")";
        else
            string += formatTime (start_sec_of_day) + " to " + formatTime (end_sec_of_day) +
                      " (max diff " + String.format ("%.1f", max_diff) + units + ")";
        return string;
    }
}
