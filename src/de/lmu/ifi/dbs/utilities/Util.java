package de.lmu.ifi.dbs.utilities;

import clusterConstraints.PlotConstraints;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * @version 0.1
 */
public final class Util
{
    /**
     * Returns the maximum of the given Distances or the first, if none is
     * greater than the other one.
     * 
     * @param d1
     *            first Distance
     * @param d2
     *            second Distance
     * @return Distance the maximum of the given Distances or the first, if
     *         neither is greater than the other one
     */
    public static Distance max(Distance d1, Distance d2)
    {
        if(d1.compareTo(d2) > 0)
        {
            return d1;
        }
        else if(d2.compareTo(d1) > 0)
        {
            return d2;
        }
        else
        {
            return d1;
        }
    }

    /**
     * Formats the double d with 2 fraction digits.
     * 
     * @param d
     *            the double to be formatted
     * @return a String representing the double d
     */
    public static String format(final double d)
    {
        return format(d, 2);
    }

    /**
     * Formats the double d with the specified fraction digits.
     * 
     * @param d
     *            the double array to be formatted
     * @param digits
     *            the number of fraction digits
     * @return a String representing the double d
     */
    public static String format(final double d, int digits)
    {
        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(digits);
        nf.setGroupingUsed(false);
        return nf.format(d);
    }

    /**
     * Formats the double d with the specified number format.
     * 
     * @param d
     *            the double array to be formatted
     * @param nf
     *            the number format to be used for formatting
     * @return a String representing the double d
     */
    public static String format(final double d, NumberFormat nf)
    {
        return nf.format(d);
    }

    /**
     * Formats the double array d with ',' as separator and 2 fraction digits.
     * 
     * @param d
     *            the double array to be formatted
     * @return a String representing the double array d
     */
    public static String format(double[] d)
    {
        return format(d, ", ", 2);
    }

    /**
     * Formats the double array d with the specified separator and the specified
     * fraction digits.
     * 
     * @param d
     *            the double array to be formatted
     * @param sep
     *            the seperator between the single values of the double array,
     *            e.g. ','
     * @param digits
     *            the number of fraction digits
     * @return a String representing the double array d
     */
    public static String format(double[] d, String sep, int digits)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < d.length; i++)
        {
            if(i < d.length - 1)
                buffer.append(format(d[i], digits)).append(sep);
            else
                buffer.append(format(d[i], digits));
        }
        return buffer.toString();
    }

    /**
     * Formats the double array d with the specified number format.
     * 
     * @param d
     *            the double array to be formatted
     * @param nf
     *            the number format to be used for formatting
     * @return a String representing the double array d
     */
    public static String format(double[] d, NumberFormat nf)
    {
        return format(d, " ", nf);
    }

    /**
     * Formats the double array d with the specified number format.
     * 
     * @param d
     *            the double array to be formatted
     * @param sep
     *            the seperator between the single values of the double array,
     *            e.g. ','
     * @param nf
     *            the number format to be used for formatting
     * @return a String representing the double array d
     */
    public static String format(double[] d, String sep, NumberFormat nf)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < d.length; i++)
        {
            if(i < d.length - 1)
                buffer.append(format(d[i], nf)).append(sep);
            else
                buffer.append(format(d[i], nf));
        }
        return buffer.toString();
    }

    /**
     * Returns the prefix of the specidfied fileName (i.e. the name of the file
     * without extension).
     * 
     * @param fileName
     *            the name of the file
     * @return the prefix of the specidfied fileName
     */
    public static String getFilePrefix(final String fileName)
    {
        final int index = fileName.lastIndexOf('.');
        if(index < 0)
            return fileName;
        return fileName.substring(0, index);
    }

    /**
     * Returns a new String array containing the same objects as are contained
     * in the given array.
     * 
     * @param array
     *            an array to copy
     * @return the copied array
     */
    public static String[] copy(String[] array)
    {
        String[] copy = new String[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    /**
     * Returns a new double array containng the same objects as are contained in
     * the given array.
     * 
     * @param array
     *            an array to copy
     * @return the copied array
     */
    public static double[] copy(double[] array)
    {
        double[] copy = new double[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    /**
     * Returns the unboxed double array of the given Object Double array.
     * 
     * @param array
     *            the array to be unboxed
     * @return the unboxed double array
     */
    public static double[] unbox(Double[] array)
    {
        double[] unboxed = new double[array.length];
        // noinspection ManualArrayCopy
        for(int i = 0; i < unboxed.length; i++)
        {
            unboxed[i] = array[i];
        }
        return unboxed;
    }

    /**
     * Returns the unboxed double array of the given Object Number array.
     * 
     * @param array
     *            the array to be unboxed
     * @return the unboxed double array
     */
    public static double[] unbox(Number[] array)
    {
        double[] unboxed = new double[array.length];
        for(int i = 0; i < unboxed.length; i++)
        {
            unboxed[i] = array[i].doubleValue();
        }
        return unboxed;
    }

    /**
     * Returns the centroid as a DoubleVector object of the specified objects
     * stored in the given database. The objects belonging to the specified ids
     * must be instance of <code>DoubleVector</code>.
     * 
     * @param database
     *            the database storing the objects
     * @param ids
     *            the ids of the objects
     * @return the centroid of the specified objects stored in the given
     *         database
     */
    public static DoubleVector centroid(Database<DoubleVector> database, List<Integer> ids)
    {
        int dim = database.dimensionality();
        double[] centroid = new double[dim];

        for(int id : ids)
        {
            DoubleVector o = database.get(id);
            for(int j = 1; j <= dim; j++)
            {
                centroid[j - 1] += o.getValue(j);
            }
        }

        for(int i = 0; i < dim; i++)
        {
            centroid[i] /= ids.size();
        }
        return new DoubleVector(centroid);
    }

    /**
     * Returns the centroid as a DoubleVector object of the specified database.
     * The objects must be instance of <code>DoubleVector</code>.
     * 
     * @param database
     *            the database storing the objects
     * @return the centroid of the specified objects stored in the given
     *         database
     */
    public static DoubleVector centroid(Database<DoubleVector> database)
    {
        int dim = database.dimensionality();
        double[] centroid = new double[dim];

        Iterator<Integer> it = database.iterator();
        while(it.hasNext())
        {
            DoubleVector o = database.get(it.next());
            for(int j = 1; j <= dim; j++)
            {
                centroid[j - 1] += o.getValue(j);
            }
        }

        for(int i = 0; i < dim; i++)
        {
            centroid[i] /= database.size();
        }
        return new DoubleVector(centroid);
    }

    /**
     * Determines the covarianvce matrix of the objects stored in the given
     * database.
     * 
     * @param database
     *            the database storing the objects
     * @param ids
     *            the ids of the objects
     * @return the covarianvce matrix of the specified objects
     */
    public static Matrix covarianceMatrix(Database<DoubleVector> database, List<Integer> ids)
    {
        // centroid
        DoubleVector centroid = centroid(database, ids);

        // covariance matrixArray
        int columns = centroid.getDimensionality();
        int rows = ids.size();

        double[][] matrixArray = new double[rows][columns];

        for(int i = 0; i < rows; i++)
        {
            DoubleVector obj = database.get(ids.get(i));
            for(int d = 0; d < columns; d++)
            {
                matrixArray[i][d] = obj.getValue(d + 1) - centroid.getValue(d + 1);
            }
        }
        Matrix centeredMatrix = new Matrix(matrixArray);
        return centeredMatrix.transpose().times(centeredMatrix);
    }

    /**
     * Determines the covarianvce matrix of the objects stored in the given
     * database.
     * 
     * @param database
     *            the database storing the objects
     * @return the covarianvce matrix of the specified objects
     */
    public static Matrix covarianceMatrix(Database<DoubleVector> database)
    {
        // centroid
        DoubleVector centroid = centroid(database);

        // covariance matrixArray
        int columns = centroid.getDimensionality();
        int rows = database.size();
        double[][] matrixArray = new double[rows][columns];

        Iterator<Integer> it = database.iterator();
        int i = 0;
        while(it.hasNext())
        {
            DoubleVector obj = database.get(it.next());
            for(int d = 0; d < columns; d++)
            {
                matrixArray[i][d] = obj.getValue(d + 1) - centroid.getValue(d + 1);
            }
            i++;
        }
        Matrix centeredMatrix = new Matrix(matrixArray);
        return centeredMatrix.transpose().times(centeredMatrix);
    }

    /**
     * Returns a new <code>Double</code> array initialized to the values
     * represented by the specified <code>String</code> and separated by
     * comma, as performed by the <code>valueOf</code> method of class
     * <code>Double</code>.
     * 
     * @param s
     *            the string to be parsed.
     * @return a new <code>Double</code> array represented by s
     */
    public static double[] parseDoubles(String s)
    {
        List<Double> result = new ArrayList<Double>();
        StringTokenizer tokenizer = new StringTokenizer(s, ",");
        while(tokenizer.hasMoreTokens())
        {
            String d = tokenizer.nextToken();
            result.add(Double.parseDouble(d));
        }
        return unbox(result.toArray(new Double[result.size()]));
    }

    /**
     * Provides a script-text for a gnuplot script
     * to use for transposed view of a specific file
     * of given size of data set.
     * 
     * 
     * @param filename the filename of the file to be plotted
     * @param datasetSize the size of the data set
     * @return a script-text for a gnuplot script
     * to use for transposed view of a specific file
     * of given size of data set
     */
    public static String transposedGnuplotScript(String filename, int datasetSize)
    {
        StringBuffer script = new StringBuffer();
        //script.append("set terminal pbm color;\n");
        script.append("set nokey\n");
        script.append("set data style linespoints\n");
        script.append("set xlabel \"attribute\"\n");
        script.append("show xlabel\n");
        script.append("plot ");
        for(int p = 1; p <= datasetSize; p++)
        {
            script.append("\""+filename+"\" using "+p);
            if(p < datasetSize)
            {
                script.append(", ");
            }
        }
        script.append('\n');
        return script.toString();
    }
    
}