package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import in.succinct.id.db.model.User;

public class BeforeValidateUser extends BeforeModelValidateExtension<User> {
    static {
        registerExtension(new BeforeValidateUser());
    }
    @Override
    public void beforeValidate(User model) {


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
