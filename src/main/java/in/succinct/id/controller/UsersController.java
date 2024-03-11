package in.succinct.id.controller;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.model.Model;

import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.plugins.collab.db.model.participants.EventHandler;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationPublicKey;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Paragraph;
import com.venky.swf.views.login.LoginView;
import com.venky.swf.views.login.LoginView.LoginContext;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.user.UserDocument;
import com.venky.swf.plugins.collab.db.model.participants.EndPoint;
import in.succinct.id.db.model.onboarding.user.User;
import in.succinct.id.db.model.onboarding.user.SubmittedUserDocument;
import com.venky.swf.plugins.collab.db.model.participants.WhiteListIp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


    protected View forgot_password(String userName){

        com.venky.swf.db.model.User user = getPath().getUser("NAME",userName);

        if (user == null){
            Select select = new Select().from(UserEmail.class);
            List<UserEmail> emails = select.where(new Expression(select.getPool(), Conjunction.AND)
                    .add(new Expression(select.getPool(),"EMAIL", Operator.EQ,userName))
                    .add(new Expression(select.getPool(), "VALIDATED",Operator.EQ,true))).execute();

            if (emails.size() == 1) {
                user = emails.get(0).getUser();
            }
        }
        if (user == null){
            throw new RuntimeException("Email not registered");
        }

        if (ObjectUtil.isVoid(user.getApiKey())){
            user.generateApiKey(true);
        }

        Link link = new Link();
        link.setUrl(Config.instance().getServerBaseUrl() + "/users/reset_password?ApiKey=" + user.getApiKey());
        link.setText("here");

        Paragraph p = new Paragraph();
        p.setText("To reset password click");
        p.addControl(link);
        Body body = new Body();

        body.addControl(p);
        Html html = new Html();
        html.addControl(body);


        user.getRawRecord().getAsProxy(User.class).sendMail("Password reset request received",html.toString());
        if (getIntegrationAdaptor() != null) {
            return getIntegrationAdaptor().createStatusResponse(getPath(), null, "Mail sent with instructions to change password");
        }else {
            HtmlView view  = createLoginView(LoginContext.PASSWORD_RESET);
            view.setStatus(StatusType.INFO,"Message sent with instructions to change password");
            return view;
        }
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
            map.put(SubmittedUserDocument.class, fields);
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
            List<String> fields = ModelReflector.instance(UserDocument.class).getVisibleFields(Collections.emptyList());
            //fields.remove("ID");
            map.put(UserDocument.class, fields);
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

        removeChildModelClasses(m,User.class, SubmittedUserDocument.class);
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
