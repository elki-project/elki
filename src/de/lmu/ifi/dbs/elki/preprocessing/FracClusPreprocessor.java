package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;

/**
 * @author Arthur Zimek
 * @param <V> Vector type
 */
public class FracClusPreprocessor<V extends RealVector<V, ?>> extends AbstractParameterizable implements Preprocessor<V> {
    private int k;

    /**
     * OptionID for {@link #NUMBER_OF_SUPPORTERS_PARAM}
     */
    public static final OptionID NUMBER_OF_SUPPORTERS_ID = OptionID.getOrCreateOptionID("supporters",
        "number of supporters (at least 2)");

    private IntParameter NUMBER_OF_SUPPORTERS_PARAM = new IntParameter(NUMBER_OF_SUPPORTERS_ID,
        new GreaterEqualConstraint(2));

    /**
     * Constructor
     */
    public FracClusPreprocessor() {
        super();
        addOption(NUMBER_OF_SUPPORTERS_PARAM);
    }

    public void run(Database<V> database, boolean verbose, boolean time) {
        EuclideanDistanceFunction<V> distanceFunction = new EuclideanDistanceFunction<V>();
        distanceFunction.setDatabase(database, verbose, time);
        if (verbose) {
          logger.verbose("assigning database objects to base clusters");
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            List<Integer> neighbors = new ArrayList<Integer>(k);
            List<DistanceResultPair<DoubleDistance>> kNN = database.kNNQueryForID(id, k + 1, distanceFunction);
            for (int i = 1; i < kNN.size(); i++) {
                DistanceResultPair<DoubleDistance> ithQueryResult = kNN.get(i);
                neighbors.add(ithQueryResult.getID());
            }
            if (logger.isDebugging()) {
                List<DoubleDoublePair> points = new ArrayList<DoubleDoublePair>(neighbors.size());
                for (int i = 1; i <= neighbors.size(); i++) {
                    points.add(new DoubleDoublePair(Math.log(distanceFunction.distance(neighbors.get(i - 1), id).getValue()), Math.log(i)));
                }
                double fractalDimension = new LinearRegression(points).getM();
                logger.debugFine("Fractal Dimension of Point " + id + ": " + fractalDimension + " -- label: " + database.getAssociation(AssociationID.LABEL, id));
            }
            database.associate(AssociationID.NEIGHBOR_IDS, id, neighbors);
        }
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        k = NUMBER_OF_SUPPORTERS_PARAM.getValue();
        return remainingParameters;
    }

    /**
     * get k
     * @return k
     */
    public int getK() {
        return this.k;
    }

}
