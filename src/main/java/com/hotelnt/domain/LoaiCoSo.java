package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "LoaiCoSo")
@Getter
@Setter
@NoArgsConstructor
public class LoaiCoSo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maLoai;

    private String tenLoai;
}
