package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the HiCO correlation dimension of objects of a certain database. The
 * PCA is based on k nearest neighbor queries.
 *
 * @author Elke Achtert
 */
public class KnnQueryBasedHiCOPreprocessor<V extends RealVector<V, ?>> extends HiCOPreprocessor<V> {

    /**
     * Optional parameter to specify the number of nearest neighbors considered in the PCA,
     * must be an integer greater than 0. If this parameter is not set, k ist set to three
     * times of the dimensionality of the database objects.
     * <p>Key: {@code -hicopreprocessor.k} </p>
     * <p>Default value: three times of the dimensionality of the database objects </p>
     */
    private final IntParameter K_PARAM = new IntParameter(OptionID.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0), true);

    /**
     * Holds the value of parameter k.
     */
    private Integer k;

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database based on a k nearest neighbor query.
     */
    public KnnQueryBasedHiCOPreprocessor() {
        super();
        optionHandler.put(K_PARAM);
    }

    /**
     * @see HiCOPreprocessor#objectIDsForPCA(Integer,Database,boolean,boolean)
     */
    protected List<Integer> objectIDsForPCA(Integer id, Database<V> database, boolean verbose, boolean time) {
        if (k == null) {
            V obj = database.get(id);
            k = 3 * obj.getDimensionality();
        }

        pcaDistanceFunction.setDatabase(database, verbose, time);
        List<QueryResult<DoubleDistance>> knns = database.kNNQueryForID(id, k, pcaDistanceFunction);

        List<Integer> ids = new ArrayList<Integer>(knns.size());
        for (QueryResult<DoubleDistance> knn : knns) {
            ids.add(knn.getID());
        }

        return ids;
    }

    /**
     * Sets the value for the parameter k. If k is not specified, the default
     * value is used.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        if (optionHandler.isSet(K_PARAM)) {
            k = getParameterValue(K_PARAM);
        }

        return remainingParameters;
    }

    /**
     * Returns a description of the class and the required parameters. <p/> This
     * description should be suitable for a usage description.
     *
     * @return String a description of the class and the required parameters
     */
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(KnnQueryBasedHiCOPreprocessor.class.getName());
        description.append(" computes the correlation dimension of objects of a certain database.\n");
        description.append("The PCA is based on k nearest neighbor queries.\n");
        description.append(optionHandler.usage("", false));
        return description.toString();
    }
}
