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
package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * A panel that contains configurators for parameters.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @has - - - de.lmu.ifi.dbs.elki.gui.configurator.ParameterConfigurator
 */
public class ConfiguratorPanel extends JPanel implements ChangeListener {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Keep a map of parameter
   */
  private Map<Object, ParameterConfigurator> childconfig = new HashMap<>();

  /**
   * Child options
   */
  private ArrayList<ParameterConfigurator> children = new ArrayList<>();

  /**
   * The event listeners for this panel.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor.
   */
  public ConfiguratorPanel() {
    super(new GridBagLayout());
  }

  /**
   * Add parameter to this panel.
   * 
   * @param param Parameter to add
   * @param track Parameter tracking object
   */
  public void addParameter(Object owner, Parameter<?> param, TrackParameters track) {
    this.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
    ParameterConfigurator cfg = null;
    { // Find
      Object cur = owner;
      while(cur != null) {
        cfg = childconfig.get(cur);
        if(cfg != null) {
          break;
        }
        cur = track.getParent(cur);
      }
    }
    if(cfg != null) {
      cfg.addParameter(owner, param, track);
      return;
    }
    else {
      cfg = makeConfigurator(param);
      cfg.addChangeListener(this);
      children.add(cfg);
    }
  }

  private ParameterConfigurator makeConfigurator(Parameter<?> param) {
    if(param instanceof Flag) {
      return new FlagParameterConfigurator((Flag) param, this);
    }
    if(param instanceof ClassListParameter) {
      ParameterConfigurator cfg = new ClassListParameterConfigurator((ClassListParameter<?>) param, this);
      childconfig.put(param, cfg);
      return cfg;
    }
    if(param instanceof ClassParameter) {
      ParameterConfigurator cfg = new ClassParameterConfigurator((ClassParameter<?>) param, this);
      childconfig.put(param, cfg);
      return cfg;
    }
    if(param instanceof FileParameter) {
      return new FileParameterConfigurator((FileParameter) param, this);
    }
    if(param instanceof EnumParameter) {
      return new EnumParameterConfigurator((EnumParameter<?>) param, this);
    }
    return new TextParameterConfigurator(param, this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if(e.getSource() instanceof ParameterConfigurator) {
      // TODO: check that e is in children?
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("stateChanged triggered by unknown source: " + e.getSource());
    }
  }

  public void addChangeListener(ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  protected void fireValueChanged() {
    ChangeEvent evt = new ChangeEvent(this);
    for(ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(evt);
    }
  }

  public void appendParameters(ListParameterization params) {
    for(ParameterConfigurator cfg : children) {
      cfg.appendParameters(params);
    }
  }

  public void clear() {
    removeAll();
    childconfig.clear();
    children.clear();
  }
}