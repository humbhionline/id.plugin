package in.succinct.id.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;

public class BeforeValidateUserPhone extends BeforeModelValidateExtension<UserPhone> {
    static {
        registerExtension(new BeforeValidateUserPhone());
    }

    @Override
    public void beforeValidate(UserPhone model) {
        if (model.getRawRecord().isFieldDirty("PHONE_NUMBER") ) {
            model.setValidated(false);
        }
    }
}
