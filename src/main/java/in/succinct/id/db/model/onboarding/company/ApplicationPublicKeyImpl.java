package in.succinct.id.db.model.onboarding.company;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.random.Randomizer;
import com.venky.core.security.Crypt;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.db.model.application.Event.EventResult;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Request;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class ApplicationPublicKeyImpl extends ModelImpl<ApplicationPublicKey> {
    public ApplicationPublicKeyImpl() {
    }

    public ApplicationPublicKeyImpl(ApplicationPublicKey proxy) {
        super(proxy);
    }

    public void verify(boolean async){
        validate(getProxy(), async);
    }

    private static void validate(ApplicationPublicKey key ,boolean async  ) {
        final ApplicationPublicKey encryptionApplicationPublicKey;
        if (ObjectUtil.equals(ApplicationPublicKey.PURPOSE_ENCRYPTION,key.getPurpose())) {
            encryptionApplicationPublicKey = key;
        }else{
            encryptionApplicationPublicKey = com.venky.swf.db.model.application.ApplicationPublicKey.find(
                    ApplicationPublicKey.PURPOSE_ENCRYPTION,key.getKeyId(), ApplicationPublicKey.class);
        }
        if (encryptionApplicationPublicKey == null || encryptionApplicationPublicKey.getRawRecord().isNewRecord() || encryptionApplicationPublicKey.isVerified()){
            return;
        }

        Task task = new Task() {
            @Override
            public void execute() {
                Event endPointVerification = Event.find("end_point_verification");
                Application application = encryptionApplicationPublicKey.getApplication().getRawRecord().getAsProxy(Application.class);
                JSONObject verificationPayload = getVerificationPayload(encryptionApplicationPublicKey);
                String expected_answer = (String)verificationPayload.remove("expected_answer");
                List<EventResult> results = endPointVerification.raise(application, verificationPayload);
                for (EventResult result : results) {
                    if (result.response != null) {
                        try {
                            JSONObject response = (JSONObject) JSONValue.parseWithException(new InputStreamReader((ByteArrayInputStream) result.response));
                            if (ObjectUtil.equals(response.get("answer"), expected_answer)) {
                                encryptionApplicationPublicKey.setTxnProperty("being.verified", true);
                                encryptionApplicationPublicKey.setVerified(true);
                                encryptionApplicationPublicKey.save();
                            }
                        } catch (Exception ex) {
                            //
                        }
                    }
                }
            }
        };
        if (async){
            TaskManager.instance().executeAsync(task,false);
        }else {
            TaskManager.instance().execute(task);
        }

    }
    @SuppressWarnings("unchecked")
    private static JSONObject getVerificationPayload(ApplicationPublicKey encryptionApplicationPublicKey) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            otp.append(Randomizer.getRandomNumber(i == 0 ? 1 : 0, 9));
        }
        //com.venky.swf.db.model.application.ApplicationPublicKey pk = com.venky.swf.db.model.application.ApplicationPublicKey.find(ApplicationPublicKey.PURPOSE_ENCRYPTION,pub_key_id);

        PublicKey partyPublicKey = Request.getEncryptionPublicKey(encryptionApplicationPublicKey.getPublicKey());
        CryptoKey cryptoKey = CryptoKey.find(Config.instance().getHostName() + ".k1",CryptoKey.PURPOSE_ENCRYPTION);
        PrivateKey myPrivateKey = Crypt.getInstance().getPrivateKey(Request.ENCRYPTION_ALGO, cryptoKey.getPrivateKey());
        try {

            KeyAgreement agreement = KeyAgreement.getInstance(Request.ENCRYPTION_ALGO);
            agreement.init(myPrivateKey);
            agreement.doPhase(partyPublicKey, true);
            SecretKey symKey = agreement.generateSecret("TlsPremasterSecret");

            String encrypted = Crypt.getInstance().encrypt(otp.toString(), "AES", symKey);
            JSONObject input = new JSONObject();
            input.put("subscriber_id", encryptionApplicationPublicKey.getApplication().getAppId());
            input.put("pub_key_id", encryptionApplicationPublicKey.getKeyId());
            input.put("challenge", encrypted);
            input.put("expected_answer",otp.toString());
            return input;
        }catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
