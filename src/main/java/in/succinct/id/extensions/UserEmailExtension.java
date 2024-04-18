package in.succinct.id.extensions;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;

public class UserEmailExtension extends ModelOperationExtension<UserEmail> {
    static {
        registerExtension(new UserEmailExtension());
    }

    @Override
    protected void beforeValidate(UserEmail instance) {
        super.beforeValidate(instance);
        Company company = instance.getCompany();

        if (company != null && company.getRawRecord().isNewRecord()) {
            company.save();
        }
    }
}
