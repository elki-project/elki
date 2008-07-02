package de.lmu.ifi.dbs.elki.algorithm.result.outlier;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DimensionsSelectingEuklideanDistanceFunction;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.BitSet;
import java.util.List;

/**
 * todo arthur comment
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */
public class SODModel<O extends RealVector<O, Double>> extends AbstractResult<O> {

    private final DimensionsSelectingEuklideanDistanceFunction<O> DISTANCE_FUNCTION = new DimensionsSelectingEuklideanDistanceFunction<O>();

    private double[] centerValues;

    private double[] variances;

    private double expectationOfVariance;

    private BitSet weightVector;

    private double sod;

    public SODModel(Database<O> database, List<Integer> neighborhood, double alpha, O queryObject) {
        super(database);
        centerValues = new double[database.dimensionality()];
        variances = new double[centerValues.length];
        for (Integer id : neighborhood) {
            O databaseObject = database.get(id);
            for (int d = 0; d < centerValues.length; d++) {
                centerValues[d] += databaseObject.getValue(d + 1);
            }
        }
        for (int d = 0; d < centerValues.length; d++) {
            centerValues[d] /= neighborhood.size();
        }
        for (Integer id : neighborhood) {
            O databaseObject = database.get(id);
            for (int d = 0; d < centerValues.length; d++) {
                // distance
                double distance = centerValues[d] - databaseObject.getValue(d + 1);
                // variance
                variances[d] += distance * distance;
            }
        }
        expectationOfVariance = 0;
        for (int d = 0; d < variances.length; d++) {
            variances[d] /= neighborhood.size();
            expectationOfVariance += variances[d];
        }
        expectationOfVariance /= variances.length;
        weightVector = new BitSet(variances.length);
        for (int d = 0; d < variances.length; d++) {
            if (variances[d] < alpha * expectationOfVariance) {
                weightVector.set(d, true);
            }
        }
        DISTANCE_FUNCTION.setSelectedDimensions(weightVector);
        sod = subspaceOutlierDegree(queryObject);
    }

    public double subspaceOutlierDegree(O queryObject) {
        O center = queryObject.newInstance(centerValues);
        double distance = DISTANCE_FUNCTION.distance(queryObject, center).getDoubleValue();
        distance /= weightVector.cardinality();
        return distance;
    }


    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.result.Result#output(java.io.PrintStream,de.lmu.ifi.dbs.elki.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        outStream.println("### " + this.getClass().getSimpleName() + ":");
        outStream.println("### relevant attributes (counting starts with 0): " + this.weightVector.toString());
        outStream.println("### center of neighborhood: " + Util.format(centerValues));
        outStream.println("### subspace outlier degree: " + this.sod);
        outStream.println("################################################################################");
        outStream.flush();
    }

    public double getSod() {
        return this.sod;
    }

}
