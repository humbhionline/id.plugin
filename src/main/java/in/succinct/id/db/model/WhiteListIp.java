package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface WhiteListIp extends com.venky.swf.db.model.application.WhiteListIp {
    @PARTICIPANT
    public Long getApplicationId();

}
