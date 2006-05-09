package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.utilities.Util;
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
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

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
   * Sets the difference of the first array minus the second array
   * as the currently set parameter array.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part) {
    currentParameterArray = Util.parameterDifference(complete, part);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();
    attributeSettings.add(new AttributeSettings(this));
    return attributeSettings;
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
