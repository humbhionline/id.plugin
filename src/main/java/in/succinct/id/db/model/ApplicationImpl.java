package in.succinct.id.db.model;

import com.venky.swf.db.table.ModelImpl;

public class ApplicationImpl extends ModelImpl<Application> {
    public ApplicationImpl(Application a){
        super(a);
    }

    public Long getAdminId(){
        Application a = getProxy();
        if (a.getCompanyId() != null){
            return a.getCompany().getRawRecord().getAsProxy(Company.class).getAdminId();
        }else {
            return a.getCreatorUserId();
        }
    }
    public void setAdminId(Long id){

    }

}
