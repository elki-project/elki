package de.lmu.ifi.dbs.elki.gui.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to manage saved settings in a text file.
 * 
 * @author Erich Schubert
 */
public class SavedSettingsFile implements Iterable<Pair<String, ArrayList<String>>> {
  /**
   * Comment prefix
   */
  public static final String COMMENT_PREFIX = "#";

  /**
   * File to read and write
   */
  private File file;
  
  /**
   * Data store
   */
  private ArrayList<Pair<String, ArrayList<String>>> store;

  /**
   * Constructor.
   * 
   * @param filename Filename
   */
  public SavedSettingsFile(String filename) {
    super();
    this.file = new File(filename);
    this.store = new ArrayList<Pair<String, ArrayList<String>>>();
  }
  
  /**
   * Save the current data to the given file.
   * 
   * @throws IOException thrown on output errors.
   */
  public void save() throws IOException {
    PrintStream p = new PrintStream(file);
    p.println(COMMENT_PREFIX + "Saved ELKI settings. First line is title, remaining lines are parameters.");
    for (Pair<String, ArrayList<String>> settings : store) {
      p.println(settings.first);
      for (String str : settings.second) {
        p.println(str);
      }
      p.println();
    }
    p.close();
  }
  
  /**
   * Read the current file
   * 
   * @throws FileNotFoundException thrown when file not found
   * @throws IOException thrown on IO errprs
   */
  public void load() throws FileNotFoundException, IOException {
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

  /**
   * Remove a given key from the file.
   * 
   * @param key Key to remove
   */
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
  
  /**
   * Find a saved setting by key.
   * 
   * @param key Key to search for
   * @return saved settings for this key
   */
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

  /**
   * Remove all saved settings.
   */
  public void clear() {
    store.clear();
  }

  /**
   * Add/Replace a saved setting
   * 
   * @param key Key
   * @param value (New) value.
   */
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

  /**
   * Return number of saved settings profiles.
   * 
   * @return Number of saved settings profiles
   */
  public int size() {
    return store.size();
  }

  /**
   * Array access.
   * 
   * @param index settings index
   * @return pair at this index
   */
  public Pair<String, ArrayList<String>> getElementAt(int index) {
    return store.get(index);
  }
}
