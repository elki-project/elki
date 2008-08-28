package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DisjointCrossValidationHoldout provides a set of partitions of a database to
 * perform cross-validation.
 * The test sets are guaranteed to be disjoint.
 *
 * @author Arthur Zimek
 */
public class DisjointCrossValidation<O extends DatabaseObject, L extends ClassLabel<L>> extends RandomizedHoldout<O,L> {
  /**
   * Default number of folds.
   */
  public static final int N_DEFAULT = 10;

  /**
   * OptionID for {@link #NFOLD_PARAM}
   */
  public static final OptionID NFOLD_ID = OptionID.getOrCreateOptionID(
      "nfold", "positive number of folds for cross-validation");

  /**
   * Parameter for number of folds.
   */
  private final IntParameter NFOLD_PARAM = new IntParameter(NFOLD_ID, new GreaterConstraint(0), N_DEFAULT);
  
  /**
   * Holds the number of folds.
   */
  protected int nfold = N_DEFAULT;

  /**
   * Provides a holdout for n-fold cross-validation.
   * Additionally to the parameter seed, the parameter n is set.
   */
  public DisjointCrossValidation() {
    super();

    addOption(NFOLD_PARAM);
  }

  /**
   * Provides a set of n partitions of a database to
   * perform n-fold cross-validation.
   */
  public TrainingAndTestSet<O,L>[] partition(Database<O> database) {
    this.database = database;
    setClassLabels(database);
    TrainingAndTestSet<O,L>[] partitions = TrainingAndTestSet.newArray(nfold);
    List<Integer> ids = database.getIDs();
    List<Integer>[] parts = Util.newArrayOfList(nfold);
    for (int i = 0; i < nfold; i++) {
      parts[i] = new ArrayList<Integer>();
    }
    for (Integer id : ids) {
      parts[random.nextInt(nfold)].add(id);

    }
    for (int i = 0; i < nfold; i++) {
      Map<Integer, List<Integer>> partition = new HashMap<Integer, List<Integer>>();
      List<Integer> training = new ArrayList<Integer>();
      for (int j = 0; j < nfold; j++) {
        if (j != i) {
          training.addAll(parts[j]);
        }
      }
      partition.put(0, training);
      partition.put(1, parts[i]);
      try {
        Map<Integer, Database<O>> part = database.partition(partition);
        partitions[i] = new TrainingAndTestSet<O,L>(part.get(0), part.get(1), this.labels);
      }
      catch (UnableToComplyException e) {
        throw new RuntimeException(e);
      }
    }
    return partitions;
  }

  public String parameterDescription() {
    return "Provides an n-fold cross-validation holdout with disjoint test sets.";
  }

  /**
   * Sets the parameter n.
   *
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    nfold = NFOLD_PARAM.getValue();
    
    return remainingParameters;
  }
}
