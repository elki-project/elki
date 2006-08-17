package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix assignment
 * to objects of a certain database.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCPreprocessor extends ProjectedDBSCANPreprocessor {

  /**
   * Flag for marking parameter delta as an absolute value.
   */
  public static final String ABSOLUTE_F = LimitEigenPairFilter.ABSOLUTE_F;

  /**
   * Description for flag abs.
   */
  public static final String ABSOLUTE_D = LimitEigenPairFilter.ABSOLUTE_D;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = LimitEigenPairFilter.DELTA_P;

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = LimitEigenPairFilter.DELTA_D;

  /**
   * Threshold for strong eigenpairs, can be absolute or relative.
   */
  private double delta;

  /**
   * Indicates wether delta is an absolute or a relative value.
   */
  private boolean absolute;

  /**
   * The parameter settings for the PCA.
   */
  private String[] pcaParameters;

  /**
   * This method implements the type of variance analysis to be computed for a given point.
   * <p/>
   * Example1: for 4C, this method should implement a PCA for the given point.
   * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
   *
   * @param id        the given point
   * @param neighbors the neighbors as query results of the given point
   * @param database  the database for which the preprocessing is performed
   */
  protected <D extends Distance<D>> void runVarianceAnalysis(Integer id, List<QueryResult<D>> neighbors, Database<RealVector> database) {
	  LinearLocalPCA pca = new LinearLocalPCA();
    try {
      pca.setParameters(pcaParameters);
    }
    catch (ParameterException e) {
      // tested before
      throw new RuntimeException("This should never happen!");
    }

    List<Integer> ids = new ArrayList<Integer>(neighbors.size());
    for (QueryResult<D> neighbor : neighbors) {
      ids.add(neighbor.getID());
    }
    pca.run(ids, database);

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n").append(id).append(" ").append(database.getAssociation(AssociationID.LABEL, id));
      msg.append("\ncorrDim ").append(pca.getCorrelationDimension());
      debugFine(msg.toString());
    }
    database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, pca.getCorrelationDimension());
    database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.similarityMatrix());
  }

  /**
   * Sets the values for the parameters alpha, pca and pcaDistancefunction if
   * specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // absolute
    absolute = optionHandler.isSet(ABSOLUTE_F);

    //delta
    if (optionHandler.isSet(DELTA_P)) {
      String deltaString = optionHandler.getOptionValue(DELTA_P);
      try {
        delta = Double.parseDouble(deltaString);
        if (! absolute && delta < 0 || delta > 1)
          throw new WrongParameterValueException(DELTA_P, deltaString, DELTA_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(DELTA_P, deltaString, DELTA_D, e);
      }
    }
    else if (! absolute) {
      delta = LimitEigenPairFilter.DEFAULT_DELTA;
    }
    else {
      throw new WrongParameterValueException("Illegal parameter setting: " +
                                             "Flag " + ABSOLUTE_F + " is set, " +
                                             "but no value for " + DELTA_P + " is specified.");
    }

    LinearLocalPCA tmpPCA = new LinearLocalPCA();
    // save parameters for pca
    List<String> tmpPCAParameters = new ArrayList<String>();
    // eigen pair filter
    tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LinearLocalPCA.EIGENPAIR_FILTER_P);
    tmpPCAParameters.add(LimitEigenPairFilter.class.getName());
    // abs
    if (absolute) {
      tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LimitEigenPairFilter.ABSOLUTE_F);
    }
    // delta
    tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LimitEigenPairFilter.DELTA_P);
    tmpPCAParameters.add(Double.toString(delta));

    // big value
    tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LinearLocalPCA.BIG_VALUE_P);
    tmpPCAParameters.add("50");
    // small value
    tmpPCAParameters.add(OptionHandler.OPTION_PREFIX + LinearLocalPCA.SMALL_VALUE_P);
    tmpPCAParameters.add("1");

    pcaParameters = tmpPCAParameters.toArray(new String[tmpPCAParameters.size()]);
    tmpPCA.setParameters(pcaParameters);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(ABSOLUTE_F, Boolean.toString(absolute));
    mySettings.addSetting(DELTA_P, Double.toString(delta));

    return attributeSettings;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(FourCPreprocessor.class.getName());
    description.append(" computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

}