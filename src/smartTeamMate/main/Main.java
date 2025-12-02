package smartTeamMate.main;

import smartTeamMate.config.LoggingConfig;

public class Main {
    public static void main(String[] args) {
        AppController app = new AppController();
        LoggingConfig.setup();

        app.run();
    }
}
