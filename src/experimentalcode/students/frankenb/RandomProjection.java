package experimentalcode.students.frankenb;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjection<V extends NumberVector<?>> {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(RandomProjection.class);

  public static final OptionID SPARSITY_ID = new OptionID("sparsity", "frequency of zeros in the projection matrix");

  private final IntParameter SPARSITY_PARAM = new IntParameter(SPARSITY_ID, 3);

  public static final OptionID NEW_DIMENSIONALITY_ID = new OptionID("newdimensionality", "amount of dimensions to project down to");

  private final IntParameter NEW_DIMENSIONALITY_PARAM = new IntParameter(NEW_DIMENSIONALITY_ID);

  private int sparsity;

  private int newDimensionality;

  public RandomProjection(Parameterization config) {
    if(config.grab(SPARSITY_PARAM)) {
      this.sparsity = SPARSITY_PARAM.getValue();
    }

    if(config.grab(NEW_DIMENSIONALITY_PARAM)) {
      this.newDimensionality = NEW_DIMENSIONALITY_PARAM.getValue();
    }
  }

  public Relation<V> project(Relation<V> dataSet) throws UnableToComplyException {
    if(RelationUtil.dimensionality(dataSet) <= this.newDimensionality) {
      throw new UnableToComplyException("New dimension is higher or equal to the old one!");
    }
    SimpleTypeInformation<V> type = new VectorFieldTypeInformation<V>(dataSet.getDataTypeInformation().getRestrictionClass(), this.newDimensionality);
    Relation<V> projectedDataSet = new MaterializedRelation<V>(dataSet.getDatabase(), type, dataSet.getDBIDs());

    Matrix projectionMatrix = new Matrix(this.newDimensionality, RelationUtil.dimensionality(dataSet));

    double possibilityOne = 1.0 / (double) sparsity;
    double baseValuePart = Math.sqrt(this.sparsity);

    Random random = new Random(System.currentTimeMillis());
    for(int i = 0; i < this.newDimensionality; ++i) {
      for(int j = 0; j < RelationUtil.dimensionality(dataSet); ++j) {
        double rnd = random.nextDouble();
        double value = baseValuePart;

        // possibility 1/s => * +1
        if(rnd < possibilityOne) {
        }
        else
        // possibility 1/s => * -1
        if(rnd < 2.0 * possibilityOne) {
          value *= -1;
        }
        else {
          // else * 0!
          value = 0;
        }

        projectionMatrix.set(i, j, value);
      }
    }

    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(dataSet);
    for(DBIDIter iditer = dataSet.iterDBIDs(); iditer.valid(); iditer.advance()) {
      V result = factory.newNumberVector(projectionMatrix.times(dataSet.get(iditer).getColumnVector()).getArrayRef());
      projectedDataSet.set(iditer, result);
    }

    if(LOG.isDebugging()) {
      LOG.debug("Projection Matrix:\n" + projectionMatrix.toString());
    }

    return projectedDataSet;
  }
}