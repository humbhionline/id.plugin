package in.succinct.id.db.model;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.MENU;

public interface Country extends com.venky.swf.plugins.collab.db.model.config.Country {
    @COLUMN_NAME("ISO_CODE")
    @IS_NULLABLE
    @UNIQUE_KEY("CODE")
    public String getCode();
    public void setCode(String code);

}
