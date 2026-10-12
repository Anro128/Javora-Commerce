package com.javora.commerce.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory")
@EntityListeners(AuditingEntityListener.class)
public class Inventory {

	@Id
	@Column(name = "variant_id")
	private Long variantId;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "reserved_quantity", nullable = false)
	private int reservedQuantity;

	// Optimistic lock so two concurrent stock changes cannot silently overwrite each other.
	@Version
	private Long version;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Inventory(Long variantId, int quantity) {
		this.variantId = variantId;
		this.quantity = quantity;
	}

	public int available() {
		return quantity - reservedQuantity;
	}

}
