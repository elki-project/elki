package elki.clustering.neighborhood.helper;

import java.util.List;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * 
 * @author Niklas Strahmann
 */
public class EpsNeighborClosedNeighborhoodSetGenerator<O> extends AbstractClosedNeighborhoodSetGenerator<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(EpsNeighborClosedNeighborhoodSetGenerator.class);

  private final Distance<? super O> distance;

  double eps;

  RangeSearcher<DBIDRef> rangeSearcher;

  public EpsNeighborClosedNeighborhoodSetGenerator(Distance<? super O> distance, double eps) {
    this.distance = distance;
    this.eps = eps;
  }

  @Override
  public List<DBIDs> getClosedNeighborhoods(Relation<? extends O> relation) {
    rangeSearcher = new QueryBuilder<>(relation, distance).precomputed().rangeByDBID(eps);
    return super.getClosedNeighborhoods(relation);
  }

  @Override
  protected DBIDs getNeighbors(DBIDRef element) {
    return rangeSearcher.getRange(element, eps);
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
    public static final OptionID EPSILON_ID = new OptionID("neighborhood.eps", "The maximum radius of the neighborhood");

    protected double eps;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> eps = x);
    }

    @Override
    public EpsNeighborClosedNeighborhoodSetGenerator<O> make() {
      return new EpsNeighborClosedNeighborhoodSetGenerator<>(distance, eps);
    }
  }

}
