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
 * @author Elke Achtert
 */
public abstract class HiCOPreprocessor<V extends RealVector<V, ?>> extends AbstractParameterizable implements Preprocessor<V> {
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
    protected DistanceFunction<V, DoubleDistance> pcaDistanceFunction;

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    public HiCOPreprocessor() {
        super();
        // parameter pca-class
        ClassParameter<LocalPCA<V>> pcaClass = new ClassParameter(PCA_CLASS_P, PCA_CLASS_D, LocalPCA.class);
        pcaClass.setDefaultValue(DEFAULT_PCA_CLASS);
        optionHandler.put(pcaClass);

        // parameter pca distance function
        ClassParameter<DistanceFunction<V, DoubleDistance>> pcaDist = new ClassParameter(PCA_DISTANCE_FUNCTION_P, PCA_DISTANCE_FUNCTION_D, DistanceFunction.class);
        pcaDist.setDefaultValue(DEFAULT_PCA_DISTANCE_FUNCTION);
        optionHandler.put(pcaDist);
    }

    /**
     * This method determines the correlation dimensions of the objects stored
     * in the specified database and sets the necessary associations in the
     * database.
     *
     * @param database the database for which the preprocessing is performed
     * @param verbose  flag to allow verbose messages while performing the algorithm
     * @param time     flag to request output of performance time
     */
    public void run(Database<V> database, boolean verbose, boolean time) {
        if (database == null) {
            throw new IllegalArgumentException("Database must not be null!");
        }

        try {
            long start = System.currentTimeMillis();
            Progress progress = new Progress("Preprocessing correlation dimension", database.size());
            if (verbose) {
                verbose("Preprocessing:");
            }

            int processed = 1;
            for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
                Integer id = it.next();
                List<Integer> ids = objectIDsForPCA(id, database, verbose, false);

                //noinspection unchecked
                // todo???
                LocalPCA<V> pca = Util.instantiate(LocalPCA.class, pcaClassName);
                pca.setParameters(pcaParameters);
                pca.run(ids, database);

                database.associate(AssociationID.LOCAL_PCA, id, pca);
                database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.similarityMatrix());
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
        catch (ParameterException e) {
            // tested before
            throw new RuntimeException("This should never happen!", e);
        }
        catch (UnableToComplyException e) {
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
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // pca distance function
        String pcaDistanceFunctionClassName = (String) optionHandler.getOptionValue(PCA_DISTANCE_FUNCTION_P);
        try {
            // noinspection unchecked
            // todo
            pcaDistanceFunction = Util.instantiate(DistanceFunction.class, pcaDistanceFunctionClassName);
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(PCA_DISTANCE_FUNCTION_P, pcaDistanceFunctionClassName, PCA_DISTANCE_FUNCTION_D);
        }
        remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);

        // pca
        LocalPCA<V> tmpPCA;
        pcaClassName = (String) optionHandler.getOptionValue(PCA_CLASS_P);
        try {
//    	 noinspection unchecked
            // todo
            tmpPCA = Util.instantiate(LocalPCA.class, pcaClassName);
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(PCA_CLASS_P, pcaClassName, PCA_CLASS_D);
        }

        // save parameters for pca
        String[] tmpPCAParameters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, tmpPCAParameters, 0, remainingParameters.length);
        // eigen pair filter
        Util.addParameter(tmpPCAParameters, OptionID.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class.getName());
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
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        try {
//    	 noinspection unchecked
            // todo???
            LocalPCA<V> pca = Util.instantiate(LocalPCA.class, pcaClassName);
            pca.setParameters(pcaParameters);
            attributeSettings.addAll(pca.getAttributeSettings());
        }
        catch (UnableToComplyException e) {
            // tested before!!! TODO more meaningful message
            throw new RuntimeException("This should never happen!");
        }
        catch (ParameterException e) {
            // tested before!!! TODO more meaningful message
            throw new RuntimeException("This should never happen!");
        }

        return attributeSettings;
    }

    /**
     * Returns the ids of the objects stored in the specified database to be
     * considerd within the PCA for the specified object id.
     *
     * @param id       the id of the object for which a PCA should be performed
     * @param database the database holding the objects
     * @param verbose  flag to allow verbose messages while performing the algorithm
     * @param time     flag to request output of performance time
     * @return the list of the object ids to be considerd within the PCA
     */
    protected abstract List<Integer> objectIDsForPCA(Integer id, Database<V> database, boolean verbose, boolean time);
}
