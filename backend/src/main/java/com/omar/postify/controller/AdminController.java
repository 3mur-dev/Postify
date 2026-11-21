package com.omar.postify.controller;

import com.omar.postify.dto.*;
import com.omar.postify.dto.admin.*;
import com.omar.postify.dto.enums.UserSortField;
import com.omar.postify.service.admin.AdminContentService;
import com.omar.postify.service.admin.AdminDashboardService;
import com.omar.postify.service.admin.AdminReportService;
import com.omar.postify.service.admin.AdminUserService;
import com.omar.postify.util.AdminContextProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;
    private final AdminContentService adminContentService;
    private final AdminReportService adminReportService;
    private final AdminContextProvider adminContextProvider;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @Valid AdminDashboardRequest request
    ) {
        AdminDashboardResponse dashboard =
                adminDashboardService.getDashboard(request);

        return
                ApiResponse.success(
                        "Dashboard loaded successfully",
                        dashboard
                );
    }

    @GetMapping("/users")
    public PagedResponse<AdminUserDto> getUsers(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "CREATED_AT")
            UserSortField sortField,

            @RequestParam(defaultValue = "DESC")
            Sort.Direction direction,

            @RequestParam(defaultValue = "")
            String search
    ) {
        return PagedResponse.from( adminUserService.getAllUsers(search, page, size, sortField, direction) );

    }

    @GetMapping("/users/{publicId}")
    public ApiResponse<AdminUserDetailsDto> getUser(
            @PathVariable String publicId
    ) {
        return ApiResponse.success(
                "User loaded successfully",
                adminUserService.getUserByPublicId(publicId)
        );
    }

    @PatchMapping("/users/{publicId}/status")
    public ApiResponse<AdminUserDto> updateUserStatus(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.success(
                "User status updated successfully",
                adminUserService.updateUserStatus(
                        publicId,
                        request.status(),
                        adminContext()
                )
        );
    }

    @GetMapping("/posts")
    public PagedResponse<AdminPostDto> getPosts(
            @Valid @ModelAttribute AdminPostFilterRequest request
    ) {
        return PagedResponse.from( adminContentService.getPosts(request));
    }

    @GetMapping("/posts/{publicId}")
    public ApiResponse<AdminPostDto> getPost(@PathVariable String publicId) {

        return ApiResponse.success(
                "Post loaded successfully",
                adminContentService.getPost(publicId)
        );
    }

    @PatchMapping("/posts/{publicId}/hide")
    public ApiResponse<AdminPostDto> hidePost(@PathVariable String publicId) {

        return ApiResponse.success(
                "Post hidden successfully",
                adminContentService.hidePost(
                        publicId,
                        adminContext()
                )
        );
    }

    @PatchMapping("/posts/{publicId}/restore")
    public ApiResponse<AdminPostDto> restorePost(@PathVariable String publicId) {

        return ApiResponse.success(
                "Post restored successfully",
                adminContentService.restorePost(
                        publicId,
                        adminContext()
                )
        );
    }
    @DeleteMapping("/posts/{publicId}")
    public ApiResponse<AdminPostDto> deletePost(@PathVariable String publicId) {

        return ApiResponse.success(
                "Post deleted successfully",
                adminContentService.softDeletePost(
                        publicId,
                        adminContext()
                )
        );
    }

    @GetMapping("/reports")
    public PagedResponse<AdminReportDto> getReports(
            @Valid @ModelAttribute AdminReportFilterRequest request
    ) {
        return PagedResponse.from( adminReportService.getReports(request));
    }

    @GetMapping("/reports/{publicId}")
    public ApiResponse<AdminReportDto> getReport(@PathVariable String publicId) {

        return ApiResponse.success(
                "Report loaded successfully",
                adminReportService.getReport(publicId)
        );
    }

    @PatchMapping("/reports/{publicId}/resolve")
    public ApiResponse<AdminReportDto> resolveReport(@PathVariable String publicId) {

        return ApiResponse.success(
                "Report resolved successfully",
                adminReportService.resolveReport(
                        publicId
                )
        );
    }
    @PatchMapping("/reports/{publicId}/reject")
    public ApiResponse<AdminReportDto> rejectReport(@PathVariable String publicId) {

        return ApiResponse.success(
                "Report rejected successfully",
                adminReportService.rejectReport(
                        publicId
                )
        );
    }

    private AdminContext adminContext() {
        return adminContextProvider.getCurrentContext();
    }
}
