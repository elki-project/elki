package experimentalcode.erich.data.images;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

public interface ComputeColorHistogram extends Parameterizable {
  public double[] computeColorHistogram(File file) throws IOException;
}