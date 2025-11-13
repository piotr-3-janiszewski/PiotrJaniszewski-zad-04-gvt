package uj.wmii.pwj.gvt;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.regex.*;

public class Gvt {
	private final static File homeFile = new File(".gvt/gvt_realm.gvtsystem");
	private final ExitHandler exitHandler;

	public Gvt(ExitHandler exitHandler) {
		this.exitHandler = exitHandler;
	}

	public static void main(String... args) {
		Gvt gvt = new Gvt(new ExitHandler());
		gvt.mainInternal(args);
	}

	public void mainInternal() {
		exitHandler.exit(1, "Please specify command.");
	}

	public void mainInternal(String... variadicArguments) {
		String[] args;
		{
			ArrayList<String> args_buff = new ArrayList<String>();
			for (String element : variadicArguments)
				args_buff.add(element);

			args = args_buff.toArray(new String[0]);
		}

		if (args == null || args.length == 0) {
			exitHandler.exit(1, "Please specify command.");
			return;
		}

		String command = args[0];
		if (command.equals("init"))
			init();
		else if (command.equals("history")) {
			int historyLines = -1;
			if (args.length >= 3 && args[1].equals("-last"))
				historyLines = Integer.parseInt(args[2]);

			history(historyLines);
		}
		else if (command.equals("version")) {
			version();
		}
		else { 
			if (!isInitialized()) {
				exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
				return;
			}

			String fileName = null;
			if (args.length >= 2)
				fileName = args[1];

			String commitMessage = null;
			if (args.length >= 4 && args[2].equals("-m"))
				commitMessage = args[3];

			if(command.equals("add"))
				add(fileName, commitMessage);
			else if (command.equals("detach"))
				detach(fileName, commitMessage);
			else if (command.equals("commit"))
				commit(fileName, commitMessage);
			else if (command.equals("checkout")) {
				checkout(Integer.parseInt(args[1]));
			}
			else {
				exitHandler.exit(1, "Unknown command " + command + ".");
				return;
			}
		}
	}

	
	private ArrayList<Version> load() {
		if (!isInitialized()) {
			exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
			return null;
		}

		ArrayList<Version> result = null;

		try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(homeFile))) {
			result = (ArrayList<Version>) input.readObject();
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
			exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
		}

		return result;
	}

	private void save(ArrayList<Version> versions) {
		try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(homeFile))) {
			output.writeObject(versions);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
			exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
		}
	}

	private static boolean isInitialized() {
		return homeFile.exists();
	}

	public void init() {
		if (isInitialized()) {
			exitHandler.exit(10, "Current directory is already initialized.");
			return;
		}
		
		File directory = new File(".gvt");
		if (!directory.exists()) {
			directory.mkdir();
		}

		ArrayList<Version> versions = new ArrayList<Version>();
		versions.add(Version.init());
		save(versions);

		exitHandler.exit(0, "Current directory initialized successfully.");
	}

	private Version getCurrentVersion() {
		ArrayList<Version> versions = load();
		Version currentVersion = versions.get(versions.size() - 1);

		return currentVersion;
	}

	private void addNextVersion(Version version) {
		ArrayList<Version> versions = load();
		versions.add(version);
		save(versions);
	}

	private void add(String fileName, String commitMessage) {
		if (!isInitialized()) {
			exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
			return;
		}

		if (fileName == null) {
			exitHandler.exit(20, "Please specify file to add.");
			return;
		}

		Path pathToFile = Paths.get(fileName);
		if (!Files.exists(pathToFile)) {
			exitHandler.exit(21, "File not found. File: " + fileName);
			return;
		}

		Version currentVersion = getCurrentVersion();

		if (currentVersion.contains(fileName)) {
			exitHandler.exit(0, "File already added. File: " + fileName);
			return;
		}

		if (commitMessage == null)
			commitMessage = "File added successfully. File: " + fileName;
		addNextVersion(Version.add(getCurrentVersion(), fileName, commitMessage));

		exitHandler.exit(0, "File added successfully. File: " + fileName);
	}

	private void detach(String fileName, String commitMessage) {
		if (!isInitialized()) {
			exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
			return;
		}

		if (fileName == null) {
			exitHandler.exit(30, "Please specify file to detach.");
			return;
		}

		Version currentVersion = getCurrentVersion();
		if (!currentVersion.contains(fileName)) {
			exitHandler.exit(0, "File is not added to gvt. File: " + fileName);
			return;
		}

		if (commitMessage == null) 
			commitMessage = "File detached successfully. File: " + fileName;

		addNextVersion(Version.detach(currentVersion, fileName, commitMessage));
		exitHandler.exit(0, "File detached successfully. File: " + fileName);
	}

	private void commit(String fileName, String commitMessage) {
		if (!isInitialized()) {
			exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
			return;
		}

		if (fileName == null) {
			exitHandler.exit(50, "Please specify file to commit.");
			return;
		}

		Path pathToFile = Paths.get(fileName);
		if (!Files.exists(pathToFile)) {
			exitHandler.exit(51, "File not found. File: " + fileName);
			return;
		}

		Version currentVersion = getCurrentVersion();
		if (!currentVersion.contains(fileName)) {
			exitHandler.exit(0, "File is not added to gvt. File: " + fileName);
			return;
		}

		if (commitMessage == null)
			commitMessage = "File committed successfully. File: " + fileName;

		addNextVersion(Version.commit(currentVersion, fileName, commitMessage));
		exitHandler.exit(0, "File committed successfully. File: " + fileName);
	}

	private void version() {
		if (!isInitialized()) {
			exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
			return;
		}

		Version currentVersion = getCurrentVersion();
		exitHandler.exit(0, "Version: " + currentVersion.getVersionID() + "\n" + currentVersion.getCommitMessage());
	}

	private void history(int lastLines) {
		if (!isInitialized()) {
			exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
			return;
		}

		ArrayList<Version> versions = load();
		String result = "";

		int bottomBoundry = 0;
		if (lastLines >= 0)
			bottomBoundry = versions.size() - lastLines;
		for (int i = versions.size() - 1; i >= bottomBoundry; i--) {
			String message = versions.get(i).getCommitMessage();

			int firstn = message.indexOf('\n');
			if (firstn >= 0)
				message = message.substring(0, firstn);

			result += versions.get(i).getVersionID() + ": " + message + "\n";
		}
		exitHandler.exit(0, result);
	}

	private void checkout(int version) {
		Version desiredVersion = null;

		ArrayList<Version> versions = load();

		for (int i = 0; i < versions.size() && desiredVersion == null; i++)
			if (versions.get(i).getVersionID() == version)
				desiredVersion = versions.get(i);
		
		if (desiredVersion == null) {
			exitHandler.exit(60, "Invalid version number: " + version);
			return;
		}


		desiredVersion.reinstantiate();
		exitHandler.exit(0, "Checkout successful for version: " + version);
	}
}
