/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bgs.DataCheck1s;

import bgs.geophys.library.Data.GeomagAbsoluteValue;
import bgs.geophys.library.Data.GeomagDataFormat;
import bgs.geophys.library.Data.ImagCDF.ImagCDF;
import bgs.geophys.library.Misc.DateUtils;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * this class holds data that is being amalgamated before being written to ImagCDF 
 *
 * @author smf
 */
public class PrepareImagCDFData 
{
    
    private final String MISSING_STRING = "Unknown";
    private final int N_ELEMENTS = 4;
    
    private String iaga_code;
    private String station_name;
    private int orientation;
    private int f_scalar_type;
    private double latitude, longitude, elevation;
    private String institute_name;
    private String vector_sens_orient;
    private String source, unique_identifier, parent_identifiers [];
    private URL reference_links [];
    private int year;
    private int month;
    private double data [] [];
    private double samp_per;
    private int samps_per_day;
    private boolean element_codes_set, f_scalar_code_set;
    private int element_codes [];
    
    /** construct a data array to hold one month of 1-second data - create an empty data array
     * @param iaga_code the code for this station
     * @param year the year for the data
     * @param month the month for the data (0=Jan)
     * @param orientation one of the GeomagAbsoluteValue.ORIENTATION_ codes to specify the orientation
     *        of the data to be written out - ORIENTATION_UNKNOW means use the orientation from the
     *        first input file found
     * @param f_scalar_type choose from: GeomagAbsoluteValue.COMPONENT_F_SCALAR,
     *                      GeomagAbsoluteValue.COMPONENT_F_DIFF, GeomagAbsoluteValue.COMPONENT_F_UNRECORDED,
     *                      GeomagAbsoluteValue.COMPONENT_UNKNOWN (to use what is in the first input file found) */
    public PrepareImagCDFData (String iaga_code, int year, int month, int orientation, int f_scalar_type)
    {
        int sample_count, element_count, n_samples;

        element_codes = new int [N_ELEMENTS];
        
        this.iaga_code = iaga_code;
        this.station_name = this.institute_name = MISSING_STRING;
        this.source = "INTERMAGNET";
        this.latitude = this.longitude = this.elevation = ImagCDF.MISSING_DATA_VALUE;
        this.vector_sens_orient = this.unique_identifier = null;
        this.parent_identifiers = new String [0];
        this.year = year;
        this.month = month;
        this.orientation = orientation;
        this.f_scalar_type = f_scalar_type;
        setElementCodes(orientation);
        setFScalarCode(f_scalar_type);
        
        try
        {
            reference_links = new URL [] {new URL ("http://www.intermagnet.org/")}; 
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException ("Fault with INTERMAGNET URL", e);
        }

        samps_per_day = 86400;
        samp_per = 1.0;
        n_samples = DateUtils.daysInMonth(month, year) * samps_per_day;
        data = new double [N_ELEMENTS] [n_samples];
        for (sample_count=0; sample_count<n_samples; sample_count++) 
        {
            for (element_count=0; element_count<N_ELEMENTS; element_count++)
                data [element_count] [sample_count] = ImagCDF.MISSING_DATA_VALUE;
        }
    }
    
    public void harvestMetadata (GeomagDataFormat data)
    {
        this.station_name = validateString (this.station_name, data.getStationName());
        this.latitude = validateNumber (this.latitude, -90.0, 90.0, data.getLatitude());
        this.longitude = validateNumber (this.longitude, -360.0, 360.0, data.getLongitude());
        this.elevation = validateNumber (this.elevation, -1000.0, 10000.0, data.getElevation());
        this.institute_name = validateString (this.institute_name, data.getInstituteName());
        this.vector_sens_orient = validateString (this.vector_sens_orient, data.getSensorOrientation());
        this.source = validateString (this.source, data.getInstituteName());
    }
    
    private String validateString (String original, String updated)
    {
        if (updated != null)
        {
            if (updated.length() > 0)
            {
                if (! updated.equalsIgnoreCase("unknown"));
                    return updated;
            }
        }
        return original;
    }
    private double validateNumber (double original, double min, double max, double updated)
    {
        if (updated >= min && updated <= max) return updated;
        return original;
    }
    
    public void addSample (GeomagAbsoluteValue sample, int index)
    {
        int element_count;
        double value;
        
        // adjust index for this type of object - silently ignore if the data can't be inserted
        if (index < 0) return;
        if (index >= data[0].length) return;
        
        if (! element_codes_set) 
        {
            setElementCodes (sample.getNativeOrientation());
            if (element_codes_set) orientation = sample.getNativeOrientation();
        }
        if (element_codes_set)
        {
            for (element_count=0; element_count<3; element_count++)
            {
                value = sample.getComponent(element_codes[element_count], GeomagAbsoluteValue.ANGLE_DEGREES);
                if (value != sample.getMissingComponentValue() && value != sample.getMissingDataValue())
                    data [element_count] [index] = value;
            }
        }
        if (! f_scalar_code_set) setFScalarCode (sample.getFScalarType ());
        if (f_scalar_code_set)
        {
            if (element_codes[3] == GeomagAbsoluteValue.COMPONENT_F_DIFF || element_codes[3] == GeomagAbsoluteValue.COMPONENT_F_SCALAR)
            {
                value = sample.getComponent(element_codes[3], GeomagAbsoluteValue.ANGLE_DEGREES);
                if (value != sample.getMissingComponentValue() && value != sample.getMissingDataValue())
                    data [3] [index] = value;
            }
        }
    }
    
    public String getIagaCode () { return iaga_code; }
    public int getOrientation () { return orientation; }
    public int getFScalarType () { return f_scalar_type; }
    public String getStationName () { return station_name; }
    public double getLatitude () { return latitude; }
    public double getLongitude () { return longitude; }
    public double getElevation () { return elevation; }
    public String getInstituteName () { return institute_name; }
    public String getVectorSensorOrientation () { return vector_sens_orient; }
    public String getSource () { return source; }
    public String getUniqueIdentifier () { return unique_identifier; }
    public String [] getParentIdentifiers () { return parent_identifiers; }
    public URL [] getReferenceLinks () { return reference_links; }
    public int getYear () { return year; }
    public int getMonth () { return month; }
    public int getNElements () { return N_ELEMENTS; }
    public int getElementCode (int index) 
    {
        if (index < 0 || index >= N_ELEMENTS) return GeomagAbsoluteValue.COMPONENT_UNKNOWN;
        if (index <= 2)
        {
            if (! element_codes_set) return GeomagAbsoluteValue.COMPONENT_UNKNOWN;
        }
        else if (! f_scalar_code_set) return GeomagAbsoluteValue.COMPONENT_UNKNOWN;
        return element_codes[index]; 
    }
    public double getSamplePeriod () { return samp_per; }
    public int getSampsPerDay () { return samps_per_day; }
    public double [] [] getData () { return data; }
    
    private void setElementCodes (int orientation)
    {
        switch (orientation)
        {
            case GeomagAbsoluteValue.ORIENTATION_XYZ:
                element_codes [0] = GeomagAbsoluteValue.COMPONENT_X;
                element_codes [1] = GeomagAbsoluteValue.COMPONENT_Y;
                element_codes [2] = GeomagAbsoluteValue.COMPONENT_Z;
                element_codes_set = true;
                break;
            case GeomagAbsoluteValue.ORIENTATION_HDZ:
                element_codes [0] = GeomagAbsoluteValue.COMPONENT_H;
                element_codes [1] = GeomagAbsoluteValue.COMPONENT_D;
                element_codes [2] = GeomagAbsoluteValue.COMPONENT_Z;
                element_codes_set = true;
                break;
            case GeomagAbsoluteValue.ORIENTATION_DIF:
                element_codes [0] = GeomagAbsoluteValue.COMPONENT_D;
                element_codes [1] = GeomagAbsoluteValue.COMPONENT_I;
                element_codes [2] = GeomagAbsoluteValue.COMPONENT_F;
                element_codes_set = true;
                break;
            default:
                element_codes_set = false;
                break;
        }
    }
    
    private void setFScalarCode (int f_scalar_type)
    {
        switch (f_scalar_type)
        {
            case GeomagAbsoluteValue.COMPONENT_F_SCALAR:
            case GeomagAbsoluteValue.COMPONENT_F_DIFF:
            case GeomagAbsoluteValue.COMPONENT_F_UNRECORDED:
                element_codes [3] = f_scalar_type;
                f_scalar_code_set = true;
            default:
                f_scalar_code_set = false;
                break;
        }
    }
    
}
