package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Random-Sampling strategy for picking reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class RandomSampleReferencePoints<V extends NumberVector<? extends V, ?>> implements ReferencePointsHeuristic<V> {
  // TODO: use reproducible Random

  /**
   * Parameter to specify the sample size.
   * <p>
   * Key: {@code -sample.n}
   * </p>
   */
  public static final OptionID N_ID = OptionID.getOrCreateOptionID("sample.n", "The number of samples to draw.");

  /**
   * Constant used in choosing optimal table sizes
   */
  private static final double log4 = Math.log(4);

  /**
   * Holds the value of {@link #N_ID}.
   */
  private int samplesize;

  /**
   * Constructor.
   * 
   * @param samplesize
   */
  public RandomSampleReferencePoints(int samplesize) {
    super();
    this.samplesize = samplesize;
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Relation<T> db) {
    if(samplesize >= db.size()) {
      LoggingUtil.warning("Sample size is larger than database size!");

      ArrayList<V> selection = new ArrayList<V>(db.size());
      for(DBID id : db.iterDBIDs()) {
        selection.add(db.get(id));
      }
      return selection;
    }

    ArrayList<V> result = new ArrayList<V>(samplesize);
    int dbsize = db.size();

    // Guess the memory requirements of a hashmap.
    // The values are based on Python code, and might need modification for
    // Java!
    // If the hashmap is likely to become too big, lazy-shuffle a list instead.
    int setsize = 21;
    if(samplesize > 5) {
      setsize += 2 << (int) Math.ceil(Math.log(samplesize * 3) / log4);
    }
    // logger.debug("Setsize: "+setsize);
    ArrayDBIDs ids = DBIDUtil.ensureArray(db.getDBIDs());
    boolean fastrandomaccess = false;
    if(ArrayList.class.isAssignableFrom(ids.getClass())) {
      fastrandomaccess = true;
    }
    if(samplesize <= setsize || !fastrandomaccess) {
      // use pool approach
      // if getIDs() is an array list, we don't need to copy it again.
      ArrayModifiableDBIDs pool = ((ArrayModifiableDBIDs.class.isAssignableFrom(ids.getClass())) ? (ArrayModifiableDBIDs) ids : DBIDUtil.newArray(ids));
      for(int i = 0; i < samplesize; i++) {
        int j = (int) Math.floor(Math.random() * (dbsize - i));
        result.add(db.get(pool.get(j)));
        pool.set(j, pool.get(dbsize - i - 1));
      }
      ids = null; // dirty!
    }
    else {
      HashSet<Integer> selected = new HashSet<Integer>();
      for(int i = 0; i < samplesize; i++) {
        int j = (int) Math.floor(Math.random() * dbsize);
        // Redraw from pool.
        while(selected.contains(j)) {
          j = (int) Math.floor(Math.random() * dbsize);
        }
        selected.add(j);
        result.add(db.get(ids.get(j)));
      }
    }
    assert (result.size() == samplesize);
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<? extends V, ?>> extends AbstractParameterizer {
    /**
     * Holds the value of {@link #N_ID}.
     */
    protected int samplesize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter samplesizeP = new IntParameter(N_ID, new GreaterConstraint(0));
      if(config.grab(samplesizeP)) {
        samplesize = samplesizeP.getValue();
      }
    }

    @Override
    protected RandomSampleReferencePoints<V> makeInstance() {
      return new RandomSampleReferencePoints<V>(samplesize);
    }
  }
}