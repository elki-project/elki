package de.lmu.ifi.dbs.utilities;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @version
 */
public final class Util
{

    /**
     * Formats the double d with 2 fraction digits.
     * 
     * @param d
     *            the double to be formatted
     * @return a String representing the double d
     */
    public static String format(final double d)
    {
        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        nf.setGroupingUsed(false);
        String format = nf.format(d);
        return format;
    }

    public static String format(final double d, int digits)
    {
        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(digits);
        nf.setGroupingUsed(false);
        String format = nf.format(d);
        return format;
    }

    public static String getFilePrefix(final String fileName)
    {
        final int index = fileName.lastIndexOf('.');
        if(index < 0)
            return fileName;
        return fileName.substring(0, index);
    }

    public static String format(double[] v, String sep)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < v.length; i++)
        {
            if(i < v.length - 1)
                buffer.append(format(v[i]) + sep);
            else
                buffer.append(format(v[i]));
        }
        return buffer.toString();
    }

    public static String format(double[] v, String sep, int digits)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < v.length; i++)
        {
            if(i < v.length - 1)
                buffer.append(format(v[i], digits) + sep);
            else
                buffer.append(format(v[i], digits));
        }
        return buffer.toString();
    }
}