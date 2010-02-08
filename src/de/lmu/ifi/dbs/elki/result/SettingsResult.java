package de.lmu.ifi.dbs.elki.result;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Result that keeps track of settings that were used in generating this
 * particular result.
 * 
 * @author Erich Schubert
 * 
 */
public class SettingsResult implements Result {
  /**
   * Settings storage.
   */
  List<Pair<Parameterizable, Option<?>>> settings;
  
  /**
   * Constructor.
   * 
   * @param settings Settings to store
   */
  public SettingsResult(List<Pair<Parameterizable, Option<?>>> settings) {
    super();
    this.settings = settings;
  }
  
  /**
   * Get the settings
   * @return the settings
   */
  public List<Pair<Parameterizable, Option<?>>> getSettings() {
    return settings;
  }

  @Override
  public String getName() {
    return "settings";
  }
}
