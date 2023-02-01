package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface EventHandler extends com.venky.swf.db.model.application.api.EventHandler {
    @PARTICIPANT
    public Long getApplicationId();

}
