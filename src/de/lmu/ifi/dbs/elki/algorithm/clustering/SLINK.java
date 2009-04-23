package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.FiniteProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Efficient implementation of the Single-Link Algorithm SLINK of R. Sibson.
 * <p>Reference:
 * R. Sibson: SLINK:  An optimally efficient algorithm for the single-link cluster method.
 * <br>In: The Computer Journal 16 (1973), No. 1, p. 30-34.
 * </p>
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used
 */
public class SLINK<O extends DatabaseObject, D extends Distance<D>> extends
    DistanceBasedAlgorithm<O, D, MultiResult> {

    /**
     * Association ID for SLINK pi pointer
     */
    private static final AssociationID<Integer> SLINK_PI = AssociationID.getOrCreateAssociationID("SLINK pi", Integer.class);

    /**
     * Association ID for SLINK lambda value
     */
    private static final AssociationID<Distance<?>> SLINK_LAMBDA = AssociationID.getOrCreateAssociationIDGenerics("SLINK lambda", Distance.class);

    /**
     * The values of the function Pi of the pointer representation.
     */
    private HashMap<Integer, Integer> pi = new HashMap<Integer, Integer>();

    /**
     * The values of the function Lambda of the pointer representation.
     */
    private HashMap<Integer, D> lambda = new HashMap<Integer, D>();

    /**
     * The values of the helper function m to determine the pointer
     * representation.
     */
    private HashMap<Integer, D> m = new HashMap<Integer, D>();

    /**
     * Provides the result of the algorithm.
     */
    protected MultiResult result;

    /**
     * Creates a new instance of a single link algorithm. Since SLINK is a non
     * abstract class the option handler is initialized.
     */
    public SLINK() {
        super();
    }

    /**
     * Performs the SLINK algorithm on the given database.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected MultiResult runInTime(Database<O> database) throws IllegalStateException {

        try {
            FiniteProgress progress = new FiniteProgress("Clustering", database.size());
            getDistanceFunction().setDatabase(database, isVerbose(), isTime());

            // sort the db objects according to their ids
            List<Integer> ids = database.getIDs();
            Collections.sort(ids);

            ArrayList<Integer> processedIDs = new ArrayList<Integer>();
            // apply the algorithm
            for (Integer id : ids) {
                step1(id);
                step2(id, processedIDs);
                step3(id, processedIDs);
                step4(id, processedIDs);

                processedIDs.add(id);

                if (isVerbose()) {
                    progress.setProcessed(id);
                    logger.progress(progress);
                }
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }

        HashMap<Integer, Integer> piClone = (HashMap<Integer, Integer>) pi.clone();
        HashMap<Integer, Distance<?>> lambdaClone = (HashMap<Integer, Distance<?>>) lambda.clone();

        result = new MultiResult();
        result.addResult(new AnnotationFromHashMap<Integer>(SLINK_PI, piClone));
        result.addResult(new AnnotationFromHashMap<Distance<?>>(SLINK_LAMBDA, lambdaClone));
        // TODO: ensure that the object ID itself is also output. using AssociationID? 
        return result;
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public MultiResult getResult() {
        return result;
    }

    /**
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    public Description getDescription() {
        return new Description(
            "SLINK",
            "Single Link Clustering",
            "Hierarchical clustering algorithm.",
            "R. Sibson: SLINK:  An optimally efficient algorithm for the single-link cluster method."
            + "In: The Computer Journal 16 (1973), No. 1, p. 30-34.");
    }

    /**
     * First step: Initialize P(id) = id, L(id) = infinity.
     *
     * @param newID the id of the object to be inserted into the pointer
     *              representation
     */
    private void step1(int newID) {
        // P(n+1) = n+1:
        pi.put(newID, newID);
        // L(n+1) = infinity
        lambda.put(newID, getDistanceFunction().infiniteDistance());
    }

    /**
     * Second step: Determine the pairwise distances from all objects in the
     * pointer representation to the new object with the specified id.
     *
     * @param newID        the id of the object to be inserted into the pointer
     *                     representation
     * @param processedIDs the already processed ids
     */
    private void step2(int newID, ArrayList<Integer> processedIDs) {
        // M(i) = dist(i, n+1)
        for (Integer id : processedIDs) {
            D distance = getDistanceFunction().distance(newID, id);
            m.put(id, distance);
        }
    }

    /**
     * Third step: Determine the values for P and L
     *
     * @param newID        the id of the object to be inserted into the pointer
     *                     representation
     * @param processedIDs the already processed ids
     */
    private void step3(int newID, ArrayList<Integer> processedIDs) {
        // for i = 1..n
        for (Integer id : processedIDs) {
            D l = lambda.get(id);
            D m = this.m.get(id);
            Integer p = pi.get(id);
            D mp = this.m.get(p);

            // if L(i) >= M(i)
            if (l.compareTo(m) >= 0) {
                D min = mp.compareTo(l) <= 0 ? mp : l;
                // M(P(i)) = min { M(P(i)), L(i) }
                this.m.put(p, min);

                // L(i) = M(i)
                lambda.put(id, m);

                // P(i) = n+1;
                pi.put(id, newID);
            }

            else {
                D min = mp.compareTo(m) <= 0 ? mp : m;
                // M(P(i)) = min { M(P(i)), M(i) }
                this.m.put(p, min);
            }
        }
    }

    /**
     * Fourth step: Actualize the clusters if necessary
     *
     * @param newID        the id of the current object
     * @param processedIDs the already processed ids
     */
    private void step4(int newID, ArrayList<Integer> processedIDs) {
        // for i = 1..n
        for (Integer id : processedIDs) {
            if (id == newID)
                continue;

            D l = lambda.get(id);
            Integer p = pi.get(id);
            D lp = lambda.get(p);

            // if L(i) >= L(P(i))
            if (l.compareTo(lp) >= 0) {
                // P(i) = n+1
                pi.put(id, newID);
            }
        }
    }
}
