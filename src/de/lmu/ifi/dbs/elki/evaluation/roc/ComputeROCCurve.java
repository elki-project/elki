package de.lmu.ifi.dbs.elki.evaluation.roc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Compute a ROC curve to evaluate a ranking algorithm and compute the
 * corresponding ROCAUC value.
 * 
 * The parameter {@code -rocauc.positive} specifies the class label of
 * "positive" hits.
 * 
 * The nested algorithm {@code -algorithm} will be run, the result will be
 * searched for an iterable or ordering result, which then is compared with the
 * clustering obtained via the given class label.
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
   * The pattern to identify positive classes.
   * 
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
  private final ObjectParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positive_class_name;

  /**
   * Holds the algorithm to run.
   */
  private Algorithm<O, Result> algorithm;

  /**
   * The association id to associate the ROC Area-under-Curve.
   */
  public static final AssociationID<Double> ROC_AUC = AssociationID.getOrCreateAssociationID("ROC AUC", Double.class);

  /**
   * Constructor
   * 
   * @param config Parameters
   */
  public ComputeROCCurve(Parameterization config) {
    super(config);
    if(config.grab(POSITIVE_CLASS_NAME_PARAM)) {
      positive_class_name = POSITIVE_CLASS_NAME_PARAM.getValue();
    }

    if(config.grab(ALGORITHM_PARAM)) {
      algorithm = ALGORITHM_PARAM.instantiateClass(config);
    }
  }

  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    Iterator<Integer> iter = getIterableResult(database, innerresult);
    Collection<Integer> positiveids = DatabaseUtil.getObjectsByLabelMatch(database, positive_class_name);

    List<Integer> order = new ArrayList<Integer>(database.size());
    while(iter.hasNext()) {
      Object o = iter.next();
      if(!(o instanceof Integer)) {
        throw new IllegalStateException("Iterable result contained non-Integer - result didn't satisfy requirements");
      }
      else {
        order.add((Integer) o);
      }
    }
    if(order.size() != database.size()) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    List<Pair<Double, Double>> roccurve = ROC.materializeROC(database.size(), positiveids, new ROC.SimpleAdapter(order.iterator()));
    double rocauc = ROC.computeAUC(roccurve);

    List<String> header = new ArrayList<String>(1);
    header.add(ROC_AUC.getLabel() + ": " + rocauc);
    MultiResult result = ResultUtil.ensureMultiResult(innerresult);
    result.addResult(new CollectionResult<Pair<Double, Double>>(roccurve, header));
    return result;
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
    if(iterables.size() >= 1) {
      for(IterableResult<?> ir : iterables) {
        Iterator<?> testit = ir.iterator();
        if(testit.hasNext() && (testit.next() instanceof Integer)) {
          // note: we DO want a fresh iterator here!
          return (Iterator<Integer>) ir.iterator();
        }
      }
    }
    // otherwise apply an ordering to the database IDs.
    if(orderings.size() == 1) {
      return orderings.get(0).iter(database.getIDs());
    }
    throw new IllegalStateException("Comparison algorithm expected exactly one iterable result part, got " + iterables.size() + " iterable results and " + orderings.size() + " ordering results.");
  }
}
