package de.lmu.ifi.dbs.elki.normalization;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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
  // TODO: support for this was removed below.
  // Instead the user can just give DummyNormalization.class.getName(), right?
  //public final static String NO_NORMALIZATION = "noNorm";

  /**
   * Option ID for normalizations
   */
  public final OptionID NORMALIZATION_ID = OptionID.getOrCreateOptionID("normalizations", "A comma separated list of normalizations for each representation. " +
  		"If in one representation no normalization is desired, please use the class '" + DummyNormalization.class.getName() + "' in the list.");

  /**
   * Normalization class parameter
   */
  private final ClassListParameter<Normalization<O>> NORMALIZATION_PARAM =
    new ClassListParameter<Normalization<O>>(
      NORMALIZATION_ID,
      Normalization.class);
  
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
    // The default value will be initialized on-demand, since we don't know
    // the number of representations beforehand.
    addOption(NORMALIZATION_PARAM);
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
    if (normalizations == null)
      initDefaultNormalizations(numberOfRepresentations);

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

  public List<MultiRepresentedObject<O>> normalize(
  List<MultiRepresentedObject<O>> featureVectors)
  throws NonNumericFeaturesException {
    if (featureVectors.size() == 0)
      return new ArrayList<MultiRepresentedObject<O>>();

    // number of representations
    int numberOfRepresentations = normalizations != null ? normalizations
    .size() : featureVectors.get(0).getNumberOfRepresentations();

    if (normalizations == null)
      initDefaultNormalizations(numberOfRepresentations);

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
   * Init default normalizations for a given number of representations.
   * 
   * @param numberOfRepresentations
   */
  private void initDefaultNormalizations(int numberOfRepresentations) {
    normalizations = new ArrayList<Normalization<O>>(
    numberOfRepresentations);
    for (int r = 0; r < numberOfRepresentations; r++) {
      try {
        Normalization<O> norm = Util.instantiateGenerics(Normalization.class,
            DEFAULT_NORMALIZATION);
        normalizations.add(norm);
      }
      catch (UnableToComplyException e) {
        throw new RuntimeException("This should never happen!");
      }
    }
  }

  /**
   * Transforms a set of feature vectors to the original attribute ranges.
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
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * all instances of {@link #normalizations}.
     */
    @Override
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
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingOptions = super.setParameters(args);

    // normalizations
    if (NORMALIZATION_PARAM.isSet()) {
      // FIXME: add support back for NO_NORMALIZATION keyword?
      // Right now, the user needs to specify DummyNormalization.class.getName()
      this.normalizations = NORMALIZATION_PARAM.instantiateClasses();
    }

    return remainingOptions;
  }
}