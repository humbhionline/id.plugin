package in.succinct.id.controller;

import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import in.succinct.id.db.model.User;
import in.succinct.id.db.model.UserEmail;

public class UserEmailsController extends com.venky.swf.plugins.collab.controller.UserEmailsController {
    public UserEmailsController(Path path) {
        super(path);
    }

    @SingleRecordAction(icon = "fas fa-check")
    public View verifyDomain(long id){
        UserEmail userEmail = Database.getTable(UserEmail.class).get(id);
        userEmail.requestDomainVerification();
        String message = "";
        if (userEmail.isDomainVerified()){
            message = "Successfully verified your domain.";
        }else {
            message = "After updating your domain's txt record, wait for some time and try again .";
        }
        if (getReturnIntegrationAdaptor() == null) {
            getPath().addInfoMessage(message);
            return back();
        }else {
            return show(userEmail);
        }
    }

    @SingleRecordAction(icon = "fas fa-hand-point-up")
    public View make_primary(long id){
        UserEmail userEmail = Database.getTable(UserEmail.class).get(id);
        if (userEmail.getCompanyId()!=null){
            User user = userEmail.getUser().getRawRecord().getAsProxy(User.class);
            user.setCompanyId(userEmail.getCompanyId());
            user.setEmail(userEmail.getEmail());
            user.setEmailVerified(false);
            user.save();
        }
        return new RedirectorView(getPath(),"/users", String.format("show/%d", userEmail.getUserId()));
    }
}
