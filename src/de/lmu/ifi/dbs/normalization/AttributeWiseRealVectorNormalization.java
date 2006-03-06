package de.lmu.ifi.dbs.normalization;


import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Class to perform and undo a normalization on real vectors with respect
 * to given minimum and maximum in each dimension.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class AttributeWiseRealVectorNormalization extends AbstractNormalization<RealVector> {
  /**
   * Parameter for minima.
   */
  public static final String MINIMA_P = "min";

  /**
   * Description for parameter minima.
   */
  public static final String MINIMA_D = "<min_1, ..., min_d>a comma separated concatenation " +
                                        "of the minimum values in each dimension";

  /**
   * Parameter for maxima.
   */
  public static final String MAXIMA_P = "max";

  /**
   * Description for parameter minima.
   */
  public static final String MAXIMA_D = "<max_1, ..., max_d>a comma separated concatenation " +
                                        "of the maximum values in each dimension";

  /**
   * Stores the maximum in each dimension.
   */
  private double[] maxima = new double[0];

  /**
   * Stores the minimum in each dimension.
   */
  private double[] minima = new double[0];

  /**
   * Sets minima and maxima parameter to the optionhandler.
   */
  public AttributeWiseRealVectorNormalization() {
    parameterToDescription.put(MINIMA_P + OptionHandler.EXPECTS_VALUE, MINIMA_D);
    parameterToDescription.put(MAXIMA_P + OptionHandler.EXPECTS_VALUE, MAXIMA_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see Normalization#normalizeObjects(java.util.List)
   */
  public List<ObjectAndAssociations<RealVector>> normalizeObjects(List<ObjectAndAssociations<RealVector>> objectAndAssociationsList) throws NonNumericFeaturesException {
    if (objectAndAssociationsList.size() == 0)
      return new ArrayList<ObjectAndAssociations<RealVector>>();

    if (minima.length == 0 && maxima.length == 0)
      determineMinMax(objectAndAssociationsList);

    int dim = objectAndAssociationsList.get(0).getObject().getDimensionality();
    if (dim != minima.length || dim != maxima.length)
      throw new IllegalArgumentException("Dimensionalities do not agree!");

    try {
      List<ObjectAndAssociations<RealVector>> normalized = new ArrayList<ObjectAndAssociations<RealVector>>();
      for (ObjectAndAssociations<RealVector> objectAndAssociations : objectAndAssociationsList) {
        double[] values = new double[objectAndAssociations.getObject().getDimensionality()];
        for (int d = 1; d <= objectAndAssociations.getObject().getDimensionality(); d++) {
          values[d - 1] = (objectAndAssociations.getObject().getValue(d).doubleValue() - minima[d - 1]) / factor(d);
        }

        RealVector normalizedFeatureVector = objectAndAssociationsList.get(0).getObject().newInstance(values);
        normalizedFeatureVector.setID(objectAndAssociations.getObject().getID());
        Map<AssociationID, Object> associations = objectAndAssociations.getAssociations();
        normalized.add(new ObjectAndAssociations<RealVector>(normalizedFeatureVector, associations));
      }
      return normalized;
    }
    catch (Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.normalization.Normalization#normalize(java.util.List)
   */
  public List<RealVector> normalize(List<RealVector> featureVectors) throws NonNumericFeaturesException {
    if (featureVectors.size() == 0)
      return new ArrayList<RealVector>();

    if (minima.length == 0 && maxima.length == 0)
      determineMinMax(featureVectors.toArray(new RealVector[featureVectors.size()]));

    int dim = featureVectors.get(0).getDimensionality();
    if (dim != minima.length || dim != maxima.length)
      throw new IllegalArgumentException("Dimensionalities do not agree!");

    try {
      List<RealVector> normalized = new ArrayList<RealVector>();
      for (RealVector featureVector : featureVectors) {
        double[] values = new double[featureVector.getDimensionality()];
        for (int d = 1; d <= featureVector.getDimensionality(); d++) {
          values[d - 1] = (featureVector.getValue(d).doubleValue() - minima[d - 1]) / factor(d);
        }
        RealVector normalizedFeatureVector = featureVectors.get(0).newInstance(values);
        normalizedFeatureVector.setID(featureVector.getID());
        normalized.add(normalizedFeatureVector);
      }
      return normalized;
    }
    catch (Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
    }
  }

  /**
   * @see Normalization#restore(de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public RealVector restore(RealVector featureVector) throws NonNumericFeaturesException {
    if (featureVector.getDimensionality() == maxima.length && featureVector.getDimensionality() == minima.length) {
      double[] values = new double[featureVector.getDimensionality()];
      for (int d = 1; d <= featureVector.getDimensionality(); d++) {
        values[d - 1] = (featureVector.getValue(d).doubleValue() * (factor(d)) + minima[d - 1]);
      }
      RealVector restoredFeatureVector = featureVector.newInstance(values);
      restoredFeatureVector.setID(featureVector.getID());
      return restoredFeatureVector;
    }
    else {
      throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: " + featureVector.getDimensionality() + " former dimensionality: " + maxima.length);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.normalization.Normalization#restore(java.util.List)
   */
  public List<RealVector> restore(List<RealVector> featureVectors) throws NonNumericFeaturesException {
    try {
      List<RealVector> restored = new ArrayList<RealVector>();
      for (RealVector featureVector : featureVectors) {
        restored.add(restore(featureVector));
      }
      return restored;
    }
    catch (Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be resized.", e);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.normalization.Normalization#transform(de.lmu.ifi.dbs.linearalgebra.Matrix)
   */
  public Matrix transform(Matrix matrix) throws NonNumericFeaturesException {
    Matrix transformed = new Matrix(matrix.getRowDimension(), matrix.getColumnDimension());
    for (int row = 0; row < matrix.getRowDimension(); row++) {
      double sum = 0.0;
      for (int col = 0; col < matrix.getColumnDimension() - 1; col++) {
        sum += minima[col] * matrix.get(row, col) / factor(col + 1);
        transformed.set(row, col, matrix.get(row, col) / factor(col + 1));
      }
      transformed.set(row, matrix.getColumnDimension() - 1, matrix.get(row, matrix.getColumnDimension() - 1) + sum);
    }
    return transformed;
  }

  /**
   * Returns a string representation of this normalization. The specified prefix pre will be
   * the prefix of each new line. This method is used to write the parameters of
   * a normalization to a result of an algorithm using this normalization.
   *
   * @param pre the prefix of each new line
   * @return a string representation of this normalization
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(pre).append("normalization class: ").append(getClass().getName());
    result.append("\n");
    result.append(pre).append("normalization minima: ").append(Util.format(minima));
    result.append("\n");
    result.append(pre).append("normalization maxima: ").append(Util.format(maxima));

    return result.toString();
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    return optionHandler.usage("", false);
  }

  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the
   * given array that are neither expected nor used by this
   * Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return String[] an array containing the unused parameters
   * @throws IllegalArgumentException in case of wrong parameter-setting
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if (optionHandler.isSet(MINIMA_P) && optionHandler.isSet(MAXIMA_P)) {
      try {
        String min = optionHandler.getOptionValue(MINIMA_P);
        minima = Util.parseDoubles(min);
        String max = optionHandler.getOptionValue(MAXIMA_P);
        maxima = Util.parseDoubles(max);
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e);
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(e);
      }

      if (minima.length != maxima.length)
        throw new IllegalArgumentException("minima and maxima parameter " +
                                           "must have the same dimensionality!");
    }

    if (optionHandler.isSet(MINIMA_P) && ! optionHandler.isSet(MAXIMA_P) ||
        ! optionHandler.isSet(MINIMA_P) && optionHandler.isSet(MAXIMA_P))
      throw new IllegalArgumentException("minama AND maxima parametrs have to be set!");

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(MINIMA_P, Util.format(minima));
    attributeSettings.addSetting(MAXIMA_P, Util.format(maxima));

    return result;
  }

  /**
   * Returns a factor for normalization in a certain dimension.
   * <p/>
   * The provided factor is the maximum-minimum in the specified dimension, if these two values differ,
   * otherwise it is the maximum if this value differs from 0,
   * otherwise it is 1.
   *
   * @param dimension the dimension to get a factor for normalization
   * @return a factor for normalization in a certain dimension
   */
  private double factor(int dimension) {
    return maxima[dimension - 1] != minima[dimension - 1] ?
           maxima[dimension - 1] - minima[dimension - 1] :
           maxima[dimension - 1] != 0 ? maxima[dimension - 1] : 1;
  }

  /**
   * Determines the minima and maxima values in each dimension of the given featureVectors.
   *
   * @param featureVectors the list of feature vectors
   */
  private void determineMinMax(RealVector[] featureVectors) {
    if (featureVectors.length == 0) return;
    int dimensionality = featureVectors[0].getDimensionality();
    initMinMax(dimensionality);

    for (RealVector featureVector : featureVectors) {
      updateMinMax(featureVector);
    }
  }

  /**
   * Determines the minima and maxima values in each dimension of the given featureVectors.
   *
   * @param objectAndAssociationsList the list of feature vectors and their associtions
   */
  private void determineMinMax(List<ObjectAndAssociations<RealVector>> objectAndAssociationsList) {
    if (objectAndAssociationsList.isEmpty()) return;
    int dimensionality = objectAndAssociationsList.get(0).getObject().getDimensionality();
    initMinMax(dimensionality);

    for (ObjectAndAssociations<RealVector> objectAndAssociations : objectAndAssociationsList) {
      updateMinMax(objectAndAssociations.getObject());
    }
  }

  /**
   * Initializes the min and max array.
   *
   * @param dimensionality the dimensionality of the feature vectors to be normalized
   */
  private void initMinMax(int dimensionality) {
    minima = new double[dimensionality];
    maxima = new double[dimensionality];
    for (int i = 0; i < dimensionality; i++) {
      maxima[i] = - Double.MAX_VALUE;
      minima[i] = Double.MAX_VALUE;
    }
  }

  /**
   * Updates the min and max array according to the specified feature vector.
   * @param featureVector the feature vector
   */
  private void updateMinMax(RealVector featureVector) {
    if (minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors differ in length.");
    }
    for (int d = 1; d <= featureVector.getDimensionality(); d++) {
      if ((featureVector.getValue(d).doubleValue()) > maxima[d - 1]) {
        maxima[d - 1] = (featureVector.getValue(d).doubleValue());
      }
      if ((featureVector.getValue(d).doubleValue()) < minima[d - 1]) {
        minima[d - 1] = (featureVector.getValue(d).doubleValue());
      }
    }
  }
}
