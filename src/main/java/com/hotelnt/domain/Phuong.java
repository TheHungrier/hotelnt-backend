package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Phuong")
@Getter
@Setter
@NoArgsConstructor
public class Phuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maPhuong;

    private String tenPhuong;
}
