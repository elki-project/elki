package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;

import java.util.List;

/**
 * Dummy normalization that does nothing. This class is used at normalization of multi-represented objects
 * if one representation needs no normalization.
 *
 * @author Elke Achtert 
 */
public class DummyNormalization<O extends DatabaseObject> extends AbstractParameterizable implements Normalization<O> {

  /**
   * @return the specified objectAndAssociationsList
   * @see Normalization#normalizeObjects(java.util.List)
   */
  public List<ObjectAndAssociations<O>> normalizeObjects(List<ObjectAndAssociations<O>> objectAndAssociationsList) throws NonNumericFeaturesException {
    return objectAndAssociationsList;
  }

  /**
   * @return the specified featureVectors
   * @see Normalization#normalize(java.util.List)
   */
  public List<O> normalize(List<O> featureVectors) throws NonNumericFeaturesException {
    return featureVectors;
  }

  /**
   * @return the specified featureVectors
   * @see Normalization#restore(java.util.List)
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
   * @return the specified linear equation system
   * @see Normalization#transform(de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem)
   */
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) throws NonNumericFeaturesException {
    return linearEquationSystem;
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
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toString() {
    return this.getClass().getName();
  }
}
