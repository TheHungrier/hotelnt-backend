package com.hotelnt.modules.host;

import com.hotelnt.common.error.*;
import com.hotelnt.common.util.Times;
import com.hotelnt.domain.*;
import com.hotelnt.modules.host.HostDtos.*;
import com.hotelnt.modules.host.HostIdentityResolver.Identity;
import com.hotelnt.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class HostService {
    private final EntityManager em;
    private final CoSoLuuTruRepository listings;
    private final TaiKhoanRepository accounts;
    private final LienHeRepository contacts;
    private final BCryptPasswordEncoder passwords;
    private final ApplicationEventPublisher events;
    private static final Set<String> EDITABLE = Set.of("DRAFT","REJECTED","HIDDEN","APPROVED");
    private static final Set<String> SUBMITTABLE = Set.of("DRAFT","REJECTED","HIDDEN");

    public HostService(EntityManager em, CoSoLuuTruRepository listings, TaiKhoanRepository accounts,
                       LienHeRepository contacts, BCryptPasswordEncoder passwords, ApplicationEventPublisher events) {
        this.em=em; this.listings=listings; this.accounts=accounts; this.contacts=contacts;
        this.passwords=passwords; this.events=events;
    }
    private TaiKhoan actor(Identity identity, boolean lock) {
        if(identity==null || identity.accountId()==null) throw new AppException(ErrorCode.TOKEN_INVALID);
        TaiKhoan a=lock ? em.find(TaiKhoan.class,identity.accountId(),LockModeType.PESSIMISTIC_WRITE)
                       : em.find(TaiKhoan.class,identity.accountId());
        if(a==null) throw new AppException(ErrorCode.TOKEN_INVALID);
        if(!"HOST".equals(a.getVaiTro())) throw new AppException(ErrorCode.FORBIDDEN_ROLE);
        if(!"ACTIVE".equals(a.getTrangThai())) throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        return a;
    }
    private CoSoLuuTru owned(Identity identity,int id,boolean lock) {
        TaiKhoan a=actor(identity,lock);
        CoSoLuuTru c=lock ? em.find(CoSoLuuTru.class,id,LockModeType.PESSIMISTIC_WRITE) : em.find(CoSoLuuTru.class,id);
        if(c==null || "REMOVED".equals(c.getTrangThai())) throw new AppException(ErrorCode.LISTING_NOT_FOUND);
        if(!a.getId().equals(c.getHost().getId())) throw new AppException(ErrorCode.NOT_OWNER);
        return c;
    }
    public Profile profile(Identity identity) {
        TaiKhoan a=actor(identity,false);
        String cccd=a.getCccd();
        if(cccd!=null) cccd=cccd.length()>6 ? cccd.substring(0,3)+"*".repeat(cccd.length()-6)+cccd.substring(cccd.length()-3) : "*".repeat(cccd.length());
        return new Profile(a.getId(),a.getVaiTro(),a.getTrangThai(),a.getHoTen(),a.getSoDienThoai(),a.getEmail(),cccd,Times.toInstant(a.getNgayTao()));
    }
    @Transactional
    public ProfileUpdated updateProfile(Identity identity, ProfileUpdate q) {
        TaiKhoan a=actor(identity,true);
        if(q.hoTen()==null && q.soDienThoai()==null) invalid("body","Không có trường nào để cập nhật");
        if(q.hoTen()!=null && q.hoTen().isBlank()) invalid("hoTen","Họ tên không được để trống");
        if(q.soDienThoai()!=null) {
            accounts.findBySoDienThoai(q.soDienThoai()).filter(other->!other.getId().equals(a.getId()))
                .ifPresent(other->{throw new AppException(ErrorCode.ACCOUNT_DUPLICATED,List.of(Map.of("field","soDienThoai","message","Số điện thoại đã được sử dụng")));});
            a.setSoDienThoai(q.soDienThoai());
        }
        if(q.hoTen()!=null) a.setHoTen(q.hoTen().trim());
        a.setNgayCapNhat(LocalDateTime.now());
        accounts.saveAndFlush(a);
        return new ProfileUpdated(a.getId(),a.getHoTen(),a.getSoDienThoai(),a.getEmail(),Times.toInstant(a.getNgayCapNhat()));
    }
    @Transactional
    public Message changePassword(Identity identity, PasswordChange q) {
        TaiKhoan a=actor(identity,true);
        if(identity.sessionId()==null) throw new AppException(ErrorCode.TOKEN_INVALID);
        PhienDangNhap current=em.find(PhienDangNhap.class,identity.sessionId(),LockModeType.PESSIMISTIC_WRITE);
        if(current==null || !a.getId().equals(current.getTaiKhoan().getId()) || Boolean.TRUE.equals(current.getDaThuHoi())
            || current.getHetHanLuc()==null || !current.getHetHanLuc().isAfter(LocalDateTime.now())) throw new AppException(ErrorCode.TOKEN_INVALID);
        if(!passwords.matches(q.oldPassword(),a.getMatKhau())) throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        if(passwords.matches(q.newPassword(),a.getMatKhau())) invalid("newPassword","Mật khẩu mới phải khác mật khẩu cũ");
        if(q.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) invalid("newPassword","Mật khẩu vượt quá 72 byte UTF-8");
        a.setMatKhau(passwords.encode(q.newPassword())); a.setNgayCapNhat(LocalDateTime.now());
        em.createQuery("update PhienDangNhap p set p.daThuHoi=true where p.taiKhoan.id=:id and p.maPhien<>:session")
            .setParameter("id",a.getId()).setParameter("session",identity.sessionId()).executeUpdate();
        return new Message("Đổi mật khẩu thành công");
    }
    public ListingPage list(Identity identity, ListingQuery q) {
        TaiKhoan a=actor(identity,false);
        Set<String> states=q.trangThai()==null ? Set.of() : new HashSet<>(q.trangThai());
        if(!Set.of("DRAFT","PENDING","APPROVED","REJECTED","HIDDEN").containsAll(states)) invalid("trangThai","Trạng thái không hợp lệ");
        Specification<CoSoLuuTru> spec=(root,query,cb)-> {
            var p=new ArrayList<jakarta.persistence.criteria.Predicate>();
            p.add(cb.equal(root.get("host").get("id"),a.getId()));
            p.add(cb.notEqual(root.get("trangThai"),"REMOVED"));
            if(!states.isEmpty())p.add(root.get("trangThai").in(states));
            if(q.keyword()!=null && !q.keyword().isBlank()) {
                String kw=q.keyword().trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
                p.add(cb.like(cb.lower(root.get("tenCSLT")),"%"+kw+"%",'\\'));
            }
            return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        int page=q.page()==null?1:q.page(),limit=q.limit()==null?10:q.limit();
        Page<CoSoLuuTru> result=listings.findAll(spec,PageRequest.of(page-1,limit,Sort.by(Sort.Direction.DESC,"ngayCapNhat").and(Sort.by("maCSLT"))));
        return new ListingPage(result.stream().map(c->new ListingSummary(c.getMaCSLT(),c.getTenCSLT(),c.getLoai().getTenLoai(),c.getGia(),
            c.getMedia().stream().filter(m->"IMAGE".equals(m.getLoai())).map(Media::getDuongDan).findFirst().orElse(null),
            c.getTrangThai(),c.getLyDoTuChoi(),c.getLyDoViPham(),Times.toInstant(c.getNgayTao()),Times.toInstant(c.getNgayCapNhat()),actions(c))).toList(),
            new Pagination(result.getTotalElements(),result.getTotalPages(),page,limit));
    }
    public ListingDetail detail(Identity identity,int id) { return detailOf(owned(identity,id,false)); }
    @Transactional
    public Created create(Identity identity, ListingWrite q) {
        TaiKhoan a=actor(identity,true); CoSoLuuTru c=new CoSoLuuTru();c.setHost(a);c.setTrangThai("DRAFT");
        apply(c,q);listings.saveAndFlush(c);
        return new Created(c.getMaCSLT(),c.getTrangThai(),Times.toInstant(c.getNgayTao()));
    }
    @Transactional
    public Updated update(Identity identity,int id, ListingWrite q) {
        CoSoLuuTru c=owned(identity,id,true);String before=c.getTrangThai();
        if("PENDING".equals(before)) throw new AppException(ErrorCode.LISTING_LOCKED_PENDING);
        if(!EDITABLE.contains(before)) throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        boolean changed=apply(c,q);
        if(changed && "APPROVED".equals(before)) {c.setTrangThai("PENDING");c.setNgayGuiDuyet(LocalDateTime.now());}
        return new Updated(c.getMaCSLT(),c.getTrangThai(),before,Times.toInstant(c.getNgayCapNhat()),Times.toInstant(c.getNgayCapNhatGia()),
            !Objects.equals(before,c.getTrangThai()),"Cập nhật cơ sở thành công");
    }
    @Transactional
    public Submitted submit(Identity identity,int id) {
        CoSoLuuTru c=owned(identity,id,true);
        if(!SUBMITTABLE.contains(c.getTrangThai())) throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        Checklist k=checklist(c);List<Map<String,String>> missing=new ArrayList<>();
        if(!k.ten())missing.add(issue("tenCSLT","Thiếu tên"));
        if(!k.diaChi())missing.add(issue("diaChiChiTiet","Thiếu địa chỉ"));
        if(!k.phuong())missing.add(issue("maPhuong","Thiếu phường"));
        if(c.getLoai()==null)missing.add(issue("maLoai","Thiếu loại cơ sở"));
        if(!k.toaDo())missing.add(issue("toaDo","Tọa độ phải thuộc khu vực Nha Trang"));
        if(!k.gia())missing.add(issue("gia","Giá phải từ 500.000 đến 100.000.000 VNĐ"));
        if(!k.anhToiThieu3())missing.add(issue("hinhAnh","Cần tối thiểu 3 ảnh"));
        if(!k.tienIchToiThieu1())missing.add(issue("maTienIch","Cần tối thiểu 1 tiện ích"));
        if(!missing.isEmpty())throw new AppException(ErrorCode.CHECKLIST_INCOMPLETE,missing);
        c.setTrangThai("PENDING");c.setNgayGuiDuyet(LocalDateTime.now());c.setNgayCapNhat(LocalDateTime.now());
        return new Submitted(c.getMaCSLT(),c.getTrangThai(),Times.toInstant(c.getNgayGuiDuyet()));
    }
    @Transactional
    public Withdrawn withdraw(Identity identity,int id) {
        CoSoLuuTru c=owned(identity,id,true);
        if(!"PENDING".equals(c.getTrangThai()))throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        c.setTrangThai("DRAFT");c.setNgayGuiDuyet(null);c.setNgayCapNhat(LocalDateTime.now());
        return new Withdrawn(c.getMaCSLT(),c.getTrangThai());
    }
    @Transactional
    public Message delete(Identity identity,int id) {
        CoSoLuuTru c=owned(identity,id,true);
        if(!Set.of("DRAFT","REJECTED").contains(c.getTrangThai()))throw new AppException(ErrorCode.LISTING_NOT_DELETABLE);
        c.setTrangThai("REMOVED");c.setNgayCapNhat(LocalDateTime.now());
        events.publishEvent(new ListingDeleted(c.getMaCSLT(),mediaOf(c)));
        return new Message("Xóa cơ sở thành công");
    }
    private boolean apply(CoSoLuuTru c, ListingWrite q) {
        if((q.latitude()==null)!=(q.longitude()==null))invalid("toaDo","Phải cung cấp đồng thời vĩ độ và kinh độ");
        if(q.latitude()!=null && !validCoordinates(q.latitude(),q.longitude())) throw new AppException(ErrorCode.LOCATION_OUT_OF_RANGE);
        LoaiCoSo type=em.find(LoaiCoSo.class,q.maLoai()); if(type==null)invalid("maLoai","Loại cơ sở không tồn tại");
        Phuong ward=em.find(Phuong.class,q.maPhuong());if(ward==null)invalid("maPhuong","Phường không tồn tại");
        Set<TienIch> amenities=new HashSet<>();
        for(Integer id:q.maTienIch()==null?List.<Integer>of():q.maTienIch()) {
            TienIch t=em.find(TienIch.class,id);if(t==null)invalid("maTienIch","Tiện ích không tồn tại");amenities.add(t);
        }
        Contact next=q.lienHe()==null ? new Contact(null,null,null) : new Contact(clean(q.lienHe().soDienThoai()),clean(q.lienHe().zalo()),clean(q.lienHe().facebook()));
        validateUrl(next.zalo(),"zalo");validateUrl(next.facebook(),"facebook");
        boolean priceChanged=!numericEquals(c.getGia(),q.gia());
        boolean changed=!Objects.equals(c.getTenCSLT(),q.tenCSLT().trim()) || !Objects.equals(c.getDiaChiChiTiet(),q.diaChiChiTiet().trim())
            || !Objects.equals(c.getMoTa(),clean(q.moTa())) || !Objects.equals(c.getLoai(),type) || !Objects.equals(c.getPhuong(),ward)
            || !numericEquals(c.getLatitude(),q.latitude()) || !numericEquals(c.getLongitude(),q.longitude()) || priceChanged
            || !c.getTienIch().equals(amenities) || !contactOf(c).equals(next);
        if(!changed)return false;
        c.setTenCSLT(q.tenCSLT().trim());c.setDiaChiChiTiet(q.diaChiChiTiet().trim());c.setMoTa(clean(q.moTa()));c.setLoai(type);c.setPhuong(ward);
        c.setLatitude(q.latitude());c.setLongitude(q.longitude());c.setGia(q.gia());
        if(priceChanged)c.setNgayCapNhatGia(q.gia()==null?null:LocalDateTime.now());
        c.getTienIch().clear();c.getTienIch().addAll(amenities);
        if(!contactOf(c).equals(next)) {
            if(c.getMaCSLT()==null) listings.saveAndFlush(c);
            contacts.deleteAll(c.getLienHe());contacts.flush();c.getLienHe().clear();
            addContact(c,"SO_DIEN_THOAI",next.soDienThoai());addContact(c,"ZALO",next.zalo());addContact(c,"FACEBOOK",next.facebook());
        }
        c.setNgayCapNhat(LocalDateTime.now());return true;
    }
    private void addContact(CoSoLuuTru c,String kind,String value) {
        if(value==null)return;LienHe l=new LienHe();l.setCoSo(c);l.setLoaiLienHe(kind);l.setGiaTri(value);contacts.save(l);c.getLienHe().add(l);
    }
    private static Contact contactOf(CoSoLuuTru c) {
        Map<String,String> m=new HashMap<>();c.getLienHe().forEach(l->m.put(l.getLoaiLienHe(),l.getGiaTri()));
        return new Contact(m.get("SO_DIEN_THOAI"),m.get("ZALO"),m.get("FACEBOOK"));
    }
    private static Checklist checklist(CoSoLuuTru c) {
        return new Checklist(clean(c.getTenCSLT())!=null,clean(c.getDiaChiChiTiet())!=null,c.getPhuong()!=null,
            validCoordinates(c.getLatitude(),c.getLongitude()),c.getGia()!=null && c.getGia().compareTo(new BigDecimal("500000"))>=0 && c.getGia().compareTo(new BigDecimal("100000000"))<=0,
            c.getMedia().stream().filter(m->"IMAGE".equals(m.getLoai())).count()>=3,!c.getTienIch().isEmpty());
    }
    private static boolean validCoordinates(BigDecimal lat,BigDecimal lng) {
        return lat!=null && lng!=null && lat.compareTo(new BigDecimal("12.1"))>=0 && lat.compareTo(new BigDecimal("12.45"))<=0
            && lng.compareTo(new BigDecimal("109.1"))>=0 && lng.compareTo(new BigDecimal("109.3"))<=0;
    }
    private static List<String> actions(CoSoLuuTru c) {
        return switch(c.getTrangThai()) {
            case "DRAFT","REJECTED" -> List.of("EDIT","SUBMIT","DELETE","PREVIEW");
            case "HIDDEN" -> List.of("EDIT","SUBMIT","PREVIEW");
            case "APPROVED" -> List.of("EDIT","PREVIEW");
            case "PENDING" -> List.of("PREVIEW","WITHDRAW");default -> List.of();
        };
    }
    private static List<MediaInfo> mediaOf(CoSoLuuTru c) {
        return c.getMedia().stream().sorted(Comparator.comparing(Media::getThuTu,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Media::getMediaId,Comparator.nullsLast(Comparator.naturalOrder())))
            .map(m->new MediaInfo(m.getMediaId(),m.getDuongDan(),m.getLoai(),m.getKichThuoc(),m.getThuTu())).toList();
    }
    private static ListingDetail detailOf(CoSoLuuTru c) {
        List<MediaInfo> media=mediaOf(c);
        return new ListingDetail(c.getMaCSLT(),c.getTenCSLT(),c.getLoai().getMaLoai(),c.getLoai().getTenLoai(),c.getPhuong().getMaPhuong(),c.getPhuong().getTenPhuong(),
            c.getDiaChiChiTiet(),c.getMoTa(),c.getLatitude(),c.getLongitude(),c.getGia(),Times.toInstant(c.getNgayCapNhatGia()),contactOf(c),
            c.getTienIch().stream().map(TienIch::getMaTienIch).sorted().toList(),media,media.stream().filter(m->"IMAGE".equals(m.type())).count(),media.stream().filter(m->"VIDEO".equals(m.type())).count(),
            c.getTrangThai(),c.getLyDoTuChoi(),c.getLyDoViPham(),Times.toInstant(c.getNgayTao()),Times.toInstant(c.getNgayCapNhat()),checklist(c),actions(c));
    }
    private static void validateUrl(String url,String field) {
        if(url==null)return;
        try { var u=java.net.URI.create(url);if(!Set.of("http","https").contains(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null)invalid(field,"Liên kết HTTP/HTTPS không hợp lệ"); }
        catch(IllegalArgumentException ex) { invalid(field,"Liên kết không hợp lệ"); }
    }
    private static boolean numericEquals(BigDecimal a,BigDecimal b) {return a==null?b==null:b!=null&&a.compareTo(b)==0;}
    private static String clean(String s) {return s==null || s.isBlank()?null:s.trim();}
    private static Map<String,String> issue(String f,String m) {return Map.of("field",f,"message",m);}
    private static void invalid(String f,String m) {throw new AppException(ErrorCode.VALIDATION_ERROR,List.of(issue(f,m)));}
}
