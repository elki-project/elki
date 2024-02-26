package elki.clustering.neighborhood.helper;

import java.util.List;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Mutual nearest neighbors set generator.
 * <p>
 * Reference:
 * <p>
 * C. H. Q. Ding, X. He<br>
 * K-nearest-neighbor consistency in data clustering: incorporating local
 * information into global optimization<br>
 * Proc. Symposium on Applied Computing (SAC) 2004
 *
 * @author Niklas Strahmann
 */
@Reference(authors = "C. H. Q. Ding, X. He", //
    title = "K-nearest-neighbor consistency in data clustering: incorporating local information into global optimization", //
    booktitle = "Proc. Symposium on Applied Computing (SAC) 2004", //
    url = "https://doi.org/10.1145/967900.968021", bibkey = "DBLP:conf/sac/DingH04")
public class MutualNeighborClosedNeighborhoodSetGenerator<O> extends AbstractClosedNeighborhoodSetGenerator<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MutualNeighborClosedNeighborhoodSetGenerator.class);

  private final int kPlus;

  private final Distance<? super O> distance;

  MutualNeighborQuery<DBIDRef> kmn;

  public MutualNeighborClosedNeighborhoodSetGenerator(int k, Distance<? super O> distance) {
    this.kPlus = k + 1; // plus self
    this.distance = distance;
  }

  @Override
  public List<DBIDs> getClosedNeighborhoods(Relation<? extends O> relation) {
    kmn = new MutualNeighborQueryBuilder<>(relation, distance).precomputed().byDBID(kPlus);
    return super.getClosedNeighborhoods(relation);
  }

  @Override
  protected DBIDs getNeighbors(DBIDRef element) {
    return kmn.getMutualNeighbors(element, kPlus);
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distance.getInputTypeRestriction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * 
   * @author Niklas Strahmann
   */
  public static class Par<O> extends AbstractClosedNeighborhoodSetGenerator.Par<O> {
    public static final OptionID K_NEIGHBORS = NearestNeighborClosedNeighborhoodSetGenerator.Par.K_NEIGHBORS;

    private int k;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_NEIGHBORS) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, p -> k = p);
    }

    @Override
    public MutualNeighborClosedNeighborhoodSetGenerator<O> make() {
      return new MutualNeighborClosedNeighborhoodSetGenerator<>(k, distance);
    }
  }
}
