package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This filter assigns static DBIDs, based on the sequence the objects appear in
 * the bundle by adding a column of DBID type to the bundle.
 * 
 * @author Erich Schubert
 */
public class FixedDBIDsFilter implements ObjectFilter {
  /**
   * Optional parameter to specify the first object ID to use.
   * <p>
   * Key: {@code -dbc.startid}
   * </p>
   */
  public static final OptionID IDSTART_ID = OptionID.getOrCreateOptionID("dbc.startid", "Object ID to start counting with");

  /**
   * The first ID to assign
   */
  final int startid;

  /**
   * Constructor.
   * 
   * @param startid ID to start enumerating with.
   */
  public FixedDBIDsFilter(int startid) {
    super();
    this.startid = startid;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    List<DBID> ids = new ArrayList<DBID>(objects.dataLength());
    for(int i = 0; i < objects.dataLength(); i++) {
      ids.add(DBIDUtil.importInteger(startid + i));
    }
    bundle.appendColumn(TypeUtil.DBID, ids);
    // copy other columns
    for(int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), objects.getColumn(j));
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    int startid = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter startidParam = new IntParameter(IDSTART_ID);
      if(config.grab(startidParam)) {
        startid = startidParam.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new FixedDBIDsFilter(startid);
    }
  }
}