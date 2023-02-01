package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;

import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.user.Email;
import com.venky.swf.plugins.security.db.model.Role;
import com.venky.swf.plugins.security.db.model.UserRole;
import in.succinct.id.db.model.Company;
import in.succinct.id.db.model.CompanyAdministrator;
import in.succinct.id.db.model.User;
import in.succinct.id.db.model.UserEmail;
import in.succinct.id.util.CompanyUtil;

import javax.swing.text.TabableView;
import java.util.UUID;

public class BeforeValidateUserEmail extends BeforeModelValidateExtension<UserEmail> {
    static {
        registerExtension(new BeforeValidateUserEmail());
    }
    @Override
    public void beforeValidate(UserEmail model) {

        if (model.getRawRecord().isFieldDirty("EMAIL")) {
            model.setValidated(false);
        }

        if (model.getRawRecord().isFieldDirty("TXT_VALUE") && !model.getReflector().isVoid(model.getTxtValue())) {
            throw new RuntimeException("TXT VALUE is generated field.");
        }
        if (model.getRawRecord().isFieldDirty("DOMAIN_VERIFIED") && model.isDomainVerified()){
            throw new RuntimeException("Domain cannot be verified manually");
        }

        if (model.getCompanyId() != null && !model.isDomainVerified()){
            if (ObjectUtil.isVoid(model.getTxtValue())){
                model.setTxtValue(UUID.randomUUID().toString());
            }else if (model.isTxtRecordVerified()) {
                model.setDomainVerified(true);
                model.setTxtValue(null);
            }
        }

        if (model.getRawRecord().isFieldDirty("COMPANY_GST_IN")){
            model.setGstInVerified(false);
        }
        if (model.getRawRecord().isFieldDirty("COMPANY_REGISTRATION_NUMBER")){
            model.setCompanyRegistrationNumberVerified(false);
        }

        if (model.isGstInVerified() && model.getRawRecord().isFieldDirty("GST_IN_VERIFIED") &&
                !model.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(model.getTxnProperty(UserEmail.class.getSimpleName() + ".GstInBeingVerified"))){
            throw new AccessDeniedException("Cannot verify GSTIn manually!");
        }
        if (model.isCompanyRegistrationNumberVerified() && model.getRawRecord().isFieldDirty("COMPANY_REGISTRATION_NUMBER_BEING_VERIFIED") &&
                !model.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(model.getTxnProperty(UserEmail.class.getSimpleName() + ".CompanyRegistrationNumberBeingVerified"))){
            throw new AccessDeniedException("Cannot verify company registration manually!");
        }

        if (!ObjectUtil.isVoid(model.getEmail()) && model.isValidated()) {
            Company company = ensureCompany(model);
            model.setCompanyId(company.getId());
            if (model.isCompanyAdmin()) {
                makeAdmin(model);
            }
            User user = model.getUser().getRawRecord().getAsProxy(User.class);
            if (user.getCompanyId() == null){
                TaskManager.instance().executeAsync(new CompanySetter(model.getUserId(),model.getCompanyId()),false);
            }
        }else {
            model.setCompanyId(null);
        }

    }
    public static class CompanySetter implements Task{
        long userId;
        long companyId;
        public CompanySetter(long userId,long companyId){
            this.userId = userId;
            this.companyId = companyId;
        }

        @Override
        public void execute() {
            User user = Database.getTable(User.class).get(userId);
            Company company = Database.getTable(Company.class).get(companyId);
            if (company != null && user != null) {
                user.setCompanyId(company.getId());
                user.save();
            }
        }
    }

    public void makeAdmin(UserEmail userEmail){
        CompanyAdministrator administrator = Database.getTable(CompanyAdministrator.class).newRecord();
        administrator.setCompanyId(userEmail.getCompanyId());
        administrator.setUserId(userEmail.getUserId());
        administrator = Database.getTable(CompanyAdministrator.class).getRefreshed(administrator);
        administrator.save();

        UserRole ur = Database.getTable(UserRole.class).newRecord();
        ur.setUserId(administrator.getUserId());
        ur.setRoleId(Role.getRole(com.venky.swf.plugins.collab.db.model.config.Role.class,"ADMIN").getId());
        ur  = Database.getTable(UserRole.class).getRefreshed(ur);
        ur.save();
    }

    public Company ensureCompany(UserEmail model){
        String email = model.getEmail();
        Email.validate(email);
        String fqdn = CompanyUtil.getFQDomainName(email.substring(email.indexOf('@')+1));
        Company company = Database.getTable(Company.class).newRecord();
        company.setDomainName(fqdn);
        company = Database.getTable(Company.class).getRefreshed(company);
        if (company.getRawRecord().isNewRecord()){
            company.setName(fqdn);
        }
        if (model.isCompanyRegistrationNumberVerified() &&
                model.getRawRecord().isFieldDirty("COMPANY_REGISTRATION_NUMBER_VERIFIED") &&
                !ObjectUtil.isVoid(model.getCompanyRegistrationNumber())){
            company.setCompanyRegistrationNumber(model.getCompanyRegistrationNumber());
        }
        if (model.isGstInVerified() &&
                model.getRawRecord().isFieldDirty("GST_IN_VERIFIED") &&
                !ObjectUtil.isVoid(model.getCompanyGstIn())){
            company.setCompanyGstIn(model.getCompanyGstIn());
        }
        company.setTxnProperty("kyc.being.verified",true);
        company.save();
        return company;
    }
}
