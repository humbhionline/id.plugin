package in.succinct.id.controller;

import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.City;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Country;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payment.Params;
import in.succinct.beckn.Request;
import in.succinct.beckn.State;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Subscribers;
import in.succinct.id.core.db.model.onboarding.company.Application;
import in.succinct.id.core.db.model.onboarding.company.Company;
import in.succinct.id.core.db.model.user.User;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;
import in.succinct.id.util.LookupManager;
import in.succinct.json.JSONAwareWrapper.JSONAwareWrapperCreator;
import in.succinct.onet.core.adaptor.NetworkAdaptorFactory;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompaniesController extends in.succinct.id.core.controller.CompaniesController {
    public CompaniesController(Path path) {
        super(path);
    }

    private void loadSubscribers(Subscribers allSubscribers , String subscriberId){
        Subscribers subscribers = LookupManager.getInstance().lookup(new Subscriber(){{
            setSubscriberId(subscriberId);
        }},null);
        for (Subscriber subscriber : subscribers){
            allSubscribers.add(subscriber);
        }
    }

    private transient final JSONAwareWrapperCreator creator = NetworkAdaptorFactory.getInstance().getAdaptor(Config.instance().getProperty("in.succinct.onet.name","beckn_open")).getObjectCreator(null);
    JSONAwareWrapperCreator getObjectCreator(){
        return creator;
    }
    @SingleRecordAction( icon = "fas fa-certificate")
    @RequireLogin(value = false)
    public View generate_beckn_json(long id){
        Company company = Database.getTable(Company.class).get(id);
        User user = company.getCreatorUser().getRawRecord().getAsProxy(User.class);

        List<Facility> facilities =  company.getFacilities();
        Facility bankingFacility = facilities.size() ==1 ? facilities.get(0) : facilities.stream().filter(f->
                    f.getFacilityCategories().stream().anyMatch(fc->fc.getMasterFacilityCategory().getName().equals("PURPOSE") && fc.getMasterFacilityCategoryValue().getAllowedValue().equals("BANKING"))
                ).findFirst().orElse(null);


        List<Application> applications = company.getApplications().stream().map(a->a.getRawRecord().getAsProxy(Application.class)).collect(Collectors.toList());

        Subscribers allSubscribers =  new Subscribers();
        for (Application application : applications){
            loadSubscribers(allSubscribers,application.getAppId());
        }
        loadSubscribers(allSubscribers,company.getSubscriberId());

        Organization organization = getOrganization(company, bankingFacility, user);

        Payment payment = getObjectCreator().create(Payment.class);
        payment.setCollectedBy(CollectedBy.BPP);
        payment.setParams(new Params(){{
            setBankAccountName(company.getName());
            setBankAccountNumber(company.getAccountNo());
            setBankCode(company.getBankCode());
            setVirtualPaymentAddress(company.getVirtualPaymentAddress());
        }});
        //Prepare beckn.json
        BecknDescriptorContent beckn = new BecknDescriptorContent();
        beckn.setSubscribers(allSubscribers);
        beckn.setOrganization(organization);
        beckn.setPayment(payment);

        Application self = ApplicationUtil.find(Config.instance().getHostName()).getRawRecord().getAsProxy(Application.class);
        Map<String, ApplicationPublicKey>  map =LookupManager.getInstance().getLatestKeys(self);
        String keyId = map.get(ApplicationPublicKey.PURPOSE_SIGNING).getKeyId();

        beckn.setRegistrarId(self.getAppId());


        BecknDescriptor descriptor = new BecknDescriptor();
        descriptor.setContent(Base64.getEncoder().encodeToString(beckn.toString().getBytes(StandardCharsets.UTF_8)));

        if (company.isKycComplete() && user.isKycComplete()) {
            //Do not sign unless kyc is complete.
            String signature = Request.generateSignature(descriptor.getContent(), CryptoKey.find(keyId, CryptoKey.PURPOSE_SIGNING).getPrivateKey());
            descriptor.setSignature(signature);
        }

        return new BytesView(getPath(),descriptor.toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON,"content-disposition", "attachment; filename=beckn.json" );
    }

    @NotNull
    private Organization getOrganization(Company company, Facility bankingFacility, User user) {
        Organization organization = getObjectCreator().create(Organization.class);
        organization.setIncomeTaxId(company.getTaxIdentificationNumber());
        organization.setDateOfIncorporation(company.getDateOfIncorporation());
        organization.setName(company.getName());
        if (bankingFacility != null) {

            organization.setCountry(new Country() {{
                setCode(bankingFacility.getCountry().getCode());
            }});
            organization.setCity(new City() {{
                setCode(bankingFacility.getCity().getCode());
            }});
            organization.setState(new State() {{
                setCode(bankingFacility.getState().getCode());
            }});
            organization.setPinCode(bankingFacility.getPinCode().getPinCode());
            organization.setAddress(organization.getAddress(bankingFacility.getAddressLine1() + "," + bankingFacility.getAddressLine2()));
        }
        organization.setContact(new Contact(){{
            setPhone(user.getPhoneNumber());
            setEmail(user.getEmail());
        }});
        return organization;
    }

    public static class BecknDescriptor extends BecknObject {
        public BecknDescriptor() {
        }

        public BecknDescriptor(String payload) {
            super(payload);
        }

        public BecknDescriptor(JSONObject object) {
            super(object);
        }
        public String getContent(){
            return get("content");
        }
        public void setContent(String content){
            set("content",content);
        }

        public String getSignature(){
            return get("signature");
        }
        public void setSignature(String signature){
            set("signature",signature);
        }

    }


    public static class BecknDescriptorContent extends BecknObject {
        public BecknDescriptorContent() {
        }

        public BecknDescriptorContent(String payload) {
            super(payload);
        }

        public BecknDescriptorContent(JSONObject object) {
            super(object);
        }

        public Organization getOrganization(){
            return get(Organization.class, "organization");
        }
        public void setOrganization(Organization organization){
            set("organization",organization);
        }

        public Subscribers getSubscribers(){
            return get(Subscribers.class, "subscribers");
        }
        public void setSubscribers(Subscribers subscribers){
            set("subscribers",subscribers);
        }

        public Payment getPayment(){
            return get(Payment.class, "payment");
        }
        public void setPayment(Payment payment){
            set("payment",payment);
        }

        public String getRegistrarId(){
            return get("registrar_id");
        }
        public void setRegistrarId(String registrar_id){
            set("registrar_id",registrar_id);
        }
    }
}
