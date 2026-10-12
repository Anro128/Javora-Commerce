package com.javora.commerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.javora.commerce.common.exception.BusinessException;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

	@Test
	void happyPathMovesForwardOneStepAtATime() {
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAID)).isTrue();
		assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
		assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
		assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
		assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
	}

	@Test
	void statusesCannotBeSkippedOrReversed() {
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
		assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PENDING_PAYMENT)).isFalse();
		assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
	}

	@Test
	void finalStatusesAreTerminal() {
		for (OrderStatus terminal : EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)) {
			for (OrderStatus next : OrderStatus.values()) {
				assertThat(terminal.canTransitionTo(next)).as("%s -> %s", terminal, next).isFalse();
			}
		}
	}

	@Test
	void orderRejectsIllegalTransitions() {
		Order order = new Order();

		order.transitionTo(OrderStatus.CANCELLED);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("CANCELLED to PAID");
	}

}
