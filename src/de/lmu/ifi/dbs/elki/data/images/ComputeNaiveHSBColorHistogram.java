package de.lmu.ifi.dbs.elki.data.images;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Compute color histograms in a Hue-Saturation-Brightness model.
 * 
 * @author Erich Schubert
 */
public class ComputeNaiveHSBColorHistogram extends ComputeHSBColorHistogram {
  /**
   * Parameter that specifies the number of bins (per plane) to use.
   * 
   * <p>
   * Key: {@code -hsbhist.bpp}
   * </p>
   */
  public static final OptionID BINSPERPLANE_ID = OptionID.getOrCreateOptionID("hsbhist.bpp", "Bins per plane for HSV/HSB histogram. This will result in bpp ** 3 bins.");

  /**
   * Constructor.
   * 
   * @param quant Number of bins to use.
   */
  public ComputeNaiveHSBColorHistogram(int quant) {
    super(quant, quant, quant);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected int quant = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter param = new IntParameter(BINSPERPLANE_ID);
      if(config.grab(param)) {
        quant = param.getValue();
      }
    }

    @Override
    protected ComputeNaiveHSBColorHistogram makeInstance() {
      return new ComputeNaiveHSBColorHistogram(quant);
    }
  }
}