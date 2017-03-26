package julioarellano.wiiselandroid7.utils;

public enum FallLogicStage {

    ZERO(""), //
    ONE(""), //
    TWO(""), //
    THREE(""); //

    String value;

    private FallLogicStage(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
