package elki.clustering.kmedoids;

import elki.clustering.kmedoids.initialization.SemiSupervisedKMedoidsInitialization;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.distance.Distance;
import elki.logging.Logging;


public class SeededKmedoids<O> extends SemiSupervisedKMedoids<O> {
    /**x
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(SeededKmedoids.class);

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public SeededKmedoids(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
        super(distance, k, maxiter, initializer);
    }


    protected static class Instance extends SemiSupervisedKMedoids.Instance {

        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int numberOfLabels) {
            super(distQ, ids, assignment, labelsMaps, clusterLabel, numberOfLabels);
        }

        @Override
        protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
          return new FasterPAM.Instance(distQ, ids, assignment).run(medoids, maxiter);
        }
        

    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    @Override
    Instance instanceWrapper(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int noLabels) {
      return new Instance(distQ, ids, assignment, labelsMaps, clusterLabel, noLabels);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends SemiSupervisedKMedoids.Par<O> {
        @Override
        public SeededKmedoids<O> make() {
            return new SeededKmedoids<>(distance, k, maxiter, initializer);
        }
    }
}
