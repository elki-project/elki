package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.algorithm.result.MultivariateModel;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredRunner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Derive a multivariate local model, using PCA to decorreltate data. This is
 * not a full model generation like DependencyDerivator, for example it does not
 * compute a linear equation system.
 *
 * @author Erich Schubert
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public class MultivariateModelDerivator<V extends RealVector<V, ?>, D extends Distance<D>> extends DistanceBasedAlgorithm<V, D> {

    /**
     * OptionID for
     * {@link de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator#SAMPLE_SIZE_PARAM}
     */
    public static final OptionID SAMPLE_SIZE_ID = OptionID.getOrCreateOptionID("derivator.sampleSize", "Threshold for the size of the random sample to use. " + "Default value is size of the complete dataset.");

    /**
     * Optional parameter to specify the treshold for the size of the random
     * sample to use, must be an integer greater than 0.
     * <p/>
     * Default value: the size of the complete dataset
     * </p>
     * <p/>
     * Key: {@code -derivator.sampleSize}
     * </p>
     */
    private final IntParameter SAMPLE_SIZE_PARAM = new IntParameter(SAMPLE_SIZE_ID, new GreaterConstraint(0), true);

    /**
     * Holds the value of {@link #SAMPLE_SIZE_PARAM}.
     */
    private Integer sampleSize;

    /**
     * Flag to use random sample (use knn query around centroid, if flag is not
     * set).
     * <p/>
     * Key: {@code -derivator.randomSample}
     * </p>
     */
    private final Flag RANDOM_SAMPLE_FLAG = new Flag(OptionID.DEPENDENCY_DERIVATOR_RANDOM_SAMPLE);

    /**
     * The PCA utility object.
     */
    private PCAFilteredRunner<V> pca = new PCAFilteredRunner<V>();

    /**
     * Holds the solution.
     */
    private MultivariateModel<V> solution;

    /**
     * Provides a dependency derivator, setting parameters alpha and output
     * accuracy additionally to parameters of super class.
     */
    public MultivariateModelDerivator() {
        super();

        // parameter sample size
        addOption(SAMPLE_SIZE_PARAM);

        // random sample
        addOption(RANDOM_SAMPLE_FLAG);
    }

    public Description getDescription() {
        return new Description("MultivariateLocalModel", "Deriving a multivariate model of the data", "Derives a multivariate Gaussian model using PCA to decorrelate data.", "unpublished");
    }

    /**
     * Runs the algorithm on the whole database or on a sample set based on a KNN
     * query.
     *
     * @param db the database
     */
    @Override
    public void runInTime(Database<V> db) throws IllegalStateException {
        if (isVerbose()) {
            verbose("retrieving database objects...");
        }
        Set<Integer> dbIDs = new HashSet<Integer>();
        for (Iterator<Integer> idIter = db.iterator(); idIter.hasNext();) {
            dbIDs.add(idIter.next());
        }
        V centroidDV = Util.centroid(db, dbIDs);
        Set<Integer> ids;
        if (this.sampleSize != null) {
            if (isSet(RANDOM_SAMPLE_FLAG)) {
                ids = db.randomSample(this.sampleSize, 1);
            }
            else {
                List<QueryResult<D>> queryResults = db.kNNQueryForObject(centroidDV, this.sampleSize, this.getDistanceFunction());
                ids = new HashSet<Integer>(this.sampleSize);
                for (QueryResult<D> qr : queryResults) {
                    ids.add(qr.getID());
                }
            }
        }
        else {
            ids = dbIDs;
        }

        this.solution = generateModel(db, ids, centroidDV);
    }

    /**
     * Runs the pca on the given set of IDs. The centroid is computed from the
     * given ids.
     *
     * @param db  the database
     * @param ids the set of ids
     */
    public MultivariateModel<V> generateModel(Database<V> db, Collection<Integer> ids) {
        V centroidDV = Util.centroid(db, ids);
        return generateModel(db, ids, centroidDV);
    }

    /**
     * Runs the pca on the given set of IDs and for the given centroid
     *
     * @param db         the database
     * @param ids        the set of ids
     * @param centroidDV the centroid
     */
    public MultivariateModel<V> generateModel(Database<V> db, Collection<Integer> ids, V centroidDV) {
        if (isVerbose()) {
            verbose("PCA...");
        }

        Vector centroid = centroidDV.getColumnVector();

        PCAFilteredResult pcares = pca.processIds(ids, db);

        solution = new MultivariateModel<V>(db, ids, centroid, pcares);

        if (isVerbose()) {
            StringBuilder log = new StringBuilder();
            log.append('\n');
            verbose(log.toString());

        }
        return solution;
    }

    public MultivariateModel<V> getResult() {
        return solution;
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #SAMPLE_SIZE_PARAM}.
     * The remaining parameters are passed to the {@link #pca}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // sample size
        if (isSet(SAMPLE_SIZE_PARAM)) {
            sampleSize = getParameterValue(SAMPLE_SIZE_PARAM);
        }

        // pca
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
}