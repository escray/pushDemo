package geektime.tdd.args.Exceptions;

public class UnsupportedOptionTypeException extends RuntimeException {
    private final String option;
    private final Class<?> type;

    public UnsupportedOptionTypeException(String value, Class<?> type) {
        this.option = value;
        this.type = type;
    }
}
