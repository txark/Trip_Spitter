package com.cp.party_trip.service;

import com.cp.party_trip.dto.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExpenseService {

    private final ExpenseRepo expenseRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final ActivityRepo activityRepo;

    public ExpenseService(ExpenseRepo expenseRepo, TripRepo tripRepo, TripMemberRepo tripMemberRepo,
            ActivityRepo activityRepo) {
        this.expenseRepo = expenseRepo;
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
        this.activityRepo = activityRepo;
    }

    @Transactional
    public Expense createExpense(Long tripId, Long userId, ExpenseRequest request, List<Long> participantIds) {
        return createExpense(tripId, userId, null, request, participantIds);
    }

    // recordedBy = คนที่กดบันทึก (บันทึกแทนเพื่อนที่จ่ายได้), ไม่ส่ง = คนจ่ายบันทึกเอง
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
        return expenseRepo.save(expense);
    }

    // แก้บิล: เฉพาะคนจ่าย และยังไม่มีเพื่อนคนไหนจ่ายคืน (ไม่งั้นยอดที่คืนไปแล้วจะไม่ตรงกับบิลใหม่)
    @Transactional
    public Expense updateExpense(Long expenseId, Long memberId, ExpenseRequest request, List<Long> participantIds) {
        return updateExpense(expenseId, memberId, null, request, participantIds);
    }

    // newPayerId = เปลี่ยนคนจ่าย (ไม่ส่ง = คนเดิม) คนที่แก้จะกลายเป็นคนบันทึก จะได้ยังแก้บิลนี้ต่อได้
    @Transactional
    public Expense updateExpense(Long expenseId, Long memberId, Long newPayerId, ExpenseRequest request,
            List<Long> participantIds) {
        Expense expense = findEditable(expenseId, memberId);
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

    @Transactional
    public void deleteExpense(Long expenseId, Long memberId) {
        expenseRepo.delete(findEditable(expenseId, memberId));
    }

    private Expense findEditable(Long expenseId, Long memberId) {
        Expense expense = expenseRepo.findById(expenseId)
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

        // ไม่ระบุวิธีหาร = หารเท่ากัน, ระบุแบบที่ไม่รองรับ = ผิด (เดิมบันทึกบิลโดยไม่มีผู้ร่วมหาร ทำให้ยอดหนี้เพี้ยน)
        String splitType = request.getSplitType() == null || request.getSplitType().isBlank()
                ? "EQUAL"
                : request.getSplitType().trim().toUpperCase();
        if (!"EQUAL".equals(splitType) && !"CUSTOM".equals(splitType)) {
            throw badRequest("วิธีหารไม่ถูกต้อง (รองรับ EQUAL หรือ CUSTOM)");
        }

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
        expense.setSplitType(splitType);

        List<ExpenseSplit> splits = expense.getExpenseSplits();

        if ("CUSTOM".equals(expense.getSplitType())) {
            // CUSTOM SPLIT: แต่ละคนจ่ายไม่เท่ากัน ตามยอดที่กรอกมา
            List<ExpenseRequest.SplitAmount> custom = request.getSplits();
            if (custom == null || custom.isEmpty()) {
                throw badRequest("กรุณาระบุยอดของผู้ร่วมหารแต่ละคน");
            }

            BigDecimal sum = BigDecimal.ZERO;
            Set<Long> seen = new HashSet<>();
            for (ExpenseRequest.SplitAmount s : custom) {
                if (s.getMemberId() == null || !seen.add(s.getMemberId())) {
                    throw badRequest("รายชื่อผู้ร่วมหารไม่ถูกต้อง");
                }
                if (s.getAmount() == null || s.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw badRequest("ยอดของแต่ละคนต้องไม่ติดลบ");
                }
                if (s.getAmount().stripTrailingZeros().scale() > 2) {
                    throw badRequest("ยอดของแต่ละคนใส่ทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
                }
                sum = sum.add(s.getAmount());
            }
            if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(totalAmount) != 0) {
                throw badRequest("ผลรวมของแต่ละคน (" + sum + ") ไม่เท่ากับยอดบิล (" + totalAmount + ")");
            }

            for (ExpenseRequest.SplitAmount s : custom) {
                // คนที่ยอด 0 ไม่ต้องสร้าง split (ไม่ได้ร่วมจ่ายบิลนี้)
                if (s.getAmount().compareTo(BigDecimal.ZERO) == 0)
                    continue;
                splits.add(newSplit(expense, findTripMember(tripId, s.getMemberId()),
                        s.getAmount().setScale(2, RoundingMode.HALF_UP)));
            }
        } else {
            // EQUAL SPLIT: ตัดรายชื่อซ้ำออก (ซ้ำแล้วคนเดียวจะมี 2 split และกดจ่ายได้แค่อันเดียว)
            // ไม่มีผู้ร่วมหารเลย = ผู้จ่ายออกเองทั้งหมด
            List<Long> people = new ArrayList<>(new LinkedHashSet<>(
                    participantIds == null ? List.<Long>of() : participantIds));
            people.removeIf(java.util.Objects::isNull);
            if (people.isEmpty()) {
                people.add(paidBy.getId());
            }
            int count = people.size();
            BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            // เศษจากการปัดทศนิยม (เช่น 100/3) ให้คนแรกรับไป เพื่อให้ผลรวม splits เท่ากับยอดบิลพอดี
            BigDecimal remainder = totalAmount.subtract(perPerson.multiply(BigDecimal.valueOf(count)));

            for (Long memberId : people) {
                splits.add(newSplit(expense, findTripMember(tripId, memberId),
                        splits.isEmpty() ? perPerson.add(remainder) : perPerson));
            }
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

    public List<Expense> getExpensesByTrip(Long tripId) {
        return expenseRepo.findByTripId(tripId);
    }
}
