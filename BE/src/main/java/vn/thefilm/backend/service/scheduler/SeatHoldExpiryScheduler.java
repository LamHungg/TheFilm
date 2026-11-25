package vn.thefilm.backend.service.scheduler;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.entity.SeatHoldEntity;
import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.enums.ShowSeatStatus;
import vn.thefilm.backend.repository.SeatHoldRepository;
import vn.thefilm.backend.enums.SeatHoldStatus;
import vn.thefilm.backend.repository.ShowSeatRepository;
import vn.thefilm.backend.service.impl.CounterFnbInventoryService;

@Component
public class SeatHoldExpiryScheduler {

    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final CounterFnbInventoryService counterFnbInventory;

    public SeatHoldExpiryScheduler(ShowSeatRepository showSeatRepository, SeatHoldRepository seatHoldRepository) {
        this(showSeatRepository, seatHoldRepository, null);
    }

    @Autowired
    public SeatHoldExpiryScheduler(ShowSeatRepository showSeatRepository, SeatHoldRepository seatHoldRepository,
            CounterFnbInventoryService counterFnbInventory) {
        this.showSeatRepository = showSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.counterFnbInventory = counterFnbInventory;
    }

    @Scheduled(fixedDelayString = "${thefilm.booking.hold-expiry-scan-ms:15000}")
    @Transactional
    public int releaseExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        var expiredSeats = showSeatRepository.findExpiredHeldSeatsForUpdate(ShowSeatStatus.HELD, now);
        for (ShowSeatEntity seat : expiredSeats) {
            seat.release();
            seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatus(
                            seat.getShowSeatId(), SeatHoldStatus.HELD)
                    .forEach(hold -> hold.release(now, SeatHoldStatus.EXPIRED));
        }
        if (counterFnbInventory != null) counterFnbInventory.releaseExpiredReservations();
        return expiredSeats.size();
    }
}
