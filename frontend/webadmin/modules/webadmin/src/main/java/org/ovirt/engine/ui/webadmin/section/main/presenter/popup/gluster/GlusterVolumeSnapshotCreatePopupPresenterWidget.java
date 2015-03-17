package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.GlusterVolumeSnapshotModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GlusterVolumeSnapshotCreatePopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<GlusterVolumeSnapshotModel, GlusterVolumeSnapshotCreatePopupPresenterWidget.ViewDef> {
    @Inject
    public GlusterVolumeSnapshotCreatePopupPresenterWidget(EventBus eventBus,
            ViewDef view,
            Provider<GlusterVolumeSnapshotCreatePopupPresenterWidget> snapshotPopupProvider,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupPrivder) {
        super(eventBus, view);
    }

    @Override
    public void init(final GlusterVolumeSnapshotModel model) {
        super.init(model);

        model.getInterval().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getView().setCriticalIntervalLabelVisibility(model,
                        Integer.parseInt(((ListModel<String>) sender).getSelectedItem()));
            }
        });

        model.getRecurrence().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getView().updateVisibilities(model);
                getView().setCriticalIntervalLabelVisibility(model,
                        Integer.parseInt(model.getInterval().getSelectedItem()));
                getView().setMessage(null);
            }
        });

        model.getEndByOptions().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getView().setEndDateVisibility(model);
            }
        });
    }

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<GlusterVolumeSnapshotModel> {
        public void updateVisibilities(GlusterVolumeSnapshotModel object);

        public void setEndDateVisibility(GlusterVolumeSnapshotModel object);

        public void setCriticalIntervalLabelVisibility(GlusterVolumeSnapshotModel object, int value);
    }
}
