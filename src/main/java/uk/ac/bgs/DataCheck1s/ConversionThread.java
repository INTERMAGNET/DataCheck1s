/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Data.GeomagAbsoluteValue;
import bgs.geophys.library.Data.GeomagDataFilename;
import bgs.geophys.library.Data.GeomagDataFormat;
import bgs.geophys.library.Data.Iaga2002;
import bgs.geophys.library.Data.Iaga2002Filename;
import bgs.geophys.library.Data.ImagCDF.IMCDFPublicationLevel;
import bgs.geophys.library.Data.ImagCDF.IMCDFStandardLevel;
import bgs.geophys.library.Data.ImagCDF.IMCDFVariableType;
import bgs.geophys.library.Data.ImagCDF.ImagCDF;
import bgs.geophys.library.Data.ImagCDF.ImagCDFFilename;
import bgs.geophys.library.Data.ImagCDF.ImagCDFLowLevel;
import bgs.geophys.library.Data.ImagCDF.ImagCDFVariable;
import bgs.geophys.library.Data.ImagCDF.ImagCDFVariableTS;
import bgs.geophys.library.File.ClasspathFileFactory;
import bgs.geophys.library.File.ClasspathFileInterface;
import bgs.geophys.library.Misc.DateUtils;
import gsfc.nssdc.cdf.CDFException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author smf
 */
public class ConversionThread extends Thread
{

    private String obs;
    private int year;
    private int orientation;
    private File out_folder;
    private ClasspathFileFactory classpath_file_factory;
    private boolean cancelled;
    private ConversionDisplayInterface conversion_display;
    private File month_files_created [];
    private boolean compress;
    
    public ConversionThread (String obs, int year, int orientation, File folder, ConversionDisplayInterface conversion_display, boolean compress)
    {
        this.obs = obs;
        this.year = year;
        this.orientation = orientation;
        this.out_folder = folder;
        this.classpath_file_factory = new ClasspathFileFactory (folder);
        this.cancelled = false;
        this.conversion_display = conversion_display;
        this.compress = compress;
        this.month_files_created = new File [12];
        for (int count=0; count<month_files_created.length; count ++)
            month_files_created [count] = null;
    }
    
    public ConversionThread (String obs, int year, int orientation, Class base_reference, String relative_reference, File out_folder, ConversionDisplayInterface conversion_display, boolean compress)
    {
        this.obs = obs;
        this.year = year;
        this.orientation = orientation;
        this.out_folder = out_folder;
        this.classpath_file_factory = new ClasspathFileFactory (base_reference, relative_reference);
        this.cancelled = false;
        this.conversion_display = conversion_display;
        this.compress = compress;
        this.month_files_created = new File [12];
        for (int count=0; count<month_files_created.length; count ++)
            month_files_created [count] = null;
    }
    
    public void cancel ()
    {
        this.cancelled = true;
        conversion_display.addConversionErrorMsg("Conversion cancelled");
    }
    
    public File [] getMonthFilesCreated ()
    {
        File month_files_created_copy [] = new File [12];
        synchronized (month_files_created)
        {
            for (int count=0; count<12; count++)
                month_files_created_copy [count] = month_files_created [count];
        }
        return month_files_created_copy;
    }
    
    @Override
    public void run() 
    {
        
        conversion_display.setProgress (0);
        
        // check that the files we want are in the folder or base URL
        GregorianCalendar cal = new GregorianCalendar (DateUtils.gmtTimeZone);
        cal.set (year, 0, 1, 0, 0, 0);
        cal.set (GregorianCalendar.MILLISECOND, 0);
        int n_iaga_files_found = 0;
        int n_possible_iaga_files = DateUtils.daysInYear(year);
        ClasspathFileInterface iaga_files [] = new ClasspathFileInterface [n_possible_iaga_files];
        for (int day_count=0; (day_count<n_possible_iaga_files) && (! cancelled); day_count++, cal.add (GregorianCalendar.DAY_OF_MONTH, 1))
        {
            // find IAGA-2002 files
            Iaga2002Filename iaga_2002_filename = new Iaga2002Filename (obs, cal.getTime(), GeomagDataFilename.QualityType.DEFINITIVE, GeomagDataFilename.Interval.SECOND, false, GeomagDataFormat.DurationType.DAY, GeomagDataFilename.Case.LOWER);
            iaga_files [day_count] = classpath_file_factory.getClasspathFile (iaga_2002_filename.getFilename());
            if (iaga_files[day_count].exists()) n_iaga_files_found ++;
            else
            {
                iaga_2002_filename = new Iaga2002Filename (obs, cal.getTime(), GeomagDataFilename.QualityType.DEFINITIVE, GeomagDataFilename.Interval.SECOND, false, GeomagDataFormat.DurationType.DAY, GeomagDataFilename.Case.UPPER);
                iaga_files [day_count] = classpath_file_factory.getClasspathFile (iaga_2002_filename.getFilename());
                if (iaga_files[day_count].exists()) n_iaga_files_found ++;
                else iaga_files[day_count] = null;
            }
        }
        if (n_iaga_files_found <= 0)
        {
            conversion_display.addConversionErrorMsg ("No 1-second IAGA 2002 data files found: can't do data conversion");
            conversion_display.setProgress (100);
            return;
        }
        if (n_iaga_files_found < n_possible_iaga_files)
            conversion_display.addConversionErrorMsg ("Not all second data available: " + 
                                                     Integer.toString (n_iaga_files_found) + " of " +
                                                     Integer.toString (n_possible_iaga_files) +
                                                     " IAGA 2002 files found."); 
        
        // when you get here:
        //   iaga_files / iaga_urls contains [NDaysInyear] entries, 1 per day file of second data
        // load the data into the 1-second data array
        SimpleDateFormat date_format = new SimpleDateFormat ("yyyy-MM-dd");
        date_format.setTimeZone(DateUtils.gmtTimeZone);
        cal.set (year, 0, 1, 0, 0, 0);
        PrepareImagCDFData second_data = null;
        for (int day_count=0; (day_count<DateUtils.daysInYear(year)) && (! cancelled);
             day_count ++, cal.add (GregorianCalendar.DAY_OF_MONTH, 1))
        {
            // start a new month??
            int day_of_month = cal.get (GregorianCalendar.DAY_OF_MONTH);
            int day_of_year = cal.get (GregorianCalendar.DAY_OF_YEAR);
            int month = cal.get (GregorianCalendar.MONTH);
            int days_in_month = DateUtils.daysInMonth(month, year);
            if (day_of_month == 1)
            {
                // create an array of second data for the year
                second_data = new PrepareImagCDFData (obs, year, month,
                                                      orientation, GeomagAbsoluteValue.COMPONENT_UNKNOWN);
                
                conversion_display.setProgress((month * 100) / (12));
            }
            
            // load the second data for this day
            Iaga2002 today_iaga_2002;
            today_iaga_2002 = loadIaga2002 (iaga_files[day_count]);

            // copy the data into the month-long array
            if (today_iaga_2002 != null)
            {
                String errmsg = loadIntoDataArray (iaga_files[day_count].getName(), today_iaga_2002, second_data);
                if (errmsg != null) conversion_display.addConversionErrorMsg(errmsg);                
            }
            
            // end of a month ??
            if (day_of_month == days_in_month)
            {
                conversion_display.setProgress((month * 100) / (12) + (100 / 24));
                
                // is there data for this month??
                int end_day = day_of_year + 1;
                int start_day = end_day - days_in_month;
                boolean write_month = false;
                for (int count=start_day; (count<end_day) && (! write_month); count++)
                {
                    if (iaga_files[count -1] != null) write_month = true;
                }

                // write this month's data
                if (write_month)
                {
                    String errmsg = writeImagCDF (second_data);
                    if (errmsg != null) conversion_display.addConversionErrorMsg(errmsg);
                }
            }
            
        }
                   
        conversion_display.setProgress (100);
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
                    conversion_display.addConversionErrorMsg("Iaga2002 file must contain 1-second data: " + file.getName());
                    iaga2002 = null;
                }
                if (iaga2002.getDataLength() != 86400)
                {
                    conversion_display.addConversionErrorMsg("Iaga2002 file must contain exactly one day of data: " + file.getName());
                    iaga2002 = null;
                }
            }
            catch (IOException ex) 
            { 
                String msg;
                msg = "Iaga 2002: Unable to load file [" + file.getName() + "]";
                if (ex.getMessage() != null) msg += ": " + ex.getMessage();
                conversion_display.addConversionErrorMsg(msg);
                iaga2002 = null; 
            }
        }
        
        return iaga2002;
    }
    
   private String loadIntoDataArray (String iaga_file_name, Iaga2002 iaga_data, PrepareImagCDFData prepared_data)
    {
        int n_samples, sample_count, memory_data_start_sample, file_data_start_sample;
        int n_samples_in_file, n_samples_in_memory;
        int orientation, f_scalar_type;
        long file_data_time, memory_data_time, milli_to_sample;
        GeomagAbsoluteValue abs_val;
        GregorianCalendar cal;
        String err_msg;
        
        err_msg = null;
        
        // load the data
        n_samples_in_file = iaga_data.getDataLength();
        
        
        // find the offset to the start of the data in the data array
        switch (iaga_data.getIntervalTypeEnum())
        {
            case SECOND: milli_to_sample = 1000l; n_samples_in_memory = 86400 * DateUtils.daysInMonth(prepared_data.getMonth(), prepared_data.getYear()); break;
            default: throw new IllegalArgumentException ("interval type must be second");
        }
        cal = new GregorianCalendar (DateUtils.gmtTimeZone);
        file_data_time = iaga_data.getStartDate().getTime();
        cal.set(prepared_data.getYear(), prepared_data.getMonth(), 1, 0, 0, 0);
        cal.set(GregorianCalendar.MILLISECOND, 0);
        memory_data_time = cal.getTimeInMillis();
        if (file_data_time < memory_data_time)
        {
            memory_data_start_sample = 0;
            file_data_start_sample = (int) ((memory_data_time - file_data_time) / milli_to_sample);
        }
        else
        {
            memory_data_start_sample = (int) ((file_data_time - memory_data_time) / milli_to_sample);
            file_data_start_sample = 0;
        }
        file_data_time = iaga_data.getStartDate().getTime() + iaga_data.getDuration();
        cal.set(prepared_data.getYear() +1, 0, 1, 0, 0, 0);
        memory_data_time = cal.getTimeInMillis();
        if (file_data_time < memory_data_time)
            n_samples = n_samples_in_file - file_data_start_sample;
        else
            n_samples = n_samples_in_memory - memory_data_start_sample;

        // copy the data from the file into the data array
        prepared_data.harvestMetadata(iaga_data);
        orientation = findOrientation (iaga_data.getComponentCodes());
        if (orientation == GeomagAbsoluteValue.ORIENTATION_UNKNOWN)
            return "Unrecognised orientation code " + iaga_data.getComponentCodes() + ": " + iaga_file_name;
        f_scalar_type = findFScalarType (iaga_data.getComponentCodes());
        for (sample_count=0; sample_count<n_samples; sample_count ++)
        {
            abs_val = new GeomagAbsoluteValue (iaga_data.getData (0, sample_count + file_data_start_sample),
                                               iaga_data.getData (1, sample_count + file_data_start_sample),
                                               iaga_data.getData (2, sample_count + file_data_start_sample),
                                               iaga_data.getData (3, sample_count + file_data_start_sample),
                                               f_scalar_type, GeomagDataFormat.MISSING_DATA_SAMPLE, GeomagDataFormat.MISSING_COMPONENT,
                                               orientation, GeomagAbsoluteValue.ANGLE_MINUTES);
            prepared_data.addSample (abs_val, sample_count + memory_data_start_sample);
        }
        
        return err_msg;
    }

    /** write prepared data to ImagCDF
     * @param data the data to write
     * @return an error string or null if there were no errors */
    private String writeImagCDF (PrepareImagCDFData data)
    {
        int count, element_code, n_elements, n_samples;
        File file;
        String element_name;
        ImagCDF imag_cdf;
        IMCDFPublicationLevel.PublicationLevel pub_level;
        ImagCDFVariable elements [];
        ImagCDFVariableTS vector_time_stamps, scalar_time_stamps;
        ImagCDFFilename cdf_filename;
        GregorianCalendar data_start_cal;

        // work out time and index offsets for this month
        data_start_cal = new GregorianCalendar (DateUtils.gmtTimeZone);
        n_samples = DateUtils.daysInMonth (data.getMonth(), data.getYear()) * data.getSampsPerDay();
        data_start_cal.set (data.getYear(), data.getMonth(), 1, 0, 0, 0);
        data_start_cal.set (GregorianCalendar.MILLISECOND, 0);
        
        // work out how many elements to create
        n_elements = data.getNElements();
        if (n_elements < 3) return "Not enough geomagnetic elements in data (must be at least 3)";
        if (n_elements > 4) n_elements = 4;
        if (data.getElementCode(0) == GeomagAbsoluteValue.COMPONENT_UNKNOWN) return "Missing essential geomagnetic element (1)";
        if (data.getElementCode(1) == GeomagAbsoluteValue.COMPONENT_UNKNOWN) return "Missing essential geomagnetic element (2)";
        if (data.getElementCode(2) == GeomagAbsoluteValue.COMPONENT_UNKNOWN) return "Missing essential geomagnetic element (3)";
        if (n_elements > 3)
        {
            switch (data.getElementCode(3))
            {
                case GeomagAbsoluteValue.COMPONENT_F_DIFF:
                case GeomagAbsoluteValue.COMPONENT_F_SCALAR:
                    break;
                default:
                    // don't record a 4th element that isn't an independent F value
                    n_elements = 3;
                    break;
            }
        }
        
        // prepare the individual geomagnetic elements
        elements = new ImagCDFVariable [n_elements];
        for (count=0; count<n_elements; count++)
        {
            element_code = data.getElementCode(count);
            element_name = GeomagAbsoluteValue.getComponentName(element_code);
            try
            {
                if (element_code == GeomagAbsoluteValue.COMPONENT_UNKNOWN) return "Missing essential geomagnetic element (" + count+1 + ")";
                String timestamp_name = "GeomagneticVectorTimes";
                if (count >= 3) timestamp_name="GeomagneticScalarTimes";
                elements [count] = new ImagCDFVariable (new IMCDFVariableType (IMCDFVariableType.VariableTypeCode.GeomagneticFieldElement),
                                                        "Geomagnetic Field Element " + element_name, 
                                                        ImagCDFLowLevel.getValidMaxMin(element_name, false),
                                                        ImagCDFLowLevel.getValidMaxMin(element_name, true),
                                                        ImagCDFLowLevel.getUnits(element_name),
                                                        ImagCDF.MISSING_DATA_VALUE, timestamp_name, element_name, data.getData()[count], 0, n_samples);
            }
            catch (CDFException e)
            {
                if (e.getMessage() != null) return "Error creating CDF variable " + count+1 + ": " + e.getMessage();
                return "Error creating CDF variable " + count+1;
            }
        }

        ImagCDFVariableTS time_stamps[];
        try
        {
            if (elements.length <= 3)
                time_stamps = new ImagCDFVariableTS[1];
            else
            {
                time_stamps = new ImagCDFVariableTS [2];
                time_stamps [1] = new ImagCDFVariableTS (data_start_cal.getTime(), data.getSamplePeriod(), n_samples, "GeomagneticScalarTimes");
            }
            time_stamps [0] = new ImagCDFVariableTS (data_start_cal.getTime(), data.getSamplePeriod(), n_samples, "GeomagneticVectorTimes");
        }
        catch (CDFException e)
        {
            if (e.getMessage() != null) return "Error creating CDF time data: " + e.getMessage();
            return "Error creating CDF time data ";
        }
        
        pub_level = IMCDFPublicationLevel.PublicationLevel.LEVEL_4;
        try
        {
            imag_cdf = new ImagCDF (data.getIagaCode(), new IMCDFPublicationLevel (pub_level), new Date (), data.getStationName(), 
                                    data.getLatitude(), data.getLongitude(), data.getElevation(), data.getInstituteName(),
                                    data.getVectorSensorOrientation(), new IMCDFStandardLevel (IMCDFStandardLevel.StandardLevel.NONE), null, null, null,
                                    data.getSource(), data.getUniqueIdentifier(), data.getParentIdentifiers(), data.getReferenceLinks(),
                                    elements, null, time_stamps);
            cdf_filename = new ImagCDFFilename (imag_cdf, ImagCDFFilename.Case.LOWER);
            file = new File (out_folder, cdf_filename.getFilename());
            imag_cdf.write (file, compress, true);
            synchronized (month_files_created)
            {
                month_files_created[data.getMonth()] = file;
            }
        }
        catch (CDFException | ParseException e)
        {
            if (e.getMessage() != null) return "Error creating CDF file: " + e.getMessage();
            return "Error creating CDF file";
        }

        return null;
    }
    
    private int findOrientation (String elements)
    {
        elements = elements.toUpperCase().trim();
        if (elements.startsWith ("XYZ")) return GeomagAbsoluteValue.ORIENTATION_XYZ;
        if (elements.startsWith ("HDZ")) return GeomagAbsoluteValue.ORIENTATION_HDZ;
        if (elements.startsWith ("DIF")) return GeomagAbsoluteValue.ORIENTATION_DIF;
        return GeomagAbsoluteValue.ORIENTATION_UNKNOWN;
    }
    
    private int findFScalarType (String elements)
    {
        if (elements.length() < 4) return GeomagAbsoluteValue.COMPONENT_F_UNRECORDED;
        elements = elements.toUpperCase();
        if (elements.endsWith("F")) return GeomagAbsoluteValue.COMPONENT_F;
        if (elements.endsWith("G")) return GeomagAbsoluteValue.COMPONENT_F_DIFF;
        if (elements.endsWith("S")) return GeomagAbsoluteValue.COMPONENT_F_SCALAR;
        return GeomagAbsoluteValue.COMPONENT_UNKNOWN;
    }
}
