package in.succinct.id.controller;

import com.venky.swf.db.model.Model;

import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import in.succinct.id.db.model.Application;
import in.succinct.id.db.model.EventHandler;
import in.succinct.id.db.model.ApplicationPublicKey;
import in.succinct.id.db.model.Company;
import in.succinct.id.db.model.DocumentType;
import in.succinct.id.db.model.EndPoint;
import in.succinct.id.db.model.User;
import in.succinct.id.db.model.UserDocument;
import in.succinct.id.db.model.UserEmail;
import in.succinct.id.db.model.WhiteListIp;

import java.util.Arrays;
import java.util.Collections;
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
        return new RedirectorView(getPath(),"show/" + getPath().getSessionUserId());
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
            map.put(User.class, fields);
            removeModelFields(map,User.class,Arrays.asList("USER_ID","COMPANY_ID","ID","COUNTRY_ID","STATE_ID"));
        }

        {
            List<String> fields = ModelReflector.instance(UserPhone.class).getVisibleFields(Collections.emptyList());
            map.put(UserPhone.class, fields);
            removeModelFields(map,UserPhone.class,Arrays.asList("USER_ID","ID"));
        }

        {
            List<String> fields = ModelReflector.instance(UserEmail.class).getVisibleFields(Collections.emptyList());
            map.put(UserEmail.class, fields);
            removeModelFields(map,UserEmail.class,Arrays.asList("USER_ID","ID"));

        }
        {
            List<String> fields = ModelReflector.instance(Application.class).getVisibleFields(Collections.emptyList());
            map.put(Application.class, fields);
            removeModelFields(map,Application.class,Arrays.asList("ID","COMPANY_ID","ADMIN_ID"));

        }
        {
            List<String> fields = ModelReflector.instance(EventHandler.class).getVisibleFields(Collections.emptyList());
            map.put(EventHandler.class, fields);
            removeModelFields(map, EventHandler.class,Arrays.asList("ID","APPLICATION_ID", "ADMIN_ID"));
        }
        {
            List<String> fields = ModelReflector.instance(ApplicationPublicKey.class).getVisibleFields(Collections.emptyList());

            map.put(ApplicationPublicKey.class, fields);
            removeModelFields(map,ApplicationPublicKey.class,Arrays.asList("ID","APPLICATION_ID"));
        }
        {
            List<String> fields = ModelReflector.instance(EndPoint.class).getVisibleFields(Collections.emptyList());

            map.put(EndPoint.class, fields);
            removeModelFields(map,EndPoint.class,Arrays.asList("ID","APPLICATION_ID"));
        }
        {
            List<String> fields = ModelReflector.instance(WhiteListIp.class).getVisibleFields(Collections.emptyList());
            map.put(WhiteListIp.class, fields);
            removeModelFields(map,WhiteListIp.class,Arrays.asList("ID","APPLICATION_ID"));

        }
        {
            List<String> fields = Arrays.asList("DOCUMENT_TYPE_ID","VERIFICATION_STATUS","VALID_FROM","VALID_TO", "REMARKS","FILE","FILE_CONTENT_NAME","FILE_CONTENT_SIZE","FILE_CONTENT_TYPE");
            map.put(UserDocument.class, fields);
        }
        {
            List<String> fields = ModelReflector.instance(OpenApi.class).getVisibleFields(Collections.emptyList());
            map.put(OpenApi.class, fields);
        }
        {
            List<String> fields = ModelReflector.instance(Country.class).getVisibleFields(Collections.emptyList());
            fields.remove("ID");
            map.put(Country.class, fields);
        }
        {
            List<String> fields = ModelReflector.instance(State.class).getVisibleFields(Collections.emptyList());
            fields.remove("ID");
                    map.put(State.class, fields);
        }
        {
            List<String> fields = ModelReflector.instance(City.class).getVisibleFields(Collections.emptyList());
            fields.remove("ID");
            map.put(City.class, fields);
        }
        {
            List<String> fields = ModelReflector.instance(Company.class).getVisibleFields(Collections.emptyList());
            fields.removeAll(Arrays.asList("ID","ADMIN_ID"));
            map.put(Company.class, fields);
        }
        {
            List<String> fields = ModelReflector.instance(DocumentType.class).getVisibleFields(Collections.emptyList());
            //fields.remove("ID");
            map.put(DocumentType.class, fields);
        }

        return map;
    }

    @Override
    protected Map<Class<? extends Model>, List<Class<? extends Model>>> getConsideredChildModels() {
        Map<Class<? extends Model>, List<Class<? extends Model>>> m = super.getConsideredChildModels();
        //removeChildModelClasses(m,User.class,Application.class);
        removeChildModelClasses(m,User.class,Company.class);
        removeChildModelClasses(m,Company.class,User.class);
        removeChildModelClasses(m,Company.class,UserEmail.class);

        removeChildModelClasses(m,User.class,UserDocument.class);
        //m.get(User.class).add(UserDocument.class);
        m.get(Company.class).add(Application.class);
        m.get(Application.class).addAll(Arrays.asList(ApplicationPublicKey.class, EventHandler.class,WhiteListIp.class, EndPoint.class));
        return m;
    }

    public void removeChildModelClasses(Map<Class<? extends Model>,List<Class<? extends Model>>> map, Class<? extends Model> parentClass, Class<? extends Model> childClass){
        ModelReflector.instance(parentClass).getModelClasses().forEach(pc->{
            ModelReflector.instance(childClass).getModelClasses().forEach(cc->{
                map.get(pc).remove(cc);
            });
        });
    }
    public void removeModelFields(Map<Class<? extends Model>,List<String>> map , Class<? extends Model> modelClass, List<String> fields){
        ModelReflector.instance(modelClass).getModelClasses().forEach(mc->{
            List<String> existing = map.get(mc);
            if (existing != null) {
                existing.removeAll(fields);
            }
        });

    }
}
