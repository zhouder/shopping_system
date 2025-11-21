package com.cst.campussecondhand.controller;

import com.cst.campussecondhand.dto.TopProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final JdbcTemplate jdbcTemplate;

    /** 销量 Top10（基于视图） */
    @GetMapping("/api/reports/top10")
    public List<TopProductDTO> top10() {
        String sql = """
            SELECT id, title, price, sales, category_name
            FROM v_product_category
            ORDER BY sales DESC, id ASC
            LIMIT 10
        """;
        return jdbcTemplate.query(sql, (rs, i) ->
                new TopProductDTO(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("category_name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("sales")
                )
        );
    }

    /** 如果你想走存储过程，也可以暴露一个 */
    @GetMapping("/api/reports/top10/sp")
    public List<TopProductDTO> top10BySP() {
        String call = "CALL sp_top10_products()";
        return jdbcTemplate.query(call, (rs, i) ->
                new TopProductDTO(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("category_name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("sales")
                )
        );
    }
}
