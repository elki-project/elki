package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;

import java.util.List;
import java.util.Arrays;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix assignment
 * to objects of a certain database.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCPreprocessor extends ProjectedDBSCANPreprocessor {

  /**
   * The parameter settings for the PCA.
   */
  private String[] pcaParameters;

  /**
   * This method perfoms the variance analysis of a given point w.r.t. a given reference set and a database.
   * This variance analysis is done by PCA applied to the reference set.
   *
   * @param id       the point
   * @param ids      the reference set
   * @param database the database
   */
  protected void runVarianceAnalysis(Integer id, List<Integer> ids, Database<RealVector> database) {

    LinearLocalPCA pca = new LinearLocalPCA();
    try {
      pca.setParameters(pcaParameters);
    }
    catch (ParameterException e) {
      // tested before
      throw new RuntimeException("This should never happen!");
    }
    pca.run(ids, database);

    database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, pca.getCorrelationDimension());
    database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.getSimilarityMatrix());
  }

  /**
   * Sets the values for the parameters alpha, pca and pcaDistancefunction if
   * specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // save parameters for pca
    LinearLocalPCA tmpPCA = new LinearLocalPCA();
    // save parameters for pca
    String[] tmpPCAParameters = new String[remainingParameters.length + 6];
    System.arraycopy(remainingParameters, 0, tmpPCAParameters, 6, remainingParameters.length);
    // eigen pair filter
    tmpPCAParameters[0] = OptionHandler.OPTION_PREFIX + LinearLocalPCA.EIGENPAIR_FILTER_P;
    tmpPCAParameters[1] = LimitEigenPairFilter.class.getName();
    // big value
    tmpPCAParameters[2] = OptionHandler.OPTION_PREFIX + LinearLocalPCA.BIG_VALUE_P;
    tmpPCAParameters[3] = "50";
    // small value
    tmpPCAParameters[4] = OptionHandler.OPTION_PREFIX + LinearLocalPCA.SMALL_VALUE_P;
    tmpPCAParameters[5] = "1";

    remainingParameters = tmpPCA.setParameters(tmpPCAParameters);
    pcaParameters = tmpPCA.getParameters();


    pcaParameters = tmpPCA.getParameters();
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

    LinearLocalPCA tmpPCA = new LinearLocalPCA();
    try {
      tmpPCA.setParameters(pcaParameters);
    }
    catch (ParameterException e) {
      // tested before
      throw new RuntimeException("This should never happen!");
    }
    attributeSettings.addAll(tmpPCA.getAttributeSettings());

    return attributeSettings;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(FourCPreprocessor.class.getName());
    description
    .append(" computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

}
