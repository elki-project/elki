package de.lmu.ifi.dbs.algorithm.classifier;


import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

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
public abstract class DistanceBasedClassifier<O extends DatabaseObject, D extends Distance<D>, L extends ClassLabel<L>>
    extends AbstractClassifier<O, L> {

    /**
     * Parameter to specify the distance function to determine the distance between database objects,
     * must extend {@link de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction}.
     * <p>Key: {@code -classifier.distancefunction} </p>
     * <p>Default value: {@link de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction} </p>
     */
    private final ClassParameter<DistanceFunction> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<DistanceFunction>(OptionID.CLASSIFIER_DISTANCEFUNCTION,
            DistanceFunction.class, EuklideanDistanceFunction.class.getName());

    /**
     * The distance function.
     */
    private DistanceFunction<O, D> distanceFunction;


    /**
     * Adds parameter for distance function to parameter map.
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
    public int classify(O instance) throws IllegalStateException {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(distanceFunction.getAttributeSettings());
        return attributeSettings;
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
     * @see AbstractClassifier#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // distance function
        String distancefunctionClass = getParameterValue(DISTANCE_FUNCTION_PARAM);
        try {
            //noinspection unchecked
            distanceFunction = Util.instantiate(DistanceFunction.class, distancefunctionClass);
        }
        catch (UnableToComplyException e) {
            throw new WrongParameterValueException(DISTANCE_FUNCTION_PARAM.getName(),
                distancefunctionClass, DISTANCE_FUNCTION_PARAM.getDescription(), e);
        }
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }


}
