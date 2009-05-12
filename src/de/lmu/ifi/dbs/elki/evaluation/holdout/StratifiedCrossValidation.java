package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A stratified n-fold crossvalidation to distribute the data to n buckets where
 * each bucket exhibits approximately the same distribution of classes as does
 * the complete data set. The buckets are disjoint. The distribution is
 * deterministic.
 *
 * @author Arthur Zimek
 */
public class StratifiedCrossValidation<O extends DatabaseObject, L extends ClassLabel> extends
    AbstractHoldout<O, L> {
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
     * Provides a stratified crossvalidation. Setting parameter N_P to the
     * OptionHandler.
     */
    public StratifiedCrossValidation() {
        super();
        addOption(NFOLD_PARAM);
    }

    public TrainingAndTestSet<O, L>[] partition(Database<O> database) {
        this.database = database;
        setClassLabels(database);

        List<Integer>[] classBuckets = ClassGenericsUtil.newArrayOfEmptyArrayList(this.labels.length);
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            int classIndex = Arrays.binarySearch(labels, database.getAssociation(CLASS, id));
            classBuckets[classIndex].add(id);
        }
        List<Integer>[] folds = ClassGenericsUtil.newArrayOfEmptyArrayList(nfold);
        for (List<Integer> bucket : classBuckets) {
            for (int i = 0; i < bucket.size(); i++) {
                folds[i % nfold].add(bucket.get(i));
            }
        }
        TrainingAndTestSet<O, L>[] partitions = ClassGenericsUtil.newArrayOfNull(nfold, TrainingAndTestSet.class);
        for (int i = 0; i < nfold; i++) {
            Map<Integer, List<Integer>> partition = new HashMap<Integer, List<Integer>>();
            List<Integer> training = new ArrayList<Integer>();
            for (int j = 0; j < nfold; j++) {
                if (j != i) {
                    training.addAll(folds[j]);
                }
            }
            partition.put(0, training);
            partition.put(1, folds[i]);
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
        return "Provides a stratified n-fold cross-validation holdout.";
    }

    /**
     * Sets the parameter n additionally to the parameters set by
     * {@link AbstractHoldout#setParameters(String[]) AbstractHoldout.setParameters(args)}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        nfold = NFOLD_PARAM.getValue();

        return remainingParameters;
    }
}
