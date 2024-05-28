package in.succinct.id.db.model.onboarding.docs;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.table.ModelImpl;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.user.User;

public class SubmittedDocumentImpl extends ModelImpl<SubmittedDocument> {

    public SubmittedDocumentImpl(SubmittedDocument p) {
        super(p);
    }


    public Long getCompanyId() {
        if (ObjectUtil.equals(Company.class.getSimpleName(),getProxy().getDocumentedModelName())) {
            return getProxy().getDocumentedModelId();
        }
        return null;
    }
    public void setCompanyId(Long id){
        if (getReflector().isVoid(id)){
            return;
        }
        getProxy().setDocumentedModelId(id);
        getProxy().setDocumentedModelName(Company.class.getSimpleName());
    }
    public Company getCompany() {
        if (ObjectUtil.equals(Company.class.getSimpleName(),getProxy().getDocumentedModelName())) {
            return getProxy().extractDocumentedModel();
        }else {
            return null;
        }
    }


    public Long getUserId() {
        if (ObjectUtil.equals(User.class.getSimpleName(),getProxy().getDocumentedModelName())) {
            return getProxy().getDocumentedModelId();
        }
        return null;
    }
    public void setUserId(Long id){
        if (getReflector().isVoid(id)){
            return;
        }
        getProxy().setDocumentedModelId(id);
        getProxy().setDocumentedModelName(User.class.getSimpleName());
    }
    public User getUser() {
        if (ObjectUtil.equals(User.class.getSimpleName(),getProxy().getDocumentedModelName())) {
            return getProxy().extractDocumentedModel();
        }else {
            return null;
        }
    }


}
