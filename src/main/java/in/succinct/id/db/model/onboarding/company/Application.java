package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.model.Model;

import java.util.List;

public interface Application extends com.venky.swf.plugins.collab.db.model.participants.Application {

    @Override
    @IS_NULLABLE(false)
    Long getCompanyId();

    public List<ApplicationContext> getApplicationContexts();


}
