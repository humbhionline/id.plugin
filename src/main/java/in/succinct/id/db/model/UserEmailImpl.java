package in.succinct.id.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.table.ModelImpl;

import java.sql.Timestamp;
import java.util.Hashtable;

public class UserEmailImpl extends ModelImpl<UserEmail> {
    public UserEmailImpl(UserEmail email) {
        super(email);
    }
    public boolean isCompanyAdmin() {
        UserEmail ue = getProxy();
        User user = ue.getUser().getRawRecord().getAsProxy(User.class);
        Company company = ue.getCompany();
        if (company == null){
            return false;
        }
        return company.getCompanyAdministrators().stream().anyMatch(a->ObjectUtil.equals(a.getUserId(),user.getId())) ||
                (ue.isValidated() && ue.isDomainVerified() && user.isAddressVerified());
    }

    public String getTxtName() {
        return "humbhionline-token";
    }

    /**
     * Check if domain has the txt record.
     */
    public void requestDomainVerification(){
        UserEmail userEmail = getProxy();
        userEmail.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        userEmail.save();
    }

    public boolean  isTxtRecordVerified() {
        UserEmail userEmail = getProxy();
        if (userEmail.isDomainVerified()){
            return true;
        }
        if (userEmail.getCompanyId() == null){
            return false;
        }

        String domain = userEmail.getCompany().getDomainName();
        if (ObjectUtil.isVoid(domain)){
            return false;
        }

        String hostName = userEmail.getTxtName() ;
        String txtValue = userEmail.getTxtValue();

        if (ObjectUtil.isVoid(txtValue) ) {
            return false;
        }
        hostName = String.format("%s.%s",hostName,domain);

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            javax.naming.   directory.DirContext dirContext
                    = new javax.naming.directory.InitialDirContext(env);
            javax.naming.directory.Attributes attrs
                    = dirContext.getAttributes(hostName, new String[]{"TXT"});
            javax.naming.directory.Attribute attr
                    = attrs.get("TXT");

            String txtRecord = "";

            if (attr != null) {
                txtRecord = attr.get().toString();
            }

            return (ObjectUtil.equals(txtRecord, userEmail.getTxtValue())) ;
        } catch (javax.naming.NamingException e) {
            return  false;
        }
    }

    @IS_VIRTUAL
    public String getCompanyGstVerificationUrl(){
        UserEmail ue = getProxy();
        if (!ObjectUtil.isVoid(ue.getCompanyGstIn())){
            return String.format("https://services.gst.gov.in/services/searchtp?GstInNo=%s&PanNum=%s",
                    ue.getCompanyGstIn(),
                    ue.getCompanyGstIn().substring(2,ue.getCompanyGstIn().length()-4));
        }
        return null;
    }
    @IS_VIRTUAL
    public String getCompanyVerificationUrl(){
        return "https://www.mca.gov.in/mcafoportal/viewCompanyMasterData.do";
    }
}
