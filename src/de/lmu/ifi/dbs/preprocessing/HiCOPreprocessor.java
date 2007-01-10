package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;
import de.lmu.ifi.dbs.varianceanalysis.PercentageEigenPairFilter;

import java.util.Iterator;
import java.util.List;

/**
 * Abstract superclass for preprocessors for HiCO correlation dimension
 * assignment to objects of a certain database.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class HiCOPreprocessor extends AbstractParameterizable implements Preprocessor<RealVector> {
	/**
	 * The default PCA class name.
	 */
	public static final String DEFAULT_PCA_CLASS = LinearLocalPCA.class.getName();

	/**
	 * Parameter for PCA.
	 */
	public static final String PCA_CLASS_P = "pca";

	/**
	 * Description for parameter pca.
	 */
	public static final String PCA_CLASS_D = "the pca to determine the strong eigenvectors "
			+ Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(LocalPCA.class) + ". Default: " + DEFAULT_PCA_CLASS;

	/**
	 * The default distance function for the PCA.
	 */
	public static final String DEFAULT_PCA_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

	/**
	 * Parameter for pca distance function.
	 */
	public static final String PCA_DISTANCE_FUNCTION_P = "pcaDistancefunction";

	/**
	 * Description for parameter pca distance function.
	 */
	public static final String PCA_DISTANCE_FUNCTION_D = "the distance function for the PCA to determine the distance between database objects "
			+ Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + ". Default: " + DEFAULT_PCA_DISTANCE_FUNCTION;

	/**
	 * The classname of the PCA to determine the strong eigenvectors.
	 */
	protected String pcaClassName;

	/**
	 * The parameter settings for the PCA.
	 */
	private String[] pcaParameters;

	/**
	 * The distance function for the PCA.
	 */
	protected DistanceFunction<RealVector, DoubleDistance> pcaDistanceFunction;

	/**
	 * Provides a new Preprocessor that computes the correlation dimension of
	 * objects of a certain database.
	 */
	public HiCOPreprocessor() {
		super();
		// parameter pca-class
		ClassParameter pcaClass = new ClassParameter(PCA_CLASS_P, PCA_CLASS_D, LocalPCA.class);
		pcaClass.setDefaultValue(DEFAULT_PCA_CLASS);
		optionHandler.put(PCA_CLASS_P, pcaClass);

		// parameter pca distance function
		ClassParameter pcaDist = new ClassParameter(PCA_DISTANCE_FUNCTION_P, PCA_DISTANCE_FUNCTION_D, DistanceFunction.class);
		pcaDist.setDefaultValue(DEFAULT_PCA_DISTANCE_FUNCTION);
		optionHandler.put(PCA_DISTANCE_FUNCTION_P, pcaDist);
	}

	/**
	 * This method determines the correlation dimensions of the objects stored
	 * in the specified database and sets the necessary associations in the
	 * database.
	 * 
	 * @param database
	 *            the database for which the preprocessing is performed
	 * @param verbose
	 *            flag to allow verbose messages while performing the algorithm
	 * @param time
	 *            flag to request output of performance time
	 */
	public void run(Database<RealVector> database, boolean verbose, boolean time) {
		if (database == null) {
			throw new IllegalArgumentException("Database must not be null!");
		}

		try {
			long start = System.currentTimeMillis();
			Progress progress = new Progress("Preprocessing correlation dimension", database.size());
			if (verbose) {
				System.out.println("Preprocessing:");
			}
			Iterator<Integer> it = database.iterator();
			int processed = 1;
			while (it.hasNext()) {
				Integer id = it.next();
				List<Integer> ids = objectIDsForPCA(id, database, verbose, false);

				LocalPCA pca = Util.instantiate(LocalPCA.class, pcaClassName);
				pca.setParameters(pcaParameters);
				pca.run(ids, database);

				database.associate(AssociationID.LOCAL_PCA, id, pca);
				database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.similarityMatrix());
				progress.setProcessed(processed++);

				if (verbose) {
					verbose("\r" + progress.toString());
				}
			}

			long end = System.currentTimeMillis();
			if (time) {
				long elapsedTime = end - start;
				System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
			}
		} catch (ParameterException e) {
			// tested before
			throw new RuntimeException("This should never happen!", e);
		} catch (UnableToComplyException e) {
			// tested before
			throw new RuntimeException("This should never happen!", e);
		}
	}

	/**
	 * Sets the values for the parameters alpha, pca and pcaDistancefunction if
	 * specified. If the parameters are not specified default values are set.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	@SuppressWarnings("unchecked")
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// pca distance function
		String pcaDistanceFunctionClassName = (String) optionHandler.getOptionValue(PCA_DISTANCE_FUNCTION_P);
		try {
			// noinspection unchecked
			pcaDistanceFunction = Util.instantiate(DistanceFunction.class, pcaDistanceFunctionClassName);
		} catch (UnableToComplyException e) {
			throw new WrongParameterValueException(PCA_DISTANCE_FUNCTION_P, pcaDistanceFunctionClassName, PCA_DISTANCE_FUNCTION_D);
		}
		remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);

		// pca
		LocalPCA tmpPCA;
		pcaClassName = (String) optionHandler.getOptionValue(PCA_CLASS_P);
		try {
			tmpPCA = Util.instantiate(LocalPCA.class, pcaClassName);
		} catch (UnableToComplyException e) {
			throw new WrongParameterValueException(PCA_CLASS_P, pcaClassName, PCA_CLASS_D);
		}

		// save parameters for pca
		String[] tmpPCAParameters = new String[remainingParameters.length + 2];
		System.arraycopy(remainingParameters, 0, tmpPCAParameters, 2, remainingParameters.length);
		// eigen pair filter
		tmpPCAParameters[0] = OptionHandler.OPTION_PREFIX + LinearLocalPCA.EIGENPAIR_FILTER_P;
		tmpPCAParameters[1] = PercentageEigenPairFilter.class.getName();

		remainingParameters = tmpPCA.setParameters(tmpPCAParameters);
		pcaParameters = tmpPCA.getParameters();

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
		mySettings.addSetting(PCA_CLASS_P, pcaClassName);
		mySettings.addSetting(PCA_DISTANCE_FUNCTION_P, pcaDistanceFunction.getClass().getName());

		try {
			LocalPCA pca = Util.instantiate(LocalPCA.class, pcaClassName);
			pca.setParameters(pcaParameters);
			attributeSettings.addAll(pca.getAttributeSettings());
		} catch (UnableToComplyException e) {
			// tested before!!!
			throw new RuntimeException("This should never happen!");
		} catch (ParameterException e) {
			// tested before!!!
			throw new RuntimeException("This should never happen!");
		}

		return attributeSettings;
	}

	/**
	 * Returns the ids of the objects stored in the specified database to be
	 * considerd within the PCA for the specified object id.
	 * 
	 * @param id
	 *            the id of the object for which a PCA should be performed
	 * @param database
	 *            the database holding the objects
	 * @param verbose
	 *            flag to allow verbose messages while performing the algorithm
	 * @param time
	 *            flag to request output of performance time
	 * @return the list of the object ids to be considerd within the PCA
	 */
	protected abstract List<Integer> objectIDsForPCA(Integer id, Database<RealVector> database, boolean verbose, boolean time);
}
