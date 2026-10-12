package com.javora.commerce.inventory;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

	List<Inventory> findByVariantIdIn(Collection<Long> variantIds);

	@Query("""
			select distinct v.productId
			from ProductVariant v, Inventory i
			where i.variantId = v.id and v.active = true and v.productId in :productIds
			  and i.quantity - i.reservedQuantity > 0""")
	List<Long> findProductIdsInStock(@Param("productIds") Collection<Long> productIds);

	@Query("""
			select new com.javora.commerce.inventory.ProductStockTotals(
			    v.productId, count(v), coalesce(sum(i.quantity - i.reservedQuantity), 0L))
			from ProductVariant v, Inventory i
			where i.variantId = v.id and v.productId in :productIds
			group by v.productId""")
	List<ProductStockTotals> stockTotals(@Param("productIds") Collection<Long> productIds);

	@Query(value = """
			select new com.javora.commerce.inventory.InventoryRow(
			    v.id, v.sku, v.name, p.id, p.name, i.quantity, i.reservedQuantity)
			from ProductVariant v, Inventory i, Product p
			where i.variantId = v.id and p.id = v.productId
			  and (lower(p.name) like :pattern or lower(v.sku) like :pattern or lower(v.name) like :pattern)
			  and (:lowOnly = false or i.quantity - i.reservedQuantity <= :threshold)
			order by p.name, v.name""",
			countQuery = """
					select count(v)
					from ProductVariant v, Inventory i, Product p
					where i.variantId = v.id and p.id = v.productId
					  and (lower(p.name) like :pattern or lower(v.sku) like :pattern or lower(v.name) like :pattern)
					  and (:lowOnly = false or i.quantity - i.reservedQuantity <= :threshold)""")
	Page<InventoryRow> search(
			@Param("pattern") String pattern,
			@Param("lowOnly") boolean lowOnly,
			@Param("threshold") int threshold,
			Pageable pageable);

}
