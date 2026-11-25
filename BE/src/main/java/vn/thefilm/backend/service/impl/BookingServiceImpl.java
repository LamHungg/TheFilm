package vn.thefilm.backend.service.impl;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.dto.HoldSeatsRequest;
import vn.thefilm.backend.dto.HoldSeatsResponse;
import vn.thefilm.backend.dto.SeatItemDto;
import vn.thefilm.backend.dto.SeatLayoutResponse;
import vn.thefilm.backend.dto.CreateOrderRequest;
import vn.thefilm.backend.dto.OrderResponse;
import vn.thefilm.backend.dto.PaymentRequest;
import vn.thefilm.backend.dto.PaymentResponse;
import vn.thefilm.backend.dto.TicketResponse;
import vn.thefilm.backend.entity.PaymentTxnEntity;
import vn.thefilm.backend.entity.SalesOrderLineEntity;
import vn.thefilm.backend.entity.SeatHoldEntity;
import vn.thefilm.backend.entity.TicketEntity;
import vn.thefilm.backend.entity.SalesOrderEntity;
import vn.thefilm.backend.entity.ShowSeatEntity;
import vn.thefilm.backend.entity.ScreeningEntity;
import vn.thefilm.backend.enums.ShowSeatStatus;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.exception.SeatUnavailableException;
import vn.thefilm.backend.repository.PriceRuleRepository;
import vn.thefilm.backend.repository.PaymentTxnRepository;
import vn.thefilm.backend.repository.TicketRepository;
import vn.thefilm.backend.enums.OrderStatus;
import vn.thefilm.backend.enums.OrderLineType;
import vn.thefilm.backend.enums.SalesChannel;
import vn.thefilm.backend.enums.SeatHoldStatus;
import vn.thefilm.backend.repository.SalesOrderRepository;
import vn.thefilm.backend.repository.SeatBasePriceProjection;
import vn.thefilm.backend.repository.SeatHoldRepository;
import vn.thefilm.backend.repository.ShowSeatRepository;
import vn.thefilm.backend.service.BookingService;
import vn.thefilm.backend.service.PromoVoucherService;

@Service
public class BookingServiceImpl implements BookingService {

    private static final long HOLD_SECONDS = 600;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final PaymentTxnRepository paymentTxnRepository;
    private final TicketRepository ticketRepository;
    private final CounterFnbInventoryService counterFnbInventory;
    private final PromoVoucherService promoVouchers;

    public BookingServiceImpl(
            ShowSeatRepository showSeatRepository,
            SeatHoldRepository seatHoldRepository,
            SalesOrderRepository salesOrderRepository,
            PriceRuleRepository priceRuleRepository) {
        this(showSeatRepository, seatHoldRepository, salesOrderRepository, priceRuleRepository, null, null, null, null);
    }

    public BookingServiceImpl(ShowSeatRepository showSeatRepository, SeatHoldRepository seatHoldRepository,
            SalesOrderRepository salesOrderRepository, PriceRuleRepository priceRuleRepository,
            PaymentTxnRepository paymentTxnRepository, TicketRepository ticketRepository) {
        this(showSeatRepository, seatHoldRepository, salesOrderRepository, priceRuleRepository,
                paymentTxnRepository, ticketRepository, null, null);
    }

    public BookingServiceImpl(ShowSeatRepository showSeatRepository, SeatHoldRepository seatHoldRepository,
            SalesOrderRepository salesOrderRepository, PriceRuleRepository priceRuleRepository,
            PaymentTxnRepository paymentTxnRepository, TicketRepository ticketRepository,
            CounterFnbInventoryService counterFnbInventory) {
        this(showSeatRepository, seatHoldRepository, salesOrderRepository, priceRuleRepository,
                paymentTxnRepository, ticketRepository, counterFnbInventory, null);
    }

    @Autowired
    public BookingServiceImpl(ShowSeatRepository showSeatRepository, SeatHoldRepository seatHoldRepository,
            SalesOrderRepository salesOrderRepository, PriceRuleRepository priceRuleRepository,
            PaymentTxnRepository paymentTxnRepository, TicketRepository ticketRepository,
            CounterFnbInventoryService counterFnbInventory, PromoVoucherService promoVouchers) {
        this.showSeatRepository = showSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.paymentTxnRepository = paymentTxnRepository;
        this.ticketRepository = ticketRepository;
        this.counterFnbInventory = counterFnbInventory;
        this.promoVouchers = promoVouchers;
    }

    @Override
    @Transactional(readOnly = true)
    public SeatLayoutResponse getScreeningSeats(Long screeningId) {
        OffsetDateTime now = OffsetDateTime.now();
        Map<Long, java.math.BigDecimal> prices = new LinkedHashMap<>();
        for (SeatBasePriceProjection price : priceRuleRepository.findBasePricesForScreening(screeningId, now)) {
            prices.put(price.getShowSeatId(), price.getBasePrice());
        }
        List<SeatItemDto> seats = showSeatRepository
                .findByScreening_ScreeningIdOrderByRowLabelSnapshotAscSeatCodeSnapshotAsc(screeningId)
                .stream()
                .map(seat -> toSeatItem(seat, now, prices.get(seat.getShowSeatId())))
                .toList();
        return new SeatLayoutResponse(screeningId, seats);
    }

    @Override
    @Transactional
    public HoldSeatsResponse holdSeats(Long screeningId, HoldSeatsRequest request) {
        return holdSeats(screeningId, request, SalesChannel.ONLINE, null);
    }

    @Override
    @Transactional
    public HoldSeatsResponse holdCounterSeats(Long screeningId, HoldSeatsRequest request, Long createdBy) {
        return holdSeats(screeningId, request, SalesChannel.BOX, createdBy);
    }

    private HoldSeatsResponse holdSeats(Long screeningId, HoldSeatsRequest request, SalesChannel salesChannel,
            Long createdBy) {
        if (request == null || request.showSeatIds() == null || request.showSeatIds().isEmpty()) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất một ghế.");
        }
        List<Long> requestedIds = request.showSeatIds();
        if (new LinkedHashSet<>(requestedIds).size() != requestedIds.size() || requestedIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("Danh sách ghế không hợp lệ hoặc bị trùng.");
        }
        Long salesOrderId = request.salesOrderId();
        SalesOrderEntity order = salesOrderId == null || salesOrderId <= 0L ? null
                : salesOrderRepository.findById(salesOrderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        if (order != null && !validHoldToken(order, request.holdToken())) {
            throw new SeatUnavailableException("Phiên giữ ghế không hợp lệ.");
        }
        if (order != null && order.getSalesChannel() != salesChannel) {
            throw new SeatUnavailableException("Kênh bán không khớp với phiên giữ ghế.");
        }
        if (order != null && order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new SeatUnavailableException("Không thể giữ ghế cho đơn hàng ở trạng thái hiện tại.");
        }
        List<ShowSeatEntity> seats = showSeatRepository.findByScreeningIdAndShowSeatIdsForUpdate(screeningId, requestedIds);
        if (seats.size() != requestedIds.size()) {
            throw new SeatUnavailableException("Một hoặc nhiều ghế không thuộc suất chiếu này.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (ShowSeatEntity seat : seats) {
            if (seat.getSeatStatus() == ShowSeatStatus.HELD && isHoldExpired(seat, now)) {
                seat.release();
                seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatus(
                                seat.getShowSeatId(), SeatHoldStatus.HELD)
                        .forEach(hold -> hold.release(now, SeatHoldStatus.EXPIRED));
            } else if (seat.getSeatStatus() != ShowSeatStatus.AVAILABLE) {
                throw new SeatUnavailableException("Một hoặc nhiều ghế đã được giữ hoặc bán.");
            }
        }

        OffsetDateTime expiresAt = now.plusSeconds(HOLD_SECONDS);
        String holdToken = request.holdToken();
        if (order == null) {
            ScreeningEntity screening = seats.get(0).getScreening();
            Long cinemaId = screening.getCinema().getCinemaId();
            String draftNumber = "HOLD" + now.toLocalDate().toString().replace("-", "")
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            holdToken = UUID.randomUUID().toString();
            order = SalesOrderEntity.createHoldDraft(draftNumber, holdToken, cinemaId, cinemaId, expiresAt,
                    salesChannel, createdBy);
            order = salesOrderRepository.save(order);
        }
        for (ShowSeatEntity seat : seats) {
            seat.hold(order, expiresAt);
            seatHoldRepository.save(new SeatHoldEntity(seat, order, expiresAt));
        }
        return new HoldSeatsResponse(order.getSalesOrderId(), holdToken, expiresAt, List.copyOf(requestedIds));
    }

    @Override
    @Transactional
    public void releaseSeats(Long salesOrderId, String holdToken, List<Long> showSeatIds) {
        if (salesOrderId == null || showSeatIds == null || showSeatIds.isEmpty()) {
            throw new IllegalArgumentException("Cần cung cấp đơn hàng và ít nhất một ghế.");
        }
        SalesOrderEntity owner = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên giữ ghế."));
        if (!validHoldToken(owner, holdToken)) throw new SeatUnavailableException("Phiên giữ ghế không hợp lệ.");
        OffsetDateTime now = OffsetDateTime.now();
        List<ShowSeatEntity> seats = showSeatRepository.findByShowSeatIdsForUpdate(showSeatIds);
        if (new LinkedHashSet<>(showSeatIds).size() != showSeatIds.size() || seats.size() != showSeatIds.size()
                || seats.stream().anyMatch(seat -> seat.getSeatStatus() != ShowSeatStatus.HELD
                || seat.getCurrentOrder() == null || !salesOrderId.equals(seat.getCurrentOrder().getSalesOrderId())
                || isHoldExpired(seat, now))) {
            throw new SeatUnavailableException("Ghế không còn được giữ bởi phiên này.");
        }
        for (ShowSeatEntity seat : seats) {
            if (seat.getSeatStatus() == ShowSeatStatus.HELD && seat.getCurrentOrder() != null
                    && salesOrderId.equals(seat.getCurrentOrder().getSalesOrderId())) {
                seat.release();
                seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatus(seat.getShowSeatId(), SeatHoldStatus.HELD)
                        .forEach(hold -> hold.release(now, SeatHoldStatus.RELEASED));
            }
        }
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        return createOrder(request, SalesChannel.ONLINE, null, null);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long appUserId) {
        return createOrder(request, SalesChannel.ONLINE, null, appUserId);
    }

    @Override
    @Transactional
    public OrderResponse createCounterOrder(CreateOrderRequest request, Long createdBy) {
        return createOrder(request, SalesChannel.BOX, createdBy, null);
    }

    private OrderResponse createOrder(CreateOrderRequest request, SalesChannel salesChannel, Long createdBy,
            Long appUserId) {
        if (request == null || request.screeningId() == null || request.salesOrderId() == null || request.showSeatIds() == null
                || request.showSeatIds().isEmpty() || request.idempotencyKey() == null
                || request.idempotencyKey().isBlank()) throw new IllegalArgumentException("Thông tin đặt vé không hợp lệ.");
        var existing = salesOrderRepository.findBySalesChannelAndIdempotencyKey(
                salesChannel, request.idempotencyKey());
        SalesOrderEntity holdOrder = salesOrderRepository.findById(request.salesOrderId())
                .orElseThrow(() -> new SeatUnavailableException("Phiên giữ ghế không còn tồn tại."));
        if (!validHoldToken(holdOrder, request.holdToken()) || holdOrder.getOrderStatus() != OrderStatus.DRAFT)
            throw new SeatUnavailableException("Phiên giữ ghế không hợp lệ.");
        if (holdOrder.getSalesChannel() != salesChannel)
            throw new SeatUnavailableException("Kênh bán không khớp với phiên giữ ghế.");
        if (existing.isPresent()) return toOrderResponse(existing.get());
        if (new LinkedHashSet<>(request.showSeatIds()).size() != request.showSeatIds().size())
            throw new IllegalArgumentException("Danh sách ghế bị trùng.");

        OffsetDateTime now = OffsetDateTime.now();
        List<ShowSeatEntity> seats = showSeatRepository.findByScreeningIdAndShowSeatIdsForUpdate(request.screeningId(), request.showSeatIds());
        if (seats.size() != request.showSeatIds().size()) throw new SeatUnavailableException("Ghế không thuộc suất chiếu.");
        for (ShowSeatEntity seat : seats) {
            if (seat.getSeatStatus() != ShowSeatStatus.HELD || seat.getCurrentOrder() == null
                    || !request.salesOrderId().equals(seat.getCurrentOrder().getSalesOrderId())
                    || seat.getHoldExpiresAt() == null || !seat.getHoldExpiresAt().isAfter(now))
                throw new SeatUnavailableException("Ghế không còn được giữ hợp lệ.");
            if (seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatusAndExpiresAtAfter(
                    seat.getShowSeatId(), SeatHoldStatus.HELD, now).isEmpty())
                throw new SeatUnavailableException("Phiên giữ ghế đã hết hạn.");
        }
        var screening = seats.get(0).getScreening();
        Map<Long, BigDecimal> prices = new LinkedHashMap<>();
        priceRuleRepository.findBasePricesForScreening(request.screeningId(), now)
                .forEach(p -> prices.put(p.getShowSeatId(), p.getBasePrice()));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ShowSeatEntity seat : seats) {
            BigDecimal price = prices.getOrDefault(seat.getShowSeatId(), BigDecimal.ZERO);
            if (price.signum() <= 0) throw new IllegalArgumentException("Không tìm thấy giá vé hợp lệ.");
            subtotal = subtotal.add(price);
        }
        List<CreateOrderRequest.FnbItem> fnbItems = request.fnbItems() == null ? List.of() : request.fnbItems();
        for (CreateOrderRequest.FnbItem item : fnbItems) {
            if (item == null || item.productId() == null || item.productName() == null || item.productName().isBlank()
                    || item.quantity() == null || item.quantity() <= 0 || item.unitPrice() == null || item.unitPrice().signum() < 0)
                throw new IllegalArgumentException("Thông tin F&B không hợp lệ.");
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        BigDecimal discount = BigDecimal.ZERO;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            if (promoVouchers == null) throw new IllegalStateException("Dịch vụ voucher chưa được cấu hình.");
            discount = promoVouchers.preview(request.voucherCode(), subtotal, appUserId).discountAmount();
        }
        List<BigDecimal> lineAmounts = new java.util.ArrayList<>();
        seats.forEach(seat -> lineAmounts.add(prices.get(seat.getShowSeatId())));
        fnbItems.forEach(item -> lineAmounts.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))));
        BigDecimal vat = calculateVat(lineAmounts, subtotal, discount);
        OffsetDateTime expires = now.plusMinutes(10);
        String code = "TF" + now.toLocalDate().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long cinemaId = screening.getCinema().getCinemaId();
        SalesOrderEntity order = new SalesOrderEntity(code, request.idempotencyKey(), cinemaId,
                cinemaId, request.guestName(), request.guestEmail(), request.guestPhone(), subtotal,
                discount, vat, subtotal.subtract(discount).add(vat), expires, salesChannel, createdBy);
        BigDecimal remainingDiscount = discount;
        BigDecimal remainingBase = subtotal;
        int line = 1;
        for (ShowSeatEntity seat : seats) {
            BigDecimal price = prices.get(seat.getShowSeatId());
            BigDecimal lineDiscount = allocateDiscount(price, remainingBase, remainingDiscount);
            remainingDiscount = remainingDiscount.subtract(lineDiscount);
            remainingBase = remainingBase.subtract(price);
            BigDecimal lineVat = price.subtract(lineDiscount).multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
            order.addLine(new SalesOrderLineEntity(order, line++, screening, seat,
                    screening.getFilm().getTitleLocal(), screening.getScreeningTypeCode(),
                    screening.getRoomTypeNameSnapshot(), seat.getSeatTypeCodeSnapshot(), price,
                    lineDiscount, new BigDecimal("0.0800"), lineVat, price.subtract(lineDiscount).add(lineVat)));
        }
        for (CreateOrderRequest.FnbItem item : fnbItems) {
            BigDecimal amount = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            BigDecimal lineDiscount = allocateDiscount(amount, remainingBase, remainingDiscount);
            remainingDiscount = remainingDiscount.subtract(lineDiscount);
            remainingBase = remainingBase.subtract(amount);
            BigDecimal lineVat = amount.subtract(lineDiscount).multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
            order.addLine(new SalesOrderLineEntity(order, line++, item.productId(), item.productName(),
                    item.productCode(), item.quantity(), item.unitPrice(), lineDiscount, new BigDecimal("0.0800"),
                    lineVat, amount.subtract(lineDiscount).add(lineVat)));
        }
        salesOrderRepository.saveAndFlush(order);
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            PromoVoucherService.Application applied = promoVouchers.reserve(request.voucherCode(), subtotal,
                    appUserId, order.getSalesOrderId());
            if (applied.discountAmount().compareTo(discount) != 0) {
                throw new IllegalStateException("Giá trị voucher thay đổi trong lúc tạo đơn; vui lòng thử lại.");
            }
        }
        if (counterFnbInventory != null && salesChannel == SalesChannel.BOX) counterFnbInventory.reserveOrder(order);
        for (ShowSeatEntity seat : seats) {
            seat.hold(order, expires);
            seatHoldRepository.findByShowSeat_ShowSeatIdAndHoldStatus(seat.getShowSeatId(), SeatHoldStatus.HELD)
                    .forEach(h -> { h.attachTo(order); });
        }
        return toOrderResponse(order);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(String orderCode, PaymentRequest request) {
        if (request == null || request.provider() == null || request.idempotencyKey() == null)
            throw new IllegalArgumentException("Thông tin thanh toán không hợp lệ.");
        SalesOrderEntity order = salesOrderRepository.findByOrderNumberForUpdate(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        if (paymentTxnRepository == null) throw new IllegalStateException("Payment repository chưa được cấu hình.");
        var previous = paymentTxnRepository.findByProviderCodeAndIdempotencyKey(request.provider(), request.idempotencyKey());
        if (previous.isPresent()) {
            if (!previous.get().getSalesOrder().getSalesOrderId().equals(order.getSalesOrderId()))
                throw new IllegalArgumentException("Idempotency key đã được dùng cho đơn hàng khác.");
            return new PaymentResponse(orderCode, previous.get().getPaymentStatus().name(),
                    order.getOrderStatus().name(), previous.get().getProviderTxnRef(), previous.get().getCompletedAt());
        }
        List<Long> seatIds = order.getLines().stream()
                .filter(line -> line.getItemType() == OrderLineType.TICKET)
                .map(SalesOrderLineEntity::getShowSeat)
                .filter(java.util.Objects::nonNull)
                .map(ShowSeatEntity::getShowSeatId)
                .toList();
        List<ShowSeatEntity> lockedSeats = seatIds.isEmpty()
                ? List.of() : showSeatRepository.findByShowSeatIdsForUpdate(seatIds);
        if (lockedSeats.size() != seatIds.size()) throw new SeatUnavailableException("Ghế của đơn hàng không còn tồn tại.");
        if (order.getOrderStatus() == OrderStatus.ISSUED)
            return new PaymentResponse(orderCode, "SUCCEEDED", order.getOrderStatus().name(), null, order.getPaidAt());
        OffsetDateTime now = OffsetDateTime.now();
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT || order.getExpiresAt() == null)
            throw new IllegalArgumentException("Đơn hàng không còn chờ thanh toán.");
        boolean paymentWindowExpired = !order.getExpiresAt().isAfter(now);
        PaymentTxnEntity payment = paymentTxnRepository.save(new PaymentTxnEntity(order, request.provider(), request.idempotencyKey(),
                request.paymentMethod() == null ? request.provider() : request.paymentMethod(), order.getTotalAmount(), now, request.successful()));
        if (!request.successful()) return new PaymentResponse(orderCode, "FAILED", order.getOrderStatus().name(),
                payment.getProviderTxnRef(), now);
        Map<Long, ShowSeatEntity> lockedSeatsById = new LinkedHashMap<>();
        lockedSeats.forEach(seat -> lockedSeatsById.put(seat.getShowSeatId(), seat));
        for (SalesOrderLineEntity line : order.getLines()) {
            if (line.getItemType() == OrderLineType.TICKET && line.getScreening() != null
                    && !line.getScreening().getStartsAt().isAfter(now))
                throw new SeatUnavailableException("Suất chiếu đã bắt đầu; cần đối soát giao dịch thay vì phát hành vé.");
        }
        for (ShowSeatEntity seat : lockedSeats) {
            boolean heldByThisOrder = seat.getSeatStatus() == ShowSeatStatus.HELD
                    && seat.getCurrentOrder() != null
                    && order.getSalesOrderId().equals(seat.getCurrentOrder().getSalesOrderId());
            boolean validHold = heldByThisOrder && seat.getHoldExpiresAt() != null
                    && seat.getHoldExpiresAt().isAfter(now);
            boolean recoverableExpiredHold = paymentWindowExpired && heldByThisOrder;
            boolean recoverableReleasedSeat = paymentWindowExpired
                    && seat.getSeatStatus() == ShowSeatStatus.AVAILABLE && seat.getCurrentOrder() == null;
            if (!validHold && !recoverableExpiredHold && !recoverableReleasedSeat)
                throw new SeatUnavailableException("Ghế đã được giữ hoặc bán cho đơn khác; giao dịch cần được đối soát.");
        }
        if (paymentWindowExpired) {
            OffsetDateTime recoveryHoldExpiresAt = now.plusSeconds(HOLD_SECONDS);
            for (ShowSeatEntity seat : lockedSeats) {
                boolean stillValidHold = seat.getSeatStatus() == ShowSeatStatus.HELD
                        && seat.getCurrentOrder() != null
                        && order.getSalesOrderId().equals(seat.getCurrentOrder().getSalesOrderId())
                        && seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isAfter(now);
                if (stillValidHold) continue;
                seat.hold(order, recoveryHoldExpiresAt);
                seatHoldRepository.save(new SeatHoldEntity(seat, order, recoveryHoldExpiresAt));
            }
        }
        if (promoVouchers != null) promoVouchers.confirmRedemption(order.getSalesOrderId());
        order.markPaid(now);
        if (counterFnbInventory != null) counterFnbInventory.completeOrder(order);
        for (SalesOrderLineEntity line : order.getLines()) {
            if (line.getItemType() != OrderLineType.TICKET) continue;
            ShowSeatEntity seat = line.getShowSeat() == null ? null
                    : lockedSeatsById.get(line.getShowSeat().getShowSeatId());
            if (seat == null) throw new SeatUnavailableException("Dòng vé không có ghế tương ứng.");
            seat.sell();
            ScreeningEntity s = line.getScreening();
            String token = UUID.randomUUID().toString();
            ticketRepository.save(new TicketEntity(line, "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(),
                    sha256(token), s.getFilm().getTitleLocal(), s.getCinema().getCinemaName(), s.getRoom().getRoomName(),
                    s.getStartsAt(), seat.getSeatLabelSnapshot()));
        }
        seatHoldRepository.findBySalesOrder_SalesOrderIdAndHoldStatus(order.getSalesOrderId(), SeatHoldStatus.HELD)
                .forEach(SeatHoldEntity::confirm);
        order.markIssued(now);
        return new PaymentResponse(orderCode, "SUCCEEDED", order.getOrderStatus().name(),
                payment.getProviderTxnRef(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(String orderCode) {
        return toOrderResponse(salesOrderRepository.findByOrderNumber(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng.")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long appUserId) {
        if (appUserId == null) return List.of();
        return salesOrderRepository.findByAppUserIdOrderByCreatedAtDesc(appUserId).stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.DRAFT)
                .map(this::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTickets(String orderCode) {
        SalesOrderEntity order = salesOrderRepository.findByOrderNumber(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        if (ticketRepository == null) return List.of();
        return ticketRepository.findByOrderLine_SalesOrder_SalesOrderId(order.getSalesOrderId()).stream()
                .map(t -> new TicketResponse(t.getTicketNumber(), t.getFilmTitleSnapshot(), t.getCinemaNameSnapshot(),
                        t.getRoomNameSnapshot(), t.getScreeningStartsAt(), t.getSeatLabelSnapshot(),
                        t.getTicketStatus().name(), "THEFILM:TICKET:" + t.getTicketNumber() + ":" + t.getQrTokenHash()))
                .toList();
    }

    private OrderResponse toOrderResponse(SalesOrderEntity order) {
        List<OrderResponse.OrderLine> lines = order.getLines().stream().map(l -> new OrderResponse.OrderLine(
                l.getProductNameSnapshot(), l.getScreeningTypeSnapshot(), l.getRoomTypeSnapshot(),
                l.getSeatTypeSnapshot(), l.getQuantity(), l.getUnitPrice(), l.getDiscountAmount(),
                l.getTaxAmount(), l.getLineTotal())).toList();
        List<TicketResponse> tickets = ticketRepository == null ? List.of() : ticketRepository
                .findByOrderLine_SalesOrder_SalesOrderId(order.getSalesOrderId()).stream().map(t -> new TicketResponse(
                        t.getTicketNumber(), t.getFilmTitleSnapshot(), t.getCinemaNameSnapshot(), t.getRoomNameSnapshot(),
                        t.getScreeningStartsAt(), t.getSeatLabelSnapshot(), t.getTicketStatus().name(),
                        "THEFILM:TICKET:" + t.getTicketNumber() + ":" + t.getQrTokenHash())).toList();
        return new OrderResponse(order.getOrderNumber(), order.getOrderStatus().name(), order.getSubtotalAmount(),
                order.getDiscountAmount(), order.getTaxAmount(), order.getTotalAmount(), order.getExpiresAt(), lines, tickets);
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Không thể tạo mã QR an toàn.", e); }
    }

    private boolean validHoldToken(SalesOrderEntity order, String token) {
        return token != null && order.getIdempotencyKey() != null && MessageDigest.isEqual(
                order.getIdempotencyKey().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal allocateDiscount(BigDecimal lineAmount, BigDecimal remainingBase, BigDecimal remainingDiscount) {
        if (remainingDiscount.signum() == 0 || lineAmount.signum() == 0) return BigDecimal.ZERO;
        if (lineAmount.compareTo(remainingBase) >= 0) return remainingDiscount;
        return remainingDiscount.multiply(lineAmount).divide(remainingBase, 2, RoundingMode.HALF_UP)
                .min(remainingDiscount);
    }

    private BigDecimal calculateVat(List<BigDecimal> lineAmounts, BigDecimal subtotal, BigDecimal discount) {
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal remainingDiscount = discount;
        BigDecimal remainingBase = subtotal;
        for (BigDecimal amount : lineAmounts) {
            BigDecimal lineDiscount = allocateDiscount(amount, remainingBase, remainingDiscount);
            remainingDiscount = remainingDiscount.subtract(lineDiscount);
            remainingBase = remainingBase.subtract(amount);
            vat = vat.add(amount.subtract(lineDiscount).multiply(new BigDecimal("0.08"))
                    .setScale(2, RoundingMode.HALF_UP));
        }
        return vat;
    }

    private SeatItemDto toSeatItem(ShowSeatEntity seat, OffsetDateTime now, java.math.BigDecimal basePrice) {
        boolean expired = seat.getSeatStatus() == ShowSeatStatus.HELD && isHoldExpired(seat, now);
        String status = expired ? ShowSeatStatus.AVAILABLE.name() : seat.getSeatStatus().name();
        return new SeatItemDto(seat.getShowSeatId(), seat.getSeatCodeSnapshot(), seat.getRowLabelSnapshot(),
                seat.getSeatLabelSnapshot(), seat.getSeatTypeCodeSnapshot(), status,
                basePrice == null ? java.math.BigDecimal.ZERO : basePrice,
                expired ? null : seat.getHoldExpiresAt());
    }

    private boolean isHoldExpired(ShowSeatEntity seat, OffsetDateTime now) {
        return seat.getHoldExpiresAt() == null || !seat.getHoldExpiresAt().isAfter(now);
    }
}
