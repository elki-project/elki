package de.lmu.ifi.dbs.elki.gui;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class KDDFileChooser {

	public static final String TEXT_FORMAT = "txt";

	public static final String XML_FORMAT = "xml";

	private JFileChooser cheesy;

	private String fileName;
	
	private String fileType;

	public KDDFileChooser(String directoryPath) {

		cheesy = new JFileChooser();
		cheesy.setCurrentDirectory(new File(directoryPath));
		addFileFilters();
	}

	private void addFileFilters() {

		cheesy.addChoosableFileFilter(new SingleExtensionFilter("xml files (.xml)", XML_FORMAT));
		cheesy.addChoosableFileFilter(new SingleExtensionFilter("text files (.txt)", TEXT_FORMAT));
		cheesy.setAcceptAllFileFilterUsed(false);
	}

	public int showSaveDialog(Component owner) {

		int returnValue = cheesy.showSaveDialog(owner);
		if (returnValue == JFileChooser.APPROVE_OPTION) {

			File file = cheesy.getSelectedFile();

			if (cheesy.accept(file)) {

				this.fileName = file.getPath();
				this.fileType = ((SingleExtensionFilter)cheesy.getFileFilter()).getExtension();
			} else {
				int i = file.getPath().lastIndexOf(".");
				if (i == -1) { // attach correct file extension
					this.fileType = ((SingleExtensionFilter)cheesy.getFileFilter()).getExtension();
					this.fileName = file.getPath() + "." + fileType;
				} else {
					KDDDialog.showMessage(null, "Given file has wrong file extension. Allowed extensions are: " + cheesy.getFileFilter().getDescription());
				}

			}
		}

		return returnValue;
	}

	public int showLoadDialog(Component owner) {

		int returnValue = cheesy.showOpenDialog(owner);
		return returnValue;
	}

	public String getFileName(){
		return fileName;
	}
	
	public String getFileType(){
		return fileType;
	}
	
	private class SingleExtensionFilter extends FileFilter {

		private String description;

		private String extension;

		public SingleExtensionFilter(String description, String extension) {
			if (description == null || extension == null) {
				throw new IllegalArgumentException("Description and extension must be non-null");
			}
			this.description = description;
			this.extension = extension;
		}

		@Override
		public boolean accept(File f) {
			if (f != null) {
				if (f.isDirectory()) {
					return true;
				}
				String fileName = f.getPath();
				int i = fileName.lastIndexOf(".");
				if (i > 0 && i < fileName.length() - 1) {

					if (extension.equals(fileName.substring(i + 1).toLowerCase())) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public String getDescription() {
			return description;
		}

		public String getExtension() {
			return extension;
		}

	}
	// TODO maybe an own FileView??

}
