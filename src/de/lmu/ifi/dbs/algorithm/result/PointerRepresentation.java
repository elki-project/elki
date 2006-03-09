package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.algorithm.clustering.SLINK;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Provides the result of the single link algorithm SLINK.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PointerRepresentation<O extends DatabaseObject, D extends Distance<D>> extends AbstractResult<O>
{

    /**
     * The values of the function Pi of the pointer representation.
     */
    private HashMap<Integer, Integer> pi = new HashMap<Integer, Integer>();

    /**
     * The values of the function Lambda of the pointer representation.
     */
    private HashMap<Integer, SLINK<O, D>.SLinkDistance> lambda = new HashMap<Integer, SLINK<O, D>.SLinkDistance>();

    /**
     * The distance function this pointer representation was computed with.
     */
    private DistanceFunction<O, D> distanceFunction;

    /**
     * Creates a new pointer representation.
     *
     * @param pi
     *            the values of the function Pi of the pointer representation
     * @param lambda
     *            the values of the function Lambda of the pointer
     *            representation
     * @param distanceFunction
     *            the distance function this pointer representation was computed
     *            with
     * @param database
     *            the database containing the objects
     */
    public PointerRepresentation(HashMap<Integer, Integer> pi, HashMap<Integer, SLINK<O, D>.SLinkDistance> lambda, DistanceFunction<O, D> distanceFunction, Database<O> database)
    {
        super(database);
        this.pi = pi;
        this.lambda = lambda;
        this.distanceFunction = distanceFunction;
    }

    /**
     * @see Result#output(File, Normalization, List)
     */
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        PrintStream outStream;
        try
        {
            outStream = new PrintStream(new FileOutputStream(out));
        }
        catch(Exception e)
        {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }

        output(outStream, normalization,settings);
    }

    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        writeHeader(outStream, settings, null);

        outStream.println(this.toString());
        outStream.flush();
    }

    /**
     * Returns a string representation of this pointer representation.
     *
     * @return a string representation of this pointer representation
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        SortedSet<Integer> keys = new TreeSet<Integer>(pi.keySet());
        for(Integer id : keys)
        {
            result.append("P(");
            result.append(id);
            result.append(") = ");
            result.append(pi.get(id));
            result.append("   L(");
            result.append(id);
            result.append(") = ");
            result.append(lambda.get(id));
            result.append("\n");
        }
        return result.toString();
    }

    /**
     * Returns the clustering result for a given distance threshold.
     *
     * @param distancePattern
     *            the pattern of the threshold
     * @return the clustering result: each element of the returned collection is
     *         a list of ids representing one cluster
     */
    public Collection<List<Integer>> getClusters(String distancePattern)
    {
        D distance = distanceFunction.valueOf(distancePattern);

        HashMap<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
        for(Integer id : pi.keySet())
        {
            Integer partitionID = id;
            while(lambda.get(partitionID).getDistance().compareTo(distance) <= 0)
            {
                partitionID = pi.get(partitionID);
            }

            List<Integer> partition = partitions.get(partitionID);
            if(partition == null)
            {
                partition = new ArrayList<Integer>();
                partitions.put(partitionID, partition);
            }
            partition.add(id);
        }
        return partitions.values();
    }
}
