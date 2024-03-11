package in.succinct.id.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;


public class BeforeValidateUserEmail extends BeforeModelValidateExtension<UserEmail> {
    static {
        registerExtension(new BeforeValidateUserEmail());
    }
    @Override
    public void beforeValidate(UserEmail model) {
        if (model.getRawRecord().isFieldDirty("EMAIL")) {
            model.setValidated(false);
        }
    }
}
