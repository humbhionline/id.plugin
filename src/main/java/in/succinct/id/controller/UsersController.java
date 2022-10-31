package in.succinct.id.controller;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;
import com.venky.swf.views.View;
import in.succinct.id.db.model.User;
import in.succinct.id.db.model.UserEmail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersController extends com.venky.swf.plugins.collab.controller.UsersController {

    public UsersController(Path path) {
        super(path);
    }

    @Override
    public View save() {
        return super.save();
    }

    public View current() {
        return show(getSessionUser().getRawRecord().getAsProxy(User.class));
    }


    @Override
    protected String[] getIncludedFields() {
        Map<Class<? extends Model>, List<String>> map  = getIncludedModelFields();
        if (map.containsKey(User.class)){
            return map.get(User.class).toArray(new String[]{});
        }else {
            return null;
        }
    }

    @Override
    protected Map<Class<? extends Model>, List<String>> getIncludedModelFields() {
        Map<Class<? extends Model>,List<String>> map = super.getIncludedModelFields();
        if( getReturnIntegrationAdaptor() == null ){
            return map ;
        }

        {
            List<String> fields = ModelReflector.instance(User.class).getVisibleFields(Collections.emptyList());
            fields.remove("USER_ID");
            map.put(User.class, fields);
        }

        {
            List<String> fields = ModelReflector.instance(UserPhone.class).getVisibleFields(Collections.emptyList());
            fields.remove("USER_ID");
            map.put(UserPhone.class, fields);
        }

        {
            List<String> fields = ModelReflector.instance(UserEmail.class).getVisibleFields(Collections.emptyList());
            fields.remove("USER_ID");
            map.put(UserEmail.class, fields);
        }

        return map;
    }
}
