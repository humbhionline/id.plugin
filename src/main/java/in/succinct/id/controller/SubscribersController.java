package in.succinct.id.controller;

import com.venky.cache.Cache;
import com.venky.cache.UnboundedCache;
import com.venky.core.date.DateUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.participants.EndPoint;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Subscriber.Domains;
import in.succinct.beckn.Subscribers;
import in.succinct.id.db.model.onboarding.company.Application;
import in.succinct.id.db.model.onboarding.company.ApplicationContext;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.util.CompanyUtil;
import in.succinct.json.JSONAwareWrapper;
import org.apache.lucene.search.Query;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SubscribersController extends Controller {
    public SubscribersController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    @Override
    public View register() {
        String payload = null ;
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
            Company networkParticipant = CompanyUtil.getCompany(subscriber.getSubscriberId());

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

            for (String sDomain : subscriber.getDomains()){
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
            subscriber.setLocation(null);
        }

        String format = getPath().getHeaders().get("pub_key_format");
        if (!ObjectUtil.isVoid(format)){
            if (!ObjectUtil.equals("PEM",format.toUpperCase())){
                throw new RuntimeException("Only allowed value to be passed is PEM");
            }
        }
        Subscribers records = lookup(subscriber,0,s->{
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


    public static Subscribers lookup(Subscriber criteria, int maxRecords, KeyFormatFixer fixer) {
        return lookup(criteria, maxRecords, null,fixer);
    }


    public static List<ApplicationContext> findSubscribedRegions(Subscriber criteria, List<Long> applicationIds){
        List<ApplicationContext> regions = new ArrayList<>();
        if (ObjectUtil.isVoid(criteria.getCity()) && ObjectUtil.isVoid(criteria.getCountry())){
            return regions;
        }
        ModelReflector<ApplicationContext> ref = ModelReflector.instance(ApplicationContext.class);
        Expression where = new Expression(ref.getPool(), Conjunction.AND);
        if (!ref.isVoid(criteria.getCity())) {
            City city = City.findByCode(criteria.getCity());
            if (city != null) {
                Expression cityWhere = new Expression(ref.getPool(), Conjunction.OR);
                cityWhere.add(new Expression(ref.getPool(), "CITY_ID", Operator.EQ, city.getId()));
                cityWhere.add(new Expression(ref.getPool(), "CITY_ID", Operator.EQ));
                where.add(cityWhere);
            }else {
                return null;
            }

        }
        if (!ref.isVoid(criteria.getCountry())) {
            Country country = Country.findByISO(criteria.getCountry());
            if (country != null) {
                where.add(new Expression(ref.getPool(), "COUNTRY_ID", Operator.EQ, country.getId()));
            }else {
                return null;
            }
        }

        if (applicationIds != null) {
            where.add(new Expression(ref.getPool(), "APPLICATION_ID", Operator.IN, applicationIds.toArray()));
        }

        Select sel = new Select("MAX(ID) AS ID","APPLICATION_ID").from(ApplicationContext.class).where(where).groupBy("APPLICATION_ID");
        return sel.execute(ApplicationContext.class);
    }
    public static Subscribers lookup(Subscriber criteria, int maxRecords, Expression additionalWhere, KeyFormatFixer fixer) {
        ApplicationPublicKey key = ObjectUtil.isVoid(criteria.getPubKeyId())? null :
                com.venky.swf.db.model.application.ApplicationPublicKey.find(criteria.getPubKeyId(),
                        ApplicationPublicKey.PURPOSE_SIGNING, ApplicationPublicKey.class);
        if (key != null && key.getRawRecord().isNewRecord()){
            //invalid key being looked up.
            return new Subscribers();
        }


        ModelReflector<Application> ref = ModelReflector.instance(Application.class);

        StringBuilder searchQry = new StringBuilder();
        Expression where = new Expression(ref.getPool(), Conjunction.AND);
        if (additionalWhere != null) {
            where.add(additionalWhere);
        }

        if (!ref.isVoid(criteria.getSubscriberId())) {
            searchQry.append("APP_ID:\"").append(criteria.getSubscriberId()).append("\"");
            where.add(new Expression(ref.getPool(), "APP_ID", Operator.EQ, criteria.getSubscriberId()));
        }
        if (key != null && !key.getReflector().isVoid(key.getApplicationId())){
            if (searchQry.length() > 0) {
                searchQry.append(" AND ");
            }
            searchQry.append(" ID:\"").append(key.getApplicationId()).append("\"");
            where.add(new Expression(ref.getPool(), "ID", Operator.EQ, key.getApplicationId()));
        }


        boolean regionPassed = !ObjectUtil.isVoid(criteria.getCity()) || !ObjectUtil.isVoid(criteria.getCountry());

        Subscribers subscribers = new Subscribers();
        List<Long> networkRoleIds = null;
        if (searchQry.length() > 0) {
            LuceneIndexer indexer = LuceneIndexer.instance(Application.class);
            Query q = indexer.constructQuery(searchQry.toString());

            networkRoleIds = indexer.findIds(q, Select.MAX_RECORDS_ALL_RECORDS);
            where.add(Expression.createExpression(ModelReflector.instance(Application.class).getPool(), "ID", Operator.IN, networkRoleIds.toArray()));
        }

        Select okSelectNetworkRole = new Select().from(Application.class).where(where);
        if (regionPassed) {
            okSelectNetworkRole.add(" and not exists ( select 1 from application_contexts where application_id = applications.id) ");
        }


        List<Application> okRoles = okSelectNetworkRole.execute();

        for (Application role : okRoles) {
            Subscribers tmpSubscribers = getSubscribers(criteria,key,role,fixer);
            for (Subscriber subscriber : tmpSubscribers) {
                subscribers.add(subscriber);
            }
        }

        if (regionPassed){
            List<ApplicationContext> subscribedRegions = findSubscribedRegions(criteria,networkRoleIds);
            if (subscribedRegions == null){
                subscribers.clear();
            }else {

                List<Application> applicationsMatchingRegions = new Select().from(Application.class).
                        where(new Expression(ModelReflector.instance(Application.class).getPool(), "ID", Operator.IN,
                                subscribedRegions.stream().map(ApplicationContext::getApplicationId).distinct().toArray())).execute();

                for (Application networkRole : applicationsMatchingRegions) {
                    Subscribers tmpSubscribers = getSubscribers(criteria,key, networkRole, fixer);
                    for (Subscriber subscriber : tmpSubscribers){
                        subscribers.add(subscriber);
                    }
                }
            }
        }


        return subscribers;
    }
    public interface KeyFormatFixer {
        void fix(Subscriber subscriber);
    }
    static Subscribers getSubscribers(Subscriber criteria ,ApplicationPublicKey criteriaKey , Application networkRole,  KeyFormatFixer fixer) {
        if (Config.instance().getBooleanProperty("beckn.require.kyc",false)){
            if (!networkRole.getCompany().getRawRecord().getAsProxy(Company.class).isKycComplete()){
                return new Subscribers();
            }
        }

        ApplicationPublicKey key = null;
        List<ApplicationPublicKey> keys ;
        if (criteriaKey != null){
            keys = new ArrayList<>();
            keys.add(criteriaKey);
        }else {
            keys = networkRole.getApplicationPublicKeys().stream().map(pk->pk.getRawRecord().getAsProxy(ApplicationPublicKey.class)).collect(Collectors.toList());
            long now = System.currentTimeMillis();
            keys.removeIf(k -> k.isExpired()); //Unverified keys are ok to return as unsubscribed records.
            keys.sort((k1, k2) -> (int) DateUtils.compareToMillis(k2.getValidFrom(), k1.getValidFrom()));
        }
        if (keys.isEmpty()){
            return new Subscribers();
        }
        Subscribers subscribers = getSubscribers(criteria,networkRole, keys);
        for (Subscriber subscriber : subscribers){
            fixer.fix(subscriber);
        }
        return subscribers;
    }

    private static Subscribers getSubscribers(Subscriber criteria,Application networkRole,  List<ApplicationPublicKey> keys) {
        Subscribers  subscribers = new Subscribers();
        Map<String,Map<String,ApplicationPublicKey>> map = new UnboundedCache<String, Map<String, ApplicationPublicKey>>() {
            @Override
            protected Map<String, ApplicationPublicKey> getValue(String s) {
                return new UnboundedCache<String, ApplicationPublicKey>() {
                    @Override
                    protected ApplicationPublicKey getValue(String s) {
                        return null;
                    }
                };
            }
        };
        keys.forEach(key->{
            map.get(key.getKeyId()).put(key.getPurpose(),key);
        });

        for (String keyId : map.keySet()){
            List<com.venky.swf.db.model.application.api.EndPoint> endPoints = networkRole.getEndPoints();
            if (!ObjectUtil.isVoid(criteria.getType())){
                endPoints = endPoints.stream().filter(ep->ep.getOpenApi().getName().equals(criteria.getType())).collect(Collectors.toList());
            }
            for (com.venky.swf.db.model.application.api.EndPoint endPoint: endPoints){
                Subscriber subscriber = new Subscriber();
                ApplicationPublicKey signingPublicKey = map.get(keyId).get(ApplicationPublicKey.PURPOSE_SIGNING);
                ApplicationPublicKey encryptionPublicKey = map.get(keyId).get(ApplicationPublicKey.PURPOSE_ENCRYPTION);
                if (signingPublicKey.isVerified() && encryptionPublicKey.isVerified() && !signingPublicKey.isExpired() && !encryptionPublicKey.isExpired()){
                    subscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_SUBSCRIBED);
                }else {
                    subscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_INITIATED);
                }
                subscriber.setSubscriberId(networkRole.getAppId());

                subscriber.setSubscriberUrl(endPoint.getBaseUrl());
                subscriber.setType(endPoint.getOpenApi().getName());

                subscriber.setSigningPublicKey(signingPublicKey.getPublicKey());
                subscriber.setEncrPublicKey(encryptionPublicKey.getPublicKey());

                subscriber.setPubKeyId(signingPublicKey.getKeyId());
                subscriber.setValidFrom(signingPublicKey.getValidFrom()) ;
                subscriber.setValidTo(signingPublicKey.getValidUntil());
                subscriber.setCreated(networkRole.getCreatedAt());
                subscriber.setUpdated(networkRole.getUpdatedAt());
                if (criteria.getCity() != null){
                    subscriber.setCity(criteria.getCity());
                }
                if (criteria.getCountry() != null){
                    subscriber.setCountry(criteria.getCountry());
                }

                subscribers.add(subscriber);
            }
        }

        return subscribers;
    }
}
