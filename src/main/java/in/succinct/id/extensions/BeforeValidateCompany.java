package in.succinct.id.extensions;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.exceptions.AccessDeniedException;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.company.CompanyDocument;

public class BeforeValidateCompany extends BeforeModelValidateExtension<Company> {
    static {
        registerExtension(new BeforeValidateCompany());
    }
    @Override
    public void beforeValidate(Company model) {
        if (model.isKycComplete() && model.getRawRecord().isFieldDirty("KYC_COMPLETE")){
            if (!model.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(model.getTxnProperty("kyc.complete"))){
                throw new AccessDeniedException();
            }
        }
        if (Database.getTable(CompanyDocument.class).recordCount() == 0){
            model.setKycComplete(true);
        }
    }
}
