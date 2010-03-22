package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Random-Sampling strategy for picking reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Vector type
 */
// TODO: ERICH: use reproducible Random
public class RandomSampleReferencePoints<O extends NumberVector<O, ?>> extends AbstractLoggable implements ReferencePointsHeuristic<O> {
  /**
   * OptionID for {@link #N_PARAM}
   */
  public static final OptionID N_ID = OptionID.getOrCreateOptionID("sample.n", "The number of samples to draw.");

  /**
   * Constant used in choosing optimal table sizes
   */
  private static final double log4 = Math.log(4);

  /**
   * Parameter to specify the sample size.
   * <p>
   * Key: {@code -sample.n}
   * </p>
   */
  private final IntParameter N_PARAM = new IntParameter(N_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #N_PARAM}.
   */
  private int samplesize;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RandomSampleReferencePoints(Parameterization config) {
    super();
    if(config.grab(N_PARAM)) {
      samplesize = N_PARAM.getValue();
    }
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    if(samplesize >= db.size()) {
      logger.warning("Sample size is larger than database size!");

      ArrayList<O> selection = new ArrayList<O>(db.size());
      for(Integer id : db) {
        selection.add(db.get(id));
      }
      return selection;
    }

    ArrayList<O> result = new ArrayList<O>(samplesize);
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
    List<Integer> ids = db.getIDs();
    boolean fastrandomaccess = false;
    if(ArrayList.class.isAssignableFrom(ids.getClass())) {
      fastrandomaccess = true;
    }
    if(samplesize <= setsize || !fastrandomaccess) {
      // use pool approach
      // if getIDs() is an array list, we don't need to copy it again.
      ArrayList<Integer> pool = ((ArrayList.class.isAssignableFrom(ids.getClass())) ? (ArrayList<Integer>) ids : new ArrayList<Integer>(ids));
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
}
