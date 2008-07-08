package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.SODModel;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.SODResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.KNNList;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * todo arthur comment
 *
 * @author Arthur Zimek
 * @param <<V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * todo parameter
 */
public class SOD<V extends RealVector<V, Double>, D extends Distance<D>> extends AbstractAlgorithm<V> {

  /**
   * The association id to associate a subspace outlier degree.
   */
  @SuppressWarnings("unchecked")
  public static final AssociationID<SODModel> SOD_MODEL = AssociationID.getOrCreateAssociationID("SOD", SODModel.class);

  
    /**
     * Parameter to indicate the number of shared nearest neighbors to be considered for learning the subspace properties.
     * <p/>
     * <p>Default value: 1</p>
     * <p/>
     * <p>Key: {@code -knn}</p>
     */
    public final IntParameter KNN_PARAM = new IntParameter("knn", "the number of shared nearest neighbors to be considered for learning the subspace properties", new GreaterConstraint(0));

    /**
     * Parameter to indicate the multiplier for the discriminance value for discerning small from large variances.
     * <p/>
     * <p>Default value: 1.1</p>
     * <p/>
     * <p>Key: {@code -alpha}</p>
     */
    public final DoubleParameter ALPHA_PARAM = new DoubleParameter("alpha", "multiplier for the discriminance value for discerning small from large variances", new GreaterConstraint(0));


    /**
     * Holds the number of shared nearest neighbors to be considered for learning the subspace properties.
     */
    private int knn;

    private SharedNearestNeighborSimilarityFunction<V, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<V, D>();

    /**
     * Hold the alpha-value for discerning small from large variances.
     */
    private double alpha;

    private SODResult<V> sodResult;

    public SOD() {
        super();
        KNN_PARAM.setDefaultValue(1);
        ALPHA_PARAM.setDefaultValue(1.1);
        ALPHA_PARAM.setOptional(true);
        addOption(KNN_PARAM);
        addOption(ALPHA_PARAM);
    }

    @Override
    public String description() {
        StringBuilder description = new StringBuilder();
        description.append(super.description());
        description.append(Description.NEWLINE);
        description.append(similarityFunction.inlineDescription());
        description.append(Description.NEWLINE);
        return description.toString();
    }

    @Override
    protected void runInTime(Database<V> database) throws IllegalStateException {
        Progress progress = new Progress("assigning SOD", database.size());
        int processed = 0;
        similarityFunction.setDatabase(database, isVerbose(), isTime());
        if (isVerbose()) {
            verbose("assigning subspace outlier degree:");
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer queryObject = iter.next();
            processed++;
            if (isVerbose()) {
                progress.setProcessed(processed);
                progress(progress);
            }
            List<Integer> knnList = getKNN(database, queryObject).idsToList();
            SODModel<V> model = new SODModel<V>(database, knnList, alpha, database.get(queryObject));
            database.associate(SOD_MODEL, queryObject, model);
        }
        if (isVerbose()) {
            verbose("");
        }
        sodResult = new SODResult<V>(database);
    }

    /**
     * Provides the k nearest neighbors in terms of the shared nearest neighbor distance.
     * <p/>
     * The query object is excluded from the knn list.
     *
     * @param database    the database holding the objects
     * @param queryObject the query object for which the kNNs should be determined
     * @return the k nearest neighbors in terms of the shared nearest neighbor distance without the query object
     */
    private KNNList<DoubleDistance> getKNN(Database<V> database, Integer queryObject) {
        similarityFunction.getPreprocessor().getParameters();
        KNNList<DoubleDistance> kNearestNeighbors = new KNNList<DoubleDistance>(knn, new DoubleDistance(Double.POSITIVE_INFINITY));
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            if (!id.equals(queryObject)) {
                DoubleDistance distance = new DoubleDistance(1.0 / similarityFunction.similarity(queryObject, id).getDoubleValue());
                kNearestNeighbors.add(new QueryResult<DoubleDistance>(id, distance));
            }
        }
        return kNearestNeighbors;
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        knn = getParameterValue(KNN_PARAM);
        alpha = getParameterValue(ALPHA_PARAM);
        remainingParameters = similarityFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    public Description getDescription() {
        return new Description("SOD", "Subspace outlier degree", "", "");
    }

    public SODResult<V> getResult() {
        return sodResult;
    }

    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(similarityFunction.getAttributeSettings());
        return attributeSettings;
    }
}
