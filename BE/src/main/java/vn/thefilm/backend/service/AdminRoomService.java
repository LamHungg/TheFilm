package vn.thefilm.backend.service;

import java.util.List;

import vn.thefilm.backend.dto.AdminCatalogDtos.LayoutCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.LayoutDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.MaintenanceDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.MaintenanceRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomAdminDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomCreateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.RoomSeatDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.SeatUpdateRequest;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeDto;
import vn.thefilm.backend.dto.AdminCatalogDtos.TypeRequest;
import vn.thefilm.backend.enums.ActiveStatus;
import vn.thefilm.backend.enums.RoomStatus;

public interface AdminRoomService {

    List<TypeDto> listRoomTypes();

    TypeDto createRoomType(TypeRequest request, AdminActionContext ctx);

    TypeDto changeRoomTypeStatus(Long id, ActiveStatus status, AdminActionContext ctx);

    List<TypeDto> listSeatTypes();

    TypeDto createSeatType(TypeRequest request, AdminActionContext ctx);

    TypeDto changeSeatTypeStatus(Long id, ActiveStatus status, AdminActionContext ctx);

    List<RoomAdminDto> listRooms(Long cinemaId);

    RoomAdminDto createRoom(Long cinemaId, RoomCreateRequest request, AdminActionContext ctx);

    RoomAdminDto changeRoomStatus(Long roomId, RoomStatus status, AdminActionContext ctx);

    List<LayoutDto> listLayouts(Long roomId);

    LayoutDto createLayout(Long roomId, LayoutCreateRequest request, AdminActionContext ctx);

    LayoutDto activateLayout(Long layoutId, AdminActionContext ctx);

    List<RoomSeatDto> listLayoutSeats(Long layoutId);

    RoomSeatDto updateSeat(Long seatId, SeatUpdateRequest request, AdminActionContext ctx);

    List<MaintenanceDto> listMaintenance(Long layoutId);

    MaintenanceDto addMaintenance(Long seatId, MaintenanceRequest request, AdminActionContext ctx);

    MaintenanceDto resolveMaintenance(Long maintenanceId, AdminActionContext ctx);
}
