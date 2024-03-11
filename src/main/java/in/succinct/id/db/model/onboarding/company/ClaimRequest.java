package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;

import java.sql.Timestamp;

public interface ClaimRequest extends Model , CompanySpecific {
    @IS_NULLABLE(false)
    @PROTECTION(Kind.DISABLED)
    @UNIQUE_KEY
    public Long getCompanyId();

    @Override
    @UNIQUE_KEY
    Long getCreatorUserId();

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION(Kind.DISABLED)
    @Index
    public boolean isDomainVerified();
    public void setDomainVerified(boolean domainVerified);


    public String getTxtValue();
    public void setTxtValue(String txtValue);

    @IS_VIRTUAL
    public String getTxtName();


    @IS_VIRTUAL
    @HIDDEN
    public boolean isTxtRecordVerified();


    @IS_VIRTUAL
    public void requestDomainVerification();

    @IS_NULLABLE
    public Timestamp getVerifiedAt();
    public void setVerifiedAt(Timestamp timestamp);

}
