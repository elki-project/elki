/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.gui.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import elki.utilities.io.FormatUtil;
import elki.utilities.pairs.Pair;

/**
 * Class to manage saved settings in a text file.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class SavedSettingsFile implements Iterable<Pair<String, ArrayList<String>>> {
  /**
   * Comment prefix
   */
  public static final String COMMENT_PREFIX = "#";

  /**
   * File to read and write
   */
  private Path file;

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
    this.file = Paths.get(filename);
    this.store = new ArrayList<>();
  }

  /**
   * Save the current data to the given file.
   */
  public void save() throws IOException {
    BufferedWriter p = Files.newBufferedWriter(file);
    p.append(COMMENT_PREFIX).append("Saved ELKI settings. First line is title, remaining lines are parameters.").append(FormatUtil.NEWLINE);
    for(Pair<String, ArrayList<String>> settings : store) {
      p.append(settings.first).append(FormatUtil.NEWLINE);
      for(String str : settings.second) {
        p.append(str).append(FormatUtil.NEWLINE);
      }
      p.append(FormatUtil.NEWLINE);
    }
    p.close();
  }

  /**
   * Read the current file
   *
   * @throws NoSuchFileException thrown when file not found
   * @throws IOException thrown on IO errors
   */
  public void load() throws NoSuchFileException, IOException {
    try (BufferedReader is = Files.newBufferedReader(file)) {
      String line;
      ArrayList<String> buf = null;
      while((line = is.readLine()) != null) {
        // skip comments
        if(line.startsWith(COMMENT_PREFIX)) {
          continue;
        }
        if(line.length() == 0) {
          buf = null;
        }
        else if(buf == null) {
          store.add(new Pair<>(line, buf = new ArrayList<>()));
        }
        else {
          buf.add(line);
        }
      }
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
    while(it.hasNext()) {
      String thisKey = it.next().first;
      if(key.equals(thisKey)) {
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
    while(it.hasNext()) {
      Pair<String, ArrayList<String>> pair = it.next();
      if(key.equals(pair.first)) {
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
    while(it.hasNext()) {
      Pair<String, ArrayList<String>> pair = it.next();
      if(key.equals(pair.first)) {
        pair.second = value;
        return;
      }
    }
    store.add(new Pair<>(key, value));
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
