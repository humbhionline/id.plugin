package in.succinct.id.db.model;

import com.venky.swf.db.table.ModelImpl;

public class GrantImpl extends ModelImpl<Grant> {
    public GrantImpl(Grant grant){
        super(grant);
    }
    public String getTokenType(){
        return "Bearer";
    }
}
