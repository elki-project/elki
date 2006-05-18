package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.PointerRepresentation;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Efficient implementation of the Single-Link Algorithm SLINK of R. Sibson.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SLINK<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D>
{
    /**
     * Holds the class specific debug status.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static final boolean DEBUG = LoggingConfiguration.DEBUG;

    /**
     * The logger of this class.
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * The values of the function Pi of the pointer representation.
     */
    private HashMap<Integer, Integer> pi = new HashMap<Integer, Integer>();

    /**
     * The values of the function Lambda of the pointer representation.
     */
    private HashMap<Integer, SLinkDistance> lambda = new HashMap<Integer, SLinkDistance>();

    /**
     * The values of the helper function m to determine the pointer
     * representation.
     */
    private HashMap<Integer, SLinkDistance> m = new HashMap<Integer, SLinkDistance>();

    /**
     * Provides the result of the algorithm.
     */
    protected Result<O> result;

    /**
     * Craetes a new instance of a single link algorithm. Since SLINK is a non
     * abstract class the option handler is initialized.
     */
    public SLINK()
    {
        super();
    }

    /**
     * Runs the algorithm.
     *
     * @param database the database to run the algorithm on
     * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
     *                               setParameters(String[]) method has been failed to be called).
     */
    protected void runInTime(Database<O> database) throws IllegalStateException
    {

        try
        {
            Progress progress = new Progress("Clustering", database.size());
            getDistanceFunction().setDatabase(database, isVerbose(), isTime());

            // sort the db objects according to their ids
            ArrayList<Integer> ids = new ArrayList<Integer>();
            Iterator<Integer> it = database.iterator();
            while(it.hasNext())
            {
                ids.add(it.next());
            }
            Collections.sort(ids);

            ArrayList<Integer> processedIDs = new ArrayList<Integer>();
            // apply the algorithm
            for(Integer id : ids)
            {
                step1(id);
                step2(id, processedIDs);
                step3(id, processedIDs);
                step4(id, processedIDs);

                processedIDs.add(id);

                if(isVerbose())
                {
                    progress.setProcessed(id);
                    logger.log(new ProgressLogRecord(Level.INFO, Util.status(progress), progress.getTask(), progress.status()));
                }
            }
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e);
        }

        HashMap<Integer, Integer> piClone = (HashMap<Integer, Integer>) pi.clone();
        HashMap<Integer, SLinkDistance> lambdaClone = (HashMap<Integer, SLinkDistance>) lambda.clone();

        if(isVerbose())
        {
            logger.info("\n");
        }
        result = new PointerRepresentation<O, D>(piClone, lambdaClone, getDistanceFunction(), database);
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public Result<O> getResult()
    {
        return result;
    }

    /**
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    public Description getDescription()
    {
        return new Description("SLINK",
                               "Single Link Clustering",
                               "Hierarchical clustering algorithm.",
                               "R. Sibson: SLINK:  An optimally efficient algorithm for the single-link cluster method." +
                               "In: The Computer Journal 16 (1973), No. 1, p. 30-34.");
    }

    /**
     * First step: Initialize P(id) = id, L(id) = infinity.
     *
     * @param newID the id of the object to be inserted into the pointer
     *              representation
     */
    private void step1(int newID)
    {
        // P(n+1) = n+1:
        pi.put(newID, newID);
        // L(n+1) = infinity
        lambda.put(newID, new SLinkDistance(getDistanceFunction().infiniteDistance(), null, null));
    }

    /**
     * Second step: Determine the pairwise distances from all objects in the
     * pointer representation to the new object with the specified id.
     *
     * @param newID the id of the object to be inserted into the pointer
     *              representation
     */
    private void step2(int newID, ArrayList<Integer> processedIDs)
    {
        // M(i) = dist(i, n+1)
        for(Integer id : processedIDs)
        {
            SLinkDistance distance = new SLinkDistance(getDistanceFunction().distance(newID, id), newID, id);
            m.put(id, distance);
        }
    }

    /**
     * Third step: Determine the values for P and L
     *
     * @param newID the id of the object to be inserted into the pointer
     *              representation
     */
    private void step3(int newID, ArrayList<Integer> processedIDs)
    {
        // for i = 1..n
        for(Integer id : processedIDs)
        {
            SLinkDistance l = lambda.get(id);
            SLinkDistance m = this.m.get(id);
            Integer p = pi.get(id);
            SLinkDistance mp = this.m.get(p);

            // if L(i) >= M(i)
            if(l.compareTo(m) >= 0)
            {
                SLinkDistance min = min(mp, l);
                // M(P(i)) = min { M(P(i)), L(i) }
                this.m.put(p, min);

                // L(i) = M(i)
                lambda.put(id, m);

                // P(i) = n+1;
                pi.put(id, newID);
            }

            else
            {
                SLinkDistance min = min(mp, m);
                // M(P(i)) = min { M(P(i)), M(i) }
                this.m.put(p, min);
            }
        }
    }

    /**
     * Fourth step: Actualize the clusters if necessary
     *
     * @param newID
     */
    private void step4(int newID, ArrayList<Integer> processedIDs)
    {
        // for i = 1..n
        for(Integer id : processedIDs)
        {
            if(id == newID)
                continue;

            SLinkDistance l = lambda.get(id);
            Integer p = pi.get(id);
            SLinkDistance lp = lambda.get(p);

            // if L(i) >= L(P(i))
            if(l.compareTo(lp) >= 0)
            {
                // P(i) = n+1
                pi.put(id, newID);
            }
        }
    }

    /**
     * Returns the minimum distance of the two given distances.
     *
     * @param d1 the first distance
     * @param d2 the second distance
     * @return the minimum distance of the two given distances
     */
    private SLinkDistance min(SLinkDistance d1, SLinkDistance d2)
    {
        int comp = d1.distance.compareTo(d2.distance);
        if(comp >= 0)
            return d1;
        return d2;
    }

    /**
     * Encapsulates the distance between two objects and their ids.
     */
    public class SLinkDistance implements Comparable<SLinkDistance>
    {
        D distance;

        Integer id1;

        Integer id2;

        public SLinkDistance(D distance, Integer id1, Integer id2)
        {
            this.distance = distance;
            this.id1 = id1;
            this.id2 = id2;
        }

        /**
         * Compares this object with the specified object for order. Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         * <p/>
         *
         * @param o the Object to be compared.
         * @return a negative integer, zero, or a positive integer as this
         *         object is less than, equal to, or greater than the specified
         *         object.
         */
        public int compareTo(SLinkDistance o)
        {
            int compare = this.distance.compareTo(o.distance);
            if(compare != 0)
                return compare;

            if(this.id1 < (o.id1))
                return -1;

            if(this.id1 > (o.id1))
                return +1;

            if(this.id2 < (o.id2))
                return -1;

            if(this.id2 > (o.id2))
                return +1;

            return 0;
        }

        /**
         * Returns the distance value.
         *
         * @return the distance value
         */
        public D getDistance()
        {
            return distance;
        }

        /**
         * Returns a string representation of the object.
         *
         * @return a string representation of the object.
         */
        public String toString()
        {
            return distance.toString() + " (" + id1 + ", " + id2 + ")";
        }
    }

}
