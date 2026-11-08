package vn.thefilm.backend.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.thefilm.backend.repository.EmployeeAssignmentRepository;
import vn.thefilm.backend.repository.EmployeeRepository;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.UserCinemaScopeRepository;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.service.CinemaScopeService;

@Service
@Transactional(readOnly = true)
public class CinemaScopeServiceImpl implements CinemaScopeService {

    private final UserCinemaScopeRepository userCinemaScopeRepository;
    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssignmentRepository employeeAssignmentRepository;

    public CinemaScopeServiceImpl(
            UserCinemaScopeRepository userCinemaScopeRepository,
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            EmployeeAssignmentRepository employeeAssignmentRepository) {
        this.userCinemaScopeRepository = userCinemaScopeRepository;
        this.appUserRepository = appUserRepository;
        this.employeeRepository = employeeRepository;
        this.employeeAssignmentRepository = employeeAssignmentRepository;
    }

    @Override
    public Set<Long> getEffectiveCinemaIds(Long appUserId) {
        if (appUserId == null) {
            return Set.of();
        }
        if (appUserRepository.findById(appUserId)
                .filter(user -> user.getStatus() == AppUserStatus.ACTIVE)
                .isEmpty()) {
            return Set.of();
        }

        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = now.toLocalDate();
        Set<Long> cinemaIds = userCinemaScopeRepository.findActiveScopes(appUserId, now).stream()
                .map(scope -> scope.getCinema().getCinemaId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        employeeRepository.findByAppUser_AppUserId(appUserId)
                .filter(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
                .ifPresent(employee -> employeeAssignmentRepository
                        .findActiveAssignments(employee.getEmployeeId(), today).stream()
                        .map(assignment -> assignment.getCinema().getCinemaId())
                        .forEach(cinemaIds::add));

        return Set.copyOf(cinemaIds);
    }

    @Override
    public boolean hasEffectiveCinemaScope(Long appUserId, Long cinemaId) {
        return cinemaId != null && getEffectiveCinemaIds(appUserId).contains(cinemaId);
    }
}
