package com.hotelnt.modules.filteroption;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelnt.modules.filteroption.FilterOptionController.FilterOptionsResponse;
import com.hotelnt.modules.filteroption.FilterOptionController.MapBoundsRange;
import com.hotelnt.modules.filteroption.FilterOptionController.PriceRange;
import com.hotelnt.modules.filteroption.FilterOptionController.SortOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Test tang web (khong can SQL Server): service duoc gia lap. */
@WebMvcTest(FilterOptionController.class)
class FilterOptionControllerTest {

    @Autowired MockMvc mvc;
    @MockBean FilterOptionService service;

    @Test void getOk() throws Exception {
        when(service.get()).thenReturn(new FilterOptionsResponse(List.of(), List.of(), List.of(), List.of(),
                new PriceRange(500_000L, 100_000_000L), List.of(1, 2, 3, 5, 10),
                List.of(new SortOption("GIA_ASC", "Giá tăng dần")),
                new MapBoundsRange(12.1, 12.45, 109.1, 109.3)));
        mvc.perform(get("/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceRange.giaMin").value(500000))
                .andExpect(jsonPath("$.priceRange.giaMax").value(100000000))
                .andExpect(jsonPath("$.radiusOptions[4]").value(10))
                .andExpect(jsonPath("$.sortOptions[0].value").value("GIA_ASC"))
                .andExpect(jsonPath("$.mapBounds.latMin").value(12.1))
                .andExpect(jsonPath("$.mapBounds.lngMax").value(109.3))
                .andExpect(jsonPath("$.wards").isArray())
                .andExpect(jsonPath("$.nearbyPlaces").isArray());
    }
}
