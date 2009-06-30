package de.lmu.ifi.dbs.elki.evaluation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Compute a ROC curve to evaluate a ranking algorithm and compute the corresponding ROCAUC value.
 * 
 * The parameter {@code -rocauc.positive} specifies the class label of "positive" hits.
 * 
 * The nested algorithm {@code -algorithm} will be run, the result will be searched for an
 * iterable or ordering result, which then is compared with the clustering obtained via
 * the given class label.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 */
// TODO: maybe add a way to process clustering results as well?
public class ComputeROCCurve<O extends DatabaseObject> extends AbstractAlgorithm<O, MultiResult> {
  /**
   * OptionID for {@link #POSITIVE_CLASS_NAME_PARAM}
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("rocauc.positive", "Class label for the 'positive' class.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -rocauc.positive}
   * </p>
   */
  private final PatternParameter POSITIVE_CLASS_NAME_PARAM = new PatternParameter(POSITIVE_CLASS_NAME_ID);

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ClassParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ClassParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);
  
  /**
   * Stores the "positive" class.
   */
  private String positive_class_name;
  
  /**
   * Holds the algorithm to run.
   */
  private Algorithm<O, Result> algorithm;  

  /**
   * The association id to associate the ROC Area-under-Curve.
   */
  public static final AssociationID<Double> ROC_AUC = AssociationID.getOrCreateAssociationID("ROC AUC", Double.class);

  /**
   * Stores the result object.
   */
  private MultiResult result;
  
  public ComputeROCCurve() {
    super();
    addOption(POSITIVE_CLASS_NAME_PARAM);
    addOption(ALGORITHM_PARAM);
  }

  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);
    
    Iterator<Integer> iter = getIterableResult(database, innerresult);
    Cluster<Model> positivecluster = getReferenceCluster(database, positive_class_name);

    List<Integer> order = new ArrayList<Integer>(database.size());
    while (iter.hasNext()) {
      Object o = iter.next();
      if (!(o instanceof Integer)) {
        throw new IllegalStateException("Iterable result contained non-Integer - result didn't satisfy requirements");        
      } else {
        order.add((Integer)o);
      }
    }
    if (order.size() != database.size()) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");        
    }
    List<Pair<Double, Double>> roccurve = ROC.materializeROC(database.size(), positivecluster.getIDs(), new ROC.SimpleAdapter(order.iterator()));
    double rocauc = ROC.computeAUC(roccurve);    
    
    result = new MultiResult();
    result.addResult(new CollectionResult<Pair<Double, Double>>(roccurve));
    ResultUtil.setGlobalAssociation(result, ROC_AUC, rocauc);
    return result;
  }

  /**
   * Find the "positive" reference cluster using a by label clustering.
   * 
   * @param database Database to search in
   * @param class_name Cluster name
   * @return found cluster or it throws an exception.
   */
  private Cluster<Model> getReferenceCluster(Database<O> database, String class_name) {
    ByLabelHierarchicalClustering<O> reference = new ByLabelHierarchicalClustering<O>();
    Clustering<Model> refc = reference.run(database);
    for (Cluster<Model> clus : refc.getAllClusters()) {
      if (clus.getNameAutomatic().compareToIgnoreCase(class_name) == 0) {
        return clus;
      }
    }
    throw new IllegalStateException("'Positive' cluster not found - cannot compute a ROCAUC value without a reference set.");
  }
  
  /**
   * Find an "iterable" result that looks like object IDs.
   * 
   * @param database Database context
   * @param result Result object
   * @return Iterator to work with
   */
  @SuppressWarnings("unchecked")
  private Iterator<Integer> getIterableResult(Database<O> database, Result result) {
    List<IterableResult<?>> iterables = ResultUtil.getIterableResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // try iterable results first
    if (iterables.size() >= 1) {
      for (IterableResult<?> ir : iterables) {
        Iterator<?> testit = ir.iterator();
        if (testit.hasNext() && (testit.next() instanceof Integer)) {
          // note: we DO want a fresh iterator here!
          return (Iterator<Integer>) ir.iterator();
        }
      }
    }
    // otherwise apply an ordering to the database IDs.
    if (orderings.size() == 1) {
      return orderings.get(0).iter(database.getIDs());
    }
    throw new IllegalStateException("Comparison algorithm expected exactly one iterable result part, got "+iterables.size()+" iterable results and "+orderings.size()+" ordering results.");
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultiResult getResult() {
    return result;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#setParameters(java.lang.String[])
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    
    positive_class_name = POSITIVE_CLASS_NAME_PARAM.getValue();

    // algorithm
    algorithm = ALGORITHM_PARAM.instantiateClass();
    addParameterizable(algorithm);
    remainingParameters = algorithm.setParameters(remainingParameters);
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
