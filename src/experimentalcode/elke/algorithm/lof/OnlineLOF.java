package experimentalcode.elke.algorithm.lof;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Online algorithm to efficiently update density-based local
 * outlier factors in a database after insertion or deletion of new objects.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class OnlineLOF<O extends DatabaseObject> extends LOF<O> {
    /**
     * OptionID for {@link #LOF_PARAM}
     */
    public static final OptionID LOF_ID = OptionID.getOrCreateOptionID(
        "onlinelof.lof",
        "The name of the file containing the LOFs of the input file."
    );

    /**
     * Parameter to specify the name of the file containing the LOFs of the input file.
     * <p>Key: {@code -onlinelof.lof} </p>
     */
    private final FileParameter LOF_PARAM = new FileParameter(LOF_ID, FileParameter.FileType.INPUT_FILE);

    /**
     * OptionID for {@link #NN_PARAM}
     */
    public static final OptionID NN_ID = OptionID.getOrCreateOptionID(
        "onlinelof.nn",
        "The name of the file containing the nearest neighbors of the input file."
    );

    /**
     * Parameter to specify the name of the file containing the nearest neighbors of the input file.
     * <p>Key: {@code -onlinelof.nn} </p>
     */
    private final FileParameter NN_PARAM = new FileParameter(NN_ID, FileParameter.FileType.INPUT_FILE);

    /**
     * OptionID for {@link #INSERTIONS_PARAM}
     */
    public static final OptionID INSERTIONS_ID = OptionID.getOrCreateOptionID(
        "onlinelof.insertions",
        "The name of the file containing the objects to be inserted."
    );

    /**
     * Parameter to specify the name of the file containing tthe objects to be inserted.
     * <p>Key: {@code -onlinelof.insertions} </p>
     */
    private final FileParameter INSERTIONS_PARAM = new FileParameter(INSERTIONS_ID, FileParameter.FileType.INPUT_FILE);

    /**
     * OptionID for {@link #PARSER_PARAM}
     */
    public static final OptionID PARSER_ID = OptionID.getOrCreateOptionID(
        "onlinelof.parser",
        "Parser to parse the insertion and/or deletion files."
    );

    /**
     * Parameter to specify the parser to parse the insertion and/or deletion files,
     * must extend {@link Parser}.
     * <p>Key: {@code -onlinelof.parser} </p>
     * <p>Default value: {@link DoubleVectorLabelParser} </p>
     */
    protected final ClassParameter<Parser<O>> PARSER_PARAM =
        new ClassParameter<Parser<O>>(PARSER_ID,
            Parser.class,
            DoubleVectorLabelParser.class.getName());

    /**
     * The objects to be inserted.
     */
    private List<Pair<O, Associations>> insertions;

    /**
     * Provides the Online LOF algorithm,
     * adding parameters
     * {@link #LOF_PARAM}, {@link #NN_PARAM}, {@link #INSERTIONS_PARAM}, and {@link #PARSER_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public OnlineLOF() {
        super();

        //parameter lof
        addOption(LOF_PARAM);

        //parameter lof
        addOption(NN_PARAM);

        //parameter insertions
        addOption(INSERTIONS_PARAM);

        //parameter parser
        addOption(PARSER_PARAM);
    }

    /**
     * Performs the Online LOF algorithm on the given database.
     */
    @Override
    protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
        lofTable.resetPageAccess();
        nnTable.resetPageAccess();
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        try {
            for (Pair<O, Associations> objectAndAssociations : insertions) {
                insert(database, objectAndAssociations);
            }
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException(e);
        }

        if (isTime()) {
          logger.verbose("Physical read Access LOF-Table: " + lofTable.getPhysicalReadAccess());
          logger.verbose("Physical write Access LOF-Table: " + lofTable.getPhysicalWriteAccess());
          logger.verbose("Logical page Access LOF-Table:  " + lofTable.getLogicalPageAccess());
          logger.verbose("Physical read Access NN-Table:  " + nnTable.getPhysicalReadAccess());
          logger.verbose("Physical write Access NN-Table:  " + nnTable.getPhysicalWriteAccess());
          logger.verbose("Logical page Access NN-Table:   " + nnTable.getLogicalPageAccess());
        }
        
        // FIXME: re-add lof and nn tables.
        //result = new LOFResult<O>(database, lofTable, nnTable);
        
        //AnnotationFromDatabase<O, Double> res1 = new AnnotationFromDatabase<O, Double>(database);
        // res1.addAssociation(AssociationID.LOF);
        // Ordering
        // OrderingFromAssociation<Double, O> res2 = new OrderingFromAssociation<Double, O>(database, AssociationID.LOF, true); 
        // combine results.
        // result = new MultiResult();
        // result.addResult(res1);
        // result.addResult(res2);
        return result;
    }

    @Override
    public Description getDescription() {
        return new Description("OnlineLOF", "Online Local Outlier Factor",
            "Algorithm to efficiently update density-based local outlier factors in a database " +
                "after insertion or deletion of new objects. ",
            "unpublished.");
    }

    /**
     * Calls the super method
     * and sets additionally the values of the parameters
     * {@link #LOF_PARAM}, {@link #NN_PARAM}, {@link #INSERTIONS_PARAM}, and {@link #PARSER_PARAM}.
     */
    @Override
    public List<String> setParameters(List<String> args) throws ParameterException {
        List<String> remainingParameters = super.setParameters(args);

        // lofTable
        try {
            lofTable = new LOFTable(LOF_PARAM.getValue(), pageSize, cacheSize, minpts);
        }
        catch (IOException e) {
            throw new WrongParameterValueException(LOF_PARAM, LOF_PARAM.getValue().getName(), e);
        }

        // nnTable
        try {
            nnTable = new NNTable(NN_PARAM.getValue(), pageSize, cacheSize, minpts);
        }
        catch (IOException e) {
            throw new WrongParameterValueException(NN_PARAM, NN_PARAM.getValue().getName(), e);
        }

        // parser for insertions
        Parser<O> parser = PARSER_PARAM.instantiateClass();

        // insertions
        try {
            InputStream in = new FileInputStream(INSERTIONS_PARAM.getValue());
            ParsingResult<O> parsingResult = parser.parse(in);
            insertions = transformObjectAndLabels(parsingResult.getObjectAndLabelList());
        }
        catch (FileNotFoundException e) {
            throw new WrongParameterValueException(INSERTIONS_PARAM, INSERTIONS_PARAM.getValue().getName(), e);
        }

        return remainingParameters;
    }

    /**
     * Inserts the specified objects and their associations into the given database
     * and updates the {@link #nnTable} and {@link #lofTable} storing the neighbors and LOFs.
     *
     * @param database             the database to perform the insertion on
     * @param objectAndAssociation the objects and their associations to be inserted
     * @throws UnableToComplyException if insertion is not possible
     */
    private void insert(Database<O> database, Pair<O, Associations> objectAndAssociation) throws UnableToComplyException {
        // insert o into db
        Integer o = database.insert(objectAndAssociation);
        if (logger.isVerbose()) {
          logger.verbose("Insert " + o);
        }

        // get neighbors and reverse nearest neighbors of o
        List<DistanceResultPair<DoubleDistance>> neighbors = database.kNNQueryForID(o, minpts + 1, getDistanceFunction());
        List<DistanceResultPair<DoubleDistance>> reverseNeighbors = database.reverseKNNQuery(o, minpts + 1, getDistanceFunction());
        neighbors.remove(0);
        reverseNeighbors.remove(0);

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append("kNNs[").append(o).append("] ").append(neighbors);
            msg.append("\nrNNs[").append(o).append("]").append(reverseNeighbors);
            logger.debugFine(msg.toString());
        }

        // 0. insert update-object o in NNTable and LOFTable
        insertObjectIntoTables(o, neighbors);

        // 1. Consequences of changing neighbors(p)
        double kNNDist_o = neighbors.get(minpts - 1).getDistance().doubleValue();
        Map<Integer, Double> knnDistances = new HashMap<Integer, Double>();
        for (DistanceResultPair<DoubleDistance> qr : reverseNeighbors) {
            // o has been added to the neighbors(p),
            // therefor another object is no longer in neighbors(p)
            Integer p = qr.getID();
            double dist_po = getDistanceFunction().distance(p, o).doubleValue();
            double reachDist_po = Math.max(kNNDist_o, dist_po);
            NeighborList neighbors_p_old = nnTable.getNeighbors(p);

            if (logger.isDebugging()) {
              logger.debugFine("old kNNs[" + p + "] " + neighbors_p_old);
            }
            // store knn distance for later use
            double knnDistance_p = Math.max(dist_po, neighbors_p_old.get(minpts - 2).getDistance());
            knnDistances.put(p, knnDistance_p);

            // 1.1 determine index von o in neighbors(p)
            int index;
            for (index = 0; index < neighbors_p_old.size(); index++) {
                Neighbor neighbor_p_old = neighbors_p_old.get(index);
                if (dist_po < neighbor_p_old.getDistance() ||
                    (dist_po == neighbor_p_old.getDistance() && p < neighbor_p_old.getNeighborID()))
                    break;
            }

            // 1.2 insert o as index-th neighbor of p in nnTable
            Neighbor neighbor_p_new = new Neighbor(p, index, o, reachDist_po, dist_po);
            Neighbor neighbor_p_old = nnTable.insertAndMove(neighbor_p_new);

            if (logger.isDebugging()) {
              logger.debugFine("old neighbor [" + p + "] " + neighbor_p_old +
                    "\nnew neighbor [" + p + "] " + neighbor_p_new +
                    "\nnew kNNs[" + p + "] " + nnTable.getNeighbors(p));
            }

            // 1.3.1 update sum1 of lof(p)
            LOFEntry lof_p = lofTable.getLOFEntryForUpdate(p);
            double sum1_p = lof_p.getSum1()
                - neighbor_p_old.getReachabilityDistance()
                + neighbor_p_new.getReachabilityDistance();
            lof_p.setSum1(sum1_p);

            // 1.3.2 update sum2 of lof(p)
            double sumReachDists_p = nnTable.getSumOfReachabiltyDistances(o);
            lof_p.insertAndMoveSum2(index, sumReachDists_p);

            NeighborList rnns_p = nnTable.getReverseNeighbors(p);
            if (logger.isDebugging()) {
              logger.debugFine("rnn [" + p + "] " + rnns_p);
            }
            for (Neighbor q : rnns_p) {
                // 1.4 for all q in rnn(p): update sum2 of lof(q)
                LOFEntry lof_q = lofTable.getLOFEntryForUpdate(q.getObjectID());
                double sum2_q_old = lof_q.getSum2(q.getIndex());
                double sum2_q = sum2_q_old
                    - neighbor_p_old.getReachabilityDistance()
                    + neighbor_p_new.getReachabilityDistance();
                lof_q.setSum2(q.getIndex(), sum2_q);
            }
        }

        // 2. Consequences of changing reachdist(q,p)
        for (DistanceResultPair<DoubleDistance> qr : reverseNeighbors) {
            Integer p = qr.getID();
            double knnDistance_p = knnDistances.get(p);
            NeighborList rnns_p = nnTable.getReverseNeighbors(p);
            if (logger.isDebugging()) {
              logger.debugFine("rnn p [" + p + "] " + rnns_p);
            }
            for (int i = 0; i < rnns_p.size(); i++) {
                Neighbor q = rnns_p.get(i);
                double reachDist_qp_old = q.getReachabilityDistance();
                double reachDist_qp_new = Math.max(q.getDistance(), knnDistance_p);

                if (reachDist_qp_old != reachDist_qp_new) {
                    // 2.1 update reachdist(q,p)
                    nnTable.setReachabilityDistance(q, reachDist_qp_new);

                    // 2.2 update sum1 of lof(q)
                    LOFEntry lof_q = lofTable.getLOFEntryForUpdate(q.getObjectID());
                    double sum1_q = lof_q.getSum1()
                        - reachDist_qp_old
                        + reachDist_qp_new;
                    lof_q.setSum1(sum1_q);

                    // 2.3 for all r in rnn(q): update sum2 of lof(r)
                    NeighborList rnns_q = nnTable.getReverseNeighbors(q.getObjectID());
                    if (logger.isDebugging()) {
                      logger.debugFine("rnn q [" + q.getObjectID() + "] " + rnns_q);
                    }
                    for (Neighbor r : rnns_q) {
                        LOFEntry lof_r = lofTable.getLOFEntryForUpdate(r.getObjectID());
                        double sum2_r = lof_r.getSum2(r.getIndex())
                            - reachDist_qp_old
                            + reachDist_qp_new;
                        lof_r.setSum2(r.getIndex(), sum2_r);
                    }
                }
            }
        }
    }

    /**
     * Auxiliary method for {@link #insert(Database, Pair)}:
     * inserts the objects with the specified id into the the {@link #nnTable} and {@link #lofTable}.
     *
     * @param id        the id of the object to be inserted
     * @param neighbors the neighbors of the object to be inserted
     */
    private void insertObjectIntoTables(Integer id, List<DistanceResultPair<DoubleDistance>> neighbors) {
        double sum1 = 0;
        double[] sum2 = new double[minpts];

        for (int i = 0; i < minpts; i++) {
            DistanceResultPair<DoubleDistance> qr = neighbors.get(i);
            Integer p = qr.getID();

            // insert into NNTable
            NeighborList neighbors_p = nnTable.getNeighbors(p);
            double knnDist_p = neighbors_p.get(minpts - 1).getDistance();
            double dist = getDistanceFunction().distance(id, p).doubleValue();
            double reachDist = Math.max(knnDist_p, dist);
            Neighbor neighbor = new Neighbor(id, i, p, reachDist, dist);
            nnTable.insert(neighbor);

            // sum1 von LOF (ok)
            sum1 += reachDist;

            // sum2 von lof (can be changed)
            double sum = 0;
            for (Neighbor q : neighbors_p) {
                sum += q.getReachabilityDistance();
            }
            sum2[i] = sum;
        }
        LOFEntry lofEntry = new LOFEntry(sum1, sum2);
        lofTable.insert(id, lofEntry);

        if (logger.isDebugging()) {
          logger.debugFine("LOF " + id + " " + lofEntry);
        }
    }

    /**
     * Transforms the specified list of objects and their labels into a list of
     * objects and their associations.
     *
     * @param objectAndLabelsList the list of object and their labels to be transformed
     * @return a list of objects and their associations
     */
    // FIXME: duplicated code, this belongs to the parser classes.
    private List<Pair<O, Associations>> transformObjectAndLabels(List<Pair<O, List<String>>> objectAndLabelsList) {
        List<Pair<O, Associations>> result = new ArrayList<Pair<O, Associations>>();

        for (Pair<O, List<String>> objectAndLabels : objectAndLabelsList) {
            List<String> labels = objectAndLabels.getSecond();

            StringBuffer labelBuffer = new StringBuffer();
            for (String label : labels) {
                String l = label.trim();
                if (l.length() != 0) {
                    if (labelBuffer.length() == 0) {
                        labelBuffer.append(l);
                    }
                    else {
                        labelBuffer.append(AbstractDatabaseConnection.LABEL_CONCATENATION);
                        labelBuffer.append(l);
                    }
                }
            }

            Associations associationMap = new Associations();
            if (labelBuffer.length() != 0)
                associationMap.put(AssociationID.LABEL, labelBuffer.toString());

            result.add(new Pair<O, Associations>(objectAndLabels.getFirst(), associationMap));
        }
        return result;
    }
}