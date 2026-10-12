package com.javora.commerce.user.service;

import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.user.dto.UpdateProfileRequest;
import com.javora.commerce.user.dto.UserResponse;
import com.javora.commerce.user.entity.User;
import com.javora.commerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserResponse getProfile(Long userId) {
		return UserResponse.from(findUser(userId));
	}

	@Transactional
	public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = findUser(userId);
		user.setFullName(request.fullName().trim());
		user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
		return UserResponse.from(user);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

}
