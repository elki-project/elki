package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Model for dendrograms, provides the distance to the child cluster.
 * 
 * @author Elke Achtert
 */
// TODO: comments
public class DendrogramModel<D extends Distance<D>> extends BaseModel {

  private D distance;

  public DendrogramModel(D distance) {
    super();
    this.distance = distance;
  }

  /**
   * @return the distance
   */
  public D getDistance() {
    return distance;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Distance to children: " + (distance != null ? distance.toString() : "null"));
  }

  @Override
  public String toString() {
    return "Distance to children: " + (distance != null ? distance.toString() : "null");
  }
}
