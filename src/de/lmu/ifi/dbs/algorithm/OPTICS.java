package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * OPTICS provides the OPTICS algorithm.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OPTICS<T extends MetricalObject> extends DistanceBasedAlgorithm<T>
{
    /**
     * Parameter for epsilon.
     */
    public static final String EPSILON_P = "epsilon";

    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

    /**
     * Parameter minimum points.
     */
    public static final String MINPTS_P = "minpts";

    /**
     * Description for parameter minimum points.
     */
    public static final String MINPTS_D = "<int>minpts";

    /**
     * Epsilon.
     */
    private String epsilon;

    /**
     * Minimum points.
     */
    private int minpts;

    /**
     * Provides the result of the algorithm.
     */
    private ClusterOrder clusterOrder;

    /**
     * Holds a set of processed ids.
     */
    private Set<Integer> processedIDs;

    /**
     * The priority queue for the algorithm.
     */
    private Heap<Distance, COEntry> pq;

    /**
     * Sets epsilon and minimum points to the optionhandler additionally to the
     * parameters provided by super-classes. Since OPTICS is a non-abstract
     * class, finally optionHandler is initialized.
     */
    public OPTICS()
    {
        super();
        parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
        parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
        optionHandler = new OptionHandler(parameterToDescription, OPTICS.class.getName());
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void run(Database<T> database) throws IllegalStateException
    {
        long start = System.currentTimeMillis();
        try
        {
            Progress progress = new Progress(database.size());

            processedIDs = new HashSet<Integer>(database.size());
            clusterOrder = new ClusterOrder(database, getDistanceFunction());
            pq = new DefaultHeap<Distance, COEntry>();
            getDistanceFunction().setDatabase(database);

            for(Iterator<Integer> it = database.iterator(); it.hasNext();)
            {
                Integer id = it.next();
                if(!processedIDs.contains(id))
                    expandClusterOrder(database, id, progress);
            }
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e);
        }
        long end = System.currentTimeMillis();

        if(isTime())
        {
            long elapsedTime = end - start;
            System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
        }
    }

    /**
     * OPTICS-function expandClusterOrder.
     * 
     * @param database
     *            the database on which the algorithm is run
     * @param objectID
     *            the currently processed object
     * @param progress
     *            the progress object to actualize the current progess if the
     *            algorithm
     */
    @SuppressWarnings( { "unchecked" })
    protected void expandClusterOrder(Database<T> database, Integer objectID, Progress progress)
    {
        clusterOrder.add(objectID, null, getDistanceFunction().infiniteDistance());
        processedIDs.add(objectID);

        List<QueryResult> neighbours = database.rangeQuery(objectID, epsilon, getDistanceFunction());
        Distance coreDistance = neighbours.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbours.get(minpts - 1).getDistance();

        if(!getDistanceFunction().isInfiniteDistance(coreDistance))
        {
            for(QueryResult neighbour : neighbours)
            {
                if(processedIDs.contains(neighbour.getID()))
                    continue;

                Distance reachability = maximum(neighbour.getDistance(), coreDistance);
                HeapNode<Distance, COEntry> node = new DefaultHeapNode<Distance, COEntry>(reachability, new COEntry(neighbour.getID(), objectID));
                pq.addNode(node);
            }

            while(!pq.isEmpty())
            {
                final HeapNode<Distance, COEntry> pqNode = pq.getMinNode();
                COEntry current = pqNode.getValue();
                if(processedIDs.contains(current.objectID))
                    continue;

                clusterOrder.add(current.objectID, current.predecessorID, pqNode.getKey());
                processedIDs.add(current.objectID);

                neighbours = database.rangeQuery(current.objectID, epsilon, getDistanceFunction());
                coreDistance = neighbours.size() < minpts ? getDistanceFunction().infiniteDistance() : neighbours.get(minpts - 1).getDistance();

                if(!getDistanceFunction().isInfiniteDistance(coreDistance))
                {
                    for(QueryResult neighbour : neighbours)
                    {
                        if(processedIDs.contains(neighbour.getID()))
                            continue;

                        Distance distance = neighbour.getDistance();
                        Distance reachability = maximum(distance, coreDistance);
                        HeapNode<Distance, COEntry> node = new DefaultHeapNode<Distance, COEntry>(reachability, new COEntry(neighbour.getID(), current.objectID));
                        pq.addNode(node);
                    }
                }
                if(isVerbose())
                {
                    progress.setProcessed(processedIDs.size());
                    System.out.println("\r" + progress.toString());
                }
            }
        }
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("OPTICS", "Density-Based Hierarchical Clustering", "Algorithm to find density-connected sets in a database based on the parameters minimumPoints and epsilon (specifying a volume). These two parameters determine a density threshold for clustering.", "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander: " + "OPTICS: Ordering Points to Identify the Clustering Structure. " + "In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)");
    }

    /**
     * Sets the parameters epsilon and minpts additionally to the parameters set
     * by the super-class' method. Both epsilon and minpts are required
     * parameters.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            getDistanceFunction().valueOf(optionHandler.getOptionValue(EPSILON_P));
            epsilon = optionHandler.getOptionValue(EPSILON_P);
            minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }

    /**
     * @see Algorithm#getResult()
     */
    public Result getResult()
    {
        return clusterOrder;
    }

    /**
     * Returns the maximum of both given distances.
     * 
     * @param d1
     *            the first distance
     * @param d2
     *            the second distance
     * @return the maximum of both given distances
     */
    private Distance maximum(Distance d1, Distance d2)
    {
        if(d1.compareTo(d2) >= 0)
            return d1;
        else
            return d2;
    }

    /**
     * Encapsulates an entry in the cluster order.
     */
    class COEntry implements Comparable<COEntry>
    {
        /**
         * The id of the entry.
         */
        Integer objectID;

        /**
         * The id of the entry's predecessor.
         */
        Integer predecessorID;

        /**
         * Creates a new entry with the specified parameters.
         * 
         * @param objectID
         *            the id of the entry
         * @param predecessorID
         *            the id of the entry's predecessor
         */
        public COEntry(Integer objectID, Integer predecessorID)
        {
            this.objectID = objectID;
            this.predecessorID = predecessorID;
        }

        /**
         * Compares this object with the specified object for order. Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         * <p>
         * 
         * @param other
         *            the Object to be compared.
         * @return a negative integer, zero, or a positive integer as this
         *         object is less than, equal to, or greater than the specified
         *         object.
         */
        public int compareTo(COEntry other)
        {
            if(this.objectID < other.objectID)
                return -1;
            if(this.objectID > other.objectID)
                return +1;

            if(this.predecessorID < other.predecessorID)
                return -1;
            if(this.predecessorID > other.predecessorID)
                return +1;
            return 0;
        }
    }
}
