package in.succinct.id.util;

import com.venky.cache.UnboundedCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.date.DateUtils;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationPublicKey;
import com.venky.swf.db.model.application.api.EndPoint;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.participants.ApplicationContext;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.Location;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Subscribers;
import in.succinct.id.core.db.model.onboarding.company.Company;
import org.apache.lucene.search.Query;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LookupManager {
    private static volatile LookupManager sSoleInstance;

    //private constructor.
    private LookupManager() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static LookupManager getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (LookupManager.class) {
                if (sSoleInstance == null) sSoleInstance = new LookupManager();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected LookupManager readResolve() {
        return getInstance();
    }
    public Subscribers lookup(Subscriber criteria, KeyFormatFixer fixer){
        return lookup(criteria,(Expression) null, fixer);
    }

    public Subscribers lookup(Subscriber criteria, Expression additionalWhere, KeyFormatFixer fixer){
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
            if (!searchQry.isEmpty()) {
                searchQry.append(" AND ");
            }
            searchQry.append(" ID:\"").append(key.getApplicationId()).append("\"");
            where.add(new Expression(ref.getPool(), "ID", Operator.EQ, key.getApplicationId()));
        }


        boolean regionPassed = !ObjectUtil.isVoid(criteria.getCity()) || !ObjectUtil.isVoid(criteria.getCountry());

        Subscribers subscribers = new Subscribers();
        List<Long> applicationIds = null;
        if (!searchQry.isEmpty()) {
            LuceneIndexer indexer = LuceneIndexer.instance(Application.class);
            Query q = indexer.constructQuery(searchQry.toString());

            applicationIds = indexer.findIds(q, Select.MAX_RECORDS_ALL_RECORDS);
            where.add(Expression.createExpression(ModelReflector.instance(Application.class).getPool(), "ID", Operator.IN, applicationIds.toArray()));
        }
        where.add(new Expression(ModelReflector.instance(Application.class).getPool(),"ID", Operator.GT, 0));

        Select okSelectNetworkRole = new Select().from(Application.class).where(where);
        if (regionPassed) {
            okSelectNetworkRole.add(" and not exists ( select 1 from application_contexts where application_id = applications.id) ");
        }


        List<Application> unrestrictedApplications = okSelectNetworkRole.execute();

        for (Application application : unrestrictedApplications) {
            Subscribers tmpSubscribers = getSubscribers(criteria,application,key,fixer);
            for (Subscriber subscriber : tmpSubscribers) {
                subscribers.add(subscriber);
            }
        }

        if (regionPassed){
            List<ApplicationContext> subscribedRegions = findSubscribedRegions(criteria,applicationIds);
            if (subscribedRegions == null){
                subscribers.clear();
            }else {

                List<Application> applicationsMatchingRegions = new Select().from(Application.class).
                        where(new Expression(ModelReflector.instance(Application.class).getPool(), "ID", Operator.IN,
                                subscribedRegions.stream().map(ApplicationContext::getApplicationId).distinct().toArray())).execute();

                for (Application networkRole : applicationsMatchingRegions) {
                    Subscribers tmpSubscribers = getSubscribers(criteria, networkRole,key, fixer);
                    for (Subscriber subscriber : tmpSubscribers){
                        subscribers.add(subscriber);
                    }
                }
            }
        }

        Subscribers defaultSubscribers = getDefaultSubscribers(criteria);
        for (Subscriber subscriber : defaultSubscribers){
            subscribers.add(subscriber);
        }

        return subscribers;
    }

    /**
     *
     * This is to add subscriber that are not applications but have been added as individuals with out applications.
     * @param criteria for doing lookup.
     * @return List of subscribers matching Subscribers
     */
    private  Subscribers getDefaultSubscribers(Subscriber criteria) {
        City city = null ;
        Country country = null ;
        if (!ObjectUtil.isVoid(criteria.getCity())) {
            city = City.findByCode(criteria.getCity());
            if (city == null){
                return new Subscribers();
            }
            country = ( city.getStateId() == null) ? null : city.getState().getCountry();
        }
        if (country == null && !ObjectUtil.isVoid(criteria.getCountry())) {
            country = Country.findByISO(criteria.getCountry());
            if (country == null){
                return new Subscribers();
            }
        }


        Subscribers subscribers = new Subscribers();
        List<Company> companies ;
        if (!ObjectUtil.isVoid(criteria.getSubscriberId())) {
            Company input = Database.getTable(Company.class).newRecord();
            input.setSubscriberId(criteria.getSubscriberId());
            Company company = Database.getTable(Company.class).getRefreshed(input);
            companies = new ArrayList<>();
            if (!company.getRawRecord().isNewRecord() && company.isKycComplete() && company.getApplications().isEmpty()) { // May be slow.
                companies.add(company);
            }
        }else if (!ObjectUtil.equals(criteria.getType(),Subscriber.SUBSCRIBER_TYPE_BPP)){
            return subscribers;
        }else {
            Select select = new Select().from(Company.class);
            StringBuilder fragment = new StringBuilder();
            Expression where = new Expression(select.getPool(),Conjunction.AND);
            if (Config.instance().getBooleanProperty("beckn.require.kyc",false)){
                where.add(new Expression(select.getPool(),"KYC_COMPLETE",Operator.EQ, true));
            }else {
                fragment.append(" where 1 = 1 ");
            }
            fragment.append(" and not exists (select 1 from applications where company_id = companies.id)");

            fragment.append( " and exists (select 1 from facilities f where company_id = companies.id ");
            if (city != null){
                fragment.append( " and f.city_id = %d ".formatted(city.getId()));
            }
            if (country != null){
                fragment.append( " and f.country_id = %d ".formatted(country.getId()));
            }
            fragment.append(")");

            select.where(where).add(fragment.toString());
            companies = select.execute();
        }

        for (Company company : companies) {
            ModelReflector<Facility> ref = ModelReflector.instance(Facility.class);
            Expression where = new Expression(ref.getPool(),Conjunction.AND);
            where.add(new Expression(ref.getPool(),"COMPANY_ID",Operator.EQ,company.getId()));
            where.add(new Expression(ref.getPool(),"CITY_ID",Operator.NE));
            where.add(new Expression(ref.getPool(),"COUNTRY_ID",Operator.NE));
            if (city != null){
                where.add(new Expression(ref.getPool(),"CITY_ID",Operator.EQ,city.getId()));
            }
            if (country != null){
                where.add(new Expression(ref.getPool(),"COUNTRY_ID",Operator.EQ,country.getId()));
            }
            List<Facility> facilities = new Select().from(Facility.class).where(where).execute();
            List<String> cities = new SequenceSet<>();
            List<String> countries = new SequenceSet<>();
            List<String> gps = new SequenceSet<>();
            for (Facility facility : facilities) {
                if (city != null && ObjectUtil.equals(city.getId(),facility.getCityId())) {
                    cities.add(city.getCode());
                }else {
                    cities.add(facility.getCity().getCode());
                }
                if (country != null && ObjectUtil.equals(country.getId(),facility.getCountryId())) {
                    countries.add(country.getCode());
                }else {
                    countries.add(facility.getCountry().getCode());
                }
                gps.add("%f,%f".formatted(facility.getLat(),facility.getLng()));
            }

            subscribers.add(new Subscriber(){{
                setSubscriberId(company.getSubscriberId());
                setStatus(Subscriber.SUBSCRIBER_STATUS_SUBSCRIBED);
                setType(criteria.getType());

                setLocation(new Location(){{
                    if (cities.size() == 1) {
                        setCity(new in.succinct.beckn.City(){{
                            setCode(cities.get(0));
                        }});
                    }
                    if (!facilities.isEmpty()){
                        setCountry(new in.succinct.beckn.Country(){{
                            setCode(countries.get(0));
                        }});
                    }
                    if (gps.size() == 1){
                        set("gps",gps.get(0));
                    }
                }});
                setCity(cities.size() != 1 ? null : cities.get(0));
                setCountry(countries.size() != 1 ? null : countries.get(0));

                setCreated(company.getCreatedAt());
            }});

        }
        return subscribers;
    }


     Subscribers getSubscribers(Subscriber criteria , Application application,  ApplicationPublicKey criteriaKey ,KeyFormatFixer fixer) {
        Company company = application.getRawRecord().getAsProxy(in.succinct.id.core.db.model.onboarding.company.Application.class).getCompany().getRawRecord().getAsProxy(Company.class);
        if (Config.instance().getBooleanProperty("beckn.require.kyc",false)){
            if (!company.isKycComplete()){
                return new Subscribers();
            }
        }

        List<ApplicationPublicKey> keys ;
        if (criteriaKey != null){
            keys = new ArrayList<>();
            keys.add(criteriaKey);
        }else {
            keys = application.getApplicationPublicKeys().stream().map(pk->pk.getRawRecord().getAsProxy(ApplicationPublicKey.class)).collect(Collectors.toList());
            keys.removeIf(k -> k.isExpired()); //Unverified keys are ok to return as unsubscribed records.
            keys.sort((k1, k2) -> (int) DateUtils.compareToMillis(k2.getValidFrom(), k1.getValidFrom()));
        }

        Subscribers subscribers = getSubscribers(criteria,application, keys);
        for (Subscriber subscriber : subscribers){
            if (fixer != null) {
                fixer.fix(subscriber);
            }
        }
        return subscribers;
    }
    public Map<String,ApplicationPublicKey> getLatestKeys(Application application){
        List<ApplicationPublicKey> keys = application.getApplicationPublicKeys().stream().
                filter(applicationPublicKey -> applicationPublicKey.isVerified() && !applicationPublicKey.isExpired()).collect(Collectors.toList());
        return getLatestKeys(application, keys);
    }
    public Map<String,ApplicationPublicKey> getLatestKeys(Application application, List<ApplicationPublicKey> keys){

        Map<String, Map<String, ApplicationPublicKey>> map = new UnboundedCache<String, Map<String, ApplicationPublicKey>>() {
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
            map.get(key.getKeyId()).put(key.getPurpose(), key);
        });
        List<String> keyIds = new ArrayList<>();
        for (String keyId : map.keySet()){
            if (map.get(keyId).size() >= 2) {
                keyIds.add(keyId);
            }
        }

        if (keyIds.size() > 1) {
            keyIds.sort((kId1, kId2) -> {
                Timestamp validFrom1 = map.get(kId1).get(ApplicationPublicKey.PURPOSE_SIGNING).getValidFrom();
                Timestamp validFrom2 = map.get(kId2).get(ApplicationPublicKey.PURPOSE_SIGNING).getValidFrom();
                return (int)DateUtils.compareToMillis(validFrom2,validFrom1);
            });
        }

        return keyIds.isEmpty()? null : map.get(keyIds.get(0));
    }


    private Subscribers getSubscribers(Subscriber criteria,Application application,  List<ApplicationPublicKey> keys) {
        Subscribers  subscribers = new Subscribers();
        Map<String, ApplicationPublicKey> latestKeys = getLatestKeys(application,keys);

        List<EndPoint> endPoints = application.getEndPoints();
        if (!ObjectUtil.isVoid(criteria.getType())){
            endPoints = endPoints.stream().filter(ep->ep.getOpenApi().getName().equals(criteria.getType())).collect(Collectors.toList());
        }
        for (EndPoint endPoint: endPoints){
            Subscriber subscriber = new Subscriber();
            if (latestKeys != null) {
                ApplicationPublicKey signingPublicKey = latestKeys.get(ApplicationPublicKey.PURPOSE_SIGNING);
                ApplicationPublicKey encryptionPublicKey = latestKeys.get(ApplicationPublicKey.PURPOSE_ENCRYPTION);
                if (signingPublicKey.isVerified() && encryptionPublicKey.isVerified() && !signingPublicKey.isExpired() && !encryptionPublicKey.isExpired()){
                    subscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_SUBSCRIBED);
                }else {
                    subscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_INITIATED);
                }
                subscriber.setSigningPublicKey(signingPublicKey.getPublicKey());
                subscriber.setEncrPublicKey(encryptionPublicKey.getPublicKey());
                subscriber.setPubKeyId(signingPublicKey.getKeyId());
                subscriber.setValidFrom(signingPublicKey.getValidFrom()) ;
                subscriber.setValidTo(signingPublicKey.getValidUntil());
            }else {
                subscriber.setStatus(Subscriber.SUBSCRIBER_STATUS_INITIATED);
            }


            subscriber.setSubscriberId(application.getAppId());

            subscriber.setSubscriberUrl(endPoint.getBaseUrl());
            subscriber.setType(endPoint.getOpenApi().getName());

            subscriber.setCreated(application.getCreatedAt());
            subscriber.setUpdated(application.getUpdatedAt());
            if (criteria.getCity() != null){
                subscriber.setCity(criteria.getCity());
            }
            if (criteria.getCountry() != null){
                subscriber.setCountry(criteria.getCountry());
            }

            subscribers.add(subscriber);
        }


        return subscribers;
    }
    public  List<ApplicationContext> findSubscribedRegions(Subscriber criteria, List<Long> applicationIds){
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

}
