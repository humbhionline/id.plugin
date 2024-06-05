package in.succinct.id.db.model.onboarding.docs;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.user.User;

public interface SubmittedDocument extends in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument{
    @PARTICIPANT
    public Long getDocumentedModelId();

    @PARTICIPANT("COMPANY")
    public Long getCompanyId();
    public void setCompanyId(Long id);
    public Company getCompany();

    @PARTICIPANT("USER")
    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();
}
