package in.succinct.id.extensions;

import com.venky.swf.db.extensions.AfterModelCreateExtension;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;

public class AfterCreateUserEmail extends AfterModelCreateExtension<UserEmail> {
    static {
        registerExtension(new AfterCreateUserEmail());
    }
    @Override
    public void afterCreate(UserEmail model) {
        if (!model.isValidated()){
            model.sendOtp();
        }
    }
}
