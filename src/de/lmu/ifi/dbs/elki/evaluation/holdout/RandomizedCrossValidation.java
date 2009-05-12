package de.lmu.ifi.dbs.elki.evaluation.holdout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * RandomizedCrossValidationHoldout provides a set of partitions of a database
 * to perform cross-validation. The test sets are not guaranteed to be disjoint.
 *
 * @author Arthur Zimek
 */
public class RandomizedCrossValidation<O extends DatabaseObject, L extends ClassLabel> extends
    RandomizedHoldout<O, L> {

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
    protected int nfold;

    /**
     * Provides a holdout for n-fold cross-validation. Additionally to the
     * parameter seed, the parameter n is set.
     */
    public RandomizedCrossValidation() {
        super();
        addOption(NFOLD_PARAM);
    }

    /**
     * Provides a set of n partitions of a database to perform n-fold
     * cross-validation.
     */
    public TrainingAndTestSet<O, L>[] partition(Database<O> database) {
        this.database = database;
        setClassLabels(database);
        TrainingAndTestSet<O, L>[] partitions = ClassGenericsUtil.newArrayOfNull(nfold, TrainingAndTestSet.class);
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
                partitions[i] = new TrainingAndTestSet<O, L>(part.get(0), part
                    .get(1), this.labels);
            }
            catch (UnableToComplyException e) {
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    @Override
    public String parameterDescription() {
        return "Provides an n-fold cross-validation holdout.";
    }

    /**
     * Sets the parameter n additionally to the parameters set by
     * {@link RandomizedHoldout#setParameters(String[]) RandomizedHoldout.setParameters(args)}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        nfold = NFOLD_PARAM.getValue();

        return remainingParameters;
    }
}
