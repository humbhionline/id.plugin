package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;
import in.succinct.id.db.model.onboarding.VerifiableDocument;


public interface SubmittedCompanyDocument extends Model, VerifiableDocument {

    @PARTICIPANT
    @HIDDEN
    @IS_NULLABLE(false)
    public Long getCompanyId();
    public void setCompanyId(Long id);
    public Company getCompany();

    @IS_NULLABLE(false)
    public Long getCompanyDocumentId();
    public void setCompanyDocumentId(Long id);
    public CompanyDocument getCompanyDocument();

}
