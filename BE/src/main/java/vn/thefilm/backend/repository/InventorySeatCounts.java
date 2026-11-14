package vn.thefilm.backend.repository;
public interface InventorySeatCounts {
    Long getScreeningId();
    long getTotal();
    long getAvailable();
    long getHeld();
    long getSold();
    long getBlocked();
}
