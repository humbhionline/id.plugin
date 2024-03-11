package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.plugins.collab.db.model.participants.EndPoint;
import com.venky.swf.plugins.collab.db.model.participants.EventHandler;
import com.venky.swf.routing.Config;

public class EndPointExtension extends ModelOperationExtension<EndPoint> {
    static {
        registerExtension(new EndPointExtension());
    }

    @Override
    protected void afterSave(EndPoint instance) {
        if (instance.getBaseUrl().startsWith(Config.instance().getServerBaseUrl())){
            return; // No need to verify ourselves.
        }
        EventHandler handler = Database.getTable(EventHandler.class).newRecord();
        handler.setApplicationId(instance.getApplicationId());
        handler.setContentType(MimeType.APPLICATION_JSON.toString());
        handler.setEndPointId(instance.getId());
        handler.setEventId(Event.find("end_point_verification").getId());
        handler.setRelativeUrl("on_subscribe");
        handler = Database.getTable(EventHandler.class).getRefreshed(handler);
        if (handler.getRawRecord().isNewRecord()) {
            handler.setEnabled(true);
            handler.save();
        }

    }


}
