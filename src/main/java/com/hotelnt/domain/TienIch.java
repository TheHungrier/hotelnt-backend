package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TienIch")
@Getter
@Setter
@NoArgsConstructor
public class TienIch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maTienIch;

    private String tenTienIch;
    private String nhom;
    private String icon;
}
