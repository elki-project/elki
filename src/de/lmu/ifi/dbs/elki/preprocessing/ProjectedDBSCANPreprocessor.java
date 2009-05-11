package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * Abstract superclass for preprocessor of algorithms extending
 * the ProjectedDBSCAN algorithm.
 *
 * @author Arthur Zimek
 * @param <D> Distance type
 * @param <V> Vector type
 */
public abstract class ProjectedDBSCANPreprocessor<D extends Distance<D>, V extends RealVector<V, ?>> extends AbstractParameterizable implements Preprocessor<V> {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -epsilon} </p>
     */
    public static final PatternParameter EPSILON_PARAM = new PatternParameter(DBSCAN.EPSILON_ID,
        "the maximum radius of the neighborhood " +
            "to be considered, must be suitable to " +
            LocallyWeightedDistanceFunction.class.getName());

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        ProjectedDBSCAN.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * The default range query distance function.
     */
    public static final String DEFAULT_DISTANCE_FUNCTION = EuclideanDistanceFunction.class.getName();

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "projdbscan.distancefunction", "Distance function to determine the neighbors for variance analysis.");
    
    /**
     * Parameter distance function
     */
    private final ClassParameter<DistanceFunction<V, D>> DISTANCE_FUNCTION_PARAM =
      new ClassParameter<DistanceFunction<V, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);

    /**
     * Contains the value of parameter epsilon;
     */
    private String epsilon;

    /**
     * The distance function for the variance analysis.
     */
    protected DistanceFunction<V, D> rangeQueryDistanceFunction;

    /**
     * Holds the value of parameter minpts.
     */
    private int minpts;

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    protected ProjectedDBSCANPreprocessor() {
        super();
        //parameter epsilon
        addOption(EPSILON_PARAM);

        //parameter minpts
        addOption(MINPTS_PARAM);

        // parameter range query distance function
        addOption(DISTANCE_FUNCTION_PARAM);

        // global constraint epsilon <-> distancefunction
        GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint<DistanceFunction<V, D>>(EPSILON_PARAM, DISTANCE_FUNCTION_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);
    }

    public void run(Database<V> database, boolean verbose, boolean time) {
        if (database == null || database.size() <= 0) {
          throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
        }

        long start = System.currentTimeMillis();
        rangeQueryDistanceFunction.setDatabase(database, verbose, time);

        FiniteProgress progress = new FiniteProgress(this.getClass().getName(), database.size());
        if (logger.isVerbose()) {
          logger.verbose("Preprocessing:");
        }
        Iterator<Integer> it = database.iterator();
        int processed = 1;
        while (it.hasNext()) {
            Integer id = it.next();
            List<DistanceResultPair<D>> neighbors = database.rangeQuery(id, epsilon, rangeQueryDistanceFunction);

            if (neighbors.size() >= minpts) {
                runVarianceAnalysis(id, neighbors, database);
            }
            else {
                DistanceResultPair<D> firstQR = neighbors.get(0);
                neighbors = new ArrayList<DistanceResultPair<D>>();
                neighbors.add(firstQR);
                runVarianceAnalysis(id, neighbors, database);
            }

            progress.setProcessed(processed++);
            if (logger.isVerbose()) {
              logger.progress(progress);
            }
        }

        long end = System.currentTimeMillis();
        if (time) {
            long elapsedTime = end - start;
            logger.verbose(this.getClass().getName() + " runtime: "
                + elapsedTime + " milliseconds.");
        }
    }

    /**
     * This method implements the type of variance analysis to be computed for a given point.
     * <p/>
     * Example1: for 4C, this method should implement a PCA for the given point.
     * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
     *
     * @param id        the given point
     * @param neighbors the neighbors as query results of the given point
     * @param database  the database for which the preprocessing is performed
     */
    protected abstract void runVarianceAnalysis(Integer id, List<DistanceResultPair<D>> neighbors, Database<V> database);

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        rangeQueryDistanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        remainingParameters = rangeQueryDistanceFunction.setParameters(remainingParameters);
        addParameterizable(rangeQueryDistanceFunction);

        // epsilon
        epsilon = EPSILON_PARAM.getValue();

        // minpts
        minpts = MINPTS_PARAM.getValue();

        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }
}