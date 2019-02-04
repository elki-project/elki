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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Class that allows chaining multiple parameterizations.
 * This is designed to allow overriding of some parameters for an algorithm,
 * while other can be configured via different means, e.g. given by the
 * user on the command line.
 * 
 * See {@link de.lmu.ifi.dbs.elki.utilities.optionhandling} package documentation
 * for examples.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class ChainedParameterization extends AbstractParameterization {
  /**
   * Keep the list of parameterizations.
   */
  private List<Parameterization> chain = new ArrayList<>();
  
  /**
   * Error target
   */
  private Parameterization errorTarget = this;

  /**
   * Constructor that takes a number of Parameterizations to chain.
   * 
   * @param ps Parameterizations
   */
  public ChainedParameterization(Parameterization... ps) {
    for(Parameterization p : ps) {
      chain.add(p);
    }
  }

  /**
   * Append a new Parameterization to the chain.
   * 
   * @param p Parameterization
   */
  public void appendParameterization(Parameterization p) {
    chain.add(p);
  }
  
  @Override
  public boolean setValueForOption(Parameter<?> opt) throws ParameterException {
    for(Parameterization p : chain) {
      if(p.setValueForOption(opt)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasUnusedParameters() {
    for(Parameterization p : chain) {
      if(p.hasUnusedParameters()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Set the error target, since there is no unique way where
   * errors can be reported.
   * 
   * @param config Parameterization to report errors to
   */
  public void errorsTo(Parameterization config) {
    this.errorTarget = config;
  }

  @Override
  public void reportError(ParameterException e) {
    if (this.equals(this.errorTarget)) {
      super.reportError(e);
    } else {
      this.errorTarget.reportError(e);
    }
  }

  /** {@inheritDoc}
   * Parallel descend in all chains.
   */
  @Override
  public Parameterization descend(Object option) {
    ChainedParameterization n = new ChainedParameterization();
    n.errorsTo(this.errorTarget);
    for (Parameterization p : this.chain) {
      n.appendParameterization(p.descend(option));
    }
    return n;
  }
}