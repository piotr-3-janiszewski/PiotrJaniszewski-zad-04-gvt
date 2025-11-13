package uj.wmii.pwj.gvt;

import java.io.*;
import java.nio.file.*;

public class SavedFile implements Serializable {
	private final String name;
	private byte[] content;

	public SavedFile(String fileName) {
		this.name = fileName;
		try {
			this.content = Files.readAllBytes(Paths.get(fileName));
		}
		catch(Exception e) {
			
		}
	}

	public SavedFile(SavedFile other) {
		this.name = other.name;
		this.content = other.content.clone();
	}

	public String getName() {
		return name;
	}

	public byte[] getContent() {
		return content.clone();
	}

	public void writeToDisk() {
		try {
			Files.write(Paths.get(name), content);
		}
		catch(Exception e) {
		}
	}
}

