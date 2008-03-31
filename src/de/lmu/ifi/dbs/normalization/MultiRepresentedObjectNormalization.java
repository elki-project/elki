package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.database.Associations;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Class to perform and undo a normalization on multi-represented objects with
 * respect to given normalizations for each representation.
 *
 * @author Elke Achtert 
 */
public class MultiRepresentedObjectNormalization<O extends DatabaseObject>
extends AbstractNormalization<MultiRepresentedObject<O>> {
  /**
   * Default normalization.
   */
  public final static String DEFAULT_NORMALIZATION = AttributeWiseRealVectorNormalization.class
  .getName();

  /**
   * Keyword for no normalization.
   */
  public final static String NO_NORMALIZATION = "noNorm";

  /**
   * Label for parameter normalizations.
   */
  public final static String NORMALIZATION_P = "normalizations";

  /**
   * Description of parameter parser.
   */
  public final static String NORMALIZATION_D = "A comma separated list of normalizations for each representation " +
                                               Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Normalization.class) +
                                               ". Default: " + DEFAULT_NORMALIZATION +
                                               ". If in one representation no normalization is desired, please use the keyword '" +
                                               NO_NORMALIZATION + "' in the list.";

  /**
   * A pattern defining a comma.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * The normalizations for each representation.
   */
  private List<Normalization<O>> normalizations;

  /**
   * Sets normalization parameter to the optionhandler.
   */
  public MultiRepresentedObjectNormalization() {
	  
	  // TODO default value
    optionHandler.put(new ClassListParameter(NORMALIZATION_P,NORMALIZATION_D,Normalization.class));
  }

  /**
   * Performs a normalization on a list of database objects and their
   * associations.
   *
   * @param objectAndAssociationsList the list of database objects and their associations
   * @return a list of normalized database objects and their associations
   *         corresponding to the given list
   * @throws NonNumericFeaturesException if feature vectors differ in length or values are not
   *                                     suitable to normalization
   */
  public List<ObjectAndAssociations<MultiRepresentedObject<O>>> normalizeObjects(
  List<ObjectAndAssociations<MultiRepresentedObject<O>>> objectAndAssociationsList)
  throws NonNumericFeaturesException {
    if (objectAndAssociationsList.size() == 0)
      return new ArrayList<ObjectAndAssociations<MultiRepresentedObject<O>>>();

    // number of representations
    int numberOfRepresentations = objectAndAssociationsList.get(0)
    .getObject().getNumberOfRepresentations();

    // init default normalizations
    // must be done here, because at setParameters() the number of
    // representations is unknown
    if (normalizations == null) {
      normalizations = new ArrayList<Normalization<O>>(
      numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        try {
          // noinspection unchecked
          normalizations.add(Util.instantiate(Normalization.class,
                                              DEFAULT_NORMALIZATION));
        }
        catch (UnableToComplyException e) {
          throw new RuntimeException("This should never happen!");
        }
      }
    }

    // normalize each representation
    List<List<O>> objects = new ArrayList<List<O>>();
    for (int r = 0; r < numberOfRepresentations; r++) {
      List<O> objectsInRepresentation = new ArrayList<O>(
      objectAndAssociationsList.size());
      for (ObjectAndAssociations<MultiRepresentedObject<O>> o : objectAndAssociationsList) {
        if (numberOfRepresentations != o.getObject()
        .getNumberOfRepresentations())
          throw new IllegalArgumentException(
          "Number of representations differs!");
        objectsInRepresentation.add(o.getObject().getRepresentation(r));
      }

      Normalization<O> normalization = normalizations.get(r);
      objects.add(normalization.normalize(objectsInRepresentation));
    }

    // build the normalized multi-represented objects
    List<ObjectAndAssociations<MultiRepresentedObject<O>>> normalized = new ArrayList<ObjectAndAssociations<MultiRepresentedObject<O>>>();
    for (int i = 0; i < objectAndAssociationsList.size(); i++) {
      List<O> representations = new ArrayList<O>(numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        representations.add(objects.get(r).get(i));
      }
      MultiRepresentedObject<O> o = new MultiRepresentedObject<O>(
      representations);
      o.setID(objectAndAssociationsList.get(i).getObject().getID());
      Associations associations = objectAndAssociationsList.get(i).getAssociations();
      normalized.add(new ObjectAndAssociations<MultiRepresentedObject<O>>(o, associations));
    }

    return normalized;
  }

  /**
   * @see Normalization#normalize(java.util.List)
   */
  public List<MultiRepresentedObject<O>> normalize(
  List<MultiRepresentedObject<O>> featureVectors)
  throws NonNumericFeaturesException {
    if (featureVectors.size() == 0)
      return new ArrayList<MultiRepresentedObject<O>>();

    // number of representations
    int numberOfRepresentations = normalizations != null ? normalizations
    .size() : featureVectors.get(0).getNumberOfRepresentations();

    // init default normalizations
    if (normalizations == null) {
      normalizations = new ArrayList<Normalization<O>>(
      numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        try {
          // noinspection unchecked
          normalizations.add(Util.instantiate(Normalization.class,
                                              DEFAULT_NORMALIZATION));
        }
        catch (UnableToComplyException e) {
          throw new RuntimeException("This should never happen!");
        }
      }
    }

    // normalize each representation
    List<List<O>> objects = new ArrayList<List<O>>();
    for (int r = 0; r < numberOfRepresentations; r++) {
      List<O> objectsInRepresentation = new ArrayList<O>(featureVectors
      .size());
      for (MultiRepresentedObject<O> o : featureVectors) {
        if (numberOfRepresentations != o.getNumberOfRepresentations())
          throw new IllegalArgumentException(
          "Number of representations differs!");
        objectsInRepresentation.add(o.getRepresentation(r));
      }

      Normalization<O> normalization = normalizations.get(r);
      objects.add(normalization.normalize(objectsInRepresentation));
    }

    // build the normalized multi-represented objects
    List<MultiRepresentedObject<O>> normalized = new ArrayList<MultiRepresentedObject<O>>();
    for (int i = 0; i < featureVectors.size(); i++) {
      List<O> representations = new ArrayList<O>(numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        representations.add(objects.get(r).get(i));
      }
      MultiRepresentedObject<O> o = new MultiRepresentedObject<O>(
      representations);
      o.setID(featureVectors.get(i).getID());
      normalized.add(o);
    }

    return normalized;
  }

  /**
   * Transforms a set of feature vectores to the original attribute ranges.
   *
   * @param featureVectors a set of feature vectors to be transformed into original space
   * @return a set of feature vectors transformed into original space
   *         corresponding to the given feature vectors
   * @throws NonNumericFeaturesException if feature vectors differ in length or are not compatible
   *                                     with values initialized during normalization
   */
  public List<MultiRepresentedObject<O>> restore(
  List<MultiRepresentedObject<O>> featureVectors)
  throws NonNumericFeaturesException {
    List<MultiRepresentedObject<O>> restored = new ArrayList<MultiRepresentedObject<O>>(
    featureVectors.size());

    for (MultiRepresentedObject<O> o : featureVectors) {
      restored.add(restore(o));
    }

    return restored;
  }

  /**
   * Transforms a feature vector to the original attribute ranges.
   *
   * @param featureVector a feature vector to be transformed into original space
   * @return a feature vector transformed into original space corresponding to
   *         the given feature vector
   * @throws NonNumericFeaturesException feature vector is not compatible with values initialized
   *                                     during normalization
   */
  public MultiRepresentedObject<O> restore(
  MultiRepresentedObject<O> featureVector)
  throws NonNumericFeaturesException {
    List<O> restored = new ArrayList<O>();

    int r = featureVector.getNumberOfRepresentations();
    for (int i = 0; i < r; i++) {
      Normalization<O> normalization = normalizations.get(i);
      restored.add(normalization.restore(featureVector
      .getRepresentation(i)));
    }
    MultiRepresentedObject<O> o = new MultiRepresentedObject<O>(restored);
    o.setID(featureVector.getID());
    return o;
  }

  /**
   * @throws UnsupportedOperationException
   * @see Normalization#transform(de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem)
   */
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) throws NonNumericFeaturesException {
    throw new UnsupportedOperationException("Operation not supported!");
  }

  /**
   * Returns a string representation of this normalization. The specified
   * prefix pre will be the prefix of each new line. This method is used to
   * write the parameters of a normalization to a result of an algorithm using
   * this normalization.
   *
   * @param pre the prefix of each new line
   * @return a string representation of this normalization
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    for (Normalization<O> normalization : normalizations) {
      result.append(normalization.toString(pre));
    }

    return result.toString();
  }

  /**
   * Returns the setting of the attributes of the algorithm.
   *
   * @return the setting of the attributes of the algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    for (Normalization<O> normalization : normalizations) {
      result.addAll(normalization.getAttributeSettings());
    }

    return result;
  }

  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the given array
   * that are neither expected nor used by this Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return String[] an array containing the unused parameters
   * @throws IllegalArgumentException in case of wrong parameter-setting
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingOptions = super.setParameters(args);

    // normalizations
    if (optionHandler.isSet(NORMALIZATION_P)) {
      List<String> norm_list= (List<String>)optionHandler.getOptionValue(NORMALIZATION_P);
//      String[] normalizationClasses = SPLIT.split(normalizationsString);
      if (norm_list.isEmpty()) {
        throw new WrongParameterValueException(NORMALIZATION_P,
                                               norm_list.toString(), NORMALIZATION_D);
      }
      this.normalizations = new ArrayList<Normalization<O>>(norm_list.size());
      for (String normalizationClass : norm_list) {
        if (normalizationClass.equals(NO_NORMALIZATION)) {
          this.normalizations.add(new DummyNormalization<O>());
        }
        else {
          try {
            // noinspection unchecked
            Normalization<O> n = Util.instantiate(
            Normalization.class, normalizationClass);
            n.setParameters(args);
            this.normalizations.add(n);
          }
          catch (UnableToComplyException e) {
            throw new WrongParameterValueException(NORMALIZATION_P,
                                                   norm_list.toString(), NORMALIZATION_D, e);
          }
        }
      }
    }
    setParameters(args, remainingOptions);
    return remainingOptions;
  }
}