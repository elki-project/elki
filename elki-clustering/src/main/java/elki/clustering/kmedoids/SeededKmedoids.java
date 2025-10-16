/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.kmedoids;

import elki.clustering.kmedoids.initialization.LabelMedoidInitialization;
import elki.clustering.kmedoids.initialization.SemiSupervisedKMedoidsInitialization;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.distance.Distance;
import elki.logging.Logging;

/**
 *
 * Implementation of the Seeded k-medoids algorithm that uses a pre-computed set of medoids
 * as initial seeds for the clustering process. This approach leverages existing
 * knowledge about cluster centers to improve convergence.
 *
 * @author Miriama Janosova
 * @author Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
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
     * @author Andreas Lang
     */
    public static class Par<O> extends SemiSupervisedKMedoids.Par<O> {

        @Override
        @SuppressWarnings("rawtypes")
        protected Class<? extends SemiSupervisedKMedoidsInitialization> defaultInitializer() {
          return LabelMedoidInitialization.class;
        }

        @Override
        public SeededKmedoids<O> make() {
            return new SeededKmedoids<>(distance, k, maxiter, initializer);
        }
    }
}
