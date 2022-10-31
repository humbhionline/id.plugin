package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.id.db.model.Company;

public class BeforeValidateCompany extends BeforeModelValidateExtension<Company> {
    @Override
    public void beforeValidate(Company model) {
        if (!ObjectUtil.isVoid(model.getCompanyGstIn()) && model.getRawRecord().isFieldDirty("COMPANY_GST_IN")){
            if (!model.getReflector().getJdbcTypeHelper().getTypeRef(Boolean.class).getTypeConverter().
                    valueOf(model.getTxnProperty("kyc.being.verified") )){
                throw new RuntimeException("Cannot change gst manually!");
            }
        }
        if (!ObjectUtil.isVoid(model.getCompanyRegistrationNumber()) &&
                model.getRawRecord().isFieldDirty("COMPANY_REGISTRATION_NUMBER")){
            if (!model.getReflector().getJdbcTypeHelper().getTypeRef(Boolean.class).getTypeConverter().
                    valueOf(model.getTxnProperty("kyc.being.verified") )){
                throw new RuntimeException("Cannot change registration manually!");
            }
        }
    }
}
