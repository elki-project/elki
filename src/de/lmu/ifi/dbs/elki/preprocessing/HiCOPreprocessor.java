package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * Abstract superclass for preprocessors for HiCO correlation dimension
 * assignment to objects of a certain database.
 *
 * @author Elke Achtert
 * @param <V> Vector type
 */
public abstract class HiCOPreprocessor<V extends RealVector<V, ?>> extends AbstractParameterizable implements Preprocessor<V> {
    /**
     * The default distance function for the PCA.
     */
    public static final String DEFAULT_PCA_DISTANCE_FUNCTION = EuclideanDistanceFunction.class.getName();

    /**
     * OptionID for {@link #PCA_DISTANCE_PARAM}
     */
    public static final OptionID PCA_DISTANCE_ID = OptionID.getOrCreateOptionID("hico.pca.distance",
        "The distance function used to select object for running PCA.");
    
    /**
     * Parameter to specify the distance function used for running PCA.
     * 
     * Key: {@code -hico.pca.distance}
     */
    protected final ClassParameter<DistanceFunction<V, DoubleDistance>> PCA_DISTANCE_PARAM = new ClassParameter<DistanceFunction<V, DoubleDistance>>(PCA_DISTANCE_ID,
        DistanceFunction.class, DEFAULT_PCA_DISTANCE_FUNCTION );
    
    /**
     * The distance function for the PCA.
     */
    protected DistanceFunction<V, DoubleDistance> pcaDistanceFunction;

    /**
     * PCA utility object
     */
    private PCAFilteredRunner<V> pca = new PCAFilteredRunner<V>();

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    public HiCOPreprocessor() {
        super();
        // parameter pca distance function
        addOption(PCA_DISTANCE_PARAM);
        addParameterizable(pca);
    }

    /**
     * This method determines the correlation dimensions of the objects stored in
     * the specified database and sets the necessary associations in the database.
     *
     * @param database the database for which the preprocessing is performed
     * @param verbose  flag to allow verbose messages while performing the
     *                 algorithm
     * @param time     flag to request output of performance time
     */
    public void run(Database<V> database, boolean verbose, boolean time) {
        if (database == null || database.size() <= 0) {
            throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
        }

        long start = System.currentTimeMillis();
        FiniteProgress progress = new FiniteProgress("Preprocessing correlation dimension", database.size());
        if (logger.isVerbose()) {
          logger.verbose("Preprocessing:");
        }

        int processed = 1;
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer id = it.next();
            List<DistanceResultPair<DoubleDistance>> objs = resultsForPCA(id, database, verbose, false);

            PCAFilteredResult pcares = pca.processQueryResult(objs, database);

            database.associate(AssociationID.LOCAL_PCA, id, pcares);
            database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pcares.similarityMatrix());
            progress.setProcessed(processed++);

            if (logger.isVerbose()) {
              logger.progress(progress);
            }
        }

        long end = System.currentTimeMillis();
        if (time) {
            long elapsedTime = end - start;
            logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
        }
    }

    /**
     * Sets the values for the parameters alpha, pca and pcaDistancefunction if
     * specified. If the parameters are not specified default values are set.
     *
     */
    @Override
    public List<String> setParameters(List<String> args) throws ParameterException {
        List<String> remainingParameters = super.setParameters(args);

        pcaDistanceFunction = PCA_DISTANCE_PARAM.instantiateClass();
        addParameterizable(pcaDistanceFunction);
        remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);
        
        remainingParameters = pca.setParameters(remainingParameters);

        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns the ids of the objects stored in the specified database to be
     * considered within the PCA for the specified object id.
     *
     * @param id       the id of the object for which a PCA should be performed
     * @param database the database holding the objects
     * @param verbose  flag to allow verbose messages while performing the
     *                 algorithm
     * @param time     flag to request output of performance time
     * @return the list of the object ids to be considered within the PCA
     */
    protected abstract List<Integer> objectIDsForPCA(Integer id, Database<V> database, boolean verbose, boolean time);

    /**
     * Returns the ids of the objects and distances stored in the specified
     * database to be considered within the PCA for the specified object id.
     *
     * @param id       the id of the object for which a PCA should be performed
     * @param database the database holding the objects
     * @param verbose  flag to allow verbose messages while performing the
     *                 algorithm
     * @param time     flag to request output of performance time
     * @return the list of the object ids to be considered within the PCA
     */
    protected abstract List<DistanceResultPair<DoubleDistance>> resultsForPCA(Integer id, Database<V> database, boolean verbose, boolean time);
}
