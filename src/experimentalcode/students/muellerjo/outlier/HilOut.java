package experimentalcode.students.muellerjo.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * @author Jonathan von Brünken
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class HilOut<O  extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HilOut.class);
  
  /**
   * Parameter to specify how many next neighbors should be used in the computation
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("HilOut.k", "Compute up to k next neighbors");
  
  /**
   * Parameter to specify how many outliers should be computed
   */
  public static final OptionID N_ID = OptionID.getOrCreateOptionID("HilOut.n", "Compute n outliers");
  
  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;
  
  /**
   * Holds the value of {@link #N_ID}.
   */
  private int n;
  
  protected HilOut(DistanceFunction<? super O, D> distanceFunction, int k, int n) {
    super(distanceFunction);
    this.n = n;
    this.k = k;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
  
  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }
  
  public static class Parameterizer<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {

    protected int k = 5;
    
    protected int n = 10;
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      
      final IntParameter kP = new IntParameter(K_ID, 5);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      
      final IntParameter nP = new IntParameter(N_ID, 10);
      if(config.grab(nP)) {
        n = nP.getValue();
      }
    }

    @Override
    protected HilOut<O, D> makeInstance() {
      return new HilOut<O, D>(distanceFunction, k, n);
    }
  }

}
