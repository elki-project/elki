package experimentalcode.erich.minigui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class SavedSettingsFile implements Iterable<Pair<String, ArrayList<String>>> {
  public static final String COMMENT_PREFIX = "#";

  private File file;
  
  private ArrayList<Pair<String, ArrayList<String>>> store;

  public SavedSettingsFile(String filename) {
    super();
    this.file = new File(filename);
    this.store = new ArrayList<Pair<String, ArrayList<String>>>();
  }
  
  public void save() throws IOException {
    PrintStream p = new PrintStream(file);
    p.println("# Save ELKI settings. First line is title, remaining lines are parameters.");
    for (Pair<String, ArrayList<String>> settings : store) {
      p.println(settings.first);
      for (String str : settings.second) {
        p.println(str);
      }
      p.println();
    }
    p.close();
  }
  
  public void load() throws IOException {
    BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    ArrayList<String> buf = new ArrayList<String>();
    while (is.ready()) {
      String line = is.readLine();
      // skip comments
      if (line.startsWith(COMMENT_PREFIX)) {
        continue;
      }
      if (line.length() == 0 && buf.size() > 0) {
        String title = buf.remove(0);
        store.add(new Pair<String,ArrayList<String>>(title, buf));
        buf = new ArrayList<String>();
      } else {
        buf.add(line);
      }
    }
    if (buf.size() > 0) {
      String title = buf.remove(0);
      store.add(new Pair<String,ArrayList<String>>(title, buf));
      buf = new ArrayList<String>();
    }
  }

  @Override
  public Iterator<Pair<String, ArrayList<String>>> iterator() {
    return store.iterator();
  }
  
  public void remove(String key) {
    Iterator<Pair<String, ArrayList<String>>> it = store.iterator();
    while (it.hasNext()) {
      String thisKey = it.next().first;
      if (key.equals(thisKey)) {
        it.remove();
        break;
      }
    }
  }
  
  public ArrayList<String> get(String key) {
    Iterator<Pair<String, ArrayList<String>>> it = store.iterator();
    while (it.hasNext()) {
      Pair<String, ArrayList<String>> pair = it.next();
      if (key.equals(pair.first)) {
        return pair.second;
      }
    }
    return null;
  }

  public void clear() {
    store.clear();
  }

  public void put(String key, ArrayList<String> value) {
    Iterator<Pair<String, ArrayList<String>>> it = store.iterator();
    while (it.hasNext()) {
      Pair<String, ArrayList<String>> pair = it.next();
      if (key.equals(pair.first)) {
        pair.second = value;
        return;
      }
    }
    store.add(new Pair<String, ArrayList<String>>(key, value));
  }

  public int size() {
    return store.size();
  }

  public Pair<String, ArrayList<String>> getElementAt(int index) {
    return store.get(index);
  }
}
