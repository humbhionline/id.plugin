package in.succinct.id.extensions;

import com.venky.swf.db.extensions.ModelOperationExtension;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;

public class ApplicationPublicKeyExtension extends ModelOperationExtension<ApplicationPublicKey> {
    static {
        registerExtension(new ApplicationPublicKeyExtension());
    }


    @Override
    protected void afterSave(ApplicationPublicKey instance) {
        instance.verify(true);
    }


}
