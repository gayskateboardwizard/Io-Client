package io.client.managers;

public enum SwapPriority {
    LOW(0),
    NORMAL(1),
    HIGH(2),
    CRITICAL(3);

    private final int weight;

    SwapPriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
