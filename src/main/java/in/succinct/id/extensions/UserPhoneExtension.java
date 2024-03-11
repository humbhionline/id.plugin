package in.succinct.id.extensions;

import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;

public class UserPhoneExtension extends ModelOperationExtension<UserPhone> {
    static {
        registerExtension(new UserPhoneExtension());
    }
    @Override
    public void afterCreate(UserPhone model) {
        if (!model.isValidated()){
            model.sendOtp();
        }
    }
}
