package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface EndPoint extends com.venky.swf.db.model.application.api.EndPoint {
    @PARTICIPANT
    public Long getApplicationId();
}
