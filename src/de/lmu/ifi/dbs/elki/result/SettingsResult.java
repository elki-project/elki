package de.lmu.ifi.dbs.elki.result;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
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
  List<Pair<Parameterizable, Parameter<?,?>>> settings;
  
  /**
   * Constructor.
   * 
   * @param settings Settings to store
   */
  public SettingsResult(List<Pair<Parameterizable, Parameter<?,?>>> settings) {
    super();
    this.settings = settings;
  }
  
  /**
   * Get the settings
   * @return the settings
   */
  public List<Pair<Parameterizable, Parameter<?,?>>> getSettings() {
    return settings;
  }

  @Override
  public String getName() {
    return "settings";
  }
}
