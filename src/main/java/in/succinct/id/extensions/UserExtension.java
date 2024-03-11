package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import com.venky.swf.plugins.security.db.model.Role;
import com.venky.swf.plugins.security.db.model.UserRole;
import in.succinct.id.db.model.DefaultUserRoles;
import in.succinct.id.db.model.onboarding.user.User;
import org.hazlewood.connor.bottema.emailaddress.EmailAddressValidator;

import java.util.Objects;

public class UserExtension extends ModelOperationExtension<User> {
    static {
        registerExtension(new UserExtension());
    }
    public void afterCreate(User model) {
        if (!model.getName().equals("root")) {
            UserRole userRole = Database.getTable(UserRole.class).newRecord();
            userRole.setRoleId(Objects.requireNonNull(Role.getRole(DefaultUserRoles.ALLOWED_ROLES[0])).getId());
            userRole.setUserId(model.getId());
            userRole = Database.getTable(UserRole.class).getRefreshed(userRole);
            if (userRole.getRawRecord().isNewRecord()) {
                userRole.save();
            }
        }
    }

    @Override
    public void beforeValidate(User model) {
        //Force login to be email
        if (!ObjectUtil.isVoid(model.getName()) && model.getName().contains("@")){
            if (EmailAddressValidator.isValid(model.getName())) {
                if (ObjectUtil.isVoid(model.getEmail()) ||
                        !ObjectUtil.equals(model.getName(),model.getEmail())) {
                    model.setEmail(model.getName().toLowerCase());
                }
            }else {
                throw new RuntimeException("Please enter a valid email address!");
            }
        }else if (!ObjectUtil.equals(model.getName(),"root")){
            throw new RuntimeException("Please enter a valid email address!");
        }

        if (!ObjectUtil.isVoid(model.getPhoneNumber())){
            model.setPhoneNumber(Phone.sanitizePhoneNumber(model.getPhoneNumber()));
        }
        /*
        if (ObjectUtil.isVoid(model.getCompanyId())){
            model.setCompanyId(CompanyUtil.getCompanyId());
        }*/
        if (!ObjectUtil.isVoid(model.getAlternatePhoneNumber())){
            model.setAlternatePhoneNumber(Phone.sanitizePhoneNumber(model.getAlternatePhoneNumber()));
        }


        validateAddress(model);

    }

    private void validateAddress(User u){
        User model = u.getRawRecord().getAsProxy(User.class);

        boolean beingVerified = model.getReflector().getJdbcTypeHelper().getTypeRef(Boolean.class).
                getTypeConverter().valueOf(model.getTxnProperty("being.verified"));

        boolean addressChanged = Address.isAddressChanged(model);


        com.venky.swf.db.model.User cu = Database.getInstance().getCurrentUser();
        User currentUser = cu == null ? null : cu.getRawRecord().getAsProxy(User.class);

        if (addressChanged && !Address.isAddressVoid(model) && !beingVerified){
            model.setAddressVerified(false);
        }
        if (model.getRawRecord().isFieldDirty("PHONE_NUMBER") && !ObjectUtil.isVoid(model.getPhoneNumber()) && !beingVerified){
            model.setPhoneNumberVerified(false);
        }

        if (model.getRawRecord().isFieldDirty("EMAIL") && !ObjectUtil.isVoid(model.getEmail()) && !beingVerified) {
            model.setEmailVerified(false);
        }
        if (addressChanged){
            model.setLat(null);model.setLng(null);
        }
    }
}
