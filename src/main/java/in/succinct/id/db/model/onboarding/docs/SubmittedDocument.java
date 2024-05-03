package in.succinct.id.db.model.onboarding.docs;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.user.User;

public interface SubmittedDocument extends in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument {

    @IS_VIRTUAL
    public Long getCompanyId();
    public void setCompanyId(Long id);
    public Company getCompany();

    @IS_VIRTUAL
    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();
}
