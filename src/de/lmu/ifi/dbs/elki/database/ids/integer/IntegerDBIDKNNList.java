package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;

/**
 * Combination interface for KNNList and IntegerDBIDs.
 * 
 * @author Erich Schubert
 */
public interface IntegerDBIDKNNList extends KNNList, DoubleDBIDList, IntegerDBIDs {
  @Override
  DoubleIntegerDBIDListIter iter();

  @Override
  public DoubleIntegerDBIDPair get(int index);
}
