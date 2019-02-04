/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;

/**
 * Result that keeps track of settings that were used in generating this
 * particular result.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class SettingsResult extends BasicResult {
  /**
   * Settings storage.
   */
  Collection<TrackedParameter> settings;

  /**
   * Constructor.
   * 
   * @param settings Settings to store
   */
  public SettingsResult(Collection<TrackedParameter> settings) {
    super("Settings", "settings");
    this.settings = settings;
  }

  /**
   * Get the settings
   * 
   * @return the settings
   */
  public Collection<TrackedParameter> getSettings() {
    return settings;
  }

  /**
   * Collect all settings results from a Result
   *
   * @param r Result
   * @return List of settings results
   */
  public static List<SettingsResult> getSettingsResults(Result r) {
    if(r instanceof SettingsResult) {
      List<SettingsResult> ors = new ArrayList<>(1);
      ors.add((SettingsResult) r);
      return ors;
    }
    if(r instanceof HierarchicalResult) {
      return ResultUtil.filterResults(((HierarchicalResult) r).getHierarchy(), r, SettingsResult.class);
    }
    return Collections.emptyList();
  }
}
