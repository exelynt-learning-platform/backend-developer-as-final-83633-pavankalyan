package com.example.bookingsystem;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourcePaginationAndSortingIntegrationTest extends BaseIntegrationTest {

    @Test
    void largePageSizeShouldBeCapped()
            throws Exception {

        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("size", "1000000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void resourcePaginationShouldReturnRequestedPageSize()
            throws Exception {

        createResource();
        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void resourcePaginationShouldReturnSecondPage()
            throws Exception {

        createResource();
        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("page", "1")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void requestingPageBeyondAvailableDataShouldReturnEmptyPage()
            throws Exception {

        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("page", "5")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(5))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void resourcesShouldSupportCustomSorting()
            throws Exception {

        createResource();
        createResource();
        createResource();

        mockMvc.perform(
                        get("/resources")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                                .param("sort", "price,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort.sorted").value(true));
    }
}