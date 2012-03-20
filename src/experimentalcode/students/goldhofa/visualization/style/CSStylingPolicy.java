package experimentalcode.students.goldhofa.visualization.style;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;

/** 
 * Die CS Vis will also eine eigene stylingpolicy basierend auf singleobjectstyling?
 * Genau so ist das gedacht. Das erzeugt eine eigene styling policy, und setzt diese dann im StylingResult als aktiv.
 * Dann löst sie ein "ResultChanged" für das StylingResult aus, und die anderen Visualizer zeichnen neu
 * 
 * @author Gott
 *
 */
public class CSStylingPolicy implements StylingPolicy {

  @Override
  public int getColorForDBID(DBID id) {
    
    
    
    return 0;
  }

}
