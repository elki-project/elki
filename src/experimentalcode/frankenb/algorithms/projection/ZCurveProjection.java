package experimentalcode.frankenb.algorithms.projection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurve;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.model.ifaces.IProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurveProjection<V extends NumberVector<V, ?>> implements IProjection<V> {
  /**
   * Constructor.
   *
   */
  public ZCurveProjection() {
    super();
  }

  @Override
  public Relation<V> project(Relation<V> dataSet) throws UnableToComplyException {
    try {
      // TODO: Double type instead of reusing the vector type?
      SimpleTypeInformation<V> type = new VectorFieldTypeInformation<V>(dataSet.getDataTypeInformation().getRestrictionClass(), 1);
      Relation<V> resultDataSet = new MaterializedRelation<V>(dataSet.getDatabase(), type, dataSet.getDBIDs());

      ZCurve.Transformer projection = new ZCurve.Transformer(dataSet, dataSet.getDBIDs());

      V factory = DatabaseUtil.assumeVectorField(dataSet).getFactory();

      for(DBID id : dataSet.iterDBIDs()) {
        double doubleProjection = projection.asBigInteger(dataSet.get(id)).doubleValue();
        resultDataSet.set(id, factory.newInstance(new double[] { doubleProjection }));
      }

      return resultDataSet;
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      throw new UnableToComplyException(e);
    }
  }
}