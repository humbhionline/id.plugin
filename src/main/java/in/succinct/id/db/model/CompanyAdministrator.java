package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;

@ORDER_BY("CREATED_AT")
public interface CompanyAdministrator extends Model {
    @UNIQUE_KEY
    @PARTICIPANT
    public long getCompanyId();
    public void setCompanyId(long id);
    public Company getCompany();

    @UNIQUE_KEY
    @PARTICIPANT
    public long getUserId();
    public void setUserId(long id);
    public User getUser();


}
