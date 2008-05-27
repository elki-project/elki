package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
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
   * <p>Key: {@code knn}</p>
   */
  public static final IntParameter KNN_PARAM = new IntParameter("knn", "the number of shared nearest neighbors to be considered for learning the subspace properties", new GreaterConstraint(0));
  
  static{
    KNN_PARAM.setDefaultValue(1);
  }
  
  /**
   * Holds the number of shared nearest neighbors to be considered for learning the subspace properties.
   */
  private int knn;
  
  private SharedNearestNeighborSimilarityFunction<O, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<O, D>();
  
  public SOD(){
    super();
    addOption(KNN_PARAM);
  }
  
  @Override
  public String description() {
    // TODO
    return super.description();
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
      
    }
    // TODO Auto-generated method stub
    if (isVerbose()) {
      verbose("");
    }
  }
  
  private KNNList<DoubleDistance> getKNN(Database<O> database, Integer queryObject){
    similarityFunction.getPreprocessor().getParameters();
    KNNList<DoubleDistance> kNearestNeighbors = new KNNList<DoubleDistance>(knn,new DoubleDistance(Double.POSITIVE_INFINITY));
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      DoubleDistance distance = new DoubleDistance(1.0 / similarityFunction.similarity(queryObject, id).getDoubleValue());
      kNearestNeighbors.add(new QueryResult<DoubleDistance>(id,distance));
    }
    return kNearestNeighbors;
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    knn = getParameterValue(KNN_PARAM);
    remainingParameters = similarityFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  public Description getDescription() {
    return new Description("SOD", "Subspace outlier degree", "", "");
  }

  public Result<O> getResult() {
    // TODO Auto-generated method stub
    return null;
  }

  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(similarityFunction.getAttributeSettings());
    return attributeSettings;
  }
}
