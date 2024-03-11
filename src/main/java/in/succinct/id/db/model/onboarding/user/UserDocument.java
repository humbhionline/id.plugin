package in.succinct.id.db.model.onboarding.user;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@MENU("Documents")
public interface UserDocument extends Model {
    public static final String AADHAR = "Aadhar";
    public static final String PAN = "Pan Card";
    public static final String GST = "Gst Certificate";

    public static final String[] DEFAULT_DOCUMENT_TYPES = new String[]{AADHAR,PAN,GST};


    @UNIQUE_KEY
    public String getName();
    public void setName(String name);
}
