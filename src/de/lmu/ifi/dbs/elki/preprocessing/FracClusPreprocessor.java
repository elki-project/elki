package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.elki.utilities.DoublePair;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class FracClusPreprocessor<V extends RealVector<V, ?>> extends AbstractParameterizable implements Preprocessor<V> {
    public static final String NUMBER_OF_SUPPORTERS_P = "supporters";

    public static final String NUMBER_OF_SUPPORTERS_D = "number of supporters (at least 2)";

    private int k;

    private IntParameter kParameter = new IntParameter(NUMBER_OF_SUPPORTERS_P, NUMBER_OF_SUPPORTERS_D, new GreaterEqualConstraint(2));

    public FracClusPreprocessor() {
        super();
        optionHandler.put(kParameter);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.preprocessing.Preprocessor#run(de.lmu.ifi.dbs.elki.database.Database,boolean,boolean)
     */
    public void run(Database<V> database, boolean verbose, boolean time) {
        EuklideanDistanceFunction<V> distanceFunction = new EuklideanDistanceFunction<V>();
        distanceFunction.setDatabase(database, false, false); //  TODO: parameters verbose, time???
        if (verbose) {
            verbose("assigning database objects to base clusters");
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            List<Integer> neighbors = new ArrayList<Integer>(k);
            List<QueryResult<DoubleDistance>> kNN = database.kNNQueryForID(id, k + 1, distanceFunction);
            for (int i = 1; i < kNN.size(); i++) {
                QueryResult<DoubleDistance> ithQueryResult = kNN.get(i);
                neighbors.add(ithQueryResult.getID());
            }
            if (this.debug) {
                List<DoublePair> points = new ArrayList<DoublePair>(neighbors.size());
                for (int i = 1; i <= neighbors.size(); i++) {
                    points.add(new DoublePair(Math.log(distanceFunction.distance(neighbors.get(i - 1), id).getDoubleValue()), Math.log(i)));
                }
                double fractalDimension = new LinearRegression(points).getM();
                debugFine("Fractal Dimension of Point " + id + ": " + fractalDimension + " -- label: " + database.getAssociation(AssociationID.LABEL, id));
            }
            database.associate(AssociationID.NEIGHBORS, id, neighbors);
        }
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        k = getParameterValue(kParameter);
        return remainingParameters;
    }

    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        AttributeSettings myAttributeSettings = new AttributeSettings(this);
        myAttributeSettings.addSetting(NUMBER_OF_SUPPORTERS_P, Integer.toString(k));
        attributeSettings.add(myAttributeSettings);
        return attributeSettings;
    }

    public int getK() {
        return this.k;
    }

}
