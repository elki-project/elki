package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.*;
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 * 
 * @author Arthur Zimek 
 */
public class FourCPreprocessor<D extends Distance<D>, V extends RealVector<V,? extends Number>> extends ProjectedDBSCANPreprocessor<D,V> {

	/**
	 * Flag for marking parameter delta as an absolute value.
	 */
	public static final String ABSOLUTE_F = LimitEigenPairFilter.ABSOLUTE_F;

	/**
	 * Description for flag abs.
	 */
	public static final String ABSOLUTE_D = LimitEigenPairFilter.ABSOLUTE_D;

	/**
	 * Option string for parameter delta.
	 */
	public static final String DELTA_P = LimitEigenPairFilter.DELTA_P;

	/**
	 * Description for parameter delta.
	 */
	public static final String DELTA_D = LimitEigenPairFilter.DELTA_D;
	
	/**
	 * The default value for delta.
	 */
	public static final double DEFAULT_DELTA = LimitEigenPairFilter.DEFAULT_DELTA;

	/**
	 * Threshold for strong eigenpairs, can be absolute or relative.
	 */
	private double delta;

	/**
	 * Indicates wether delta is an absolute or a relative value.
	 */
	private boolean absolute;

	/**
	 * The parameter settings for the PCA.
	 */
	private String[] pcaParameters;

	public FourCPreprocessor() {
		super();

		// Parameter delta
		// parameter constraint are only valid if delta is a relative value!
		// Thus they are
		// dependent on the absolute flag, that is they are global constraints!
		DoubleParameter delta = new DoubleParameter(DELTA_P, DELTA_D);
		delta.setDefaultValue(DEFAULT_DELTA);
		optionHandler.put(DELTA_P, delta);

		final ArrayList<ParameterConstraint<Number>> deltaCons = new ArrayList<ParameterConstraint<Number>>();
		deltaCons.add(new GreaterEqualConstraint(0));
		deltaCons.add(new LessEqualConstraint(1));

		// flag absolute
		Flag abs = new Flag(ABSOLUTE_F, ABSOLUTE_D);
		optionHandler.put(ABSOLUTE_F, abs);
		
		GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint(delta,deltaCons,abs,false);
		optionHandler.setGlobalParameterConstraint(gpc);
	}

	/**
	 * This method implements the type of variance analysis to be computed for a
	 * given point. <p/> Example1: for 4C, this method should implement a PCA
	 * for the given point. Example2: for PreDeCon, this method should implement
	 * a simple axis-parallel variance analysis.
	 * 
	 * @param id
	 *            the given point
	 * @param neighbors
	 *            the neighbors as query results of the given point
	 * @param database
	 *            the database for which the preprocessing is performed
	 */
	protected void runVarianceAnalysis(Integer id, List<QueryResult<D>> neighbors, Database<V> database) {
		LinearLocalPCA<V> pca = new LinearLocalPCA<V>();
		try {
			pca.setParameters(pcaParameters);
		} catch (ParameterException e) {
			// tested before
			throw new RuntimeException("This should never happen!");
		}

		List<Integer> ids = new ArrayList<Integer>(neighbors.size());
		for (QueryResult<D> neighbor : neighbors) {
			ids.add(neighbor.getID());
		}
		pca.run(ids, database);

		if (this.debug) {
			StringBuffer msg = new StringBuffer();
			msg.append("\n").append(id).append(" ").append(database.getAssociation(AssociationID.LABEL, id));
			msg.append("\ncorrDim ").append(pca.getCorrelationDimension());
			debugFine(msg.toString());
		}
		database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, pca.getCorrelationDimension());
		database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.similarityMatrix());
	}

	/**
	 * Sets the values for the parameters alpha, pca and pcaDistancefunction if
	 * specified. If the parameters are not specified default values are set.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// absolute
		absolute = optionHandler.isSet(ABSOLUTE_F);

		// delta

		/*
		 * Daran denken: ich kann auch abfragen, ob der default wert gesetzt
		 * wurde!! somit kann ich also auf den flag 'absolute' reagieren...
		 * Trotzdem ist die abfrage irgendwie seltsam...
		 */
		delta = (Double) optionHandler.getOptionValue(DELTA_P);
		if (absolute && ((Parameter) optionHandler.getOption(DELTA_P)).tookDefaultValue()) {
			throw new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_F + " is set, " + "but no value for "
					+ DELTA_P + " is specified.");
		}
//		if (optionHandler.isSet(DELTA_P)) {
//			delta = (Double) optionHandler.getOptionValue(DELTA_P);
//			try {
//				if (!absolute && delta < 0 || delta > 1)
//					throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D);
//			} catch (NumberFormatException e) {
//				throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D, e);
//			}
//		} else if (!absolute) {
//			delta = LimitEigenPairFilter.DEFAULT_DELTA;
//		} else {
//			throw new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_F + " is set, " + "but no value for " + DELTA_P + " is specified.");
//		}

		LinearLocalPCA<V> tmpPCA = new LinearLocalPCA<V>();
		// save parameters for pca
		List<String> tmpPCAParameters = new ArrayList<String>();
		// eigen pair filter
		tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LinearLocalPCA.EIGENPAIR_FILTER_P);
		tmpPCAParameters.add(LimitEigenPairFilter.class.getName());
		// abs
		if (absolute) {
			tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LimitEigenPairFilter.ABSOLUTE_F);
		}
		// delta
		tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LimitEigenPairFilter.DELTA_P);
		tmpPCAParameters.add(Double.toString(delta));

		// big value
		tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LinearLocalPCA.BIG_VALUE_P);
		tmpPCAParameters.add("50");
		// small value
		tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LinearLocalPCA.SMALL_VALUE_P);
		tmpPCAParameters.add("1");

		pcaParameters = tmpPCAParameters.toArray(new String[tmpPCAParameters.size()]);
		tmpPCA.setParameters(pcaParameters);

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

		LinearLocalPCA<V> pca = new LinearLocalPCA<V>();
		try {
			pca.setParameters(pcaParameters);
		} catch (ParameterException e) {
			// tested before
			throw new RuntimeException("This should never happen!");
		}
		attributeSettings.addAll(pca.getAttributeSettings());

		return attributeSettings;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
	 */
	public String description() {
		StringBuffer description = new StringBuffer();
		description.append(FourCPreprocessor.class.getName());
		description.append(" computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n");
		description.append("The PCA is based on epsilon range queries.\n");
		description.append(optionHandler.usage("", false));
		return description.toString();
	}

}