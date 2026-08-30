package app.tuxguitar.app.view.dialog.browser.main;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.tools.browser.TGBrowserManager;
import app.tuxguitar.app.tools.browser.base.TGBrowserFactory;
import app.tuxguitar.tools.browser.TGBrowserCollection;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.event.UISelectionListener;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuItem;
import app.tuxguitar.ui.resource.UIImage;
import app.tuxguitar.ui.toolbar.UIToolActionItem;
import app.tuxguitar.ui.toolbar.UIToolActionMenuItem;
import app.tuxguitar.ui.toolbar.UIToolBar;
import app.tuxguitar.ui.toolbar.UIToolCustomItem;
import app.tuxguitar.ui.toolbar.UIToolMenuItem;
import app.tuxguitar.ui.widget.UIContainer;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UISelectItem;

public class TGBrowserToolBar extends TGBrowserBar{

	private UIToolBar toolBar;

	private UIToolMenuItem newItem;
	private UIMenuActionItem demoSongs;

	private UIToolActionItem root;
	private UIToolActionItem back;
	private UIToolActionItem refresh;
	private UIToolActionMenuItem rating;
	private UIToolActionItem ratingView;
	private UIImage ratingImage;
	private UIMenuActionItem ratingNone;
	private UIMenuCheckableItem[] ratingLevels;

	private UIToolCustomItem collectionsItem;
	private UIDropDownSelect<TGBrowserCollection> collections;
	private TGBrowserCollection currentCollection;

	public TGBrowserToolBar(TGBrowserDialog browser){
		super(browser);
	}

	public void createToolBar(UIContainer parent){
		this.toolBar = this.getBrowser().getUIFactory().createHorizontalToolBar(parent);

		//---New Collection------------------------------------------------------
		this.newItem = this.toolBar.createMenuItem();
		this.newItem.setImage(TuxGuitar.getInstance().getIconManager().getBrowserNew());

		this.toolBar.createSeparator();

		//---Root Folder---------------------------------------------------------
		this.root = this.toolBar.createActionItem();
		this.root.setImage(TuxGuitar.getInstance().getIconManager().getBrowserRoot());
		this.root.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				getBrowser().cdRoot();
			}
		});

		//---Back Folder---------------------------------------------------------
		this.back = this.toolBar.createActionItem();
		this.back.setImage(TuxGuitar.getInstance().getIconManager().getBrowserBack());
		this.back.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				getBrowser().cdUp();
			}
		});

		//---Refresh Folder------------------------------------------------------
		this.refresh = this.toolBar.createActionItem();
		this.refresh.setImage(TuxGuitar.getInstance().getIconManager().getBrowserRefresh());
		this.refresh.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				getBrowser().listElements();
			}
		});

		//---Rating---------------------------------------------------------------
		this.rating = this.toolBar.createActionMenuItem();
		this.ratingImage = this.createRatingImage();
		this.rating.setImage(this.ratingImage);
		this.rating.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				toggleRating();
			}
		});
		this.ratingNone = this.rating.getMenu().createActionItem();
		this.ratingNone.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				setRating(0);
			}
		});
		this.ratingLevels = new UIMenuCheckableItem[5];
		for (int i = 0; i < this.ratingLevels.length; i++) {
			final int level = i + 1;
			this.ratingLevels[i] = this.rating.getMenu().createRadioItem();
			this.ratingLevels[i].addSelectionListener(new UISelectionListener() {
				public void onSelect(UISelectionEvent event) {
					setRating(level);
				}
			});
		}

		//---Rated files view------------------------------------------------------
		this.ratingView = this.toolBar.createActionItem();
		this.ratingView.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.MARKER_LIST));
		this.ratingView.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				getBrowser().toggleRatingView();
			}
		});

		this.toolBar.createSeparator();

		//---Collections---------------------------------------------------------
		this.collectionsItem = this.toolBar.createCustomItem();
		this.collectionsItem.setLayoutAttribute(UIToolCustomItem.FILL, true);
		this.collectionsItem.setLayoutAttribute(UIToolCustomItem.PACKED_WIDTH, 0f);

		this.collections = this.getBrowser().getUIFactory().createDropDownSelect(this.collectionsItem);
		this.collections.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				updateCollection();
			}
		});
	}

	public void updateItems(){
		this.newItem.setEnabled(!getBrowser().getConnection().isLocked());
		this.collections.setEnabled(!getBrowser().getConnection().isLocked());
		boolean navigationEnabled = (!getBrowser().isRatingView()
				&& !getBrowser().getConnection().isLocked() && getBrowser().getConnection().isOpen());
		this.root.setEnabled(navigationEnabled);
		this.back.setEnabled(navigationEnabled);
		this.refresh.setEnabled(navigationEnabled);
		this.updateRatingItem();
	}

	public void updateTypes() {
		List<UIMenuItem> newItems = this.newItem.getMenu().getItems();
		for(UIMenuItem uiMenuItem : newItems){
			uiMenuItem.dispose();
		}

		Iterator<TGBrowserFactory> bookTypes = TGBrowserManager.getInstance(getBrowser().getContext()).getFactories();

		while(bookTypes.hasNext()) {
			final TGBrowserFactory bookType = bookTypes.next();
			UIMenuActionItem item = this.newItem.getMenu().createActionItem();
			item.setText(bookType.getName());
			item.setImage(bookType.getIcon());
			item.addSelectionListener(new UISelectionListener() {
				public void onSelect(UISelectionEvent event) {
					newCollection( bookType.getType());
				}
			});
		}

		this.newItem.getMenu().createSeparator();

		this.demoSongs = this.newItem.getMenu().createActionItem();
		this.demoSongs.setText(TuxGuitar.getProperty("browser.collection.demo-songs"));
		this.demoSongs.setImage(TuxGuitar.getInstance().getIconManager().getBrowserCollection());
		this.demoSongs.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				addDemoCollection();
			}
		});
	}

	public void updateCollections(TGBrowserCollection selection){
		this.currentCollection = selection;
		this.collections.setIgnoreEvents(true);
		this.collections.removeItems();
		this.collections.addItem(new UISelectItem<TGBrowserCollection>(TuxGuitar.getProperty("browser.collection.select"), null));

		Iterator<TGBrowserCollection> it = TGBrowserManager.getInstance(getBrowser().getContext()).getCollections();
		while(it.hasNext()){
			TGBrowserCollection collection = it.next();
			if( collection.getSettings() != null ) {
				this.collections.addItem(new UISelectItem<TGBrowserCollection>(collection.getSettings().getTitle(), collection));
			}
		}

		this.collections.setSelectedValue(this.currentCollection);
		this.collections.setIgnoreEvents(false);
	}

	public void loadProperties(){
		this.newItem.setToolTipText(TuxGuitar.getProperty("add"));
		this.updateTypes();
		this.root.setToolTipText(TuxGuitar.getProperty("browser.go-root"));
		this.back.setToolTipText(TuxGuitar.getProperty("browser.go-back"));
		this.refresh.setToolTipText(TuxGuitar.getProperty("browser.refresh"));
		this.rating.setToolTipText(TuxGuitar.getProperty("browser.rating"));
		this.ratingView.setToolTipText(TuxGuitar.getProperty("browser.rating.view"));
		this.ratingNone.setText(TuxGuitar.getProperty("browser.rating.none"));
		for (int i = 0; i < this.ratingLevels.length; i++) {
			this.ratingLevels[i].setText(TuxGuitar.getProperty("browser.rating.level") + " " + (i + 1));
		}
		this.updateCollections(getBrowser().getCollection());
	}

	public void updateRatingItem() {
		if (this.rating == null || this.rating.isDisposed()) {
			return;
		}
		TGBrowserCollection collection = getBrowser().getCollection();
		app.tuxguitar.app.tools.browser.base.TGBrowserElement element = getBrowser().getSelectedElement();
		boolean canRate = getBrowser().getRatingStore().canRate(collection, element);
		int value = (canRate ? getBrowser().getRatingStore().getRating(collection, element) : 0);

		this.rating.setEnabled(canRate && !getBrowser().getConnection().isLocked());
		this.rating.setToolTipText(TuxGuitar.getProperty("browser.rating") + (value > 0 ? ": " + value : ""));
		this.ratingView.setEnabled(collection != null && !getBrowser().getConnection().isLocked());
		this.ratingView.setToolTipText(TuxGuitar.getProperty("browser.rating.view")
				+ (getBrowser().isRatingView() ? " [★]" : ""));
		this.ratingNone.setEnabled(canRate);
		for (int i = 0; i < this.ratingLevels.length; i++) {
			this.ratingLevels[i].setEnabled(canRate);
			this.ratingLevels[i].setChecked(value == i + 1);
		}
	}

	private void toggleRating() {
		TGBrowserCollection collection = getBrowser().getCollection();
		app.tuxguitar.app.tools.browser.base.TGBrowserElement element = getBrowser().getSelectedElement();
		if (getBrowser().getRatingStore().canRate(collection, element)) {
			int value = getBrowser().getRatingStore().getRating(collection, element);
			setRating(value > 0 ? 0 : getBrowser().getRatingStore().getLastRating());
		}
	}

	private void setRating(int value) {
		getBrowser().getRatingStore().setRating(getBrowser().getCollection(), getBrowser().getSelectedElement(), value);
		getBrowser().updateTable();
	}

	public void reload(){
		if(!this.toolBar.isDisposed()) {
			UIContainer parent = (UIContainer) this.toolBar.getParent();

			this.dispose();
			this.createToolBar(parent);
			this.loadProperties();
			this.updateItems();
		}
	}

	public void updateCollection() {
		TGBrowserCollection collection = this.collections.getSelectedValue();
		if( this.currentCollection != collection ) {
			this.currentCollection = collection;

			if( this.currentCollection == null ){
				this.closeCollection();
			} else {
				this.openCollection(this.currentCollection);
			}
		}
	}

	private void dispose(){
		this.toolBar.dispose();
		if (this.ratingImage != null && !this.ratingImage.isDisposed()) {
			this.ratingImage.dispose();
		}
	}

	private UIImage createRatingImage() {
		String data = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAABmJLR0T///////8JWPfcAAAAB3RJTUUH6ggeCR4Hvklo2gAAAuVJREFUSMdjYBiqoLU2ZR3TBenFDQ0JCQwM///D+KSaw0SuAwSm85VxnqlKkj4pLs8vg+CTag4jqRogPmbZCeH9dgu39fY3NGNgWHl468bzp2CqWHc1NCxYwMDwx53qISB8TcCZa4HXDhhfhlfCXOAybnmqO+Bn4a+wP759drYCptuUihgYmLSZ7jNaMjDA+DB5Ys2DRwEsMRGrMTMw+pV1JgMDjwpXNftNBoYvd761/lRnYJi+fqnY0enEewjugAk1eTqcLDaMH1g+mXz/c/if4gwZZaFkBgbfNuc3OrsxNbKHs4WxqGAJoZW/Vv25gym+uWqvyBVXBob7GU/uvpvLwCDwh+8MJ4stEzwKClomXfn+5wg0BAQZ3679oPdt5eXOSY8Wfjz0iIHhQ9+nc9924rYY3WEw9TD9MPNg5sPsY0Y34MCBCxcYGH4wWMppcfz5NT1BVETIiPsJy//jCud/PlS2kzB5ontX9i8DA4sOszaTEO4QmCW9gut4GwMDRH/LxfdXPu7/3hH2A5I7fqyDqceZCCEKGWIYOBk4GFi/mPJz8/7hqCE+BGDqYfrh5qEBgrngc/4Xr5+R5a1W8432KTQixC8L34x+JsHA0N09e/a+fQg+DMDUw/TjMh9nQdTclvSXcav47b+//iX/936hkn06xsDGjoHh8PvTnnd7GRguud1sf27KwCAoyr+fc7HT+/evPzp+j90nqLdLvVLyNAODraDpduViBoappksuHDnEwMDMxjSXcavEndqqecz/vV+qEgwBIXGBuVwmCVqwoFyctkHgzGEGhpuF97e/dviwmUWFeTJTmNS//OyJD7/H7heC8WHyMPUw/TjziI4CWNB9/PqZ5UcLAwPLbmZLJumddT+v/rrw56v4zJqYubz/Vj2HJ2IYHyYPUw/TjysqMKIAUiAJQHnv/4sqCdlxv6nmf33v3aGvIm2foImJIICYw8AA0V/FB9Hf+hEiK8gIMecDAwCEGWwEQfMvpAAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAyNi0wOC0zMFQwOTozMDowNyswMDowMP+GKBQAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMjYtMDgtMzBUMDk6MzA6MDcrMDA6MDCO25CoAAAAAElFTkSuQmCC";
		return this.getBrowser().getUIFactory().createImage(
				new ByteArrayInputStream(Base64.getDecoder().decode(data)));
	}

	public UIToolBar getControl() {
		return this.toolBar;
	}
}
