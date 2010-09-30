package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Result that keeps track of settings that were used in generating this
 * particular result.
 * 
 * @author Erich Schubert
 */
public class SettingsResult  extends TreeResult {
  /**
   * Settings storage.
   */
  Collection<Pair<Object, Parameter<?,?>>> settings;
  
  /**
   * Constructor.
   * 
   * @param settings Settings to store
   */
  public SettingsResult(Collection<Pair<Object, Parameter<?,?>>> settings) {
    super("Settings", "settings");
    this.settings = settings;
  }
  
  /**
   * Get the settings
   * @return the settings
   */
  public Collection<Pair<Object, Parameter<?,?>>> getSettings() {
    return settings;
  }
}