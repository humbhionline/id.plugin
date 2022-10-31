package in.succinct.id.extensions;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.extensions.ApplicationAuthenticator;
import com.venky.swf.extensions.BasicAuthExtension;
import com.venky.swf.path.Path;
import in.succinct.id.db.model.Grant;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class BearerAuthentication extends ApplicationAuthenticator {
    static {
        Registry.instance().registerExtension(ApplicationUtil.APPLICATION_AUTHENTICATOR_EXTENSION,new BearerAuthentication());
    }

    @Override
    protected void authenticate(String scheme, String schemeDetails, ByteArrayInputStream payload, Map<String, String> headers, ObjectHolder<Application> applicationObjectHolder) {
        if (!ObjectUtil.equals(scheme,"bearer")){
            return;
        }
        Grant grant = Database.getTable(Grant.class).newRecord();
        grant.setAccessToken(new String(Base64.getDecoder().decode(schemeDetails.getBytes(StandardCharsets.UTF_8))));
        grant = Database.getTable(Grant.class).find(grant,false);
        if (grant!= null){
            if (grant.getAccessTokenExpiry() > System.currentTimeMillis()) {
                applicationObjectHolder.set(grant.getApplication());
            }else {
                grant.destroy();
            }
        }
    }
}
