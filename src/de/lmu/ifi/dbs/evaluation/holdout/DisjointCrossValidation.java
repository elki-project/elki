package de.lmu.ifi.dbs.evaluation.holdout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * DisjointCrossValidationHoldout provides a set of partitions of a database to
 * perform cross-validation.
 * The test sets are guaranteed to be disjoint.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DisjointCrossValidation<O extends DatabaseObject> extends RandomizedHoldout<O> {
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
  public static final String N_D = "positive number of folds for cross-validation";

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

    IntParameter n = new IntParameter(N_P,N_D,new GreaterConstraint(0));
    n.setDefaultValue(N_DEFAULT );
    optionHandler.put(N_P, n);
  }

  /**
   * Provides a set of n partitions of a database to
   * perform n-fold cross-validation.
   *
   * @see Holdout#partition(de.lmu.ifi.dbs.database.Database)
   */
  public TrainingAndTestSet<O>[] partition(Database<O> database) {
    this.database = database;
    setClassLabels(database);
    //noinspection unchecked
    TrainingAndTestSet<O>[] partitions = new TrainingAndTestSet[nfold];
    List<Integer> ids = database.getIDs();
    //noinspection unchecked
    List<Integer>[] parts = new List[nfold];
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
        partitions[i] = new TrainingAndTestSet<O>(part.get(0), part.get(1), this.labels);
      }
      catch (UnableToComplyException e) {
        throw new RuntimeException(e);
      }
    }
    return partitions;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return "Provides an n-fold cross-validation holdout with disjoint test sets.";
  }

  /**
   * Sets the parameter n.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    if (optionHandler.isSet(N_P)) {
      String nfoldString = optionHandler.getOptionValue(N_P);
      try {
        nfold = Integer.parseInt(nfoldString);
        if (nfold <= 0) {
          throw new WrongParameterValueException(N_P, nfoldString, N_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(N_P, nfoldString, N_D, e);
      }
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see RandomizedHoldout#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(N_P, Integer.toString(nfold));
    attributeSettings.add(mySettings);
    return attributeSettings;
  }


}
