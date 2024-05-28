package in.succinct.id.extensions;

import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.user.User;
import in.succinct.plugins.kyc.util.DocumentedModelRegistry;

public class DocumentedModelRegistrar {
    static {
        DocumentedModelRegistry.getInstance().register(Company.class);
        DocumentedModelRegistry.getInstance().register(User.class);
    }
}
