package app.tuxguitar.app.tools.browser;

import app.tuxguitar.app.tools.browser.base.TGBrowserElement;

public class TGBrowserRatingEntry implements TGBrowserElement {

	private final String name;
	private final String path;
	private final int rating;

	public TGBrowserRatingEntry(String name, String path, int rating) {
		this.name = name;
		this.path = path;
		this.rating = rating;
	}

	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

	public String getPersistentId() {
		return this.path;
	}

	public int getRating() {
		return this.rating;
	}

	public boolean isFolder() {
		return false;
	}
}
