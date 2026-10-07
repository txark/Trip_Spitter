package com.cp.party_trip.service.impl;

import com.cp.party_trip.dto.response.ExpenseViewResponse;
import com.cp.party_trip.mapper.ExpenseMapper;
import com.cp.party_trip.service.ExpenseService;
import com.cp.party_trip.common.Money;
import com.cp.party_trip.dto.request.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.ExpenseSplitRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.cp.party_trip.event.ExpenseAddedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.cp.party_trip.service.split.SplitContext;
import com.cp.party_trip.service.split.SplitShare;
import com.cp.party_trip.service.split.SplitStrategy;
import com.cp.party_trip.service.split.SplitStrategyFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepo expenseRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final ActivityRepo activityRepo;
    private final ExpenseSplitRepo expenseSplitRepo;
    private final ExpenseMapper expenseMapper;
    private final SplitStrategyFactory splitStrategyFactory;
    private final ApplicationEventPublisher events;

    public ExpenseServiceImpl(ExpenseRepo expenseRepo, TripRepo tripRepo, TripMemberRepo tripMemberRepo,
            ActivityRepo activityRepo, ExpenseSplitRepo expenseSplitRepo,
            ExpenseMapper expenseMapper, SplitStrategyFactory splitStrategyFactory,
            ApplicationEventPublisher events) {
        this.expenseRepo = expenseRepo;
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
        this.activityRepo = activityRepo;
        this.expenseSplitRepo = expenseSplitRepo;
        this.expenseMapper = expenseMapper;
        this.splitStrategyFactory = splitStrategyFactory;
        this.events = events;
    }

    @Override
    @Transactional
    public Expense createExpense(Long tripId, Long userId, ExpenseRequest request, List<Long> participantIds) {
        return createExpense(tripId, userId, null, request, participantIds);
    }

    // recordedBy = คนที่กดบันทึก (บันทึกแทนเพื่อนที่จ่ายได้), ไม่ส่ง = คนจ่ายบันทึกเอง
    @Override
    @Transactional
    public Expense createExpense(Long tripId, Long userId, Long recordedBy, ExpenseRequest request,
            List<Long> participantIds) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลทริป"));

        // ผู้จ่ายต้องเป็นสมาชิกของทริปนี้ (เดิมรับสมาชิกทริปอื่นได้)
        TripMember paidBy = findTripMember(tripId, userId);
        TripMember recorder = recordedBy == null ? paidBy : findTripMember(tripId, recordedBy);

        Expense expense = new Expense();
        expense.setTrip(trip);
        expense.setUser(paidBy);
        expense.setRecordedById(recorder.getId());
        expense.setExpenseSplits(new ArrayList<>());
        apply(expense, tripId, paidBy, request, participantIds);
        Expense saved = expenseRepo.save(expense);
        events.publishEvent(new ExpenseAddedEvent(tripId, recorder.getId(), recorder.getGuestName(),
                saved.getTitle(), saved.getTotalAmount()));
        return saved;
    }

    // แก้บิล: เฉพาะคนจ่าย และยังไม่มีเพื่อนคนไหนจ่ายคืน (ไม่งั้นยอดที่คืนไปแล้วจะไม่ตรงกับบิลใหม่)
    @Override
    @Transactional
    public Expense updateExpense(Long expenseId, Long memberId, ExpenseRequest request, List<Long> participantIds) {
        return updateExpense(expenseId, memberId, null, request, participantIds);
    }

    // newPayerId = เปลี่ยนคนจ่าย (ไม่ส่ง = คนเดิม) คนที่แก้จะกลายเป็นคนบันทึก จะได้ยังแก้บิลนี้ต่อได้
    @Override
    @Transactional
    public Expense updateExpense(Long expenseId, Long memberId, Long newPayerId, ExpenseRequest request,
            List<Long> participantIds) {
        return updateExpense(expenseId, memberId, newPayerId, request, participantIds, null);
    }

    // expectedRevision = รุ่นของบิลตอนเปิดฟอร์มแก้ (ไม่ตรงกับปัจจุบัน = อีกเครื่องแก้ไปแล้ว -> 409)
    @Override
    @Transactional
    public Expense updateExpense(Long expenseId, Long memberId, Long newPayerId, ExpenseRequest request,
            List<Long> participantIds, Integer expectedRevision) {
        Expense expense = findEditable(expenseId, memberId);
        requireRevision(expense, expectedRevision);
        expense.setRevision(expense.getRevision() + 1);
        Long tripId = expense.getTrip().getId();
        if (newPayerId != null && !newPayerId.equals(expense.getUser().getId())) {
            expense.setUser(findTripMember(tripId, newPayerId));
            expense.setRecordedById(memberId);
        }
        // สร้างส่วนแบ่งใหม่ทั้งหมด (ลิสต์เดิม + orphanRemoval = ลบแถวเก่าให้เอง)
        if (expense.getExpenseSplits() == null) {
            expense.setExpenseSplits(new ArrayList<>());
        }
        expense.getExpenseSplits().clear();
        apply(expense, tripId, expense.getUser(), request, participantIds);
        return expenseRepo.save(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long expenseId, Long memberId) {
        deleteExpense(expenseId, memberId, null);
    }

    @Override
    @Transactional
    public void deleteExpense(Long expenseId, Long memberId, Integer expectedRevision) {
        Expense expense = findEditable(expenseId, memberId);
        requireRevision(expense, expectedRevision);
        expenseRepo.delete(expense);
    }

    // findEditable ล็อกแถวบิลไว้แล้ว: คำขอที่ 2 จากอีกเครื่องจะรอจนคำขอแรกเสร็จ แล้วเจอรุ่นที่ไม่ตรง
    private void requireRevision(Expense expense, Integer expectedRevision) {
        if (expectedRevision != null && expectedRevision != expense.getRevision()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "บิลนี้เพิ่งถูกแก้จากอีกเครื่อง โหลดข้อมูลใหม่แล้วลองอีกครั้ง");
        }
    }

    private Expense findEditable(Long expenseId, Long memberId) {
        Expense expense = expenseRepo.lockById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบบิลนี้"));
        TripMember paidBy = expense.getUser();
        boolean allowed = memberId != null && paidBy != null
                && (memberId.equals(paidBy.getId()) || memberId.equals(expense.getRecordedById()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "แก้หรือลบได้เฉพาะคนที่จ่ายหรือคนที่บันทึกบิลนี้");
        }
        boolean repaid = expense.getExpenseSplits() != null && expense.getExpenseSplits().stream()
                .anyMatch(s -> s.paidSoFar().signum() > 0 && s.getTripMember() != null
                        && !paidBy.getId().equals(s.getTripMember().getId()));
        if (repaid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "มีเพื่อนจ่ายคืนบิลนี้แล้ว จึงแก้หรือลบไม่ได้");
        }
        return expense;
    }

    // ตรวจข้อมูลบิล แล้วเติมลง expense พร้อมสร้างส่วนแบ่งของแต่ละคน (ใช้ทั้งตอนเพิ่มและแก้)
    private void apply(Expense expense, Long tripId, TripMember paidBy, ExpenseRequest request,
            List<Long> participantIds) {
        if (request.getTotalAmount() == null) {
            throw badRequest("ยอดบิลต้องมากกว่า 0");
        }
        // เก็บเงินเป็นทศนิยม 2 ตำแหน่งเสมอ ให้ยอดบิลกับผลรวมของแต่ละคนตรงกันพอดี
        BigDecimal totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("ยอดบิลต้องมากกว่า 0");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw badRequest("กรุณาตั้งชื่อรายการ");
        }

        // เลือกวิธีหารจาก splitType (ไม่ระบุ = หารเท่ากัน, ไม่รองรับ = 400) ตรรกะการหารอยู่ใน SplitStrategy
        SplitStrategy splitStrategy = splitStrategyFactory.forType(request.getSplitType());

        expense.setTitle(request.getTitle().trim());
        expense.setTotalAmount(totalAmount);
        // เงินต่างประเทศ: ยอดเงินบาทต้องตรงกับ ยอดตามใบเสร็จ × เรท (คลาดได้ไม่เกิน 1 สตางค์จากการปัด)
        String currency = Money.currency(request.getCurrency());
        if (currency == null) {
            expense.setCurrency(Money.BASE);
            expense.setOriginalAmount(null);
            expense.setExchangeRate(null);
        } else {
            BigDecimal original = request.getOriginalAmount();
            if (original == null || original.signum() <= 0) {
                throw badRequest("กรุณาใส่ยอดตามใบเสร็จ");
            }
            BigDecimal rate = Money.rate(request.getExchangeRate());
            original = original.setScale(2, RoundingMode.HALF_UP);
            BigDecimal expected = original.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            if (expected.subtract(totalAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw badRequest("ยอดเงินบาทไม่ตรงกับยอดตามใบเสร็จ × อัตราแลกเปลี่ยน");
            }
            expense.setCurrency(currency);
            expense.setOriginalAmount(original);
            expense.setExchangeRate(rate);
        }
        expense.setCategory(request.getCategory());
        // วันที่จ่าย: เปลี่ยนแค่วัน เวลาคงเดิม (บิลใหม่ = เวลาตอนบันทึก)
        if (request.getExpenseDate() != null) {
            int year = request.getExpenseDate().getYear();
            if (year < 2000 || year > 2100) {
                throw badRequest("วันที่ของบิลไม่ถูกต้อง");
            }
            // วันที่จ่ายเป็นอนาคตไม่ได้ (เผื่อ 1 วัน: เขตเวลาของทริปอาจเร็วกว่าเซิร์ฟเวอร์)
            if (request.getExpenseDate().isAfter(java.time.LocalDate.now().plusDays(1))) {
                throw badRequest("วันที่จ่ายต้องไม่เป็นวันในอนาคต");
            }
            java.time.LocalTime time = expense.getExpenseDate() != null
                    ? expense.getExpenseDate().toLocalTime()
                    : java.time.LocalTime.now();
            expense.setExpenseDate(request.getExpenseDate().atTime(time));
        }
        if (request.getActivityId() != null) {
            boolean sameTrip = activityRepo.findById(request.getActivityId())
                    .map(a -> a.getTrip() != null && tripId.equals(a.getTrip().getId()))
                    .orElse(false);
            if (!sameTrip) {
                throw badRequest("ไม่พบรายการนี้ในแพลนของทริป");
            }
        }
        expense.setActivityId(request.getActivityId());
        expense.setSplitType(splitStrategy.type());

        List<ExpenseSplit> splits = expense.getExpenseSplits();

        SplitContext context = new SplitContext(totalAmount, paidBy.getId(), participantIds, request.getSplits());
        for (SplitShare share : splitStrategy.split(context)) {
            splits.add(newSplit(expense, findTripMember(tripId, share.memberId()), share.amount()));
        }
    }

    // หาสมาชิก และตรวจว่าอยู่ในทริปนี้จริง (กันส่ง ID สมาชิกของทริปอื่นมา)
    private TripMember findTripMember(Long tripId, Long memberId) {
        TripMember member = tripMemberRepo.findById(memberId)
                .orElseThrow(() -> badRequest("ไม่พบข้อมูลสมาชิกผู้ร่วมหาร"));
        if (member.getTrip() == null || !tripId.equals(member.getTrip().getId())) {
            throw badRequest("สมาชิก #" + memberId + " ไม่ได้อยู่ในทริปนี้");
        }
        return member;
    }

    private ExpenseSplit newSplit(Expense expense, TripMember member, BigDecimal amount) {
        ExpenseSplit split = new ExpenseSplit();
        split.setExpense(expense);
        split.setTripMember(member);
        split.setAmountOwed(amount);
        return split;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @Override
    public List<Expense> getExpensesByTrip(Long tripId) {
        return expenseRepo.findByTripId(tripId);
    }

    // บิลทั้งทริป + การแบ่งเงินของแต่ละบิล ในรูปที่หน้าเว็บใช้
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseViewResponse> getTripExpenseViews(Long tripId) {
        return expenseRepo.findByTripId(tripId).stream()
                .map(exp -> expenseMapper.toView(exp, expenseSplitRepo.findByExpenseId(exp.getId())))
                .toList();
    }

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> SORTABLE = Set.of("expenseDate", "totalAmount", "title", "id");

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseViewResponse> getTripExpenseViewsPage(Long tripId, Pageable pageable) {
        // เรียงได้เฉพาะช่องที่อนุญาต (ชื่อช่องมาจากผู้ใช้ ถ้าไม่ตรวจจะได้ 500 หรือเรียงด้วยช่องที่ไม่ควรเปิด)
        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw badRequest("เรียงลำดับด้วย '" + order.getProperty() + "' ไม่ได้ (ใช้ได้: expenseDate, totalAmount, title, id)");
            }
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw badRequest("ขอได้ไม่เกินหน้าละ " + MAX_PAGE_SIZE + " รายการ");
        }
        Pageable effective = pageable.getSort().isSorted() ? pageable
                : org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                                "expenseDate", "id"));
        return expenseRepo.findPageByTripId(tripId, effective)
                .map(exp -> expenseMapper.toView(exp, expenseSplitRepo.findByExpenseId(exp.getId())));
    }

    // คนจ่ายบิล (หรือคนบันทึกแทน) ยืนยันว่าได้รับเงินส่วนของ memberId ครบแล้ว
    // actingMemberId = สมาชิกของเจ้าของ token
    @Override
    @Transactional
    public void markSplitPaid(Long expenseId, Long memberId, Long actingMemberId) {
        Expense expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบบิลนี้"));
        boolean owner = (expense.getUser() != null && actingMemberId.equals(expense.getUser().getId()))
                || actingMemberId.equals(expense.getRecordedById());
        if (!owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ยืนยันรับเงินได้เฉพาะคนที่จ่ายบิลนี้");
        }
        // คนบันทึกแทนที่เป็นลูกหนี้ในบิลเดียวกัน ห้ามกดว่าตัวเองจ่ายแล้ว (ให้คนจ่ายจริงยืนยัน)
        boolean payer = expense.getUser() != null && actingMemberId.equals(expense.getUser().getId());
        if (actingMemberId.equals(memberId) && !payer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ยืนยันว่าตัวเองจ่ายคืนแล้วไม่ได้ ให้คนที่จ่ายบิลเป็นคนยืนยัน");
        }
        List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseId(expenseId);

        if (splits != null) {
            for (ExpenseSplit split : splits) {
                if (split.getTripMember() != null && split.getTripMember().getId().equals(memberId)) {
                    split.setPaid(true); // ได้รับส่วนที่เหลือครบแล้ว
                    split.setPaidAmount(split.getAmountOwed());
                    expenseSplitRepo.save(split);
                    return;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบส่วนของสมาชิกคนนี้ในบิล");
    }
}
