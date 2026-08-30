package app.tuxguitar.app.tools.browser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import app.tuxguitar.app.tools.browser.base.TGBrowserElement;
import app.tuxguitar.app.util.TGFileUtils;
import app.tuxguitar.tools.browser.TGBrowserCollection;

public class TGBrowserRatingStore {

	private static final String FILE_NAME = "browser-ratings.properties";
	private static final String RATING_PREFIX = "rating.";
	private static final String COLLECTION_PREFIX = "collection.";
	private static final String COLLECTION_ID_PREFIX = "collectionId.";
	private static final String NAME_PREFIX = "name.";
	private static final String PATH_PREFIX = "path.";

	private final File file;
	private final Properties properties;
	private int lastRating;
	private boolean dirty;

	public TGBrowserRatingStore() {
		this.file = new File(TGFileUtils.PATH_USER_CONFIG, FILE_NAME);
		this.properties = new Properties();
		this.lastRating = 3;
		this.dirty = false;
		this.load();
	}

	public synchronized int getRating(TGBrowserCollection collection, TGBrowserElement element) {
		String key = getElementKey(collection, element);
		if (key == null) {
			return 0;
		}
		try {
			int rating = Math.max(0, Math.min(5,
					Integer.parseInt(this.properties.getProperty(RATING_PREFIX + key, "0"))));
			if (rating > 0) {
				this.updateMetadata(key, collection, element);
			}
			return rating;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public synchronized void setRating(TGBrowserCollection collection, TGBrowserElement element, int rating) {
		String key = getElementKey(collection, element);
		if (key != null) {
			if (rating > 0) {
				int value = Math.min(5, rating);
				this.properties.setProperty(RATING_PREFIX + key, Integer.toString(value));
				this.setMetadata(key, collection, element);
				this.lastRating = value;
			} else {
				this.properties.remove(RATING_PREFIX + key);
				this.properties.remove(COLLECTION_PREFIX + key);
				this.properties.remove(COLLECTION_ID_PREFIX + key);
				this.properties.remove(NAME_PREFIX + key);
				this.properties.remove(PATH_PREFIX + key);
			}
			this.save();
		}
	}

	public synchronized int getLastRating() {
		return this.lastRating;
	}

	public synchronized void flush() {
		if (this.dirty) {
			this.save();
		}
	}

	public synchronized List<TGBrowserElement> getRatings(TGBrowserCollection collection) {
		List<TGBrowserElement> result = new ArrayList<TGBrowserElement>();
		String collectionId = getCollectionKey(collection);
		String collectionTitle = (collection != null && collection.getSettings() != null
				? collection.getSettings().getTitle() : null);
		for (String propertyName : this.properties.stringPropertyNames()) {
			if (!propertyName.startsWith(RATING_PREFIX)) {
				continue;
			}
			String key = propertyName.substring(RATING_PREFIX.length());
			String storedCollectionId = this.properties.getProperty(COLLECTION_ID_PREFIX + key);
			String storedCollectionTitle = this.properties.getProperty(COLLECTION_PREFIX + key);
			boolean sameCollection = (storedCollectionId != null
					? storedCollectionId.equals(collectionId)
					: collectionTitle != null && collectionTitle.equals(storedCollectionTitle));
			String name = this.properties.getProperty(NAME_PREFIX + key);
			String path = this.properties.getProperty(PATH_PREFIX + key);
			if (sameCollection && name != null && path != null) {
				try {
					int rating = Integer.parseInt(this.properties.getProperty(propertyName));
					if (rating >= 1 && rating <= 5) {
						result.add(new TGBrowserRatingEntry(name, path, rating));
					}
				} catch (NumberFormatException e) {
					// Ignore a malformed entry without affecting other ratings.
				}
			}
		}
		Collections.sort(result, new Comparator<TGBrowserElement>() {
			public int compare(TGBrowserElement first, TGBrowserElement second) {
				int firstRating = ((TGBrowserRatingEntry) first).getRating();
				int secondRating = ((TGBrowserRatingEntry) second).getRating();
				return (firstRating != secondRating ? secondRating - firstRating
						: first.getName().compareToIgnoreCase(second.getName()));
			}
		});
		return result;
	}

	public boolean canRate(TGBrowserCollection collection, TGBrowserElement element) {
		return (collection != null && element != null && !element.isFolder()
				&& this.resolveElementId(element) != null);
	}

	private String getCollectionKey(TGBrowserCollection collection) {
		if (collection == null || collection.getSettings() == null) {
			return null;
		}
		return digest(collection.getType() + "\n" + collection.getSettings().getData());
	}

	private String getElementKey(TGBrowserCollection collection, TGBrowserElement element) {
		String collectionKey = getCollectionKey(collection);
		String elementId = resolveElementId(element);
		return (collectionKey != null && elementId != null ? digest(collectionKey + "\n" + elementId) : null);
	}

	private void updateMetadata(String key, TGBrowserCollection collection, TGBrowserElement element) {
		if (!this.properties.containsKey(NAME_PREFIX + key)
				|| !this.properties.containsKey(PATH_PREFIX + key)
				|| !this.properties.containsKey(COLLECTION_PREFIX + key)
				|| !this.properties.containsKey(COLLECTION_ID_PREFIX + key)) {
			this.setMetadata(key, collection, element);
			this.dirty = true;
		}
	}

	private void setMetadata(String key, TGBrowserCollection collection, TGBrowserElement element) {
		String storedPath = this.resolveStoredPath(element);
		if (storedPath != null) {
			this.properties.setProperty(PATH_PREFIX + key, storedPath);
		}
		Object rawName = invoke(element, "getRawName");
		String name = (rawName != null ? rawName.toString() : element.getName());
		if (name != null) {
			this.properties.setProperty(NAME_PREFIX + key, name);
		}
		if (collection != null && collection.getSettings() != null
				&& collection.getSettings().getTitle() != null) {
			this.properties.setProperty(COLLECTION_PREFIX + key, collection.getSettings().getTitle());
		}
		String collectionId = getCollectionKey(collection);
		if (collectionId != null) {
			this.properties.setProperty(COLLECTION_ID_PREFIX + key, collectionId);
		}
	}

	private String resolveStoredPath(TGBrowserElement element) {
		Object file = invoke(element, "getFile");
		if (file instanceof File) {
			try {
				return ((File) file).getCanonicalPath();
			} catch (Throwable throwable) {
				return ((File) file).getAbsolutePath();
			}
		}
		return this.resolveElementId(element);
	}

	private String resolveElementId(TGBrowserElement element) {
		if (element == null) {
			return null;
		}
		Object value = invoke(element, "getPersistentId");
		if (value == null) {
			value = invoke(element, "getPath");
		}
		if (value == null) {
			value = invoke(element, "getFile");
		}
		if (value instanceof File) {
			try {
				return ((File) value).getCanonicalPath();
			} catch (Throwable throwable) {
				return ((File) value).getAbsolutePath();
			}
		}
		return (value != null && value.toString().length() > 0 ? value.toString() : null);
	}

	private Object invoke(TGBrowserElement element, String methodName) {
		try {
			Method method = element.getClass().getMethod(methodName);
			return method.invoke(element);
		} catch (Throwable throwable) {
			return null;
		}
	}

	private String digest(String value) {
		try {
			byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder();
			for (byte item : bytes) {
				result.append(String.format("%02x", item & 0xff));
			}
			return result.toString();
		} catch (Throwable throwable) {
			throw new IllegalStateException(throwable);
		}
	}

	private void load() {
		if (this.file.isFile()) {
			try (FileInputStream input = new FileInputStream(this.file)) {
				this.properties.load(input);
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
	}

	private void save() {
		File parent = this.file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		File temporary = new File(this.file.getAbsolutePath() + ".tmp");
		try {
			try (FileOutputStream output = new FileOutputStream(temporary)) {
				this.properties.store(output, "TuxGuitar collection browser ratings");
			}
			try {
				Files.move(temporary.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (Throwable throwable) {
				Files.move(temporary.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			this.dirty = false;
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}
}
