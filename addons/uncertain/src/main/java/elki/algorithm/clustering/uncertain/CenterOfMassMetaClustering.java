/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package elki.algorithm.clustering.uncertain;

import elki.algorithm.AbstractAlgorithm;
import elki.algorithm.clustering.ClusteringAlgorithm;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.data.uncertain.UncertainObject;
import elki.database.Database;
import elki.database.ProxyDatabase;
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Center-of-mass meta clustering reduces uncertain objects to their center of
 * mass, then runs a vector-oriented clustering algorithm on this data set.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Alexander Koos, Tobias Emrich, Andreas Züfle,
 * Klaus Arthur Schmid, Arthur Zimek<br>
 * A Framework for Clustering Uncertain Data<br>
 * In Proceedings of the VLDB Endowment, 8(12), 2015.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <C> Clustering result type (inherited from inner algorithm)
 */
@Reference(authors = "Erich Schubert, Alexander Koos, Tobias Emrich, Andreas Züfle, Klaus Arthur Schmid, Arthur Zimek", //
    title = "A Framework for Clustering Uncertain Data", //
    booktitle = "Proceedings of the VLDB Endowment, 8(12)", //
    url = "http://www.vldb.org/pvldb/vol8/p1976-schubert.pdf", //
    bibkey = "DBLP:journals/pvldb/SchubertKEZSZ15")
public class CenterOfMassMetaClustering<C extends Clustering<?>> extends AbstractAlgorithm<C> implements ClusteringAlgorithm<C> {
  /**
   * Initialize a Logger.
   */
  private static final Logging LOG = Logging.getLogger(CenterOfMassMetaClustering.class);

  /**
   * The algorithm to be wrapped and run.
   */
  protected ClusteringAlgorithm<C> inner;

  /**
   * Constructor, quite trivial.
   *
   * @param inner Primary clustering algorithm
   */
  public CenterOfMassMetaClustering(ClusteringAlgorithm<C> inner) {
    this.inner = inner;
  }

  /**
   * This run method will do the wrapping.
   * <p>
   * Its called from {@link AbstractAlgorithm#run(Database)} and performs the
   * call to the algorithms particular run method as well as the storing and
   * comparison of the resulting Clusterings.
   *
   * @param database Database
   * @param relation Data relation of uncertain objects
   * @return Clustering result
   */
  public C run(Database database, Relation<? extends UncertainObject> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    DBIDs ids = relation.getDBIDs();
    // Build a relation storing the center of mass:
    WritableDataStore<DoubleVector> store1 = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, DoubleVector.class);
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      store1.put(iter, relation.get(iter).getCenterOfMass());
    }
    return runClusteringAlgorithm(relation, ids, store1, dim, "Uncertain Model: Center of Mass");
  }

  /**
   * Run a clustering algorithm on a single instance.
   *
   * @param parent Parent result to attach to
   * @param ids Object IDs to process
   * @param store Input data
   * @param dim Dimensionality
   * @param title Title of relation
   * @return Clustering result
   */
  protected C runClusteringAlgorithm(Object parent, DBIDs ids, DataStore<DoubleVector> store, int dim, String title) {
    SimpleTypeInformation<DoubleVector> t = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    Relation<DoubleVector> sample = new MaterializedRelation<>(title, t, ids, store);
    ProxyDatabase d = new ProxyDatabase(ids, sample);
    C clusterResult = inner.run(d);
    ResultUtil.removeRecursive(sample);
    ResultUtil.removeRecursive(clusterResult);
    Metadata.hierarchyOf(parent).addChild(sample);
    Metadata.hierarchyOf(sample).addChild(clusterResult);
    return clusterResult;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(UncertainObject.UNCERTAIN_OBJECT_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return CenterOfMassMetaClustering.LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<C extends Clustering<?>> extends AbstractParameterizer {
    /**
     * Field to store the algorithm.
     */
    protected ClusteringAlgorithm<C> inner;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ClusteringAlgorithm<C>> palgorithm = new ObjectParameter<>(AbstractAlgorithm.ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(palgorithm)) {
        inner = palgorithm.instantiateClass(config);
        if(inner != null && inner.getInputTypeRestriction().length > 0 && //
            !inner.getInputTypeRestriction()[0].isAssignableFromType(TypeUtil.NUMBER_VECTOR_FIELD)) {
          config.reportError(new WrongParameterValueException(palgorithm, palgorithm.getValueAsString(), "The inner clustering algorithm (as configured) does not accept numerical vectors: " + inner.getInputTypeRestriction()[0]));
        }
      }
    }

    @Override
    protected CenterOfMassMetaClustering<C> makeInstance() {
      return new CenterOfMassMetaClustering<C>(inner);
    }
  }
}
