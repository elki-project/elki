package de.lmu.ifi.dbs.elki.algorithm.classifier;


import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.List;


/**
 * An abstract classifier already based on DistanceBasedAlgorithm
 * making use of settings for time and verbose and DistanceFunction.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
public abstract class DistanceBasedClassifier<O extends DatabaseObject, D extends Distance<D>, L extends ClassLabel>
    extends AbstractClassifier<O, L, Result> {

    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.classifier.DistanceBasedClassifier#DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "classifier.distancefunction",
        "Classname of the distance function to determine the distance between database objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + "."
    );

    /**
     * Parameter to specify the distance function to determine the distance between database objects,
     * must extend {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
     * <p>Key: {@code -classifier.distancefunction} </p>
     * <p>Default value: {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction} </p>
     */
    private final ClassParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<DistanceFunction<O, D>>(
            DISTANCE_FUNCTION_ID,
            DistanceFunction.class,
            EuclideanDistanceFunction.class.getName());

    /**
     * Holds the instance of the distance function specified by {@link #DISTANCE_FUNCTION_PARAM}.
     */
    private DistanceFunction<O, D> distanceFunction;

    /**
     * Adds parameter
     * {@link #DISTANCE_FUNCTION_PARAM},
     * to the option handler additionally to parameters of super class.
     */
    protected DistanceBasedClassifier() {
        super();
        // distance function
        addOption(DISTANCE_FUNCTION_PARAM);
    }

    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label
     * in {@link #labels labels}.
     * <p/>
     * This method returns the index of the maximum probability
     * as provided by {@link #classDistribution(DatabaseObject) classDistribution(M)}.
     * If an extending classifier requires a different classification,
     * it should overwrite this method.
     *
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     *                               or properly trained
     */
    @Override
    public int classify(O instance) throws IllegalStateException {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }

    /**
     * Returns the distanceFunction.
     *
     * @return the distanceFunction
     */
    protected DistanceFunction<O, D> getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Calls the super method
     * and instantiates {@link #distanceFunction} according to the value of parameter
     * {@link #DISTANCE_FUNCTION_PARAM}.
     * The remaining parameters are passed to the {@link #distanceFunction}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // distance function
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #distanceFunction}.
     *
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(distanceFunction.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Calls the super method
     * and appends the parameter description of {@link #distanceFunction} if it is already initialized.
     */
    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(super.parameterDescription());
        description.append(distanceFunction.parameterDescription());
        return description.toString();
    }
}
