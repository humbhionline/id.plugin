package in.succinct.id.controller;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationContext;
import com.venky.swf.plugins.collab.db.model.participants.EndPoint;
import com.venky.swf.plugins.collab.util.CompanyUtil;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Subscribers;
import in.succinct.id.core.db.model.onboarding.company.Application;
import in.succinct.id.core.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;
import in.succinct.id.util.LookupManager;
import in.succinct.json.JSONAwareWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Map;

@SuppressWarnings("unused")
public class SubscribersController extends Controller {
    public SubscribersController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    @Override
    public View register() {
        String payload  ;
        try {
            payload = StringUtil.read(getPath().getInputStream());
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
        JSONAware jsonAware = ObjectUtil.isVoid(payload) ? new JSONArray() : JSONAwareWrapper.parse(payload);
        Subscribers subscribers = new Subscribers();
        if (jsonAware instanceof JSONObject){
            subscribers.add(new Subscriber((JSONObject) jsonAware));
        }else {
            subscribers.setInner((JSONArray) jsonAware);
        }


        for (Subscriber subscriber :subscribers){
            Company networkParticipant = CompanyUtil.getCompany(subscriber.getSubscriberId()).getRawRecord().getAsProxy(Company.class);

            if (subscriber.getOrganization() == null){
                subscriber.setOrganization(new Organization());
                subscriber.getOrganization().setName(networkParticipant.getName());
                subscriber.getOrganization().setDateOfIncorporation(networkParticipant.getDateOfIncorporation());
            }

            networkParticipant.setDateOfIncorporation(new java.sql.Date(subscriber.getOrganization().getDateOfIncorporation().getTime()));
            networkParticipant.setName(subscriber.getOrganization().getName());
            if (networkParticipant.getRawRecord().isNewRecord()){
                networkParticipant.setKycComplete(false);
            }
            networkParticipant.save();

            Application role = Database.getTable(Application.class).newRecord();
            role.setCompanyId(networkParticipant.getId());
            role.setAppId(subscriber.getSubscriberId());
            //role.setStatus(NetworkRole.SUBSCRIBER_STATUS_INITIATED);
            role.setHashingAlgorithm("BLAKE2B-512");
            role.setSigningAlgorithm("Ed25519");
            role.setHeaders("(created) (expires) digest");
            role = Database.getTable(Application.class).getRefreshed(role);
            role.save();
            OpenApi openApi = OpenApi.find(subscriber.getType());

            EndPoint endPoint = Database.getTable(EndPoint.class).newRecord();
            endPoint.setApplicationId(role.getId());
            endPoint.setBaseUrl(subscriber.getSubscriberUrl());
            endPoint.setOpenApiId(openApi.getId());
            endPoint = Database.getTable(EndPoint.class).getRefreshed(endPoint);
            endPoint.save();

            subscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_INITIATED);

            ApplicationPublicKey key = Database.getTable(ApplicationPublicKey.class).newRecord();
            key.setApplicationId(role.getId());
            key.setKeyId(subscriber.getPubKeyId());
            key.setPurpose(ApplicationPublicKey.PURPOSE_SIGNING);
            key.setPublicKey(Request.getRawSigningKey(subscriber.getSigningPublicKey()));
            key.setAlgorithm("Ed25519");
            key = Database.getTable(ApplicationPublicKey.class).getRefreshed(key);
            if (key.getRawRecord().isNewRecord()){
                key.setVerified(false);
            }
            if (!key.getRawRecord().isNewRecord() && key.isDirty()){
                throw  new RuntimeException("Cannot modify key attributes as part of registration");
            }
            key.setValidFrom(new Timestamp(subscriber.getValidFrom().getTime()));
            key.setValidUntil(new Timestamp(subscriber.getValidTo().getTime()));
            key.save();

            key = Database.getTable(ApplicationPublicKey.class).newRecord();
            key.setApplicationId(role.getId());
            key.setKeyId(subscriber.getPubKeyId());
            key.setPurpose(ApplicationPublicKey.PURPOSE_ENCRYPTION);
            key.setPublicKey(Request.getRawEncryptionKey(subscriber.getEncrPublicKey()));
            key.setAlgorithm("X25519");
            key = Database.getTable(ApplicationPublicKey.class).getRefreshed(key);
            if (key.getRawRecord().isNewRecord()){
                key.setVerified(false);
            }
            if (!key.getRawRecord().isNewRecord() && key.isDirty()){
                throw  new RuntimeException("Cannot modify key attributes as part of registration");
            }
            key.setValidFrom(new Timestamp(subscriber.getValidFrom().getTime()));
            key.setValidUntil(new Timestamp(subscriber.getValidTo().getTime()));
            key.save();

            loadRegion(subscriber,role);
        }
        if (subscribers.size() == 1){
            return new BytesView(getPath(),subscribers.getInner().get(0).toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
        }else {
            return new BytesView(getPath(),subscribers.getInner().toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
        }
    }

    @RequireLogin(false)
    public View subscribe() throws Exception{
        String payload = StringUtil.read(getPath().getInputStream());
        Request request = new Request(payload);
        Map<String,String> params = request.extractAuthorizationParams("Authorization",getPath().getHeaders());
        if (params.isEmpty()){
            throw new RuntimeException("Signature Verification failed");
        }

        String pub_key_id = params.get("pub_key_id");
        String subscriber_id = params.get("subscriber_id");
        ApplicationPublicKey signedWithKey = com.venky.swf.db.model.application.ApplicationPublicKey.find(ApplicationPublicKey.PURPOSE_SIGNING,pub_key_id, ApplicationPublicKey.class);

        if (signedWithKey == null){
            throw new RuntimeException("Signature Key not recorded. Please contact your registrar!");
        }

        Application application = signedWithKey.getApplication().getRawRecord().getAsProxy(Application.class);
        if (!ObjectUtil.equals(application.getAppId(),subscriber_id )){
            throw new RuntimeException("Signature key doesn't match the  subscriber");
        }
        if (!application.getCompany().getRawRecord().getAsProxy(Company.class).isKycComplete()){ // Equivalent to subscribed!
            throw new RuntimeException("Your onboarding process is not complete. Please contact your registrar!");
        }

        if (!request.verifySignature("Authorization",getPath().getHeaders(),true)){
            throw new RuntimeException("Signature Verification failed");
        }
        if (signedWithKey.isExpired()){
            throw new RuntimeException("Key has expired, Contact Registrar!");
        }
        if (!signedWithKey.isVerified()){
            signedWithKey.setTxnProperty("being.verified",true);
            signedWithKey.setVerified(true);
            signedWithKey.save();
        }


        JSONAware jsonAware = ObjectUtil.isVoid(payload) ? new JSONArray() : JSONAwareWrapper.parse(payload);
        Subscribers subscribers = new Subscribers();
        if (jsonAware instanceof JSONObject){
            subscribers.add(new Subscriber((JSONObject) jsonAware));
        }else {
            subscribers.setInner((JSONArray) jsonAware);
        }

        Subscribers outSubscribers = new Subscribers();
        for (Subscriber subscriber : subscribers){
            if (!ObjectUtil.isVoid(subscriber.getSubscriberId())){
                if (!ObjectUtil.equals(subscriber.getSubscriberId(),application.getAppId())){
                    throw new RuntimeException("Cannot sign for a different subscriber!");
                }
            }else{
                subscriber.setSubscriberId(application.getAppId());
            }
            if (!ObjectUtil.isVoid(subscriber.getPubKeyId())) {
                ensureApplicationPublicKeys(application, subscriber);
            }

            Subscriber outSubscriber = new Subscriber(subscriber.toString());
            if (!ObjectUtil.isVoid(subscriber.getSubscriberUrl())){
                EndPoint endPoint= Database.getTable(EndPoint.class).newRecord();
                endPoint.setApplicationId(application.getId());
                endPoint.setBaseUrl(subscriber.getSubscriberUrl());
                endPoint.setOpenApiId(OpenApi.find(subscriber.getType()).getId());
                endPoint = Database.getTable(EndPoint.class).getRefreshed(endPoint);
                if (endPoint.getRawRecord().isNewRecord()){
                    endPoint.save();
                }
            }
            loadRegion(subscriber,application);
            outSubscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_INITIATED);
            outSubscribers.add(outSubscriber);
        }
        if (outSubscribers.size() != 1) {
            return new BytesView(getPath(), outSubscribers.getInner().toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
        }else {
            return new BytesView(getPath(), outSubscribers.get(0).getInner().toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
        }
    }

    private void ensureApplicationPublicKeys(Application application, Subscriber subscriber) {
        ApplicationPublicKey signingKeyPassed = com.venky.swf.db.model.application.ApplicationPublicKey.find(ApplicationPublicKey.PURPOSE_SIGNING,
                subscriber.getPubKeyId(), ApplicationPublicKey.class);

        ApplicationPublicKey encKeyPassed = com.venky.swf.db.model.application.ApplicationPublicKey.find(ApplicationPublicKey.PURPOSE_ENCRYPTION,
                subscriber.getPubKeyId(), ApplicationPublicKey.class);

        boolean newSigningKeyPassed = signingKeyPassed.getRawRecord().isNewRecord() || !signingKeyPassed.getRawRecord().getAsProxy(ApplicationPublicKey.class).isVerified() ;

        if (!ObjectUtil.isVoid(subscriber.getSigningPublicKey())) {
            signingKeyPassed.setPublicKey(subscriber.getSigningPublicKey());
        }

        if (!ObjectUtil.isVoid(subscriber.getEncrPublicKey())){
            encKeyPassed.setPublicKey(subscriber.getEncrPublicKey());
        }

        if (signingKeyPassed.isDirty() && !newSigningKeyPassed ){
            throw new RuntimeException("Cannot modify a verified registered key. Please create a new key.");
        }
        signingKeyPassed.setApplicationId(application.getId());
        encKeyPassed.setApplicationId(application.getId());

        if (!ObjectUtil.isVoid(subscriber.getValidFrom())){
            signingKeyPassed.setValidFrom(new Timestamp(subscriber.getValidFrom().getTime()));
        }
        if (!ObjectUtil.isVoid(subscriber.getValidTo())){
            signingKeyPassed.setValidUntil(new Timestamp(subscriber.getValidTo().getTime()));
        }
        encKeyPassed.setValidFrom(signingKeyPassed.getValidFrom());
        encKeyPassed.setValidUntil(signingKeyPassed.getValidUntil());
        encKeyPassed.save();
        signingKeyPassed.save(); //After save triggers on_subscribe

    }




    public void loadRegion(Subscriber subscriber, Application role){
        ApplicationContext region = Database.getTable(ApplicationContext.class).newRecord();
        region.setApplicationId(role.getId());
        if (!ObjectUtil.isVoid(subscriber.getCity())){
            City city = City.findByCode(subscriber.getCity());
            if (city == null){
                throw new RuntimeException("City:" + subscriber.getCity() + " not available. Contact Registrar!");
            }
            region.setCityId(city.getId());
            region.setCountryId(region.getCity().getState().getCountryId());
        }else if (!ObjectUtil.isVoid(subscriber.getCountry())){
            region.setCountryId(Country.findByISO(subscriber.getCountry()).getId());
        }else {
            return;
        }

        region = Database.getTable(ApplicationContext.class).getRefreshed(region);
        region.save();

    }

    @RequireLogin(false)
    public View lookup() throws Exception{
        Subscriber subscriber = new Subscriber((JSONObject) Subscriber.parse(getPath().getInputStream()));
        if (ObjectUtil.isVoid(subscriber.getCity()) && subscriber.getLocation() != null && subscriber.getLocation().getCity() != null) {
            subscriber.setCity(subscriber.getLocation().getCity().getCode());
        }
        if (ObjectUtil.isVoid(subscriber.getCountry()) && subscriber.getLocation() != null && subscriber.getLocation().getCountry() != null) {
            subscriber.setCountry(subscriber.getLocation().getCountry().getCode());
        }
        subscriber.setLocation(null);
        String format = getPath().getHeaders().get("pub_key_format");
        if (!ObjectUtil.isVoid(format)){
            if (!ObjectUtil.equals("PEM",format.toUpperCase())){
                throw new RuntimeException("Only allowed value to be passed is PEM");
            }
        }
        Subscribers records = LookupManager.getInstance().lookup(subscriber, s->{
            if (s.getSigningPublicKey() == null && s.getEncrPublicKey() == null) {
                return;
            }
            if (!ObjectUtil.isVoid(format)){
                s.setSigningPublicKey(Request.getPemSigningKey(s.getSigningPublicKey()));
                s.setEncrPublicKey(Request.getPemEncryptionKey(s.getEncrPublicKey()));
            }else {
                s.setSigningPublicKey(Request.getRawSigningKey(s.getSigningPublicKey()));
                s.setEncrPublicKey(Request.getRawEncryptionKey(s.getEncrPublicKey()));
            }
        });


        return new BytesView(getPath(),records.getInner().toString().getBytes(),MimeType.APPLICATION_JSON);

    }


    @RequireLogin(false)
    public <T> View disable() throws Exception {

        String payload = StringUtil.read(getPath().getInputStream());
        Request request = new Request(payload);
        Map<String,String> params = request.extractAuthorizationParams("X-Gateway-Authorization",getPath().getHeaders());
        if (params.isEmpty()){
            throw new RuntimeException("Signature Verification failed");
        }

        String pub_key_id = params.get("pub_key_id");
        String subscriber_id = params.get("subscriber_id");
        ApplicationPublicKey signedWithKey = com.venky.swf.db.model.application.ApplicationPublicKey.find(com.venky.swf.db.model.application.ApplicationPublicKey.PURPOSE_SIGNING,pub_key_id,ApplicationPublicKey.class);
        if (!signedWithKey.isVerified()){
            throw new RuntimeException("Your signing key is not verified by the registrar! Please contact registrar or sign with a verified key.");
        }
        if (!request.verifySignature("X-Gateway-Authorization",getPath().getHeaders(),true)){
            throw new RuntimeException("Signature Verification failed");
        }
        Application application = ApplicationUtil.find(subscriber_id,Application.class);

        if (application == null){
            throw new RuntimeException("Invalid Subscriber : " + subscriber_id) ;
        }


        if (!ObjectUtil.equals(application.getId() ,signedWithKey.getApplicationId())){
            throw new RuntimeException("Key signed with is not registered against you. Please contact registrar");
        }


        Subscribers subscribers = new Subscribers(payload);
        for (Subscriber subscriber :subscribers) {
            Application disabledApplication =  ApplicationUtil.find(subscriber.getSubscriberId(),Application.class);
            if (disabledApplication == null){
                continue;
            }
            Map<String, ApplicationPublicKey> keys = LookupManager.getInstance().getLatestKeys(disabledApplication);
            ApplicationPublicKey key = keys.get(ApplicationPublicKey.PURPOSE_SIGNING).getRawRecord().getAsProxy(ApplicationPublicKey.class);
            key.setVerified(false);
            key.save();
            // when App calls signed /subscribe again, it would get verified.
        }
        return new BytesView(getPath(),subscribers.getInner().toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }
}
