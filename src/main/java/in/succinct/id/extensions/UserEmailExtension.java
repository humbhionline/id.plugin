package in.succinct.id.extensions;

import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;

public class UserEmailExtension extends ModelOperationExtension<UserEmail> {
    static {
        registerExtension(new UserEmailExtension());
    }
    @Override
    public void afterCreate(UserEmail model) {
        if (!model.isValidated()){
            model.sendOtp();
        }
    }
}
