package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ModelOperationExtension;
import in.succinct.id.db.model.onboarding.company.Application;
import in.succinct.id.db.model.onboarding.company.Company;

import java.util.UUID;

public class CompanyExtension extends ModelOperationExtension<Company> {
    static {
        registerExtension(new CompanyExtension());
    }

    @Override
    protected void beforeValidate(Company company) {
        if (ObjectUtil.isVoid(company.getSubscriberId())){
            if (!ObjectUtil.isVoid(company.getDomainName())) {
                company.setSubscriberId(company.getDomainName());
            }else {
                company.setSubscriberId(UUID.randomUUID().toString());
            }
        }
    }


}
