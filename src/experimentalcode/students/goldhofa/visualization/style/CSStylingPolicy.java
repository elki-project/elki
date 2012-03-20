package experimentalcode.students.goldhofa.visualization.style;

import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;

/** 
 * Die CS Vis will also eine eigene stylingpolicy basierend auf singleobjectstyling?
 * Genau so ist das gedacht. Das erzeugt eine eigene styling policy, und setzt diese dann im StylingResult als aktiv.
 * Dann löst sie ein "ResultChanged" für das StylingResult aus, und die anderen Visualizer zeichnen neu
 * 
 * @author Sascha Goldhofer
 *
 */
public class CSStylingPolicy implements StylingPolicy {
  
  TreeMap<DBID, Integer> selection;
  
  public CSStylingPolicy() {
    selection = new TreeMap<DBID, Integer>();
  }
  
  public void deselectObject(DBID id) {
    //System.out.println("removing object "+id.toString());
    this.selection.remove(id);
  }
  
  public void selectObject(DBID id, int color) {
    //System.out.println("adding object "+id.toString()+" ("+color+")");
    this.selection.put(id, color);
  }
  
  public void setSelectedDBIDs(TreeMap<String, DBIDs> objectSelection) {
  }

  @Override
  public int getColorForDBID(DBID id) {
    //System.out.println("CSStylingPolicy distributing color");
    if (this.selection.containsKey(id)) {
      return this.selection.get(id);
    }
    return 0;
  }
}
