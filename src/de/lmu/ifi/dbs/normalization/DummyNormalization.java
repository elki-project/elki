package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy normalization that does nothing. This class is used at normalization of multi-represented objects
 * if one representation needs no normalization.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DummyNormalization<O extends DatabaseObject> implements Normalization<O> {

  /**
   * @return the specified objectAndAssociationsList
   * @see Normalization#normalizeObjects(java.util.List<de.lmu.ifi.dbs.database.ObjectAndAssociations<O>>)
   */
  public List<ObjectAndAssociations<O>> normalizeObjects(List<ObjectAndAssociations<O>> objectAndAssociationsList) throws NonNumericFeaturesException {
    return objectAndAssociationsList;
  }

  /**
   * @return the specified featureVectors
   * @see Normalization#normalize(java.util.List<O>)
   */
  public List<O> normalize(List<O> featureVectors) throws NonNumericFeaturesException {
    return featureVectors;
  }

  /**
   * @return the specified featureVectors
   * @see Normalization#restore(java.util.List<O>)
   */
  public List<O> restore(List<O> featureVectors) throws NonNumericFeaturesException {
    return featureVectors;
  }

  /**
   * @return the specified featureVector
   * @see Normalization#restore(DatabaseObject)
   */
  public O restore(O featureVector) throws NonNumericFeaturesException {
    return featureVector;
  }

  /**
   * @return the specified matrix
   * @see Normalization#transform(de.lmu.ifi.dbs.linearalgebra.Matrix)
   */
  public Matrix transform(Matrix matrix) throws NonNumericFeaturesException {
    return matrix;
  }

  /**
   * @see Normalization#toString(String)
   */
  public String toString(String pre) {
    return pre + toString();
  }

  /**
   * @see de.lmu.ifi.dbs.normalization.Normalization#description()
   */
  public String description() {
    return "Dummy normalization that does nothing. This class is used at normalization of multi-represented " +
           "objects if one representation needs no normalization.";
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    return args;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    return new ArrayList<AttributeSettings>();
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toString() {
    return this.getClass().getName();
  }
}
