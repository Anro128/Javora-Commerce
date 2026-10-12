package com.javora.commerce.user.repository;

import com.javora.commerce.user.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

	List<Address> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

	Optional<Address> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

	@Modifying
	@Query("update Address a set a.defaultAddress = false where a.userId = :userId and a.defaultAddress = true")
	void clearDefault(@Param("userId") Long userId);

}
