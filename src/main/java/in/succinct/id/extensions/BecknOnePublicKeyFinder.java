package in.succinct.id.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;

public class BecknOnePublicKeyFinder implements Extension {
    static {
        Registry.instance().registerExtension("beckn.public.key.get",new BecknOnePublicKeyFinder());
    }
    @Override
    public void invoke(Object... context) {
        String subscriber_id = (String)context[0];
        String uniqueKeyId = (String)context[1];
        ObjectHolder<String> publicKeyHolder = (ObjectHolder<String>) context[2];
        if (publicKeyHolder.get() != null){
            return;
        }
        ApplicationPublicKey key = com.venky.swf.db.model.application.ApplicationPublicKey.find(com.venky.swf.db.model.application.ApplicationPublicKey.PURPOSE_SIGNING,uniqueKeyId, ApplicationPublicKey.class);
        if (key != null && !key.getRawRecord().isNewRecord()){
            publicKeyHolder.set(key.getPublicKey());
        }
    }
}
