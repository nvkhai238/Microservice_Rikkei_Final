package com.ecommerce.identity.user;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(profileService.get(CurrentUser.id()));
    }

    @PutMapping
    public ApiResponse<UserResponse> update(@Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        return ApiResponse.success(profileService.update(CurrentUser.id(), request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        profileService.changePassword(CurrentUser.id(), request);
        return ApiResponse.success(null, "Password changed, please log in again on other devices");
    }
}
