package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.outlier.SODModel;
import de.lmu.ifi.dbs.algorithm.result.outlier.SODResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class SOD<O extends RealVector<O,Double>, D extends Distance<D>> extends AbstractAlgorithm<O> {

  /**
   * Parameter to indicate the number of shared nearest neighbors to be considered for learning the subspace properties.
   * 
   * <p>Default value: 1</p>
   * 
   * <p>Key: {@code -knn}</p>
   */
  public final IntParameter KNN_PARAM = new IntParameter("knn", "the number of shared nearest neighbors to be considered for learning the subspace properties", new GreaterConstraint(0));
  
  /**
   * Parameter to indicate the multiplier for the discriminance value for discerning small from large variances.
   * 
   * <p>Default value: 1.1</p>
   * 
   * <p>Key: {@code -alpha}</p>
   */
  public final DoubleParameter ALPHA_PARAM = new DoubleParameter("alpha","multiplier for the discriminance value for discerning small from large variances", new GreaterConstraint(0));
  
  
  /**
   * Holds the number of shared nearest neighbors to be considered for learning the subspace properties.
   */
  private int knn;
  
  private SharedNearestNeighborSimilarityFunction<O, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<O, D>();
  
  /**
   * Hold the alpha-value for discerning small from large variances.
   */
  private double alpha;
  
  private SODResult<O> sodResult;
  
  public SOD(){
    super();
    KNN_PARAM.setDefaultValue(1);
    ALPHA_PARAM.setDefaultValue(1.1);
    ALPHA_PARAM.setOptional(true);
    addOption(KNN_PARAM);
    addOption(ALPHA_PARAM);
  }
  
  @Override
  public String description() {
    StringBuilder description = new StringBuilder(); 
    description.append(super.description());
    description.append(Description.NEWLINE);
    description.append(similarityFunction.inlineDescription());
    description.append(Description.NEWLINE);
    return description.toString();
  }

  @Override
  protected void runInTime(Database<O> database) throws IllegalStateException {
    Progress progress = new Progress("assigning SOD",database.size());
    int processed = 0;
    similarityFunction.setDatabase(database, isVerbose(), isTime());
    if(isVerbose()){
      verbose("assigning subspace outlier degree:");
    }
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer queryObject = iter.next();
      processed++;
      if(isVerbose()){
        progress.setProcessed(processed);
        progress(progress);
      }
      List<Integer> knnList = getKNN(database, queryObject).idsToList();
      SODModel<O> model = new SODModel<O>(database,knnList,alpha,database.get(queryObject));
      database.associate(AssociationID.SOD_MODEL, queryObject, model);
    }
    if (isVerbose()) {
      verbose("");
    }
    sodResult = new SODResult<O>(database);
  }
  
  /**
   * Provides the k nearest neighbors in terms of the shared nearest neighbor distance.
   * 
   * The query object is excluded from the knn list.
   * 
   * @param database
   * @param queryObject
   * @return the k nearest neighbors in terms of the shared nearest neighbor distance without the query object
   */
  private KNNList<DoubleDistance> getKNN(Database<O> database, Integer queryObject){
    similarityFunction.getPreprocessor().getParameters();
    KNNList<DoubleDistance> kNearestNeighbors = new KNNList<DoubleDistance>(knn,new DoubleDistance(Double.POSITIVE_INFINITY));
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if(!id.equals(queryObject)){
        DoubleDistance distance = new DoubleDistance(1.0 / similarityFunction.similarity(queryObject, id).getDoubleValue());
        kNearestNeighbors.add(new QueryResult<DoubleDistance>(id,distance));
      }
    }
    return kNearestNeighbors;
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    knn = getParameterValue(KNN_PARAM);
    alpha = getParameterValue(ALPHA_PARAM);
    remainingParameters = similarityFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  public Description getDescription() {
    return new Description("SOD", "Subspace outlier degree", "", "");
  }

  public SODResult<O> getResult() {
    return sodResult;
  }

  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(similarityFunction.getAttributeSettings());
    return attributeSettings;
  }
}
