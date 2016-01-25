package de.lmu.ifi.dbs.elki.application.jsmap;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Handle results by serving them via a web server to mapping applications.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.composedOf JSONWebServer
 */
public class JSONResultHandler implements ResultHandler {
  /**
   * The actual web server
   */
  private JSONWebServer server;

  /**
   * Listen port
   */
  private int port;

  /**
   * Constructor.
   * 
   * @param port Port to listen on
   */
  public JSONResultHandler(int port) {
    super();
    this.port = port;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    if(server == null) {
      server = new JSONWebServer(port, hier, newResult);
    }

    // TODO: stop somehow. UI with stop button?
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Port to use for listening
     */
    public static final OptionID PORT_ID = new OptionID("json.port", "Port for the JSON web server to listen on.");

    /**
     * Our port
     */
    int port = 8080;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter portP = new IntParameter(PORT_ID, port);
      portP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      portP.addConstraint(new LessEqualConstraint(65535));
      if(config.grab(portP)) {
        this.port = portP.getValue();
      }
    }

    @Override
    protected JSONResultHandler makeInstance() {
      return new JSONResultHandler(port);
    }
  }
}