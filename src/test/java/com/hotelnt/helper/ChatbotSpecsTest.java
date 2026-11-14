package com.hotelnt.helper;

import com.hotelnt.domain.CoSoLuuTru;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotSpecsTest {

    @Mock
    private Root<CoSoLuuTru> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> trangThaiPath;

    @Mock
    @SuppressWarnings("rawtypes")
    private Join hostJoin;

    @Mock
    private Path<Object> hostTrangThaiPath;

    @Mock
    private Predicate approvedPredicate;

    @Mock
    private Predicate hostActivePredicate;

    @Mock
    private Predicate andPredicate;

    @Test
    @DisplayName("visibleApproved tạo điều kiện trangThai = APPROVED và host.trangThai = ACTIVE")
    @SuppressWarnings("unchecked")
    void visibleApproved_createsPredicatesForApprovedListingAndActiveHost() {
        Specification<CoSoLuuTru> spec = ChatbotSpecs.visibleApproved();

        doReturn(String.class).when(query).getResultType();
        when(root.get("trangThai")).thenReturn(trangThaiPath);
        when(cb.equal(trangThaiPath, "APPROVED")).thenReturn(approvedPredicate);

        doReturn(hostJoin).when(root).join(eq("host"), eq(JoinType.INNER));
        when(hostJoin.get("trangThai")).thenReturn(hostTrangThaiPath);
        when(cb.equal(hostTrangThaiPath, "ACTIVE")).thenReturn(hostActivePredicate);

        when(cb.and(approvedPredicate, hostActivePredicate)).thenReturn(andPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(andPredicate);
        verify(root).fetch("loai", JoinType.LEFT);
        verify(root).fetch("phuong", JoinType.LEFT);
        verify(cb).equal(trangThaiPath, "APPROVED");
        verify(cb).equal(hostTrangThaiPath, "ACTIVE");
    }
}
