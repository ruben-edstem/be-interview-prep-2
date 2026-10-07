package com.edstem.interviewprep.product.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductRequestTest {

	@Test
	void trimsSurroundingWhitespaceFromNameAndCategory() {
		ProductRequest request = new ProductRequest("  Desk Lamp ", " Books\t", 1_000L, 5, 4.0);

		assertThat(request.name()).isEqualTo("Desk Lamp");
		assertThat(request.category()).isEqualTo("Books");
	}

	@Test
	void keepsMissingValuesNullSoValidationCanReportThem() {
		ProductRequest request = new ProductRequest(null, null, 1_000L, 5, 4.0);

		assertThat(request.name()).isNull();
		assertThat(request.category()).isNull();
	}

	@Test
	void whitespaceOnlyValuesBecomeEmptySoNotBlankStillRejectsThem() {
		ProductRequest request = new ProductRequest("   ", "  ", 1_000L, 5, 4.0);

		assertThat(request.name()).isEmpty();
		assertThat(request.category()).isEmpty();
	}
}
