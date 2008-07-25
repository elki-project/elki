package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RandomizedCrossValidationHoldout provides a set of partitions of a database
 * to perform cross-validation. The test sets are not guaranteed to be disjoint.
 *
 * @author Arthur Zimek 
 */
public class RandomizedCrossValidation<O extends DatabaseObject, L extends ClassLabel<L>> extends
    RandomizedHoldout<O,L> {
  /**
   * Parameter n for the number of folds.
   */
  public static final String N_P = "nfold";

  /**
   * Default number of folds.
   */
  public static final int N_DEFAULT = 10;

  /**
   * Description of the parameter n.
   */
  public static final String N_D = "number of folds for cross-validation";

  /**
   * Holds the number of folds.
   */
  protected int nfold;

  /**
   * Provides a holdout for n-fold cross-validation. Additionally to the
   * parameter seed, the parameter n is set.
   */
  public RandomizedCrossValidation() {
    super();
    IntParameter n = new IntParameter(N_P, N_D, new GreaterEqualConstraint(1));
    n.setDefaultValue(N_DEFAULT);
    optionHandler.put(n);
  }

  /**
   * Provides a set of n partitions of a database to perform n-fold
   * cross-validation.
   *
   * @see Holdout#partition(de.lmu.ifi.dbs.elki.database.Database)
   */
  public TrainingAndTestSet<O,L>[] partition(Database<O> database) {
    this.database = database;
    setClassLabels(database);
    // noinspection unchecked
    TrainingAndTestSet<O,L>[] partitions = new TrainingAndTestSet[nfold];
    List<Integer> ids = database.getIDs();
    for (int i = 0; i < nfold; i++) {
      List<Integer> training = new ArrayList<Integer>();
      List<Integer> test = new ArrayList<Integer>();
      for (Integer id : ids) {
        if (random.nextInt(nfold) < nfold - 1) {
          training.add(id);
        }
        else {
          test.add(id);
        }
      }
      Map<Integer, List<Integer>> partition = new HashMap<Integer, List<Integer>>();
      partition.put(0, training);
      partition.put(1, test);
      try {
        Map<Integer, Database<O>> part = database.partition(partition);
        partitions[i] = new TrainingAndTestSet<O,L>(part.get(0), part
            .get(1), this.labels);
      }
      catch (UnableToComplyException e) {
        throw new RuntimeException(e);
      }
    }
    return partitions;
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
   */
  public String parameterDescription() {
    return "Provides an n-fold cross-validation holdout.";
  }

  /**
   * Sets the parameter n additionally to the parameters set by
   * {@link RandomizedHoldout#setParameters(String[]) RandomizedHoldout.setParameters(args)}.
   *
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    nfold = (Integer) optionHandler.getOptionValue(N_P);

    return remainingParameters;
  }
}
