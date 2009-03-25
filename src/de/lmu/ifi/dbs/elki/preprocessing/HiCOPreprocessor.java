package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;

/**
 * Abstract superclass for preprocessors for HiCO correlation dimension
 * assignment to objects of a certain database.
 *
 * @author Elke Achtert
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
        if (database == null) {
            throw new IllegalArgumentException("Database must not be null!");
        }

        long start = System.currentTimeMillis();
        Progress progress = new Progress("Preprocessing correlation dimension", database.size());
        if (verbose) {
            verbose("Preprocessing:");
        }

        int processed = 1;
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer id = it.next();
            List<ComparablePair<DoubleDistance, Integer>> objs = resultsForPCA(id, database, verbose, false);

            PCAFilteredResult pcares = pca.processQueryResult(objs, database);

            database.associate(AssociationID.LOCAL_PCA, id, pcares);
            database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pcares.similarityMatrix());
            progress.setProcessed(processed++);

            if (verbose) {
                progress(progress);
            }
        }

        long end = System.currentTimeMillis();
        if (time) {
            long elapsedTime = end - start;
            verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
        }
    }

    /**
     * Sets the values for the parameters alpha, pca and pcaDistancefunction if
     * specified. If the parameters are not specified default values are set.
     *
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        pcaDistanceFunction = PCA_DISTANCE_PARAM.instantiateClass();
        remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);
        remainingParameters = pca.setParameters(remainingParameters);

        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #pca}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(pca.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Returns the ids of the objects stored in the specified database to be
     * considerd within the PCA for the specified object id.
     *
     * @param id       the id of the object for which a PCA should be performed
     * @param database the database holding the objects
     * @param verbose  flag to allow verbose messages while performing the
     *                 algorithm
     * @param time     flag to request output of performance time
     * @return the list of the object ids to be considerd within the PCA
     */
    protected abstract List<Integer> objectIDsForPCA(Integer id, Database<V> database, boolean verbose, boolean time);

    /**
     * Returns the ids of the objects and distances stored in the specified
     * database to be considerd within the PCA for the specified object id.
     *
     * @param id       the id of the object for which a PCA should be performed
     * @param database the database holding the objects
     * @param verbose  flag to allow verbose messages while performing the
     *                 algorithm
     * @param time     flag to request output of performance time
     * @return the list of the object ids to be considerd within the PCA
     */
    protected abstract List<ComparablePair<DoubleDistance, Integer>> resultsForPCA(Integer id, Database<V> database, boolean verbose, boolean time);
}
