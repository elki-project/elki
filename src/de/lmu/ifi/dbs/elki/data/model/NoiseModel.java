package de.lmu.ifi.dbs.elki.data.model;

/**
 * Generic cluster model. Does not supply additional meta information except that it is "noise".
 * Since there is no meta information, you should use the static {@link NOISE} object.
 * 
 * @author Erich Schubert
 *
 */

public final class NoiseModel extends BaseModel {
  /**
   * Implementation of {@link Model}  interface
   */
  @Override
  public String getSuggestedLabel() {
    return "Noise";
  }
  
  /**
   * Static instance of this object that should be used instead of individual instances.
   */
  public final static NoiseModel NOISE = new NoiseModel();
}
