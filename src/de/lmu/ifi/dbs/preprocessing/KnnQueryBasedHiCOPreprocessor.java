package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Computes the HiCO correlation dimension of objects of a certain database. The
 * PCA is based on k nearest neighbor queries.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KnnQueryBasedHiCOPreprocessor extends HiCOPreprocessor {

	/**
	 * Option string for parameter k.
	 */
	public static final String K_P = "k";

	/**
	 * Description for parameter k.
	 */
	public static final String K_D = "a positive integer specifying the number of " + "nearest neighbors considered in the PCA. "
			+ "If this value is not defined, k ist set to three " + "times of the dimensionality of the database objects.";

	/**
	 * The number of nearest neighbors considered in the PCA.
	 */
	private Integer k;

	/**
	 * Provides a new Preprocessor that computes the correlation dimension of
	 * objects of a certain database based on a k nearest neighbor query.
	 */
	public KnnQueryBasedHiCOPreprocessor() {
		super();

		IntParameter kPam = new IntParameter(K_P, K_D, new GreaterConstraint(0));
		kPam.setOptional(true);
		optionHandler.put(K_P, kPam);
	}

	/**
	 * @see HiCOPreprocessor#objectIDsForPCA(Integer, Database,boolean, boolean)
	 */
	protected List<Integer> objectIDsForPCA(Integer id, Database<RealVector> database, boolean verbose, boolean time) {
		if (k == null) {
			RealVector obj = database.get(id);
			k = 3 * obj.getDimensionality();
		}

		pcaDistanceFunction.setDatabase(database, verbose, time);
		List<QueryResult<DoubleDistance>> knns = database.kNNQueryForID(id, k, pcaDistanceFunction);

		List<Integer> ids = new ArrayList<Integer>(knns.size());
		for (QueryResult knn : knns) {
			ids.add(knn.getID());
		}

		return ids;
	}

	/**
	 * Sets the value for the parameter k. If k is not specified, the default
	 * value is used.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		if (optionHandler.isSet(K_P)) {
			k = (Integer) optionHandler.getOptionValue(K_P);
		}

		setParameters(args, remainingParameters);
		return remainingParameters;
	}

	/**
	 * Returns the parameter setting of the attributes.
	 * 
	 * @return the parameter setting of the attributes
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> attributeSettings = super.getAttributeSettings();

		AttributeSettings mySettings = attributeSettings.get(0);
		mySettings.addSetting(K_P, Integer.toString(k));

		return attributeSettings;
	}

	/**
	 * Returns a description of the class and the required parameters. <p/> This
	 * description should be suitable for a usage description.
	 * 
	 * @return String a description of the class and the required parameters
	 */
	public String description() {
		StringBuffer description = new StringBuffer();
		description.append(KnnQueryBasedHiCOPreprocessor.class.getName());
		description.append(" computes the correlation dimension of objects of a certain database.\n");
		description.append("The PCA is based on k nearest neighbor queries.\n");
		description.append(optionHandler.usage("", false));
		return description.toString();
	}
}
