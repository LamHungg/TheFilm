package vn.thefilm.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRoomRepository;
import vn.thefilm.backend.repository.RoomLayoutRepository;
import vn.thefilm.backend.repository.RoomSeatRepository;
import vn.thefilm.backend.repository.ScreeningRepository;
import vn.thefilm.backend.repository.SeatMaintenanceRepository;

/** Tim rap so huu doi tuong roi kiem tra quyen theo pham vi rap cua nguoi dung. */
@Component
public class AdminScopeGuard {

    private final CinemaScopeValidator validator;
    private final CinemaRoomRepository roomRepository;
    private final RoomLayoutRepository layoutRepository;
    private final RoomSeatRepository seatRepository;
    private final SeatMaintenanceRepository maintenanceRepository;
    private final ScreeningRepository screeningRepository;

    public AdminScopeGuard(CinemaScopeValidator validator, CinemaRoomRepository roomRepository,
            RoomLayoutRepository layoutRepository, RoomSeatRepository seatRepository,
            SeatMaintenanceRepository maintenanceRepository, ScreeningRepository screeningRepository) {
        this.validator = validator;
        this.roomRepository = roomRepository;
        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.screeningRepository = screeningRepository;
    }

    public void forCinema(Authentication auth, Long cinemaId) {
        validator.requireAccess(auth, cinemaId);
    }

    public void forRoom(Authentication auth, Long roomId) {
        validator.requireAccess(auth, roomRepository.findCinemaIdByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phong chieu " + roomId)));
    }

    public void forLayout(Authentication auth, Long layoutId) {
        validator.requireAccess(auth, layoutRepository.findCinemaIdByLayoutId(layoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay so do ghe " + layoutId)));
    }

    public void forSeat(Authentication auth, Long seatId) {
        validator.requireAccess(auth, seatRepository.findCinemaIdBySeatId(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ghe " + seatId)));
    }

    public void forMaintenance(Authentication auth, Long maintenanceId) {
        validator.requireAccess(auth, maintenanceRepository.findCinemaIdById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ban ghi bao tri " + maintenanceId)));
    }

    public void forScreening(Authentication auth, Long screeningId) {
        validator.requireAccess(auth, screeningRepository.findCinemaIdByScreeningId(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay suat chieu " + screeningId)));
    }
}
