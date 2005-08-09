package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.database.Database;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;



/**
 * Provides a result of a clustering-algorithm that computes
 * several clusters and remaining noise.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClustersPlusNoise implements Result
{
    /**
     * An array of clusters and noise, respectively,
     * where each array provides the object ids of its members
     */
    protected Integer[][] clustersAndNoise;
    
    /**
     * The database containing the objects of clusters.
     */
    protected Database db;
    
    /**
     * String to separate different attribute values while printing.
     */
    protected String separator = " ";
    
    /**
     * Provides a result of a clustering-algorithm that computes
     * several clusters and remaining noise.
     * 
     * @param clustersAndNoise an array of clusters and noise, respectively,
     * where each array provides the object ids of its members
     * @param db the database containing the objects of clusters
     */
    public ClustersPlusNoise(Integer[][] clustersAndNoise, Database db)
    {
        this.clustersAndNoise = clustersAndNoise;
        this.db = db;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.Result#output(java.io.File)
     */
    public void output(File out)
    {
        for(int c = 0; c < this.clustersAndNoise.length; c++)
        {
            String marker;
            if(c < clustersAndNoise.length - 1)
            {
                marker = "Cluster"+format(c+1,clustersAndNoise.length - 1);
            }
            else
            {
                marker = "Noise";
            }
            PrintStream markedOut;
            try
            {
                File markedFile = new File(out.getAbsolutePath()+marker);
                markedFile.getParentFile().mkdirs();
                markedOut = new PrintStream(new FileOutputStream(markedFile));
            }
            catch (Exception e)
            {
                markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
                markedOut.println(marker+":");
            }
            write(c,markedOut);
            markedOut.flush();
        }

    }
    
    /**
     * Returns an integer-string for the given input,
     * that has as many leading zeros as to match the
     * length of the specified maximum.
     * 
     * 
     * @param input an integer to be formatted
     * @param maximum the maximum to adapt the format to
     * @return an integer-string for the given input,
     * that has as many leading zeros as to match the
     * length of the specified maximum
     */
    protected String format(int input, int maximum)
    {
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        formatter.setMinimumIntegerDigits(Integer.toString(maximum).length());
        return formatter.format(input);
    }
    
    /**
     * Writes a cluster denoted by its cluster number
     * to the designated print stream.
     * 
     * @param clusterIndex the number of the cluster to be written
     * @param out the print stream where to write
     */
    protected void write(int clusterIndex, PrintStream out)
    {
        for(int i = 0; i < clustersAndNoise[clusterIndex].length; i++)
        {
            out.println(db.get(clustersAndNoise[clusterIndex][i]).toString());
        }
    }

}
