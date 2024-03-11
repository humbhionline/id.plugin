package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.id.db.model.onboarding.VerifiableDocument;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.company.CompanyDocument;
import in.succinct.id.db.model.onboarding.company.SubmittedCompanyDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmittedDocumentExtension extends VerifiableDocumentExtension<SubmittedCompanyDocument> {
    static {
        registerExtension(new SubmittedDocumentExtension());
    }
    @Override
    public void afterSave(SubmittedCompanyDocument model) {
        KycInspector.submitInspection(model.getCompany());
    }
    @Override
    public void afterDestroy(SubmittedCompanyDocument model) {
        KycInspector.submitInspection(model.getCompany());
    }

    public static class KycInspector implements Task {
        Company participant ;
        public KycInspector(Company participant){
            this.participant = participant;
        }
        static void submitInspection(){
            List<Company> companies = new Select().from(Company.class).execute();
            submitInspection(companies);
        }
        private static void submitInspection(Company company){
            TaskManager.instance().executeAsync(new KycInspector(company),false); //Should be true.
        }
        private static void submitInspection(List<Company> companies) {
            for (Company company : companies) {
                submitInspection(company);
            }
        }



        @Override
        public int hashCode() {
            return (Company.class.getName() + ":" + participant.getId()).hashCode();
        }

        @Override
        public void execute() {
            Select select = new Select().from(CompanyDocument.class);
            select.where(new Expression(select.getPool(),"REQUIRED_FOR_KYC", Operator.EQ,true));
            List<CompanyDocument> documentPurposes = select.execute();
            List<SubmittedCompanyDocument> submittedDocuments = participant.getSubmittedDocuments();
            Map<Long,Boolean> kycRequirementCompletionMap = new HashMap<>();
            documentPurposes.forEach(p->{
                kycRequirementCompletionMap.put(p.getId(),false);
            });
            submittedDocuments.forEach(sd->{
                if (kycRequirementCompletionMap.containsKey(sd.getCompanyDocumentId())){
                    if (!sd.isExpired() && ObjectUtil.equals(sd.getVerificationStatus(), VerifiableDocument.APPROVED)){
                        kycRequirementCompletionMap.remove(sd.getCompanyDocumentId());
                    }
                }
            });
            if (kycRequirementCompletionMap.isEmpty()){
                participant.setTxnProperty("kyc.complete",true);
                participant.setKycComplete(true);
                participant.save();
            }else {
                participant.setKycComplete(false);
                participant.save();
            }
        }
    }
    @Override
    public void beforeValidate(SubmittedCompanyDocument document) {
        if (document.getCompanyDocument() == null){
            return;
        }
        super.beforeValidate(document);
    }
}
