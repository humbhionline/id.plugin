package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;

import java.sql.Timestamp;

public interface UserEmail extends com.venky.swf.plugins.collab.db.model.user.UserEmail {

    @IS_VIRTUAL
    public boolean isCompanyAdmin();

    @IS_VIRTUAL(value = false)
    public Long getCompanyId();
    public void setCompanyId(Long id);
    public Company getCompany();

    public String getCompanyGstIn();
    public void setCompanyGstIn(String companyGstIn);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION(Kind.EDITABLE)
    public boolean isGstInVerified();
    public void setGstInVerified(boolean gstInVerified);

    public String getCompanyRegistrationNumber();
    public void setCompanyRegistrationNumber(String companyRegistrationNumber);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION(Kind.EDITABLE)
    public boolean isCompanyRegistrationNumberVerified();
    public void setCompanyRegistrationNumberVerified(boolean companyRegistrationNumberVerified);

    @IS_VIRTUAL
    public String getCompanyGstVerificationUrl();


    @IS_VIRTUAL
    public String getCompanyVerificationUrl();

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION(Kind.DISABLED)
    public boolean isDomainVerified();
    public void setDomainVerified(boolean domainVerified);

    @IS_VIRTUAL
    public String getTxtName();

    @IS_NULLABLE
    public String getTxtValue();
    public void setTxtValue(String txtValue);

    @IS_VIRTUAL
    @HIDDEN
    public boolean isTxtRecordVerified();


    @IS_VIRTUAL
    public void requestDomainVerification();

    @IS_NULLABLE
    public Timestamp getVerifiedAt();
    public void setVerifiedAt(Timestamp timestamp);

}
