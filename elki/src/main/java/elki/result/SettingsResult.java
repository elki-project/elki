/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import elki.utilities.optionhandling.parameterization.TrackedParameter;
import elki.utilities.optionhandling.parameters.ClassParameter;

/**
 * Result that keeps track of settings that were used in generating this
 * particular result.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class SettingsResult {
  /**
   * Settings storage.
   */
  Collection<SettingInformation> settings = new ArrayList<>();

  /**
   * Constructor.
   * 
   * @param settings Settings to store
   */
  public SettingsResult(Collection<TrackedParameter> settings) {
    super();
    Metadata.of(this).setLongName("Settings");
    // Retain only a string representation, in order to not leak references to
    // large objects via the database - beware of memory leak potential here!
    Object lastOwner = new Object();
    String ownername = "[unknown]";
    for(TrackedParameter setting : settings) {
      if(setting.getOwner() != lastOwner && setting.getOwner() != null) {
        try {
          if(setting.getOwner() instanceof Class) {
            ownername = ((Class<?>) setting.getOwner()).getName();
          }
          else {
            ownername = setting.getOwner().getClass().getName();
          }
          if(ClassParameter.class.isInstance(setting.getOwner())) {
            ownername = ((ClassParameter<?>) setting.getOwner()).getValue().getName();
          }
        }
        catch(NullPointerException e) {
          ownername = "[null]";
        }
        lastOwner = setting.getOwner();
      }
      // get name and value
      String name = setting.getParameter().getOptionID().getName();
      String value = "[unset]";
      try {
        if(setting.getParameter().isDefined()) {
          value = setting.getParameter().getValueAsString();
        }
      }
      catch(NullPointerException e) {
        value = "[null]";
      }
      this.settings.add(new SettingInformation(ownername, name, value));
    }
  }

  /**
   * Get the settings
   * 
   * @return the settings
   */
  public Collection<SettingInformation> getSettings() {
    return settings;
  }

  /**
   * Collect all settings results from a Result
   *
   * @param r Result
   * @return List of settings results
   */
  public static List<SettingsResult> getSettingsResults(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .filter(SettingsResult.class).collect(new ArrayList<>());
  }

  /**
   * Settings information.
   *
   * @author Erich Schubert
   */
  public static class SettingInformation {
    /**
     * Owner object of this setting
     */
    public String owner;

    /**
     * Parameter name
     */
    public String name;

    /**
     * Parameter value
     */
    public String value;

    /**
     * Constructor.
     *
     * @param owner Owner name
     * @param name Parameter name
     * @param value Value name
     */
    public SettingInformation(String owner, String name, String value) {
      this.owner = owner;
      this.name = name;
      this.value = value;
    }
  }
}
