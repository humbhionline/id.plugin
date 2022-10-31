package in.succinct.id.extensions;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelCreateExtension;
import com.venky.swf.plugins.security.db.model.Role;
import com.venky.swf.plugins.security.db.model.UserRole;
import in.succinct.id.db.model.DefaultUserRoles;
import in.succinct.id.db.model.User;

import java.util.Objects;

public class AfterCreateUser extends AfterModelCreateExtension<User> {
    static {
        registerExtension(new AfterCreateUser());
    }
    @Override
    public void afterCreate(User model) {
        UserRole userRole = Database.getTable(UserRole.class).newRecord();
        userRole.setRoleId(Objects.requireNonNull(Role.getRole(DefaultUserRoles.ALLOWED_ROLES[0])).getId());
        userRole.setUserId(model.getId());
        userRole = Database.getTable(UserRole.class).getRefreshed(userRole);
        if (userRole.getRawRecord().isNewRecord()) {
            userRole.save();
        }
    }
}
