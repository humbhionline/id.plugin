package in.succinct.id.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;
import in.succinct.id.db.model.Grant;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.StringTokenizer;

public class RequestAuthenticator implements Extension {
    static{
        Registry.instance().registerExtension(Path.REQUEST_AUTHENTICATOR,new RequestAuthenticator());
    }
    @Override
    public void invoke(Object... context) {
        Path path = (Path) context[0];
        ObjectHolder<User> userObjectHolder = (ObjectHolder<User>) context[1];

        Map<String,String> headers = path.getHeaders();
        String authorization = headers.get("Authorization");
        if (ObjectUtil.isVoid(authorization)) {
            return ;
        }

        if (userObjectHolder.get() != null){
            return;
        }

        StringTokenizer authTokenizer = new StringTokenizer(authorization);
        String scheme = authTokenizer.nextToken().toLowerCase();
        String schemeDetails = authorization.substring(scheme.length()).trim();

        if (!ObjectUtil.equals(scheme,"bearer")){
            return;
        }

        Grant grant = Database.getTable(Grant.class).newRecord();
        grant.setAccessToken(new String(Base64.getDecoder().decode(schemeDetails.getBytes(StandardCharsets.UTF_8))));
        grant = Database.getTable(Grant.class).find(grant,false);
        if (grant!= null ){
            if (grant.getAccessTokenExpiry() > System.currentTimeMillis()) {
                userObjectHolder.set(grant.getUser());
            }else {
                grant.destroy();
            }
        }
    }
}
