package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Du lieu gui den API dang ky Host. */
public record SignupRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
        String hoTen,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
        String soDienThoai,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
        String email,

        @NotBlank(message = "CCCD không được để trống")
        @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải gồm đúng 12 chữ số")
        String cccd,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
        @Pattern(regexp = "(?s)^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$",
                message = "Mật khẩu phải gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt")
        String matKhau
) {}
