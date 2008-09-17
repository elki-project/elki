package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.elki.preprocessing.FracClusPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.DoublePair;
import de.lmu.ifi.dbs.elki.utilities.KNNList;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
//  todo arthur comment all
public class FractalDimensionBasedDistanceFunction<V extends RealVector<V, ?>>
    extends AbstractPreprocessorBasedDistanceFunction<V, FracClusPreprocessor<V>, DoubleDistance> {

    public final EuclideanDistanceFunction<V> STANDARD_DOUBLE_DISTANCE_FUNCTION = new EuclideanDistanceFunction<V>();

    public FractalDimensionBasedDistanceFunction() {
        super(Pattern.compile(new EuclideanDistanceFunction<V>().requiredInputPattern()));
    }

    public DoubleDistance distance(V v1, V v2) {
        List<Integer> neighbors1 = this.getDatabase().getAssociation(this.getAssociationID(), v1.getID());
        List<Integer> neighbors2 = this.getDatabase().getAssociation(this.getAssociationID(), v2.getID());

        Set<Integer> supporters = new HashSet<Integer>();
        supporters.addAll(neighbors1);
        supporters.addAll(neighbors2);

        V centroid = v1.plus(v2).multiplicate(0.5);

        KNNList<DoubleDistance> knnList = new KNNList<DoubleDistance>(this.getPreprocessor().getK(), STANDARD_DOUBLE_DISTANCE_FUNCTION.infiniteDistance());
        for (Integer id : supporters) {
            knnList.add(new QueryResult<DoubleDistance>(id, STANDARD_DOUBLE_DISTANCE_FUNCTION.distance(id, centroid)));
        }

        List<DoubleDistance> distances = new ArrayList<DoubleDistance>();

        for (QueryResult<DoubleDistance> qr : knnList.toList()) {
            distances.add(qr.getDistance());
        }

        List<DoublePair> points = new ArrayList<DoublePair>(distances.size());
        for (int i = 0; i < distances.size(); i++) {
            points.add(new DoublePair(Math.log(distances.get(i).getValue()), Math.log(i + 1)));
        }
        return new DoubleDistance(new LinearRegression(points).getM());
    }

    public DoubleDistance infiniteDistance() {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.infiniteDistance();
    }

    public DoubleDistance nullDistance() {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.nullDistance();
    }

    public DoubleDistance undefinedDistance() {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.undefinedDistance();
    }

    public DoubleDistance valueOf(String pattern) throws IllegalArgumentException {
        return STANDARD_DOUBLE_DISTANCE_FUNCTION.valueOf(pattern);
    }

    /**
     * @return the association ID for the association to be set by the preprocessor,
     *         which is {@link AssociationID#NEIGHBOR_IDS}
     */
    public AssociationID<List<Integer>> getAssociationID() {
        return AssociationID.NEIGHBOR_IDS;
    }

    /**
     * @return the name of the default preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.FracClusPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return FracClusPreprocessor.class.getName();
    }

    public String getPreprocessorDescription() {
        return this.optionHandler.usage("");
    }

    /**
     * @return the super class for the preprocessor parameter,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.FracClusPreprocessor}
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return FracClusPreprocessor.class;
    }

    @Override
    public void setDatabase(Database<V> database, boolean verbose, boolean time) {
        super.setDatabase(database, verbose, time);
        STANDARD_DOUBLE_DISTANCE_FUNCTION.setDatabase(this.getDatabase(), verbose, time);
    }


}
