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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.*;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Utility wrapper to track parameters for a configuration session.
 *
 * All actual Parameterization operations are forwarded to the inner class.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @composed - - - TrackedParameter
 */
public class TrackParameters implements Parameterization {
  /**
   * Inner parameterization
   */
  Parameterization inner;

  /**
   * Tracking storage
   */
  List<TrackedParameter> options = new ArrayList<>();

  /**
   * Tree information: parent links
   */
  Map<Object, Object> parents = new HashMap<>();

  /**
   * Tree information: child links
   */
  // Implementation note: we need the map to support "null" keys!
  Map<Object, List<Object>> children = new HashMap<>();

  /**
   * Current parent for nested parameterization
   */
  Object owner = null;

  /**
   * Constructor.
   *
   * @param inner Inner parameterization to wrap.
   */
  public TrackParameters(Parameterization inner) {
    super();
    this.inner = inner;
  }

  /**
   * Constructor.
   *
   * @param inner Inner parameterization to wrap.
   * @param owner Class/instance owning the parameter
   */
  public TrackParameters(Parameterization inner, Object owner) {
    super();
    this.inner = inner;
    this.owner = owner;
  }

  /**
   * Internal constructor, for nested tracking.
   *
   * @param inner Inner parameterization
   * @param owner Object owning the current parameters
   * @param options List of options
   * @param parents Parent map
   * @param children Child map
   */
  private TrackParameters(Parameterization inner, Object owner, List<TrackedParameter> options, Map<Object, Object> parents, Map<Object, List<Object>> children) {
    super();
    this.inner = inner.descend(owner);
    this.owner = owner;
    this.options = options;
    this.parents = parents;
    this.children = children;
  }

  @Override
  public Collection<ParameterException> getErrors() {
    return inner.getErrors();
  }

  @Override
  public boolean grab(Parameter<?> opt) {
    registerChild(opt);
    options.add(new TrackedParameter(owner, opt));
    return inner.grab(opt);
  }

  @Override
  public boolean hasUnusedParameters() {
    return inner.hasUnusedParameters();
  }

  @Override
  public void reportError(ParameterException e) {
    inner.reportError(e);
  }

  @Override
  public boolean setValueForOption(Parameter<?> opt) throws ParameterException {
    registerChild(opt);
    options.add(new TrackedParameter(owner, opt));
    return inner.setValueForOption(opt);
  }

  /**
   * Get all seen parameters, set or unset, along with their owner objects.
   *
   * @return Parameters seen
   */
  public Collection<TrackedParameter> getAllParameters() {
    return options;
  }

  /**
   * {@inheritDoc}
   *
   * Track parameters using a shared options list with parent tracker.
   */
  @Override
  public Parameterization descend(Object option) {
    registerChild(option);
    return new TrackParameters(inner, option, options, parents, children);
  }

  private void registerChild(Object opt) {
    if(opt == owner) {
      LoggingUtil.exception("Options shouldn't have themselves as parents!", new Throwable());
    }
    parents.put(opt, owner);
    List<Object> c = children.get(owner);
    if(c == null) {
      c = new ArrayList<>();
      children.put(owner, c);
    }
    if(!c.contains(opt)) {
      c.add(opt);
    }
  }

  /**
   * Traverse the tree upwards.
   *
   * @param pos Current object
   * @return Parent object
   */
  public Object getParent(Object pos) {
    return parents.get(pos);
  }
}