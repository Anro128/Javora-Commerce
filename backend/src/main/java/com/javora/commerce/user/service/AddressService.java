package com.javora.commerce.user.service;

import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.user.dto.AddressRequest;
import com.javora.commerce.user.dto.AddressResponse;
import com.javora.commerce.user.entity.Address;
import com.javora.commerce.user.mapper.AddressMapper;
import com.javora.commerce.user.repository.AddressRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

	static final int MAX_ADDRESSES_PER_USER = 10;

	private final AddressRepository addressRepository;
	private final AddressMapper addressMapper;

	@Transactional(readOnly = true)
	public List<AddressResponse> list(Long userId) {
		return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
				.map(addressMapper::toResponse)
				.toList();
	}

	@Transactional
	public AddressResponse create(Long userId, AddressRequest request) {
		long existing = addressRepository.countByUserId(userId);
		if (existing >= MAX_ADDRESSES_PER_USER) {
			throw new BusinessException("You can save at most %d addresses".formatted(MAX_ADDRESSES_PER_USER));
		}

		Address address = new Address();
		address.setUserId(userId);
		addressMapper.apply(request, address);

		// The first address always becomes the default one.
		boolean makeDefault = Boolean.TRUE.equals(request.defaultAddress()) || existing == 0;
		if (makeDefault) {
			addressRepository.clearDefault(userId);
		}
		address.setDefaultAddress(makeDefault);
		return addressMapper.toResponse(addressRepository.save(address));
	}

	@Transactional
	public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
		Address address = findOwned(userId, addressId);
		boolean wasDefault = address.isDefaultAddress();
		addressMapper.apply(request, address);

		if (Boolean.TRUE.equals(request.defaultAddress()) && !wasDefault) {
			addressRepository.clearDefault(userId);
			address.setDefaultAddress(true);
		} else {
			// A default address cannot be demoted directly; pick another one as default instead.
			address.setDefaultAddress(wasDefault);
		}
		return addressMapper.toResponse(address);
	}

	@Transactional
	public AddressResponse setDefault(Long userId, Long addressId) {
		Address address = findOwned(userId, addressId);
		if (!address.isDefaultAddress()) {
			addressRepository.clearDefault(userId);
			address.setDefaultAddress(true);
		}
		return addressMapper.toResponse(address);
	}

	@Transactional
	public void delete(Long userId, Long addressId) {
		Address address = findOwned(userId, addressId);
		boolean wasDefault = address.isDefaultAddress();
		addressRepository.delete(address);
		addressRepository.flush();

		if (wasDefault) {
			addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
					.findFirst()
					.ifPresent(next -> next.setDefaultAddress(true));
		}
	}

	private Address findOwned(Long userId, Long addressId) {
		return addressRepository.findByIdAndUserId(addressId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
	}

}
