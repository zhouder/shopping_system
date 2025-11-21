package com.cst.campussecondhand.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TopProductDTO {
    private Integer id;
    private String  title;
    private String  categoryName;
    private BigDecimal price;
    private Integer sales;
}
