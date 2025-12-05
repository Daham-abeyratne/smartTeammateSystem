package smartTeamMate.main;

import smartTeamMate.config.LoggingConfig;

public class Main {
    public static void main(String[] args) {
        LoggingConfig.setup();
        AppController app = new AppController();

        app.run();
    }
}
