package com.example.viewer;

/**
 * <br />
 * Represents entity stored in JSON file.
 */
public class Article {
	private int id;
	private String title;
	private String url;

	public Article(int id, String title, String url) {
		this.id = id;
		this.title = title;
		this.url = url;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return title;
	}
}
