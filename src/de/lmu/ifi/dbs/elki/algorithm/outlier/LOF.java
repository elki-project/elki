package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationsFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * <p/>
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter minpts. The computed nearest neighbors and LOFs are persistently stored in
 * tables in order to enable insertions or deletions of database objects using the algorithm {@link de.lmu.ifi.dbs.elki.algorithm.outlier.OnlineLOF}.
 * </p>
 * <p/>
 * Reference:
 * <br>M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander:
 * LOF: Identifying Density-Based Local Outliers.
 * <br>In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000.
 * </p>
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class LOF<O extends DatabaseObject> extends
    DistanceBasedAlgorithm<O, DoubleDistance, MultiResult> {

    /**
     * OptionID for {@link #PAGE_SIZE_PARAM}
     */
    public static final OptionID PAGE_SIZE_ID = OptionID.getOrCreateOptionID(
        "lof.pagesize",
        "The size of a page in bytes."
    );

    /**
     * Parameter to specify the size of a page in bytes,
     * must be an integer greater than 0.
     * <p>Default value: {@code 4000} </p>
     * <p>Key: {@code -lof.pagesize} </p>
     */
    private final IntParameter PAGE_SIZE_PARAM = new IntParameter(
        PAGE_SIZE_ID, new GreaterConstraint(0), 4000);

    /**
     * Holds the value of {@link #PAGE_SIZE_PARAM}.
     */
    int pageSize;

    /**
     * OptionID for {@link #CACHE_SIZE_PARAM}
     */
    public static final OptionID CACHE_SIZE_ID = OptionID.getOrCreateOptionID(
        "lof.cachesize",
        "The size of the cache in bytes."
    );

    /**
     * Parameter to specify the size of the cache in bytes,
     * must be an integer greater than 0.
     * <p>Default value: {@link Integer#MAX_VALUE} </p>
     * <p>Key: {@code -lof.cachesize} </p>
     */
    private final IntParameter CACHE_SIZE_PARAM = new IntParameter(
        CACHE_SIZE_ID, new GreaterConstraint(0), Integer.MAX_VALUE);

    /**
     * Holds the value of {@link #CACHE_SIZE_PARAM}.
     */
    int cacheSize;

    /**
     * OptionID for {@link #MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "lof.minpts",
        "The number of nearest neighbors of an object to be considered for computing its LOF."
    );

    /**
     * Parameter to specify the number of nearest neighbors of an object to be considered for computing its LOF,
     * must be an integer greater than 0.
     * <p>Key: {@code -lof.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Holds the value of {@link #MINPTS_PARAM}.
     */
    int minpts;

    /**
     * Provides the result of the algorithm.
     */
    MultiResult result;

    /**
     * The table for nearest and reverse nearest neighbors.
     */
    NNTable nnTable;

    /**
     * The table for neares and reverse nearest neighbors.
     */
    LOFTable lofTable;

    /**
     * Provides the LOF algorithm,
     * adding parameters
     * {@link #PAGE_SIZE_PARAM}, {@link #CACHE_SIZE_PARAM}, and {@link #MINPTS_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public LOF() {
        super();
        // parameter page size
        addOption(PAGE_SIZE_PARAM);

        // parameter cache size
        addOption(CACHE_SIZE_PARAM);

        //parameter minpts
        addOption(MINPTS_PARAM);
    }

    /**
     * Performs the LOF algorithm on the given database.
     */
    @Override
    protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        if (logger.isVerbose()) {
          logger.verbose("##### Computing LOFs:");
        }

        {// compute neighbors of each db object
            if (logger.isVerbose()) {
              logger.verbose("Step 1: computing neighborhoods:");
            }
            FiniteProgress progressNeighborhoods = new FiniteProgress("LOF", database.size());
            int counter = 1;
            nnTable = new NNTable(pageSize, cacheSize, minpts);
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                computeNeighbors(database, id);
                if (logger.isVerbose()) {
                    progressNeighborhoods.setProcessed(counter);
                    logger.progress(progressNeighborhoods);
                }
            }
        }

        {// computing reachability distances
            if (logger.isVerbose()) {
              logger.verbose("Step 2: computing reachability distances:");
            }
            FiniteProgress progressNeighborhoods = new FiniteProgress("LOF", database.size());
            int counter = 1;
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                nnTable.computeReachabilityDistances(id);
                if (logger.isVerbose()) {
                    progressNeighborhoods.setProcessed(counter);
                    logger.progress(progressNeighborhoods);
                }
            }
        }

        {// compute LOF of each db object
            if (logger.isVerbose()) {
              logger.verbose("Step 3: computing LOFs:");
            }
            // keeps the lofs for each object
            lofTable = new LOFTable(pageSize, cacheSize, minpts);
            {
                FiniteProgress progressLOFs = new FiniteProgress("LOF: LOF for objects",
                    database.size());
                int counter = 0;
                for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                    Integer id = iter.next();
                    computeLOF(id);
                    if (logger.isVerbose()) {
                        progressLOFs.setProcessed(counter + 1);
                        logger.progress(progressLOFs);
                    }
                }
            }

            if (isTime()) {
              logger.verbose("\nPhysical read Access LOF-Table: "
                    + lofTable.getPhysicalReadAccess());

              logger.verbose("Physical write Access LOF-Table: "
                    + lofTable.getPhysicalWriteAccess());

              logger.verbose("Logical page Access LOF-Table:  "
                    + lofTable.getLogicalPageAccess());

              logger.verbose("Physical read Access NN-Table:  "
                    + nnTable.getPhysicalReadAccess());

              logger.verbose("Physical write Access NN-Table:  "
                    + nnTable.getPhysicalWriteAccess());

              logger.verbose("Logical page Access NN-Table:   "
                    + nnTable.getLogicalPageAccess());
            }
            // FIXME: re-add lof and nn tables.
            
            //AnnotationsFromDatabase<O, Double> res1 = new AnnotationsFromDatabase<O, Double>(database);
            //res1.addAssociation(AssociationID.LOF);
            // Ordering
            //OrderingFromAssociation<Double, O> res2 = new OrderingFromAssociation<Double, O>(database, AssociationID.LOF, true); 
            // combine results.
            //result = new MultiResult();
            //result.addResult(res1);
            //result.addResult(res2);
        }
        return result;
    }

    /**
     * Computes the minpts-nearest neighbors of a given object in a given
     * database and inserts them into the nnTable.
     *
     * @param database the database containing the objects
     * @param id       the object id
     */
    public void computeNeighbors(Database<O> database, Integer id) {
        List<DistanceResultPair<DoubleDistance>> neighbors = database.kNNQueryForID(
            id, minpts + 1, getDistanceFunction());
        neighbors.remove(0);

        for (int k = 0; k < minpts; k++) {
          DistanceResultPair<DoubleDistance> qr = neighbors.get(k);
            Neighbor neighbor = new Neighbor(id, k, qr.getID(), 0, qr.getDistance().getValue());
            nnTable.insert(neighbor);
        }
    }

    /**
     * Computes the LOF value for a given object
     *
     * @param id the object id
     */
    protected void computeLOF(Integer id) {
        NeighborList neighbors_o = nnTable.getNeighbors(id);

        double sum1 = 0;
        double[] sum2 = new double[minpts];

        for (int k = 0; k < neighbors_o.size(); k++) {
            Neighbor p = neighbors_o.get(k);

            // sum1
            sum1 += p.getReachabilityDistance();

            // sum2
            double sum = 0;
            NeighborList neighbors_p = nnTable.getNeighbors(p.getNeighborID());
            for (Neighbor q : neighbors_p) {
                sum += q.getReachabilityDistance();
            }
            sum2[k] = sum;
        }

        LOFEntry entry = new LOFEntry(sum1, sum2);
        lofTable.insert(id, entry);
    }

    public Description getDescription() {
        return new Description(
            "LOF",
            "Local Outlier Factor",
            "Algorithm to compute density-based local outlier factors in a database based on the parameter " +
                MINPTS_PARAM,
            "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander: " +
                " LOF: Identifying Density-Based Local Outliers. " +
                "In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000.");
    }

    /**
     * Calls the super method
     * and sets additionally the values of the parameters
     * {@link #PAGE_SIZE_PARAM}, {@link #CACHE_SIZE_PARAM}, and {@link #MINPTS_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // pagesize
        pageSize = PAGE_SIZE_PARAM.getValue();

        // cachesize
        cacheSize = CACHE_SIZE_PARAM.getValue();

        // minpts
        minpts = MINPTS_PARAM.getValue();

        return remainingParameters;
    }

    public MultiResult getResult() {
        return result;
    }
}
