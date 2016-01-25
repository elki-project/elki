package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.FourC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.PreDeConNeighborPredicate.PreDeConModel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * The PreDeCon core point predicate -- having at least minpts. neighbors, and a
 * maximum preference dimensionality of lambda.
 * 
 * <p>
 * Reference:
 * <p>
 * C. Böhm, K. Kailing, P. Kröger, A. Zimek:<br />
 * Computing Clusters of Correlation Connected Objects. <br>
 * In Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.has Instance
 */
@Reference(authors = "C. Böhm, K. Kailing, P. Kröger, A. Zimek", //
title = "Computing Clusters of Correlation Connected Objects", //
booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466", //
url = "http://dx.doi.org/10.1145/1007568.1007620")
public class FourCCorePredicate implements CorePredicate {
  /**
   * The PreDeCon settings class.
   */
  protected FourC.Settings settings;

  /**
   * Default constructor.
   * 
   * @param settings PreDeCon settings
   */
  public FourCCorePredicate(FourC.Settings settings) {
    super();
    this.settings = settings;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> CorePredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    return (CorePredicate.Instance<T>) new Instance(settings);
  }

  @Override
  public boolean acceptsType(SimpleTypeInformation<?> type) {
    return (type.getRestrictionClass().isAssignableFrom(PreDeConModel.class));
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements CorePredicate.Instance<PreDeConModel> {
    /**
     * The PreDeCon settings class.
     */
    protected FourC.Settings settings;

    /**
     * Constructor for this predicate.
     * 
     * @param settings PreDeCon settings
     */
    public Instance(FourC.Settings settings) {
      super();
      this.settings = settings;
    }

    @Override
    public boolean isCorePoint(DBIDRef point, PreDeConModel model) {
      return (model.pdim <= settings.lambda) && (model.ids.size() >= settings.minpts);
    }
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
     * The PreDeCon settings class.
     */
    protected FourC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      settings = config.tryInstantiate(FourC.Settings.class);
    }

    @Override
    protected FourCCorePredicate makeInstance() {
      return new FourCCorePredicate(settings);
    }
  }
}