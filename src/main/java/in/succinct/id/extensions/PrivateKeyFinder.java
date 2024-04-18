package in.succinct.id.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.routing.Config;
import in.succinct.id.db.model.onboarding.company.Application;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;

import java.util.Optional;


public class PrivateKeyFinder implements Extension {
    static {
        Registry.instance().registerExtension("private.key.get.Ed25519",new PrivateKeyFinder());
    }

    @Override
    public void invoke(Object... context) {
        ObjectHolder<String> holder = (ObjectHolder<String>) context[0];
        if (holder.get() != null){
            return;
        }
        Application application = ApplicationUtil.find(Config.instance().getHostName(), Application.class);
        if (application == null){
            return;
        }
        Optional<ApplicationPublicKey> currentKey = application.getApplicationPublicKeys().stream().
                map(applicationPublicKey -> applicationPublicKey.getRawRecord().getAsProxy(ApplicationPublicKey.class)).
                filter(applicationPublicKey -> applicationPublicKey.isVerified() && !applicationPublicKey.isExpired()).findFirst();

        if (!currentKey.isPresent()){
            return;
        }
        String keyId = currentKey.get().getKeyId();
        CryptoKey signingKey = CryptoKey.find(keyId,CryptoKey.PURPOSE_SIGNING);

        String privateKey = signingKey.getPrivateKey();
        holder.set(String.format("%s|%s:%s",application.getAppId(),keyId,privateKey));
    }
}
