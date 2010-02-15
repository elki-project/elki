package de.lmu.ifi.dbs.elki.normalization;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.AllOrNoneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualSizeGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to perform and undo a normalization on real vectors with respect to
 * given minimum and maximum in each dimension.
 * 
 * @author Elke Achtert
 * @param <V> vector type
 */
// TODO: extract superclass AbstractAttributeWiseNormalization
public class AttributeWiseMinMaxNormalization<V extends NumberVector<V, ?>> extends AbstractNormalization<V> {
  /**
   * OptionID for {@link #MINIMA_PARAM}
   */
  public static final OptionID MINIMA_ID = OptionID.getOrCreateOptionID("normalize.min", "a comma separated concatenation of the minimum values in each dimension that are mapped to 0. If no value is specified, the minimum value of the attribute range in this dimension will be taken.");

  /**
   * OptionID for {@link #MAXIMA_PARAM}
   */
  public static final OptionID MAXIMA_ID = OptionID.getOrCreateOptionID("normalize.max", "a comma separated concatenation of the maximum values in each dimension that are mapped to 1. If no value is specified, the maximum value of the attribute range in this dimension will be taken.");

  /**
   * Parameter for minimum.
   */
  private final DoubleListParameter MINIMA_PARAM = new DoubleListParameter(MINIMA_ID, true);

  /**
   * Parameter for maximum.
   */
  private final DoubleListParameter MAXIMA_PARAM = new DoubleListParameter(MAXIMA_ID, true);

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
  public AttributeWiseMinMaxNormalization(Parameterization config) {
    super();
    if (config.grab(this, MINIMA_PARAM)) {
      List<Double> min_list = MINIMA_PARAM.getValue();
      minima = Util.unbox(min_list.toArray(new Double[min_list.size()]));
    }
    if (config.grab(this, MAXIMA_PARAM)) {
      List<Double> max_list = MAXIMA_PARAM.getValue();
      maxima = Util.unbox(max_list.toArray(new Double[max_list.size()]));
    }

    ArrayList<Parameter<?, ?>> global_1 = new ArrayList<Parameter<?, ?>>();
    global_1.add(MINIMA_PARAM);
    global_1.add(MAXIMA_PARAM);
    addGlobalParameterConstraint(new AllOrNoneMustBeSetGlobalConstraint(global_1));

    ArrayList<ListParameter<?>> global = new ArrayList<ListParameter<?>>();
    global.add(MINIMA_PARAM);
    global.add(MAXIMA_PARAM);
    addGlobalParameterConstraint(new EqualSizeGlobalConstraint(global));
  }

  public List<Pair<V, Associations>> normalizeObjects(List<Pair<V, Associations>> objectAndAssociationsList) throws NonNumericFeaturesException {
    if(objectAndAssociationsList.size() == 0) {
      return new ArrayList<Pair<V, Associations>>();
    }

    if(minima.length == 0 && maxima.length == 0) {
      determineMinMax(objectAndAssociationsList);
    }

    int dim = objectAndAssociationsList.get(0).getFirst().getDimensionality();
    if(dim != minima.length || dim != maxima.length) {
      throw new IllegalArgumentException("Dimensionalities do not agree!");
    }

    try {
      List<Pair<V, Associations>> normalized = new ArrayList<Pair<V, Associations>>();
      for(Pair<V, Associations> objectAndAssociations : objectAndAssociationsList) {
        double[] values = new double[objectAndAssociations.getFirst().getDimensionality()];
        for(int d = 1; d <= objectAndAssociations.getFirst().getDimensionality(); d++) {
          values[d - 1] = (objectAndAssociations.getFirst().doubleValue(d) - minima[d - 1]) / factor(d);
        }

        V normalizedFeatureVector = objectAndAssociationsList.get(0).getFirst().newInstance(values);
        normalizedFeatureVector.setID(objectAndAssociations.getFirst().getID());
        Associations associations = objectAndAssociations.getSecond();
        normalized.add(new Pair<V, Associations>(normalizedFeatureVector, associations));
      }
      return normalized;
    }
    catch(Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
    }
  }

  public List<V> normalize(List<V> featureVectors) throws NonNumericFeaturesException {
    if(featureVectors.size() == 0) {
      return new ArrayList<V>();
    }

    if(minima.length == 0 && maxima.length == 0) {
      determineMinMax(ClassGenericsUtil.toArray(featureVectors, NumberVector.class));
    }

    int dim = featureVectors.get(0).getDimensionality();
    if(dim != minima.length || dim != maxima.length) {
      throw new IllegalArgumentException("Dimensionalities do not agree!");
    }

    try {
      List<V> normalized = new ArrayList<V>();
      for(V featureVector : featureVectors) {
        double[] values = new double[featureVector.getDimensionality()];
        for(int d = 1; d <= featureVector.getDimensionality(); d++) {
          values[d - 1] = (featureVector.doubleValue(d) - minima[d - 1]) / factor(d);
        }
        V normalizedFeatureVector = featureVectors.get(0).newInstance(values);
        normalizedFeatureVector.setID(featureVector.getID());
        normalized.add(normalizedFeatureVector);
      }
      return normalized;
    }
    catch(Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
    }
  }

  public V restore(V featureVector) throws NonNumericFeaturesException {
    if(featureVector.getDimensionality() == maxima.length && featureVector.getDimensionality() == minima.length) {
      double[] values = new double[featureVector.getDimensionality()];
      for(int d = 1; d <= featureVector.getDimensionality(); d++) {
        values[d - 1] = (featureVector.doubleValue(d) * (factor(d)) + minima[d - 1]);
      }
      V restoredFeatureVector = featureVector.newInstance(values);
      restoredFeatureVector.setID(featureVector.getID());
      return restoredFeatureVector;
    }
    else {
      throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: " + featureVector.getDimensionality() + " former dimensionality: " + maxima.length);
    }
  }

  public List<V> restore(List<V> featureVectors) throws NonNumericFeaturesException {
    try {
      List<V> restored = new ArrayList<V>();
      for(V featureVector : featureVectors) {
        restored.add(restore(featureVector));
      }
      return restored;
    }
    catch(Exception e) {
      throw new NonNumericFeaturesException("Attributes cannot be resized.", e);
    }
  }

  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    double[][] coeff = linearEquationSystem.getCoefficents();
    double[] rhs = linearEquationSystem.getRHS();
    int[] row = linearEquationSystem.getRowPermutations();
    int[] col = linearEquationSystem.getColumnPermutations();

    // noinspection ForLoopReplaceableByForEach
    for(int i = 0; i < coeff.length; i++)
      for(int r = 0; r < coeff.length; r++) {
        double sum = 0.0;
        for(int c = 0; c < coeff[0].length; c++) {
          sum += minima[c] * coeff[row[r]][col[c]] / factor(c + 1);
          coeff[row[r]][col[c]] = coeff[row[r]][col[c]] / factor(c + 1);
        }
        rhs[row[r]] = rhs[row[r]] + sum;
      }

    LinearEquationSystem lq = new LinearEquationSystem(coeff, rhs, row, col);
    return lq;
  }

  /**
   * Returns a string representation of this normalization. The specified prefix
   * pre will be the prefix of each new line. This method is used to write the
   * parameters of a normalization to a result of an algorithm using this
   * normalization.
   * 
   * @param pre the prefix of each new line
   * @return a string representation of this normalization
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(pre).append("normalization class: ").append(getClass().getName());
    result.append("\n");
    result.append(pre).append("normalization minima: ").append(FormatUtil.format(minima));
    result.append("\n");
    result.append(pre).append("normalization maxima: ").append(FormatUtil.format(maxima));

    return result.toString();
  }

  /**
   * Returns a factor for normalization in a certain dimension.
   * <p/>
   * The provided factor is the maximum-minimum in the specified dimension, if
   * these two values differ, otherwise it is the maximum if this value differs
   * from 0, otherwise it is 1.
   * 
   * @param dimension the dimension to get a factor for normalization
   * @return a factor for normalization in a certain dimension
   */
  private double factor(int dimension) {
    return maxima[dimension - 1] != minima[dimension - 1] ? maxima[dimension - 1] - minima[dimension - 1] : maxima[dimension - 1] != 0 ? maxima[dimension - 1] : 1;
  }

  /**
   * Determines the minima and maxima values in each dimension of the given
   * featureVectors.
   * 
   * @param featureVectors the list of feature vectors
   */
  private void determineMinMax(V[] featureVectors) {
    if(featureVectors.length == 0) {
      return;
    }
    int dimensionality = featureVectors[0].getDimensionality();
    initMinMax(dimensionality);

    for(V featureVector : featureVectors) {
      updateMinMax(featureVector);
    }
  }

  /**
   * Determines the minima and maxima values in each dimension of the given
   * featureVectors.
   * 
   * @param objectAndAssociationsList the list of feature vectors and their
   *        associations
   */
  private void determineMinMax(List<Pair<V, Associations>> objectAndAssociationsList) {
    if(objectAndAssociationsList.isEmpty()) {
      return;
    }
    int dimensionality = objectAndAssociationsList.get(0).getFirst().getDimensionality();
    initMinMax(dimensionality);

    for(Pair<V, Associations> objectAndAssociations : objectAndAssociationsList) {
      updateMinMax(objectAndAssociations.getFirst());
    }
  }

  /**
   * Initializes the min and max array.
   * 
   * @param dimensionality the dimensionality of the feature vectors to be
   *        normalized
   */
  private void initMinMax(int dimensionality) {
    minima = new double[dimensionality];
    maxima = new double[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      maxima[i] = -Double.MAX_VALUE;
      minima[i] = Double.MAX_VALUE;
    }
  }

  /**
   * Updates the min and max array according to the specified feature vector.
   * 
   * @param featureVector the feature vector
   */
  private void updateMinMax(V featureVector) {
    if(minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors differ in length.");
    }
    for(int d = 1; d <= featureVector.getDimensionality(); d++) {
      if((featureVector.doubleValue(d)) > maxima[d - 1]) {
        maxima[d - 1] = featureVector.doubleValue(d);
      }
      if((featureVector.doubleValue(d)) < minima[d - 1]) {
        minima[d - 1] = featureVector.doubleValue(d);
      }
    }
  }
}
