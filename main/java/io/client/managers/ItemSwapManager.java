package io.client.managers;

public class ItemSwapManager {
    public static final ItemSwapManager INSTANCE = new ItemSwapManager();

    private String owner;
    private SwapPriority priority = SwapPriority.LOW;
    private long leaseUntilMs;

    private ItemSwapManager() {
    }

    public synchronized boolean acquire(String requester, SwapPriority requestedPriority, long leaseMs) {
        long now = System.currentTimeMillis();
        boolean leaseExpired = now > leaseUntilMs;
        boolean sameOwner = requester.equals(owner);
        boolean canPreempt = owner == null || leaseExpired || requestedPriority.weight() >= priority.weight();

        if (sameOwner || canPreempt) {
            owner = requester;
            priority = requestedPriority;
            leaseUntilMs = now + Math.max(1L, leaseMs);
            return true;
        }
        return false;
    }

    public synchronized void release(String requester) {
        if (requester.equals(owner)) {
            owner = null;
            priority = SwapPriority.LOW;
            leaseUntilMs = 0L;
        }
    }
}
