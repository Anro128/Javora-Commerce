package com.javora.commerce.shipping;

import com.javora.commerce.user.entity.Address;
import java.util.List;

// Seam for a live courier API such as RajaOngkir or Biteship; only the zone based provider exists today.
public interface ShippingRateProvider {

	List<ShippingOption> quote(Address destination, int totalWeightGram);

}
