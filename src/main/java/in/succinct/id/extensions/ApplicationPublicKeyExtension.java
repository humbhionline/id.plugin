package in.succinct.id.extensions;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.random.Randomizer;
import com.venky.core.security.Crypt;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.db.model.application.Event.EventResult;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Request;
import in.succinct.id.db.model.onboarding.company.Application;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplicationPublicKeyExtension extends ModelOperationExtension<ApplicationPublicKey> {
    static {
        registerExtension(new ApplicationPublicKeyExtension());
    }

    @Override
    protected void beforeValidate(ApplicationPublicKey instance) {
        if (instance.getRawRecord().isNewRecord()){
            if (instance.isVerified() &&
                    !instance.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().
                            valueOf(instance.getTxnProperty("being.verified"))) {
                    throw new RuntimeException("Key requires verification via /subscribe and /on_subcribe.");
            }
            return;
        }
        if (instance.isVerified() ){
            Set<String> fieldsAllowedToBeChanged = new HashSet<String>() {{
                add("VALID_FROM");
                add("VALID_UNTIL");
                add("UPDATED_AT");
            }};
            if (instance.getRawRecord().isFieldDirty("VERIFIED")){
                if (!instance.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(instance.getTxnProperty("being.verified"))){
                    if (ObjectUtil.equals(ApplicationPublicKey.PURPOSE_ENCRYPTION,instance.getPurpose())){
                        throw  new RuntimeException("Encryption key can be verified by solving the on_subscribe challenge");
                    }else {
                        throw  new RuntimeException("Signature key can be verified by calling a signed subscribe request");
                    }
                }
                fieldsAllowedToBeChanged.add("VERIFIED");
            }
            Set<String> fieldsChanged = new HashSet<>(instance.getRawRecord().getDirtyFields());
            fieldsChanged.removeAll(fieldsAllowedToBeChanged);
            if (!fieldsChanged.isEmpty()) {
                throw new RuntimeException("Cannot change " + fieldsChanged + " once the key is verified. Please create new one.");
            }
        }else {
            if (instance.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(
                    instance.getRawRecord().getOldValue("VERIFIED"))){
                throw new RuntimeException("Cannot change verification status once verified");
            }
        }
    }

    @Override
    protected void afterSave(ApplicationPublicKey instance) {
        instance.verify(true);
    }


}
