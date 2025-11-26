package smartTeamMate.exceptions;

public class TeamFormationException extends RuntimeException {
    public TeamFormationException(String message) {
        super(message);
    }

    public TeamFormationException(String message, Throwable cause) {
        super(message, cause);
    }
}
