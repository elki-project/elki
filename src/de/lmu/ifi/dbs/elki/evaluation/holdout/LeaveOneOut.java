package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A leave-one-out-holdout is to provide a set of partitions of a database
 * where each instances once hold out as a test instance while the respectively remaining
 * instances are training instances.
 *
 * @author Arthur Zimek
 */
public class LeaveOneOut<O extends DatabaseObject, L extends ClassLabel> extends AbstractHoldout<O, L> {


    /**
     * Provides a leave-one-out partitioner.
     */
    public LeaveOneOut() {
        super();
    }

    /**
     * Provides a set of partitions of the database, where each element is once
     * hold out as test instance, the remaining instances are given in the
     * training set.
     */
    public TrainingAndTestSet<O, L>[] partition(Database<O> database) {
        this.database = database;
        setClassLabels(database);
        int size = database.size();
        TrainingAndTestSet<O, L>[] partitions = ClassGenericsUtil.newArrayOfNull(size, TrainingAndTestSet.class);
        List<Integer> ids = database.getIDs();
        for (int i = 0; i < size; i++) {
            Map<Integer, List<Integer>> partition = new HashMap<Integer, List<Integer>>();
            List<Integer> training = new ArrayList<Integer>(ids);
            List<Integer> test = new ArrayList<Integer>();
            Integer holdoutID = training.remove(i);
            test.add(holdoutID);
            partition.put(0, training);
            partition.put(1, test);
            try {
                Map<Integer, Database<O>> part = database.partition(partition);
                partitions[i] = new TrainingAndTestSet<O, L>(part.get(0), part.get(1), this.labels);
            }
            catch (UnableToComplyException e) {
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    @Override
    public String parameterDescription() {
        return "Provides a leave-one-out (jackknife) holdout.";
    }
}
