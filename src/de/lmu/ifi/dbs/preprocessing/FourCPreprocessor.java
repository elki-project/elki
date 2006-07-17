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
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix assignment
 * to objects of a certain database.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCPreprocessor extends ProjectedDBSCANPreprocessor {
//  /**
//   * Holds the class specific debug status.
//   */
//  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
////  private static final boolean DEBUG = true;
//
//  /**
//   * The logger of this class.
//   */
//  @SuppressWarnings({"FieldCanBeLocal"})
//  private Logger logger = Logger.getLogger(this.getClass().getName());

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
//      msg.append("\nsimMatrix ").append(pca.getSimilarityMatrix());
      debugFine(msg.toString());
//      logger.fine(msg.toString());
    }
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

    // FIXME: hier Fehler? (Simon hat Problem: keine Options gesetzt)
    remainingParameters = tmpPCA.setParameters(tmpPCAParameters);
    // XXX warum remainingParameters ueberschrieben?
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
    description.append(" computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

}