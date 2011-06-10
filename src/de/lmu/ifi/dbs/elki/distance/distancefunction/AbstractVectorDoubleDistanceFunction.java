package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Abstract base class for the most common family of distance functions: defined
 * on number vectors and returning double values.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public abstract class AbstractVectorDoubleDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements PrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * Constructor.
   */
  public AbstractVectorDoubleDistanceFunction() {
    super();
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public final DoubleDistance distance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    return new DoubleDistance(doubleDistance(o1, o2));
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}