package vn.thefilm.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.CinemaRoomRepository;
import vn.thefilm.backend.repository.CinemaRepository;
import vn.thefilm.backend.repository.RoomTypeRepository;
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
    private final CinemaRepository cinemaRepository;
    private final RoomTypeRepository roomTypeRepository;

    public AdminScopeGuard(CinemaScopeValidator validator, CinemaRoomRepository roomRepository,
            RoomLayoutRepository layoutRepository, RoomSeatRepository seatRepository,
            SeatMaintenanceRepository maintenanceRepository, ScreeningRepository screeningRepository,
            CinemaRepository cinemaRepository, RoomTypeRepository roomTypeRepository) {
        this.validator = validator;
        this.roomRepository = roomRepository;
        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.screeningRepository = screeningRepository;
        this.cinemaRepository = cinemaRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public void forCinema(Authentication auth, Long cinemaId) {
        validator.requireAccess(auth, cinemaId);
    }

    public void forRoom(Authentication auth, Long roomId) {
        validator.requireAccess(auth, roomRepository.findCinemaIdByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phong chieu " + roomId)));
    }

    public void forRoomType(Authentication auth, Long roomTypeId) {
        var type = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai phong " + roomTypeId));
        if (auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CHAIN_ADMIN".equals(authority.getAuthority()))) {
            return;
        }
        for (Long cinemaId : cinemaRepository.findCinemaIdsByChainId(type.getChainId())) {
            try {
                validator.requireAccess(auth, cinemaId);
                return;
            } catch (AccessDeniedException ignored) {
                // A manager scoped to any cinema in the chain can manage its shared catalog.
            }
        }
        throw new AccessDeniedException("The account has no active access to this room type's chain.");
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
