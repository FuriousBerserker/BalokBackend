package backend;

public class Status {

    // guarantee exclusive write to these two field
    private volatile int submittedAccess;

    private volatile int tackledAccess;

    public void submit() {
        submittedAccess++;
    }

    public void tackle() {
        tackledAccess++;
    }

    public int untackled() {
        return submittedAccess - tackledAccess;
    }
}
