package in.succinct.id.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.KeyCase;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.View;

public class OpenViewModelController<M extends Model> extends ModelController<M> {
    private void setUp(){
        Config.instance().setApiKeyCase(KeyCase.SNAKE);
        Config.instance().setRootElementNameRequiredForApis(false);

    }
    private void tearDown(){
        Config.instance().setApiKeyCase(null);
        Config.instance().setRootElementNameRequiredForApis(null);
    }

    public OpenViewModelController(Path path) {
        super(path);
    }

    @Override
    @RequireLogin(false)
    public View search() {
        try {
            setUp();
            return super.search();
        }finally {
            tearDown();
        }
    }

    @Override
    @RequireLogin(false)
    public View search(String strQuery) {
        try {
            setUp();
            return super.search(strQuery);
        }finally {
            tearDown();
        }
    }

    @Override
    @RequireLogin(false)
    public View index(){
        try {
            setUp();
            return super.index();
        }finally {
            tearDown();
        }
    }
}
