package de.lmu.ifi.dbs.elki.normalization;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.AllOrNoneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualSizeGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.output.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to perform and undo a normalization on real vectors with respect to
 * given minimum and maximum in each dimension.
 *
 * @author Elke Achtert 
 */
public class AttributeWiseRealVectorNormalization<V extends RealVector<V, ? >> extends AbstractNormalization<V> {
  /**
   * OptionID for {@link #MINIMA_PARAM}
   */
  public static final OptionID MINIMA_ID = OptionID.getOrCreateOptionID(
      "normalize.min", "a comma separated concatenation "
      + "of the minimum values in each dimension that are mapped to 0. "
      + "If no value is specified, the minimum value of the attribute "
      + "range in this dimension will be taken.");

  /**
   * OptionID for {@link #MAXIMA_PARAM}
   */
  public static final OptionID MAXIMA_ID = OptionID.getOrCreateOptionID(
      "normalize.max", "a comma separated concatenation "
      + "of the maximum values in each dimension that are mapped to 1 "
      + "If no value is specified, the maximum value of the attribute "
      + "range in this dimension will be taken.");

  /**
   * Parameter for minimum.
   */
  private final DoubleListParameter MINIMA_PARAM = new DoubleListParameter(MINIMA_ID, null, true, null);
  
  /**
   * Parameter for maximum.
   */
  private final DoubleListParameter MAXIMA_PARAM = new DoubleListParameter(MAXIMA_ID, null, true, null);
  
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
    addOption(MINIMA_PARAM);
    addOption(MAXIMA_PARAM);

    ArrayList<Parameter<?,?>> global_1 = new ArrayList<Parameter<?,?>>();
    global_1.add(MINIMA_PARAM);
    global_1.add(MAXIMA_PARAM);
    optionHandler.setGlobalParameterConstraint(new AllOrNoneMustBeSetGlobalConstraint(global_1));

    ArrayList<ListParameter<?>> global = new ArrayList<ListParameter<?>>();
    global.add(MINIMA_PARAM);
    global.add(MAXIMA_PARAM);
    optionHandler.setGlobalParameterConstraint(new EqualSizeGlobalConstraint(global));
  }

  public List<ObjectAndAssociations<V>> normalizeObjects(List<ObjectAndAssociations<V>> objectAndAssociationsList)
      throws NonNumericFeaturesException {
    if (objectAndAssociationsList.size() == 0)
      return new ArrayList<ObjectAndAssociations<V>>();

    if (minima.length == 0 && maxima.length == 0)
      determineMinMax(objectAndAssociationsList);

    int dim = objectAndAssociationsList.get(0).getObject().getDimensionality();
    if (dim != minima.length || dim != maxima.length)
      throw new IllegalArgumentException("Dimensionalities do not agree!");

    try {
      List<ObjectAndAssociations<V>> normalized = new ArrayList<ObjectAndAssociations<V>>();
      for (ObjectAndAssociations<V> objectAndAssociations : objectAndAssociationsList) {
        double[] values = new double[objectAndAssociations.getObject().getDimensionality()];
        for (int d = 1; d <= objectAndAssociations.getObject().getDimensionality(); d++) {
          values[d - 1] = (objectAndAssociations.getObject().getValue(d).doubleValue() - minima[d - 1]) / factor(d);
        }

        V normalizedFeatureVector = objectAndAssociationsList.get(0).getObject().newInstance(values);
        normalizedFeatureVector.setID(objectAndAssociations.getObject().getID());
        Associations associations = objectAndAssociations.getAssociations();
        normalized.add(new ObjectAndAssociations<V>(normalizedFeatureVector, associations));
      }
      return normalized;
    }
    catch (Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
    }
  }

  @SuppressWarnings("unchecked")
  public List<V> normalize(List<V> featureVectors) throws NonNumericFeaturesException {
    if (featureVectors.size() == 0)
      return new ArrayList<V>();

    if (minima.length == 0 && maxima.length == 0)
      determineMinMax(featureVectors.toArray((V[])new RealVector[featureVectors.size()]));

    int dim = featureVectors.get(0).getDimensionality();
    if (dim != minima.length || dim != maxima.length)
      throw new IllegalArgumentException("Dimensionalities do not agree!");

    try {
      List<V> normalized = new ArrayList<V>();
      for (V featureVector : featureVectors) {
        double[] values = new double[featureVector.getDimensionality()];
        for (int d = 1; d <= featureVector.getDimensionality(); d++) {
          values[d - 1] = (featureVector.getValue(d).doubleValue() - minima[d - 1]) / factor(d);
        }
        V normalizedFeatureVector = featureVectors.get(0).newInstance(values);
        normalizedFeatureVector.setID(featureVector.getID());
        normalized.add(normalizedFeatureVector);
      }
      return normalized;
    }
    catch (Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
    }
  }

  public V restore(V featureVector) throws NonNumericFeaturesException {
    if (featureVector.getDimensionality() == maxima.length && featureVector.getDimensionality() == minima.length) {
      double[] values = new double[featureVector.getDimensionality()];
      for (int d = 1; d <= featureVector.getDimensionality(); d++) {
        values[d - 1] = (featureVector.getValue(d).doubleValue() * (factor(d)) + minima[d - 1]);
      }
      V restoredFeatureVector = featureVector.newInstance(values);
      restoredFeatureVector.setID(featureVector.getID());
      return restoredFeatureVector;
    }
    else {
      throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: "
                                            + featureVector.getDimensionality() + " former dimensionality: " + maxima.length);
    }
  }

  public List<V> restore(List<V> featureVectors) throws NonNumericFeaturesException {
    try {
      List<V> restored = new ArrayList<V>();
      for (V featureVector : featureVectors) {
        restored.add(restore(featureVector));
      }
      return restored;
    }
    catch (Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be resized.", e);
    }
  }

  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) throws NonNumericFeaturesException {
    double[][] coeff = linearEquationSystem.getCoefficents();
    double[] rhs = linearEquationSystem.getRHS();
    int[] row = linearEquationSystem.getRowPermutations();
    int[] col = linearEquationSystem.getColumnPermutations();

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < coeff.length; i++)
      for (int r = 0; r < coeff.length; r++) {
        double sum = 0.0;
        for (int c = 0; c < coeff[0].length; c++) {
          sum += minima[c] * coeff[row[r]][col[c]] / factor(c + 1);
          coeff[row[r]][col[c]] = coeff[row[r]][col[c]] / factor(c + 1);
        }
        rhs[row[r]] = rhs[row[r]] + sum;
      }

    LinearEquationSystem lq = new LinearEquationSystem(coeff, rhs, col, row);
    return lq;
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
    result.append(pre).append("normalization class: ").append(getClass().getName());
    result.append("\n");
    result.append(pre).append("normalization minima: ").append(Util.format(minima));
    result.append("\n");
    result.append(pre).append("normalization maxima: ").append(Util.format(maxima));

    return result.toString();
  }

  /**
   * Returns a description of the class and the required parameters. <p/> This
   * description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String parameterDescription() {
    return optionHandler.usage("", false);
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
    String[] remainingParameters = super.setParameters(args);

    if (MINIMA_PARAM.isSet() || MAXIMA_PARAM.isSet()) {
      List<Double> min_list = MINIMA_PARAM.getValue();
      List<Double> max_list = MAXIMA_PARAM.getValue();

      minima = Util.unbox(min_list.toArray(new Double[min_list.size()]));

      maxima = Util.unbox(max_list.toArray(new Double[max_list.size()]));
    }
    return remainingParameters;
  }

  // todo minima und maxima doppelt drin?
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting("value of min ", Format.format(minima));
    mySettings.addSetting("value of max ", Format.format(maxima));
    return settings;
  }

  /**
   * Returns a factor for normalization in a certain dimension. <p/> The
   * provided factor is the maximum-minimum in the specified dimension, if
   * these two values differ, otherwise it is the maximum if this value
   * differs from 0, otherwise it is 1.
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
   * Determines the minima and maxima values in each dimension of the given
   * featureVectors.
   *
   * @param featureVectors the list of feature vectors
   */
  private void determineMinMax(V[] featureVectors) {
    if (featureVectors.length == 0)
      return;
    int dimensionality = featureVectors[0].getDimensionality();
    initMinMax(dimensionality);

    for (V featureVector : featureVectors) {
      updateMinMax(featureVector);
    }
  }

  /**
   * Determines the minima and maxima values in each dimension of the given
   * featureVectors.
   *
   * @param objectAndAssociationsList the list of feature vectors and their associtions
   */
  private void determineMinMax(List<ObjectAndAssociations<V>> objectAndAssociationsList) {
    if (objectAndAssociationsList.isEmpty())
      return;
    int dimensionality = objectAndAssociationsList.get(0).getObject().getDimensionality();
    initMinMax(dimensionality);

    for (ObjectAndAssociations<V> objectAndAssociations : objectAndAssociationsList) {
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
      maxima[i] = -Double.MAX_VALUE;
      minima[i] = Double.MAX_VALUE;
    }
  }

  /**
   * Updates the min and max array according to the specified feature vector.
   *
   * @param featureVector
   *            the feature vector
   */
  private void updateMinMax(V featureVector) {
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
