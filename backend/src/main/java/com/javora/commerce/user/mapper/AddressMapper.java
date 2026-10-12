package com.javora.commerce.user.mapper;

import com.javora.commerce.user.dto.AddressRequest;
import com.javora.commerce.user.dto.AddressResponse;
import com.javora.commerce.user.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

	AddressResponse toResponse(Address address);

	void apply(AddressRequest request, @MappingTarget Address address);

}
