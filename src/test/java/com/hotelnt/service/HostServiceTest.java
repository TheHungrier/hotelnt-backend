package com.hotelnt.service;

import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.helper.HostIdentityResolver.Identity;
import com.hotelnt.helper.HostIdentityResolver.Identity;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.hotelnt.common.error.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {
    @Mock EntityManager em;
    @Mock CoSoLuuTruRepository listings;
    @Mock TaiKhoanRepository accounts;
    @Mock LienHeRepository contacts;
    @Mock ApplicationEventPublisher events;
    BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    HostService service; TaiKhoan host;CoSoLuuTru c;LoaiCoSo type;Phuong ward;
    Identity identity=new Identity(1,"current");
    @BeforeEach void setup() {
        service=new HostService(em,listings,accounts,contacts,encoder,events);
        host=new TaiKhoan();host.setId(1);host.setVaiTro("HOST");host.setTrangThai("ACTIVE");host.setCccd("079123456234");
        host.setMatKhau(encoder.encode("Host@1234"));
        c=new CoSoLuuTru();c.setMaCSLT(10);c.setHost(host);c.setTrangThai("DRAFT");c.setTenCSLT("Villa");c.setDiaChiChiTiet("12 Tran Phu");
        c.setLatitude(new BigDecimal("12.20"));c.setLongitude(new BigDecimal("109.20"));c.setGia(new BigDecimal("650000"));
        type=new LoaiCoSo();type.setMaLoai(1);ward=new Phuong();ward.setMaPhuong(1);c.setLoai(type);c.setPhuong(ward);
        lenient().when(em.find(TaiKhoan.class,1)).thenReturn(host);
        lenient().when(em.find(TaiKhoan.class,1,LockModeType.PESSIMISTIC_WRITE)).thenReturn(host);
        lenient().when(em.find(CoSoLuuTru.class,10,LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        lenient().when(em.find(CoSoLuuTru.class,10)).thenReturn(c);
    }
    void error(ErrorCode code,org.junit.jupiter.api.function.Executable action) {
        assertEquals(code,assertThrows(AppException.class,action).getCode());
    }
    void complete() {
        for(int i=0;i<3;i++){Media m=new Media();m.setLoai("IMAGE");c.getMedia().add(m);}
        TienIch t=new TienIch();t.setMaTienIch(1);c.getTienIch().add(t);
    }
    ListingWrite write(BigDecimal price) {return new ListingWrite("Villa",1,1,"12 Tran Phu",null,null,price,List.of(),new BigDecimal("12.2"),new BigDecimal("109.2"));}
    void refs(){when(em.find(LoaiCoSo.class,1)).thenReturn(type);when(em.find(Phuong.class,1)).thenReturn(ward);}
    @Test void rejectsMissingIdentity(){error(ErrorCode.TOKEN_INVALID,()->service.profile(null));}
    @Test void rejectsAdmin(){host.setVaiTro("ADMIN");error(ErrorCode.FORBIDDEN_ROLE,()->service.profile(identity));}
    @Test void rejectsLockedHost(){host.setTrangThai("LOCKED");error(ErrorCode.ACCOUNT_LOCKED,()->service.profile(identity));}
    @Test void masksCccd(){assertEquals("079******234",service.profile(identity).cccd());}
    @Test void rejectsOtherOwner(){TaiKhoan other=new TaiKhoan();other.setId(2);c.setHost(other);error(ErrorCode.NOT_OWNER,()->service.withdraw(identity,10));}
    @Test void removedCannotBeViewed(){c.setTrangThai("REMOVED");error(ErrorCode.LISTING_NOT_FOUND,()->service.detail(identity,10));}
    @Test void cannotEditPending(){c.setTrangThai("PENDING");error(ErrorCode.LISTING_LOCKED_PENDING,()->service.update(identity,10,write(c.getGia())));}
    @Test void unchangedApprovedStaysApproved(){refs();c.setTrangThai("APPROVED");LocalDateTime stamp=LocalDateTime.now().minusDays(1);c.setNgayCapNhat(stamp);c.setNgayCapNhatGia(stamp);
        Updated r=service.update(identity,10,write(new BigDecimal("650000.0")));
        assertEquals("APPROVED",r.trangThai());assertFalse(r.thayDoiTrangThai());assertEquals(stamp,c.getNgayCapNhat());assertEquals(stamp,c.getNgayCapNhatGia());}
    @Test void changedApprovedRequiresReview(){refs();c.setTrangThai("APPROVED");
        Updated r=service.update(identity,10,write(new BigDecimal("700000")));
        assertEquals("PENDING",r.trangThai());assertTrue(r.thayDoiTrangThai());assertNotNull(c.getNgayCapNhatGia());assertNotNull(c.getNgayGuiDuyet());}
    @Test void rejectedRemainsRejectedOnEdit(){refs();c.setTrangThai("REJECTED");assertEquals("REJECTED",service.update(identity,10,write(new BigDecimal("700000"))).trangThai());}
    @Test void clearingPriceClearsTimestamp(){refs();c.setNgayCapNhatGia(LocalDateTime.now());service.update(identity,10,write(null));assertNull(c.getNgayCapNhatGia());}
    @Test void incompleteChecklistHasFieldErrors(){AppException e=assertThrows(AppException.class,()->service.submit(identity,10));assertEquals(ErrorCode.CHECKLIST_INCOMPLETE,e.getCode());assertTrue(e.getDetails().toString().contains("hinhAnh"));assertEquals("DRAFT",c.getTrangThai());}
    @Test void sendsCompleteListingForReview(){complete();assertEquals("PENDING",service.submit(identity,10).trangThai());assertNotNull(c.getNgayGuiDuyet());}
    @Test void hiddenCanResubmit(){complete();c.setTrangThai("HIDDEN");assertEquals("PENDING",service.submit(identity,10).trangThai());}
    @Test void approvedCannotSubmitAgain(){c.setTrangThai("APPROVED");error(ErrorCode.INVALID_STATE_TRANSITION,()->service.submit(identity,10));}
    @Test void cannotWithdrawAfterAdminApproves(){c.setTrangThai("APPROVED");error(ErrorCode.INVALID_STATE_TRANSITION,()->service.withdraw(identity,10));}
    @Test void withdrawClearsSubmissionDate(){c.setTrangThai("PENDING");c.setNgayGuiDuyet(LocalDateTime.now());assertEquals("DRAFT",service.withdraw(identity,10).trangThai());assertNull(c.getNgayGuiDuyet());}
    @Test void deletingDraftSoftDeletesAndPublishesCleanup(){service.delete(identity,10);assertEquals("REMOVED",c.getTrangThai());verify(events).publishEvent(any(ListingDeleted.class));verify(listings,never()).delete(any(CoSoLuuTru.class));}
    @Test void cannotDeleteApproved(){c.setTrangThai("APPROVED");error(ErrorCode.LISTING_NOT_DELETABLE,()->service.delete(identity,10));verifyNoInteractions(events);}
    @Test void rejectsIncompleteCoordinates(){ListingWrite q=write(c.getGia());q=new ListingWrite(q.tenCSLT(),1,1,q.diaChiChiTiet(),null,null,q.gia(),List.of(),new BigDecimal("12.2"),null);ListingWrite request=q;error(ErrorCode.VALIDATION_ERROR,()->service.update(identity,10,request));}
    @Test void rejectsCoordinateOutsideSupportedRegion(){ListingWrite q=write(c.getGia());ListingWrite request=new ListingWrite(q.tenCSLT(),1,1,q.diaChiChiTiet(),null,null,q.gia(),List.of(),new BigDecimal("10"),new BigDecimal("109.2"));error(ErrorCode.LOCATION_OUT_OF_RANGE,()->service.update(identity,10,request));}
    @Test void rejectsDuplicatePhone(){TaiKhoan other=new TaiKhoan();other.setId(2);when(accounts.findBySoDienThoai("0901234567")).thenReturn(Optional.of(other));error(ErrorCode.ACCOUNT_DUPLICATED,()->service.updateProfile(identity,new ProfileUpdate(null,"0901234567")));}
    @Test void passwordChangeNeedsValidSession(){error(ErrorCode.TOKEN_INVALID,()->service.changePassword(identity,new PasswordChange("Host@1234","NewPass@123")));}
    @Test void wrongOldPasswordRejected(){session();error(ErrorCode.OLD_PASSWORD_INCORRECT,()->service.changePassword(identity,new PasswordChange("wrong","NewPass@123")));}
    @Test void reusedPasswordRejected(){session();error(ErrorCode.VALIDATION_ERROR,()->service.changePassword(identity,new PasswordChange("Host@1234","Host@1234")));}
    @Test void changesHashAndRevokesOtherSessions(){session();Query query=mock(Query.class);when(em.createQuery(startsWith("update PhienDangNhap"))).thenReturn(query);when(query.setParameter(anyString(),any())).thenReturn(query);
        service.changePassword(identity,new PasswordChange("Host@1234","NewPass@123"));assertTrue(encoder.matches("NewPass@123",host.getMatKhau()));verify(query).setParameter("session","current");verify(query).executeUpdate();}
    @Test void noOpDoesNotReplaceContacts(){refs();service.update(identity,10,write(c.getGia()));verifyNoInteractions(contacts);}
    @Test void absentOriginDoesNotPassSubmitChecklist(){complete();c.setLatitude(null);error(ErrorCode.CHECKLIST_INCOMPLETE,()->service.submit(identity,10));}
    @Test void rejectedDeletionAllowed(){c.setTrangThai("REJECTED");service.delete(identity,10);assertEquals("REMOVED",c.getTrangThai());}
    @Test void pendingDeletionRejected(){c.setTrangThai("PENDING");error(ErrorCode.LISTING_NOT_DELETABLE,()->service.delete(identity,10));}
    @Test void createAssignsOwnerAndDraft(){refs();when(listings.saveAndFlush(any(CoSoLuuTru.class))).thenAnswer(inv->{CoSoLuuTru row=inv.getArgument(0);row.setMaCSLT(11);return row;});
        Created result=service.create(identity,write(new BigDecimal("650000")));assertEquals(11,result.maCSLT());assertEquals("DRAFT",result.trangThai());
        ArgumentCaptor<CoSoLuuTru> captor=ArgumentCaptor.forClass(CoSoLuuTru.class);verify(listings).saveAndFlush(captor.capture());assertSame(host,captor.getValue().getHost());}
    @Test void rejectsScriptContactLink(){refs();ListingWrite q=write(c.getGia());ListingWrite request=new ListingWrite(q.tenCSLT(),1,1,q.diaChiChiTiet(),null,new Contact(null,"javascript:alert(1)",null),q.gia(),List.of(),q.latitude(),q.longitude());error(ErrorCode.VALIDATION_ERROR,()->service.update(identity,10,request));}
    void session(){PhienDangNhap s=new PhienDangNhap();s.setTaiKhoan(host);s.setHetHanLuc(LocalDateTime.now().plusHours(1));when(em.find(PhienDangNhap.class,"current",LockModeType.PESSIMISTIC_WRITE)).thenReturn(s);}
}