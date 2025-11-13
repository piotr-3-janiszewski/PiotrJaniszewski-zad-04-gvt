package uj.wmii.pwj.gvt;

import java.io.*;

public class Version implements Serializable{
	private int versionID;
	private SavedFile[] savedFiles;
	private String commitMessage;

	private Version() {}

	private static Version nextVersion(Version previousVersion, String commitMessage) {
		if (previousVersion == null)
			throw new IllegalArgumentException("previousVersion is null");

		Version result = new Version();
		result.versionID = previousVersion.versionID + 1;
		result.commitMessage = commitMessage;
		
		return result;
	}

	public static Version init() {
		Version result = new Version();
		result.versionID = 0;
		result.savedFiles = new SavedFile[0];
		result.commitMessage = "GVT initialized.";

		return result;
	}

	public static Version add(Version previousVersion, String fileToAdd, String commitMessage) {
		Version result = nextVersion(previousVersion, commitMessage);

		result.savedFiles = new SavedFile[previousVersion.savedFiles.length + 1];

		int savedFilesIterator = 0;
		for (SavedFile savedFile : previousVersion.savedFiles)
			result.savedFiles[savedFilesIterator++] = new SavedFile(savedFile);

		result.savedFiles[savedFilesIterator++] = new SavedFile(fileToAdd);

		return result;
	}

	public static Version detach(Version previousVersion, String fileToDetach, String commitMessage) {
		Version result = nextVersion(previousVersion, commitMessage);
		result.savedFiles = new SavedFile[previousVersion.savedFiles.length - 1];

		int savedFilesIterator = 0;
		for (SavedFile savedFile : previousVersion.savedFiles)
			if (!savedFile.getName().equals(fileToDetach))
				result.savedFiles[savedFilesIterator++] = new SavedFile(savedFile);

		return result;
	}

	public static Version commit(Version previousVersion, String fileToCommit, String commitMessage) {
		Version result = nextVersion(previousVersion, commitMessage);
		
		result.savedFiles = new SavedFile[previousVersion.savedFiles.length];

		int savedFilesIterator = 0;
		for (SavedFile savedFile : previousVersion.savedFiles)
			if (!savedFile.getName().equals(fileToCommit))
				result.savedFiles[savedFilesIterator++] = new SavedFile(savedFile);
			else
				result.savedFiles[savedFilesIterator++] = new SavedFile(fileToCommit);

		return result;
	}

	public boolean contains(String filename) {
		boolean result = false;

		for (SavedFile savedFile : savedFiles)
			result |= (savedFile.getName().equals(filename));

		return result;
	}

	public void reinstantiate() {
		for (SavedFile savedFile : savedFiles) {
			savedFile.reinstantiate();
		}
	}

	public int getVersionID() {
		return versionID;
	}

	public String getCommitMessage() {
		return commitMessage;
	}
}
