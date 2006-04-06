package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;

import sun.misc.Launcher;

/**
 * @version 0.1
 */
public final class Util
{
    /**
     * Holds the class specific debug status.
     */
    private static final boolean DEBUG = LoggingConfiguration.DEBUG;
    
    /**
     * The logger of this class.
     */
    private static Logger logger = Logger.getLogger(Util.class.getName());
    static
    {
        LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
    }

    
    /**
     * Returns the maximum of the given Distances or the first, if none is
     * greater than the other one.
     *
     * @param d1 first Distance
     * @param d2 second Distance
     * @return Distance the maximum of the given Distances or the first, if
     *         neither is greater than the other one
     */
    public static <D extends Distance<D>> D max(D d1, D d2)
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
     * Returns the minimum of the given Distances or the first, if none is less
     * than the other one.
     *
     * @param d1 first Distance
     * @param d2 second Distance
     * @return Distance the minimum of the given Distances or the first, if
     *         neither is less than the other one
     */
    public static <D extends Distance<D>> D min(D d1, D d2)
    {
        if(d1.compareTo(d2) < 0)
        {
            return d1;
        }
        else if(d2.compareTo(d1) < 0)
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
     * @param d the double to be formatted
     * @return a String representing the double d
     */
    public static String format(final double d)
    {
        return format(d, 2);
    }

    /**
     * Formats the double d with the specified fraction digits.
     *
     * @param d      the double array to be formatted
     * @param digits the number of fraction digits
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
     * @param d  the double array to be formatted
     * @param nf the number format to be used for formatting
     * @return a String representing the double d
     */
    public static String format(final double d, NumberFormat nf)
    {
        return nf.format(d);
    }

    /**
     * Formats the double array d with ',' as separator and 2 fraction digits.
     *
     * @param d the double array to be formatted
     * @return a String representing the double array d
     */
    public static String format(double[] d)
    {
        return format(d, ", ", 2);
    }

    /**
     * Formats the double array d with ',' as separator and 2 fraction digits.
     *
     * @param d the double array to be formatted
     * @return a String representing the double array d
     */
    public static String format(double[][] d)
    {
        StringBuffer buffer = new StringBuffer();
        for(double[] array : d)
            buffer.append(format(array, ", ", 2) + "\n");
        return buffer.toString();
    }

    /**
     * Formats the double array d with the specified separator and the specified
     * fraction digits.
     *
     * @param d      the double array to be formatted
     * @param sep    the seperator between the single values of the double array,
     *               e.g. ','
     * @param digits the number of fraction digits
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
     * @param d  the double array to be formatted
     * @param nf the number format to be used for formatting
     * @return a String representing the double array d
     */
    public static String format(double[] d, NumberFormat nf)
    {
        return format(d, " ", nf);
    }

    /**
     * Formats the double array d with the specified number format.
     *
     * @param d   the double array to be formatted
     * @param sep the seperator between the single values of the double array,
     *            e.g. ','
     * @param nf  the number format to be used for formatting
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
     * Formats the float array f with the specified separator and the specified
     * fraction digits.
     *
     * @param f      the float array to be formatted
     * @param sep    the seperator between the single values of the float array,
     *               e.g. ','
     * @param digits the number of fraction digits
     * @return a String representing the float array f
     */
    public static String format(float[] f, String sep, int digits)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < f.length; i++)
        {
            if(i < f.length - 1)
            {
                buffer.append(format(f[i], digits)).append(sep);
            }
            else
            {
                buffer.append(format(f[i], digits));
            }
        }
        return buffer.toString();
    }

    /**
     * Formats the float array f with ',' as separator and 2 fraction digits.
     *
     * @param f the float array to be formatted
     * @return a String representing the float array f
     */
    public static String format(float[] f)
    {
        return format(f, ", ", 2);
    }

    /**
     * Formats the int array d for printing purposes.
     *
     * @param d the int array to be formatted
     * @return a String representing the int array d
     */
    public static String format(int[] d)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < d.length; i++)
        {
            if(i < d.length - 1)
            {
                buffer.append(d[i]).append(", ");
            }
            else
            {
                buffer.append(d[i]);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the prefix of the specidfied fileName (i.e. the name of the file
     * without extension).
     *
     * @param fileName the name of the file
     * @return the prefix of the specidfied fileName
     */
    public static String getFilePrefix(final String fileName)
    {
        final int index = fileName.lastIndexOf('.');
        if(index < 0)
        {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    /**
     * Returns a new String array containing the same objects as are contained
     * in the given array.
     *
     * @param array an array to copy
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
     * @param array an array to copy
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
     * @param array the array to be unboxed
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
     * @param array the array to be unboxed
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
     * Returns the unboxed float array of the given Object Number array.
     *
     * @param array the array to be unboxed
     * @return the unboxed float array
     */
    public static float[] unboxToFloat(Number[] array)
    {
        float[] unboxed = new float[array.length];
        for(int i = 0; i < unboxed.length; i++)
        {
            unboxed[i] = array[i].floatValue();
        }
        return unboxed;
    }

    /**
     * Returns the centroid as a RealVector object of the specified objects
     * stored in the given database. The objects belonging to the specified ids
     * must be instance of <code>RealVector</code>.
     *
     * @param database the database storing the objects
     * @param ids      the ids of the objects
     * @return the centroid of the specified objects stored in the given
     *         database
     * @throws IllegalArgumentException if the id list is empty
     */
    @SuppressWarnings("unchecked")
    public static RealVector centroid(Database<RealVector> database, List<Integer> ids)
    {
        if(ids.isEmpty())
            throw new IllegalArgumentException("Empty list of ids!");

        int dim = database.dimensionality();
        double[] centroid = new double[dim];

        for(int id : ids)
        {
            RealVector o = database.get(id);
            for(int j = 1; j <= dim; j++)
            {
                centroid[j - 1] += o.getValue(j).doubleValue();
            }
        }

        for(int i = 0; i < dim; i++)
        {
            centroid[i] /= ids.size();
        }

        RealVector o = database.get(ids.get(0));
        return o.newInstance(centroid);
    }

    /**
     * Returns the centroid as a RealVector object of the specified database.
     * The objects must be instance of <code>RealVector</code>.
     *
     * @param database the database storing the objects
     * @return the centroid of the specified objects stored in the given
     *         database
     */
    @SuppressWarnings("unchecked")
    public static RealVector centroid(Database<RealVector> database)
    {
        if(database.size() == 0)
        {
            throw new IllegalArgumentException("Database is empty!");
        }
        int dim = database.dimensionality();
        double[] centroid = new double[dim];

        Iterator<Integer> it = database.iterator();
        while(it.hasNext())
        {
            RealVector o = database.get(it.next());
            for(int j = 1; j <= dim; j++)
            {
                centroid[j - 1] += o.getValue(j).doubleValue();
            }
        }

        for(int i = 0; i < dim; i++)
        {
            centroid[i] /= database.size();
        }
        RealVector o = database.get(database.iterator().next());
        return o.newInstance(centroid);
    }

    /**
     * Determines the covarianvce matrix of the objects stored in the given
     * database.
     *
     * @param database the database storing the objects
     * @param ids      the ids of the objects
     * @return the covarianvce matrix of the specified objects
     */
    @SuppressWarnings("unchecked")
    public static Matrix covarianceMatrix(Database<RealVector> database, List<Integer> ids)
    {
        // centroid
        RealVector centroid = centroid(database, ids);

        // covariance matrixArray
        int columns = centroid.getDimensionality();
        int rows = ids.size();

        double[][] matrixArray = new double[rows][columns];

        for(int i = 0; i < rows; i++)
        {
            RealVector obj = database.get(ids.get(i));
            for(int d = 0; d < columns; d++)
            {
                matrixArray[i][d] = obj.getValue(d + 1).doubleValue() - centroid.getValue(d + 1).doubleValue();
            }
        }
        Matrix centeredMatrix = new Matrix(matrixArray);
        return centeredMatrix.transpose().times(centeredMatrix);
    }

    /**
     * Determines the covarianvce matrix of the objects stored in the given
     * database.
     *
     * @param database the database storing the objects
     * @return the covarianvce matrix of the specified objects
     */
    @SuppressWarnings("unchecked")
    public static Matrix covarianceMatrix(Database<RealVector> database)
    {
        // centroid
        RealVector centroid = centroid(database);

        // covariance matrixArray
        int columns = centroid.getDimensionality();
        int rows = database.size();
        double[][] matrixArray = new double[rows][columns];

        Iterator<Integer> it = database.iterator();
        int i = 0;
        while(it.hasNext())
        {
            RealVector obj = database.get(it.next());
            for(int d = 0; d < columns; d++)
            {
                matrixArray[i][d] = obj.getValue(d + 1).doubleValue() - centroid.getValue(d + 1).doubleValue();
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
     * @param s the string to be parsed.
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
     * Returns a new <code>Float</code> array initialized to the values
     * represented by the specified <code>String</code> and separated by
     * comma, as performed by the <code>valueOf</code> method of class
     * <code>Float</code>.
     *
     * @param s the string to be parsed.
     * @return a new <code>Float</code> array represented by s
     */
    public static float[] parseFloats(String s)
    {
        List<Float> result = new ArrayList<Float>();
        StringTokenizer tokenizer = new StringTokenizer(s, ",");
        while(tokenizer.hasMoreTokens())
        {
            String d = tokenizer.nextToken();
            result.add(Float.parseFloat(d));
        }
        return unboxToFloat(result.toArray(new Float[result.size()]));
    }

    /**
     * Converts the specified list of double objects to a list of float objects.
     *
     * @param values the list of double objects to be converted
     * @return the converted list of float objects
     */
    public static List<Float> convert(List<Double> values)
    {
        List<Float> result = new ArrayList<Float>(values.size());
        for(Double value : values)
        {
            result.add(new Float(value));
        }
        return result;
    }

    /**
     * Converts the specified array of doubles to an array of floats.
     *
     * @param values the array of doubles to be converted
     * @return the converted array of floats
     */
    public static float[] convertToFloat(double[] values)
    {
        float[] result = new float[values.length];
        for(int i = 0; i < values.length; i++)
        {
            result[i] = (float) values[i];
        }
        return result;
    }

    /**
     * Converts the specified array of doubles to an array of floats.
     *
     * @param values the array of doubles to be converted
     * @return the converted array of floats
     */
    public static double[] convertToDoubles(float[] values)
    {
        double[] result = new double[values.length];
        for(int i = 0; i < values.length; i++)
        {
            result[i] = values[i];
        }
        return result;
    }

    /**
     * Provides a script-text for a gnuplot script to use for transposed view of
     * a specific file of given size of data set.
     *
     * @param filename       the filename of the transposed file to be plotted
     * @param datasetSize    the size of the transposed data set
     * @param dimensionality the dimensionality of the transposed data set
     * @return a script-text for a gnuplot script to use for transposed view of
     *         a specific file of given size of data set
     */
    public static String transposedGnuplotScript(String filename, int datasetSize, int dimensionality)
    {
        StringBuffer script = new StringBuffer();
        // script.append("set terminal pbm color;\n");
        script.append("set nokey\n");
        script.append("set data style linespoints\n");
        script.append("set xlabel \"attribute\"\n");
        script.append("show xlabel\n");
        script.append("plot [0:");
        script.append(datasetSize - 1).append("] []");
        for(int p = 1; p <= dimensionality; p++)
        {
            script.append("\"").append(filename).append("\" using ").append(p);
            if(p < dimensionality)
            {
                script.append(", ");
            }
        }
        script.append("\npause -1");
        return script.toString();
    }

    /**
     * Returns a new instance of the given type for the specified className.
     * <p/> If the Class for className is not found, the instantiation is tried
     * using the package of the given type as package of the given className.
     *
     * @param type      desired Class type of the Object to retrieve
     * @param className name of the class to instantiate
     * @return a new instance of the given type for the specified className
     * @throws UnableToComplyException if the instantiation cannot be performed successfully
     */
    public static <T> T instantiate(Class<T> type, String className) throws UnableToComplyException
    {
        T instance;
        try
        {
            try
            {
                instance = type.cast(Class.forName(className).newInstance());
            }
            catch(ClassNotFoundException e)
            {
                // try package of type
                instance = type.cast(Class.forName(type.getPackage().getName() + "." + className).newInstance());
            }
        }
        catch(InstantiationException e)
        {
            throw new UnableToComplyException("InstantiationException occurred!", e);
        }
        catch(IllegalAccessException e)
        {
            throw new UnableToComplyException("IllegalAccessException occurred!", e);
        }
        catch(ClassNotFoundException e)
        {
            throw new UnableToComplyException("ClassNotFoundException occurred!", e);
        }
        catch(ClassCastException e)
        {
            throw new UnableToComplyException("ClassCastException occurred!", e);
        }
        return instance;
    }

    /**
     * Provides a status report line with leading carriage return. Suitable for
     * density based algorithms, since the number of found clusters is counted.
     *
     * @param progress the progress status
     * @param clusters current number of clusters
     * @return a status report line with leading carriage return
     */
    public static String status(Progress progress, int clusters)
    {
        StringBuffer status = new StringBuffer();
        status.append("\r");
        status.append(progress.toString());
        status.append(" Number of clusters: ");
        status.append(clusters);
        status.append(".                           ");
        return status.toString();
    }

    /**
     * Provides a status report line with leading carriage return.
     *
     * @param progress the progress status
     * @return a status report line with leading carriage return
     */
    public static String status(Progress progress)
    {
        StringBuffer status = new StringBuffer();
        status.append("\r");
        status.append(progress.toString());
        status.append("                           ");
        return status.toString();
    }

    /**
     * Prints the given list to the specified PrintStream. The list entries are
     * separated by the specified separator. The last entry is not followed by a
     * separator. Thus, if a newline is used as separator, it might make sense
     * to print a newline to the PrintStream after calling this method.
     *
     * @param list      the list to be printed
     * @param separator the separator to separate entries of the list
     * @param out       the target PrintStream
     */
    public static <O extends Object> void print(List<O> list, String separator, PrintStream out)
    {
        for(Iterator<O> iter = list.iterator(); iter.hasNext();)
        {
            out.print(iter.next());
            if(iter.hasNext())
            {
                out.print(separator);
            }
        }
    }

    /**
     * Returns the index of the maximum of the given values. If no value is
     * bigger than the first, the index of the first entry is returned.
     *
     * @param values the values to find the index of the maximum
     * @return the index of the maximum in the given values
     * @throws ArrayIndexOutOfBoundsException if <code>values.length==0</code>
     */
    public static int getIndexOfMaximum(double[] values) throws ArrayIndexOutOfBoundsException
    {
        int index = 0;
        double max = values[index];
        for(int i = 0; i < values.length; i++)
        {
            if(values[i] > max)
            {
                max = values[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Retrieves all class labels within the database.
     *
     * @param database the database to be scanned for class labels
     * @return a set comprising all class labels that are currently set in the
     *         database
     */
    @SuppressWarnings("unchecked")
    public static SortedSet<ClassLabel> getClassLabels(Database database)
    {
        if(!database.isSet(AssociationID.CLASS))
        {
            throw new IllegalStateException("AssociationID " + AssociationID.CLASS.getName() + " is not set.");
        }
        SortedSet<ClassLabel> labels = new TreeSet<ClassLabel>();
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            labels.add((ClassLabel) database.getAssociation(AssociationID.CLASS, iter.next()));
        }
        return labels;
    }

    /**
     * Returns an array that contains all elements of the first parameter that
     * are not contained by the second parameter. The first parameter must at
     * least be as long as the second. The second must not contain entries that
     * are not contained by the first.
     *
     * @param complete the complete array
     * @param part     an array that contains only elements of the first parameter
     * @return an array that contains all elements of the first parameter that
     *         are not contained by the second parameter
     * @throws IllegalArgumentException if the first array, <code>complete</code> is not as long as
     *                                  the second array, <code>part</code> or the second,
     *                                  <code>part</code>, contains entries that are not contained
     *                                  by the first, <code>complete</code>
     */
    public static String[] difference(String[] complete, String[] part) throws IllegalArgumentException
    {
        if(complete.length < part.length)
        {
            throw new IllegalArgumentException("first array must be at least as long as second array.");
        }
        List<String> result = new ArrayList<String>();

        int first = 0;
        int second = 0;
        while(first < complete.length && second < part.length)
        {
            if(complete[first].equals(part[second]))
            {
                first++;
                second++;
            }
            else
            {
                result.add(complete[first]);
                first++;
            }
        }
        if(second < part.length)
        {
            throw new IllegalArgumentException("second array contains entries that are not contained by the first array.");
        }
        while(first < complete.length)
        {
            result.add(complete[first]);
            first++;
        }
        String[] resultArray = new String[result.size()];
        return result.toArray(resultArray);
    }
    
    /**
     * Provides a string describing the restriction to implement or extend
     * the specified class.
     * 
     * The message has a structure like follows:
     * <pre>
     * (implementing typeName -- available classes:
     * -->class1.name
     * -->class2.name
     * )
     * </pre>
     * 
     * 
     * @param type the type restricting the possible classes
     * @return a description listing all available classes
     * restricted by the specified class
     */
    @SuppressWarnings("unchecked")
    public static String restrictionString(Class type)
    {
        StringBuilder msg = new StringBuilder();
        msg.append('(');
        if(type.isInterface())
        {
            msg.append("implementing ");
        }
        else
        {
            msg.append("extending ");
        }
        msg.append(type.getName());
        Class[] classes = implementingClasses(type);
        if(classes.length > 0)
        {
            msg.append(" -- available classes:\n");
            for(Class c : classes)
            {
                msg.append("-->");
                msg.append(c.getName());
                msg.append('\n');
            }
        }
        msg.append(')');
        return msg.toString();
    }
    
    /**
     * Provides all classes currently known by the Launcher
     * that are instance of the specified type
     * and that are instantiable by the default constructor.
     * 
     * 
     * @param type the common superclass or interface
     * of the required classes
     * @return all classes currently known by the Launcher
     * that are instance of the specified type
     * and that are instantiable by the default constructor
     */
    @SuppressWarnings("unchecked")
    public static Class[] implementingClasses(Class type)
    {
        List<Class> classes = new ArrayList<Class>();
        Package[] packages = Package.getPackages();
        if(DEBUG)
        {
            logger.finest("number of found packages: "+packages.length);
        }
        for(Package p : packages)
        {
            if(DEBUG)
            {
                logger.finest(p.getName());
            }
            Class[] classesInPackage = classesInPackage(p);
            for(Class c : classesInPackage)
            {
                try
                {
                    if(type.isAssignableFrom(c))
                    {
                        classes.add(c);    
                    }
//                    c.asSubclass(type);
//                    classes.add(c);
                }
                catch(ClassCastException e)
                {
                    if(true)
                    {
                        logger.finest(e.getMessage());
                    }
                }
            }
        }
        Class[] result = new Class[classes.size()];
        return classes.toArray(result);
    }
    
    /**
     * Provides all classes in the specified package
     * as currently present in the Launcher.
     * Only those classes are included
     * that can be instantiated per default constructor.
     * 
     * 
     * @param p the package to retrieve classes for
     * @return all classes in the specified package
     */
    @SuppressWarnings("unchecked")
    public static Class[] classesInPackage(Package p)
    {
        List<Class> classes = new ArrayList<Class>();
        final String CLASS_SUFFIX = ".class";
        String pname = p.getName();
        URL url = Launcher.class.getResource(pname);
        if(url==null)
        {            
            pname = pname.replace('.', '/');
            if(!pname.startsWith("/"))
            {
                pname = "/"+pname; 
            }
            url = Launcher.class.getResource(pname);
        }
        if(url!=null)
        {
            File dir = new File(url.getFile());
            if(dir.exists())
            {
                String[] files = dir.list();
                for(String f : files)
                {
                    if(f.endsWith(CLASS_SUFFIX))
                    {
                        // remove the .class extension
                        String classname = f.substring(0,f.length()-6);
                        try
                        {
                            if(DEBUG)
                            {
                                logger.finest("classname: " + classname);
                            }
                            Class c = Class.forName(p.getName()+"."+classname);
                            if(DEBUG)
                            {
                                logger.finest("class: " + c.getName());
                            }
                            Object o = c.newInstance();
                            if(DEBUG)
                            {
                                logger.finest("object class: " + o.getClass().getName());
                            }
                            classes.add(c);
                        }
                        catch(Exception e)
                        {
                            if(DEBUG)
                            {
                                logger.finest(e.getMessage()+"\n");
                            }
                        }
                    }
                }
            }
        }
        else
        {
            if(DEBUG)
            {
                logger.finest("No resource available for name: \""+pname+"\"\n");
            }
        }
        Class[] result = new Class[classes.size()];
        return classes.toArray(result);        
    }
    
}