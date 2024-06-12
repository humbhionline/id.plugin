package in.succinct.id.controller;

import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
import com.venky.swf.views.View;

public class UsersController extends in.succinct.id.core.controller.UsersController {
    public UsersController(Path path){
        super(path);
    }

    private void setUp(){
        Config.instance().setApiKeyCase(KeyCase.SNAKE);
        Config.instance().setRootElementNameRequiredForApis(false);

    }
    private void tearDown(){
        Config.instance().setApiKeyCase(null);
        Config.instance().setRootElementNameRequiredForApis(null);
    }
    public View resource_json(){
        try {
            setUp();
            return show(getPath().getSessionUserId());
        }finally {
            tearDown();
        }
    }
}
