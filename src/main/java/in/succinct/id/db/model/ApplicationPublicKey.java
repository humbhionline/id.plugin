package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface ApplicationPublicKey extends com.venky.swf.db.model.application.ApplicationPublicKey {
    @PARTICIPANT
    public Long getApplicationId();

}