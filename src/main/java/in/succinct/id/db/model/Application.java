package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.plugins.collab.db.model.user.User;

@HAS_DESCRIPTION_FIELD("APP_ID")
public interface Application extends com.venky.swf.plugins.collab.db.model.participants.Application {

    @PARTICIPANT("ADMIN")
    @IS_VIRTUAL
    public Long getAdminId();
    public void setAdminId(Long id);
    public User getAdmin();

    @Override
    @PARTICIPANT
    Long getCompanyId();
}
