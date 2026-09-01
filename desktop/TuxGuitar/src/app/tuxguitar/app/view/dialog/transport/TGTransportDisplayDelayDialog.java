package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.transport.TGTransport;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.event.UISelectionListener;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UISpinner;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;

public class TGTransportDisplayDelayDialog {

	public void show(final TGContext context) {
		UIFactory factory = TGApplication.getInstance(context).getFactory();
		final UIWindow dialog = factory.createWindow(TGWindow.getInstance(context).getWindow(), true, false);
		UITableLayout layout = new UITableLayout();
		dialog.setLayout(layout);
		dialog.setText(TuxGuitar.getProperty("transport.display-delay"));

		UILabel label = factory.createLabel(dialog);
		label.setText(TuxGuitar.getProperty("transport.display-delay.milliseconds") + ":");
		layout.set(label, 1, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, false, true);

		final UISpinner delay = factory.createSpinner(dialog);
		delay.setMinimum(-5000);
		delay.setMaximum(5000);
		delay.setIncrement(10);
		delay.setValue(TGTransport.getInstance(context).getCache().getDisplayDelayMillis());
		layout.set(delay, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);

		UITableLayout buttonsLayout = new UITableLayout(0f);
		UIPanel buttons = factory.createPanel(dialog, false);
		buttons.setLayout(buttonsLayout);
		layout.set(buttons, 2, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, true, 1, 2);

		UIButton ok = factory.createButton(buttons);
		ok.setText(TuxGuitar.getProperty("ok"));
		ok.setDefaultButton();
		ok.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTransport.getInstance(context).getCache().setDisplayDelayMillis(delay.getValue());
				dialog.dispose();
			}
		});
		buttonsLayout.set(ok, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true,
				1, 1, 80f, 25f, null);

		UIButton cancel = factory.createButton(buttons);
		cancel.setText(TuxGuitar.getProperty("cancel"));
		cancel.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				dialog.dispose();
			}
		});
		buttonsLayout.set(cancel, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true,
				1, 1, 80f, 25f, null);

		TGDialogUtil.openDialog(dialog, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}
}
