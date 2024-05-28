package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import in.succinct.id.db.model.onboarding.user.User;

public class UserEmailExtension extends ModelOperationExtension<UserEmail> {
    static {
        registerExtension(new UserEmailExtension());
    }

    /* Don't auto create company
    @Override
    protected void beforeValidate(UserEmail instance) {
        super.beforeValidate(instance);
        Company company = instance.getCompany();

        if (company != null && company.getRawRecord().isNewRecord()) {
            company.save();
        }
    }

     */

    @Override
    protected void afterCreate(UserEmail instance) {
        super.afterCreate(instance);
        if (!instance.isValidated()) {
            instance.sendOtp();
        }else{
            User user = instance.getUser().getRawRecord().getAsProxy(User.class);
            if (ObjectUtil.equals(user.getEmail(),instance.getEmail())){
                user.setEmailVerified(true);
                user.setTxnProperty("being.verified",true);
                user.save();
            }
        }
    }
}
