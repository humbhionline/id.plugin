package in.succinct.id.configuration;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.db.model.application.api.EndPoint;
import com.venky.swf.db.model.application.api.OpenApi;
import com.venky.swf.plugins.collab.db.model.config.Role;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.Request;
import in.succinct.id.db.model.DefaultUserRoles;
import in.succinct.id.db.model.onboarding.company.Application;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.user.User;
import in.succinct.id.util.CompanyUtil;
import in.succinct.plugins.kyc.db.model.submissions.Document;
import in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument;

import java.security.KeyPair;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AppInstaller implements Installer {

    public void install() {
        Database.getInstance().resetIdGeneration();
        installOpenApis();
        installRoles();
        installDocumentTypes();
        installEvents();
        generateBecknKeys();
        migrateSubmittedDocuments();
        generateSubscriberIds();
        fixFacilityGeographies();
    }
    private void fixFacilityGeographies(){
        Select select = new Select().from(Facility.class);
        List<Facility> facilities = select.where(new Expression(select.getPool(),"CITY_ID", Operator.EQ)).execute();
        facilities.forEach(facility -> {
            facility.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            facility.save(); //Let before validate default fill the defaults.
        });

    }
    private void generateSubscriberIds(){
        Select select = new Select().from(Company.class);
        List<Company> companies = select.where(new Expression(select.getPool(),"SUBSCRIBER_ID", Operator.EQ)).execute();
        companies.forEach(c->{
            c.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            c.save(); //Let before validate default fill the defaults.
        });

    }
    private void migrateSubmittedDocuments(){

        Select select = new Select().from(SubmittedDocument.class);
        select.where(new Expression(select.getPool(), Conjunction.AND).
                add(new Expression(select.getPool(),"USER_ID", Operator.EQ)).
                add(new Expression(select.getPool(), "COMPANY_ID",Operator.EQ)));
        List<SubmittedDocument> documentList = select.execute();
        documentList.forEach(document->{
            document.setRemarks("Migrate User / Company");
            document.save();
        });

    }

    private void installOpenApis() {
        if (Database.getTable(OpenApi.class).recordCount() == 0){
            for (String api : new String[]{"LREG","BG","BPP","BAP"}){
                OpenApi openApi = Database.getTable(OpenApi.class).newRecord();
                openApi.setName(api);
                openApi.setSpecificationLocation("/openApis/"+api.toLowerCase() + "-0.9.4.yaml");
                openApi.save();
            }
        }
    }

    private void generateBecknKeys() {
        String keyId = String.format("%s.%s",Config.instance().getHostName(),"k1");

        CryptoKey signingKey = CryptoKey.find(keyId,CryptoKey.PURPOSE_SIGNING);
        if (signingKey.getRawRecord().isNewRecord()){
            KeyPair pair = Crypt.getInstance().generateKeyPair(Request.SIGNATURE_ALGO,Request.SIGNATURE_ALGO_KEY_LENGTH);
            signingKey.setAlgorithm(Request.SIGNATURE_ALGO);
            signingKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
            signingKey.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
            signingKey.save();
        }

        CryptoKey encryptionKey = CryptoKey.find(keyId,CryptoKey.PURPOSE_ENCRYPTION);
        if (encryptionKey.getRawRecord().isNewRecord()){
            KeyPair pair = Crypt.getInstance().generateKeyPair(Request.ENCRYPTION_ALGO,Request.ENCRYPTION_ALGO_KEY_LENGTH);
            encryptionKey.setAlgorithm(Request.ENCRYPTION_ALGO);
            encryptionKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
            encryptionKey.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
            encryptionKey.save();
        }

        Company company = CompanyUtil.getCompany();
        if (company.getRawRecord().isNewRecord()){
            company.setTxnProperty("kyc.complete",true);
            company.setKycComplete(true);
            company.setDateOfIncorporation(new Date(System.currentTimeMillis()));
            company.save();
        }
        Application application = Database.getTable(Application.class).newRecord();
        application.setAppId(Config.instance().getHostName());
        application.setCompanyId(company.getId());
        application.setSigningAlgorithm("Ed25519");
        application.setHashingAlgorithm("BLAKE2B-512");
        application.setHeaders("(created) (expires) digest");
        application = Database.getTable(Application.class).getRefreshed(application);

        if (application.getRawRecord().isNewRecord()) {
            application.save();
        }

        for (CryptoKey cryptoKey : new CryptoKey[]{signingKey,encryptionKey}){
            ApplicationPublicKey applicationPublicKey = Database.getTable(ApplicationPublicKey.class).newRecord();
            applicationPublicKey.setKeyId(keyId);
            applicationPublicKey.setPurpose(cryptoKey.getPurpose());
            applicationPublicKey = Database.getTable(ApplicationPublicKey.class).getRefreshed(applicationPublicKey);
            if (applicationPublicKey.getRawRecord().isNewRecord()) {
                applicationPublicKey.setAlgorithm(cryptoKey.getAlgorithm());
                applicationPublicKey.setApplicationId(application.getId());
                applicationPublicKey.setPublicKey(cryptoKey.getPublicKey());
                applicationPublicKey.setValidFrom(new Timestamp(System.currentTimeMillis()));
                applicationPublicKey.setValidUntil(new Timestamp(applicationPublicKey.getValidFrom().getTime() + (long)(10L * 365.25D * 24L * 60L * 60L * 1000L))) ; //10 years
                applicationPublicKey.setVerified(true);
                applicationPublicKey.setTxnProperty("being.verified",true);
                applicationPublicKey.save();
            }

        }
        EndPoint endPoint = Database.getTable(EndPoint.class).newRecord();
        endPoint.setApplicationId(application.getId());
        endPoint.setBaseUrl(Config.instance().getServerBaseUrl() + "/subscribers");
        endPoint.setOpenApiId(OpenApi.find("LREG").getId());
        endPoint = Database.getTable(EndPoint.class).getRefreshed(endPoint);
        if (endPoint.getRawRecord().isNewRecord()){
            endPoint.save();
        }

    }
    public void installEvents(){
        for (String name : new String[]{"end_point_verification"}){
            Event event = Database.getTable(Event.class).newRecord();
            event.setName(name);
            event = Database.getTable(Event.class).getRefreshed(event);
            event.save();
        }
    }
    public void installRoles(){
        if (Database.getTable(Role.class).isEmpty()) {
            for (String allowedRole : DefaultUserRoles.ALLOWED_ROLES) {
                Role role = Database.getTable(Role.class).newRecord();
                role.setName(allowedRole);
                role.save();
            }
        }
        Role admin = com.venky.swf.plugins.security.db.model.Role.getRole(Role.class,"ADMIN");
        assert admin != null;
        if (!admin.isStaff()){
            admin.setStaff(true);
            admin.save();
        }
    }

    public void installDocumentTypes(){
        if (!new Select().from(Document.class).execute(1).isEmpty()){
            return;
        }
        for (String defaultDocumentType : User.DEFAULT_DOCUMENTS) {
            Document documentType = Database.getTable(Document.class).newRecord();
            documentType.setDocumentName(defaultDocumentType);
            documentType.setDocumentedModelName(User.class.getSimpleName());
            documentType = Database.getTable(Document.class).getRefreshed(documentType);
            documentType.save();
        }
        for (String defaultDocumentType : Company.DEFAULT_DOCUMENTS) {
            Document documentType = Database.getTable(Document.class).newRecord();
            documentType.setDocumentName(defaultDocumentType);
            documentType.setDocumentedModelName(Company.class.getSimpleName());
            documentType = Database.getTable(Document.class).getRefreshed(documentType);
            documentType.save();
        }


    }
}

