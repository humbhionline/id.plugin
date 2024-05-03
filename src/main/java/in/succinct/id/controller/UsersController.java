package in.succinct.id.controller;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationPublicKey;
import com.venky.swf.plugins.collab.db.model.participants.EndPoint;
import com.venky.swf.plugins.collab.db.model.participants.EventHandler;
import com.venky.swf.plugins.collab.db.model.participants.WhiteListIp;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Paragraph;
import com.venky.swf.views.login.LoginView.LoginContext;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.company.CompanyNetworkDomain;
import in.succinct.id.db.model.onboarding.company.CompanyNetworkUsage;
import in.succinct.id.db.model.onboarding.company.NetworkDomain;
import in.succinct.id.db.model.onboarding.company.NetworkUsage;
import in.succinct.id.db.model.onboarding.user.User;
import in.succinct.plugins.kyc.db.model.submissions.Document;
import in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
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


    protected View sendPasswordResetEmail(User user, String subject, String text){
        if (ObjectUtil.isVoid(user.getApiKey())){

            user.generateApiKey(true);
        }

        Link link = new Link();
        link.setUrl(Config.instance().getProperty("swf.password.reset.link",
                Config.instance().getServerBaseUrl() + "/users/reset_password")+"?ApiKey=" + user.getApiKey());
        link.setText("here");

        Paragraph p = new Paragraph();
        p.setText(String.format("%sTo set your password , click ",StringUtil.valueOf(text)));
        p.addControl(link);
        Body body = new Body();

        body.addControl(p);
        Html html = new Html();
        html.addControl(body);


        user.getRawRecord().getAsProxy(User.class).sendMail(subject == null ? "Password reset request received" : subject,
                html.toString());

        if (getIntegrationAdaptor() != null) {
            return getIntegrationAdaptor().createStatusResponse(getPath(), null, "Mail sent with instructions to set password");
        }else {
            if (getPath().getSessionUserId() == null) {
                HtmlView view = createLoginView(LoginContext.PASSWORD_RESET);
                view.setStatus(StatusType.INFO, "Message sent with instructions to set password");
                return view;
            }else {
                getPath().addInfoMessage("Message sent with instructions to set password");
                return back();
            }
        }

    }

    public View sendInviteMail(long id){
        User user = Database.getTable(User.class).get(id);
        return sendPasswordResetEmail(user,"Congratulations! You are granted access to the platform. ", """
                    Congratulations, You are granted access to the platform.
                    You need to first set your password to start enjoying its features.
                    - Admin
                    
                    
                    """
        );
    }

    @Override
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

        return sendPasswordResetEmail(user.getRawRecord().getAsProxy(User.class),null,null);

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
    public List<String> getExistingModelFields(Map<Class<? extends Model>,List<String>> map , Class<? extends Model> modelClass){
        List<String> existing = new SequenceSet<>();
        ModelReflector.instance(modelClass).getModelClasses().forEach(mc->{
            List<String> e = map.get(mc);
            if (e != null) {
                existing.addAll(e);
            }
        });
        return existing;
    }
    private void addToIncludedModelFieldsMap(Map<Class<? extends Model>,List<String>> map, Class<? extends Model> clazz , List<String> excludedFields){
        List<String> fields = ModelReflector.instance(clazz).getVisibleFields(Collections.emptyList());
        List<String> oldFields = getExistingModelFields(map,clazz);

        SequenceSet<String> finalFields = new SequenceSet<>();
        finalFields.addAll(fields);
        excludedFields.forEach(finalFields::remove);
        finalFields.addAll(oldFields);
        map.put(clazz,finalFields);
    }
    @Override
    protected Map<Class<? extends Model>, List<String>> getIncludedModelFields() {
        Map<Class<? extends Model>,List<String>> map = super.getIncludedModelFields();
        if( getReturnIntegrationAdaptor() == null ){
            return map ;
        }

        addToIncludedModelFieldsMap(map,User.class,Arrays.asList("COMPANY_ID","ID","COUNTRY_ID","STATE_ID"));
        addToIncludedModelFieldsMap(map,UserPhone.class,Arrays.asList("USER_ID","ID"));
        addToIncludedModelFieldsMap(map,UserEmail.class,Arrays.asList("USER_ID","ID"));
        addToIncludedModelFieldsMap(map,Application.class,Arrays.asList("ID","COMPANY_ID","ADMIN_ID"));
        addToIncludedModelFieldsMap(map,EventHandler.class,Arrays.asList("ID","APPLICATION_ID","ADMIN_ID"));
        addToIncludedModelFieldsMap(map,ApplicationPublicKey.class,Arrays.asList("ID","APPLICATION_ID"));
        addToIncludedModelFieldsMap(map,EndPoint.class,Arrays.asList("ID","APPLICATION_ID"));
        addToIncludedModelFieldsMap(map,WhiteListIp.class,Arrays.asList("ID","APPLICATION_ID"));
        addToIncludedModelFieldsMap(map, SubmittedDocument.class,Arrays.asList("ID","DOCUMENTED_MODEL_ID", "DOCUMENTED_MODEL_NAME"));
        addToIncludedModelFieldsMap(map,OpenApi.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map,Country.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map,State.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map,City.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map,Company.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map, Document.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map, CompanyNetworkDomain.class,Arrays.asList("ID","COMPANY_ID"));
        addToIncludedModelFieldsMap(map, NetworkDomain.class,Collections.singletonList("ID"));
        addToIncludedModelFieldsMap(map, CompanyNetworkUsage.class,Arrays.asList("ID","COMPANY_ID"));
        addToIncludedModelFieldsMap(map, NetworkUsage.class,Collections.singletonList("ID"));



        return map;
    }

    @Override
    protected Map<Class<? extends Model>, List<Class<? extends Model>>> getConsideredChildModels() {
        Map<Class<? extends Model>, List<Class<? extends Model>>> m = super.getConsideredChildModels();
        //removeChildModelClasses(m,User.class,Application.class);
        removeChildModelClasses(m,User.class,Company.class);
        removeChildModelClasses(m,Company.class,User.class);
        removeChildModelClasses(m,Company.class,UserEmail.class);

        removeChildModelClasses(m,User.class, SubmittedDocument.class);
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

    @RequireLogin(false)
    public View hasPassword() throws Exception {
        ensureIntegrationMethod(HttpMethod.POST);
        List<com.venky.swf.db.model.User> users = getIntegrationAdaptor().readRequest(getPath());
        JSONObject out = new JSONObject();
        if (users.size() == 1){
            User user = users.get(0).getRawRecord().getAsProxy(User.class);
            boolean newRecord = user.getRawRecord().isNewRecord();
            out.put("Registered",!newRecord);
            if (!newRecord){
                out.put("PasswordSet", !ObjectUtil.isVoid(user.getPassword()));
            }

        }

        return new BytesView(getPath(), out.toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }
}
